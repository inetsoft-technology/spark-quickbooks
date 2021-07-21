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

import inetsoft.spark.quickbooks.QuickbooksDataSourceOptions;
import org.apache.spark.sql.connector.catalog.*;
import org.apache.spark.sql.connector.expressions.Transform;
import org.apache.spark.sql.sources.DataSourceRegister;
import org.apache.spark.sql.types.*;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;

import java.util.*;

public class DefaultSource implements TableProvider, DataSourceRegister {
   @Override
   public String shortName() {
      return "quickbooks";
   }

   @Override
   public StructType inferSchema(CaseInsensitiveStringMap caseInsensitiveStringMap) {
      return getTable(null,
                      new Transform[0],
                      caseInsensitiveStringMap.asCaseSensitiveMap()).schema();
   }

   @Override
   public Table getTable(StructType structType, Transform[] transforms, Map<String, String> map) {
      final QuickbooksDataSourceOptions options = QuickbooksDataSourceOptions.from(map);
      final QuickbooksStreamReader reader = new QuickbooksStreamReader(options);
      return new QuickbooksTable(options, reader);
   }
}
