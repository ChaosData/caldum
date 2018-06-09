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

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@SuppressWarnings("WeakerAccess")
public class TmpLogger {

  public static Logger DEFAULT = build("caldum");

  public static Logger build(String name) {
    try {
      Logger logger = Logger.getLogger(name);

      logger.setUseParentHandlers(false);
      logger.setLevel(Level.INFO);

      FileHandler fh = new FileHandler("/tmp/" + name + ".log", true);
      fh.setFormatter(new SimpleFormatter());

      logger.addHandler(fh);

      return logger;
    } catch (Throwable t) {
      return null;
    }
  }
}

