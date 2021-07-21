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

import inetsoft.spark.quickbooks.QuickbooksDataSourceOptions;
import inetsoft.spark.quickbooks.SparkSchema;
import org.apache.spark.sql.connector.catalog.*;
import org.apache.spark.sql.connector.read.*;
import org.apache.spark.sql.types.*;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;

import java.io.Serializable;
import java.util.*;

public class QuickbooksTable implements Table, SupportsRead, Serializable {
   public QuickbooksTable(QuickbooksDataSourceOptions options, QuickbooksStreamReader reader) {
      this.quickbooksOptions = options;
      this.reader = reader;
   }

   @Override
   public ScanBuilder newScanBuilder(CaseInsensitiveStringMap options) {
      return QuickbooksInputScan::new;
   }

   @Override
   public String name() {
      return quickbooksOptions.getCompanyId() + ":" + quickbooksOptions.getEntity();
   }

   @Override
   public StructType schema() {
      if(schema == null) {
         schema = reader.getSchema();
      }

      return schema.getStructType();
   }

   @Override
   public Set<TableCapability> capabilities() {
      return Collections.singleton(TableCapability.BATCH_READ);
   }

   public class QuickbooksInputScan implements Scan, Batch, Serializable {
      @Override
      public Batch toBatch() {
         return this;
      }

      @Override
      public StructType readSchema() {
         if(schema == null) {
            schema = reader.getSchema();
         }

         return schema.getStructType();
      }

      @Override
      public InputPartition[] planInputPartitions() {
         return new InputPartition[]{ new Partition() {} };
      }

      @Override
      public PartitionReaderFactory createReaderFactory() {
         return partition -> new QuickbooksPartitionReader(reader, schema);
      }
   }

   public static class Partition implements InputPartition, Serializable {
   }

   private final QuickbooksStreamReader reader;
   private final QuickbooksDataSourceOptions quickbooksOptions;
   private SparkSchema schema = null;
}
