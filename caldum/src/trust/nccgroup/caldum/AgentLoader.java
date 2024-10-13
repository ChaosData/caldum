/*
Copyright 2018-2019 NCC Group
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

package trust.nccgroup.caldum;

import net.bytebuddy.agent.builder.AgentBuilder;
import trust.nccgroup.caldum.util.TmpLogger;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;

public final class AgentLoader {

  private static final Object lock = new Object();

  public static final class AgentHolder {
    Class<?> agentClass = null;
    ArrayList<DestructingResettableClassFileTransformer> rcfts = null;
    Instrumentation inst;
  }

  private static Map<String,AgentHolder> loadedAgents = new HashMap<String,AgentHolder>();

  private static final Logger logger = TmpLogger.DEFAULT;


  public static boolean load(boolean isPre, String path,
                             String agentArgs, Instrumentation inst) {
    URL url;
    try {
      url = new File(path).toURI().toURL();
    } catch (MalformedURLException e) {
      logger.log(SEVERE, "unknown error", e);
      return false;
    }
    return load(isPre, path, url, agentArgs, inst);
  }

  public static boolean load(boolean isPre, String path, URL url,
                         String agentArgs, Instrumentation inst) {
    if (logger == null) {
      System.err.println("logger not initialized. bailing.");
      return false;
    }

    synchronized (lock) {

      if (!unload(path, false)) {
        return false;
      }

      JarInputStream jis;
      try {
        jis = new JarInputStream(url.openStream());
      } catch (IOException ioe) {
        logger.severe(String.format(
          "Failed to load agent: Could not read '%s'.",
          path
        ));
        return false;
      }
      Manifest mf = jis.getManifest();
      Attributes attrs = mf.getMainAttributes();

      String attrName = isPre ? "Premain-Class" : "Agent-Class";
      String agentName = attrs.getValue(attrName);

      if (agentName == null) {
        logger.info(String.format(
          "Could not find '%s' attribute in '%s'.",
          attrName, path
        ));
      }

      String scanPrefix = attrs.getValue("Caldum-Scan-Prefix");

      InverseURLClassLoader childloader = new InverseURLClassLoader(
        new URL[]{ url },
        AgentLoader.class.getClassLoader(),
        logger
      );
      //childloader.loadAll();

      Class<?> agentClass = null;
      boolean ret = true;

      AgentHolder ah = new AgentHolder();

      ah.inst = inst;

      if (agentName != null) {
        try {
          agentClass = Class.forName(agentName, true, childloader);
        } catch (ClassNotFoundException cnfe) {
          logger.log(Level.SEVERE, ">>here<<", cnfe);
          logger.severe(String.format(
            "Failed to load agent: " +
              "Could not find class '%s' in '%s'.",
            agentName, path
          ));
        }
      }

      if (agentClass != null) {
        String methodName = isPre ? "premain" : "agentmain";

        try {
          Method loggingAgentMethod = null;
          Method agentMethod = null;

          try {
            loggingAgentMethod = agentClass.getMethod(
              methodName,
              String.class, Instrumentation.class, Logger.class
            );
          } catch (NoSuchMethodException ignored) {
            agentMethod = agentClass.getMethod(
              methodName,
              String.class, Instrumentation.class
            );
          }


          try {
            if (loggingAgentMethod != null) {
              loggingAgentMethod.invoke(null, agentArgs, inst, logger);
            } else if (agentMethod != null) {
              agentMethod.invoke(null, agentArgs, inst);
            } else {
              throw new NoSuchMethodException();
            }

            ah.agentClass = agentClass;
            //ah.rcfts = HookProcessor.process(inst, childloader, scanPrefix);
            //logger.info(String.format("Loaded agent '%s'.", path));
          } catch (IllegalAccessException iae) {
            logger.log(SEVERE, String.format(
              "Failed to load agent: " +
              "Method '%s' within class '%s' at path '%s' is not accessible.",
              methodName, agentName, path
            ), iae);
            ret = false;
          } catch (InvocationTargetException e) {
            logger.log(SEVERE,
              "Error loading agent: " +
                "unknown error",
              e.getTargetException()
            );
            ret = false;
          }

        } catch (NoSuchMethodException e) {
          logger.severe(String.format(
            "Failed to load agent: " +
            "Class '%s' at path '%s' does not have a public '%s' method.",
            agentName, path, methodName
          ));
          ret = false;
        }
      }
      ah.rcfts = HookProcessor.process(inst, childloader, scanPrefix);
      loadedAgents.put(new File(path).getName(), ah);
      logger.info(String.format("Loaded agent '%s'.", path));

      try {
        Method close = null;

        try {
          close = childloader.getClass().getMethod("close");
        } catch (NoSuchMethodException e) {
          try {
            close = childloader.getClass().getMethod("java6close");
          } catch (NoSuchMethodException ignored) { }
        }

        if (close == null) {
          logger.severe(
            "Error closing child ClassLoader: " +
            "No close method found for URLClassLoader."
          );
          ret = false;
        } else {
          try {
            close.invoke(childloader);
          } catch (IllegalAccessException iae) {
            logger.log(SEVERE,
              "Error closing child ClassLoader: " +
              "Could not access URLClassLoader::close",
              iae
            );
            ret = false;
          } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (t instanceof IOException) {
              throw (IOException)t;
            } else {
              logger.log(SEVERE,
                "Error closing child ClassLoader: " +
                "unknown error",
                t
              );
              ret = false;
            }
          }
        }
      } catch (IOException ioe) {
        logger.log(SEVERE, "Error closing child ClassLoader:", ioe);
        ret = false;
      }

      return ret;
    }
  }

  public static boolean unload(String path) {
    synchronized (lock) {
      return unload(path, true);
    }
  }

  private static synchronized boolean unload(String path, boolean explicit) {
    assert logger != null;

    if (!explicit && (path == null || path.equals(""))) {
      logger.warning("Warning: Ignoring non-explicit unload of all agents. ");
      return false;
    }

    if (path == null || path.equals("")) {
      Map<String, AgentHolder> nLoadedAgents = new HashMap<String, AgentHolder>();

      for (Map.Entry<String, AgentHolder> kv : loadedAgents.entrySet()) {
        if (!unloadAgent(kv.getValue(), kv.getKey())) {
          nLoadedAgents.put(kv.getKey(), kv.getValue());
        }
      }
      loadedAgents = nLoadedAgents;
      return false;
    } else {
      path = new File(path).getName();

      AgentHolder ah = loadedAgents.remove(path);

      if (ah == null
        || (ah.agentClass == null && ah.rcfts.size() == 0)) {
        if (explicit) {
          if (explicit) {
            logger.severe(String.format(
              "Failed to unload agent: File '%s' was not loaded.",
              path
            ));
            return false;
          }
        }
        return true;
      }

      boolean unloaded = unloadAgent(ah, path);
      if (!unloaded) {
        loadedAgents.put(new File(path).getName(), ah);
      }
      return unloaded;
    }
  }

  private static synchronized boolean unloadAgent(AgentHolder ah, String path) {
    assert logger != null;


    logger.info(String.format("Unloading agent: %s (ah.rcfts.size(): %d).", path, ah.rcfts != null ? ah.rcfts.size() : -1));

    boolean ret = false;

    Class<?> agent = ah.agentClass;
    if (agent != null) {
      try {
        Method unload = agent.getMethod("unload");
        try {
          unload.invoke(null);
          ret = true;
        } catch (IllegalAccessException iae) {
          logger.log(SEVERE, String.format(
            "Failed to unload agent: " +
              "'unload' method within class '%s' at path '%s' is not accessible.",
            agent.getName(), path
          ), iae);
        } catch (InvocationTargetException ite) {
          logger.log(SEVERE, String.format(
            "Failed to unload agent: " +
              "'unload' method within class '%s' at path '%s' threw.",
            agent.getName(), path
          ), ite);
        }
      } catch (NoSuchMethodException nsme) {
        logger.log(SEVERE, String.format(
          "Failed to unload agent: " +
            "Class '%s' at path '%s' does not have a public 'unload' method.",
          agent.getName(), path
        ), nsme);
      }
    } else {
      logger.log(INFO, "ah.agentClass == null");
    }

    if (ah.rcfts != null) {
      ret = true;
      ArrayList<DestructingResettableClassFileTransformer> nrcfts = new ArrayList<DestructingResettableClassFileTransformer>();
      for (int i=0; i < ah.rcfts.size(); i++) {
        DestructingResettableClassFileTransformer rcft = ah.rcfts.get(i);
        logger.log(INFO, "rcft: " + rcft);
        if (!rcft.reset(ah.inst, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)) {
          // currently failing on the @Debug/@Dump annotated magic listener/wrapper class
          logger.log(SEVERE, "rcft.reset(): failed");
          ret = false;
          nrcfts.add(rcft);
        }
      }
      ah.rcfts = nrcfts;
    } else {
      //System.out.println("ah.rcfts == null");
      logger.log(INFO, "ah.rcfts == null");
    }

    if (ret == false) {
      logger.log(SEVERE, "failed to unload agent");
    }
    return ret;
  }

  public static void initialize(Instrumentation inst) {
    Initialization.run(inst);
  }
}
