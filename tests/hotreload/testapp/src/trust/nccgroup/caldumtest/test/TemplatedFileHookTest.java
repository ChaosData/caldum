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
      throw new AssertionError("failure[#" + i+1 + "]: could not find `" + c + "` in " + s);
    }
  }

  //@Test
  //public void unsecret() {
    //assertArrayEquals("__notsecret__".getBytes(), "__secret__".getBytes());
    //assertArrayEquals("__notsecret".getBytes(), "__secret".getBytes());

  //}

}

