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

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import trust.nccgroup.caldum.util.CompatHelper;
import trust.nccgroup.caldum.util.TmpLogger;

import java.io.Closeable;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DestructingResettableClassFileTransformer {

  private ResettableClassFileTransformer rcft;
  private Class<?> loaded;

  private static Class<?> autoclosable;

  private static final Logger logger = TmpLogger.DEFAULT;

  static {
    try {
      autoclosable = Class.forName("java.lang.AutoCloseable");
    } catch (ClassNotFoundException e) {
      autoclosable = null;
    }
  }

  public DestructingResettableClassFileTransformer(ResettableClassFileTransformer _rcft, Class<?> _loaded) {
    rcft = _rcft;
    loaded = _loaded;
  }

  boolean reset(Instrumentation inst, AgentBuilder.RedefinitionStrategy strat) {
    Field[] fields = loaded.getDeclaredFields();
    for (Field field : fields) {
      if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
        continue;
      }

//      if (!field.isAccessible()) {
//        field.setAccessible(true);
//      }
      CompatHelper.trySetAccessible(field);

      if (field.getType().equals(Logger.class)) {
        try {
          Logger l = (Logger)field.get(null);
          for (Handler h : l.getHandlers()) {
            h.close();
          }
        } catch (IllegalAccessException e) {
          continue;
        }
      } else if (Closeable.class.isAssignableFrom(field.getType())) {
        Closeable c;
        try {
          c = (Closeable)field.get(null);
        } catch (IllegalAccessException e) {
          continue;
        }
        try {
          c.close();
        } catch (IOException ignore) { }
      } else if (autoclosable != null && autoclosable.isAssignableFrom(field.getType())) {
        Object ac;
        try {
          ac = field.get(null);
        } catch (IllegalAccessException e) { continue; }
        try {
          Method close = ac.getClass().getMethod("close");
          close.invoke(ac);
        } catch (NoSuchMethodException ignored) {
          //pass
        } catch (IllegalAccessException ignored) {
          //pass
        } catch (InvocationTargetException ignored) {
          //pass
        }
      }

      try {
        field.set(null, null);
      } catch (IllegalAccessException ignored) { }
        catch (IllegalArgumentException ignored) { }
    }

    boolean ret = rcft.reset(inst, strat);
    if (!ret) {
      logger.log(Level.SEVERE, "reset failed: loaded: " + loaded + ", rcft: " + rcft);
    }
    return ret;
  }


}
