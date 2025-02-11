/*
Copyright 2018-2019 NCC Group
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

package trust.nccgroup.caldum.wrappers;

import trust.nccgroup.caldum.annotation.*;
import trust.nccgroup.caldum.global.State;
import trust.nccgroup.caldum.util.CompatHelper;

import java.util.Objects;

import static net.bytebuddy.asm.Advice.*;
import static trust.nccgroup.caldum.global.State.*;

//@SuppressWarnings("ALL")
public class NoRecursion {
  // intended to wrap hook code to enable it to further call hooked code without infinitely invoking the hook

//  static class EntryOnly {
//    @OnMethodEnter(skipOn = OnNonDefaultValue.class)
//    static boolean enter() {
//      if (!State.active.add(Thread.currentThread().getId())) {
//        return true;
//      }
//      return false;
//    }
//
//    @OnMethodExit
//    static void exit(@OnMethodEnter boolean skipped) {
//      if (!skipped) {
//        State.active.remove(Thread.currentThread().getId());
//      }
//    }
//  }

  @Wrapper.OnMethodEnter
  static class EntryHook {
    @OnMethodEnter(skipOn = OnNonDefaultValue.class)
    static boolean enter_enter(@Origin Class<?> hook_class) {
      //System.out.println("enter_enter");

      State s;
      synchronized (State.states) {
        s = CompatHelper.mapPutIfAbsent(State.states, Thread.currentThread().getId(), new State(ENTER_ENTER, hook_class));

        if (s == null) { // first NoRecursion hook Wrapper.(OnMethodEnter/OnMethodExit)
          return false; // run entry hook body, eventually run exit hook body (if exit hook'd)
        } else if (s.equals(new State(ENTER_EXIT, hook_class))) {
//          synchronized (State.states) {
            //State.states.replace(Thread.currentThread().getId(), new State(ENTER_EXIT, hook_class), new State(ENTER_ENTER, hook_class));
          State oldValue = new State(ENTER_EXIT, hook_class);
          State newValue = new State(ENTER_ENTER, hook_class);
          Long key = Thread.currentThread().getId();
          State curValue = State.states.get(key);
          if (!CompatHelper.objectsEquals(curValue, oldValue) || (curValue == null && !State.states.containsKey(key))) {
            //pass
          } else {
            State.states.put(key, newValue);
          }
//          }
          return false; // top level is entry-only hook and existing state is stale, replace and run entry hook body
        } else { // already set
          return true; // skip
        }
      }
    }

    @OnMethodExit
    static void enter_exit(@Origin Class<?> hook_class, @Enter boolean enter_skipped) {
      //System.out.println("enter_exit");

      if (enter_skipped) {
        return;
      } else { // ratchet state
        synchronized (State.states) {
          //State.states.replace(Thread.currentThread().getId(), new State(ENTER_ENTER, hook_class), new State(ENTER_EXIT, hook_class));
          State oldValue = new State(ENTER_ENTER, hook_class);
          State newValue = new State(ENTER_EXIT, hook_class);
          Long key = Thread.currentThread().getId();
          State curValue = State.states.get(key);
          if (!CompatHelper.objectsEquals(curValue, oldValue) || (curValue == null && !State.states.containsKey(key))) {
            //pass
          } else {
            State.states.put(key, newValue);
          }
        }
      }
    }
  }

  @Wrapper.OnMethodExit
  static class ExitHook {
    @OnMethodEnter(skipOn = OnNonDefaultValue.class)
    static boolean exit_enter(@Origin Class<?> hook_class) {
      //System.out.println("exit_enter");

      State s;
      synchronized (State.states) {
        s = CompatHelper.mapPutIfAbsent(State.states, Thread.currentThread().getId(), new State(EXIT_ENTER, hook_class));

        if (s == null) { // first NoRecursion hook Wrapper.(OnMethodEnter/OnMethodExit)
          return false; // run exit hook body
        } else if (s.equals(new State(ENTER_EXIT, hook_class))) { // this is the exit to the just completed entry hook
//          synchronized (State.states) {
            //State.states.replace(Thread.currentThread().getId(), new State(ENTER_EXIT, hook_class), new State(EXIT_ENTER, hook_class));
          State oldValue = new State(ENTER_EXIT, hook_class);
          State newValue = new State(EXIT_ENTER, hook_class);
          Long key = Thread.currentThread().getId();
          State curValue = State.states.get(key);
          if (!CompatHelper.objectsEquals(curValue, oldValue) || (curValue == null && !State.states.containsKey(key))) {
            //pass
          } else {
            State.states.put(key, newValue);
          }
//          }
          return false; // run exit hook body
        } else {
          return true; // skip
        }
      }
    }

    @OnMethodExit(backupArguments=false)
    static void exit_exit(@Origin Class<?> hook_class, @Enter boolean enter_skipped) {
      //System.out.println("exit_exit");

      if (enter_skipped) {
        return;
      } else { // clear state
        synchronized (State.states) {
          CompatHelper.mapRemove(State.states, Thread.currentThread().getId(), new State(EXIT_ENTER, hook_class));
        }
      }
    }
  }

}
