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

import net.bytebuddy.dynamic.ClassFileLocator;
import trust.nccgroup.caldum.annotation.DI;
import trust.nccgroup.caldum.annotation.Dump;
import trust.nccgroup.caldum.annotation.Hook;
import trust.nccgroup.caldum.util.CompatHelper;
import trust.nccgroup.caldum.util.Dumper;
import trust.nccgroup.caldum.util.TmpLogger;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.ClassLoader.getSystemClassLoader;
import static trust.nccgroup.caldum.asm.DynamicFields.DYNVARS;

public final class HookProcessor {

  public static Instrumentation inst;
  private static final Logger logger = TmpLogger.DEFAULT;

  public static ArrayList<DestructingResettableClassFileTransformer> process(
    Instrumentation _inst, ClassLoader cl, String scanPrefix
  ) {
    return process(_inst, cl, scanPrefix, null, false);
  }

  public static ArrayList<DestructingResettableClassFileTransformer> process(
    Instrumentation _inst, ClassLoader cl, String scanPrefix,
    String jarPath, boolean isSystem
  ) {
    inst = _inst;
    if (logger == null) {
      System.err.println("caldum: Failed to initialize logger, exiting.");
      return null;
    }

    if (scanPrefix == null) {
      scanPrefix = "";
    }

    ArrayList<DestructingResettableClassFileTransformer> ret =
      new ArrayList<DestructingResettableClassFileTransformer>(8);

    ArrayList<Class<?>> hooks = new ArrayList<Class<?>>(8);
    ArrayList<Class<?>> providers = new ArrayList<Class<?>>(8);
    HashMap<String,Object> globalProvides = new HashMap<String, Object>(16);

    Iterable<Class<?>> cs;
    if (cl instanceof InverseURLClassLoader) {
      cs = ClassScanner.scan((InverseURLClassLoader)cl);
    } else if (jarPath != null) {
      cs = ClassScanner.scan(cl, jarPath);
    } else if (cl != null) {
      cs = Arrays.asList((Class<?>[])inst.getInitiatedClasses(cl));
    } else {
      cs = Arrays.asList((Class<?>[])inst.getAllLoadedClasses());
    }

    for (Class<?> c : cs) {
      if (c == null) {
        continue;
      }

      if (!c.getName().startsWith(scanPrefix)) {
        continue;
      }
      if (c.getAnnotation(Hook.class) != null) {
        hooks.add(c);
      } else if (c.getAnnotation(DI.Provider.class) != null) {
        providers.add(c);
      }
    }

    for (Class<?> provider : providers) {
      globalProvides.putAll(DependencyInjection.getProvides(provider));
    }

    for (Class<?> hook : hooks) {
      Class<?>[] nestedClasses = hook.getDeclaredClasses();
      if (nestedClasses.length != 1) {
        logger.log(Level.SEVERE, String.format(
          "invalid number of nested classes within %s, must be 1",
          hook.getName()
        ));
        continue;
      }
      Class<?> configClass = nestedClasses[0];

      Class<?> alreadyInjectedClass = null;
      try {
        Class<?> old = getSystemClassLoader().loadClass(hook.getName());
        if (old.getClassLoader() == null) {
          alreadyInjectedClass = old;
//          logger.log(Level.INFO, "old: " + old);
//          logger.log(Level.INFO, "old.getClassLoader(): " + old.getClassLoader());
        }
      } catch (ClassNotFoundException cnfe) {
        // don't add dynvar instrumentation since it's the first time // ???
      }

      Class<?> injectedhooks[] = new Class<?>[]{null,null};
      try {
//        logger.log(Level.INFO, "hook class fields: " + hook.getName());
//        int counter = 0;
//        for (Field f : hook.getDeclaredFields()) {
//          counter += 1;
//          logger.log(Level.INFO, "" + counter + " - " + f.toString());
//        }
//        Dumper.dumpClass(inst, hook, "./hook." + hook.getName() + ".class");

        injectedhooks[1] = BootstrapSwapInjector.swapOrInject(hook, inst, isSystem, injectedhooks);
      } catch (UnmodifiableClassException e) {
        logger.log(Level.SEVERE, "failed (unmodifiable) to swap/inject class: " + hook.getName(), e);
        continue;
      } catch (IOException e) {
        logger.log(Level.SEVERE, "failed (IO error) to swap/inject class: " + hook.getName(), e);
        continue;
      } catch (Throwable t) {
        logger.log(Level.SEVERE, "??? " + hook.getName());
        logger.log(Level.SEVERE, "??? failed to swap/inject class: " + hook.getName(), t);
        if (t instanceof UnsupportedOperationException) {
          logger.log(Level.SEVERE, "failed class fields: " + hook.getName());
          int counter = 0;
          for (Field f : hook.getDeclaredFields()) {
            counter += 1;
            logger.log(Level.SEVERE, "" + counter + " - " + f.toString());
          }

          try {
            Class<?> old = getSystemClassLoader().getParent().loadClass(hook.getName());
            if (old.getClassLoader() == null) {
              logger.log(Level.SEVERE, "currently loaded class: " + old);
              counter = 0;
              for (Field f : old.getDeclaredFields()) {
                counter += 1;
                logger.log(Level.SEVERE, "" + counter + " - " + f.toString());
              }
              Dumper.dumpClass(inst, old, "./old." + old.getName() + ".class");

            } else {
              logger.log(Level.SEVERE, "found in other classloader? " + old.toString());
            }
          } catch (ClassNotFoundException ignore) {
            logger.log(Level.SEVERE, "not found in bootstrap classloader");
          }
        }
        throw new RuntimeException(t);
      }
      logger.log(Level.INFO, "\"successfully\" (w/o exception) swap/injected: " + hook);

      if (injectedhooks[1] == null) {
        logger.log(Level.SEVERE, "failed (unknown error) to swap/inject class: " + hook.getName());
        continue;
      } else {
        logger.log(Level.INFO, "loaded class: " + hook.getName());
        for (Field f : hook.getDeclaredFields()) {
          logger.log(Level.INFO, "- " + f.toString());
        }
        logger.log(Level.INFO, "swapped/injected class: " + injectedhooks[1].getName());
        for (Field f : injectedhooks[1].getDeclaredFields()) {
          logger.log(Level.INFO, "- " + f.toString());
        }
      }

      HashMap<String,Object> localProvides = new HashMap<String, Object>();
      for (Class<?> nested : hook.getDeclaredClasses()) {
        localProvides.putAll(DependencyInjection.getProvides(nested));
      }

      for (Class<?> injectedhook : injectedhooks) {
        if (injectedhook == null) {
          continue;
        }

        //initDynVars(hook);
        copyFields(hook, injectedhook);
        DependencyInjection.injectGlobal(injectedhook, globalProvides, hook);
        DependencyInjection.injectLocal(injectedhook, localProvides, hook);

        if (injectedhook == injectedhooks[0]) {
          // yes, ==, we only want to set up the fields on the classpath version
          // we don't want to cause the hooks to be applied twice
          continue;
        }

        DestructingResettableClassFileTransformer rcft;
        try {
          rcft = new DestructingResettableClassFileTransformer(
            PluggableAdviceAgent.Builder.fromClass(configClass).build(inst, alreadyInjectedClass),
            injectedhook
          );
          logger.log(Level.INFO, "applied hook for " + configClass.getDeclaringClass().toString());
        } catch (PluggableAdviceAgent.BuildException e) {
          logger.log(Level.SEVERE, "failed to build hook", e);
          continue;
        }
        ret.add(rcft);
      }
    }
    return ret;
  }

