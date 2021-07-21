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

package inetsoft.spark.quickbooks.source;

import inetsoft.spark.quickbooks.SparkSchema;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.catalyst.expressions.GenericInternalRow;
import org.apache.spark.sql.catalyst.util.ArrayData;
import org.apache.spark.sql.connector.read.PartitionReader;
import org.apache.spark.sql.types.*;
import org.apache.spark.unsafe.types.UTF8String;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;

public class QuickbooksPartitionReader implements PartitionReader<InternalRow> {
   public QuickbooksPartitionReader(QuickbooksStreamReader reader, SparkSchema schema) {
      this.entitiesIter = reader.getEntities().iterator();
      this.schema = schema;
   }

   @Override
   public boolean next() {
      if(entitiesIter.hasNext()) {
         currObj = entitiesIter.next();
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
   public InternalRow get() {
      return createRow(currObj, schema);
   }

   private InternalRow createRow(Object data, SparkSchema dataSchema) {
      if(data == null || dataSchema == null) {
         return null;
      }

      final List<Object> cells = new ArrayList<>();
      final StructType structType = dataSchema.getStructType();
      StructField[] fields = structType.fields();

      for(StructField structField : fields) {
         // try to get data from object
         final Object result = getObjectByField(data, structField, dataSchema);
         cells.add(result);
      }

      return new GenericInternalRow(cells.toArray());
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
   private Object getObjectByField(Object parentObject,
                                   StructField field,
                                   SparkSchema parentSchema)
   {
      final String name = field.name();
      final String[] tokens = name.split("_");
      final String fieldName = tokens[0];
      final SparkSchema schema = parentSchema.getSchema(fieldName);
      final String methodName = parentSchema.getMethodName(fieldName);
      // get the POJO getter name from the schema
      Object result = callMethodOnObject(parentObject, methodName);

      if(schema != null && schema.isFlattened()) {
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

      if(result == null) {
         return null;
      }

      if(field.dataType() instanceof ArrayType) {
         final ArrayList<Object> childCells = new ArrayList<>();
         final Iterator<?> iterator = ((Collection<?>) result).iterator();
         iterator.forEachRemaining(obj -> childCells.add(createRow(obj, schema)));
         result = ArrayData.toArrayData(childCells.toArray());
      }
      else if(field.dataType() instanceof StructType) {
         // wrap objects in nested rows
         result = createRow(result, schema);
      }
      else if(result instanceof Date) {
         result = ((Date) result).getTime();
      }
      else if(result instanceof Enum) {
         result = UTF8String.fromString(result.toString());
      }
      else if(result instanceof String) {
         result = UTF8String.fromString((String) result);
      }
      else if(result instanceof BigDecimal) {
         result = Decimal.apply((BigDecimal) result);
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

   private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   private Object currObj;
   private final Iterator<Object> entitiesIter;
   private final SparkSchema schema;
}
