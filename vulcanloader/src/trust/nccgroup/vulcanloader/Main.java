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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLEncoder;
import java.util.Locale;

///import com.sun.tools.attach.VirtualMachine;
//import net.bytebuddy.agent.VirtualMachine;
import com.sun.tools.attach.VirtualMachine;
import org.wikibooks.Base64;

class Main {

  private static final String USAGE = "" +
    "java -jar vl.jar <pid> {" +
      " -u |" +
      " -c <vulcanloader.conf> |" +
      " <agent> [agent2...] [ -- {" +
        " unhook | <agent args> " +
      "} ]" +
    "}";

  public static void main(String[] argv) {
    if (argv.length < 2) {
      System.err.println(USAGE);
      return;
    }

    String pid = argv[0];
    try {
      int p = Integer.parseInt(pid);
      if (p < 0) {
        throw new NumberFormatException();
      }
    } catch (NumberFormatException nfe) {
      System.err.println(String.format("Invalid pid of '%s'", pid));
      return;
    }

    String encodedPathAgentArgs;

    if ("-c".equals(argv[1])) {
      if (argv.length != 3) {
        System.err.println("Missing config file path.");
        return;
      }

      try {
        encodedPathAgentArgs = "||" + URLEncoder.encode(Base64.encode(argv[2]), "UTF-8");
      } catch (UnsupportedEncodingException ignored) {
        return;
      }
    } else if ("-u".equals(argv[1])) {
      try {
        encodedPathAgentArgs = "||" + URLEncoder.encode(Base64.encode("unload"), "UTF-8");
      } catch (UnsupportedEncodingException ignored) {
        return;
      }
    } else {
      StringBuilder sb = new StringBuilder(512);

      int i = 1;
      for (; i < argv.length; i++) {
        if ("--".equals(argv[i])) {
          if (i == argv.length-1) {
            System.err.println("Dangling -- observed. Missing agent arguments.");
            return;
          }
          break;
        }

        try {
          sb.append(URLEncoder.encode(Base64.encode(argv[i]), "UTF-8"));
        } catch (UnsupportedEncodingException ignored) {
          return;
        }
        sb.append('|');
      }
      sb.append('|');

      StringBuilder sb2 = new StringBuilder(256);
      for (i += 1; i < argv.length - 1; i++) {
        sb2.append(argv[i]);
        sb2.append(" ");
      }
      if (i == argv.length-1) {
        sb2.append(argv[i]);
      }

      String agentArgs = sb2.toString();

      try {
        sb.append(URLEncoder.encode(Base64.encode(agentArgs), "UTF-8"));
      } catch (UnsupportedEncodingException ignored) {
        return;
      }

      encodedPathAgentArgs = sb.toString();
    }

    send(pid, encodedPathAgentArgs);
  }

  /*
  private static Class<? extends VirtualMachine> get() {
    return System.getProperty("java.vm.vendor").toUpperCase(Locale.US).contains("J9")
      ? VirtualMachine.ForOpenJ9.class
      : VirtualMachine.ForHotSpot.class;
  }*/

  private static Class<?> virtualMachine = null;
  private static Class<?> getVirtualMachine() {
    if (virtualMachine != null) {
      return virtualMachine;
    }

    //note: openj9 emulates com.sun.tools.attach.VirtualMachine, so we only have to look for that
    String className = "com.sun.tools.attach.VirtualMachine";
    Class<?> clazz = null;
    try {
      clazz = Class.forName(className);
    } catch (ClassNotFoundException cnfe) { }
    if (clazz == null) {
      // $JAVA_HOME/lib/tools.jar
      String home = System.getProperty("java.home");
      String url = "file://" + home + "/../lib/tools.jar";
      try {
        URL[] urls = new URL[]{new URL(url)};
        ClassLoader cl = new URLClassLoader(urls);
        clazz = cl.loadClass(className);
      } catch (MalformedURLException e) {
        System.err.println("Error: Invalid tools.jar URL: " + url);
      } catch (ClassNotFoundException e) {
        System.err.println("Error: Could not find VirtualMachine class in tools.jar");
        e.printStackTrace();
      }
    }
    return clazz;
  }

  private static void send(String pid, String encodedPathAgentArgs) {
    try {
      String path = Main.class
        .getProtectionDomain()
        .getCodeSource()
        .getLocation()
        .getPath();

      //note: by loading VirtualMachine directly this works on jdk >=9
      try {
        VirtualMachine vm0 = VirtualMachine.attach(pid);
        vm0.loadAgent(path, encodedPathAgentArgs);
        vm0.detach();
        return;
      } catch (NoClassDefFoundError ignore) { }

      //note: for jdk <=8, we extract VirtualMachine out of the tools.jar.
      //      this approach has issues on 17+ due to modules (terrible design btw),
      //      so luckily it doesn't need to run on 17+.
      Class<?> VirtualMachineClass = getVirtualMachine();
      if (VirtualMachineClass == null) {
        System.err.println("Error: Could not find VirtualMachine class");
        return;
      }
      Method attach = VirtualMachineClass.getMethod("attach", String.class);
      Object vm = attach.invoke(null, pid);

      //note: there seem to be some weird issues in how openj9 jdk8 proxies com.sun.tools.attach.VirtualMachine
      //      so we pull the loadAgent/detach methods out of the actual object returned from attach()
      Method loadAgent = vm.getClass().getMethod("loadAgent", String.class, String.class);
      Method detach = vm.getClass().getMethod("detach");

      loadAgent.invoke(vm, path, encodedPathAgentArgs);
      detach.invoke(vm);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }
}
