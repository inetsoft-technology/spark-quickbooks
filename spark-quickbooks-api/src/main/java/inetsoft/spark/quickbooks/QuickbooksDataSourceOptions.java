/*
 * Copyright 2021 InetSoft Technology
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

import java.io.Serializable;
import java.util.Map;

public class QuickbooksDataSourceOptions implements Serializable {
   private QuickbooksDataSourceOptions(Map<String, String> options) {
      this.options = options;
   }

   public static QuickbooksDataSourceOptions from(Map<String, String> options) {
      return new QuickbooksDataSourceOptions(options);
   }

   public String getClientId() {
      return options.getOrDefault("clientId", null);
   }

   public String getClientSecret() {
      return options.getOrDefault("clientSecret", null);
   }

   public String getAuthorizationCode() {
      return options.getOrDefault("authorizationCode", null);

   }

   public String getAccessToken() {
      return options.getOrDefault("accessToken", null);
   }

   public String getCompanyId() {
      return options.getOrDefault("companyId", null);
   }

   public String getRedirectUrl() {
      return options.getOrDefault("redirectUrl", null);
   }

   public boolean isProduction() {
      return options.get("production").equalsIgnoreCase(Boolean.TRUE.toString());
   }

   public boolean isExpandArrays() {
      return options.get("expandArrays").equalsIgnoreCase(Boolean.TRUE.toString());
   }

   public boolean isExpandStructs() {
      return options.get("expandStructs").equalsIgnoreCase(Boolean.TRUE.toString());
   }

   public String getEntity() {
      return options.getOrDefault("entity", "companyInfo");
   }

   public boolean isSchemaOnly() {
      return schemaOnly;
   }

   public void setSchemaOnly(boolean schemaOnly) {
      this.schemaOnly = schemaOnly;
   }

   private final Map<String, String> options;
   private boolean schemaOnly;
}
