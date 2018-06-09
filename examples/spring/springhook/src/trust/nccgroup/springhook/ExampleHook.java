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

package trust.nccgroup.springhook;

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

import java.util.*;
import java.util.logging.*;

public class ExampleHook {
  static Instrumentation inst = null;

  @Hook
  public static class ServletWrapper {

    public static class Settings {
      @Type
      static ElementMatcher typeMatcher() throws ClassNotFoundException {
        ElementMatcher.Junction<? extends TypeDescription> ret = none();
        for (Class<?> c : inst.getAllLoadedClasses()) {
          if ("javax.servlet.Servlet".equals(c.getName())) {
            ret = ret.or(isSubTypeOf(c));
          }
        }
        return ret;
      }

      @Member
      static ElementMatcher m = named("service").and(takesArguments(2));
    }

    @Inject
    public static Logger logger;

    @OnMethodEnter
    static void enter(@Origin Class clazz,
                      @This(optional = false) Object self,
                      @AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args) {
      Object req = args[0];

      try {
        String path = (String)req.getClass().getDeclaredMethod("getRequestURI").invoke(req);
        String query = (String)req.getClass().getDeclaredMethod("getQueryString").invoke(req);

        logger.info(">> " + path + "?" + query);
        //System.out.println("Hello AsiaSecWest!!");
      } catch (Throwable t) { }
    }

  }

}
