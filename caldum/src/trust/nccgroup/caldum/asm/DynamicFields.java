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

  private final Class<?> newClass;
  private final Class<?> alreadyInjectedClass;
  private final TypeDescription instrumentedType;
  private final MethodDescription instrumentedMethod;

  public DynamicFields(Class<?> newClass, Class<?> alreadyInjectedClass,
                       TypeDescription instrumentedType,
                       MethodDescription instrumentedMethod,
                       MethodVisitor methodVisitor) {

    super(methodVisitor, instrumentedMethod);
    this.newClass = newClass;
    this.alreadyInjectedClass = alreadyInjectedClass;
    this.instrumentedType = instrumentedType;
    this.instrumentedMethod = instrumentedMethod;
  }

  private static String internal(String s) {
    return s.replace('.','/');
  }

  private static String internal(Class<?> c) {
    return c.getName().replace('.','/');
  }

  public void visitFieldInsn(int opcode, String owner, String name, String desc) {
    boolean doswap = false;

//    if ("foobar".equals(name)) {
//      name = DYNVARS;
//    }

    if (opcode == GETSTATIC || opcode == PUTSTATIC) {
      if (internal(newClass).equals(owner) && !DYNVARS.equals(name)) {
        if (opcode == GETSTATIC) {
          super.visitFieldInsn(GETSTATIC, internal(newClass), DYNVARS, "Ljava/util/Map;");
          super.visitLdcInsn(name);
          super.visitMethodInsn(INVOKEINTERFACE, internal(Map.class), "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
          if ("J".equals(desc)) {
            super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long");
            super.visitMethodInsn(INVOKEVIRTUAL, internal(Long.class), "longValue", "()J", false);
          } else if ("D".equals(desc)) {
            super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double");
            super.visitMethodInsn(INVOKEVIRTUAL, internal(Double.class), "doubleValue", "()D", false);
          }
          return;
        } else {
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
            super.visitFieldInsn(GETSTATIC, internal(newClass), DYNVARS, "Ljava/util/Map;");
            super.visitInsn(Opcodes.SWAP);
            super.visitLdcInsn(name);
            super.visitInsn(Opcodes.SWAP);
            super.visitMethodInsn(INVOKEINTERFACE, internal(Map.class), "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
            super.visitInsn(Opcodes.POP);
            return;
          }
        }
      }
    }

    super.visitFieldInsn(opcode, owner, name, desc);
  }
}
