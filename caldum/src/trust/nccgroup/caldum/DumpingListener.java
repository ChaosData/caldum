package trust.nccgroup.caldum;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bytecode.Throw;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.JavaType;
import trust.nccgroup.caldum.util.TmpLogger;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

public class DumpingListener extends AgentBuilder.Listener.StreamWriting implements AgentBuilder.TransformerDecorator {

  private static final Logger logger = TmpLogger.DEFAULT;

  public static class OriginalDumpingClassFileTransformer implements ResettableClassFileTransformer {

    private final AgentBuilder.RawMatcher matcher;
    private ResettableClassFileTransformer classFileTransformer;
    private final Method[] transforms;

    private static final AgentBuilder.CircularityLock DEFAULT_LOCK = new AgentBuilder.CircularityLock.Default();

    private final AgentBuilder.ClassFileBufferStrategy classFileBufferStrategy = AgentBuilder.ClassFileBufferStrategy.Default.RETAINING;
    private final AgentBuilder.LocationStrategy locationStrategy = AgentBuilder.LocationStrategy.ForClassLoader.STRONG;
    private final AgentBuilder.PoolStrategy poolStrategy = AgentBuilder.PoolStrategy.Default.FAST;
    private final AgentBuilder.DescriptionStrategy descriptionStrategy = AgentBuilder.DescriptionStrategy.Default.HYBRID;
    private final AgentBuilder.CircularityLock circularityLock = DEFAULT_LOCK;

