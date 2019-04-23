/*
 * Copyright (c) 2019, InetSoft Technology Corp, All Rights Reserved.
 *
 * The software and information contained herein are copyrighted and
 * proprietary to InetSoft Technology Corp. This software is furnished
 * pursuant to a written license agreement and may be used, copied,
 * transmitted, and stored only in accordance with the terms of such
 * license and with the inclusion of the above copyright notice. Please
 * refer to the file "COPYRIGHT" for further copyright and licensing
 * information. This software and information or any other copies
 * thereof may not be provided or otherwise made available to any other
 * person.
 */
package inetsoft.spark.quickbooks;

import com.intuit.ipp.core.Context;
import com.intuit.ipp.core.ServiceType;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.security.OAuth2Authorizer;
import com.intuit.ipp.services.DataService;
import com.intuit.ipp.services.QueryResult;
import com.intuit.ipp.util.Config;
import com.intuit.oauth2.client.OAuth2PlatformClient;
import com.intuit.oauth2.config.Environment;
import com.intuit.oauth2.config.OAuth2Config;
import com.intuit.oauth2.data.BearerTokenResponse;
import com.intuit.oauth2.exception.OAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

public class QuickbooksRuntime implements QuickbooksAPI {
   public QuickbooksQueryResult loadData(String clientId,
                                         String clientSecret,
                                         String authorizationCode,
                                         String companyId,
                                         String redirectUrl,
                                         String entity)
   {
      this.clientId = clientId;
      this.clientSecret = clientSecret;
      this.authorizationCode = authorizationCode;
      this.companyId = companyId;
      this.redirectUrl = redirectUrl;
      this.entity = entity;
      return loadData(QuickbooksConfig.readConfig(clientId, companyId));
   }

   /**
    * Call the Quickbooks API to check the tokens and execute our query
    */
   private QuickbooksQueryResult loadData(QuickbooksConfig config) {
      final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

      try {
         Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

         // check tokens
         OAuth2Config oauth2Config = new OAuth2Config.OAuth2ConfigBuilder(clientId, clientSecret)
            .callDiscoveryAPI(Environment.SANDBOX)
            .buildConfig();
         client = new OAuth2PlatformClient(oauth2Config);
         final String accessToken = connect(config);
         return execute(accessToken);
      }
      catch(OAuthException e) {
         LOG.error("OAuth authentication failed", e);
         throw new RuntimeException(e);
      }
      catch(FMSException e) {
         LOG.error("SDK exception", e);
         throw new RuntimeException(e);
      }
      finally {
         // switch back to original classloader
         Thread.currentThread().setContextClassLoader(contextClassLoader);
      }
   }

   private QuickbooksQueryResult execute(String accessToken) throws FMSException {
      final String query = "select * from " + entity;
      final QueryResult result = execute(query, companyId, accessToken);
      return new QueryResultAdapter(result);
   }

   private QueryResult execute(String query,
                               String companyId,
                               String accessToken) throws FMSException
   {
      final String URL = "https://sandbox-quickbooks.api.intuit.com/v3/company";
      Config.setProperty(Config.BASE_URL_QBO, URL);
      OAuth2Authorizer oauth = new OAuth2Authorizer(accessToken);
      Context context = new Context(oauth, ServiceType.QBO, companyId);
      DataService service = new DataService(context);
      return service.executeQuery(query);
   }

   /**
    * Handle OAuth connection. If the access token is null retrieve new tokens. If the refresh token
    * has expired and the access token is not null refresh the current access token. Otherwise don't
    * call any authorization mechanism
    *
    * @return the current and valid access token
    */
   private String connect(QuickbooksConfig config) throws OAuthException {
      final String accessToken = config.getAccessToken();
      final long expiration = config.getExpiration();

      if(accessToken == null) {
         LOG.debug("Fetching OAuth tokens");
         final BearerTokenResponse response =
            client.retrieveBearerTokens(authorizationCode, redirectUrl);
         config = config.updateCredentials(response.getExpiresIn(),
                                           response.getAccessToken(),
                                           response.getRefreshToken(), clientId, companyId);
      }
      else {
         if(expiration > -1 && expiration < System.currentTimeMillis()) {
            LOG.debug("Refreshing OAuth Tokens");
            final BearerTokenResponse response;
            response = client.refreshToken(config.getRefreshToken());
            config = config.updateCredentials(response.getExpiresIn(),
                                              response.getAccessToken(),
                                              response.getRefreshToken(), clientId, companyId);
         }
      }

      return config.getAccessToken();
   }

   public static class QueryResultAdapter implements QuickbooksQueryResult {
      public QueryResultAdapter(QueryResult queryResult) {
         this.queryResult = queryResult;
      }

      @Override
      public List<Object> getEntities() {
         return new ArrayList<>(queryResult.getEntities());
      }

      @Override
      public int getStartPosition() {
         return queryResult.getStartPosition();
      }

      @Override
      public int getMaxResults() {
         return queryResult.getMaxResults();
      }

      @Override
      public int getTotalCount() {
         return queryResult.getTotalCount();
      }

      private QueryResult queryResult;
   }

   private final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   private OAuth2PlatformClient client;
   private String clientId;
   private String clientSecret;
   private String authorizationCode;
   private String companyId;
   private String redirectUrl;
   private String entity;
}
