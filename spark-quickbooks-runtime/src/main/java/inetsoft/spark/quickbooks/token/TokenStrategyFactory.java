package inetsoft.spark.quickbooks.token;

/**
 * Determine the type of token strategy to use from the data source options. Passing a non-null
 * access token will avoid any OAuth handling.
 */
public class TokenStrategyFactory {
   public static TokenStrategy create(String accessToken, String clientId,
                                      String clientSecret,
                                      String companyId,
                                      String authorizationCode,
                                      boolean production,
                                      String redirectUrl)
   {
      if(accessToken != null) {
         return new ExternalOAuthTokenStrategy(accessToken);
      }

      return new AuthorizationCodeFlowTokenStrategyBuilder().setClientId(clientId)
                                                            .setClientSecret(clientSecret)
                                                            .setCompanyId(companyId)
                                                            .setAuthorizationCode(authorizationCode)
                                                            .setProduction(production)
                                                            .setRedirectUrl(redirectUrl)
                                                            .build();
   }
}
