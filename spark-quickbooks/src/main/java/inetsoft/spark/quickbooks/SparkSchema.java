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

import org.apache.spark.sql.types.*;

import java.io.Serializable;
import java.util.*;

/**
 * Wrapper to add extra information on top of the spark schema
 */
public class SparkSchema implements Serializable {
   public SparkSchema() {
      structType = new StructType();
      methodNames = new HashMap<>();
      schemas = new HashMap<>();
   }

   public SparkSchema flatten(boolean expandArrays) {
      final StructType flat = flatten("", schemas, expandArrays);
      structType = structType.merge(flat);
      return this;
   }

   public StructType flatten(String parent, Map<String, SparkSchema> schemas, boolean expand) {
      final List<StructField> structFields = new ArrayList<>();
      final Map<String, SparkSchema> newSchemas = new HashMap<>();

      for(Map.Entry<String, SparkSchema> entry : schemas.entrySet()) {
         final String key = entry.getKey();
         final SparkSchema schema = entry.getValue();
         String prefixedKey = parent + key;

         if(structType.nonEmpty()) {
            schema.flattened = true;
            final int schemaArraySize = schema.getArraySize();

            if(schemaArraySize == 0) {
               flattenStruct(schemas, structFields, newSchemas, key, schema, prefixedKey, expand);
            }
            else if(expand){
               for(int i = 0; i < schemaArraySize; i++) {
                  final String newKeyName = prefixedKey + "_" + i;
                  flattenStruct(schemas, structFields, newSchemas, key, schema, newKeyName, expand);
               }
            }
         }
      }

      schemas.putAll(newSchemas);
      return DataTypes.createStructType(structFields);
   }

   /**
    * Recursively flatten nested objects to '_' delimited key/value pairs
    *  @param schemas      The schema of the object to flatten
    * @param structFields a list of the flattened fields to add to our parent struct
    * @param newSchemas   a mapping back to the original schema for the nested field
    * @param key          the original name of the field
    * @param schema       the schema of the object we're flattening
    * @param newKeyName   the new flattened name of for the field
    * @param expand       true to expand arrays to columns
    */
   private void flattenStruct(Map<String, SparkSchema> schemas,
                              List<StructField> structFields,
                              Map<String, SparkSchema> newSchemas,
                              String key,
                              SparkSchema schema,
                              String newKeyName, boolean expand)
   {
      final Map<String, SparkSchema> originalSchemas = schema.schemas;
      final String parent = newKeyName + "_";
      final StructType newStructType = schema.flatten(parent, new HashMap<>(originalSchemas), expand);

      if(newStructType.nonEmpty()) {
         for(StructField field : newStructType.fields()) {
            structFields.add(field);
            newSchemas.put(field.name(), schemas.get(key));
         }
      }
      else {
         final int fieldIndex = structType.fieldIndex(key);
         final StructField type = structType.apply(fieldIndex);
         final StructField structField =
            new StructField(newKeyName, type.dataType(), type.nullable(), type.metadata());
         structFields.add(structField);
      }
   }

   public void setMethodName(String fieldName, String methodName) {
      methodNames.put(fieldName, methodName);
   }

   public String getMethodName(String fieldName) {
      return methodNames.get(fieldName);
   }

   public void setStructType(StructType structType) {
      this.structType = structType;
   }

   public StructType getStructType() {
      return structType;
   }

   public void addSchema(String fieldName, SparkSchema schema) {
      schemas.merge(fieldName, schema, SparkSchema::merge);
   }

   public SparkSchema getSchema(String fieldName) {
      return schemas.get(fieldName);
   }

   public int getArraySize() {
      return arraySize;
   }

   public void setArraySize(int arraySize) {
      this.arraySize = arraySize;
   }

   /**
    * @return if this schema represents a flattened field on the root entity
    */
   public boolean isFlattened() {
      return flattened;
   }

   private static SparkSchema merge(SparkSchema a, SparkSchema b) {
      final SparkSchema newSchema = new SparkSchema();
      newSchema.structType = a.structType.merge(b.structType);
      newSchema.methodNames.putAll(a.methodNames);
      newSchema.methodNames.putAll(b.methodNames);
      newSchema.schemas.putAll(a.schemas);
      newSchema.setArraySize(Math.max(a.arraySize, b.arraySize));
      assert a.flattened == b.flattened : "Trying to merge flattened struct with nested struct";
      newSchema.flattened = a.flattened;

      for(Map.Entry<String, SparkSchema> entry : b.schemas.entrySet()) {
         newSchema.schemas.merge(entry.getKey(), entry.getValue(), SparkSchema::merge);
      }

      return newSchema;
   }

   private StructType structType;
   private final Map<String, String> methodNames;
   private final Map<String, SparkSchema> schemas;
   private boolean flattened = false;
   private int arraySize = 0;
}
