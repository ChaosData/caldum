package trust.nccgroup.caldumtest;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.Socket;

import org.junit.runner.JUnitCore;

public class Paused {

  int port = 7777;
  ServerSocket serverSocket = null;

  public static Paused INSTANCE = null;

  public Paused(int _port) {
    if (port > 0) {
      port = _port;
    }

    try {
      InetAddress host = InetAddress.getByName("127.0.0.1");
      serverSocket = new ServerSocket(port, 1, host);
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return;
    }
  }

  public static Paused getInstance(int port) {
    if (INSTANCE == null) {
      INSTANCE = new Paused(port);
    } else if (INSTANCE.port != port) {
      return new Paused(port);
    }
    return INSTANCE;
  }

  public void pause() {
    try {
      Socket clientSocket = serverSocket.accept();
      clientSocket.close();
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return;
    }
  }

  public static void pause(int port) {
    Paused p = getInstance(port);
    p.pause();
  }

}

