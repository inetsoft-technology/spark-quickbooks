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
import org.apache.spark.sql.types.*;
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
                    boolean production,
                    boolean expandArrays,
                    String entity)
   {
      this.expandArrays = expandArrays;
      reader = new QuickbooksStreamReader(clientId, clientSecret, authorizationCode, companyId, redirectUrl, production, entity);
   }

   @Override
   public StructType readSchema() {
      if(schema == null) {
         final List<Object> entities = reader.getEntities();
         schema = getSparkSchema(entities);
      }

      return schema.getStructType();
   }

   @Override
   public List<DataReaderFactory<Row>> createDataReaderFactories() {
      return Arrays.asList(new QuickbooksReaderFactory(schema, reader));
   }

   private SparkSchema getSparkSchema(List<Object> entities) {
      final SparkSchema sparkSchema = sparkSchemaGenerator.generateSchema(entities.toArray());
      return expandArrays ? sparkSchema.flatten() : sparkSchema;
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
         return createRow(currObj, schema);
      }

      private Row createRow(Object data, SparkSchema dataSchema) {
         if(data == null || dataSchema == null) {
            return null;
         }

         final ArrayList<Object> cells = new ArrayList<>();
         final StructType structType = dataSchema.getStructType();

         for(StructField structField : structType.fields()) {
               // try to get data from object
            final Object result = getObjectByField(data, structField, dataSchema);
            cells.add(result);
         }

         return new GenericRowWithSchema(cells.toArray(), structType);
      }

      /**
       * Get the cell parentObject for a field in the schema
       *
       * @param parentObject the POJO that has the field
       * @param field        the field we are trying to create
       * @param parentSchema the schema of the parent object
       *
       * @return the parentObject that will fill the cell in the row
       */
      private Object getObjectByField(Object parentObject, StructField field, SparkSchema parentSchema) {
         final String name = field.name();
         final String[] tokens = name.split("_");
         final String fieldName = tokens[0];
         SparkSchema schema = parentSchema.getSchema(fieldName);
         final String methodName = parentSchema.getMethodName(fieldName);
         // get the POJO getter name from the schema
         Object result = callMethodOnObject(parentObject, methodName);

         if(tokens.length > 1) {
            SparkSchema currentParent = parentSchema;

            for(int i = 1; i < tokens.length; i++) {
               final String parent = tokens[i - 1];
               String token = tokens[i];

               if(result != null) {
                  final StructType parentStructType = currentParent.getStructType();
                  final StructField parentField = parentStructType.apply(parent);

                  // if this is an array it's delimited by index e.g. customers_3_name so we
                  // pull the 3rd element from the array and move to the next token
                  if(parentField.dataType() instanceof ArrayType && result instanceof Collection) {
                     final int index;
                     final int size = ((Collection) result).size();

                     if(size > 0) {
                        // parse index and skip to next token
                        index = Integer.parseInt(token);
                        token = tokens[++i];
                     }
                     else {
                        return null;
                     }

                     // arrays must have consistent schema but projecting the schema onto the
                     // values may create null elements
                     if(index >= size) {
                        return null;
                     }

                     final Object[] objects = ((Collection) result).toArray();
                     result = objects[index];
                  }

                  currentParent = currentParent.getSchema(parent);

                  // get the POJO getter name from the schema
                  String meth = currentParent.getMethodName(token);
                  result = callMethodOnObject(result, meth);
               }
               else {
                  return null;
               }
            }
         }

         if(result == null || result instanceof Enum) {
            return null;
         }

         if(field.dataType() instanceof ArrayType) {
            final ArrayList<Object> childCells = new ArrayList<>();
            final Iterator<?> iterator = ((Collection<?>) result).iterator();
            iterator.forEachRemaining(obj -> childCells.add(createRow(obj, schema)));
            result = childCells.toArray();
         }
         if(field.dataType() instanceof StructType) {
            // wrap objects in nested rows
            result = createRow(result, schema);
         }

         if(result instanceof Date) {
            result = new java.sql.Date(((Date) result).getTime());
         }

         return result;
      }

      private Object callMethodOnObject(Object bean, String methodName) {
         if(methodName == null) {
            return null;
         }

         try {
            // get the getter method from the object class
            final Method method = bean.getClass().getMethod(methodName);
            return method.invoke(bean);
         }
         catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LOG.warn("Failed to get data from object, using null", e);
            return null;
         }
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

   private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   private final SparkSchemaGenerator sparkSchemaGenerator = new SparkSchemaGenerator();
   private final QuickbooksStreamReader reader;
   private final boolean expandArrays;
   private SparkSchema schema = null;
}
