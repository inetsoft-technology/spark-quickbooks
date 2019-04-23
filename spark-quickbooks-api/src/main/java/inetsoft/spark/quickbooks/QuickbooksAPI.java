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

import java.util.List;

public interface QuickbooksAPI {
   QuickbooksQueryResult loadData(String clientId,
                                  String clientSecret,
                                  String authorizationCode,
                                  String companyID,
                                  String redirectUrl,
                                  String entity);

   interface QuickbooksQueryResult {
      List<Object> getEntities();
      int getStartPosition();
      int getMaxResults();
      int getTotalCount();
   }
}
