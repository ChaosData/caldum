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

package trust.nccgroup.caldum.wrappers;

import trust.nccgroup.caldum.annotation.*;
import trust.nccgroup.caldum.global.State;

import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.asm.Advice.*;
import static trust.nccgroup.caldum.global.State.*;

@SuppressWarnings("ALL")
public class NoSelfRecursion {
  // intended to wrap hook code to enable it to further call hooked code without infinitely invoking the hook

  @Wrapper.OnMethodEnter
  static class EntryHook {
    @OnMethodEnter(skipOn = OnNonDefaultValue.class)
    static boolean enter_enter(@Origin Class<?> hook_class) {
      //System.out.println("enter_enter");
      Map<Long, State> cs;
      synchronized (State.class_states) {
        cs = State.class_states.get(hook_class);
        if (cs == null) {
          Map<Long, State> _s = new HashMap<Long, State>();
          cs = State.class_states.putIfAbsent(hook_class, _s);
          if (cs == null) {
            cs = _s;
          }
        }
      }
      State s;
      synchronized (cs) {
        s = cs.putIfAbsent(Thread.currentThread().getId(), new State(ENTER_ENTER, hook_class));
      }
      if (s == null) { // first NoRecursion hook Wrapper.(OnMethodEnter/OnMethodExit)
        return false; // run entry hook body, eventually run exit hook body (if exit hook'd)
      } else if (s.equals(new State(ENTER_EXIT, hook_class))) {
        synchronized (cs) {
          cs.replace(Thread.currentThread().getId(), new State(ENTER_EXIT, hook_class), new State(ENTER_ENTER, hook_class));
        }
        return false; // top level is entry-only hook and existing state is stale, replace and run entry hook body
      } else { // already set
        return true; // skip
      }
    }

    @OnMethodExit
    static void enter_exit(@Origin Class<?> hook_class, @Enter boolean enter_skipped) {
      //System.out.println("enter_exit");

      if (enter_skipped) {
        return;
      } else { // ratchet state
        Map<Long, State> cs;
        synchronized (State.class_states) {
          cs = State.class_states.get(hook_class);
          if (cs == null) {
            Map<Long, State> _s = new HashMap<Long, State>();
            cs = State.class_states.putIfAbsent(hook_class, _s);
            if (cs == null) {
              cs = _s;
            }
          }
        }
        synchronized (cs) {
          cs.replace(Thread.currentThread().getId(), new State(ENTER_ENTER, hook_class), new State(ENTER_EXIT, hook_class));
        }
      }
    }
  }

  @Wrapper.OnMethodExit
  static class ExitHook {
    @OnMethodEnter(skipOn = OnNonDefaultValue.class)
    static boolean exit_enter(@Origin Class<?> hook_class) {
      //System.out.println("exit_enter");

      Map<Long, State> cs;
      synchronized (State.class_states) {
        cs = State.class_states.get(hook_class);
        if (cs == null) {
          Map<Long, State> _s = new HashMap<Long, State>();
          cs = State.class_states.putIfAbsent(hook_class, _s);
          if (cs == null) {
            cs = _s;
          }
        }
      }
      State s;
      synchronized (cs) {
        s = cs.putIfAbsent(Thread.currentThread().getId(), new State(EXIT_ENTER, hook_class));
      }
      if (s == null) { // first NoRecursion hook Wrapper.(OnMethodEnter/OnMethodExit)
        return false; // run exit hook body
      } else if (s.equals(new State(ENTER_EXIT, hook_class))) { // this is the exit to the just completed entry hook
        synchronized (cs) {
          cs.replace(Thread.currentThread().getId(), new State(ENTER_EXIT, hook_class), new State(EXIT_ENTER, hook_class));
        }
        return false; // run exit hook body
      } else {
        return true; // skip
      }
    }

