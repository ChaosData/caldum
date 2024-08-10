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
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.asm.MemberRemoval;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TypeResolutionStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import net.bytebuddy.implementation.auxiliary.TypeProxy;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.ClassWriter;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.JavaModule;
import trust.nccgroup.caldum.annotation.DI;
import trust.nccgroup.caldum.annotation.Dynamic;
import trust.nccgroup.caldum.annotation.Hook;
import trust.nccgroup.caldum.asm.DynamicFields;
import trust.nccgroup.caldum.asm.NopByteCodeAppender;
import trust.nccgroup.caldum.util.TmpLogger;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.ClassLoader.getSystemClassLoader;
import static net.bytebuddy.jar.asm.Opcodes.NOP;
import static net.bytebuddy.matcher.ElementMatchers.*;
import static trust.nccgroup.caldum.asm.DynamicFields.DYNVARS;
import static trust.nccgroup.caldum.asm.DynamicFields.DYNANNOS;
import static trust.nccgroup.caldum.asm.DynamicFields.DYNNSVARS;

public class DynVarsAgent {
  private static final Logger logger = TmpLogger.DEFAULT;

//  public static int halfcounter = 0;
  private static final Object lock = new Object();

  //note: i was originally thinking of stashing something like __dynannos__ in each @Dynamic @Hook class,
  //      but for now, using a single locked map works ok. will still probably eventually do the former,
  //      but then i'll probably want to convert to actual annotation objects instead of sort of string/ast
  //      wrapping around them.
  public static Map<String,Map<String,AnnotationList>> annotations = new HashMap<String,Map<String,AnnotationList>>();

  public static Map<String,AnnotationList> getAnnomap(String className) {
    Map<String,AnnotationList> mal = null;
    synchronized (lock) {
      mal = annotations.get(className);
    }
    if (mal != null) {
      logger.info("getAnnomap for " + className);
    }
    return mal;
  }

  public static void putAnnomap(String className, Map<String,AnnotationList> annomap) {
    logger.info("putAnnomap for " + className);
    synchronized (lock) {
      annotations.put(className, annomap);
    }
  }

