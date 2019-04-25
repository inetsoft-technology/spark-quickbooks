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

import org.apache.spark.sql.types.StructType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper to add extra information on top of the spark schema
 */
public class SparkSchema implements Serializable {
   public SparkSchema() {
      structType = new StructType();
      methodNames = new HashMap<>();
      structTypes = new HashMap<>();
      schemas = new HashMap<>();
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

   public void setStructType(String field, StructType structType) {
      structTypes.put(field, structType);
   }

   public StructType getStructType(String field) {
      return structTypes.get(field);
   }

   public void addSchema(String fieldName, SparkSchema schema) {
      schemas.merge(fieldName, schema, SparkSchema::merge);
   }

   public SparkSchema getSchema(String fieldName) {
      return schemas.get(fieldName);
   }

   private static SparkSchema merge(SparkSchema a, SparkSchema b) {
      final SparkSchema newSchema = new SparkSchema();
      newSchema.structType = a.structType.merge(b.structType);
      newSchema.methodNames.putAll(a.methodNames);
      newSchema.methodNames.putAll(b.methodNames);
      newSchema.schemas.putAll(a.schemas);

      for(Map.Entry<String, SparkSchema> entry : b.schemas.entrySet()) {
         newSchema.schemas.merge(entry.getKey(), entry.getValue(), SparkSchema::merge);
      }

      newSchema.structTypes.putAll(a.structTypes);

      for(Map.Entry<String, StructType> entry : b.structTypes.entrySet()) {
         newSchema.structTypes.merge(entry.getKey(), entry.getValue(), StructType::merge);
      }

      return newSchema;
   }

   private StructType structType;
   private final Map<String, String> methodNames;
   private final Map<String, StructType> structTypes;
   private final Map<String, SparkSchema> schemas;
}
