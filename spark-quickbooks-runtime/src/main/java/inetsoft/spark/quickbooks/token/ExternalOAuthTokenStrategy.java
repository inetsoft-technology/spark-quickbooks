package inetsoft.spark.quickbooks.token;

/**
 * This strategy assumes that OAuth is handled outside of the data source so we just use the given
 * access token
 */
public class ExternalOAuthTokenStrategy implements TokenStrategy {
   public ExternalOAuthTokenStrategy(String accessToken) {
      this.accessToken = accessToken;
   }

   @Override
   public String getAccessToken() {
      return accessToken;
   }

   private final String accessToken;
}
