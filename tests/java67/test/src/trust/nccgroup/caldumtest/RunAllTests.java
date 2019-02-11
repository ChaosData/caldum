package trust.nccgroup.caldumtest;

//import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import trust.nccgroup.caldumtest.test.*;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  Version.class,
  StringHookTest.class,
})
public class RunAllTests {}
