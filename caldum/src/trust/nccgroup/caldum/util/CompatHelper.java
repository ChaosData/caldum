/*
Copyright 2024 Jeff Dileo

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

package trust.nccgroup.caldum.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CompatHelper {
  private static final Logger logger = TmpLogger.DEFAULT;

  final static Method trySetAccessible_m;
  final static Method isAccessible_m;

  static {
    Method _trySetAccessible = null;
    Method _isAccessible = null;
    try {
      //java 9+
      _trySetAccessible = AccessibleObject.class.getDeclaredMethod("trySetAccessible");
    } catch (NoSuchMethodException e) {
      //java <9
      try {
        _isAccessible = AccessibleObject.class.getDeclaredMethod("isAccessible");
      } catch (NoSuchMethodException e2) {
        //wat
        logger.log(Level.SEVERE, "Could not find trySetAccessible or isAccessible in AccessibleObject class.");
      }
    }
    trySetAccessible_m = _trySetAccessible;
    isAccessible_m = _isAccessible;
  }

  public static void trySetAccessible(AccessibleObject ao) {
    try {
      if (trySetAccessible_m != null) {
        trySetAccessible_m.invoke(ao);
      } else if (isAccessible_m != null) {
        if (!(Boolean)isAccessible_m.invoke(ao)) {
          ao.setAccessible(true);
        }
      } else {
        logger.log(Level.SEVERE, "attempted trySetAccessible() polyfill but neither method obtained.");
      }
    } catch (IllegalAccessException e) {
      logger.log(Level.SEVERE, "iae", e);
    } catch (InvocationTargetException e) {
      logger.log(Level.SEVERE, "ite", e);
    }
  }

  public static boolean objectsEquals(Object o1, Object o2) {
    return (o1 == o2) || (o1 != null && o1.equals(o2));
  }

  public static <K,V> boolean mapReplace(Map<K,V> map, K key, V oldValue, V newValue) {
    V curValue = map.get(key);

    if ( !((curValue == oldValue) || (curValue != null && curValue.equals(oldValue))) || (curValue == null && !map.containsKey(key))) {
      //pass
      return false;
    } else {
      map.put(key, newValue);
      return true;
    }
  }

  public static <K,V> V mapPutIfAbsent(Map<K,V> map, K key, V value) {
    V curValue = map.get(key);
    if (curValue == null) {
      curValue = map.put(key, value);
    }
    return curValue;
  }

  public static <K,V> boolean mapRemove(Map<K,V> map, K key, V value) {
    V curValue = map.get(key);
    if (!objectsEquals(curValue, value) ||
        (curValue == null && !map.containsKey(key))) {
      return false;
    }
    map.remove(key);
    return true;
  }
}
