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
import java.net.URISyntaxException;
import java.net.URL;
//import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

/**
 * Instances of this class are not thread safe.
 * Attempts to parallelize may result in termination.
 *
 *   - The Management
 */
public final class ClassScanner implements Iterable<Class<?>>, Iterator<Class<?>> {

  private ClassLoader ucl;
  private ArrayList<File> as;
  private State s;

  private static class State {
    int idx = 0;
    JarFile jf = null;
    Enumeration<JarEntry> je = null;
  }

  //TODO: migrate to JarInputStream to support embedded JARs
  private ClassScanner(ClassLoader _ucl, HashSet<File> _hs) {
    ucl = _ucl;
    as = new ArrayList<File>(_hs);
    s = new State();

    for (int i=0; i<as.size(); i++) {
      File f = as.get(i);
      if (f.exists()) {
        JarFile jf;
        try {
          jf = new JarFile(f);
        } catch (IOException e) {
          continue;
        }

        s.je = jf.entries();
        s.jf = jf;
        s.idx = i;
        break;
      }
    }

  }

  @Override
  public boolean hasNext() {
    if (s.je.hasMoreElements()) {
      return true;
    }

    if (s.jf == null) {
      return false;
    }

    try {
      s.jf.close();
    } catch (IOException ignored) { }

    for (int i=s.idx; i<as.size(); i++) {
      File f = as.get(i);
      if (f.exists()) {
        JarFile jf;
        try {
          jf = new JarFile(f);
        } catch (IOException e) {
          continue;
        }

        s.je = jf.entries();
        s.jf = jf;
        s.idx = i;
        return hasNext();
      }
    }

    s.idx = as.size();
    s.jf = null;
    s.je = new Enumeration<JarEntry>() {
      @Override
      public boolean hasMoreElements() {
        return false;
      }

      @Override
      public JarEntry nextElement() {
        return null;
      }
    };

    return false;
  }

  @Override
  public Class<?> next() {
    if (!hasNext()) {
      return null;
    }

    while (s.je.hasMoreElements()) {
      JarEntry je = s.je.nextElement();
      if (je.isDirectory() || !je.getName().endsWith(".class")) {
        continue;
      }
      String className = je.getName().substring(0, je.getName().length() - 6);
      className = className.replace('/', '.');

      Class<?> c;
      try {
        c = ucl.loadClass(className);
      } catch (Throwable ignored) {
        //ignored.printStackTrace();
        continue;
      }
      return c;
    }

    if (s.idx == as.size() - 1) {
      s.idx = as.size();
      s.jf = null;
      s.je = new Enumeration<JarEntry>() {
        @Override
        public boolean hasMoreElements() {
          return false;
        }

        @Override
        public JarEntry nextElement() {
          return null;
        }
      };
      return null;
    }

    if (s.idx < as.size()) {
      return next();
    }

    return null;
  }

  @Override
  public void remove() {
    //nope
  }

  @Override
  public Iterator<Class<?>> iterator() {
    return this;
  }

  public static ClassScanner scan(InverseURLClassLoader _ucl) {

    HashSet<File> h = new HashSet<File>();

    for (URL u : _ucl.getURLs()) {
      if (!"file".equals(u.getProtocol())) {
        continue;
      }
      File f;
      try {
        f = new File(u.toURI());
      } catch (URISyntaxException e) {
        f = new File(u.getPath());
      }
      try {
        f = f.getCanonicalFile();
      } catch (IOException e) {
        continue;
      }
      if (h.contains(f)) {
        continue;
      }
      h.add(f);
    }

    return new ClassScanner(_ucl, h);
  }

  public static ClassScanner scan(ClassLoader cl, String jarPath) {

    HashSet<File> h = new HashSet<File>();
    h.add(new File(jarPath));

    return new ClassScanner(cl, h);
  }
}
