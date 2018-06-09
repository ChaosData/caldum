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

package trust.nccgroup.vulcanloader;

import trust.nccgroup.caldum.util.TmpLogger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.instrument.Instrumentation;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import trust.nccgroup.caldum.AgentLoader;

class ConfigLoader {

  private static final Logger logger = TmpLogger.DEFAULT;

  static void load(String confPath, Instrumentation inst, boolean pre) {
    assert logger != null;

    String conf = readConfig(confPath);
    if (conf == null) {
      return;
    }

    String[] lines = conf.split("[\\r\\n]+");

    for (String line : lines) {
      if (line.startsWith("#")) {
        continue;
      }

      if (line.replaceAll("\\s", "").length() == 0) {
        continue;
      }

      String path;
      String args;
      int pos = line.indexOf(": ");
      if (pos != -1) {
        path = line.substring(0, pos);
        args = line.substring(pos+2);
      } else {
        path = line;
        args = "";
      }

      AgentLoader.initialize(inst);
      boolean succeeded;
      if ("unload".equals(args)) {
        if (pre) {
          logger.info(String.format(
            "Skipping premain() unload attempt for '%s'.", path
          ));
          continue;
        } else {
          succeeded = AgentLoader.unload(path);
        }
      } else {
        succeeded = AgentLoader.load(pre, path, args, inst);
      }
      if (!succeeded) {
        return;
      }
    }
  }

  private static String readConfig(String configPath) {
    assert logger != null;

    if (configPath.endsWith(".jar") || configPath.contains(".jar=")) {
      int pos = configPath.indexOf(".jar=");
      String out = "";
      if (pos == -1) {
        out += configPath + "\n";
      } else {
        out += configPath.substring(0, pos+4) + ": ";
        out += configPath.substring(pos+6) + "\n";
      }
      return out;
    }

    RandomAccessFile f;
    try {
      f = new RandomAccessFile(configPath, "r");
    } catch (FileNotFoundException fnfe) {
      logger.log(Level.SEVERE, String.format(
        "Failed to configure agent: " +
          "Config file '%s' not found.",
        configPath
      ), fnfe);
      return null;
    }

    byte[] b;
    try {
      b = new byte[(int) f.length()];
      f.readFully(b);
    } catch (IOException ioe) {
      logger.log(Level.SEVERE, String.format(
        "Failed to configure agent: " +
          "Error reading config file '%s'.",
        configPath
      ), ioe);
      return null;
    }

    Charset utf8 = Charset.forName("UTF-8");

    String config;
    try {
      config = utf8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(b)).toString();
    } catch (CharacterCodingException cce) {
      logger.log(Level.SEVERE, String.format(
        "Failed to configure agent: " +
          "Invalid characters in config file '%s'.",
        configPath
      ), cce);
      return null;
    }

    return config;
  }

}
