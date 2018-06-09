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

import trust.nccgroup.caldum.annotation.DI;
import trust.nccgroup.caldum.annotation.Hook;
import trust.nccgroup.caldum.util.TmpLogger;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HookProcessor {

  private static final Logger logger = TmpLogger.DEFAULT;

  public static ArrayList<DestructingResettableClassFileTransformer> process(
    Instrumentation inst, ClassLoader cl, String scanPrefix
  ) {
    return process(inst, cl, scanPrefix, null, false);
  }


  public static ArrayList<DestructingResettableClassFileTransformer> process(
    Instrumentation inst, ClassLoader cl, String scanPrefix,
    String jarPath, boolean isSystem
  ) {
    if (logger == null) {
      System.err.println("caldum: Failed to initialize logger, exiting.");
      return null;
    }

    if (scanPrefix == null) {
      scanPrefix = "";
    }

    ArrayList<DestructingResettableClassFileTransformer> ret =
      new ArrayList<DestructingResettableClassFileTransformer>(8);

    ArrayList<Class<?>> hooks = new ArrayList<Class<?>>();
    ArrayList<Class<?>> providers = new ArrayList<Class<?>>();
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
        logger.severe(String.format(
          "invalid number of nested classes within %s, must be 1",
          hook.getName()
        ));
        continue;
      }
      Class<?> configClass = nestedClasses[0];

      Class<?> bootstrapHook;
      try {
        bootstrapHook = BootstrapSwapInjector.swapOrInject(hook, inst, isSystem);
      } catch (UnmodifiableClassException e) {
        logger.log(Level.SEVERE, "failed (unmodifiable) to swap/inject class: " + hook.getName(), e);
        continue;
      } catch (IOException e) {
        logger.log(Level.SEVERE, "failed (IO error) to swap/inject class: " + hook.getName(), e);
        continue;
      }

      if (bootstrapHook == null) {
        logger.log(Level.SEVERE, "failed (unknown error) to swap/inject class: " + hook.getName());
        continue;
      }

      for (Field field : hook.getDeclaredFields()) {
        if (!Modifier.isStatic(field.getModifiers())) {
          continue;
        }

        if (!field.isAccessible()) {
          field.setAccessible(true);
        }

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

        if (!nfield.isAccessible()) {
          nfield.setAccessible(true);
        }

        try {
          nfield.set(null, val);
        } catch (IllegalAccessException iae) {
          logger.log(Level.SEVERE, "unable to copy static initialized field", iae);
        }

      }

      DependencyInjection.injectGlobal(bootstrapHook, globalProvides, hook);

      HashMap<String,Object> localProvides = new HashMap<String, Object>();
      for (Class<?> nested : hook.getDeclaredClasses()) {
        localProvides.putAll(DependencyInjection.getProvides(nested));
      }

      DependencyInjection.injectLocal(bootstrapHook, localProvides, hook);

      DestructingResettableClassFileTransformer rcft;
      try {
        rcft = new DestructingResettableClassFileTransformer(PluggableAdviceAgent.Builder.fromClass(configClass).build(inst), bootstrapHook);
        logger.log(Level.INFO, "applied hook for " + configClass.getDeclaringClass().toString());
      } catch (PluggableAdviceAgent.BuildException e) {
        logger.log(Level.SEVERE, "failed to build hook", e);
        continue;
      }
      ret.add(rcft);
    }

    return ret;
  }

}
