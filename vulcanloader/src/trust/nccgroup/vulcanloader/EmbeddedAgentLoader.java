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

package trust.nccgroup.vulcanloader;

import trust.nccgroup.caldum.AgentLoader;
import trust.nccgroup.caldum.util.TmpDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.List;

public class EmbeddedAgentLoader {

  private static final String JAVAAGENT = "-javaagent:";
  private static final String EMBEDDED_AGENT = "__embedded_agent__";
  private static final String EMBEDDED_AGENT_PATH = "assets/agent.jar";

  static boolean hasEmbeddedAgent() {
    return PreMain.class.getClassLoader().getResource(EMBEDDED_AGENT_PATH) != null;
  }

  static void load(String args, Instrumentation inst) {
    String agent_path = null;
    List<String> argv = ManagementFactory.getRuntimeMXBean().getInputArguments();
    for (String arg : argv) {
      if (arg.startsWith(JAVAAGENT)) {
        agent_path = arg.substring(JAVAAGENT.length());
        break;
      }
    }
    if (agent_path == null) {
      System.err.println("Startup error: ???");
      return;
    }

    URL embedded_agent = PreMain.class.getClassLoader().getResource(EMBEDDED_AGENT_PATH);
    if (embedded_agent == null) {
      System.err.println("Startup error: Could not find embedded agent.");
      return;
    }

    URL tmp_agent_url = null;
    File tmp = TmpDir.create();
    File tmpagent = new File(tmp, "agent.jar");
    try {
      if (tmpagent.createNewFile()) {
        InputStream embedded_agent_stream = embedded_agent.openStream();
        if (embedded_agent_stream != null) {
          ReadableByteChannel rbc = Channels.newChannel(embedded_agent_stream);
          FileOutputStream fos = new FileOutputStream(tmpagent);
          fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

          tmp_agent_url = tmpagent.toURI().toURL();
          embedded_agent_stream.close();
        }
      }

    } catch (IOException ioe) { }

    if (tmp_agent_url == null) {
      System.err.println("Startup error: Failed to extract and load embedded agent.");
      return;
    }

    AgentLoader.initialize(inst);
    boolean succeeded = AgentLoader.load(true, EMBEDDED_AGENT, tmp_agent_url, args, inst);

    TmpDir.delete(tmp);

    if (!succeeded) {
      System.err.println("Startup error: Embedded agent failed to load.");
    }

  }

}
