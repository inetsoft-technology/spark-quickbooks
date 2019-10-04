/*
 * Copyright 2019 InetSoft Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
                                         String apiUrl,
                                         String entity)
   {
      this.clientId = clientId;
      this.clientSecret = clientSecret;
      this.authorizationCode = authorizationCode;
      this.companyId = companyId;
      this.redirectUrl = redirectUrl;
      this.apiUrl = apiUrl;
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
      Config.setProperty(Config.BASE_URL_QBO, apiUrl);
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
   private String apiUrl;
   private String entity;
}
