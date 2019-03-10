package trust.nccgroup.caldum;

import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class SimpleDumpingClassFileTransformer implements ClassFileTransformer {
  private String suffix;
  private SimpleDumpingClassFileTransformer(String _suffix) {
    suffix = _suffix;
  }

  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                          ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
    try {
      String path = "./" + className.replace("/", ".") + suffix + ".class";
      FileOutputStream stream = new FileOutputStream(path);
      stream.write(classfileBuffer);
    } catch (Throwable t) {
      t.printStackTrace();
    }
    return null;
  }

  public static void dump(Instrumentation inst, Class<?> hookClass, String suffix) {
    try {
      SimpleDumpingClassFileTransformer d = new SimpleDumpingClassFileTransformer(suffix);
      inst.addTransformer(d, true);
      inst.retransformClasses(hookClass);
      inst.removeTransformer(d);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  public static void dump(Instrumentation inst, Class<?> hookClass, String suffix, byte[] alt) {
    try {
      String path = "./" + hookClass.getName() + suffix + ".class";
      FileOutputStream stream = new FileOutputStream(path);
      stream.write(alt);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }
}
