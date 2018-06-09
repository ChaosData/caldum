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

package trust.nccgroup.caldum.util;

import java.io.File;
import java.io.IOException;

public class TmpDir {

  public static boolean delete(File d) {
    if (!d.isDirectory()) {
      return false;
    }
    for (File c : d.listFiles()) {
      delete(c);
    }
    return d.delete();
  }

  public static File create() {

    try {
      String name = RandomHex.get(8);
      File temp = File.createTempFile(name, ".tmp");

      //noinspection ResultOfMethodCallIgnored
      temp.delete();

      if (temp.exists()) {
        if (!delete(temp)) {
          System.out.println("failed to delete");
          return null;
        }
      }

      if (!temp.mkdirs()) {
        System.out.println("failed to mkdirs");
        return null;
      }
      if (!temp.setWritable(true, true)) {
        System.out.println("failed to setW");
        return null;
      }
      if (!temp.setExecutable(true, true)) {
        System.out.println("failed to setX");
        return null;
      }
      if (!temp.setReadable(true, true)) {
        System.out.println("failed to setR");
        return null;
      }
      return temp;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

  }
}
