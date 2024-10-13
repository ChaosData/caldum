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
