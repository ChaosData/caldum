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

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import trust.nccgroup.caldum.annotation.DI;
import trust.nccgroup.caldum.util.CompatHelper;
import trust.nccgroup.caldum.util.TmpLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static trust.nccgroup.caldum.asm.DynamicFields.DYNANNOS;
import static trust.nccgroup.caldum.asm.DynamicFields.DYNVARS;

public class DependencyInjection {
  private static final Logger logger = TmpLogger.DEFAULT;

  public static HashMap<String,Object> getProvides(Class<?> provider) {

    HashMap<String,Object> ret = new HashMap<String, Object>(16);

    Field[] fields = provider.getDeclaredFields();
    Method[] methods = provider.getDeclaredMethods();

    for (Field field : fields) {
      if (!Modifier.isStatic(field.getModifiers())) {
        continue;
      }

//      if (!field.isAccessible()) {
//        field.setAccessible(true);
//      }
      CompatHelper.trySetAccessible(field);


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

//      if (!method.isAccessible()) {
//        method.setAccessible(true);
//      }
      CompatHelper.trySetAccessible(method);


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

  @SuppressWarnings({"unchecked","Duplicates"})
  public static void injectLocal(Class<?> target, HashMap<String,Object> vals, Class<?> injected) {
    //Field[] fields = full.getDeclaredFields();
    Field[] fields = target.getDeclaredFields();
    for (Field field : fields) {
      if (!Modifier.isStatic(field.getModifiers())) {
        continue;
      }

      CompatHelper.trySetAccessible(field);

      if (field.getAnnotation(DI.AgentClassLoader.class) != null) {
        try {
          Field tfield;
          try {
            tfield = target.getDeclaredField(field.getName());
          } catch (NoSuchFieldException e) {
            continue;
          }

          tfield.set(null, injected.getClassLoader());
        } catch (IllegalAccessException ignored) { }
        continue;
      } else if (field.getAnnotation(DI.Inject.class) != null) {
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

    Map<String, AnnotationList> annomap = DynVarsAgent.getAnnomap(target.getName());
    if (annomap == null) {
      //not dynvars
    } else {
      Map<String,Object> dynvars = null;
      try {
        dynvars = (Map<String,Object>)target.getDeclaredField(DYNVARS).get(null);
      } catch (Throwable t) {
        return;
      }
      for (String fieldname : annomap.keySet()) {
        AnnotationList annos = annomap.get(fieldname);
        if (annos == null) {
          continue;
        }

        for (AnnotationDescription anno : annos) {
          TypeDescription atd = anno.getAnnotationType();
          String annoname = atd.getActualName();
          if (DI.Inject.class.getName().equals(annoname)) {
            Object val = vals.get(fieldname);
            if (val != null) {
              dynvars.put(fieldname, val);
            }
          } else if (DI.AgentClassLoader.class.getName().equals(annoname)) {
            dynvars.put(fieldname, injected.getClassLoader());
          }
        }
      }
    }

//    Map<String, Annotation[]> annomap2 = null;
//    try {
//      annomap2 = (Map<String, Annotation[]>)target.getDeclaredField(DYNANNOS).get(null);
//    } catch (NoSuchFieldException e) {
//      logger.log(Level.SEVERE, "nfe on " + target, e);
//    } catch (IllegalAccessException e) {
//      logger.log(Level.SEVERE, "iae on" + target, e);
//    }
//
//    logger.info("annomap2: " + annomap2);
//    if (annomap2 != null) {
//    }


  }

  @SuppressWarnings({"unchecked","Duplicates"})
  public static void injectGlobal(Class<?> target, HashMap<String,Object> vals/*, Class<?> full*/) {
    //Field[] fields = full.getDeclaredFields();
    Field[] fields = target.getDeclaredFields();
    for (Field field : fields) {
      if (!Modifier.isStatic(field.getModifiers())) {
        continue;
      }

      CompatHelper.trySetAccessible(field);

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
      } catch (IllegalAccessException ignored) {
        System.out.println("should not be reached");
      }
    }

    Map<String, AnnotationList> annomap = DynVarsAgent.getAnnomap(target.getName());
    if (annomap == null) {
      //not dynvars
    } else {
      Map<String,Object> dynvars = null;
      try {
        dynvars = (Map<String,Object>)target.getDeclaredField(DYNVARS).get(null);
      } catch (Throwable t) {
        return;
      }
      for (String fieldname : annomap.keySet()) {
        AnnotationList annos = annomap.get(fieldname);
        if (annos == null) {
          continue;
        }

        for (AnnotationDescription anno : annos) {
          TypeDescription atd = anno.getAnnotationType();
          String annoname = atd.getActualName();
          if (DI.Inject.class.getName().equals(annoname)) {
            Object val = vals.get(fieldname);
            if (val != null) {
              dynvars.put(fieldname, val);
            }
          }
        }
      }
    }
  }


}
