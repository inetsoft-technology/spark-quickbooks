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
package inetsoft.spark.quickbooks.source;

import org.apache.spark.sql.sources.v2.*;
import org.apache.spark.sql.sources.v2.reader.DataSourceReader;

public class DefaultSource implements DataSourceV2, ReadSupport {
   @Override
   public DataSourceReader createReader(DataSourceOptions options) {
      final String clientId = options.get("clientId").orElse("");
      final String clientSecret = options.get("clientSecret").orElse("");
      final String authorizationCode = options.get("authorizationCode").orElse("");
      final String companyId = options.get("companyId").orElse("");
      final String redirectUrl = options.get("redirectUrl").orElse(URL);
      final String entity = options.get("entity").orElse("companyInfo");
      return new QuickbooksReader(clientId, clientSecret, authorizationCode,
                                  companyId, redirectUrl, entity);
   }

   private static final String URL = "https://developer.intuit.com/v2/OAuth2Playground/RedirectUrl";
}
