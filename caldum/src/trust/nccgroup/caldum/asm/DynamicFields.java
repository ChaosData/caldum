package trust.nccgroup.caldum.asm;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.visitor.StackAwareMethodVisitor;
import trust.nccgroup.caldum.annotation.Dynamic;
import trust.nccgroup.caldum.util.TmpLogger;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DynamicFields extends StackAwareMethodVisitor implements Opcodes {
  public static final String DYNVARS = "__dynvars__";
  public static final String DYNANNOS = "__dynannos__";
  public static final String DYNNSVARS = "__dynnsvars__";
  private static final Logger logger = TmpLogger.DEFAULT;

  private final String iclass;
  private final MethodDescription instrumentedMethod;
  private final Type type;

  public static Map<String, Boolean> owners_dynamic = new HashMap<String, Boolean>();

  public enum Type {
    TypeInitializer,
    Constructor,
    Method
  }

  public DynamicFields(Class<?> clazz,
                       MethodDescription instrumentedMethod,
                       MethodVisitor methodVisitor,
                       Type _type) {

    this(clazz.getName(), instrumentedMethod, methodVisitor, _type);
  }

  public DynamicFields(TypeDescription instrumentedType,
                       MethodDescription instrumentedMethod,
                       MethodVisitor methodVisitor,
                       Type _type) {

    this(instrumentedType.getTypeName(), instrumentedMethod, methodVisitor, _type);
  }

  public DynamicFields(String clazz,
                       MethodDescription instrumentedMethod,
                       MethodVisitor methodVisitor,
                       Type _type) {
    super(methodVisitor, instrumentedMethod);
    this.iclass = internal(clazz);
    this.instrumentedMethod = instrumentedMethod;
    this.type = _type;
  }

  public static AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper methodVisitor(final Type _type) {
    return new AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper() {
      @Override
      public MethodVisitor wrap(TypeDescription instrumentedType,
                                MethodDescription instrumentedMethod,
                                MethodVisitor methodVisitor,
                                Implementation.Context implementationContext,
                                TypePool typePool, int writerFlags, int readerFlags) {
        return new DynamicFields(instrumentedType, instrumentedMethod, methodVisitor, _type);
      }
    };
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

    if (type == Type.TypeInitializer) {
      super.visitTypeInsn(NEW, "java/util/HashMap");
      super.visitInsn(DUP);
      super.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
      super.visitFieldInsn(PUTSTATIC, iclass, DYNVARS, "Ljava/util/Map;");
    } else if (type == Type.Constructor) {
      /*
         0: aload_0
         1: invokespecial #1                  // Method java/lang/Object."<init>":()V
         4: aload_0
         5: new           #2                  // class java/util/HashMap
         8: dup
         9: invokespecial #3                  // Method java/util/HashMap."<init>":()V
        12: putfield      #4                  // Field __dynnsvars__:Ljava/util/Map;
        15: aload_0
        16: getfield      #4                  // Field __dynnsvars__:Ljava/util/Map;
        19: ldc           #5                  // String test
        21: ldc           #6                  // String foo
        23: invokeinterface #7,  3            // InterfaceMethod java/util/Map.put:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
        28: pop
        29: return
      */
      super.visitVarInsn(ALOAD, 0);
      super.visitTypeInsn(NEW, "java/util/HashMap");
      super.visitInsn(DUP);
      super.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
      super.visitFieldInsn(PUTFIELD, iclass, DYNNSVARS, "Ljava/util/Map;");
    }
  }

  private boolean isDynamic(String owner) {
    boolean is_dynamic = iclass.equals(owner);
    if (!is_dynamic) {
      Boolean owner_dynamic = owners_dynamic.get(owner);
      if (owner_dynamic != null) {
        is_dynamic = owner_dynamic;
      } else {
        try {
          Class<?> c = Class.forName(external(owner), true, null);
          Annotation d = c.getAnnotation(Dynamic.Bootstrap.INSTANCE);
          if (d != null) {
            is_dynamic = true;
            owners_dynamic.put(owner, Boolean.TRUE);
          }
        } catch (Throwable t) {
          //probably possible to get in a loop if two @Dynamic classes refer to each other
          //might need to do this another way
          //maybe force reload/reinstrument after the other classes are loaded
          logger.log(Level.SEVERE, "error loading class", t);
        }
      }
    }
    return is_dynamic;
  }

  private final static Map<String,String[]> nativePuts;
  static {
    nativePuts = new HashMap<String,String[]>();
    nativePuts.put("J", new String[]{"java/lang/Long", "(J)Ljava/lang/Long;"});
    nativePuts.put("D", new String[]{"java/lang/Double", "(D)Ljava/lang/Double;"});
    nativePuts.put("Z", new String[]{"java/lang/Boolean", "(Z)Ljava/lang/Boolean;"});
    nativePuts.put("B", new String[]{"java/lang/Byte", "(B)Ljava/lang/Byte;"});
    nativePuts.put("C", new String[]{"java/lang/Character", "(C)Ljava/lang/Character;"});
    nativePuts.put("S", new String[]{"java/lang/Short", "(S)Ljava/lang/Short;"});
    nativePuts.put("I", new String[]{"java/lang/Integer", "(I)Ljava/lang/Integer;"});
    nativePuts.put("F", new String[]{"java/lang/Float", "(F)Ljava/lang/Float;"});
  }

  private final static Map<String,String[]> nativeGets;
  static {
    nativeGets = new HashMap<String,String[]>();
    nativeGets.put("J", new String[]{"java/lang/Long", "longValue", "()J;"});
    nativeGets.put("D", new String[]{"java/lang/Double", "doubleValue", "()D"});
    nativeGets.put("Z", new String[]{"java/lang/Boolean", "booleanValue", "()Z"});
    nativeGets.put("B", new String[]{"java/lang/Byte", "byteValue", "()B"});
    nativeGets.put("C", new String[]{"java/lang/Character", "charValue", "()C"});
    nativeGets.put("S", new String[]{"java/lang/Short", "shortValue", "()S"});
    nativeGets.put("I", new String[]{"java/lang/Integer", "intValue", "()I"});
    nativeGets.put("F", new String[]{"java/lang/Float", "floatValue", "()F"});
  }

  private void getCasts(int opcode, String owner, String name, String desc) {
    String[] ng = nativeGets.get(desc);
    if (ng != null) {
      super.visitTypeInsn(Opcodes.CHECKCAST, ng[0]);
      super.visitMethodInsn(INVOKEVIRTUAL, ng[0], ng[1], ng[2], false);
    } else if (desc.startsWith("L")) {
      String d2 = desc.substring(1, desc.length() - 1);
      super.visitTypeInsn(Opcodes.CHECKCAST, d2);
    } else if (desc.startsWith("[")) {
      // todo: handle arrays
      super.visitTypeInsn(Opcodes.CHECKCAST, desc);
    } else {
      logger.severe("unexpected desc: " + desc);
      super.visitTypeInsn(Opcodes.CHECKCAST, desc);
    }

//    if ("J".equals(desc)) {
//      super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long");
//      super.visitMethodInsn(INVOKEVIRTUAL, internal(Long.class), "longValue", "()J", false);
//    } else if ("D".equals(desc)) {
//      super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double");
//      super.visitMethodInsn(INVOKEVIRTUAL, internal(Double.class), "doubleValue", "()D", false);
//    } else if ("Z".equals(desc)) {
//      super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
//      super.visitMethodInsn(INVOKEVIRTUAL, internal(Boolean.class), "booleanValue", "()Z", false);
//    } else if ("B".equals(desc)) {
//      super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Byte");
//      super.visitMethodInsn(INVOKEVIRTUAL, internal(Byte.class), "byteValue", "()B", false);
//    } else if ("C".equals(desc)) {
//      super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character");
//      super.visitMethodInsn(INVOKEVIRTUAL, internal(Character.class), "charValue", "()C", false);
//    } else if ("S".equals(desc)) {
//      super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Short");
//      super.visitMethodInsn(INVOKEVIRTUAL, internal(Short.class), "shortValue", "()S", false);
//    } else if ("I".equals(desc)) {
//      super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
//      super.visitMethodInsn(INVOKEVIRTUAL, internal(Integer.class), "intValue", "()I", false);
//    } else if ("F".equals(desc)) {
//      super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float");
//      super.visitMethodInsn(INVOKEVIRTUAL, internal(Float.class), "floatValue", "()F", false);
//    } else {
//      if (desc.startsWith("L")) {
//        String d2 = desc.substring(1, desc.length()-1);
//        super.visitTypeInsn(Opcodes.CHECKCAST, d2);
//      } else {
//        // todo: handle arrays
//        super.visitTypeInsn(Opcodes.CHECKCAST, desc);
//      }
//    }
  }

  private boolean putCasts(int opcode, String owner, String name, String desc) {
    // we just don't support setting these things right now, so we just clear the value off the stack to NOP it.
    // getting an extra local variable to stash the double-sized values is complicated.

    /*
      10: aload_0
      11: ldc2_w        #4                  // long 57l
      14: putfield      #6                  // Field ll:J
      ////
      32: aload         4
      34: getfield      #9                  // Field __dynnsvars__:Ljava/util/Map;
      37: ldc           #17                 // String l
      39: lload_2
      40: invokestatic  #18                 // Method java/lang/Long.valueOf:(J)Ljava/lang/Long;
      43: invokeinterface #19,  3           // InterfaceMethod java/util/Map.put:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
      48: pop
     */

    String[] np = nativePuts.get(desc);
    if (np == null) {
      return true;
    }
    super.visitMethodInsn(INVOKESTATIC, np[0], "valueOf", np[1], false);

//    if ("J".equals(desc)) {
//      logger.warning("Dynamic variable set for type `long` not supported in `" +
//          instrumentedMethod.toString() + "`. Operation will be disabled in generated code.");
//      super.visitInsn(Opcodes.POP2);
//      return false;
//    } else if ("D".equals(desc)) {
//      logger.warning("Dynamic variable set for type `double` not supported in `" +
//          instrumentedMethod.toString() + "`. Operation will be disabled in generated code.");
//      super.visitInsn(Opcodes.POP2);
//      return false;
//    }
//
//    //todo: test things like int/boolean


    return true;
  }

  public void putStatic(int opcode, String owner, String name, String desc) {
    if (!putCasts(opcode, owner, name, desc)) {
      return;
    }

    // because the value is already on the stack, we play some tricks to shift it down
    //   without using a local variable.
    super.visitFieldInsn(GETSTATIC, owner, DYNVARS, "Ljava/util/Map;");
    super.visitInsn(Opcodes.SWAP);
    super.visitLdcInsn(name);
    super.visitInsn(Opcodes.SWAP);
    super.visitMethodInsn(INVOKEINTERFACE, internal(Map.class), "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
    super.visitInsn(Opcodes.POP);
  }

  public void getStatic(int opcode, String owner, String name, String desc) {
    super.visitFieldInsn(GETSTATIC, owner, DYNVARS, "Ljava/util/Map;");
    super.visitLdcInsn(name);
    super.visitMethodInsn(INVOKEINTERFACE, internal(Map.class), "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
    getCasts(opcode, owner, name, desc);
  }

  public void putField(int opcode, String owner, String name, String desc) {
    /*
      15: aload_0
      16: getfield      #4                  // Field __dynnsvars__:Ljava/util/Map;
      19: ldc           #5                  // String test
      21: ldc           #6                  // String foo
      23: invokeinterface #7,  3            // InterfaceMethod java/util/Map.put:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
      28: pop
      ///////////
      4: aload_0
      5: ldc           #2                  // String foo
      7: putfield      #3                  // Field test:Ljava/lang/String;
      ///////////
      4: aload_0
      5: ldc           #2                  // String foo
      //7: putfield      #3                  // Field test:Ljava/lang/String;
      [ this, foo ]
      ->swap
      [ foo, this ]
      ->getfield // Field __dynnsvars__:Ljava/util/Map;
      [ foo, __dynnsvars__ ]
      ->swap
      [ __dynnsvars__, foo ]
      ->ldc      // String test
      [ __dynnsvars__, foo, test ]
      ->swap
      [ __dynnsvars__, test, foo ]
      ->invokeinterface #7,  3            // InterfaceMethod java/util/Map.put:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
      [ put_ret ]
      ->pop
      [ ]
     */

    if (!putCasts(opcode, owner, name, desc)) {
      return;
    }
    //super.visitVarInsn(ALOAD, 0);
    super.visitInsn(Opcodes.SWAP);
    super.visitFieldInsn(GETFIELD, owner, DYNNSVARS, "Ljava/util/Map;");
    super.visitInsn(Opcodes.SWAP);
    super.visitLdcInsn(name);
    super.visitInsn(Opcodes.SWAP);
    super.visitMethodInsn(INVOKEINTERFACE, internal(Map.class), "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
    super.visitInsn(Opcodes.POP);
  }

  public void getField(int opcode, String owner, String name, String desc) {
    /*
      52: getfield      #4                  // Field __dynnsvars__:Ljava/util/Map;
      55: ldc           #5                  // String test
      57: invokeinterface #20,  2           // InterfaceMethod java/util/Map.get:(Ljava/lang/Object;)Ljava/lang/Object;
      ////
      52: getfield      #3                  // Field test:Ljava/lang/String;
      ////
      //52: getfield      #3                  // Field test:Ljava/lang/String;
      ->getfield __dynnsvars__
      ->ldc <name>
      ->invokeinterface #20,  2           // InterfaceMethod java/util/Map.get:(Ljava/lang/Object;)Ljava/lang/Object;
      ->[if needed] unboxing
     */
    super.visitFieldInsn(GETFIELD, owner, DYNNSVARS, "Ljava/util/Map;");
    super.visitLdcInsn(name);
    super.visitMethodInsn(INVOKEINTERFACE, internal(Map.class), "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
    getCasts(opcode, owner, name, desc);
  }


  public void visitFieldInsn(int opcode, String owner, String name, String desc) {
    //System.out.println("visitFieldInsn: " + owner);

    if (opcode == GETSTATIC) {
      boolean is_dynamic = isDynamic(owner);
      if (is_dynamic && !DYNVARS.equals(name)) {
        getStatic(opcode, owner, name, desc);
        return;
      }
    } else if (opcode == PUTSTATIC) {
      boolean is_dynamic = isDynamic(owner);
      if (is_dynamic && !DYNVARS.equals(name)) {
        putStatic(opcode, owner, name, desc);
        return;
      }
    } else if (opcode == GETFIELD) {
      boolean is_dynamic = isDynamic(owner);
      if (is_dynamic && !DYNNSVARS.equals(name)) {
        getField(opcode, owner, name, desc);
        return;
      }
    } else if (opcode == PUTFIELD) {
      boolean is_dynamic = isDynamic(owner);
      if (is_dynamic && !DYNNSVARS.equals(name)) {
        putField(opcode, owner, name, desc);
        return;
      }
    }

    super.visitFieldInsn(opcode, owner, name, desc);
  }
}
