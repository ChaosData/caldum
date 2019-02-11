package trust.nccgroup.caldumtest.test;

//import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/*
@RunWith(ClasspathSuite.class)
@ClasspathSuite.IncludeJars(true)
@ClasspathSuite.ClassnameFilters({"trust\\.nccgroup\\.caldum\\.test\\..*", ".*"})
public class RunAllTests {}
*/


@RunWith(Suite.class)
@Suite.SuiteClasses({
  Version.class,
  StringHookTest.class,
})
public class RunAllTests {}
