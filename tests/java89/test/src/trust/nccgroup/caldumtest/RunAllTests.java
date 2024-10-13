/*
Copyright 2019 NCC Group
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

package trust.nccgroup.caldumtest;

import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import trust.nccgroup.caldumtest.test.*;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  Version.class,
  StringHookTest.class,
  ProviderTest.class,
  NoRecursionTest.class,
  SpringTest.class,
})
public class RunAllTests {

    @After
    public void doAfter() {
        System.out.println("doAfter called()");
        //System.exit(0);
    }

}
