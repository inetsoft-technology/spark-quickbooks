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

import inetsoft.spark.quickbooks.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;

/**
 * Load the quickbooks runtime and execute a query
 */
public class QuickbooksStreamReader implements Serializable {
   public QuickbooksStreamReader(QuickbooksDataSourceOptions options) {
      this.options = options;
   }

   public List<Object> getEntities() {
      return getEntities(false);
   }

   public SparkSchema getSchema() {
      final List<Object> entities = getEntities(true);
      final SparkSchema sparkSchema = new SparkSchemaGenerator().generateSchema(entities.toArray());
      return options.isExpandStructs() ? sparkSchema.flatten(options.isExpandArrays()) : sparkSchema;
   }

   private List<Object> getEntities(boolean schemaOnly) {
      options.setSchemaOnly(schemaOnly);

      try {
         final QuickbooksClassloader classLoader =
            QuickbooksClassloader.create(getClass().getClassLoader());
         final Class<?> aClass =
            classLoader.loadClass("inetsoft.spark.quickbooks.QuickbooksRuntime");
         final QuickbooksAPI api = (QuickbooksAPI) aClass.newInstance();
         final QuickbooksAPI.QuickbooksQueryResult result = api.loadData(options);
         return Collections.unmodifiableList(result.getEntities());
      }
      catch(Exception e) {
         LOG.error("Failed to execute quickbooks query", e);
         throw new RuntimeException(e);
      }
   }

   private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   private final QuickbooksDataSourceOptions options;
}
