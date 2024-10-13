/*
Copyright 2019 NCC Group
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

