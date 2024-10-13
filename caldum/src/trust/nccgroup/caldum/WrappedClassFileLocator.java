/*
Copyright 2018-2019 NCC Group

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

import java.io.*;
import net.bytebuddy.dynamic.ClassFileLocator;

class WrappedClassFileLocator implements ClassFileLocator {
  private final String name;
  private final byte[] impl;

  public WrappedClassFileLocator(String _name, byte[] _impl) {
    name = _name; impl = _impl;
  }

  static class Resolution implements ClassFileLocator.Resolution {

    private final byte[] bytes;

    public Resolution(byte[] _bytes) {
      bytes = _bytes;
    }

    @Override
    public boolean isResolved() {
      return true;
    }

    @Override
    public byte[] resolve() {
      return bytes;
    }
  }

  @Override
  public ClassFileLocator.Resolution locate(String typeName) throws IOException {
    if (name.equals(typeName)) {
      return new ClassFileLocator.Resolution.Explicit(impl);
    }
    try {
      return new Resolution(ClassFileLocator.ForClassLoader.read(
        WrappedClassFileLocator.class.getClassLoader().loadClass(typeName)
      ));
    } catch (Throwable t) {
      return null;
    }
  }

  @Override
  public void close() throws IOException {

  }
}
