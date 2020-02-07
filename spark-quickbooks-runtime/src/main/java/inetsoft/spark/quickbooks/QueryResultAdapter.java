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

package inetsoft.spark.quickbooks;

import com.intuit.ipp.services.QueryResult;

import java.util.ArrayList;
import java.util.List;

public class QueryResultAdapter implements QuickbooksAPI.QuickbooksQueryResult {
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
