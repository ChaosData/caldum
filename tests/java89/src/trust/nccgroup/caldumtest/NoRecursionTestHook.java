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

package trust.nccgroup.caldumtest;

import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.description.type.TypeDescription;

import trust.nccgroup.caldum.annotation.Debug;
import trust.nccgroup.caldum.annotation.Dump;
import trust.nccgroup.caldum.annotation.DumpWrappers;
import trust.nccgroup.caldum.annotation.Hook;
import trust.nccgroup.caldum.annotation.Matcher;
import trust.nccgroup.caldum.wrappers.*;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.lang.reflect.*;


import static net.bytebuddy.asm.Advice.*;
import static net.bytebuddy.asm.Advice.This;
import static net.bytebuddy.asm.Advice.Origin;
import static net.bytebuddy.asm.Advice.Return;
import static net.bytebuddy.matcher.ElementMatchers.*;
import static trust.nccgroup.caldum.annotation.Matcher.Member;
import static trust.nccgroup.caldum.annotation.Matcher.Type;

public class NoRecursionTestHook {
  static Instrumentation inst = null;

  @Hook(wrappers = { NoRecursion.class })
  public static class NoRecursion1Wrapper {

    public static class Settings {
      @Type
      static ElementMatcher typeMatcher() {
        TypePool tp = TypePool.Default.ofSystemLoader();
        TypeDescription td = tp.describe("trust.nccgroup.caldumtest.test.NoRecursionTest").resolve();
        return isSubTypeOf(td);
      }
      @Member
      static ElementMatcher m = isMethod().and(named("norecursion1"));
    }

    @OnMethodExit
    static void exit(@This(optional = true) Object self, @Origin Method m, @Return(readOnly = false) int ret) {
      try {
        ret += (Integer)m.invoke(self);
      } catch (Throwable ignored) { }
    }

  }

  @Hook(wrappers = { NoRecursion.class })
  @Dump
  public static class NoRecursion2Wrapper {

    public static class Settings {
      @Type
      static ElementMatcher typeMatcher() {
        TypePool tp = TypePool.Default.ofSystemLoader();
        TypeDescription td = tp.describe("trust.nccgroup.caldumtest.test.NoRecursionTest$Inner").resolve();
        return isSubTypeOf(td);
      }
      @Member
      static ElementMatcher m = isMethod().and(named("norecursion2"));
    }

    @OnMethodExit
    static void exit(@This(optional = true) Object self, @Origin Method m, @Return(readOnly = false) int ret) {
      try {
        ret += (Integer)m.invoke(self);
      } catch (Throwable ignored) { }
    }

  }

  @Hook(wrappers = { NoSelfRecursion.class })
  @Dump
  public static class NoSelfRecursionWrapper {

    public static class Settings {
      @Type
      static ElementMatcher typeMatcher() {
        TypePool tp = TypePool.Default.ofSystemLoader();
        TypeDescription td = tp.describe("trust.nccgroup.caldumtest.test.NoRecursionTest").resolve();
        return isSubTypeOf(td);
      }
      @Member
      static ElementMatcher m = isMethod().and(nameStartsWith("noselfrecursion")).and(returns(int.class));
    }

    @OnMethodExit
    static void exit(@This(optional = true) Object self, @Origin Method m, @Return(readOnly = false) int ret) {
      try {
        ret += (Integer)m.invoke(self);
      } catch (Throwable ignored) { }
    }

  }

}
