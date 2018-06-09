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
    return swapOrInject(current, inst, false);
  }

  public static Class<?> swapOrInject(Class<?> current, Instrumentation inst, boolean isSystem)
  throws UnmodifiableClassException, IOException {

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
            return null;
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
          return null;
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
          ClassFileLocator.ForClassLoader.read(newer).resolve()
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

    cs.put(td, ClassFileLocator.ForClassLoader.read(current).resolve());

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
