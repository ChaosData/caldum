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
import trust.nccgroup.caldum.annotation.Hook;
import trust.nccgroup.caldum.annotation.Matcher;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.logging.Logger;
import java.lang.reflect.*;
import java.util.Map;

import static net.bytebuddy.asm.Advice.OnMethodExit;
import static net.bytebuddy.asm.Advice.Return;
import static net.bytebuddy.matcher.ElementMatchers.*;
import static trust.nccgroup.caldum.annotation.Matcher.Member;
import static trust.nccgroup.caldum.annotation.Matcher.Type;
import static trust.nccgroup.caldum.annotation.DI.*;
import trust.nccgroup.caldum.annotation.Test;
import trust.nccgroup.caldum.annotation.Dump;
import trust.nccgroup.caldum.annotation.DumpWrappers;

public class StringHook {
  static Instrumentation inst = null;

  @Hook
  //@Dump
  //@DumpWrappers
  //@Test
  public static class GetBytesWrapper {

    public static class Settings {
      @Matcher.Ignore
      static ElementMatcher i = none();

      @Type
      static ElementMatcher typeMatcher() {
        return is(String.class);
      }

      @Member
      static ElementMatcher m = isMethod().and(named("getBytes"));
    }

    @Inject
    public static Logger logger;

    public static long iii = 5555L;

    //@Dump
    public static Long III = 5555L;
    public static Map foobar = null;


    @OnMethodExit
    static void exit(@Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object ret) {
      /*
      try {
        long b = 777L;
        Long B = 777L;
        System.out.println("AAAA");
        iii = b;
        III = B;
        System.out.println("BBBB");
        System.out.println(iii);
        System.out.println(III);
        System.out.println("CCCC");
        foobar.put("iii", b);
        foobar.put("III", B);
        System.out.println("DDDD");
        long c = (Long)foobar.get("iii");
        System.out.println(c);
        Long C = (Long)foobar.get("III");
        System.out.println(C);
        System.out.println("EEEE");
      } catch (Throwable t) { t.printStackTrace(); }
      */
      byte[] r = (byte[])ret;
      if (Arrays.equals(r, new byte[]{'_', '_', 's', 'e', 'c', 'r', 'e', 't', '_', '_'})) {
        ret = new byte[]{'_', '_', 'n', 'o', 't', 's', 'e', 'c', 'r', 'e', 't', '_', '_'};
      } else if (Arrays.equals(r, new byte[]{'_', '_', 'm', 'a', 'g', 'i', 'c', '_', '_'})) {
        if (logger == null) {
          ret = null;
        }
      }
    }

  }

}
