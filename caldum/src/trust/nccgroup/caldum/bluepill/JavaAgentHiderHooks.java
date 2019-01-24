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

package trust.nccgroup.caldum.bluepill;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatcher;
import trust.nccgroup.caldum.annotation.Hook;
import trust.nccgroup.caldum.annotation.Matcher;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class JavaAgentHiderHooks {

  @Hook // Caldum-internal @Hooks are loaded manually in Initialization::run
  public static class RMXB {

    public static List<String> vmArgs = null;
    public static Object lock = new Object();

    public static class Settings {
      @Matcher.Type
      static ElementMatcher typeMatcher() {
        RuntimeMXBean rmxb = ManagementFactory.getRuntimeMXBean();
        return is(rmxb.getClass());
      }

      @Matcher.Member
      static ElementMatcher m = named("getInputArguments");

      @Matcher.Ignore
      static ElementMatcher i = none();
    }

    @Advice.OnMethodExit(onThrowable = Exception.class)
    static void exit(@Advice.Return (readOnly = false) List<String> value) {
      synchronized (lock) {
        if (vmArgs == null) {
          List<String> n = new ArrayList<String>();
          for (String s : value) {
            if (s.startsWith("-javaagent:")) {
              continue;
            }
            n.add(s);
          }
          vmArgs = Collections.unmodifiableList(n); // from VMManagementImpl::getVmArguments
        }
        value = vmArgs;
      }
    }
  }


}
