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
import java.net.URLEncoder;
import java.util.Locale;

import com.sun.tools.attach.VirtualMachine;
//import net.bytebuddy.agent.VirtualMachine;
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

  private static void send(String pid, String encodedPathAgentArgs) {
    try {
      String path = Main.class
        .getProtectionDomain()
        .getCodeSource()
        .getLocation()
        .getPath();
      VirtualMachine vm = VirtualMachine.attach(pid);
      // the following relies on JNA garbage
      /*VirtualMachine vm = (VirtualMachine) get() //VirtualMachine.Resolver.INSTANCE.run()
        .getMethod("attach", String.class)
        .invoke(null, pid);*/
      vm.loadAgent(path, encodedPathAgentArgs);
      vm.detach();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
