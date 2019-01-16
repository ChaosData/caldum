package trust.nccgroup.vulcanloader;

import trust.nccgroup.caldum.AgentLoader;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.URL;
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

    AgentLoader.initialize(inst);
    boolean succeeded = AgentLoader.load(true, EMBEDDED_AGENT, embedded_agent, args, inst);

    if (!succeeded) {
      System.err.println("Startup error: Embedded agent failed to load.");
    }

  }

}