  public static ResettableClassFileTransformer setup(Instrumentation inst) {

    AgentBuilder ab = new AgentBuilder.Default()
      .with(AgentBuilder.RedefinitionStrategy.DISABLED) // we don't want to do this for classes that have already been loaded
      .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
      .with(AgentBuilder.InitializationStrategy.Minimal.INSTANCE)
        .with(new AgentBuilder.Listener.WithErrorsOnly(AgentBuilder.Listener.StreamWriting.toSystemError()))
        //.with(new AgentBuilder.Listener.WithTransformationsOnly(AgentBuilder.Listener.StreamWriting.toSystemOut()))
        .with(new AgentBuilder.FallbackStrategy() {
          @Override
          public boolean isFallback(Class<?> type, Throwable throwable) {
            System.err.println("isFallback called: type:" + type.getName() + ", throwable:" + throwable);
            throwable.printStackTrace();
            return true;
          }
        })
        .with(AgentBuilder.InstallationListener.StreamWriting.toSystemOut())

        //.with(ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR)
    //.disableClassFormatChanges() // unclear right now what bb is doing, but it's causing a lot of issues for merely returning the builder w/o applying anything
      //.with(new AgentBuilder.Listener.StreamWriting(System.err));
      ;

    AgentBuilder.Identified.Narrowable abn = ab.type(
      hasAnnotation(annotationType(ElementMatchers.<TypeDescription>named(Hook.class.getName())))
        .and(hasAnnotation(annotationType(ElementMatchers.<TypeDescription>named(Dynamic.class.getName())))));

    AgentBuilder.Identified.Extendable abe = abn.transform(new AgentBuilder.Transformer() {
      @Override
      public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, ProtectionDomain domain) {

//        if (typeDescription.getDeclaredFields().filter(named(DYNVARS)).size() > 0) {
//          logger.info("already transformed " + typeDescription.getName() + ", bailing out");
//          return builder;
//        } else {
//          logger.info("transforming " + typeDescription.getName());
//        }

//        try {
//          Map<TypeDescription, File> m = builder.make().saveIn(new File("./.dynvars-pre"));
//        } catch (IOException e) {
//          throw new RuntimeException(e);
//        }

        //if (builder.invokable(isTypeInitializer()).) {

        //}

        // iterate fields for annotations
        Map<String, AnnotationList> annomap = new HashMap<String, AnnotationList>();
        for (FieldDescription.InDefinedShape field : typeDescription.getDeclaredFields()) {
          String name = field.getActualName();
          AnnotationList annos = field.getDeclaredAnnotations();
          annomap.put(name, annos);
//          for (AnnotationDescription anno : annos) {
//            TypeDescription atd = anno.getAnnotationType();
//            FieldList<FieldDescription.InDefinedShape> atdfs = atd.getDeclaredFields();
//            for (FieldDescription.InDefinedShape atdf : atdfs) {
//
//            }
//          }
        }
        putAnnomap(typeDescription.getName(), annomap);

        Map<String, Annotation[]> annomap2 = new HashMap<String, Annotation[]>();
        for (FieldDescription.InDefinedShape field : typeDescription.getDeclaredFields()) {
          String fieldname = field.getActualName();
          AnnotationList annos = field.getDeclaredAnnotations();
          Annotation[] annoarr = new Annotation[annos.size()];
          for (int i = 0; i < annoarr.length; i++) {
            AnnotationDescription ad = annos.get(i);
            String classname = ad.getAnnotationType().getActualName();
            Class<? extends Annotation> clazz = null;
            try {
              Class<?> _clazz = Class.forName(classname);
              if (_clazz.isAssignableFrom(Annotation.class)) {
                clazz = _clazz.asSubclass(Annotation.class);
              }
            } catch (ClassNotFoundException e) {
              logger.log(Level.SEVERE, "could not find class", e);
            } catch (ClassCastException e) {
              logger.log(Level.SEVERE, "failed to cast annotation class to Class<Annotation>", e);
            }
            if (clazz != null) {
              annoarr[i] = ad.prepare(clazz).load();
            }
          }
          annomap2.put(fieldname, annoarr);
        }

        try {
          Class<?> old = getSystemClassLoader().getParent().loadClass(typeDescription.getName());
          if (old.getClassLoader() == null) {
            //alreadyInjectedClass = old;
            logger.info("DynVarsAgent:old: " + old);
            logger.info("DynVarsAgent:old.getClassLoader(): " + old.getClassLoader()); // generally null for bootstrap
          }
        } catch (ClassNotFoundException cnfe) {
          // don't add dynvar instrumentation since it's the first time // ???? why?
          logger.info("first!");
          //return builder;
        }

        if (typeDescription.getDeclaredFields().filter(named(DYNVARS)).isEmpty()) {
          //if ((halfcounter % 2) == 1) {
          logger.info("adding __dynvars__ to class: " + typeDescription.getName() + " (from classloader: " + classLoader + ")");
          builder = builder.defineField(DYNVARS, Map.class, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
          //} else {
          //  logger.info("not adding __dynvars__ to class: " + typeDescription.getName() + " (from classloader: " + classLoader + ")");
          //}
          //halfcounter += 1;
        } else {
          logger.info("not adding __dynvars__ to class (has at least one): " + typeDescription.getName() + " (from classloader: " + classLoader + ")");
        }
//        //todo: implement DynamicFields impl for access to non-static fields
        if (typeDescription.getDeclaredFields().filter(named(DYNNSVARS)).isEmpty()) {
          logger.info("adding __dynnsvars__ to class: " + typeDescription.getName() + " (from classloader: " + classLoader + ")");
          builder = builder.defineField(DYNNSVARS, Map.class, Opcodes.ACC_PUBLIC);
        } else {
          logger.info("not adding __dynnsvars__ to class (has at least one): " + typeDescription.getName() + " (from classloader: " + classLoader + ")");
        }

        // we need to force a static/type initializer into existence if one doesn't exist
        // it's unclear from the bytebuddy api if there's a good wey to figure out if one
        // is already there to begin with, so adding a NOP to an existing one if it's there should be relatively safe.
        // note: we may not need this now that we're adding an initializer for __dynannos__. (nope, not yet at least)
        builder = builder.initializer(new NopByteCodeAppender());

        if (typeDescription.getDeclaredMethods().filter(
              ElementMatchers.<MethodDescription.InDefinedShape>isConstructor().or(
                ElementMatchers.<MethodDescription.InDefinedShape>isDefaultConstructor())
            ).isEmpty()) {

          Constructor<?>[] cs = null;
          TypeDescription.Generic supe = typeDescription.getSuperClass();
          if (supe != null) {
            cs = supe.getClass().getDeclaredConstructors();
          } else {
            cs = new Constructor[]{};
          }

          if (cs.length > 0) {
//            for (Constructor<?> c : cs) {
//              if (c.getParameterTypes().length == 0) {
//                builder = builder.defineConstructor(Visibility.PUBLIC).intercept(MethodCall.invoke(c).onSuper());
//                break;
//              }
//            }
            builder = builder.defineConstructor(Visibility.PUBLIC).intercept(SuperMethodCall.INSTANCE);
          } else {
            builder = builder.defineConstructor(Visibility.PUBLIC).intercept(new Implementation() {
              @Override
              public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return NoOp.INSTANCE.prepare(instrumentedType);
              }

              @Override
              public ByteCodeAppender appender(Target implementationTarget) {
                return new NopByteCodeAppender();
              }
            });
          }
        }

        builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods()
            .writerFlags(ClassWriter.COMPUTE_MAXS)
            .invokable(ElementMatchers.isTypeInitializer(),
                DynamicFields.methodVisitor(DynamicFields.Type.TypeInitializer)
            ));

        builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods()
            .writerFlags(ClassWriter.COMPUTE_MAXS)
            .invokable(ElementMatchers.isConstructor(),
                DynamicFields.methodVisitor(DynamicFields.Type.Constructor)
            ));

        builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods()
            .invokable(any().and(isMethod().and(not(isTypeInitializer()).and(not(isConstructor())))),
                DynamicFields.methodVisitor(DynamicFields.Type.Method)));

//        logger.info("removing all other fields: " + typeDescription.getName() + " (from classloader: " + classLoader + ")");
//        builder = builder.visit(new MemberRemoval().stripFields(not(named(DYNVARS))));
        logger.info("removing all other static fields: " + typeDescription.getName() + " (from classloader: " + classLoader + ")");
        builder = builder.visit(new MemberRemoval().stripFields(ElementMatchers.<FieldDescription.InDefinedShape>isStatic().and(not(named(DYNVARS).or(named(DYNANNOS))))));


