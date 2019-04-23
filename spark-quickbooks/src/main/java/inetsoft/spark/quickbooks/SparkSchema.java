/*
 * Copyright (c) 2019, InetSoft Technology Corp, All Rights Reserved.
 *
 * The software and information contained herein are copyrighted and
 * proprietary to InetSoft Technology Corp. This software is furnished
 * pursuant to a written license agreement and may be used, copied,
 * transmitted, and stored only in accordance with the terms of such
 * license and with the inclusion of the above copyright notice. Please
 * refer to the file "COPYRIGHT" for further copyright and licensing
 * information. This software and information or any other copies
 * thereof may not be provided or otherwise made available to any other
 * person.
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
