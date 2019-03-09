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

public class SpringHook {
  static Instrumentation inst = null;

  @Hook(wrappers = { NoRecursion.class })
  @Dump
  public static class HttpServeletRequestGetRequestURIWrapper {

    public static class Settings {
      //@Matcher.Ignore
      //static ElementMatcher i = none();

      @Type
      static ElementMatcher typeMatcher() {
        //TypePool tp = TypePool.Default.ofClassPath();
        TypePool tp = TypePool.Default.ofSystemLoader();
        TypeDescription td = tp.describe("javax.servlet.http.HttpServletRequest").resolve();
        return isSubTypeOf(td);
      }

      @Member
      //static ElementMatcher m = isMethod().and(nameStartsWith("get"));
      static ElementMatcher m = isMethod().and(named("getServletPath"));
    }


    @OnMethodExit(onThrowable = Exception.class)
    static void exit(@Origin Class c, @Origin Method m, @This Object self,
                     @Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object ret) {
      /*try {
        String cn = c == null ? "(null)" : c.getName();
        String mn = m == null ? "(null)" : m.getName();
        String ss = self == null ? "(null)" : self.getClass().getName();
        String rs = ret == null ? "(null)" : ret.toString();

        System.out.println(cn + "::" + mn + " -> " + rs + ("/test".equals(rs) ? " <<<<<<<<<<<<<" : ""));
      } catch (Throwable t) {
        t.printStackTrace();
      }*/

      if ("/test".equals(ret)) {
        ret = new String("/nottest");
      }
    }

  }

  @Hook(wrappers = { NoRecursion.class })
  public static class RequestParamInterceptor {

    public static class Settings {
      @Type
      static ElementMatcher typeMatcher() {
        TypePool tp = TypePool.Default.ofSystemLoader();
        TypeDescription td = tp.describe("org.springframework.web.method.annotation.RequestParamMethodArgumentResolver").resolve();
        return isSubTypeOf(td);
      }

      @Member
      //static ElementMatcher m = isMethod().and(nameStartsWith("get"));
      static ElementMatcher m = isMethod().and(named("resolveName"));
    }


    @OnMethodEnter
    static String enter(@Origin Class c, @Origin Method m, @This Object self, @Argument(value=0) String name) {
      /*try {
        String cn = c == null ? "(null)" : c.getName();
        String mn = m == null ? "(null)" : m.getName();
        String ss = self == null ? "(null)" : self.getClass().getName();

        System.out.println(cn + "::" + mn + "(\"" + name + "\", ...)" + ("name".equals(name) ? " <<<<<<<<<<<<<" : ""));
      } catch (Throwable t) {
        t.printStackTrace();
      }*/

      if ("name".equals(name)) {
        return name;
      }
      return null;
    }

    @OnMethodExit(onThrowable = Exception.class)
    static void exit(@Enter String name, @Return(readOnly = false) Object ret) {
      if (name == null || ret == null) {
        return;
      }

      if (!(ret instanceof String)) {
        return;
      }

      String r = (String)ret;
      if ("zzzzz".equals(r)) {
        ret = "caldum";
      }
    }

  }

}
