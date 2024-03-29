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

import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.services.QueryResult;
import com.intuit.oauth2.exception.OAuthException;
import inetsoft.spark.quickbooks.token.TokenStrategy;
import inetsoft.spark.quickbooks.token.TokenStrategyFactory;

public class QuickbooksRuntime implements QuickbooksAPI {
   public QuickbooksQueryResult loadData(QuickbooksDataSourceOptions options) {
      final String accessToken = options.getAccessToken();
      final String clientId = options.getClientId();
      final String clientSecret = options.getClientSecret();
      final String authorizationCode = options.getAuthorizationCode();
      final String companyId = options.getCompanyId();
      final String redirectUrl = options.getRedirectUrl();
      final boolean production = options.isProduction();
      final String entity = options.getEntity();
      final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

      try {
         Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
         final TokenStrategy tokenStrategy = TokenStrategyFactory.create(accessToken, clientId,
                                                                         clientSecret, companyId,
                                                                         authorizationCode,
                                                                         production, redirectUrl);
         final String token = tokenStrategy.getAccessToken();
         final QueryExecutor queryExecutor = new QueryExecutorService();
         final QueryResult result = queryExecutor.execute(token, companyId, production,
                                                          entity, options.isSchemaOnly());
         return new QueryResultAdapter(result);
      }
      catch(OAuthException e) {
         throw new RuntimeException("OAuth authentication failed", e);
      }
      catch(FMSException e) {
         throw new RuntimeException("SDK exception", e);
      }
      finally {
         // switch back to original classloader
         Thread.currentThread().setContextClassLoader(contextClassLoader);
      }
   }
}
