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

package trust.nccgroup.clojurehook;

import java.lang.instrument.Instrumentation;
import static net.bytebuddy.asm.Advice.*;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.description.type.TypeDescription;
import static net.bytebuddy.matcher.ElementMatchers.*;
import net.bytebuddy.pool.TypePool;
import trust.nccgroup.caldum.annotation.*;
import static trust.nccgroup.caldum.annotation.Matcher.*;
import static trust.nccgroup.caldum.annotation.DI.*;
import trust.nccgroup.caldum.wrappers.*;

import java.util.*;
import java.util.logging.*;

public class InvokeHook {
  static Instrumentation inst = null;

  @Hook(wrappers = { NoRecursion.class }) // required, many `toString()` calls
                                          // `invoke(...)` more Clojure code
  public static class InvokeWrapper {

    public static class Settings {
      @Type
      static ElementMatcher typeMatcher() throws ClassNotFoundException {
        return isSubTypeOf(Class.forName("clojure.lang.IFn"));
      }

      @Member
      static ElementMatcher m = named("invoke");
    }

    @Inject
    public static Logger logger;

    @OnMethodEnter
    static void enter(@Origin Class clazz,
                      @This(optional = false) Object self,
                      @AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args) {
      String out = "";
      try {
        if (self == null) {
          out += "this: " + clazz.getName() + "\n";
        } else {
          out += "this: " + self.getClass().getName() + "\n";
        }
        for (Object a : args) {
          try {
            out += ">> " + (a==null?"(null)":a.toString()) + "\n";
          } catch (Throwable tt) {
            out += ">> (exception raised on .toString(): " + tt.getClass().getName() + ": " + tt.getMessage() + "): " + a.getClass() + "\n";
          }
        }

        logger.info(out);
      } catch (Throwable t) {
        logger.log(Level.SEVERE, "exception: " + t.getClass().getName() + ": " + t.getMessage());
      }
    }

  }

}
