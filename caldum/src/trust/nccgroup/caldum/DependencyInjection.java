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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public class DependencyInjection {

  public static HashMap<String,Object> getProvides(Class<?> provider) {

    HashMap<String,Object> ret = new HashMap<String, Object>(16);

    Field[] fields = provider.getDeclaredFields();
    Method[] methods = provider.getDeclaredMethods();

    for (Field field : fields) {
      if (!Modifier.isStatic(field.getModifiers())) {
        continue;
      }

      if (!field.isAccessible()) {
        field.setAccessible(true);
      }

      DI.Provide p = field.getAnnotation(DI.Provide.class);
      if (p == null) {
        continue;
      }

      Object value;
      try {
        value = field.get(null);
      } catch (IllegalAccessException e) {
        continue;
      }

      if (value == null) {
        continue;
      }

      String name = p.name();
      if ("".equals(name)) {
        name = field.getName();
      }

      ret.put(name, value);
    }

    for (Method method : methods) {
      if (!Modifier.isStatic(method.getModifiers())) {
        continue;
      }

      if (!method.isAccessible()) {
        method.setAccessible(true);
      }

      DI.Provide p = method.getAnnotation(DI.Provide.class);
      if (p == null) {
        continue;
      }

      HashMap<String,Object> valuemap = new HashMap<String, Object>(8);
      try {
        Object rawvals = method.invoke(null);
        if (rawvals instanceof HashMap) {
          HashMap vals = (HashMap)rawvals;
          for(Object o : vals.keySet()) {
            if (o instanceof String) {
              String s = (String)o;
              valuemap.put(s, vals.get(o));
            }
          }
        }
      } catch (IllegalAccessException e) {
        continue;
      } catch (InvocationTargetException e) {
        continue;
      }

      ret.putAll(valuemap);
    }

    return ret;
  }

  public static void injectLocal(Class<?> target, HashMap<String,Object> vals, Class<?> full) {
    Field[] fields = full.getDeclaredFields();
    for (Field field : fields) {
      if (!Modifier.isStatic(field.getModifiers())) {
        continue;
      }

      if (!field.isAccessible()) {
        field.setAccessible(true);
      }

      if (field.getAnnotation(DI.AgentClassLoader.class) != null) {
        try {
          Field tfield;
          try {
            tfield = target.getDeclaredField(field.getName());
          } catch (NoSuchFieldException e) {
            continue;
          }

          tfield.set(null, full.getClassLoader());
        } catch (IllegalAccessException ignored) { }
        continue;
      }

      if (field.getAnnotation(DI.Inject.class) != null) {
        continue;
      }

      Object val = vals.get(field.getName());
      if (val == null) {
        continue;
      }
      try {
        Field tfield;
        try {
          tfield = target.getDeclaredField(field.getName());
        } catch (NoSuchFieldException e) {
          continue;
        }

        tfield.set(null, val);
      } catch (IllegalAccessException ignored) { }
    }
  }

  public static void injectGlobal(Class<?> target, HashMap<String,Object> vals, Class<?> full) {
    Field[] fields = full.getDeclaredFields();
    for (Field field : fields) {
      if (!Modifier.isStatic(field.getModifiers())) {
        continue;
      }

      if (!field.isAccessible()) {
        field.setAccessible(true);
      }

      if (field.getAnnotation(DI.Inject.class) == null) {
        continue;
      }

      Object val = vals.get(field.getName());
      if (val == null) {
        continue;
      }

      try {
        Field tfield;
        try {
          tfield = target.getDeclaredField(field.getName());
        } catch (NoSuchFieldException e) {
          continue;
        }

        tfield.set(null, val);
      } catch (IllegalAccessException ignored) { }
    }
  }


}
