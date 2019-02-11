package trust.nccgroup.caldumtest.test;

import static org.junit.Assert.*;
import org.junit.Test;

public class Version {

  @Test
  public void version() {
    System.out.println("java.version: " + System.getProperty("java.version"));
  }

}
