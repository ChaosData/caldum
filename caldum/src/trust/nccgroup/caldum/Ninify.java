/*
Copyright 2019 NCC Group

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

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.utility.JavaType;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

public class Ninify {

  @SuppressWarnings("unchecked")
  public static <T extends ClassFileTransformer> Class<T> convertClassFileTransformer(Class<T> c) {
    try {
      // shamelessly copied from net.bytebuddy.agent.builder.AgentBuilder.Default.ExecutingTransformer.Factory.CreationAction::run
      return (Class<T>)new ByteBuddy()
        .with(TypeValidation.DISABLED)
        .subclass(c)
        .name(c.getName() + "$caldum$ModuleSupport")
        .method(named("transform").and(takesArgument(0, JavaType.MODULE.load())))
        .intercept(MethodCall.invoke(c.getDeclaredMethod("transform",
          Object.class,
          ClassLoader.class,
          String.class,
          Class.class,
          ProtectionDomain.class,
          byte[].class)).onSuper().withAllArguments())
        .make()
        .load(c.getClassLoader(),
          ClassLoadingStrategy.Default.WRAPPER_PERSISTENT
            .with(c.getProtectionDomain())
        )
        .getLoaded();
    } catch (Exception ignored) {
      return null;
    }
  }

}
