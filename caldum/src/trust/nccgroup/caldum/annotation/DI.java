/*
Copyright 2018 NCC Group
Copyright 2024 Jeff Dileo

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

public class DI {
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface Inject {
    public static final String NAME = Inject.class.getName();
    public static class Bootstrap {
      public static Class<? extends Annotation> INSTANCE = null;
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  public static @interface Provide {
    public static final String NAME = Provide.class.getName();

    String name() default "";
    public static class Bootstrap {
      public static Class<? extends Annotation> INSTANCE = null;
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  public static @interface Provider {
    public static final String NAME = Provider.class.getName();
    public static class Bootstrap {
      public static Class<? extends Annotation> INSTANCE = null;
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  public static @interface AgentClassLoader {
    public static final String NAME = AgentClassLoader.class.getName();
    public static class Bootstrap {
      public static Class<? extends Annotation> INSTANCE = null;
    }
  }
}
