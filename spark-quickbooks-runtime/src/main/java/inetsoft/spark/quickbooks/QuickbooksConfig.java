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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;

/**
 * Manage the storage of the Quickbooks OAuth tokens
 */
public class QuickbooksConfig {
   private QuickbooksConfig() {
      this(null, null, -1);
   }

   private QuickbooksConfig(String accessToken,
                            String refreshToken,
                            long expiration)
   {
      this.accessToken = accessToken;
      this.refreshToken = refreshToken;
      this.expiration = expiration;
   }

   public static String getQbLibDir() {
      return qbLibDir;
   }

   public static QuickbooksConfig readConfig(String clientId, String companyId) {
      final File file = getConfigFile(clientId, companyId);

      try(FileReader reader = new FileReader(file)) {
         final BufferedReader bufferedReader = new BufferedReader(reader);
         final String accessToken = bufferedReader.readLine();
         final String refreshToken = bufferedReader.readLine();
         long expiration = Long.parseLong(bufferedReader.readLine());
         return new QuickbooksConfig(accessToken, refreshToken, expiration);
      }
      catch(NumberFormatException | IOException e) {
         LOG.info("Could not read configuration file. Generating new tokens");

         if(file.exists() && !file.delete()) {
            LOG.warn("Configuration file could not be removed");
         }
      }

      return new QuickbooksConfig();
   }

   private static File getConfigFile(String clientId, String companyId) {
      LOG.info("Reading OAuth config file from {}", qbLibDir);
      return new File(qbLibDir, clientId + companyId);
   }

   public QuickbooksConfig updateCredentials(long expiresIn, String accessToken, String refreshToken, String clientId,
                                             String companyId)
   {
      final long expiration = expiresIn * 1000 + System.currentTimeMillis();
      final QuickbooksConfig newConfig = new QuickbooksConfig(accessToken, refreshToken,
                                                              expiration);
      return newConfig.saveConfig(clientId, companyId);
   }

   public String getAccessToken() {
      return accessToken;
   }

   public String getRefreshToken() {
      return refreshToken;
   }

   public long getExpiration() {
      return expiration;
   }

   private QuickbooksConfig saveConfig(String clientId, String companyId) {
      final File configFile = getConfigFile(clientId, companyId);

      try(PrintWriter writer = new PrintWriter(configFile)) {
         writer.println(accessToken);
         writer.println(refreshToken);
         writer.println(expiration);
      }
      catch(IOException e) {
         LOG.error("Could not save OAuth configuration. Generate a new authorization code");
         throw new RuntimeException(e);
      }

      return this;
   }

   private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   private static final String qbLibDir;

   static {
      final String envLib = System.getenv("QUICKBOOKS_LIB");
      qbLibDir = (envLib != null) ? envLib : (System.getProperty("user.home") + "/quickbooks-lib");
   }

   private final String accessToken;
   private final String refreshToken;
   private final long expiration;
}
