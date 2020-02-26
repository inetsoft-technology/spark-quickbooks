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
package inetsoft.spark.quickbooks.source;

import org.apache.spark.sql.sources.DataSourceRegister;
import org.apache.spark.sql.sources.v2.*;
import org.apache.spark.sql.sources.v2.reader.DataSourceReader;

public class DefaultSource implements DataSourceV2, ReadSupport, DataSourceRegister {
   @Override
   public String shortName() {
      return "quickbooks";
   }

   @Override
   public DataSourceReader createReader(DataSourceOptions options) {
      final String clientId = options.get("clientId").orElse(null);
      final String clientSecret = options.get("clientSecret").orElse(null);
      final String authorizationCode = options.get("authorizationCode").orElse(null);
      final String accessToken = options.get("accessToken").orElse(null);
      final String companyId = options.get("companyId").orElse(null);
      final String redirectUrl = options.get("redirectUrl").orElse(URL);
      final String entity = options.get("entity").orElse("companyInfo");
      final boolean expandArrays = options.get("expandArrays")
                                          .map(Boolean.TRUE.toString()::equals)
                                          .orElse(false);
      final boolean production = options.get("production")
                                        .map(Boolean.TRUE.toString()::equals)
                                        .orElse(false);
      return new QuickbooksReader(clientId, clientSecret, authorizationCode, accessToken,
                                  companyId, redirectUrl, production, expandArrays, entity);
   }

   private static final String URL = "https://developer.intuit.com/v2/OAuth2Playground/RedirectUrl";
}
