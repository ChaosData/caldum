package trust.nccgroup.caldum.asm;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.MethodVisitor;

import static net.bytebuddy.jar.asm.Opcodes.NOP;

public class NopByteCodeAppender implements ByteCodeAppender {
  @Override
  public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
    methodVisitor.visitInsn(NOP);
    return Size.ZERO;
  }
}
