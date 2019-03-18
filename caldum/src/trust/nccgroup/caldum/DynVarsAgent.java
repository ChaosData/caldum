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

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import trust.nccgroup.caldum.annotation.Hook;

import java.lang.instrument.Instrumentation;
import java.util.Map;

import static java.lang.ClassLoader.getSystemClassLoader;
import static net.bytebuddy.matcher.ElementMatchers.*;
import static trust.nccgroup.caldum.asm.DynamicFields.DYNVARS;

public class DynVarsAgent {

  public static ResettableClassFileTransformer setup(Instrumentation inst) {

    AgentBuilder ab = new AgentBuilder.Default()
      .with(AgentBuilder.RedefinitionStrategy.REDEFINITION);

    AgentBuilder.Identified.Narrowable abn = ab.type(hasAnnotation(annotationType(ElementMatchers.<TypeDescription>named(Hook.class.getName()))));

    AgentBuilder.Identified.Extendable abe = abn.transform(new AgentBuilder.Transformer() {
      @Override
      public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
        if (typeDescription.getDeclaredFields().filter(named(DYNVARS)).size() == 0) {
          builder = builder.defineField(DYNVARS, Map.class, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
        }

        Class<?> alreadyInjectedClass = null;
        try {
          Class<?> old = getSystemClassLoader().getParent().loadClass(typeDescription.getName());
          if (old.getClassLoader() == null) {
            alreadyInjectedClass = old;
            System.out.println("DynVarsAgent:old: " + old);
            System.out.println("DynVarsAgent:old.getClassLoader(): " + old.getClassLoader());
          }
        } catch (ClassNotFoundException cnfe) {
          // don't add dynvar instrumentation since it's the first time
        }

        if (alreadyInjectedClass == null) {
          System.out.println("alreadyInjectedClass == null: " + typeDescription.getName());
        }

        return builder;
      }
    });

    return abe.installOn(inst);
  }


}
