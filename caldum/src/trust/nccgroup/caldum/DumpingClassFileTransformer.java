package trust.nccgroup.caldum;

import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class DumpingClassFileTransformer implements ClassFileTransformer {
  private String suffix;
  private DumpingClassFileTransformer(String _suffix) {
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
      DumpingClassFileTransformer d = new DumpingClassFileTransformer(suffix);
      inst.addTransformer(d, true);
      inst.retransformClasses(hookClass);
      inst.removeTransformer(d);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }
}
