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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.*;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles generating a spark {@link StructType} schema from a POJO
 */
public class SparkSchemaGenerator {
   /**
    * Generate the spark schema for a list of objects
    */
   public SparkSchema generateSchema(Object... entities) {
      SparkSchema sparkSchema = new SparkSchema();

      for(Object entity : entities) {
         StructType structType = sparkSchema.getStructType();

         final List<PropertyDescriptor> properties = getPropertyDescriptors(entity.getClass());

         for(PropertyDescriptor property : properties) {
            final String propertyName = property.getName();
            final Class<?> propertyType = property.getPropertyType();
            final Method readMethod = property.getReadMethod();

            if(readMethod != null) {
               StructField field;

               try {
                  field = createStructField(propertyName, readMethod.invoke(entity), propertyType, sparkSchema);
               }
               catch(IllegalAccessException | InvocationTargetException e) {
                  LOG.error("Unable to access object properties", e);
                  return null;
               }

               StructType s = new StructType();
               s = s.add(field);
               structType = structType.merge(s);
               sparkSchema.setStructType(structType);
               sparkSchema.setMethodName(propertyName, readMethod.getName());
               LOG.debug(structType.toString());
            }
         }
      }

      return sparkSchema;
   }

   /**
    * Create a StructField from a POJO property
    *
    * @param propertyName  the name of the property
    * @param propertyValue the value of the property
    * @param propertyType  the class of the property
    *
    * @return a StructField that represents this POJO property
    */
   private StructField createStructField(String propertyName,
                                         Object propertyValue,
                                         Class<?> propertyType,
                                         SparkSchema schema)
   {
      final StructField field;
      final DataType type = getDataTypeFromClass(propertyType);

      // primitive or primitive wrapper
      if(!type.sameType(DataTypes.BinaryType)) {
         field = new StructField(propertyName, type, true, Metadata.empty());
         schema.addSchema(propertyName, new SparkSchema());
      }
      // object
      else if(propertyValue != null && !propertyType.isEnum()) {
         if(Collection.class.isAssignableFrom(propertyType)) {
            field = createArrayField(propertyName, (Collection) propertyValue, schema);
         }
         else if(propertyType.isArray()) {
            field = createArrayField(propertyName, Arrays.asList(propertyValue), schema);
         }
         else {
            final SparkSchema nestedSchema = generateSchema(propertyValue);
            final StructType structType = nestedSchema.getStructType();
            schema.addSchema(propertyName, nestedSchema);
            field = new StructField(propertyName, structType, true, Metadata.empty());
         }
      }
      // null
      else {
         field = new StructField(propertyName, new StructType(), true, Metadata.empty());
      }

      return field;
   }

   private StructField createArrayField(String propertyName, Collection children, SparkSchema schema) {
      StructField field;
      final SparkSchema sparkSchema = generateSchema(children.toArray());
      schema.addSchema(propertyName, sparkSchema);
      sparkSchema.setArraySize(children.size());
      final StructType complexStructType = sparkSchema.getStructType();
      field = new StructField(propertyName,
                              DataTypes.createArrayType(complexStructType, true),
                              true,
                              Metadata.empty());
      return field;
   }

   private List<PropertyDescriptor> getPropertyDescriptors(Class<?> clazz) {
      try {
         BeanInfo beanInfo = Introspector.getBeanInfo(clazz, Object.class);
         final PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();

         if(propertyDescriptors != null) {
            return Arrays.stream(propertyDescriptors)
                         .filter(Objects::nonNull)
                         .collect(Collectors.toList());
         }
      }
      catch(IntrospectionException e) {
         LOG.error("Failed to generate schema", e);
      }

      return Collections.emptyList();
   }

   /**
    * From https://spark.apache.org/docs/latest/sql-reference.html, translate simple class name
    * to Spark DataType
    */
   private static DataType getDataTypeFromClass(Class clazz) {
      final String typeName = clazz.getSimpleName();

      switch(typeName) {
         case "byte":
         case "Byte":
            return DataTypes.ByteType;
         case "short":
         case "Short":
            return DataTypes.ShortType;
         case "int":
         case "Integer":
            return DataTypes.IntegerType;
         case "long":
         case "Long":
            return DataTypes.LongType;
         case "float":
         case "Float":
            return DataTypes.FloatType;
         case "double":
         case "Double":
            return DataTypes.DoubleType;
         case "BigDecimal":
            return DataTypes.createDecimalType();
         case "String":
            return DataTypes.StringType;
         case "boolean":
         case "Boolean":
            return DataTypes.BooleanType;
         case "Date":
            return DataTypes.DateType;
         case "Timestamp":
            return DataTypes.TimestampType;
         case "Object":
            return DataTypes.BinaryType;
         default:
            LOG.debug("Using BinaryType for [{}]", typeName);
            return DataTypes.BinaryType;
      }
   }

   private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
}