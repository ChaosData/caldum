package trust.nccgroup.caldumtest.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
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


public class SpringTest {

  public static Instrumentation inst = null;

  static class Dumper implements ClassFileTransformer {
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
  }

  @Test
  public void startspring() {
    MyApplication.main(new String[]{});

    OkHttpClient c = new OkHttpClient();
    Request req = new Request.Builder()
      .url("http://127.0.0.1:8080/test?name=zzzzz")
      .build();
    try {
      Response res = c.newCall(req).execute();
      System.out.println(res.body().string());
    } catch (Throwable t) {
      t.printStackTrace();
    }

    try {
      Dumper d = new Dumper();
      inst.addTransformer(d, true);
      inst.retransformClasses(org.apache.catalina.connector.Request.class);
      inst.removeTransformer(d);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  @Configuration
  @EnableAutoConfiguration
  static class MyApplication {
    public static void main(String[] fakeargv) {
      SpringApplication.run(MyApplication.class, fakeargv);
    }

    @RestController
    static class TestController {
      @RequestMapping("/test")
      public String test(@RequestParam(value="name", defaultValue="World") String name, HttpServletRequest req) {
        System.out.println(req.getRequestURI());
        return "Hello " + name;
      }

    }

  }


}
