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

package trust.nccgroup.caldum;

import trust.nccgroup.caldum.annotation.*;
import trust.nccgroup.caldum.bluepill.JavaAgentHiderHooks;
import trust.nccgroup.caldum.global.State;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicBoolean;

public class Initialization {
  private static final AtomicBoolean initialized = new AtomicBoolean(false);

  private static final Class<?>[] injects = new Class[]{
    State.class, Hook.class, Debug.class, Dump.class, DumpWrappers.class,
    Dynamic.class, Wrapper.OnMethodEnter.class, Wrapper.OnMethodExit.class,
    DI.Inject.class, DI.AgentClassLoader.class, DI.Provide.class,
    DI.Provider.class, Matcher.class, Test.class
  };

  static void run(Instrumentation inst) {
    if (initialized.getAndSet(true)) {
      return;
    }

    //we have to inject (all of?) these in so that they can be
    //properly referenced from the bootstrap classloader versions
    //of hooks, especially the class @annotations.
    for (Class<?> c : injects) {
      try {
        BootstrapSwapInjector.inject(c, inst);
      } catch (IOException ignore) { }
    }

    // We try to hook every @Hook-annotated class on initial load so that we
    // can add add the __dynvars__ variable to it.
    DynVarsAgent.setup(inst);

    // Java agents are not directly aware of their own JAR files
    // due to this, we can't iterate the core JAR to scan for hooks
    // we manually load the @Hook-annotated classes and scan the
    // loaded classes from the system classloader.
    // We could use build-time code gen for this, but the overhead
    // is high, and would slow down Caldum builds more than I'm
    // looking for right now.

    Class<?>[] bluepill = new Class<?>[] {
      JavaAgentHiderHooks.RMXB.class
    };

    for (Class<?> c : bluepill) {
      try {
        ClassLoader.getSystemClassLoader().loadClass(c.getName());
      } catch (ClassNotFoundException ignore) { }

    }

    HookProcessor.process(
      inst,
      AgentLoader.class.getClassLoader(),
      AgentLoader.class.getPackage().getName(),
      null,
      true
    );
  }
}
