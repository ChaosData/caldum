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
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@SuppressWarnings("WeakerAccess")
public class TmpLogger {

  public static Logger DEFAULT = build("caldum");

  private static String getTempDir() {
    String tmpdir = System.getProperty("java.io.tmpdir");
    if (tmpdir != null) {
      return tmpdir;
    }

    String name = RandomHex.get(8);
    try {
      File temp = File.createTempFile(name, ".tmp");
      String ppath = temp.getParentFile().getAbsolutePath();
      if (temp.delete()) {
        return ppath;
      }
      return ppath;
    } catch (IOException e) {
      return null;
    }
  }

  public static Logger build(String name) {
    return build(name, getTempDir());
  }

  public static Logger build(String name, String dir) {
    try {
      Logger logger = Logger.getLogger(name);

      logger.setUseParentHandlers(false);
      logger.setLevel(Level.INFO);

      FileHandler fh = new FileHandler(dir + "/" + name + ".log", true);
      fh.setFormatter(new SimpleFormatter());

      logger.addHandler(fh);

      return logger;
    } catch (Throwable t) {
      return null;
    }
  }
}

