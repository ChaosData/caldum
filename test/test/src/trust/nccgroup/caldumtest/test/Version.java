package trust.nccgroup.caldumtest.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class Version {

  @Test
  public void version() {
    System.out.println("java.version: " + System.getProperty("java.version"));
  }

}
