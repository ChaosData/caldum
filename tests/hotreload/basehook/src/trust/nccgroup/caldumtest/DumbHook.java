package trust.nccgroup.caldumtest;

import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import trust.nccgroup.caldum.annotation.*;
import trust.nccgroup.caldum.annotation.DI.Inject;

import java.util.Arrays;
import java.util.logging.Logger;
import java.util.logging.Level;

import static net.bytebuddy.asm.Advice.OnMethodExit;
import static net.bytebuddy.asm.Advice.Return;
import static net.bytebuddy.matcher.ElementMatchers.*;
import static trust.nccgroup.caldum.annotation.Matcher.Member;
import static trust.nccgroup.caldum.annotation.Matcher.Type;

public class DumbHook {

  @trust.nccgroup.caldum.annotation.Dynamic
  @Hook
  public static class NopHook {

    public static class Settings {
      @Matcher.Ignore
      static ElementMatcher i = any();

      @Type
      static ElementMatcher typeMatcher() {
        return named("foo.Bar");
      }

      @Member
      static ElementMatcher m = isMethod().and(named("foobar"));
    }

    public static String retval = "__notsecret!__";

    @OnMethodExit
    static void exit() {
      System.out.println("should not be reached!");
    }

  }

}
