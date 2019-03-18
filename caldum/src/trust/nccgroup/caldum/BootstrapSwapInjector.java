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

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassInjector;
import trust.nccgroup.caldum.util.TmpDir;
import trust.nccgroup.caldum.util.TmpLogger;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
//import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.ClassLoader.getSystemClassLoader;

public class BootstrapSwapInjector {

  private static final Object lock = new Object();

  public static Class<?> swapOrInject(Class<?> current, Instrumentation inst)
    throws UnmodifiableClassException, IOException {
    return swapOrInject(current, inst, false, null);
  }

  public static Class<?> swapOrInject(Class<?> current, Instrumentation inst, boolean isSystem, Class<?>[] extra)
  throws UnmodifiableClassException, IOException {

    /*System.out.println(">>");
    System.out.println(System.getProperty("java.ext.dirs"));
    System.out.println(System.getProperty("java.class.path"));
    System.out.println(">>");*/

    Class<?> newClass;

    synchronized (lock) {

      try {
        Class<?> old = getSystemClassLoader().loadClass(current.getName());
        if (old.getClassLoader() == null) { // all subsequent attempts
          newClass = __swap(old, current, inst);
        } else { // should never happen for normal users
                 // only if class already in system classloader (e.g. agent)
          if (isSystem) {
            newClass = __inject(current, inst);
          } else {
            // seems to happen in `gradle test`

            // gradle runs the instance w/ a security manager that does weird stuff
            // https://github.com/gradle/gradle/blob/fad5133/subprojects/core/src/main/java/org/gradle/process/internal/worker/child/BootstrapSecurityManager.java
            // doesn't stop anything malicious; if it tried, it has a race condition when it unsets the security manager
            // it uses a custom stdin protocol to pass a (massive) series of file paths in to load into the system
            //   classloader and then updates java.class.path with the total list

            // the crux of the problem is that this new classpath list includes the following:
            //   path/to/mainproject/build/classes/java/main
            // which means that the hook .class files will be loaded first into the core before the url classloader even
            //   initializes

            // due to this, the injected class exists in both the url classloader and the system classloader
            // somehow, the class<?> returned by the instrumentation api ends up being the system classloader one,
            //   even though it was injected into the bootstrap and subsequent lookups will use the bootstrap one
            // the injected class is somehow returned through the system classloader
            // however, at runtime injected hook code still gets it from the bootstrap causing a mismatch on which one
            //  was actually dependency injected into

            // while `gradle test` will recompile them if they are deleted, running a delete loop on them while running
            //   `gradle test` shows that everything will work as normal.
            // this means that this is essentially an edge case that will only happen in gradle test and is not
            //   something any sort of anti-debugging could reasonably hope to do

            // we therefore try to handle this case as gracefully as possible given the circumstances
            // we swap the system classloader version and inject into the bootstrap classloader
            // we then have to handle setting the fields on both, so we need to pass back info that this happened

            Class<?> systemClass = null;
            if (extra != null) {
              systemClass = __swap(old, current, inst);
              extra[0] = systemClass;
            }
            newClass = __inject(current, inst); //bootstrap inject, returns system handle

            /*System.out.println("-------");
            System.out.println(current.getClassLoader());
            System.out.println(newClass);
            System.out.println(newClass.getClassLoader());
            System.out.println(newClass.getClassLoader().loadClass(newClass.getName()));
            System.out.println(newClass.getClassLoader().loadClass(newClass.getName()).getClassLoader());
            System.out.println(newClass.getClassLoader().getParent());
            System.out.println(newClass.getClassLoader().getParent().loadClass(newClass.getName()));
            System.out.println(newClass.getClassLoader().getParent().loadClass(newClass.getName()).getClassLoader());
            Class<?> t = Class.forName(current.getName(), false, null);
            System.out.println(t);
            System.out.println(t.getClassLoader());
            System.out.println("-------");*/

            ClassLoader cl = newClass.getClassLoader();
            if ( cl == ClassLoader.getSystemClassLoader()
              || cl == ClassLoader.getSystemClassLoader().getParent()) {
              try {
                newClass = Class.forName(current.getName(), false, null); // bootstrap
              } catch (ClassNotFoundException ignored) {}
            }
          }
        }
      } catch (ClassNotFoundException cnfe) { // first time
        newClass = __inject(current, inst);
      }

    }

    return newClass;
  }

  public static Class<?> inject(Class<?> current, Instrumentation inst)
    throws IOException {

    synchronized (lock) {

      try {
        Class<?> old = getSystemClassLoader().loadClass(current.getName());
        if (old.getClassLoader() == null) { // all subsequent attempts
          return old;
        } else { // should never happen
          return __inject(current, inst);
          //TmpLogger.DEFAULT.severe("???????? 2: " + current.getName());
          //return null;
        }
      } catch (ClassNotFoundException cnfe) { // first time
        return __inject(current, inst);
      }

    }
  }

  private static Class<?> __swap(Class<?> old, Class<?> newer, Instrumentation inst)
    throws UnmodifiableClassException /*, IOException */ {

//    ClassFileTransformer cft = new BootstrapSwapTransformer(
//      newer.getName(),
//      ClassFileLocator.ForClassLoader.read(newer)
//        .resolve()
//    );
//
//    inst.addTransformer(cft, true);
//    inst.retransformClasses(old);
//    inst.removeTransformer(cft);

    try {
      inst.redefineClasses(
        new ClassDefinition(
          old,
          ClassFileLocator.ForClassLoader.read(newer)
        )
      );
      return getSystemClassLoader().loadClass(newer.getName());
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    return null;
  }


  private static Class<?> __inject(Class<?> current, Instrumentation inst)
    throws IOException {

    TypeDescription td = new TypeDescription.ForLoadedType(current);
    Map<TypeDescription,byte[]> cs = new HashMap<TypeDescription,byte[]>();

    //picks up the dynvars transformation, the default uses the original classfile
    //also worth noting that the injection doesn't go through the standard instrumentation passes
    //  so we can't just apply the modifications there
    cs.put(td, ClassFileLocator.AgentBased.of(inst, current).locate(current.getName()).resolve());
    //cs.put(td, ClassFileLocator.ForClassLoader.read(current));

    File temp = TmpDir.create();
    if (temp == null) {
      throw new IOException("failed to create tmp directory");
    }

    Map<TypeDescription, Class<?>> injected =
      ClassInjector.UsingInstrumentation.of(
        temp,
        ClassInjector.UsingInstrumentation.Target.BOOTSTRAP,
        inst
      ).inject(cs);

    return injected.get(td);
  }
}
