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

package trust.nccgroup.caldum.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Wrapper {
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface OnMethodEnter {
    public static final String NAME = OnMethodEnter.class.getName();
    public class Bootstrap {
      public static Class<? extends Annotation> INSTANCE = null;
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  public static @interface OnMethodExit {
    public static final String NAME = OnMethodExit.class.getName();
    public static class Bootstrap {
      public static Class<? extends Annotation> INSTANCE = null;
    }
  }
}
