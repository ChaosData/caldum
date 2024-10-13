/*
Copyright 2019 NCC Group

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
