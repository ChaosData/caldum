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
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.asm.MemberRemoval;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.jar.asm.ClassWriter;
import net.bytebuddy.jar.asm.FieldVisitor;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.JavaModule;
import trust.nccgroup.caldum.annotation.Dynamic;
import trust.nccgroup.caldum.annotation.Hook;
import trust.nccgroup.caldum.asm.DynamicFields;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Map;

import static java.lang.ClassLoader.getSystemClassLoader;
import static net.bytebuddy.matcher.ElementMatchers.*;
import static trust.nccgroup.caldum.asm.DynamicFields.DYNVARS;

public class DynVarsAgent {

  public static ResettableClassFileTransformer setup(Instrumentation inst) {

    AgentBuilder ab = new AgentBuilder.Default()
      .with(AgentBuilder.RedefinitionStrategy.DISABLED) // we don't want to do this for classes that have already been loaded
      //.disableClassFormatChanges() // unclear right now what bb is doing, but it's causing a lot of issues for merely returning the builder w/o applying anything
      .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
      .with(new AgentBuilder.Listener.StreamWriting(System.err));
      ;

    AgentBuilder.Identified.Narrowable abn = ab.type(
      hasAnnotation(annotationType(ElementMatchers.<TypeDescription>named(Hook.class.getName())))
        .and(hasAnnotation(annotationType(ElementMatchers.<TypeDescription>named(Dynamic.class.getName())))));

    AgentBuilder.Identified.Extendable abe = abn.transform(new AgentBuilder.Transformer() {
      @Override
      public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {

        if (typeDescription.getDeclaredFields().filter(named(DYNVARS)).size() > 0) {
          System.out.println("already transformed " + typeDescription.getName() + ", bailing out");
          return builder;
        } else {
          System.out.println("transforming " + typeDescription.getName());
        }

        try {
          Class<?> old = getSystemClassLoader().getParent().loadClass(typeDescription.getName());
          if (old.getClassLoader() == null) {
            //alreadyInjectedClass = old;
            System.out.println("DynVarsAgent:old: " + old);
            //System.out.println("DynVarsAgent:old.getClassLoader(): " + old.getClassLoader()); // generally null for bootstrap
          }
        } catch (ClassNotFoundException cnfe) {
          // don't add dynvar instrumentation since it's the first time // ???? why?
          //System.out.println("first!");
          //return builder;
        }

        /*if (alreadyInjectedClass == null) {
          System.out.println("alreadyInjectedClass == null: " + typeDescription.getName());
        }*/

        //System.out.println("!!!!");

        if (typeDescription.getDeclaredFields().filter(named(DYNVARS)).size() == 0) {
          System.out.println("adding __dynvars__ to class: " + typeDescription.getName() + " (from classloader: " + classLoader + ")");
          builder = builder.defineField(DYNVARS, Map.class, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
        }



//        builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods()
//          .method(isAnnotatedWith(Advice.OnMethodEnter.class)
//              .or(ElementMatchers.isAnnotatedWith(Advice.OnMethodExit.class)),
//            new AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper() {
//              @Override
//              public MethodVisitor wrap(TypeDescription instrumentedType,
//                                        MethodDescription instrumentedMethod,
//                                        MethodVisitor methodVisitor,
//                                        Implementation.Context implementationContext,
//                                        TypePool typePool, int writerFlags, int readerFlags) {
//                System.out.println("ForDeclaredMethods.MethodVisitorWrapper wrap");
//                return new DynamicFields(instrumentedType, instrumentedMethod, methodVisitor);
//              }
//            }));

//        builder = builder.visit(new AsmVisitorWrapper.ForDeclaredFields().field(any(), new AsmVisitorWrapper.ForDeclaredFields.FieldVisitorWrapper() {
//          @Override
//          public FieldVisitor wrap(TypeDescription typeDescription, FieldDescription.InDefinedShape inDefinedShape, FieldVisitor fieldVisitor) {
//            System.out.println("FieldVisitorWrapper wrap 1: " + typeDescription.toString());
//            return fieldVisitor;
//          }
//        }));

//        builder = builder.visit(new MemberRemoval().stripFields(not(named(DYNVARS))));

//        builder = builder.visit(new AsmVisitorWrapper.ForDeclaredFields().field(any(), new AsmVisitorWrapper.ForDeclaredFields.FieldVisitorWrapper() {
//          @Override
//          public FieldVisitor wrap(TypeDescription typeDescription, FieldDescription.InDefinedShape inDefinedShape, FieldVisitor fieldVisitor) {
//            System.out.println("FieldVisitorWrapper wrap 2: " + typeDescription.toString());
//            return fieldVisitor;
//          }
//        }));

//        return builder;
//      }
//    });

//    abe = abe.transform(new AgentBuilder.Transformer() {
//      @Override
//      public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
        System.out.println("dynamic fields w/ initializer: " + typeDescription.getName() + " (from classloader: " + classLoader + ")");

        builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods()
          .writerFlags(ClassWriter.COMPUTE_MAXS)
          .invokable(ElementMatchers.isTypeInitializer(),
            new AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper() {
              @Override
              public MethodVisitor wrap(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        MethodVisitor methodVisitor,
                                        Implementation.Context implementationContext,
                                        TypePool typePool, int writerFlags, int readerFlags) {
                return new DynamicFields(instrumentedType, instrumentedMethod, methodVisitor, true);
              }
            }));
//      }
//    });

//    abe = abe.transform(new AgentBuilder.Transformer() {
//      @Override
//      public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
//        System.out.println("dynamic fields w/o initializer: " + typeDescription.getName() + " (from classloader: " + classLoader + ")");

        builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods()
          .invokable(any().and(not(isTypeInitializer())),
//          .method(isAnnotatedWith(Advice.OnMethodEnter.class)
//              .or(ElementMatchers.isAnnotatedWith(Advice.OnMethodExit.class)),
            new AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper() {
              @Override
              public MethodVisitor wrap(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        MethodVisitor methodVisitor,
                                        Implementation.Context implementationContext,
                                        TypePool typePool, int writerFlags, int readerFlags) {
                System.out.println("ForDeclaredMethods.MethodVisitorWrapper wrap");
                return new DynamicFields(instrumentedType, instrumentedMethod, methodVisitor, false);
              }
            }));
//      }
//    });

//    abe = abe.transform(new AgentBuilder.Transformer() {
//      @Override
//      public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
        System.out.println("removing all other fields: " + typeDescription.getName() + " (from classloader: " + classLoader + ")");
        builder = builder.visit(new MemberRemoval().stripFields(not(named(DYNVARS))));
        //return builder.visit(new MemberRemoval().stripFields(named("zzz")));

//        builder = builder.visit(new MemberRemoval().stripFields(any()));
        //builder = builder.defineField(DYNVARS, Map.class, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);

        return builder;
      }
    });

    abe = abe.transform(new AgentBuilder.Transformer() {
      @Override
      public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
        try {
          Map<TypeDescription, File> m = builder.make().saveIn(new File("./"));
          for (Map.Entry<TypeDescription, File> kv : m.entrySet()) {
            System.out.println(kv.getValue());
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
        return builder;
      }
    });


    return abe.installOn(inst);
  }


}
