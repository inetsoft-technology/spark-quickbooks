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

import com.intuit.ipp.core.*;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.security.OAuth2Authorizer;
import com.intuit.ipp.services.*;
import com.intuit.ipp.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

public class QueryExecutorService implements QueryExecutor {
   @Override
   public QueryResult execute(String token, String companyId, boolean production,
                              String entity, boolean schemaOnly) throws FMSException
   {
      final String apiUrl = production ? productionUrl : sandboxUrl;
      Config.setProperty(Config.BASE_URL_QBO, apiUrl);
      final OAuth2Authorizer oauth = new OAuth2Authorizer(token);
      final Context context = new Context(oauth, ServiceType.QBO, companyId);
      final DataService service = new DataService(context);

      // first execute a count query to determine pagination
      final int totalCount = schemaOnly ? 1 : getTotalCount(service, entity);

      // next execute paginated results until complete
      final QueryResult queryResult = new QueryResult();
      int startPosition = 1;
      queryResult.setStartPosition(startPosition);
      queryResult.setTotalCount(totalCount);
      queryResult.setMaxResults(totalCount);
      final ArrayList<IEntity> entities = new ArrayList<>();
      BatchOperation batchOperation = new BatchOperation();
      int counter = 0;

      for(int remaining = totalCount; remaining > 0; remaining -= RESULT_LIMIT) {
         final int maxResults = Math.min(RESULT_LIMIT, remaining);
         final String query = String.format("SELECT * FROM %s STARTPOSITION %d MAXRESULTS %d",
                                            entity,
                                            startPosition,
                                            maxResults);
         batchOperation.addQuery(query, String.valueOf(counter++));
         startPosition += RESULT_LIMIT;

         if(counter % BATCH_LIMIT == 0) {
            executeBatchOperation(service, entities, batchOperation);
            batchOperation = new BatchOperation();
         }
      }

      executeBatchOperation(service, entities, batchOperation);
      queryResult.setEntities(entities);
      return queryResult;
   }

   private void executeBatchOperation(DataService service,
                                      ArrayList<IEntity> entities,
                                      BatchOperation batchOperation) throws FMSException
   {
      final List<String> bIds = batchOperation.getBIds();

      if(bIds.size() > 0) {
         final String index = bIds.get(0);
         LOG.debug("Executing QuickBooks query from index: {}", index);
         service.executeBatch(batchOperation);

         for(String bId : bIds) {
            final QueryResult queryResponse = batchOperation.getQueryResponse(bId);
            entities.addAll(queryResponse.getEntities());
         }
      }
   }

   /**
    * Get the total number of entities in the query response
    */
   private int getTotalCount(DataService service, String entity) throws FMSException {
      final QueryResult countResult = service.executeQuery("SELECT COUNT(*) FROM " + entity);
      final Integer totalCount = countResult.getTotalCount();
      LOG.debug("QuickBooks count returned {} result(s)", totalCount);
      return totalCount != null ? totalCount : 1;
   }

   // max 30 queries per batch operation
   public static final int BATCH_LIMIT = 30;
   // max number of results quickbooks can return in 1 call
   private static final int RESULT_LIMIT = 1000;
   private static final String sandboxUrl = "https://sandbox-quickbooks.api.intuit.com/v3/company";
   private static final String productionUrl = "https://quickbooks.api.intuit.com/v3/company";
   private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
}
