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

