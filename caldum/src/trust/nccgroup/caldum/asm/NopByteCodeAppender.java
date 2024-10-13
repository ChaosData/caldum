/*
Copyright 2024 Jeff Dileo

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

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
