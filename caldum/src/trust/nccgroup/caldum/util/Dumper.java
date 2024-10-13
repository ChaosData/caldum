/*
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
