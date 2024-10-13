/*
Copyright 2018-2019 NCC Group
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

package trust.nccgroup.caldum;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.asm.MemberRemoval;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.attribute.AnnotationRetention;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.JavaModule;
import trust.nccgroup.caldum.annotation.*;
import trust.nccgroup.caldum.asm.DynamicFields;
import trust.nccgroup.caldum.util.CompatHelper;
import trust.nccgroup.caldum.util.TmpLogger;

import java.lang.annotation.Annotation;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.ClassLoader.getSystemClassLoader;
import static net.bytebuddy.matcher.ElementMatchers.*;
import static trust.nccgroup.caldum.asm.DynamicFields.DYNVARS;

public class PluggableAdviceAgent {
  private static final Logger logger = TmpLogger.DEFAULT;

  public static class Builder {

    private AgentBuilder.RawMatcher rawMatcher = null;

    private ElementMatcher<? super TypeDescription> typeMatcher = null;
    private ElementMatcher<? super ClassLoader> classLoaderMatcher = any();
    private ElementMatcher<? super JavaModule> moduleMatcher = any();

    private ElementMatcher<? super MethodDescription> memberMatcher = null;

    private ElementMatcher<?>[] ignoreMatchers = null;

    private boolean test = false;
    private boolean debug = false;
    private boolean dump = false;
    private boolean dump_wrappers = false;

    private Class<?>[] wrappers = new Class<?>[]{};

    private AgentBuilder.RedefinitionStrategy redefinitionStrategy = AgentBuilder.RedefinitionStrategy.RETRANSFORMATION;

    private Class<?> hookClass = null;

    public Builder() {
    }

    public Builder rawMatcher(AgentBuilder.RawMatcher _rawMatcher) {
      rawMatcher = _rawMatcher;
      return this;
    }

    @SuppressWarnings("unchecked")
    public Builder typeMatcher(ElementMatcher<?> _typeMatcher) {
      typeMatcher = (ElementMatcher<? super TypeDescription>) _typeMatcher;
      return this;
    }

    @SuppressWarnings("unchecked")
    public Builder classLoaderMatcher(ElementMatcher<?> _classLoaderMatcher) {
      classLoaderMatcher = (ElementMatcher<? super ClassLoader>) _classLoaderMatcher;
      return this;
    }

    @SuppressWarnings("unchecked")
    public Builder moduleMatcher(ElementMatcher<?> _moduleMatcher) {
      moduleMatcher = (ElementMatcher<? super JavaModule>) _moduleMatcher;
      return this;
    }

    @SuppressWarnings("unchecked")
    public Builder memberMatcher(ElementMatcher<?> _memberMatcher) {
      memberMatcher = (ElementMatcher<? super MethodDescription>) _memberMatcher;
      return this;
    }

    public Builder ignoreMatchers(ElementMatcher<?>[] _ignoreMatchers) {
      ignoreMatchers = _ignoreMatchers;
      return this;
    }

    @SuppressWarnings("unchecked")
    private static AgentBuilder setIgnore(AgentBuilder ab, ElementMatcher<?>[] ignoreMatchers) {
      if (ignoreMatchers.length == 1) {
        ab = ab.ignore((ElementMatcher<? super TypeDescription>) ignoreMatchers[0]);
      } else if (ignoreMatchers.length == 2) {
        ab = ab.ignore(
          (ElementMatcher<? super TypeDescription>) ignoreMatchers[0],
          (ElementMatcher<? super ClassLoader>) ignoreMatchers[1]
        );
      } else if (ignoreMatchers.length == 3) {
        ab = ab.ignore(
          (ElementMatcher<? super TypeDescription>) ignoreMatchers[0],
          (ElementMatcher<? super ClassLoader>) ignoreMatchers[1],
          (ElementMatcher<? super JavaModule>) ignoreMatchers[2]
        );
      }
      return ab;
    }

    public Builder test(boolean _test) {
      test = _test;
      return this;
    }

    public Builder debug(boolean _debug) {
      debug = _debug;
      return this;
    }

    public Builder dump(boolean _dump) {
      dump = _dump;
      return this;
    }

    public Builder dump_wrappers(boolean _dump_wrappers) {
      dump_wrappers = _dump_wrappers;
      return this;
    }

    public Builder wrappers(Class<?>[] _wrappers) {
      wrappers = _wrappers;
      return this;
    }

    public Builder hookClass(Class<?> _hookClass) {
      hookClass = _hookClass;
      return this;
    }

    public static Builder fromClass(Class<?> configClass) throws BuildException {
      Builder builder = new Builder();

      Class<?> hook = configClass.getDeclaringClass();
      Hook h = hook.getAnnotation(Hook.class);
      if (h == null) {
        throw new BuildException("parent class is not @Hook");
      }

      builder.hookClass(hook);
      builder.wrappers(h.wrappers());
      if (hook.getAnnotation(Test.class) != null || (Test.Bootstrap.INSTANCE != null && hook.getAnnotation(Test.Bootstrap.INSTANCE) != null)) {
        builder.test(true);
      }
      if (hook.getAnnotation(Debug.class) != null || (Debug.Bootstrap.INSTANCE != null && hook.getAnnotation(Debug.Bootstrap.INSTANCE) != null)) {
        builder.debug(true);
      }
      if (hook.getAnnotation(Dump.class) != null || (Dump.Bootstrap.INSTANCE != null && hook.getAnnotation(Dump.Bootstrap.INSTANCE) != null)) {
        builder.dump(true);
      }
      if (hook.getAnnotation(DumpWrappers.class) != null || (DumpWrappers.Bootstrap.INSTANCE != null && hook.getAnnotation(DumpWrappers.Bootstrap.INSTANCE) != null)) {
        builder.dump_wrappers(true);
      }

      for (Field field : configClass.getDeclaredFields()) {
        if (!Modifier.isStatic(field.getModifiers())) {
          continue;
        }
        //note: java 9+ complain about isAccessible, but we can't replace it w/ canAccess (9+) due to supporting 6-8
        //todo: wrap with a multi-release class
//        if (!field.isAccessible()) {
//          field.setAccessible(true);
//        }
        CompatHelper.trySetAccessible(field);

        Annotation[] annos = field.getDeclaredAnnotations();

        if (annos.length != 1) {
          continue;
        }

        Annotation anno = annos[0];

        Object value;
        try {
          value = field.get(null);
        } catch (IllegalAccessException e) {
          continue;
        }
        if (value == null) {
          continue;
        }

        builder.matcher(field, null, anno, value);
      }

      for (Method method : configClass.getDeclaredMethods()) {
        if (!Modifier.isStatic(method.getModifiers())) {
          continue;
        }
//        if (!method.isAccessible()) {
//          method.setAccessible(true);
//        }
        CompatHelper.trySetAccessible(method);


        Annotation[] annos = method.getDeclaredAnnotations();

        if (annos.length != 1) {
          continue;
        }

        Annotation anno = annos[0];

        Object value;
        try {
          value = method.invoke(null);
        } catch (IllegalAccessException e) {
          continue;
        } catch (InvocationTargetException e) {
          throw new BuildException("failed to execute matcher generator", e.getCause());
        }
        if (value == null) {
          continue;
        }

        builder.matcher(null, method, anno, value);
      }

      return builder;
    }

    private Builder matcher(Field field, Method method, Annotation anno, Object value) throws BuildException {
      try {
        if (anno instanceof Matcher.Type) {
          typeMatcher((ElementMatcher<?>) value);
        } else if (anno instanceof Matcher.Member) {
          memberMatcher((ElementMatcher<?>) value);
        } else if (anno instanceof Matcher.Raw) {
          rawMatcher((AgentBuilder.RawMatcher) value);
        } else if (anno instanceof Matcher.Loader) {
          classLoaderMatcher((ElementMatcher<?>) value);
        } else if (anno instanceof Matcher.Module) {
          moduleMatcher((ElementMatcher<?>) value);
        } else if (anno instanceof Matcher.Ignore) {
          if (value.getClass().isArray()) {
            ignoreMatchers((ElementMatcher<?>[]) value);
          } else {
            ignoreMatchers(new ElementMatcher<?>[]{(ElementMatcher<?>) value});
          }
        }
      } catch (ClassCastException cce) {
        if (field != null) {
          throw new BuildException("wrong matcher annotation for " + field.toGenericString());
        } else if (method != null) {
          throw new BuildException("wrong matcher annotation for " + method.toGenericString());
        } else {
          throw new BuildException("wrong matcher annotation for ???");
        }

      }

      return this;
    }

    public ResettableClassFileTransformer build(Instrumentation inst) throws BuildException {
      return build(inst, null);
    }

    public ResettableClassFileTransformer build(Instrumentation inst, Class<?> alreadyInjectedClass)
      throws BuildException {
      if (typeMatcher == null && rawMatcher == null) {
        throw new BuildException("no type matcher set");
      }

      if (memberMatcher == null) {
        throw new BuildException("no method/constructor matcher set");
      }

      if (hookClass == null) {
        throw new BuildException("no hook class set");
      }

      AgentBuilder ab = new AgentBuilder.Default()
        .disableClassFormatChanges()
        .with(redefinitionStrategy);

      if (ignoreMatchers != null) {
        ab = setIgnore(ab, ignoreMatchers);
      }

      AgentBuilder.Identified.Narrowable abn;
      if (rawMatcher == null) {
        rawMatcher = new AgentBuilder.RawMatcher.ForElementMatchers(typeMatcher, classLoaderMatcher, not(supportsModules()).or(moduleMatcher));
      }

      DumpingListener dl = null;

      if (debug || dump) {
        dl = new DumpingListener(debug ? System.out : null, dump, rawMatcher, inst);
        ab = ab.with((AgentBuilder.Listener)dl);
        ab = ab.with((AgentBuilder.TransformerDecorator)dl);
      }
      abn = ab.type(rawMatcher);


      byte[] alt_hook_bytes = null;

      DynamicType.Builder<?> dtb = new ByteBuddy()
        .with(InstrumentedType.Factory.Default.FROZEN)
        .with(Implementation.Context.Disabled.Factory.INSTANCE) // don't add method for static init
        .with(AnnotationRetention.ENABLED)
        .redefine(hookClass, ClassFileLocator.ForInstrumentation.of(inst, hookClass));

      // this is moved to DynVarsAgent
      //dtb = dtb.defineField(DYNVARS, Map.class, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);


      if (alreadyInjectedClass != null || test) {
        /* // remove for now
        final Class<?> _alreadyInjectedClass = alreadyInjectedClass;
        // add dynvar instrumentation to the to be injected class
        dtb = dtb.visit(new AsmVisitorWrapper.ForDeclaredMethods()
          .method(isAnnotatedWith(Advice.OnMethodEnter.class)
              .or(ElementMatchers.isAnnotatedWith(Advice.OnMethodExit.class)),
            new AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper() {
              @Override
              public MethodVisitor wrap(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        MethodVisitor methodVisitor,
                                        Implementation.Context implementationContext,
                                        TypePool typePool, int writerFlags, int readerFlags) {
                return new DynamicFields(hookClass, _alreadyInjectedClass, instrumentedType, instrumentedMethod, methodVisitor);
              }
            }));
        */

        // delete all fields and add all the ones from the existing one
        if (alreadyInjectedClass != null) {
          logger.info("alreadyInjectedClass: " + alreadyInjectedClass);
          //dtb = dtb.visit(new MemberRemoval().stripFields(not(named(DYNVARS))));

          /* // remove for now
          Set<String> previousFields = new HashSet<String>();
          for (Field pf : alreadyInjectedClass.getDeclaredFields()) {
            previousFields.add(pf.getName());
          }
          previousFields.add(DYNVARS);

          dtb = dtb.visit(new MemberRemoval().stripFields(not(anyOf(previousFields.toArray()))));
          */

          /*alt_hook_bytes = dtb.make().getBytes();

          dtb = new ByteBuddy()
            .with(InstrumentedType.Factory.Default.MODIFIABLE)
            .with(Implementation.Context.Disabled.Factory.INSTANCE) // don't add method for static init
            .with(AnnotationRetention.ENABLED)
            .redefine(hookClass, new WrappedClassFileLocator(hookClass.getName(), alt_hook_bytes));

          for (Field f : alreadyInjectedClass.getDeclaredFields()) {
            if (!DYNVARS.equals(f.getName())) {
              //dtb = dtb.defineField(f.getName(), f.getType(), f.getModifiers());
            }
          }*/
        }


      }

      if (wrappers.length > 0 && wrappers[0] != void.class) {
        for (Class<?> hook_wrapper : wrappers) {
          if (hook_wrapper == void.class) {
            continue;
          }

          Class<?>[] nested = hook_wrapper.getDeclaredClasses();

          if (nested.length == 0) {
            dtb = dtb.visit(Advice.to(hook_wrapper, ClassFileLocator.ForInstrumentation.of(inst, hook_wrapper)).on(
              isMethod().and(isAnnotatedWith(Advice.OnMethodEnter.class).or(ElementMatchers.isAnnotatedWith(Advice.OnMethodExit.class)))
            ));
          } else {
            for (Class<?> nc : nested) {
              if (nc.getAnnotation(Wrapper.OnMethodEnter.class) != null) {
                dtb = dtb.visit(Advice.to(nc, ClassFileLocator.ForInstrumentation.of(inst, nc)).on(isMethod().and(isAnnotatedWith(Advice.OnMethodEnter.class))));
              }
              if (nc.getAnnotation(Wrapper.OnMethodExit.class) != null) {
                dtb = dtb.visit(Advice.to(nc, ClassFileLocator.ForInstrumentation.of(inst, nc)).on(isMethod().and(isAnnotatedWith(Advice.OnMethodExit.class))));
              }
            }
          }
        }
      }
      alt_hook_bytes = dtb.make().getBytes();

      AgentBuilder.Identified.Extendable abe = null;

      if (alt_hook_bytes == null) {
        try {
          abe = abn.transform(
              new AdviceTransformer(inst, hookClass, memberMatcher)
          );
        } catch (Throwable t) {
          logger.log(Level.SEVERE, "AdviceTransformer error", t);
        }
      } else {
        if (dump_wrappers) {
          SimpleDumpingClassFileTransformer.dump(inst, hookClass, "");
          SimpleDumpingClassFileTransformer.dump(inst, hookClass, ".wrapped", alt_hook_bytes);
        }
        try {
          abe = abn.transform(
              new AdviceTransformer(hookClass, alt_hook_bytes, memberMatcher)
          );
        } catch (Throwable t) {
          logger.log(Level.SEVERE, "AdviceTransformer error", t);
        }
      }
      //if (dump) {
      //  return abe.installOn(inst, dl);
      //} else {

      if (abe == null) {
        return null;
      }
      return abe.installOn(inst);
      //}

    }

  }

  static class BuildException extends Exception {

    BuildException(String msg) {
      super(msg);
    }

    BuildException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }


  private static class AdviceTransformer implements AgentBuilder.Transformer {

    AsmVisitorWrapper avw;
    //private ElementMatcher<? super MethodDescription> memberMatcher;

    AdviceTransformer(Instrumentation inst, Class<?> hookClass,
                ElementMatcher<? super MethodDescription> _memberMatcher) {
      //memberMatcher = _memberMatcher;
      avw = Advice.to(hookClass, ClassFileLocator.ForInstrumentation.of(inst, hookClass)).on(_memberMatcher);
    }

    AdviceTransformer(Class<?> hookClass, byte[] alt_hook_bytes,
                      ElementMatcher<? super MethodDescription> _memberMatcher) {
      //memberMatcher = _memberMatcher;

      avw = Advice.to(
        hookClass,
        new WrappedClassFileLocator(hookClass.getName(), alt_hook_bytes)
      ).on(_memberMatcher);
    }

    @Override
    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                            TypeDescription typeDescription,
                                            ClassLoader classLoader,
                                            JavaModule module,
                                            ProtectionDomain domain) {
      //builder.method(memberMatcher).intercept()
      return builder.visit(avw);
    }
  }

}