    @OnMethodExit(backupArguments=false)
    static void exit_exit(@Origin Class<?> hook_class, @Enter boolean enter_skipped) {
      //System.out.println("exit_exit");

      if (enter_skipped) {
        return;
      } else { // clear state
        Map<Long, State> cs;
        synchronized (State.class_states) {
          cs = State.class_states.get(hook_class);
          if (cs == null) {
            Map<Long, State> _s = new HashMap<Long, State>();
            cs = State.class_states.putIfAbsent(hook_class, _s);
            if (cs == null) {
              cs = _s;
            }
          }
        }
        synchronized (cs) {
          cs.remove(Thread.currentThread().getId(), new State(EXIT_ENTER, hook_class));
        }
      }
    }
  }


//  @Wrapper.OnMethodEnter
//  static class EntryHook {
//    @OnMethodEnter(skipOn = OnNonDefaultValue.class)
//    static boolean enter_enter(@Origin Class<?> hook_class) {
//      System.out.println("enter_enter");
//      ConcurrentHashMap<Long, State> cs = State.class_states.get(hook_class);
//      if (cs == null) {
//        ConcurrentHashMap<Long, State> _s = new ConcurrentHashMap<Long, State>();
//        cs = State.class_states.putIfAbsent(hook_class, _s);
//        if (cs == null) {
//          cs = _s;
//        }
//      }
//
//      State s = cs.putIfAbsent(Thread.currentThread().getId(), new State(ENTER_ENTER, hook_class));
//      if (s == null) { // first NoRecursion hook Wrapper.(OnMethodEnter/OnMethodExit)
//        return false; // run entry hook body, eventually run exit hook body (if exit hook'd)
//      } else if (s.equals(new State(ENTER_EXIT, hook_class))) {
//        cs.replace(Thread.currentThread().getId(), new State(ENTER_EXIT, hook_class), new State(ENTER_ENTER, hook_class));
//        return false; // top level is entry-only hook and existing state is stale, replace and run entry hook body
//      } else { // already set
//        return true; // skip
//      }
//    }
//
//    @OnMethodExit
//    static void enter_exit(@Origin Class<?> hook_class, @Enter boolean enter_skipped) {
//      System.out.println("enter_exit");
//      ConcurrentHashMap<Long, State> cs = State.class_states.get(hook_class);
//      if (cs == null) {
//        ConcurrentHashMap<Long, State> _s = new ConcurrentHashMap<Long, State>();
//        cs = State.class_states.putIfAbsent(hook_class, _s);
//        if (cs == null) {
//          cs = _s;
//        }
//      }
//
//      if (enter_skipped) {
//        return;
//      } else { // ratchet state
//        cs.replace(Thread.currentThread().getId(), new State(ENTER_ENTER, hook_class), new State(ENTER_EXIT, hook_class));
//      }
//    }
//  }
//
//  @Wrapper.OnMethodExit
//  static class ExitHook {
//    @OnMethodEnter(skipOn = OnNonDefaultValue.class)
//    static boolean exit_enter(@Origin Class<?> hook_class) {
//      System.out.println("exit_enter");
//      ConcurrentHashMap<Long, State> cs = State.class_states.get(hook_class);
//      if (cs == null) {
//        ConcurrentHashMap<Long, State> _s = new ConcurrentHashMap<Long, State>();
//        cs = State.class_states.putIfAbsent(hook_class, _s);
//        if (cs == null) {
//          cs = _s;
//        }
//      }
//
//      State s = cs.putIfAbsent(Thread.currentThread().getId(), new State(EXIT_ENTER, hook_class));
//      if (s == null) { // first NoRecursion hook Wrapper.(OnMethodEnter/OnMethodExit)
//        return false; // run exit hook body
//      } else if (s.equals(new State(ENTER_EXIT, hook_class))) { // this is the exit to the just completed entry hook
//        cs.replace(Thread.currentThread().getId(), new State(ENTER_EXIT, hook_class), new State(EXIT_ENTER, hook_class));
//        return false; // run exit hook body
//      } else {
//        return true; // skip
//      }
//    }
//
//    @OnMethodExit(backupArguments=false)
//    static void exit_exit(@Origin Class<?> hook_class, @Enter boolean enter_skipped) {
//      System.out.println("exit_exit");
//      ConcurrentHashMap<Long, State> cs = State.class_states.get(hook_class);
//      if (cs == null) {
//        ConcurrentHashMap<Long, State> _s = new ConcurrentHashMap<Long, State>();
//        cs = State.class_states.putIfAbsent(hook_class, _s);
//        if (cs == null) {
//          cs = _s;
//        }
//      }
//
//      if (enter_skipped) {
//        return;
//      } else { // clear state
//        cs.remove(Thread.currentThread().getId(), new State(EXIT_ENTER, hook_class));
//      }
//    }
//  }


}
