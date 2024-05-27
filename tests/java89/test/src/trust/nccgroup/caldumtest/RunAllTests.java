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
