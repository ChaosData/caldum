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

package trust.nccgroup.caldum.global;

//import java.util.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class State {
  // Written as such (not using an enum) to keep to one .class file

  // the separate class (the wrapped hook class, not the class it hooks)
  // tracking is done to ensure that a lone exit hook down the call stack
  // from an enter-exit hook does not muck with the state

  public static final State ENTER_ENTER = new State(0, State.class);
  public static final State ENTER_EXIT = new State(1, State.class);
  public static final State EXIT_ENTER = new State(2, State.class);
  public static final State EXIT_EXIT = new State(3, State.class); // unused
  public static final State SKIP_SKIP = new State(4, State.class); // unused

  private int state;
  private Class<?> clazz;

  private State(int _state, Class<?> _clazz) {
    state = _state;
    clazz = _clazz;
  }

  public State(State _h, Class<?> _clazz) {
    state = _h.state;
    clazz = _clazz;
  }

  public boolean equals(Object o) {
    if (o instanceof State) {
      State h = (State)o;
      return (this.state == h.state)
        && (this.clazz == h.clazz);
    }
    return false;
  }

  public String toString() {
    switch (state) {
      case 0: { return "ENTER_ENTER"; }
      case 1: { return "ENTER_EXIT"; }
      case 2: { return "EXIT_ENTER"; }
      case 3: { return "EXIT_EXIT"; }
      case 4: { return "SKIP_SKIP"; }
      default: { return "INVALID"; }
    }
  }

  //public static Set<Long> active = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());

  //public static ConcurrentHashMap<Long, State> states = new ConcurrentHashMap<Long, State>(16, 0.75f, 1);
  // using synchronized over ConcurrentHashMap due to concerns that writes may not finish (and reads get a stale value)
  public final static Map<Long, State> states = new HashMap<Long, State>();
  public final static Map<Class<?>, Map<Long, State>> class_states = new HashMap<Class<?>, Map<Long, State>>();



}
