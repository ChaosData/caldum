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

package trust.nccgroup.caldumtest.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.*;

public class StringHookTest {

  /*
            0x00002c0b      59             dup
            0x00002c0c      03             iconst_0
            0x00002c0d      105f           bipush 95
            0x00002c0f      54             bastore
            0x00002c10      59             dup
            0x00002c11      04             iconst_1
            0x00002c12      105f           bipush 95
            0x00002c14      54             bastore
            0x00002c15      59             dup
            0x00002c16      05             iconst_2
            0x00002c17      1073           bipush 115
            0x00002c19      54             bastore
            0x00002c1a      59             dup
  */
  final static private byte[] post_bytes = new byte[]{
    0x59, 0x03, 0x10, 0x5f, 0x54, 0x59, 0x04, 0x10,
    0x5f, 0x54, 0x59, 0x05, 0x10, 0x73, 0x54, 0x59
  };

  private int doThing1() {
    return 5;
  }

  @Test
  public void unsecret() {
    //test for StringHook
    assertArrayEquals("__notsecret__".getBytes(), "__secret__".getBytes());

    //test for StringHook @Dump
    try {
      RandomAccessFile f = new RandomAccessFile("./java.lang.String.pre.class", "r");
      byte[] b = new byte[(int)f.length()];
      f.readFully(b);
      int pos = bytesIndexOf(b, post_bytes, 0);
      assertEquals(-1, pos);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }

    try {
      RandomAccessFile f = new RandomAccessFile("./java.lang.String.post.class", "r");
      byte[] b = new byte[(int)f.length()];
      f.readFully(b);
      int pos = bytesIndexOf(b, post_bytes, 0);
      assertNotEquals(-1, pos);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  static int bytesIndexOf(byte[] source, byte[] search, int fromIndex) {
    boolean find = false;
    int i;
    for (i = fromIndex; i <= (source.length - search.length); i++) {
      if (source[i] == search[0]) {
        find = true;
        for (int j = 0; j < search.length; j++) {
          if (source[i + j] != search[j]) {
            find = false;
          }
        }
      }
      if (find) {
        break;
      }
    }
    if (!find) {
      return -1;
    }
    return i;
  }
}
