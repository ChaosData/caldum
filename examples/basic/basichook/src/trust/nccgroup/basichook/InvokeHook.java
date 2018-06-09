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

import java.lang.instrument.Instrumentation;
import static net.bytebuddy.asm.Advice.*;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.description.type.TypeDescription;
import static net.bytebuddy.matcher.ElementMatchers.*;
import net.bytebuddy.pool.TypePool;
import trust.nccgroup.caldum.annotation.*;
import static trust.nccgroup.caldum.annotation.Matcher.*;
import trust.nccgroup.caldum.wrappers.*;

import java.util.*;
import java.util.logging.*;

public class InvokeHook {
  static Instrumentation inst = null;

  @Hook(wrappers = { TestWrapper1.class, NoRecursion.class, TestWrapper2.class })
  //@Hook(wrappers = { NoRecursion.class })
  //@Hook
  //@Debug
  public static class InvokeWrapper {

    public static class Settings {
      @Type
      static ElementMatcher typeMatcher() throws ClassNotFoundException {
        return any();
      }

      @Member
      static ElementMatcher m = isMethod().and(named("bar"));
    }

    @OnMethodEnter
    static void enter(@Origin Class<?> cc) {
      try {
        System.out.println(">> Hook::enter()");
        cc.getDeclaredMethod("foo").invoke(null);
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }

    @OnMethodExit
    static void exit() {
      //...
    }

  }

}
