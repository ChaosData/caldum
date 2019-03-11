package trust.nccgroup.caldumtest.test;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class NoRecursionTest {

  static class Inner {
    static int norecursion2() {
      return 2;
    }
  }

  private int norecursion1() {
    return 1 + Inner.norecursion2();
  }

  @Test
  public void norecursion() {
    assertEquals(8, norecursion1()); // adds return of itself: 1 + (2 [+ 2]) [+ (1 (+ 2))]
    assertEquals(4, Inner.norecursion2()); // adds return of itself: 2 [+ 2]
  }

  private static int noselfrecursion2() {
    return 2;
  }

  private int noselfrecursion1() {
    return 1 + noselfrecursion2();
  }

  @Test
  public void noselfrecursion() {
    assertEquals(8, noselfrecursion1()); // adds return of itself: 1 + (2 [+ 2]) [+ (1 (+ 2))]
    assertEquals(4, noselfrecursion2()); // adds return of itself: 2 [+ 2]
  }

}
