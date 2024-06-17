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

import trust.nccgroup.caldum.global.State;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static trust.nccgroup.caldum.global.State.ENTER_ENTER;
import static trust.nccgroup.caldum.global.State.SKIP_SKIP;

public class RecursionCaller {
  //note: Be very careful with this, there is generally a reason why one doesn't
  //      want hook code to run during nested calls from hook code. This disables
  //      that down the stack. To prevent even more insane problems it will at least
  //      bail out if it itself gets called from within such a context.

  static Object callWithHooks(Class<?>[] hook_classes, Object obj, Method m, Object[] args) throws Throwable {
    Throwable t = null;

    State old_state = null;
    synchronized (State.states) {
      old_state = State.states.put(Thread.currentThread().getId(), new State(SKIP_SKIP, RecursionCaller.class));
    }
    if (new State(SKIP_SKIP, RecursionCaller.class).equals(old_state)) {
      throw new Throwable("Already under RecursionCaller call chain.");
    }

    Map<Long, State> cs;
    State[] old_states = new State[hook_classes.length];
    synchronized (State.class_states) {
      int i = 0;
      for (Class<?> hook_class : hook_classes) {
        cs = State.class_states.get(hook_class);
        if (cs == null) {
          Map<Long, State> _s = new HashMap<Long, State>();

          cs = State.class_states.get(hook_class);
          if (cs == null) {
            State.class_states.put(hook_class, _s);
            cs = _s;
          }
        }
        synchronized (cs) {
          State old = cs.put(Thread.currentThread().getId(), new State(SKIP_SKIP, RecursionCaller.class));
          old_states[i] = old;
        }
        i++;
      }
    }

    try {
      m.setAccessible(true);
      m.invoke(obj, args);
    } catch (IllegalAccessException e) {
      t = e;
    } catch (InvocationTargetException e) {
      t = e;
    } catch (Throwable e) {
      t = e;
    }

    synchronized (State.states) {
      State.states.put(Thread.currentThread().getId(), old_state);
    }
    synchronized (State.class_states) {
      int i = 0;
      for (Class<?> hook_class : hook_classes) {
        cs = State.class_states.get(hook_class);
        synchronized (cs) {
          cs.put(Thread.currentThread().getId(), old_states[i]);
        }
        i++;
      }
    }

    if (t != null) {
      throw t;
    }
    return null;
  }
}
