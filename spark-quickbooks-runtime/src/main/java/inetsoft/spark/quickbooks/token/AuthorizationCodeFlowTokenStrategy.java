package inetsoft.spark.quickbooks.token;

import com.intuit.oauth2.client.OAuth2PlatformClient;
import com.intuit.oauth2.config.Environment;
import com.intuit.oauth2.config.OAuth2Config;
import com.intuit.oauth2.data.BearerTokenResponse;
import com.intuit.oauth2.exception.OAuthException;
import inetsoft.spark.quickbooks.QuickbooksConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Handle exchanging the authorization code for an access token, saving the tokens to disk, and
 * refreshing the tokens if necessary.
 */
public class AuthorizationCodeFlowTokenStrategy implements TokenStrategy {
   public AuthorizationCodeFlowTokenStrategy(String clientId,
                                             String clientSecret,
                                             String companyId,
                                             String authorizationCode,
                                             boolean production,
                                             String redirectUrl)
   {
      this.clientId = clientId;
      this.companyId = companyId;
      this.authorizationCode = authorizationCode;
      this.clientSecret = clientSecret;
      this.production = production;
      this.redirectUrl = redirectUrl;
   }

   @Override
   public String getAccessToken() throws OAuthException {
      OAuth2Config oauth2Config = new OAuth2Config.OAuth2ConfigBuilder(clientId, clientSecret)
         .callDiscoveryAPI(production ? Environment.PRODUCTION : Environment.SANDBOX)
         .buildConfig();
      client = new OAuth2PlatformClient(oauth2Config);
      final QuickbooksConfig config = QuickbooksConfig.readConfig(clientId, companyId);
      return connect(config);
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

   private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   private final String clientId;
   private final String companyId;
   private final String authorizationCode;
   private final String clientSecret;
   private final boolean production;
   private final String redirectUrl;
   private OAuth2PlatformClient client;
}
