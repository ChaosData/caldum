/*
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

