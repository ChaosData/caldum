package trust.nccgroup.caldumtest;

import java.io.IOException;
import java.net.*;

import org.junit.runner.JUnitCore;

public class PausedMain {

  public static boolean pause(String port) {
    System.out.println("PausedMain.pause called");
    try {
      if (!"run".equals(port)) {
        System.out.println("listening on port " + port);
        InetAddress host = InetAddress.getByName("127.0.0.1");
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(host, Integer.parseInt(port)), 1);
        Socket clientSocket = serverSocket.accept();
        clientSocket.close();
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return false;
    }
    return true;
  }

  public static void main(String[] argv) {

    String port = argv.length > 0 ? argv[0] : "7777";
    if (!pause(port)) {
      return;
    }

    System.setProperty("log4j.debug", "false");
    System.setProperty("log4j.rootLogger", "OFF");

    JUnitCore.main(new String[]{ RunAllTests.class.getName() });
  }

}

