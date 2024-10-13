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

package trust.nccgroup.caldumtest;

import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import trust.nccgroup.caldum.annotation.*;
import trust.nccgroup.caldum.annotation.DI.Inject;

import java.util.Arrays;
import java.util.logging.Logger;
import java.util.logging.Level;

import static net.bytebuddy.asm.Advice.OnMethodExit;
import static net.bytebuddy.asm.Advice.Return;
import static net.bytebuddy.matcher.ElementMatchers.*;
import static trust.nccgroup.caldum.annotation.Matcher.Member;
import static trust.nccgroup.caldum.annotation.Matcher.Type;

public class DumbHook {

  @trust.nccgroup.caldum.annotation.Dynamic
  @Hook
  public static class NopHook {

    public static class Settings {
      @Matcher.Ignore
      static ElementMatcher i = any();

      @Type
      static ElementMatcher typeMatcher() {
        return named("foo.Bar");
      }

      @Member
      static ElementMatcher m = isMethod().and(named("foobar"));
    }

    public static String retval = "__notsecret!__";

    @OnMethodExit
    static void exit() {
      System.out.println("should not be reached!");
    }

  }

}
