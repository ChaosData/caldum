/*
Copyright 2018 NCC Group

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

package trust.nccgroup.caldum.util;

import java.security.SecureRandom;

public class RandomHex {

  private final static String chars = "0123456789abcdef";

  public static String get(int n) {
    assert n > 0;

    SecureRandom sr = new SecureRandom();
    byte bb[] = new byte[1];
    StringBuilder sb = new StringBuilder(n*2);

    for (int i=0; i<n; i++) {
      sr.nextBytes(bb);

      byte b = bb[0];
      int ah = (b >> 4) & 0xf;
      int al = b & 0xf;

      sb.append(chars.charAt(ah));
      sb.append(chars.charAt(al));
    }
    return sb.toString();
  }

}
