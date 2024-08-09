package trust.nccgroup.caldum.asm;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.utility.visitor.StackAwareMethodVisitor;
import trust.nccgroup.caldum.annotation.Dynamic;
import trust.nccgroup.caldum.util.TmpLogger;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DynamicFields extends StackAwareMethodVisitor implements Opcodes {
  public static final String DYNVARS = "__dynvars__";
  public static final String DYNANNOS = "__dynannos__";
  public static final String DYNNSVARS = "__dynnsvars__";
  private static final Logger logger = TmpLogger.DEFAULT;

  private final String iclass;
  private final MethodDescription instrumentedMethod;
  private final boolean forInitializer;

  public static Map<String, Boolean> owners_dynamic = new HashMap<String, Boolean>();

  public DynamicFields(Class<?> clazz,
                       MethodDescription instrumentedMethod,
                       MethodVisitor methodVisitor,
                       boolean forInitializer) {

    this(clazz.getName(), instrumentedMethod, methodVisitor, forInitializer);
  }

  public DynamicFields(TypeDescription instrumentedType,
                       MethodDescription instrumentedMethod,
                       MethodVisitor methodVisitor,
                       boolean forInitializer) {

    this(instrumentedType.getTypeName(), instrumentedMethod, methodVisitor, forInitializer);
  }


  public DynamicFields(String clazz,
                       MethodDescription instrumentedMethod,
                       MethodVisitor methodVisitor,
                       boolean forInitializer) {
    super(methodVisitor, instrumentedMethod);
    this.iclass = internal(clazz);
    this.instrumentedMethod = instrumentedMethod;
    this.forInitializer = forInitializer;
  }

  private static String external(String s) {
    return s.replace('/','.');
  }

  private static String internal(String s) {
    return s.replace('.','/');
  }

  private static String internal(Class<?> c) {
    return c.getName().replace('.','/');
  }

  public void visitCode() { // instrument function entry
    super.visitCode();
    if (forInitializer) {
      super.visitTypeInsn(NEW, "java/util/HashMap");
      super.visitInsn(DUP);
      super.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
      super.visitFieldInsn(PUTSTATIC, iclass, DYNVARS, "Ljava/util/Map;");
    }
  }

  public void visitFieldInsn(int opcode, String owner, String name, String desc) {
    //System.out.println("visitFieldInsn: " + owner);

    boolean doswap = false;

    if (opcode == GETSTATIC || opcode == PUTSTATIC) {
      //logger.info("opcode: " + opcode + " owner: " + owner + " name: " + name + " desc: " + desc + " iclass: " + iclass);
      boolean is_dynamic = iclass.equals(owner);
      if (!is_dynamic) {
        Boolean owner_dynamic = owners_dynamic.get(owner);
        if (owner_dynamic == null) {
          try {
            Class<?> c = Class.forName(external(owner));
            Annotation[] cas = c.getDeclaredAnnotations();

            for (Annotation ca : cas) {
              if (ca.annotationType().getName().equals(Dynamic.NAME)) {
                is_dynamic = true;
                owners_dynamic.put(owner, Boolean.TRUE);
                break;
              }
            }
            //this appears to fail to find the annotations, probably b/c the bootstrap injected version
            //is considered different from the .class one here
//            Dynamic d = c.getAnnotation(Dynamic.class);
//            if (d != null) {
//              logger.info("is_dynamic: true for " + owner + " " + name);
//              is_dynamic = true;
//              owners_dynamic.put(owner, Boolean.TRUE);
//            }
          } catch (Throwable t) {
            //probably possible to get in a loop if two @Dynamic classes refer to each other
            //might need to do this another way
            //maybe force reload/reinstrument after the other classes are loaded
            logger.log(Level.SEVERE, "error loading class", t);
          }
        }
      }

      if (is_dynamic && !DYNVARS.equals(name)) {
        if (opcode == GETSTATIC) {
          super.visitFieldInsn(GETSTATIC, owner, DYNVARS, "Ljava/util/Map;");
          super.visitLdcInsn(name);
          super.visitMethodInsn(INVOKEINTERFACE, internal(Map.class), "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
          if ("J".equals(desc)) {
            super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long");
            super.visitMethodInsn(INVOKEVIRTUAL, internal(Long.class), "longValue", "()J", false);
          } else if ("D".equals(desc)) {
            super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double");
            super.visitMethodInsn(INVOKEVIRTUAL, internal(Double.class), "doubleValue", "()D", false);
          } else if ("Z".equals(desc)) {
            super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
            super.visitMethodInsn(INVOKEVIRTUAL, internal(Boolean.class), "booleanValue", "()Z", false);
          } else if ("B".equals(desc)) {
            super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Byte");
            super.visitMethodInsn(INVOKEVIRTUAL, internal(Byte.class), "byteValue", "()B", false);
          } else if ("C".equals(desc)) {
            super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character");
            super.visitMethodInsn(INVOKEVIRTUAL, internal(Character.class), "charValue", "()C", false);
          } else if ("S".equals(desc)) {
            super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Short");
            super.visitMethodInsn(INVOKEVIRTUAL, internal(Short.class), "shortValue", "()S", false);
          } else if ("I".equals(desc)) {
            super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
            super.visitMethodInsn(INVOKEVIRTUAL, internal(Integer.class), "intValue", "()I", false);
          } else if ("F".equals(desc)) {
            super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float");
            super.visitMethodInsn(INVOKEVIRTUAL, internal(Float.class), "floatValue", "()F", false);
          } else {
            if (desc.startsWith("L")) {
              String d2 = desc.substring(1, desc.length()-1);
              super.visitTypeInsn(Opcodes.CHECKCAST, d2);
            } else {
              // todo: handle arrays
              super.visitTypeInsn(Opcodes.CHECKCAST, desc);
            }
          }
          return;
        } else if (opcode == PUTSTATIC) {
          // we just don't support setting these things right now, so we just clear the value off the stack to NOP it.
          // getting an extra local variable to stash the double-sized values is complicated.
          if ("J".equals(desc)) {
            logger.warning("Dynamic variable set for type `long` not supported in `" +
              instrumentedMethod.toString() + "`. Operation will be disabled in generated code.");
            super.visitInsn(Opcodes.POP2);
            return;
          } else if ("D".equals(desc)) {
            logger.warning("Dynamic variable set for type `double` not supported in `" +
              instrumentedMethod.toString() + "`. Operation will be disabled in generated code.");
            super.visitInsn(Opcodes.POP2);
            return;
          } else {
            // because the value is already on the stack, we play some tricks to shift it down
            //   without using a local variable.
            super.visitFieldInsn(GETSTATIC, owner, DYNVARS, "Ljava/util/Map;");
            super.visitInsn(Opcodes.SWAP);
            super.visitLdcInsn(name);
            super.visitInsn(Opcodes.SWAP);
            super.visitMethodInsn(INVOKEINTERFACE, internal(Map.class), "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
            super.visitInsn(Opcodes.POP);

            return;
          }
        }
      }/* else {
        System.out.println(">> iclass != owner: " + iclass + " != " + owner + ", name: " + name + ", desc: " + desc);
      }*/
    }

    super.visitFieldInsn(opcode, owner, name, desc);
  }
}
