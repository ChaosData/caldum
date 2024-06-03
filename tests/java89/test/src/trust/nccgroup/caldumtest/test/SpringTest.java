package trust.nccgroup.caldumtest.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import ch.qos.logback.classic.Level;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringApplication;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.lang.instrument.*;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;

import com.google.common.util.concurrent.SettableFuture;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
//import java.util.logging.Level;


public class SpringTest {

  public static Instrumentation inst = null;
  public static SettableFuture<Optional<Runnable>> routeTestFuture = SettableFuture.create();
  public static SettableFuture<Optional<Runnable>> paramTestFuture = SettableFuture.create();

  /*static class Dumper implements ClassFileTransformer {
    public byte[] transform​(ClassLoader loader, String className, Class<?> classBeingRedefined,
                      ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
      if (classBeingRedefined != null) {
        try {
          Path path = Paths.get("./" + classBeingRedefined.getName() + ".class");
          Files.write(path, classfileBuffer);
        } catch(Throwable t) {
          t.printStackTrace();
        }
      }
      return null;
    }
  }*/

  @Test
  public void startspring() throws Throwable {
    MyApplication.main();

    String resText = null;
    for (int i=0; i<3; i++) {
      OkHttpClient c = new OkHttpClient();
      Request req = new Request.Builder()
        .url("http://127.0.0.1:8084/test?name=zzzzz")
        .build();
      try {
        Response res = c.newCall(req).execute();
        resText = res.body().string();
      } catch (Throwable t) {
        t.printStackTrace();
        fail("unknown error");
      }
      //PausedMain.pause("" + (7778 + i));
    }

    try {
      while (true) {
        try {
          routeTestFuture.get().ifPresent((test) -> { test.run(); });
          break;
        } catch (InterruptedException ignored) { }
      }
      while (true) {
        try {
          paramTestFuture.get().ifPresent((test) -> { test.run(); });
          break;
        } catch (InterruptedException ignored) { }
      }
    } catch (ExecutionException ee) {
      throw (Throwable)ee;
    }
    assertEquals("Hello caldum", resText);

    /*try {
      Dumper d = new Dumper();
      inst.addTransformer(d, true);
      inst.retransformClasses(org.apache.catalina.connector.Request.class);
      inst.removeTransformer(d);
    } catch (Throwable t) {
      t.printStackTrace();
    }*/

    //test for SpringHook @DumpWrappers
    byte [] wrapped_pattern_bytes = "java/lang/Thread".getBytes();
    try {
      RandomAccessFile f = new RandomAccessFile("./trust.nccgroup.caldumtest.SpringHook$HttpServeletRequestGetRequestURIWrapper.class", "r");
      byte[] b = new byte[(int)f.length()];
      f.readFully(b);
      int pos = bytesIndexOf(b, wrapped_pattern_bytes, 0);
      assertEquals(-1, pos);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }

    try {
      RandomAccessFile f = new RandomAccessFile("./trust.nccgroup.caldumtest.SpringHook$HttpServeletRequestGetRequestURIWrapper.wrapped.class", "r");
      byte[] b = new byte[(int)f.length()];
      f.readFully(b);
      int pos = bytesIndexOf(b, wrapped_pattern_bytes, 0);
      assertNotEquals(-1, pos);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  static int bytesIndexOf(byte[] source, byte[] search, int fromIndex) {
    boolean find = false;
    int i;
    for (i = fromIndex; i <= (source.length - search.length); i++) {
      if (source[i] == search[0]) {
        find = true;
        for (int j = 0; j < search.length; j++) {
          if (source[i + j] != search[j]) {
            find = false;
          }
        }
      }
      if (find) {
        break;
      }
    }
    if (!find) {
      return -1;
    }
    return i;
  }

  @Configuration
  @EnableAutoConfiguration
  static class MyApplication {
    public static void main() {
      System.setProperty("server.address", "127.0.0.1");
      System.setProperty("server.port", "8084");

//      String cerl = "org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener";
//      Log l = LogFactory.getLog(cerl);
//      try {
//        Field f = l.getClass().getSuperclass().getDeclaredField("logger");
//        f.setAccessible(true);
//        ch.qos.logback.classic.Logger ll = (ch.qos.logback.classic.Logger)f.get(l);
//        ll.setLevel(Level.ERROR);
//      } catch (Throwable t) {t.printStackTrace();}

      java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.OFF);

      ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
      root.setLevel(ch.qos.logback.classic.Level.OFF);

      SpringApplication app = new SpringApplicationBuilder()
        .sources(MyApplication.class)
        .logStartupInfo(false)
        .build();
      //app.setDefaultProperties(System.getProperties());
      app.setLogStartupInfo(false);
      app.run(new String[]{
        "--spring.main.banner-mode=OFF",
      });
    }

    @RestController
    static class TestController {
      @RequestMapping("/test")
      public String test() {
        routeTestFuture.set(Optional.of(() -> {
          fail("Reaching this implies that /test was not changed to /nottest by the Advice.");
        }));
        return "failure";
      }

      @RequestMapping("/nottest")
      public String test(@RequestParam(name="name", defaultValue="World") String name, HttpServletRequest req) {
        routeTestFuture.set(Optional.empty());
        paramTestFuture.set(Optional.of(() -> {
          assertEquals("caldum", name);
        }));
        return "Hello " + name;
      }

    }

  }


}
