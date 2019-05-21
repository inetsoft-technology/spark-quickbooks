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

import inetsoft.spark.quickbooks.QuickbooksUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Loads the QuickBooks runtime classes for jackson dependency conflicts with spark
 */
public class QuickbooksClassloader extends URLClassLoader {
   private QuickbooksClassloader(URL[] urls, ClassLoader parent) {
      super(urls, parent);
   }

   public static QuickbooksClassloader create(ClassLoader parent)
      throws IOException, URISyntaxException
   {
      final URL location =
         QuickbooksClassloader.class.getProtectionDomain().getCodeSource().getLocation();
      final File quickbooksJar = new File(location.toURI());
      final File libDir = new File(QuickbooksUtil.getQbLibDir());

      if(libDir.mkdir() || libDir.lastModified() < quickbooksJar.lastModified()) {
         final JarFile jarFile = new JarFile(quickbooksJar);
         final ArrayList<JarEntry> entries = Collections.list(jarFile.entries());

         for(JarEntry jarEntry : entries) {
            if(!jarEntry.isDirectory() && jarEntry.getName().startsWith("quickbooks-lib/")) {
               final File file = new File(libDir.getParent(), jarEntry.getName());
               final InputStream jarInputStream = jarFile.getInputStream(jarEntry);

               try(BufferedInputStream in = new BufferedInputStream(jarInputStream);
                   BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file)))
               {
                  int read;

                  while((read = in.read()) != -1) {
                     out.write(read);
                  }
               }
            }
         }

         jarFile.close();
      }

      final File[] files = libDir.listFiles((dir, name) -> name.endsWith(".jar"));

      if(files == null) {
         throw new FileNotFoundException("Could not read the QuickBooks lib folder");
      }

      return new QuickbooksClassloader(Arrays.stream(files)
                                             .filter(Objects::nonNull)
                                             .map(File::toURI)
                                             .map(QuickbooksClassloader::toUrl)
                                             .toArray(URL[]::new), parent);
   }

   @Override
   public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      final URL[] urls = getURLs();

      if(!name.startsWith("org.slf4j") && !name.startsWith("com.sun.xml")) {
         for(URL url : urls) {
            final Class<?> clazz = findClassInJAR(url, name, "", false);

            if(clazz != null) {
               return clazz;
            }
         }
      }

      return super.loadClass(name, resolve);
   }

   private static URL toUrl(URI uri) {
      try {
         return uri.toURL();
      }
      catch(MalformedURLException e) {
         throw new RuntimeException(e);
      }
   }

   private Class findClassInJAR(URL searchURL, String name, String prefix, boolean resolve)
      throws ClassNotFoundException
   {
      if(searchURL != null && name.startsWith(prefix)) {
         String path = name.replace('.', '/').concat(".class");
         URL url = findResource(path);

         if(url != null && "jar".equals(url.getProtocol())) {
            path = url.getFile();
            int index = path.indexOf('!');

            if(index >= 0) {
               path = path.substring(0, index);
            }

            try {
               url = new URL(path);

               if(url.equals(searchURL)) {
                  Class<?> clazz = findClass(name);

                  if(resolve) {
                     resolveClass(clazz);
                  }

                  return clazz;
               }
            }
            catch(MalformedURLException e) {
               LOG.warn("Failed to check JAR file URL", e);
            }
         }
      }

      return null;
   }

   private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
}