  private static void initDynVars(Class<?> hook) {
    try {
      // the injected version will get initialized from this later
      Map<String,Object> m = new HashMap<String,Object>();
      Field dynvars = hook.getDeclaredField(DYNVARS);
      dynvars.set(null, m);
    } catch (NoSuchFieldException ignored) { }
      catch (IllegalAccessException ignored) { }
  }

  @SuppressWarnings("unchecked")
  private static void copyFields(Class<?> hook, Class<?> bootstrapHook) {
    Map<String,Object> dynvars = null;
    try {
      Field dynvars_field = hook.getDeclaredField(DYNVARS);
      dynvars = (Map<String,Object>)dynvars_field.get(null);
    } catch (NoSuchFieldException ignored) { }
    catch (IllegalAccessException ignored) { }

    for (Field field : hook.getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers())) {
        continue;
      }

//      if (!field.isAccessible()) {
//        field.setAccessible(true);
//      }
      CompatHelper.trySetAccessible(field);

      Object val;
      try {
        val = field.get(null);
      } catch (IllegalAccessException iae) {
        logger.log(Level.SEVERE, "unable to copy static initialized field", iae);
        continue;
      }

      if (val == null) {
        continue;
      }

      Field nfield;
      try {
        nfield = bootstrapHook.getDeclaredField(field.getName());
      } catch (NoSuchFieldException nsfe) {
        logger.log(Level.SEVERE, "unable to copy static initialized field", nsfe);
        continue;
      }

//      if (!nfield.isAccessible()) {
//        nfield.setAccessible(true);
//      }
      CompatHelper.trySetAccessible(field);

      try {
        nfield.set(null, val);
        if (!DYNVARS.equals(field.getName())) {
          if (dynvars != null) {
            dynvars.put(field.getName(), val);
          }
        }
      } catch (IllegalAccessException iae) {
        logger.log(Level.SEVERE, "unable to copy static initialized field", iae);
      }
    }

  }
}
