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

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

public class Entry {

  public static Logger logger = null;

  public static void agentmain(String agentArgs, Instrumentation inst) {
    setup(agentArgs, inst);
  }

  public static void premain(String agentArgs, Instrumentation inst) {
    setup(agentArgs, inst);
  }

  public static void setup(String agentArgs, Instrumentation inst) {

  }

  public static void unload() {

  }
}
