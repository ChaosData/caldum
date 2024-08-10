package trust.nccgroup.caldumtest.generated;

import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import trust.nccgroup.caldum.annotation.*;
import trust.nccgroup.caldum.annotation.DI.Inject;

import java.util.Arrays;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;
import java.lang.reflect.*;

import static net.bytebuddy.asm.Advice.OnMethodExit;
import static net.bytebuddy.asm.Advice.Return;
import static net.bytebuddy.matcher.ElementMatchers.*;
import static trust.nccgroup.caldum.annotation.Matcher.Member;
import static trust.nccgroup.caldum.annotation.Matcher.Type;

public class TemplatedFileHook {
  @Hook
  @Dynamic
  //@Dump
  @Debug
  public static class FileAbsPathHook {

    public String test = "baz";
    public static int foo = 57;
    public int foo2 = 67;
    public long longone = 68L;

    public static Map __dynvars__;
    //public long ll = 57;
    //public Map<String,Object> __dynnsvars__ = new HashMap<String,Object>();

    static {
    }


    public static class Settings {
      @Matcher.Ignore
      static ElementMatcher i = none();

      @Type
      static ElementMatcher typeMatcher() {
        return is(java.io.File.class);
      }

      @Member
      static ElementMatcher m = isMethod().and(named("getAbsolutePath"));
    }

    @Inject
    public static Logger logger;

    public static String replacement = "__notsecret!!__";

    @OnMethodExit
    static void exit(@Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object ret) {
      String _s = (String)ret;
      if (_s != null && _s.indexOf("__secret__") != -1) {
        System.out.println("!!!!!!!!!!!!!!");
        try {
          //long l = 42;
          FileAbsPathHook f = new FileAbsPathHook();
          //f.__dynnsvars__.put("l", l);
          //f.longone = 42;
          foo = 43;
          f.foo2 = 44;
          System.out.println("__secret__ found in File::getAbsolutePath(), returning " + replacement + " (" + f.test + ")" + foo + ":" + f.foo2 + ":" + f.longone);
          //System.out.println("__secret__ found in File::getAbsolutePath(), returning " + replacement + " (" + f.test + ")" + foo + ":" + f.foo2);
        } catch (Throwable t) { t.printStackTrace(); }
        ret = _s.replace("__secret__", "" + replacement);
      }
    }

  }

}
