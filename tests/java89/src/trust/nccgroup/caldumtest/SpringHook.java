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

import trust.nccgroup.caldum.annotation.*;
import trust.nccgroup.caldum.wrappers.*;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;


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
  //@DumpWrappers
  public static class HttpServeletRequestGetRequestURIWrapper {

    public static class Settings {
      //@Matcher.Ignore
      //static ElementMatcher i = none();

      @Type
      static ElementMatcher typeMatcher() {
        System.out.println("typeMatcher!!!!!!");

        //TypePool tp = TypePool.Default.ofClassPath();
        TypePool tp = TypePool.Default.ofSystemLoader();
        try {
          TypeDescription td = tp.describe("javax.servlet.http.HttpServletRequest").resolve();
          return isSubTypeOf(td);
        } catch (Throwable t) {
          // java.lang.LinkageError: loader (instance of  sun/misc/Launcher$AppClassLoader): attempted  duplicate class definition for name: "net/bytebuddy/pool/TypePool$Default$LazyTypeDescription$GenericTypeToken$ForTypeVariable$UnresolvedTypeVariable"
          // potentially some sort of race condition being hit (reliably)
          // only appears to happen when there are a certain number of wrapped (any type) hooks in play.
          // the odd thing is that it happens in a single thread execution chain during the non-bytecode generation/instrumentation part of the pluggableadviceagent builder code
          // probably an issue with bytebuddy
          // TODO: wait until code is open sourced to report since triage is likely to be insane
          /*
SEVERE: failed to build hook
trust.nccgroup.caldum.PluggableAdviceAgent$BuildException: failed to execute matcher generator
  at trust.nccgroup.caldum.PluggableAdviceAgent$Builder.fromClass(PluggableAdviceAgent.java:215)
  at trust.nccgroup.caldum.HookProcessor.process(HookProcessor.java:137)
  at trust.nccgroup.caldum.HookProcessor.process(HookProcessor.java:41)
  at trust.nccgroup.caldum.AgentLoader.load(AgentLoader.java:165)
  at trust.nccgroup.vulcanloader.EmbeddedAgentLoader.load(EmbeddedAgentLoader.java:72)
  at trust.nccgroup.vulcanloader.PreMain.premain(PreMain.java:42)
  at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
  at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
  at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
  at java.lang.reflect.Method.invoke(Method.java:498)
  at sun.instrument.InstrumentationImpl.loadClassAndStartAgent(InstrumentationImpl.java:386)
  at sun.instrument.InstrumentationImpl.loadClassAndCallPremain(InstrumentationImpl.java:401)
Caused by: java.lang.LinkageError: loader (instance of  sun/misc/Launcher$AppClassLoader): attempted  duplicate class definition for name: "net/bytebuddy/pool/TypePool$Default$LazyTypeDescription$GenericTypeToken$ForTypeVariable$UnresolvedTypeVariable"
  at java.lang.ClassLoader.defineClass1(Native Method)
  at java.lang.ClassLoader.defineClass(ClassLoader.java:763)
  at java.security.SecureClassLoader.defineClass(SecureClassLoader.java:142)
  at java.net.URLClassLoader.defineClass(URLClassLoader.java:467)
  at java.net.URLClassLoader.access$100(URLClassLoader.java:73)
  at java.net.URLClassLoader$1.run(URLClassLoader.java:368)
  at java.net.URLClassLoader$1.run(URLClassLoader.java:362)
  at java.security.AccessController.doPrivileged(Native Method)
  at java.net.URLClassLoader.findClass(URLClassLoader.java:361)
  at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
  at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:331)
  at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
  at net.bytebuddy.pool.TypePool$Default$GenericTypeExtractor.visitTypeVariable(TypePool.java:2142)
  at net.bytebuddy.jar.asm.signature.SignatureReader.parseType(SignatureReader.java:178)
  at net.bytebuddy.jar.asm.signature.SignatureReader.parseType(SignatureReader.java:240)
  at net.bytebuddy.jar.asm.signature.SignatureReader.accept(SignatureReader.java:111)
  at net.bytebuddy.pool.TypePool$Default$GenericTypeExtractor$ForSignature.extract(TypePool.java:2482)
  at net.bytebuddy.pool.TypePool$Default$GenericTypeExtractor$ForSignature$OfMethod.extract(TypePool.java:2662)
  at net.bytebuddy.pool.TypePool$Default$LazyTypeDescription$MethodToken.<init>(TypePool.java:5979)
  at net.bytebuddy.pool.TypePool$Default$TypeExtractor$MethodExtractor.visitEnd(TypePool.java:8279)
  at net.bytebuddy.jar.asm.ClassReader.readMethod(ClassReader.java:1279)
  at net.bytebuddy.jar.asm.ClassReader.accept(ClassReader.java:679)
  at net.bytebuddy.jar.asm.ClassReader.accept(ClassReader.java:391)
  at net.bytebuddy.pool.TypePool$Default.parse(TypePool.java:1176)
  at net.bytebuddy.pool.TypePool$Default.doDescribe(TypePool.java:1160)
  at net.bytebuddy.pool.TypePool$AbstractBase.describe(TypePool.java:408)
  at net.bytebuddy.pool.TypePool$AbstractBase$Hierarchical.describe(TypePool.java:471)
  at trust.nccgroup.caldumtest.SpringHook$HttpServeletRequestGetRequestURIWrapper$Settings.typeMatcher(SpringHook.java:60)
  at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
  at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
  at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
  at java.lang.reflect.Method.invoke(Method.java:498)
  at trust.nccgroup.caldum.PluggableAdviceAgent$Builder.fromClass(PluggableAdviceAgent.java:211)
  ... 11 more
          */
          TypeDescription td = tp.describe("javax.servlet.http.HttpServletRequest").resolve();
          return isSubTypeOf(td);
        }
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
  @Dynamic
  //@Dump
  public static class RequestParamInterceptor {

    public static String sss = "s-";
//    public static String sss2 = "z-";
    public static String zzz = "GGGG";
    public static Map<String,Object> foomap = new HashMap<String,Object>();

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
      // 03:19:14.077 [http-nio-127.0.0.1-8084-exec-2] DEBUG org.springframework.web.servlet.DispatcherServlet
      // - Failed to complete request: org.springframework.web.util.NestedServletException: Handler dispatch failed;
      // nested exception is java.lang.NoSuchFieldError: sss

      System.out.println("v2");

      /*try {
        String cn = c == null ? "(null)" : c.getName();
        String mn = m == null ? "(null)" : m.getName();
        String ss = self == null ? "(null)" : self.getClass().getName();

        System.out.println(cn + "::" + mn + "(\"" + name + "\", ...)" + ("name".equals(name) ? " <<<<<<<<<<<<<" : ""));
      } catch (Throwable t) {
        t.printStackTrace();
      }*/

      sss = sss + "s";
//      sss2 = sss2 + "z";

      //String f = (String)foomap.get("zzz");

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
        System.out.println(sss);
        //System.out.println(sss + sss2);
        ret = "caldum";
      }
    }

  }

}
