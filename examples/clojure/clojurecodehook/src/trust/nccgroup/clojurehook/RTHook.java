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
import static trust.nccgroup.caldum.annotation.Matcher.*;
import trust.nccgroup.caldum.wrappers.*;

public class RTHook {

  @Hook
  public static class LastModifiedWrapper {
    public static class Settings {
      @Type
      static ElementMatcher typeMatcher() throws ClassNotFoundException {
        return named("clojure.lang.RT");
      }

      @Member
      static ElementMatcher m = named("lastModified").and(takesArguments(2));
    }

    @OnMethodExit
    static void exit(@Return(readOnly = false) long ret,
                     @AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args) {
      String name = (String)args[1];
      if ("foo/core.clj".equals(name)) { //.endsWith(".clj")) {
        ret = Long.MAX_VALUE;
      }
    }
  }

}
