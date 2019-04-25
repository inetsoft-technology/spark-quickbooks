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

import inetsoft.spark.quickbooks.SparkSchema;
import inetsoft.spark.quickbooks.SparkSchemaGenerator;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema;
import org.apache.spark.sql.sources.v2.reader.*;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Read the quickbooks schema and create the entities factory
 */
public class QuickbooksReader implements DataSourceReader {
   QuickbooksReader(String clientId,
                    String clientSecret,
                    String authorizationCode,
                    String companyId,
                    String redirectUrl,
                    String entity)
   {
      reader = new QuickbooksStreamReader(clientId, clientSecret, authorizationCode, companyId, redirectUrl, entity);
   }

   @Override
   public StructType readSchema() {
      if(schema == null) {
         final List<Object> entities = reader.getEntities();
         schema = getSparkSchema(entities);
      }

      return schema.getStructType();
   }

   private SparkSchema getSparkSchema(List<Object> entities) {
      try {
         return sparkSchemaGenerator.generateSchema(entities.toArray());
      }
      catch(Exception e) {
         LOG.error("Failed to generate schema for {}", getClass(), e);
         throw new RuntimeException(e);
      }
   }

   @Override
   public List<DataReaderFactory<Row>> createDataReaderFactories() {
      return Arrays.asList(new QuickbooksReaderFactory(schema, reader));
   }

   private static class TaskDataReader implements DataReader<Row> {
      public TaskDataReader(SparkSchema schema, QuickbooksStreamReader reader) {
         this.schema = schema;
         entities = reader.getEntities().iterator();
      }

      @Override
      public boolean next() {
         if(entities.hasNext()) {
            currObj = entities.next();
         }
         else {
            currObj = null;
         }

         return currObj != null;
      }

      @Override
      public void close() {
      }

      @Override
      public Row get() {
         return getRow(currObj, schema);
      }

      private Row getRow(Object data, SparkSchema schema) {
         if(data == null) {
            return null;
         }

         final List<Object> cells = new ArrayList<>();

         for(String field : schema.getStructType().fieldNames()) {
            final String methodName = schema.getMethodName(field);
            final StructType structType = schema.getStructType(field);

            try {
               final Method method = data.getClass().getMethod(methodName);
               Object result = method.invoke(data);

               if(structType != null) {
                  final SparkSchema sparkSchema = schema.getSchema(field);

                  if(result instanceof Collection<?>) {
                     final ArrayList<Object> childCells = new ArrayList<>();
                     final Iterator<?> iterator = ((Collection<?>) result).iterator();
                     iterator.forEachRemaining(obj -> childCells.add(getRow(obj, sparkSchema)));
                     result = childCells.toArray();
                  }
                  else {
                     result = getRow(result, sparkSchema);
                  }
               }

               if(result instanceof Date) {
                  result = new java.sql.Date(((Date) result).getTime());
               }

               cells.add(result);
            }
            catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
               LOG.error(e.getMessage());
               return null;
            }
         }

         return new GenericRowWithSchema(cells.toArray(), schema.getStructType());
      }

      private SparkSchema schema;
      private Iterator entities;
      private Object currObj;
   }

   /**
    * Note that this has to be serializable. Each instance is sent to an executor,
    * which uses it to create a entities for its own use.
    */
   private static class QuickbooksReaderFactory implements DataReaderFactory<Row> {
      public QuickbooksReaderFactory(SparkSchema schema, QuickbooksStreamReader reader) {
         this.schema = schema;
         this.reader = reader;
      }

      @Override
      public DataReader<Row> createDataReader() {
         return new TaskDataReader(schema, reader);
      }

      private final QuickbooksStreamReader reader;
      private SparkSchema schema;
   }

   private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   private final SparkSchemaGenerator sparkSchemaGenerator = new SparkSchemaGenerator();
   private final QuickbooksStreamReader reader;
   private SparkSchema schema = null;
}
