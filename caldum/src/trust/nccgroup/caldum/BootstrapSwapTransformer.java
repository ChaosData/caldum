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

package trust.nccgroup.caldum;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class BootstrapSwapTransformer implements ClassFileTransformer {

  private final String className;
  private final byte[] new_impl;

  public BootstrapSwapTransformer(String _className, byte[] _new_impl) {
    className = _className;
    new_impl = _new_impl;
  }

  @Override
  public byte[] transform(ClassLoader loader, String _className,
                          Class<?> clz, ProtectionDomain pd, byte[] buf)
    throws IllegalClassFormatException {
    if (loader == null && className.equals(_className)) {
      return new_impl;
    }
    return null;
  }
}
