package trust.nccgroup.caldum.util;

import net.bytebuddy.dynamic.ClassFileLocator;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Dumper {

  private static final Logger logger = TmpLogger.DEFAULT;

  public static void dumpClass(Instrumentation inst, Class<?> clazz, String path) {
    try {
      byte[] bytes = ClassFileLocator.ForInstrumentation.of(inst, clazz).locate(clazz.getName()).resolve();
      FileOutputStream stream = new FileOutputStream(path);
      stream.write(bytes);
      stream.close();
    } catch (Throwable t) {
      logger.log(Level.SEVERE, "class dump failed", t);
    }
  }
}
