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

import inetsoft.spark.quickbooks.QuickbooksAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;

/**
 * Load the quickbooks runtime and execute a query
 */
public class QuickbooksStreamReader implements Serializable {
   public QuickbooksStreamReader(String clientId,
                                 String clientSecret,
                                 String authorizationCode,
                                 String companyId,
                                 String redirectUrl,
                                 String entity)
   {
      this.clientId = clientId;
      this.clientSecret = clientSecret;
      this.authorizationCode = authorizationCode;
      this.companyId = companyId;
      this.redirectUrl = redirectUrl;
      this.entity = entity;
   }

   public List<Object> getEntities() {
      try {
         final QuickbooksClassloader classLoader =
            QuickbooksClassloader.create(getClass().getClassLoader());
         final Class<?> aClass =
            classLoader.loadClass("inetsoft.spark.quickbooks.QuickbooksRuntime");
         final QuickbooksAPI api = (QuickbooksAPI) aClass.newInstance();
         final QuickbooksAPI.QuickbooksQueryResult result =
            api.loadData(clientId, clientSecret, authorizationCode, companyId, redirectUrl, entity);
         return Collections.unmodifiableList(result.getEntities());
      }
      catch(Exception e) {
         LOG.error("Failed to execute quickbooks query", e);
         throw new RuntimeException(e);
      }
   }

   private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   private final String clientId;
   private final String clientSecret;
   private final String authorizationCode;
   private final String companyId;
   private final String redirectUrl;
   private final String entity;
}
