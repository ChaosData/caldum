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

package trust.nccgroup.caldumtest.test;

import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import trust.nccgroup.caldumtest.Paused;

import java.io.File;

public class TemplatedFileHookTest {

  static int i = 0;

  static String[] checkValues = {
    "__notsecret__",
    "__secret__",
    "__notsecret__",
    "__notsecret!__",
    "__notsecret!!__",
    "__secret__",
    "__secret__"
  };

  static String mod = "";

  @Before
  public void pause() {
    Paused.INSTANCE.pause();
  }

  @After
  public void bump() {
    i = i + 1;
  }

  private static String getCheckValue() {
    return checkValues[i];
  }

  @Test
  public void filetest() {
    File f = new File("/tmp/__secret__");
    String s = f.getAbsolutePath();
    String c = getCheckValue();
    if (s.indexOf(c) == -1) {
      throw new AssertionError("failure[#" + (i+1) + "]: could not find `" + c + "` in " + s);
    }
  }

  //@Test
  //public void unsecret() {
    //assertArrayEquals("__notsecret__".getBytes(), "__secret__".getBytes());
    //assertArrayEquals("__notsecret".getBytes(), "__secret".getBytes());

  //}

}

