/*
Copyright 2018 NCC Group

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package trust.nccgroup.caldum;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

//import org.xeustechnologies.jcl.JarClassLoader;
//note: migrating to support JarClassLoader and its strange behaviors will
//      require a lot of additional reworking:
//      - appears to handle jar closing internally by loading all classes
//      - internal loader used to get classes from loaded URLs is unclear
//      - use of nested JARs works, but the rest of caldum isn't built for it
//      - doesn't allow for setting the parent classloader
//      - will probably want to have 2 separate InverseURLClassLoader
//        implementations, one based on URLcl and the other on JCL, since
//        JCL probably results in some performance impacts against existing
//        tuning and is only needed for embedded agents, which are just for
//        convenience.
//
//      for now, embedded agents will simply be extracted out to a tmp dir


@SuppressWarnings("WeakerAccess")
public class InverseURLClassLoader extends /*JarClassLoader*/ URLClassLoader {

  private final URL[] urls;
  private final Logger logger;
  private static final String error_msg = "Error while attempting to close Java 6 URLClassLoader:";
//  private final ClassLoader parent;

  //note: previously subclassed URLClassLoader, but URLClassLoader
  //      does not support jar: URLs for a JAR within a JAR
  public InverseURLClassLoader(URL[] _urls, ClassLoader _parent, Logger _logger) {
    super(_urls);

//    getSystemLoader().setOrder(3); // system class loader
//    getLocalLoader().setOrder(2);  // local class loader
//    getParentLoader().setOrder(4); // parent class loader
//    getThreadLoader().setOrder(5); // thread context class loader
//    getCurrentLoader().setOrder(1);      // current class loader

//    parent = _parent;

    urls = _urls;
    assert _logger != null;
    logger = _logger;
  }

//  //adding this in since JarClassLoader does not have this
//  public URL[] getURLs() {
//    return urls;
//  }

  @Override
  public Class<?> loadClass(String name) {
    try {
      return super.findClass(name);
      //return super.loadClass(name);
    } catch (ClassNotFoundException e) {
      try {
        return super.loadClass(name);
        //return parent.loadClass(name);
      } catch (ClassNotFoundException e1) {
        //System.out.println("failed to find " + name);
        return null;
      }
    }
  }


  public void java6close() { //via https://stackoverflow.com/a/31114719
    /*if (this.getClass().getSuperclass().getName().equals("org.xeustechnologies.jcl.JarClassLoader")) {
      return;
    }*/

    try {
      Field f__sun_misc_URLClassPath__ucp = URLClassLoader.class.getDeclaredField("ucp");
      f__sun_misc_URLClassPath__ucp.setAccessible(true);

      Object ucp = f__sun_misc_URLClassPath__ucp.get(this);
      Field f__Collection__loaders = ucp.getClass()
                      .getDeclaredField("loaders");
      f__Collection__loaders.setAccessible(true);

      Collection loaders = (Collection) f__Collection__loaders.get(ucp);

      for (Object loader : loaders) { // sun.misc.URLClassPath$JarLoader
        Class<?> loader_class = loader.getClass();
        if (!"sun.misc.URLClassPath$JarLoader".equals(loader_class.getName())) {
          logger.severe("unexpected loader type of " + loader_class.getName());
          continue;
        }

        Field f__JarFile__jar;
        JarFile jf = null;
        try {
          f__JarFile__jar = loader.getClass().getDeclaredField("jar");
          f__JarFile__jar.setAccessible(true);
          jf = (JarFile) f__JarFile__jar.get(loader);
        } catch (IllegalAccessException iae) {
          logger.log(Level.SEVERE, error_msg, iae);
        } catch (NoSuchFieldException nsfe) {
          logger.log(Level.SEVERE, error_msg, nsfe);
        }

        if (jf != null) {
          try {
            jf.close();
          } catch (IOException ioe) {
            logger.log(Level.SEVERE, error_msg, ioe);
          }
        }
      }
    } catch (IllegalAccessException iae) {
      logger.log(Level.SEVERE, error_msg, iae);
    } catch (NoSuchFieldException nsfe) {
      logger.log(Level.SEVERE, error_msg, nsfe);
    }
  }


  public void loadAll() {
    String[] paths = new String[urls.length];
    for (int i=0; i<urls.length; i++) {
      if (!"file".equals(urls[i].getProtocol())) {
        return;
      }
      try {
        paths[i] = urls[i].toURI().getPath();
      } catch (URISyntaxException e) {
        return;
      }
    }

    for (String path : paths) {
      JarFile jarFile;
      try {
        jarFile = new JarFile(new File(path));
      } catch (IOException e) {
        continue;
      }
      Enumeration<JarEntry> e = jarFile.entries();

      while (e.hasMoreElements()) {
        JarEntry je = e.nextElement();
        if (je.isDirectory() || !je.getName().endsWith(".class")) {
          continue;
        }
        String className = je.getName().substring(0, je.getName().length() - 6);
        className = className.replace('/', '.');
        try {
          this.loadClass(className);
        } catch (Throwable ignored) { }
      }
    }
  }

}
