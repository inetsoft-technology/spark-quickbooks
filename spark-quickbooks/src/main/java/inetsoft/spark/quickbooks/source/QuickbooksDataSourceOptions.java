/*
 * Copyright 2020 InetSoft Technology
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

package inetsoft.spark.quickbooks.source;

import org.apache.spark.sql.sources.v2.DataSourceOptions;

public class QuickbooksDataSourceOptions {
   private QuickbooksDataSourceOptions(DataSourceOptions options) {
      this.options = options;
   }

   public static QuickbooksDataSourceOptions from(DataSourceOptions options) {
      return new QuickbooksDataSourceOptions(options);
   }

   public String getClientId() {
      return options.get("clientId").orElse(null);
   }

   public String getClientSecret() {
      return options.get("clientSecret").orElse(null);
   }

   public String getAuthorizationCode() {
      return options.get("authorizationCode").orElse(null);

   }

   public String getAccessToken() {
      return options.get("accessToken").orElse(null);
   }

   public String getCompanyId() {
      final String companyId = options.get("companyId").orElse(null);
      return companyId;
   }

   public String getRedirectUrl() {
      final String redirectUrl = options.get("redirectUrl").orElse(URL);
      return redirectUrl;
   }

   public boolean isProduction() {
      return options.get("production")
                    .map(Boolean.TRUE.toString()::equals)
                    .orElse(false);
   }

   public boolean isExpandArrays() {
      final boolean expandArrays = options.get("expandArrays")
                                          .map(Boolean.TRUE.toString()::equals)
                                          .orElse(false);
      return expandArrays;
   }

   public boolean isExpandStructs() {
      final boolean expandArrays = options.get("expandStructs")
                                          .map(Boolean.TRUE.toString()::equals)
                                          .orElse(false);
      return expandArrays;
   }

   public String getEntity() {
      final String entity = options.get("entity").orElse("companyInfo");
      return entity;
   }

   private static final String URL = "https://developer.intuit.com/v2/OAuth2Playground/RedirectUrl";
   private final DataSourceOptions options;
}
