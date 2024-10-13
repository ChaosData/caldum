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

package trust.nccgroup.caldumtest.generated;

import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import trust.nccgroup.caldum.annotation.*;
import trust.nccgroup.caldum.annotation.DI.Inject;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import static net.bytebuddy.asm.Advice.OnMethodExit;
import static net.bytebuddy.asm.Advice.Return;
import static net.bytebuddy.matcher.ElementMatchers.*;
import static trust.nccgroup.caldum.annotation.Matcher.Member;
import static trust.nccgroup.caldum.annotation.Matcher.Type;

public class TemplatedFileHook {
  @Hook
  @Dynamic
  //@Dump
  public static class FileAbsPathHook {

    public String test;
    //public Map<String,Object> __dynnsvars__ = new HashMap<String,Object>();

    //static {}

    /*public FileAbsPathHook() {
      __dynnsvars__.put("test", "foo");
    }*/

    public static class Settings {
      @Matcher.Ignore
      static ElementMatcher i = none();

      @Type
      static ElementMatcher typeMatcher() {
        return is(java.io.File.class);
      }

      @Member
      static ElementMatcher m = isMethod().and(named("getAbsolutePath"));
    }

    @Inject
    public static Logger logger;
    @Inject
    public static String wat;

    @OnMethodExit
    static void exit(@Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object ret) {
      String _s = (String)ret;
      if (_s != null && _s.indexOf("__secret__") != -1) {
        //System.out.println("wat: " + wat + " " + (new FileAbsPathHook()).__dynnsvars__.get("test"));
        FileAbsPathHook f = new FileAbsPathHook();
        f.test = "foo";
        System.out.println("wat: " + wat + " " + f.test);
        //System.out.println("wat: " + wat + " " + (new FileAbsPathHook()).test);
        System.out.println("__secret__ found in File::getAbsolutePath(), returning __notsecret__");
        ret = _s.replace("__secret__", "__notsecret__");
      }
    }

  }

}
