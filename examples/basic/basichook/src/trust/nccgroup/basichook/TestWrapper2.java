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

package trust.nccgroup.basichook;

import trust.nccgroup.caldum.annotation.*;
import static net.bytebuddy.asm.Advice.*;

public class TestWrapper2 {

  @Wrapper.OnMethodEnter
  static class EntryHook {
    @OnMethodEnter
    static void enter_enter() {
      System.out.println("TW2::enter_enter");
    }

    @OnMethodExit
    static void enter_exit() {
      System.out.println("TW2::enter_exit");
    }
  }

  @Wrapper.OnMethodExit
  static class ExitHook {
    @OnMethodEnter
    static void exit_enter(@Origin Class<?> hook_class) {
      System.out.println("TW2::exit_enter");
    }

    @OnMethodExit
    static void exit_exit() {
      System.out.println("TW2::exit_exit");
    }
  }
}
