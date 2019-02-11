package trust.nccgroup.caldumtest;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.Socket;

import org.junit.runner.JUnitCore;

public class PausedMain {

  public static void main(String[] argv) {
    try {
      String port = argv.length > 0 ? argv[1] : "7777";
      InetAddress host = InetAddress.getByName("127.0.0.1");
      ServerSocket serverSocket = new ServerSocket(Integer.parseInt(port), 1, host);
      Socket clientSocket = serverSocket.accept();
      clientSocket.close();
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return;
    }

    JUnitCore.main(new String[]{ RunAllTests.class.getName() });
  }

}