    // TODO: figure out how to get byte-buddy to generate the subclass within the same module so we can reflect on it
    public OriginalDumpingClassFileTransformer(AgentBuilder.RawMatcher _matcher, ResettableClassFileTransformer _classFileTransformer) {
      matcher = _matcher;
      classFileTransformer = _classFileTransformer;
      transforms = new Method[2];

      Class<?> c = classFileTransformer.getClass();
      Method[] ms = c.getDeclaredMethods();
      for (Method m : ms) {
        if ("transform".equals(m.getName()) && byte[].class.equals(m.getReturnType())) {
          Class<?>[] pts = m.getParameterTypes();
          if (pts.length == 5) {
            if (ClassLoader.class.equals(pts[0])
              && String.class.equals(pts[1])
              && Class.class.equals(pts[2])
              && ProtectionDomain.class.equals(pts[3])
              && byte[].class.equals(pts[4])
            ) {
              transforms[0] = m;
            }
          } else if (pts.length == 6) {
            if ("java.lang.Module".equals(pts[0].getName())
              && ClassLoader.class.equals(pts[1])
              && String.class.equals(pts[2])
              && Class.class.equals(pts[3])
              && ProtectionDomain.class.equals(pts[4])
              && byte[].class.equals(pts[5])
            ) {
              transforms[1] = m;
            }
          }
        }
      }
    }

    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {
      return transform((Object)null, loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
    }

//    @Override
//    public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
//      return transform((Object)module, loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
//    }

    protected byte[] transform(Object rawModule,
                               ClassLoader loader,
                               String className,
                               Class<?> classBeingRedefined,
                               ProtectionDomain protectionDomain,
                               byte[] classfileBuffer) {
      JavaModule module = rawModule == null ? null : JavaModule.of(rawModule);

      String typeName = className.replace('/', '.');
      ClassFileLocator classFileLocator = new ClassFileLocator.Compound(classFileBufferStrategy.resolve(typeName,
        classfileBuffer,
        loader,
        module,
        protectionDomain), locationStrategy.classFileLocator(loader, module));
      TypePool typePool = poolStrategy.typePool(classFileLocator, loader);

      TypeDescription typeDescription = descriptionStrategy.apply(typeName, classBeingRedefined, typePool, circularityLock, loader, module);
      if (matcher.matches(typeDescription, loader, module, classBeingRedefined, protectionDomain)) {
        try {
          String path = "./" + typeName + ".pre.class";
          FileOutputStream stream = new FileOutputStream(path);
          stream.write(classfileBuffer);
        } catch (Throwable t) {
          t.printStackTrace();
        }
      }

      try {
        if (transforms[1] != null) {
          return (byte[]) transforms[1].invoke(classFileTransformer,
            rawModule, loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
        } else {
          return classFileTransformer.transform(
            loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
        }
      } catch (Throwable t) {
        t.printStackTrace();
      }
      return null;
    }

    @Override
    public Iterator<AgentBuilder.Transformer> iterator(TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule, Class<?> aClass, ProtectionDomain protectionDomain) {
      return classFileTransformer.iterator(typeDescription, classLoader, javaModule, aClass, protectionDomain);
    }

    @Override
    public boolean reset(Instrumentation instrumentation, AgentBuilder.RedefinitionStrategy redefinitionStrategy) {
      logger.log(Level.SEVERE, "reset:a");
      return classFileTransformer.reset(instrumentation, redefinitionStrategy);
    }

    @Override
    public boolean reset(Instrumentation instrumentation, AgentBuilder.RedefinitionStrategy redefinitionStrategy, AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator) {
      logger.log(Level.SEVERE, "reset:b");
      return classFileTransformer.reset(instrumentation, redefinitionStrategy, redefinitionBatchAllocator);
    }

    @Override
    public boolean reset(Instrumentation instrumentation, AgentBuilder.RedefinitionStrategy redefinitionStrategy, AgentBuilder.RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy) {
      logger.log(Level.SEVERE, "reset:c");
      return classFileTransformer.reset(instrumentation, redefinitionStrategy, redefinitionDiscoveryStrategy);
    }

    @Override
    public boolean reset(Instrumentation instrumentation, AgentBuilder.RedefinitionStrategy redefinitionStrategy, AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator, AgentBuilder.RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy) {
      logger.log(Level.SEVERE, "reset:d");
      return classFileTransformer.reset(instrumentation, redefinitionStrategy, redefinitionBatchAllocator, redefinitionDiscoveryStrategy);
    }

    @Override
    public boolean reset(Instrumentation instrumentation, AgentBuilder.RedefinitionStrategy redefinitionStrategy, AgentBuilder.RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy, AgentBuilder.RedefinitionStrategy.Listener redefinitionListener) {
      logger.log(Level.SEVERE, "reset:e");
      return classFileTransformer.reset(instrumentation, redefinitionStrategy, redefinitionDiscoveryStrategy, redefinitionListener);
    }

    @Override
    public boolean reset(Instrumentation instrumentation, AgentBuilder.RedefinitionStrategy redefinitionStrategy, AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator, AgentBuilder.RedefinitionStrategy.Listener redefinitionListener) {
      logger.log(Level.SEVERE, "reset:f");
      return classFileTransformer.reset(instrumentation, redefinitionStrategy, redefinitionBatchAllocator, redefinitionListener);
    }

    @Override
    public boolean reset(Instrumentation instrumentation, AgentBuilder.RedefinitionStrategy redefinitionStrategy, AgentBuilder.RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy, AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator, AgentBuilder.RedefinitionStrategy.Listener redefinitionListener) {
      logger.log(Level.SEVERE, "reset:g");
      return classFileTransformer.reset(instrumentation, redefinitionStrategy, redefinitionDiscoveryStrategy, redefinitionBatchAllocator, redefinitionListener);
    }

    @Override
    public boolean reset(Instrumentation instrumentation, ResettableClassFileTransformer resettableClassFileTransformer, AgentBuilder.RedefinitionStrategy redefinitionStrategy, AgentBuilder.RedefinitionStrategy.DiscoveryStrategy discoveryStrategy, AgentBuilder.RedefinitionStrategy.BatchAllocator batchAllocator, AgentBuilder.RedefinitionStrategy.Listener listener) {
      logger.log(Level.SEVERE, "reset:h");
      return classFileTransformer.reset(instrumentation, resettableClassFileTransformer, redefinitionStrategy, discoveryStrategy, batchAllocator, listener);
    }
  }

  final private PrintStream printStream;
  final private boolean dump;
  final private AgentBuilder.RawMatcher matcher;
  final private Instrumentation inst;

  /**
   * Creates a new stream writing listener.
   *
   * @param _printStream The print stream written to. If null, will not print events.
   * @param _dump Whether or not to run the dumping logic.
   * @param _matcher Matcher from hook used here to determine if classes undergoing transformations should be saved
   * @param _inst Used to set up a ClassFileTransformer for extracting class bytes
   */
  public DumpingListener(PrintStream _printStream, boolean _dump, AgentBuilder.RawMatcher _matcher, Instrumentation _inst) {
    super(_printStream);
    printStream = _printStream;
    dump = _dump;
    matcher = _matcher;
    inst = _inst;
    /*
    classFileTransformer = new OriginalDumpingClassFileTransformer(matcher);

    try {
      inst.addTransformer(classFileTransformer, true);
    } catch (Throwable t) {
      t.printStackTrace();
    }
    */
  }

  @Override
  public ResettableClassFileTransformer decorate(ResettableClassFileTransformer classFileTransformer) {
    if (!dump) {
      return classFileTransformer;
    }

    ResettableClassFileTransformer cft = null;
    Class<? extends ResettableClassFileTransformer> cftc = Ninify.convertClassFileTransformer(OriginalDumpingClassFileTransformer.class);
    if (cftc == null) {
      cft = new OriginalDumpingClassFileTransformer(matcher, classFileTransformer);
    } else {
      try {
        Constructor<? extends ResettableClassFileTransformer> c = cftc.getDeclaredConstructor(AgentBuilder.RawMatcher.class, ResettableClassFileTransformer.class);
        cft = c.newInstance(matcher, classFileTransformer);
      } catch (Throwable t) {
        t.printStackTrace();
        return classFileTransformer;
      }
    }

    return cft;
  }


  /**
   * {@inheritDoc}
   */
  public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
    if (printStream != null) {
      super.onDiscovery(typeName, classLoader, module, loaded);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
    if (printStream != null) {
      super.onTransformation(typeDescription, classLoader, module, loaded, dynamicType);
    }
    if (dump) {
      try {
        String path = "./" + typeDescription.getName() + ".post.class";
        FileOutputStream stream = new FileOutputStream(path);
        stream.write(dynamicType.getBytes());
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded) {
    if (printStream != null) {
      super.onIgnored(typeDescription, classLoader, module, loaded);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
    if (printStream != null) {
      super.onError(typeName, classLoader, module, loaded, throwable);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
    if (printStream != null) {
      super.onComplete(typeName, classLoader, module, loaded);
    }
    if (dump) {
      //System.out.println("onComplete: " + typeName);
      //watchlist.remove(typeName);
    }
  }

}
