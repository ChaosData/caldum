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

import org.wikibooks.Base64;

import java.io.UnsupportedEncodingException;
import java.lang.instrument.Instrumentation;
import java.net.URLDecoder;
import java.util.logging.Logger;

//import static trust.nccgroup.vulcanloader.AgentLogger.logger;

import trust.nccgroup.caldum.AgentLoader;
import trust.nccgroup.caldum.util.TmpLogger;

@SuppressWarnings("unused")
public class AgentMain {

  private static final Logger logger = TmpLogger.DEFAULT;
  //public static Instrumentation inst = null;

  // args: <path:base64:urlencoded>|<path:base64:urlencoded>||<agentArgs:base64:urlencoded>
  // args: ||<confPath:base64:urlencoded>
  public static void agentmain(String args, Instrumentation inst) {
    //inst = _inst;
    //System.out.println("raw args: " + args);
    if (logger == null) {
      System.err.println("Startup error: no logger. bailing.");
      return;
    }

    int pos = args.indexOf("||");
    if (pos == -1) {
      pos = args.length();
    }
    //System.out.println("pos: " + pos);
    if (pos == 0 && !"||".equals(args)) {
      String encodedConfPath = args.substring(2);
      String decodedConfPath = decode(encodedConfPath);
      if (!"unhook".equals(decodedConfPath)&&!"unload".equals(decodedConfPath)) {
        ConfigLoader.load(decodedConfPath, inst, false);
        return;
      }
    }

    String[] encodedPaths = args.substring(0, pos).split("\\|");
    //System.out.println("encodedPaths.length: " + encodedPaths.length);
    String encodedAgentArgs = args.substring(pos+2);
    //System.out.println("encodedAgentArgs: " + encodedAgentArgs);

    String agentArgs = decode(encodedAgentArgs);
    //System.out.println("decoded agentArgs: " + agentArgs);

    if (agentArgs == null) {
      logger.severe(
        "Error loading agent: " +
        "Failed to decode internal loader protocol?"
      );
      return;
    } else if ("".equals(agentArgs)) {
      agentArgs = "";
    }

    String[] paths = new String[encodedPaths.length];

    for (int i=0; i< encodedPaths.length; i++) {
      String path = decode(encodedPaths[i]);
      if (path == null) {
        logger.severe(
          "Error loading agent: " +
            "Failed to decode internal loader protocol?"
        );
        return;
      }
      paths[i] = path;
    }

    AgentLoader.initialize(inst);
    if ("unhook".equals(agentArgs)||"unload".equals(agentArgs)) {
      if (paths.length != 0) {
        for (String path : paths) {
          AgentLoader.unload(path);
        }
      } else {
        AgentLoader.unload("");
      }
    } else {
      for (String path : paths) {
        if (!AgentLoader.load(false, path, agentArgs, inst)) {
          return;
        }
      }
    }
  }

  private static String decode(String encoded) {
    if (encoded.length() == 0) {
      return "";
    }
    try {
      return Base64.decode(URLDecoder.decode(encoded, "UTF-8"));
    } catch (UnsupportedEncodingException ignored) {
      return null;
    }
  }
}
