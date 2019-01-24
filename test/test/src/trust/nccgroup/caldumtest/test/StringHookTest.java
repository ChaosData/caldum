package trust.nccgroup.caldumtest.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class StringHookTest {

  private int doThing1() {
    return 5;
  }

  @Test
  public void unsecret() {
    assertArrayEquals("__notsecret__".getBytes(), "__secret__".getBytes());

  }

}
