package trust.nccgroup.caldumtest.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringApplication;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.lang.instrument.*;
import java.lang.reflect.*;
import java.security.ProtectionDomain;
import java.nio.file.*;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;

import com.google.common.util.concurrent.SettableFuture;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

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
    MyApplication.main(new String[]{});

    OkHttpClient c = new OkHttpClient();
    Request req = new Request.Builder()
      .url("http://127.0.0.1:8080/test?name=zzzzz")
      .build();
    String resText = null;
    try {
      Response res = c.newCall(req).execute();
      resText = res.body().string();
    } catch (Throwable t) {
      t.printStackTrace();
      fail("unknown error");
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
  }

  @Configuration
  @EnableAutoConfiguration
  static class MyApplication {
    public static void main(String[] fakeargv) {
      System.setProperty("server.address", "127.0.0.1");
      SpringApplication.run(MyApplication.class, fakeargv);
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
