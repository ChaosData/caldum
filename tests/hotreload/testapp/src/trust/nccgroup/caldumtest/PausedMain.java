package trust.nccgroup.caldumtest;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.Socket;

import org.junit.runner.JUnitCore;

public class PausedMain {

  public static void main(String[] argv) {
    System.out.println("java.version: " + System.getProperty("java.version"));
    int port = argv.length > 0 ? Integer.parseInt(argv[1]) : 7777;

    Paused.pause(port);
    JUnitCore.main(new String[]{ RunAllTests.class.getName() });
  }

}