        if (typeDescription.getDeclaredFields().filter(named(DYNANNOS)).isEmpty()) {
          builder = builder.defineField(DYNANNOS, Map.class, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
        }
        builder = builder.visit(new MemberRemoval().stripFields(not(ElementMatchers.<FieldDescription.InDefinedShape>isStatic()).and(not(named(DYNNSVARS)))));

        //this doesn't appear to actually work or do anything
        //builder = builder.initializer(new LoadedTypeInitializer.ForStaticField(DYNANNOS, annomap2));

        System.out.println("//////////////// got to end of builder");
        logger.info("//////////////// got to end of builder");

        return builder;
      }
    });

//    abe = abe.transform(new AgentBuilder.Transformer() {
//      @Override
//      public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, ProtectionDomain domain) {
//        try {
//          //todo: do this only when annotated for it
//          Map<TypeDescription, File> m = builder.make().saveIn(new File("./.dynvars"));
//          /*for (Map.Entry<TypeDescription, File> kv : m.entrySet()) {
//            System.out.println(kv.getValue());
//          }*/
//        } catAgentBuilder.Listener.StreamWriting.toSystemError()ch (IOException e) {
//          e.printStackTrace();
//        }
//        return builder;
//      }
//    });




    return abe.installOn(inst);
  }


}
