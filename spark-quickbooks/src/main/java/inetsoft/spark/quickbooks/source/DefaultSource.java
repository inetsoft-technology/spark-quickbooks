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

import org.apache.spark.sql.sources.DataSourceRegister;
import org.apache.spark.sql.sources.v2.*;
import org.apache.spark.sql.sources.v2.reader.DataSourceReader;

public class DefaultSource implements DataSourceV2, ReadSupport, DataSourceRegister {
   @Override
   public String shortName() {
      return "quickbooks";
   }

   @Override
   public DataSourceReader createReader(DataSourceOptions options) {
      return new QuickbooksReader(QuickbooksDataSourceOptions.from(options));
   }
}
