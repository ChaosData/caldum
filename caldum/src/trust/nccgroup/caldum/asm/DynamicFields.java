package trust.nccgroup.caldum.asm;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.utility.visitor.StackAwareMethodVisitor;
import trust.nccgroup.caldum.util.TmpLogger;

import java.util.Map;
import java.util.logging.Logger;

public class DynamicFields extends StackAwareMethodVisitor implements Opcodes {
  public static final String DYNVARS = "__dynvars__";
  private static final Logger logger = TmpLogger.DEFAULT;

  private final String iclass;
  private final MethodDescription instrumentedMethod;
  private final boolean forInitializer;

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

//    if ("foobar".equals(name)) {
//      name = DYNVARS;
//    }


    if (opcode == GETSTATIC || opcode == PUTSTATIC) {
      if (iclass.equals(owner) && !DYNVARS.equals(name)) {
        if (opcode == GETSTATIC) {
          super.visitFieldInsn(GETSTATIC, iclass, DYNVARS, "Ljava/util/Map;");
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
            super.visitFieldInsn(GETSTATIC, iclass, DYNVARS, "Ljava/util/Map;");
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
