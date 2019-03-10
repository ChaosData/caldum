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
