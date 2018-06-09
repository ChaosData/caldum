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

import static net.bytebuddy.asm.Advice.*;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import static net.bytebuddy.matcher.ElementMatchers.*;
import trust.nccgroup.caldum.annotation.*;
import trust.nccgroup.caldum.annotation.Matcher;
import trust.nccgroup.caldum.wrappers.*;

import java.lang.reflect.*;
import java.io.*;

public class CompilerHook {

  @Hook(wrappers = { NoRecursion.class })
  public static class LoadWrapper {
    public static class Settings {
      @Matcher.Type
      static ElementMatcher typeMatcher() throws ClassNotFoundException {
        return named("clojure.lang.Compiler");
      }

      @Matcher.Member
      static ElementMatcher m = named("load").and(takesArguments(3));
    }

    @OnMethodEnter(skipOn = OnNonDefaultValue.class)
    static boolean enter(@Origin Class clazz, @Origin Method method,
                      @AllArguments(typing = Assigner.Typing.DYNAMIC) Object[] args) {
      if (!"foo/core.clj".equals(args[1])) {
        return false;
      }

      String s = "";
      try {
        InputStreamReader isr = (InputStreamReader)args[0];
        char[] buf = new char[4096];
        int r = 0;
        while (r != -1) {
          r = isr.read(buf, 0, 4096);
          if (r > 0) {
            s += new String(buf, 0, r);
          }
        }
      } catch (Throwable t) {
        t.printStackTrace();
        return false;
      }

      String r = s.replace("Executed", "Executed (this code was modified)");
      r = r.replace("interpose \" \"", "interpose \"_\"");

      try {
        method.invoke(null, new StringReader(r), args[1], args[2]);
        return true;
      } catch (Throwable t) {
        t.printStackTrace();
      }
      return false;
    }
  }

}
