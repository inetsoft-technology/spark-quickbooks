package inetsoft.spark.quickbooks.token;

public class AuthorizationCodeFlowTokenStrategyBuilder {
   public AuthorizationCodeFlowTokenStrategyBuilder setClientId(String clientId) {
      this.clientId = clientId;
      return this;
   }

   public AuthorizationCodeFlowTokenStrategyBuilder setClientSecret(String clientSecret) {
      this.clientSecret = clientSecret;
      return this;
   }

   public AuthorizationCodeFlowTokenStrategyBuilder setCompanyId(String companyId) {
      this.companyId = companyId;
      return this;
   }

   public AuthorizationCodeFlowTokenStrategyBuilder setAuthorizationCode(String authorizationCode) {
      this.authorizationCode = authorizationCode;
      return this;
   }

   public AuthorizationCodeFlowTokenStrategyBuilder setProduction(boolean production) {
      this.production = production;
      return this;
   }

   public AuthorizationCodeFlowTokenStrategyBuilder setRedirectUrl(String redirectUrl) {
      this.redirectUrl = redirectUrl;
      return this;
   }

   public AuthorizationCodeFlowTokenStrategy build() {
      return new AuthorizationCodeFlowTokenStrategy(clientId,
                                                    clientSecret,
                                                    companyId,
                                                    authorizationCode,
                                                    production,
                                                    redirectUrl);
   }

   private String clientId;
   private String clientSecret;
   private String companyId;
   private String authorizationCode;
   private boolean production;
   private String redirectUrl;
}