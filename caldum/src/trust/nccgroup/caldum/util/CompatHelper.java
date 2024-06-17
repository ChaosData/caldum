package trust.nccgroup.caldum.util;

import java.util.Map;

public class CompatHelper {

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
