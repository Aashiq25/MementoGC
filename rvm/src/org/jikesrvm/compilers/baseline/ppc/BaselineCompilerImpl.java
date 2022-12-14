/*
 *  This file is part of the Jikes RVM project (http://jikesrvm.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  See the COPYRIGHT.txt file distributed with this work for information
 *  regarding copyright ownership.
 */
package org.jikesrvm.compilers.baseline.ppc;

import static org.jikesrvm.VM.NOT_REACHED;
import static org.jikesrvm.compilers.baseline.BBConstants.ADDRESS_TYPE;
import static org.jikesrvm.compilers.baseline.BBConstants.DOUBLE_TYPE;
import static org.jikesrvm.compilers.baseline.BBConstants.FLOAT_TYPE;
import static org.jikesrvm.compilers.baseline.BBConstants.INT_TYPE;
import static org.jikesrvm.compilers.baseline.BBConstants.LONGHALF_TYPE;
import static org.jikesrvm.compilers.baseline.BBConstants.LONG_TYPE;
import static org.jikesrvm.compilers.baseline.BBConstants.VOID_TYPE;
import static org.jikesrvm.compilers.common.assembler.ppc.AssemblerConstants.EQ;
import static org.jikesrvm.compilers.common.assembler.ppc.AssemblerConstants.GE;
import static org.jikesrvm.compilers.common.assembler.ppc.AssemblerConstants.GT;
import static org.jikesrvm.compilers.common.assembler.ppc.AssemblerConstants.LE;
import static org.jikesrvm.compilers.common.assembler.ppc.AssemblerConstants.LT;
import static org.jikesrvm.compilers.common.assembler.ppc.AssemblerConstants.NE;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_ADDRESS_PUTFIELD_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_BOOLEAN_PUTFIELD_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_BYTE_ASTORE_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_BYTE_PUTFIELD_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_CHAR_ASTORE_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_CHAR_PUTFIELD_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_DOUBLE_ASTORE_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_DOUBLE_PUTFIELD_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_EXTENT_PUTFIELD_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_FLOAT_ASTORE_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_FLOAT_PUTFIELD_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_INT_ASTORE_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_INT_PUTFIELD_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_LONG_ASTORE_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_LONG_PUTFIELD_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_OBJECT_ALOAD_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_OBJECT_GETFIELD_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_OBJECT_GETSTATIC_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_OBJECT_PUTFIELD_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_OBJECT_PUTSTATIC_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_OFFSET_PUTFIELD_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_SHORT_ASTORE_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_SHORT_PUTFIELD_BARRIER;
import static org.jikesrvm.mm.mminterface.Barriers.NEEDS_WORD_PUTFIELD_BARRIER;
import static org.jikesrvm.objectmodel.TIBLayoutConstants.NEEDS_DYNAMIC_LINK;
import static org.jikesrvm.objectmodel.TIBLayoutConstants.TIB_DOES_IMPLEMENT_INDEX;
import static org.jikesrvm.objectmodel.TIBLayoutConstants.TIB_INTERFACE_DISPATCH_TABLE_INDEX;
import static org.jikesrvm.objectmodel.TIBLayoutConstants.TIB_SUPERCLASS_IDS_INDEX;
import static org.jikesrvm.objectmodel.TIBLayoutConstants.TIB_TYPE_INDEX;
import static org.jikesrvm.ppc.BaselineConstants.F0;
import static org.jikesrvm.ppc.BaselineConstants.F1;
import static org.jikesrvm.ppc.BaselineConstants.F3;
import static org.jikesrvm.ppc.BaselineConstants.FIRST_FIXED_LOCAL_REGISTER;
import static org.jikesrvm.ppc.BaselineConstants.FIRST_FLOAT_LOCAL_REGISTER;
import static org.jikesrvm.ppc.BaselineConstants.FP;
import static org.jikesrvm.ppc.BaselineConstants.JTOC;
import static org.jikesrvm.ppc.BaselineConstants.LAST_FIXED_LOCAL_REGISTER;
import static org.jikesrvm.ppc.BaselineConstants.LAST_FIXED_STACK_REGISTER;
import static org.jikesrvm.ppc.BaselineConstants.LAST_FLOAT_LOCAL_REGISTER;
import static org.jikesrvm.ppc.BaselineConstants.LAST_FLOAT_STACK_REGISTER;
import static org.jikesrvm.ppc.BaselineConstants.MIN_PARAM_REGISTERS;
import static org.jikesrvm.ppc.BaselineConstants.S0;
import static org.jikesrvm.ppc.BaselineConstants.S1;
import static org.jikesrvm.ppc.BaselineConstants.T0;
import static org.jikesrvm.ppc.BaselineConstants.T1;
import static org.jikesrvm.ppc.BaselineConstants.T2;
import static org.jikesrvm.ppc.BaselineConstants.T3;
import static org.jikesrvm.ppc.BaselineConstants.T4;
import static org.jikesrvm.ppc.BaselineConstants.T5;
import static org.jikesrvm.ppc.BaselineConstants.T6;
import static org.jikesrvm.ppc.BaselineConstants.T7;
import static org.jikesrvm.ppc.RegisterConstants.FIRST_NONVOLATILE_GPR;
import static org.jikesrvm.ppc.RegisterConstants.FIRST_OS_PARAMETER_FPR;
import static org.jikesrvm.ppc.RegisterConstants.FIRST_OS_PARAMETER_GPR;
import static org.jikesrvm.ppc.RegisterConstants.FIRST_SCRATCH_FPR;
import static org.jikesrvm.ppc.RegisterConstants.FIRST_SCRATCH_GPR;
import static org.jikesrvm.ppc.RegisterConstants.FIRST_VOLATILE_FPR;
import static org.jikesrvm.ppc.RegisterConstants.FIRST_VOLATILE_GPR;
import static org.jikesrvm.ppc.RegisterConstants.INSTRUCTION_WIDTH;
import static org.jikesrvm.ppc.RegisterConstants.LAST_NONVOLATILE_FPR;
import static org.jikesrvm.ppc.RegisterConstants.LAST_NONVOLATILE_GPR;
import static org.jikesrvm.ppc.RegisterConstants.LAST_OS_PARAMETER_FPR;
import static org.jikesrvm.ppc.RegisterConstants.LAST_OS_PARAMETER_GPR;
import static org.jikesrvm.ppc.RegisterConstants.LAST_SCRATCH_FPR;
import static org.jikesrvm.ppc.RegisterConstants.LAST_SCRATCH_GPR;
import static org.jikesrvm.ppc.RegisterConstants.LAST_VOLATILE_FPR;
import static org.jikesrvm.ppc.RegisterConstants.LAST_VOLATILE_GPR;
import static org.jikesrvm.ppc.RegisterConstants.THREAD_REGISTER;
import static org.jikesrvm.ppc.StackframeLayoutConstants.BYTES_IN_STACKSLOT;
import static org.jikesrvm.ppc.StackframeLayoutConstants.LOG_BYTES_IN_STACKSLOT;
import static org.jikesrvm.ppc.StackframeLayoutConstants.STACKFRAME_ALIGNMENT;
import static org.jikesrvm.ppc.StackframeLayoutConstants.STACKFRAME_FRAME_POINTER_OFFSET;
import static org.jikesrvm.ppc.StackframeLayoutConstants.STACKFRAME_HEADER_SIZE;
import static org.jikesrvm.ppc.StackframeLayoutConstants.STACKFRAME_METHOD_ID_OFFSET;
import static org.jikesrvm.ppc.StackframeLayoutConstants.STACKFRAME_RETURN_ADDRESS_OFFSET;
import static org.jikesrvm.ppc.TrapConstants.CHECKCAST_TRAP;
import static org.jikesrvm.ppc.TrapConstants.MUST_IMPLEMENT_TRAP;
import static org.jikesrvm.runtime.JavaSizeConstants.BYTES_IN_CHAR;
import static org.jikesrvm.runtime.JavaSizeConstants.BYTES_IN_DOUBLE;
import static org.jikesrvm.runtime.JavaSizeConstants.BYTES_IN_FLOAT;
import static org.jikesrvm.runtime.JavaSizeConstants.BYTES_IN_INT;
import static org.jikesrvm.runtime.JavaSizeConstants.BYTES_IN_LONG;
import static org.jikesrvm.runtime.JavaSizeConstants.BYTES_IN_SHORT;
import static org.jikesrvm.runtime.JavaSizeConstants.LOG_BYTES_IN_CHAR;
import static org.jikesrvm.runtime.JavaSizeConstants.LOG_BYTES_IN_DOUBLE;
import static org.jikesrvm.runtime.JavaSizeConstants.LOG_BYTES_IN_FLOAT;
import static org.jikesrvm.runtime.JavaSizeConstants.LOG_BYTES_IN_INT;
import static org.jikesrvm.runtime.JavaSizeConstants.LOG_BYTES_IN_LONG;
import static org.jikesrvm.runtime.JavaSizeConstants.LOG_BYTES_IN_SHORT;
import static org.jikesrvm.runtime.UnboxedSizeConstants.BYTES_IN_ADDRESS;
import static org.jikesrvm.runtime.UnboxedSizeConstants.BYTES_IN_OFFSET;
import static org.jikesrvm.runtime.UnboxedSizeConstants.LOG_BYTES_IN_ADDRESS;

import org.jikesrvm.VM;
import org.jikesrvm.adaptive.AosEntrypoints;
import org.jikesrvm.adaptive.recompilation.InvocationCounts;
import org.jikesrvm.architecture.MachineRegister;
import org.jikesrvm.classloader.Atom;
import org.jikesrvm.classloader.DynamicTypeCheck;
import org.jikesrvm.classloader.FieldReference;
import org.jikesrvm.classloader.InterfaceInvocation;
import org.jikesrvm.classloader.InterfaceMethodSignature;
import org.jikesrvm.classloader.MemberReference;
import org.jikesrvm.classloader.MethodReference;
import org.jikesrvm.classloader.NormalMethod;
import org.jikesrvm.classloader.RVMArray;
import org.jikesrvm.classloader.RVMClass;
import org.jikesrvm.classloader.RVMField;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.classloader.TypeReference;
import org.jikesrvm.compilers.baseline.BaselineCompiledMethod;
import org.jikesrvm.compilers.baseline.BaselineCompiler;
import org.jikesrvm.compilers.baseline.EdgeCounts;
import org.jikesrvm.compilers.common.CompiledMethod;
import org.jikesrvm.compilers.common.assembler.AbstractAssembler;
import org.jikesrvm.compilers.common.assembler.ForwardReference;
import org.jikesrvm.compilers.common.assembler.ppc.Assembler;
import org.jikesrvm.compilers.common.assembler.ppc.Lister;
import org.jikesrvm.jni.ppc.JNICompiler;
import org.jikesrvm.mm.mminterface.MemoryManager;
import org.jikesrvm.objectmodel.ObjectModel;
import org.jikesrvm.ppc.RegisterConstants.FPR;
import org.jikesrvm.ppc.RegisterConstants.GPR;
import org.jikesrvm.runtime.ArchEntrypoints;
import org.jikesrvm.runtime.Entrypoints;
import org.jikesrvm.runtime.MagicNames;
import org.jikesrvm.runtime.Memory;
import org.jikesrvm.runtime.Statics;
import org.jikesrvm.scheduler.RVMThread;
import org.vmmagic.pragma.Inline;
import org.vmmagic.pragma.Pure;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.Offset;

/**
 * Compiler is the baseline compiler class for powerPC architectures.
 * TODO improve JavaDoc comment
 */
public final class BaselineCompilerImpl extends BaselineCompiler {

  final Assembler asm;
  final Lister lister;

  // stackframe pseudo-constants //
  private int frameSize;
  private final int emptyStackOffset;
  private final int fullStackOffset;
  private final int startLocalOffset;
  private int spillOffset;

  /** Current offset of the sp from fp, also accessed by Assembler */
  public int spTopOffset;

  /**
   * If we're doing a short forward jump of less than this number of
   * bytecodes, then we can always use a short-form conditional branch
   * (don't have to emit a nop & bc).
   */
  private static final int SHORT_FORWARD_LIMIT = 500;

  private static final boolean USE_NONVOLATILE_REGISTERS = true;

  private byte firstFixedStackRegister; //after the fixed local registers !!
  private byte firstFloatStackRegister; //after the float local registers !!

  private byte lastFixedStackRegister;
  private byte lastFloatStackRegister;

  private final short[] localFixedLocations;
  private final short[] localFloatLocations;

  /** Can non-volatile registers be used? */
  private final boolean use_nonvolatile_registers;

  /**
   * Create a Compiler object for the compilation of method.
   */
  public BaselineCompilerImpl(BaselineCompiledMethod cm) {
    super(cm);
    localFixedLocations = new short[((NormalMethod)cm.getMethod()).getLocalWords()];
    localFloatLocations = new short[((NormalMethod)cm.getMethod()).getLocalWords()];
    use_nonvolatile_registers = USE_NONVOLATILE_REGISTERS && !method.hasBaselineNoRegistersAnnotation();

    if (VM.VerifyAssertions) VM._assert(T7.value() <= LAST_VOLATILE_GPR.value());           // need 8 gp temps
    if (VM.VerifyAssertions) VM._assert(F3.value() <= LAST_VOLATILE_FPR.value());           // need 4 fp temps
    if (VM.VerifyAssertions) VM._assert(S0.value() < S1.value() && S1.value() <= LAST_SCRATCH_GPR.value()); // need 2 scratch
    stackHeights = new int[bcodes.length()];
    startLocalOffset = getInternalStartLocalOffset(method);
    emptyStackOffset = getEmptyStackOffset(method);
    fullStackOffset = emptyStackOffset - (method.getOperandWords() << LOG_BYTES_IN_STACKSLOT);
    asm = new Assembler(bcodes.length(),shouldPrint, this, bytecodeMap);
    lister = asm.getLister();
  }

  @Override
  protected AbstractAssembler getAssembler() {
    return asm;
  }

  @Override
  protected Lister getLister() {
    return lister;
  }

  @Override
  protected void initializeCompiler() {
    defineStackAndLocalLocations(); //alters framesize, this can only be performed after localTypes are filled in by buildReferenceMaps

    frameSize = getInternalFrameSize(); //after defineStackAndLocalLocations !!
  }

  //----------------//
  // more interface //
  //----------------//

  /** position of operand stack within method's stackframe */
  @Uninterruptible
  static short getEmptyStackOffset(NormalMethod m) {
    int params = m.getOperandWords() << LOG_BYTES_IN_STACKSLOT; // maximum parameter area
    int spill = params - (MIN_PARAM_REGISTERS << LOG_BYTES_IN_STACKSLOT);
    if (spill < 0) spill = 0;
    int stack = m.getOperandWords() << LOG_BYTES_IN_STACKSLOT; // maximum stack size
    return (short)(STACKFRAME_HEADER_SIZE + spill + stack);
  }

  /** start position of locals within method's stackframe */
  @Uninterruptible
  private static int getInternalStartLocalOffset(NormalMethod m) {
    int locals = m.getLocalWords() << LOG_BYTES_IN_STACKSLOT;       // input param words + pure locals
    return getEmptyStackOffset(m) + locals; // bottom-most local
  }

  /** size of method's stackframe. */
  @Uninterruptible
  private int getInternalFrameSize() {
    int size = startLocalOffset;
    if (method.getDeclaringClass().hasDynamicBridgeAnnotation()) {
      size += (LAST_NONVOLATILE_FPR.value() - FIRST_VOLATILE_FPR.value() + 1) << LOG_BYTES_IN_DOUBLE;
      size += (LAST_NONVOLATILE_GPR.value() - FIRST_VOLATILE_GPR.value() + 1) << LOG_BYTES_IN_ADDRESS;
    } else {
      size += (lastFloatStackRegister - FIRST_FLOAT_LOCAL_REGISTER.value() + 1) << LOG_BYTES_IN_DOUBLE;
      size += (lastFixedStackRegister - FIRST_FIXED_LOCAL_REGISTER.value() + 1) << LOG_BYTES_IN_ADDRESS;
    }
    if (VM.BuildFor32Addr) {
      size = Memory.alignUp(size, STACKFRAME_ALIGNMENT);
    }
    return size;
  }

  @Uninterruptible
  static int getFrameSize(NormalMethod m, byte lastFloatStackRegister, byte lastFixedStackRegister) {
    int size = getInternalStartLocalOffset(m);
    if (m.getDeclaringClass().hasDynamicBridgeAnnotation()) {
      size += (LAST_NONVOLATILE_FPR.value() - FIRST_VOLATILE_FPR.value() + 1) << LOG_BYTES_IN_DOUBLE;
      size += (LAST_NONVOLATILE_GPR.value() - FIRST_VOLATILE_GPR.value() + 1) << LOG_BYTES_IN_ADDRESS;
    } else {
      size += (lastFloatStackRegister - FIRST_FLOAT_LOCAL_REGISTER.value() + 1) << LOG_BYTES_IN_DOUBLE;
      size += (lastFixedStackRegister - FIRST_FIXED_LOCAL_REGISTER.value() + 1) << LOG_BYTES_IN_ADDRESS;
    }
    if (VM.BuildFor32Addr) {
      size = Memory.alignUp(size, STACKFRAME_ALIGNMENT);
    }
    return size;
  }

  private void defineStackAndLocalLocations() {

    short nextFixedLocalRegister = FIRST_FIXED_LOCAL_REGISTER.value();
    short nextFloatLocalRegister = FIRST_FLOAT_LOCAL_REGISTER.value();

    //define local registers
    int nparam = method.getParameterWords();
    TypeReference[] types = method.getParameterTypes();
    int localIndex = 0;
    if (!method.isStatic()) {
      if (VM.VerifyAssertions) VM._assert(localTypes[0] == ADDRESS_TYPE);
      if (!use_nonvolatile_registers || (nextFixedLocalRegister > LAST_FIXED_LOCAL_REGISTER.value())) {
        localFixedLocations[localIndex] = offsetToLocation(localOffset(localIndex));
      } else {
        localFixedLocations[localIndex] = nextFixedLocalRegister++;
      }
      localIndex++;
      nparam++;
    }
    for (int i = 0; i < types.length; i++, localIndex++) {
      TypeReference t = types[i];
      if (t.isLongType()) {
        if (VM.BuildFor64Addr) {
          if (!use_nonvolatile_registers || (nextFixedLocalRegister > LAST_FIXED_LOCAL_REGISTER.value())) {
            localFixedLocations[localIndex] = offsetToLocation(localOffset(localIndex));
          } else {
            localFixedLocations[localIndex] = nextFixedLocalRegister++;
          }
        } else {
          if (!use_nonvolatile_registers || (nextFixedLocalRegister >= LAST_FIXED_LOCAL_REGISTER.value())) {
            localFixedLocations[localIndex] =
                offsetToLocation(localOffset(localIndex)); // lo mem := lo register (== hi word)
            //we don't fill in the second location !! Every access is through te location of the first half
          } else {
            localFixedLocations[localIndex] = nextFixedLocalRegister++;
            localFixedLocations[localIndex + 1] = nextFixedLocalRegister++;
          }
        }
        localIndex++;
      } else if (t.isFloatType()) {
        if (!use_nonvolatile_registers || (nextFloatLocalRegister > LAST_FLOAT_LOCAL_REGISTER.value())) {
          localFloatLocations[localIndex] = offsetToLocation(localOffset(localIndex));
        } else {
          localFloatLocations[localIndex] = nextFloatLocalRegister++;
        }
      } else if (t.isDoubleType()) {
        if (!use_nonvolatile_registers || (nextFloatLocalRegister > LAST_FLOAT_LOCAL_REGISTER.value())) {
          localFloatLocations[localIndex] = offsetToLocation(localOffset(localIndex));
        } else {
          localFloatLocations[localIndex] = nextFloatLocalRegister++;
        }
        localIndex++;
      } else if (t.isIntLikeType()) {
        if (!use_nonvolatile_registers || (nextFixedLocalRegister > LAST_FIXED_LOCAL_REGISTER.value())) {
          localFixedLocations[localIndex] = offsetToLocation(localOffset(localIndex));
        } else {
          localFixedLocations[localIndex] = nextFixedLocalRegister++;
        }
      } else { // t is object
        if (!use_nonvolatile_registers || (nextFixedLocalRegister > LAST_FIXED_LOCAL_REGISTER.value())) {
          localFixedLocations[localIndex] = offsetToLocation(localOffset(localIndex));
        } else {
          localFixedLocations[localIndex] = nextFixedLocalRegister++;
        }
      }
    }

    if (VM.VerifyAssertions) VM._assert(localIndex == nparam);
    //rest of locals, non parameters, could be reused for different types
    int nLocalWords = method.getLocalWords();
    for (; localIndex < nLocalWords; localIndex++) {
      byte currentLocal = localTypes[localIndex];

      if (needsFloatRegister(currentLocal)) { //float or double
        if (!use_nonvolatile_registers || (nextFloatLocalRegister > LAST_FLOAT_LOCAL_REGISTER.value())) {
          localFloatLocations[localIndex] = offsetToLocation(localOffset(localIndex));
        } else {
          localFloatLocations[localIndex] = nextFloatLocalRegister++;
        }
      }

      currentLocal = stripFloatRegisters(currentLocal);
      if (currentLocal != VOID_TYPE) { //object or intlike
        if (VM.BuildFor32Addr && containsLongType(currentLocal)) { //long
          if (!use_nonvolatile_registers || (nextFixedLocalRegister >= LAST_FIXED_LOCAL_REGISTER.value())) {
            localFixedLocations[localIndex] = offsetToLocation(localOffset(localIndex));
            //two longs next to each other, overlapping one location, last long can't be stored in registers anymore :
            if (use_nonvolatile_registers &&
                (nextFixedLocalRegister == LAST_FIXED_LOCAL_REGISTER.value()) &&
                containsLongType(localTypes[localIndex - 1])) {
              nextFixedLocalRegister++; //if only 1 reg left, but already reserved by previous long, count it here !!
            }
          } else {
            localFixedLocations[localIndex] = nextFixedLocalRegister++;
          }
          localTypes[localIndex + 1] |=
              INT_TYPE; //there is at least one more, since this is long; mark so that we certainly assign a location to the second half
        } else if (!use_nonvolatile_registers || (nextFixedLocalRegister > LAST_FIXED_LOCAL_REGISTER.value())) {
          localFixedLocations[localIndex] = offsetToLocation(localOffset(localIndex));
        } else {
          localFixedLocations[localIndex] = nextFixedLocalRegister++;
        }
      }
      //else unused, assign nothing, can be the case after long or double
    }

    firstFixedStackRegister = (byte)nextFixedLocalRegister;
    firstFloatStackRegister = (byte)nextFloatLocalRegister;

    //define stack registers
    //KV: TODO
    lastFixedStackRegister = (byte)(firstFixedStackRegister - 1);
    lastFloatStackRegister = (byte)(firstFloatStackRegister - 1);

    if (USE_NONVOLATILE_REGISTERS && method.hasBaselineSaveLSRegistersAnnotation()) {
      //methods with SaveLSRegisters pragma need to save/restore ALL registers in their prolog/epilog
      lastFixedStackRegister = LAST_FIXED_STACK_REGISTER.value();
      lastFloatStackRegister = LAST_FLOAT_STACK_REGISTER.value();
    }
  }

  @Uninterruptible
  byte getLastFixedStackRegister() {
    return lastFixedStackRegister;
  }

  @Uninterruptible
  byte getLastFloatStackRegister() {
    return lastFloatStackRegister;
  }

  private static boolean needsFloatRegister(byte type) {
    return 0 != (type & (FLOAT_TYPE | DOUBLE_TYPE));
  }

  private static byte stripFloatRegisters(byte type) {
    return (byte) (type & (~(FLOAT_TYPE | DOUBLE_TYPE)));
  }

  private static boolean containsLongType(byte type) {
    return 0 != (type & (LONG_TYPE));
  }

  @Uninterruptible
  private short getGeneralLocalLocation(int index) {
    return localFixedLocations[index];
  }

  @Uninterruptible
  private short getFloatLocalLocation(int index) {
    return localFloatLocations[index];
  }

  @Uninterruptible
  short [] getLocalFixedLocations() {
    return localFixedLocations;
  }

  @Uninterruptible
  short [] getLocalFloatLocations() {
    return localFloatLocations;
  }

  /*
   * implementation of abstract methods of BaselineCompiler
   */

  /*
   * Misc routines not directly tied to a particular bytecode
   */

  private short getSingleStackLocation(int index) {
    return offsetToLocation(spTopOffset + BYTES_IN_STACKSLOT + (index << LOG_BYTES_IN_STACKSLOT));
  }

  private short getDoubleStackLocation(int index) {
    return offsetToLocation(spTopOffset + 2 * BYTES_IN_STACKSLOT + (index << LOG_BYTES_IN_STACKSLOT));
  }

  private short getTopOfStackLocationForPush() {
    return offsetToLocation(spTopOffset);
  }

  /**
   * About to start generating code for bytecode biStart.
   * Perform any platform specific setup
   */
  @Override
  protected void starting_bytecode() {
    spTopOffset = startLocalOffset - BYTES_IN_STACKSLOT - (stackHeights[biStart] * BYTES_IN_STACKSLOT);
  }

  @Override
  protected void emit_prologue() {
    spTopOffset = emptyStackOffset;
    genPrologue();
  }

  @Override
  protected void emit_threadSwitchTest(int whereFrom) {
    genThreadSwitchTest(whereFrom);
  }

  @Override
  protected boolean emit_Magic(MethodReference magicMethod) {
    return generateInlineCode(magicMethod);
  }

  /*
   * Helper functions for expression stack manipulation
   */
  /**
   * Validate that that pushing bytesActuallyWritten
   * onto the expression stack won't cause anything to
   * be written outside of the portion of the physical stackframe
   * that is reserved for use by the Java expression stack.
   * This method should be called before spTopOffset is decremented by
   * a push operation, passing the actual size of the data to be
   * pushed on the stack (which on 64bit, is not the same as the change to spTopOffset).
   */
  private void validateStackPush(int bytesActuallyWritten) {
    if (VM.VerifyAssertions) {
      boolean pushDoesNotOverwrite = (spTopOffset - bytesActuallyWritten) >= fullStackOffset;
      if (!pushDoesNotOverwrite) {
        String msg = " spTopOffset=" + spTopOffset + ", empty=" + emptyStackOffset +
            ", full=" + fullStackOffset + ", bw=" + bytesActuallyWritten;
        VM._assert(VM.NOT_REACHED, msg);
      }
    }
  }

  private void discardSlot() {
    spTopOffset += BYTES_IN_STACKSLOT;
  }

  protected void discardSlots(int n) {
    spTopOffset += n * BYTES_IN_STACKSLOT;
  }

  /**
   * Emit the code to push an intlike (boolean, byte, char, short, int) value
   * contained in 'reg' onto the expression stack
   * @param reg register containing the value to push
   */
  private void pushInt(GPR reg) {
    if (VM.VerifyAssertions) validateStackPush(BYTES_IN_INT);
    asm.emitSTW(reg, spTopOffset - BYTES_IN_INT, FP);
    spTopOffset -= BYTES_IN_STACKSLOT;
  }

  /**
   * Emit the code to push a float value
   * contained in 'reg' onto the expression stack
   * @param reg register containing the value to push
   */
  private void pushFloat(FPR reg) {
    if (VM.VerifyAssertions) validateStackPush(BYTES_IN_FLOAT);
    asm.emitSTFS(reg, spTopOffset - BYTES_IN_FLOAT, FP);
    spTopOffset -= BYTES_IN_STACKSLOT;
  }

  /**
   * Emit the code to push a double value
   * contained in 'reg' onto the expression stack
   * @param reg register containing the value to push
   */
  private void pushDouble(FPR reg) {
    if (VM.VerifyAssertions) validateStackPush(BYTES_IN_DOUBLE);
    asm.emitSTFD(reg, spTopOffset - BYTES_IN_DOUBLE, FP);
    spTopOffset -= 2 * BYTES_IN_STACKSLOT;
  }

  /**
   * Emit the code to push a double value
   * contained in 'reg' onto the expression stack
   * @param reg register containing the value to push
   */
  private void pushLowDoubleAsInt(FPR reg) {
    if (VM.VerifyAssertions) validateStackPush(BYTES_IN_DOUBLE);
    asm.emitSTFD(reg, spTopOffset - BYTES_IN_DOUBLE, FP);
    spTopOffset -= BYTES_IN_STACKSLOT;
  }

  /**
   * Emit the code to push a long value
   * contained in 'reg1' and 'reg2' onto the expression stack
   * @param reg1 register containing,  the most significant 32 bits to push on 32bit arch (to lowest address), not used on 64bit
   * @param reg2 register containing,  the least significant 32 bits on 32bit arch (to highest address), the whole value on 64bit
   */
  private void pushLong(GPR reg1, GPR reg2) {
    if (VM.VerifyAssertions) validateStackPush(BYTES_IN_LONG);
    if (VM.BuildFor64Addr) {
      asm.emitSTD(reg2, spTopOffset - BYTES_IN_LONG, FP);
    } else {
      asm.emitSTW(reg2, spTopOffset - BYTES_IN_STACKSLOT, FP);
      asm.emitSTW(reg1, spTopOffset - 2 * BYTES_IN_STACKSLOT, FP);
    }
    spTopOffset -= 2 * BYTES_IN_STACKSLOT;
  }

  /**
   * Emit the code to push a long value
   * contained in 'reg' onto the expression stack.<p>
   *
   * NOTE: in 32 bit mode, reg is actually a FP register and
   * we are treating the long value as if it were an FP value to do this in
   * one instruction!!!
   * @param reg register containing the value to push
   */
  private void pushLongAsDouble(FPR reg) {
    if (VM.VerifyAssertions) validateStackPush(BYTES_IN_LONG);
    asm.emitSTFD(reg, spTopOffset - BYTES_IN_LONG, FP);
    spTopOffset -= 2 * BYTES_IN_STACKSLOT;
  }

  /**
   * Emit the code to push a reference/address value
   * contained in 'reg' onto the expression stack
   * @param reg register containing the value to push
   */
  private void pushAddr(GPR reg) {
    if (VM.VerifyAssertions) validateStackPush(BYTES_IN_ADDRESS);
    asm.emitSTAddr(reg, spTopOffset - BYTES_IN_ADDRESS, FP);
    spTopOffset -= BYTES_IN_STACKSLOT;
  }

  /**
   * Emit the code to poke an address
   * contained in 'reg' onto the expression stack on position idx.
   * @param reg register to peek the value into
   */
  private void pokeAddr(GPR reg, int idx) {
    int offset = BYTES_IN_STACKSLOT - BYTES_IN_ADDRESS + (idx << LOG_BYTES_IN_STACKSLOT);
    if (VM.VerifyAssertions) validateStackPush(-offset);
    asm.emitSTAddr(reg, spTopOffset + offset, FP);
  }

  /**
   * Emit the code to poke an int
   * contained in 'reg' onto the expression stack on position idx.
   * @param reg register to peek the value into
   */
  private void pokeInt(GPR reg, int idx) {
    int offset = BYTES_IN_STACKSLOT - BYTES_IN_INT + (idx << LOG_BYTES_IN_STACKSLOT);
    if (VM.VerifyAssertions) validateStackPush(-offset);
    asm.emitSTW(reg, spTopOffset + offset, FP);
  }

  /**
   * Emit the code to pop a char value from the expression stack into
   * the register 'reg' as an int.
   * @param reg register to pop the value into
   */
  private void popCharAsInt(GPR reg) {
    asm.emitLHZ(reg, spTopOffset + BYTES_IN_STACKSLOT - BYTES_IN_CHAR, FP);
    discardSlot();
  }

  /**
   * Emit the code to pop a short value from the expression stack into
   * the register 'reg' as an int.
   * @param reg register to pop the value into
   */
  private void popShortAsInt(GPR reg) {
    asm.emitLHA(reg, spTopOffset + BYTES_IN_STACKSLOT - BYTES_IN_SHORT, FP);
    discardSlot();
  }

  /**
   * Emit the code to pop a byte value from the expression stack into
   * the register 'reg' as an int.
   * @param reg register to pop the value into
   */
  private void popByteAsInt(GPR reg) {
    asm.emitLWZ(reg, spTopOffset + BYTES_IN_STACKSLOT - BYTES_IN_INT, FP);
    asm.emitEXTSB(reg, reg);
    discardSlot();
  }

  /**
   * Emit the code to pop an intlike (boolean, byte, char, short, int) value
   * from the expression stack into the register 'reg'. Sign extend on 64 bit platform.
   * @param reg register to pop the value into
   */
  private void popInt(GPR reg) {
    asm.emitLInt(reg, spTopOffset + BYTES_IN_STACKSLOT - BYTES_IN_INT, FP);
    discardSlot();
  }

  /**
   * Emit the code to pop a float value
   * from the expression stack into the register 'reg'.
   * @param reg register to pop the value into
   */
  private void popFloat(FPR reg) {
    asm.emitLFS(reg, spTopOffset + BYTES_IN_STACKSLOT - BYTES_IN_FLOAT, FP);
    discardSlot();
  }

  /**
   * Emit the code to pop a double value
   * from the expression stack into the register 'reg'.
   * @param reg register to pop the value into
   */
  private void popDouble(FPR reg) {
    asm.emitLFD(reg, spTopOffset + 2 * BYTES_IN_STACKSLOT - BYTES_IN_DOUBLE, FP);
    discardSlots(2);
  }

  /**
   * Emit the code to push a long value
   * contained in 'reg1' and 'reg2' onto the expression stack
   * @param reg1 register to pop,  the most significant 32 bits on 32bit arch (lowest address), not used on 64bit
   * @param reg2 register to pop,  the least significant 32 bits on 32bit arch (highest address), the whole value on 64bit
   */
  private void popLong(GPR reg1, GPR reg2) {
    if (VM.BuildFor64Addr) {
      asm.emitLD(reg2, spTopOffset + 2 * BYTES_IN_STACKSLOT - BYTES_IN_LONG, FP);
    } else {
      asm.emitLWZ(reg1, spTopOffset, FP);
      asm.emitLWZ(reg2, spTopOffset + BYTES_IN_STACKSLOT, FP);
    }
    discardSlots(2);
  }

  /**
   * Emit the code to pop a long value
   * from the expression stack into the register 'reg'.<p>
   *
   * NOTE: in 32 bit mode, reg is actually a FP register and
   * we are treating the long value as if it were an FP value to do this in
   * one instruction!!!
   * @param reg register to pop the value into
   */
  private void popLongAsDouble(FPR reg) {
    asm.emitLFD(reg, spTopOffset + 2 * BYTES_IN_STACKSLOT - BYTES_IN_DOUBLE, FP);
    discardSlots(2);
  }

  /**
   * Emit the code to pop a reference/address value
   * from the expression stack into the register 'reg'.
   * @param reg register to pop the value into
   */
  private void popAddr(GPR reg) {
    asm.emitLAddr(reg, spTopOffset + BYTES_IN_STACKSLOT - BYTES_IN_ADDRESS, FP);
    discardSlot();
  }

  /**
   * Emits the code to pop an Offset (a value of the unboxed type Offset)
   * from the expression stack into the register 'reg'.
   * @param reg register to pop the value into
   */
  private void popOffset(GPR reg) {
    asm.emitLAddr(reg, spTopOffset + BYTES_IN_STACKSLOT - BYTES_IN_OFFSET, FP);
    discardSlot();
  }

  /**
   * Emit the code to peek an intlike (boolean, byte, char, short, int) value
   * from the expression stack into the register 'reg'.
   * @param reg register to peek the value into
   */
  void peekInt(GPR reg, int idx) {
    asm.emitLInt(reg, spTopOffset + BYTES_IN_STACKSLOT - BYTES_IN_INT + (idx << LOG_BYTES_IN_STACKSLOT), FP);
  }

  /**
   * Emit the code to peek a float value
   * from the expression stack into the register 'reg'.
   * @param reg register to peek the value into
   */
  protected void peekFloat(FPR reg, int idx) {
    asm.emitLFS(reg, spTopOffset + BYTES_IN_STACKSLOT - BYTES_IN_FLOAT + (idx << LOG_BYTES_IN_STACKSLOT), FP);
  }

  /**
   * Emit the code to peek a double value
   * from the expression stack into the register 'reg'.
   * @param reg register to peek the value into
   */
  protected void peekDouble(FPR reg, int idx) {
    asm.emitLFD(reg, spTopOffset + 2 * BYTES_IN_STACKSLOT - BYTES_IN_DOUBLE + (idx << LOG_BYTES_IN_STACKSLOT), FP);
  }

  /**
   * Emit the code to peek a long value
   * from the expression stack into 'reg1' and 'reg2'.
   * @param reg1 register to peek,  the most significant 32 bits on 32bit arch (lowest address), not used on 64bit
   * @param reg2 register to peek,  the least significant 32 bits on 32bit arch (highest address), the whole value on 64bit
   */
  protected void peekLong(GPR reg1, GPR reg2, int idx) {
    if (VM.BuildFor64Addr) {
      asm.emitLD(reg2, spTopOffset + 2 * BYTES_IN_STACKSLOT - BYTES_IN_LONG + (idx << LOG_BYTES_IN_STACKSLOT), FP);
    } else {
      asm.emitLWZ(reg1, spTopOffset + (idx << LOG_BYTES_IN_STACKSLOT), FP);
      asm.emitLWZ(reg2, spTopOffset + BYTES_IN_STACKSLOT + (idx << LOG_BYTES_IN_STACKSLOT), FP);
    }
  }

  /**
   * Emit the code to peek a reference/address value
   * from the expression stack into the register 'reg'.
   * @param reg register to peek the value into
   */
  public void peekAddr(GPR reg, int idx) {
    asm.emitLAddr(reg, spTopOffset + BYTES_IN_STACKSLOT - BYTES_IN_ADDRESS + (idx << LOG_BYTES_IN_STACKSLOT), FP);
  }

  /**
   * Emits the code to peek an unboxed (i.e. pointer-sized) value
   * from the expression stack into the register 'reg'.
   * @param reg register to peek the value into
   */
  public void peekUnboxed(GPR reg, int idx) {
    asm.emitLAddr(reg, spTopOffset + BYTES_IN_STACKSLOT - BYTES_IN_ADDRESS + (idx << LOG_BYTES_IN_STACKSLOT), FP);
  }

  /*
  * Loading constants
  */

  @Override
  protected void emit_aconst_null() {
    asm.emitLVAL(T0, 0);
    pushAddr(T0);
  }

  @Override
  protected void emit_iconst(int val) {
    asm.emitLVAL(T0, val);
    pushInt(T0);
  }

  @Override
  protected void emit_lconst(int val) {
    if (val == 0) {
      asm.emitLFStoc(F0, Entrypoints.zeroFloatField.getOffset(), T0);
    } else {
      if (VM.VerifyAssertions) VM._assert(val == 1);
      asm.emitLFDtoc(F0, Entrypoints.longOneField.getOffset(), T0);
    }
    pushLongAsDouble(F0);
  }

  @Override
  protected void emit_fconst_0() {
    asm.emitLFStoc(F0, Entrypoints.zeroFloatField.getOffset(), T0);
    pushFloat(F0);
  }

  @Override
  protected void emit_fconst_1() {
    asm.emitLFStoc(F0, Entrypoints.oneFloatField.getOffset(), T0);
    pushFloat(F0);
  }

  @Override
  protected void emit_fconst_2() {
    asm.emitLFStoc(F0, Entrypoints.twoFloatField.getOffset(), T0);
    pushFloat(F0);
  }

  @Override
  protected void emit_dconst_0() {
    asm.emitLFStoc(F0, Entrypoints.zeroFloatField.getOffset(), T0);
    pushDouble(F0);
  }

  @Override
  protected void emit_dconst_1() {
    asm.emitLFStoc(F0, Entrypoints.oneFloatField.getOffset(), T0);
    pushDouble(F0);
  }

  @Override
  protected void emit_ldc(Offset offset, byte type) {
    if (Statics.isReference(Statics.offsetAsSlot(offset))) {
      asm.emitLAddrToc(T0, offset);
      pushAddr(T0);
    } else {
      asm.emitLIntToc(T0, offset);
      pushInt(T0);
    }
  }

  @Override
  protected void emit_ldc2(Offset offset, byte type) {
    asm.emitLFDtoc(F0, offset, T0);
    pushDouble(F0);
  }

  /*
  * loading local variables
  */

  @Override
  protected void emit_regular_iload(int index) {
    short dstLoc = getTopOfStackLocationForPush();
    copyByLocation(INT_TYPE, getGeneralLocalLocation(index), INT_TYPE, dstLoc);
    if (VM.VerifyAssertions) validateStackPush(BYTES_IN_INT);
    spTopOffset -= BYTES_IN_STACKSLOT;
  }

  @Override
  protected void emit_lload(int index) {
    short dstLoc = getTopOfStackLocationForPush();
    copyByLocation(LONG_TYPE, getGeneralLocalLocation(index), LONG_TYPE, dstLoc);
    if (VM.VerifyAssertions) validateStackPush(BYTES_IN_LONG);
    spTopOffset -= 2 * BYTES_IN_STACKSLOT;
  }

  @Override
  protected void emit_fload(int index) {
    short dstLoc = getTopOfStackLocationForPush();
    copyByLocation(FLOAT_TYPE, getFloatLocalLocation(index), FLOAT_TYPE, dstLoc);
    if (VM.VerifyAssertions) validateStackPush(BYTES_IN_FLOAT);
    spTopOffset -= BYTES_IN_STACKSLOT;
  }

  @Override
  protected void emit_dload(int index) {
    short dstLoc = getTopOfStackLocationForPush();
    copyByLocation(DOUBLE_TYPE, getFloatLocalLocation(index), DOUBLE_TYPE, dstLoc);
    if (VM.VerifyAssertions) validateStackPush(BYTES_IN_DOUBLE);
    spTopOffset -= 2 * BYTES_IN_STACKSLOT;
  }

  @Override
  protected void emit_regular_aload(int index) {
    short dstLoc = getTopOfStackLocationForPush();
    copyByLocation(ADDRESS_TYPE, getGeneralLocalLocation(index), ADDRESS_TYPE, dstLoc);
    if (VM.VerifyAssertions) validateStackPush(BYTES_IN_ADDRESS);
    spTopOffset -= BYTES_IN_STACKSLOT;
  }

  /*
   * storing local variables
   */

  @Override
  protected void emit_istore(int index) {
    short srcLoc = getSingleStackLocation(0);
    copyByLocation(INT_TYPE, srcLoc, INT_TYPE, getGeneralLocalLocation(index));
    discardSlot();
  }

  @Override
  protected void emit_lstore(int index) {
    copyByLocation(LONG_TYPE, getDoubleStackLocation(0), LONG_TYPE, getGeneralLocalLocation(index));
    discardSlots(2);
  }

  @Override
  protected void emit_fstore(int index) {
    short srcLoc = getSingleStackLocation(0);
    copyByLocation(FLOAT_TYPE, srcLoc, FLOAT_TYPE, getFloatLocalLocation(index));
    discardSlot();
  }

  @Override
  protected void emit_dstore(int index) {
    short srcLoc = getDoubleStackLocation(0);
    copyByLocation(DOUBLE_TYPE, srcLoc, DOUBLE_TYPE, getFloatLocalLocation(index));
    discardSlots(2);
  }

  @Override
  protected void emit_astore(int index) {
    short srcLoc = getSingleStackLocation(0);
    copyByLocation(ADDRESS_TYPE, srcLoc, ADDRESS_TYPE, getGeneralLocalLocation(index));
    discardSlot();
  }

  /*
  * array loads
  */

  @Override
  protected void emit_iaload() {
    genBoundsCheck();
    asm.emitSLWI(T1, T1, LOG_BYTES_IN_INT);  // convert index to offset
    asm.emitLIntX(T2, T0, T1);  // load desired int array element
    pushInt(T2);
  }

  @Override
  protected void emit_laload() {
    genBoundsCheck();
    asm.emitSLWI(T1, T1, LOG_BYTES_IN_LONG);  // convert index to offset
    asm.emitLFDX(F0, T0, T1);  // load desired (long) array element
    pushLongAsDouble(F0);
  }

  @Override
  protected void emit_faload() {
    genBoundsCheck();
    asm.emitSLWI(T1, T1, LOG_BYTES_IN_FLOAT);  // convert index to offset
    asm.emitLWZX(T2, T0, T1);  // load desired (float) array element
    pushInt(T2);  //LFSX not implemented yet
//    asm.emitLFSX  (F0, T0, T1);  // load desired (float) array element
//    pushFloat(F0);
  }

  @Override
  protected void emit_daload() {
    genBoundsCheck();
    asm.emitSLWI(T1, T1, LOG_BYTES_IN_DOUBLE);  // convert index to offset
    asm.emitLFDX(F0, T0, T1);  // load desired (double) array element
    pushDouble(F0);
  }

  @Override
  protected void emit_aaload() {
    genBoundsCheck();
    if (NEEDS_OBJECT_ALOAD_BARRIER) {
      Barriers.compileArrayLoadBarrier(this);
      pushAddr(T0);
    } else {
      asm.emitSLWI(T1, T1, LOG_BYTES_IN_ADDRESS);  // convert index to offset
      asm.emitLAddrX(T2, T0, T1);  // load desired (ref) array element
      pushAddr(T2);
    }
  }

  @Override
  protected void emit_baload() {
    genBoundsCheck();
    asm.emitLBZX(T2, T0, T1);  // no load byte algebraic ...
    asm.emitEXTSB(T2, T2);
    pushInt(T2);
  }

  @Override
  protected void emit_caload() {
    genBoundsCheck();
    asm.emitSLWI(T1, T1, LOG_BYTES_IN_CHAR);  // convert index to offset
    asm.emitLHZX(T2, T0, T1);  // load desired (char) array element
    pushInt(T2);
  }

  @Override
  protected void emit_saload() {
    genBoundsCheck();
    asm.emitSLWI(T1, T1, LOG_BYTES_IN_SHORT);  // convert index to offset
    asm.emitLHAX(T2, T0, T1);  // load desired (short) array element
    pushInt(T2);
  }

  /*
   * array stores
   */

  @Override
  protected void emit_iastore() {
    if (NEEDS_INT_ASTORE_BARRIER) {
      genBoundsCheck(1, 2); // skip int value on stack and do bounds check
      Barriers.compileArrayStoreBarrierInt(this);
    } else {
      popInt(T2); // T2 is value to store
      genBoundsCheck();
      asm.emitSLWI(T1, T1, LOG_BYTES_IN_INT); // convert index to offset
      asm.emitSTWX(T2, T0, T1); // store int value in array
    }
  }

  @Override
  protected void emit_lastore() {
    if (NEEDS_LONG_ASTORE_BARRIER) {
      genBoundsCheck(2, 3); // skip long value on stack and do bounds check
      Barriers.compileArrayStoreBarrierLong(this);
    } else {
      popLongAsDouble(F0);                    // F0 is value to store
      genBoundsCheck();
      asm.emitSLWI(T1, T1, LOG_BYTES_IN_LONG); // convert index to offset
      asm.emitSTFDX(F0, T0, T1); // store long value in array
    }
  }

  @Override
  protected void emit_fastore() {
    if (NEEDS_FLOAT_ASTORE_BARRIER) {
      genBoundsCheck(1, 2); // skip float value on stack and do bounds check
      Barriers.compileArrayStoreBarrierFloat(this);
    } else {
      popInt(T2);      // T2 is value to store
      genBoundsCheck();
      asm.emitSLWI(T1, T1, LOG_BYTES_IN_FLOAT); // convert index to offset
      asm.emitSTWX(T2, T0, T1); // store float value in array
    }
  }

  @Override
  protected void emit_dastore() {
    if (NEEDS_DOUBLE_ASTORE_BARRIER) {
      genBoundsCheck(2, 3); // skip double value on stack and do bounds check
      Barriers.compileArrayStoreBarrierDouble(this);
    } else {
      popDouble(F0);         // F0 is value to store
      genBoundsCheck();
      asm.emitSLWI(T1, T1, LOG_BYTES_IN_DOUBLE); // convert index to offset
      asm.emitSTFDX(F0, T0, T1); // store double value in array
    }
  }

  @Override
  protected void emit_aastore() {
    if (doesCheckStore) {
      emit_resolved_invokestatic((MethodReference)Entrypoints.aastoreMethod.getMemberRef());
    } else {
      emit_resolved_invokestatic((MethodReference)Entrypoints.aastoreUninterruptibleMethod.getMemberRef());
    }
  }

  @Override
  protected void emit_bastore() {
    if (NEEDS_BYTE_ASTORE_BARRIER) {
      genBoundsCheck(1, 2); // skip byte value on stack and do bounds check
      Barriers.compileArrayStoreBarrierByte(this);
    } else {
      popInt(T2);      // T2 is value to store
      genBoundsCheck();
      asm.emitSTBX(T2, T0, T1); // store byte value in array
    }
  }

  @Override
  protected void emit_castore() {
    if (NEEDS_CHAR_ASTORE_BARRIER) {
      genBoundsCheck(1, 2); // skip char value on stack and do bounds check
      Barriers.compileArrayStoreBarrierChar(this);
    } else {
      popInt(T2);      // T2 is value to store
      genBoundsCheck();
      asm.emitSLWI(T1, T1, LOG_BYTES_IN_CHAR); // convert index to offset
      asm.emitSTHX(T2, T0, T1); // store char value in array
    }
  }

  @Override
  protected void emit_sastore() {
    if (NEEDS_SHORT_ASTORE_BARRIER) {
      genBoundsCheck(1, 2); // skip short value on stack and do bounds check
      Barriers.compileArrayStoreBarrierShort(this);
    } else {
      popInt(T2);      // T2 is value to store
      genBoundsCheck();
      asm.emitSLWI(T1, T1, LOG_BYTES_IN_SHORT); // convert index to offset
      asm.emitSTHX(T2, T0, T1); // store short value in array
    }
  }

  /*
  * expression stack manipulation
  */

  @Override
  protected void emit_pop() {
    discardSlot();
  }

  @Override
  protected void emit_pop2() {
    discardSlots(2);
  }

  @Override
  protected void emit_dup() {
    peekAddr(T0, 0);
    pushAddr(T0);
  }

  @Override
  protected void emit_dup_x1() {
    popAddr(T0);
    popAddr(T1);
    pushAddr(T0);
    pushAddr(T1);
    pushAddr(T0);
  }

  @Override
  protected void emit_dup_x2() {
    popAddr(T0);
    popAddr(T1);
    popAddr(T2);
    pushAddr(T0);
    pushAddr(T2);
    pushAddr(T1);
    pushAddr(T0);
  }

  @Override
  protected void emit_dup2() {
    peekAddr(T0, 0);
    peekAddr(T1, 1);
    pushAddr(T1);
    pushAddr(T0);
  }

  @Override
  protected void emit_dup2_x1() {
    popAddr(T0);
    popAddr(T1);
    popAddr(T2);
    pushAddr(T1);
    pushAddr(T0);
    pushAddr(T2);
    pushAddr(T1);
    pushAddr(T0);
  }

  @Override
  protected void emit_dup2_x2() {
    popAddr(T0);
    popAddr(T1);
    popAddr(T2);
    popAddr(T3);
    pushAddr(T1);
    pushAddr(T0);
    pushAddr(T3);
    pushAddr(T2);
    pushAddr(T1);
    pushAddr(T0);
  }

  @Override
  protected void emit_swap() {
    popAddr(T0);
    popAddr(T1);
    pushAddr(T0);
    pushAddr(T1);
  }

  /*
  * int ALU
  */

  @Override
  protected void emit_iadd() {
    popInt(T0);
    popInt(T1);
    asm.emitADD(T2, T1, T0);
    pushInt(T2);
  }

  @Override
  protected void emit_isub() {
    popInt(T0);
    popInt(T1);
    asm.emitSUBFC(T2, T0, T1);
    pushInt(T2);
  }

  @Override
  protected void emit_imul() {
    popInt(T1);
    popInt(T0);
    asm.emitMULLW(T1, T0, T1);
    pushInt(T1);
  }

  @Override
  protected void emit_idiv() {
    popInt(T1);
    popInt(T0);
    asm.emitTWEQ0(T1);
    asm.emitDIVW(T0, T0, T1);  // T0 := T0/T1
    pushInt(T0);
  }

  @Override
  protected void emit_irem() {
    popInt(T1);
    popInt(T0);
    asm.emitTWEQ0(T1);
    asm.emitDIVW(T2, T0, T1);   // T2 := T0/T1
    asm.emitMULLW(T2, T2, T1);   // T2 := [T0/T1]*T1
    asm.emitSUBFC(T1, T2, T0);   // T1 := T0 - [T0/T1]*T1
    pushInt(T1);
  }

  @Override
  protected void emit_ineg() {
    popInt(T0);
    asm.emitNEG(T0, T0);
    pushInt(T0);
  }

  @Override
  protected void emit_ishl() {
    popInt(T1);
    popInt(T0);
    asm.emitANDI(T1, T1, 0x1F);
    asm.emitSLW(T0, T0, T1);
    pushInt(T0);
  }

  @Override
  protected void emit_ishr() {
    popInt(T1);
    popInt(T0);
    asm.emitANDI(T1, T1, 0x1F);
    asm.emitSRAW(T0, T0, T1);
    pushInt(T0);
  }

  @Override
  protected void emit_iushr() {
    popInt(T1);
    popInt(T0);
    asm.emitANDI(T1, T1, 0x1F);
    asm.emitSRW(T0, T0, T1);
    pushInt(T0);
  }

  @Override
  protected void emit_iand() {
    popInt(T1);
    popInt(T0);
    asm.emitAND(T2, T0, T1);
    pushInt(T2);
  }

  @Override
  protected void emit_ior() {
    popInt(T1);
    popInt(T0);
    asm.emitOR(T2, T0, T1);
    pushInt(T2);
  }

  @Override
  protected void emit_ixor() {
    popInt(T1);
    popInt(T0);
    asm.emitXOR(T2, T0, T1);
    pushInt(T2);
  }

  @Override
  protected void emit_iinc(int index, int val) {
    short loc = getGeneralLocalLocation(index);
    if (isRegister(loc)) {
      asm.emitADDI(GPR.lookup(loc), val, GPR.lookup(loc));
    } else {
      copyMemToReg(INT_TYPE, locationToOffset(loc), T0);
      asm.emitADDI(T0, val, T0);
      copyRegToMem(INT_TYPE, T0, locationToOffset(loc));
    }
  }

  /*
  * long ALU
  */

  @Override
  protected void emit_ladd() {
    popLong(T2, T0);
    popLong(T3, T1);
    asm.emitADD(T0, T1, T0);
    if (VM.BuildFor32Addr) {
      asm.emitADDE(T1, T2, T3);
    }
    pushLong(T1, T0);
  }

  @Override
  protected void emit_lsub() {
    popLong(T2, T0);
    popLong(T3, T1);
    asm.emitSUBFC(T0, T0, T1);
    if (VM.BuildFor32Addr) {
      asm.emitSUBFE(T1, T2, T3);
    }
    pushLong(T1, T0);
  }

  @Override
  protected void emit_lmul() {
    popLong(T2, T3);
    popLong(T0, T1);
    if (VM.BuildFor64Addr) {
      asm.emitMULLD(T1, T1, T3);
    } else {
      asm.emitMULHWU(S0, T1, T3);
      asm.emitMULLW(T0, T0, T3);
      asm.emitADD(T0, T0, S0);
      asm.emitMULLW(S0, T1, T2);
      asm.emitMULLW(T1, T1, T3);
      asm.emitADD(T0, T0, S0);
    }
    pushLong(T0, T1);
  }

  @Override
  protected void emit_ldiv() {
    popLong(T2, T3);
    if (VM.BuildFor64Addr) {
      popLong(T0, T1);
      asm.emitTDEQ0(T3);
      asm.emitDIVD(T1, T1, T3);
    } else {
      asm.emitOR(T0, T3, T2); // or two halves of denominator together
      asm.emitTWEQ0(T0);         // trap if 0.
      popLong(T0, T1);
      generateSysCall(16, Entrypoints.sysLongDivideIPField);
//    If we had an implementation of Magic.unsignedRemainder and Magic.unsignedDiv we could write the else block as:
//      asm.emitOR(T0, T3, T2); // or two halves of denominator together
//      asm.emitTWEQ0(T0);         // trap if 0.
//      Offset methodOffset = Entrypoints.ldivMethod.getOffset();
//      asm.emitLAddrToc(T0, methodOffset);
//      asm.emitMTCTR(T0);
//      popLong(T0, T1);
//      asm.emitBCCTRL();
    }
    pushLong(T0, T1);
  }

  @Override
  protected void emit_lrem() {
    popLong(T2, T3);
    if (VM.BuildFor64Addr) {
      popLong(T0, T1);
      asm.emitTDEQ0(T3);
      asm.emitDIVD(T0, T1, T3);      // T0 := T1/T3
      asm.emitMULLD(T0, T0, T3);   // T0 := [T1/T3]*T3
      asm.emitSUBFC(T1, T0, T1);   // T1 := T1 - [T1/T3]*T3
    } else {
      asm.emitOR(T0, T3, T2); // or two halves of denominator together
      asm.emitTWEQ0(T0);         // trap if 0.
      popLong(T0, T1);
      generateSysCall(16, Entrypoints.sysLongRemainderIPField);
//    If we had an implementation of Magic.unsignedRemainder and Magic.unsignedDiv we could write the else block as:
//      asm.emitOR(T0, T3, T2); // or two halves of denominator together
//      asm.emitTWEQ0(T0);         // trap if 0.
//      Offset methodOffset = Entrypoints.lremMethod.getOffset();
//      asm.emitLAddrToc(T0, methodOffset);
//      asm.emitMTCTR(T0);
//      popLong(T0, T1);
//      asm.emitBCCTRL();
    }
    pushLong(T0, T1);
  }

  @Override
  protected void emit_lneg() {
    popLong(T1, T0);
    if (VM.BuildFor64Addr) {
      asm.emitNEG(T0, T0);
    } else {
      asm.emitSUBFIC(T0, T0, 0x0);
      asm.emitSUBFZE(T1, T1);
    }
    pushLong(T1, T0);
  }

  @Override
  protected void emit_lshl() {
    popInt(T0);                    // T0 is n
    popLong(T2, T1);
    if (VM.BuildFor64Addr) {
      asm.emitANDI(T0, T0, 0x3F);
      asm.emitSLD(T1, T1, T0);
      pushLong(T1, T1);
    } else {
      asm.emitANDI(T3, T0, 0x20);  // shift more than 31 bits?
      asm.emitXOR(T0, T3, T0);    // restrict shift to at most 31 bits
      asm.emitSLW(T3, T1, T0);    // low bits of l shifted n or n-32 bits
      ForwardReference fr1 = asm.emitForwardBC(EQ); // if shift less than 32, goto
      asm.emitLVAL(T0, 0);        // low bits are zero
      pushLong(T3, T0);
      ForwardReference fr2 = asm.emitForwardB();
      fr1.resolve(asm);
      asm.emitSLW(T2, T2, T0);    // high bits of l shifted n bits left
      asm.emitSUBFIC(T0, T0, 0x20);  // T0 := 32 - T0;
      asm.emitSRW(T1, T1, T0);    // T1 is middle bits of result
      asm.emitOR(T2, T2, T1);    // T2 is high bits of result
      pushLong(T2, T3);
      fr2.resolve(asm);
    }
  }

  @Override
  protected void emit_lshr() {
    popInt(T0);                    // T0 is n
    popLong(T2, T1);
    if (VM.BuildFor64Addr) {
      asm.emitANDI(T0, T0, 0x3F);
      asm.emitSRAD(T1, T1, T0);
      pushLong(T1, T1);
    } else {
      asm.emitANDI(T3, T0, 0x20);  // shift more than 31 bits?
      asm.emitXOR(T0, T3, T0);    // restrict shift to at most 31 bits
      asm.emitSRAW(T3, T2, T0);    // high bits of l shifted n or n-32 bits
      ForwardReference fr1 = asm.emitForwardBC(EQ);
      asm.emitSRAWI(T0, T3, 0x1F);  // propogate a full work of sign bit
      pushLong(T0, T3);
      ForwardReference fr2 = asm.emitForwardB();
      fr1.resolve(asm);
      asm.emitSRW(T1, T1, T0);    // low bits of l shifted n bits right
      asm.emitSUBFIC(T0, T0, 0x20);  // T0 := 32 - T0;
      asm.emitSLW(T2, T2, T0);    // T2 is middle bits of result
      asm.emitOR(T1, T1, T2);    // T1 is low bits of result
      pushLong(T3, T1);
      fr2.resolve(asm);
    }
  }

  @Override
  protected void emit_lushr() {
    popInt(T0);                    // T0 is n
    popLong(T2, T1);
    if (VM.BuildFor64Addr) {
      asm.emitANDI(T0, T0, 0x3F);
      asm.emitSRD(T1, T1, T0);
      pushLong(T1, T1);
    } else {
      asm.emitANDI(T3, T0, 0x20);  // shift more than 31 bits?
      asm.emitXOR(T0, T3, T0);    // restrict shift to at most 31 bits
      asm.emitSRW(T3, T2, T0);    // high bits of l shifted n or n-32 bits
      ForwardReference fr1 = asm.emitForwardBC(EQ);
      asm.emitLVAL(T0, 0);        // high bits are zero
      pushLong(T0, T3);
      ForwardReference fr2 = asm.emitForwardB();
      fr1.resolve(asm);
      asm.emitSRW(T1, T1, T0);    // low bits of l shifted n bits right
      asm.emitSUBFIC(T0, T0, 0x20);  // T0 := 32 - T0;
      asm.emitSLW(T2, T2, T0);    // T2 is middle bits of result
      asm.emitOR(T1, T1, T2);    // T1 is low bits of result
      pushLong(T3, T1);
      fr2.resolve(asm);
    }
  }

  @Override
  protected void emit_land() {
    popLong(T2, T0);
    popLong(T3, T1);
    asm.emitAND(T0, T1, T0);
    if (VM.BuildFor32Addr) {
      asm.emitAND(T1, T2, T3);
    }
    pushLong(T1, T0);
  }

  @Override
  protected void emit_lor() {
    popLong(T2, T0);
    popLong(T3, T1);
    asm.emitOR(T0, T1, T0);
    if (VM.BuildFor32Addr) {
      asm.emitOR(T1, T2, T3);
    }
    pushLong(T1, T0);
  }

  @Override
  protected void emit_lxor() {
    popLong(T2, T0);
    popLong(T3, T1);
    asm.emitXOR(T0, T1, T0);
    if (VM.BuildFor32Addr) {
      asm.emitXOR(T1, T2, T3);
    }
    pushLong(T1, T0);
  }

  /*
  * float ALU
  */

  @Override
  protected void emit_fadd() {
    popFloat(F0);
    popFloat(F1);
    asm.emitFADDS(F0, F1, F0);
    pushFloat(F0);
  }

  @Override
  protected void emit_fsub() {
    popFloat(F0);
    popFloat(F1);
    asm.emitFSUBS(F0, F1, F0);
    pushFloat(F0);
  }

  @Override
  protected void emit_fmul() {
    popFloat(F0);
    popFloat(F1);
    asm.emitFMULS(F0, F1, F0); // single precision multiply
    pushFloat(F0);
  }

  @Override
  protected void emit_fdiv() {
    popFloat(F0);
    popFloat(F1);
    asm.emitFDIVS(F0, F1, F0);
    pushFloat(F0);
  }

  @Override
  protected void emit_frem() {
    popFloat(F1);
    popFloat(F0);
    generateSysCall(16, Entrypoints.sysDoubleRemainderIPField);
    pushFloat(F0);
  }

  @Override
  protected void emit_fneg() {
    popFloat(F0);
    asm.emitFNEG(F0, F0);
    pushFloat(F0);
  }

 /*
  * double ALU
  */

  @Override
  protected void emit_dadd() {
    popDouble(F0);
    popDouble(F1);
    asm.emitFADD(F0, F1, F0);
    pushDouble(F0);
  }

  @Override
  protected void emit_dsub() {
    popDouble(F0);
    popDouble(F1);
    asm.emitFSUB(F0, F1, F0);
    pushDouble(F0);
  }

  @Override
  protected void emit_dmul() {
    popDouble(F0);
    popDouble(F1);
    asm.emitFMUL(F0, F1, F0);
    pushDouble(F0);
  }

  @Override
  protected void emit_ddiv() {
    popDouble(F0);
    popDouble(F1);
    asm.emitFDIV(F0, F1, F0);
    pushDouble(F0);
  }

  @Override
  protected void emit_drem() {
    popDouble(F1);                 //F1 is b
    popDouble(F0);                 //F0 is a
    generateSysCall(16, Entrypoints.sysDoubleRemainderIPField);
    pushDouble(F0);
  }

  @Override
  protected void emit_dneg() {
    popDouble(F0);
    asm.emitFNEG(F0, F0);
    pushDouble(F0);
  }

 /*
  * conversion ops
  */

  @Override
  protected void emit_i2l() {
    if (VM.BuildFor64Addr) {
      popInt(T0);
      pushLong(T0, T0);
    } else {
      peekInt(T0, 0);
      asm.emitSRAWI(T1, T0, 31);
      pushInt(T1);
    }
  }

  @Override
  protected void emit_i2f() {
    if (VM.BuildFor64Addr) {
      popInt(T0);               // TO is X  (an int)
      pushLong(T0, T0);
      popDouble(F0);            // load long
      asm.emitFCFID(F0, F0);    // convert it
      pushFloat(F0);            // store the float
    } else {
      popInt(T0);               // TO is X  (an int)
      asm.emitLFDtoc(F0, Entrypoints.IEEEmagicField.getOffset(), T1);  // F0 is MAGIC
      asm.emitSTFDoffset(F0, THREAD_REGISTER, Entrypoints.scratchStorageField.getOffset());
      asm.emitSTWoffset(T0, THREAD_REGISTER, Entrypoints.scratchStorageField.getOffset().plus(4));
      asm.emitCMPI(T0, 0);                // is X < 0
      ForwardReference fr = asm.emitForwardBC(GE);
      asm.emitLIntOffset(T0, THREAD_REGISTER, Entrypoints.scratchStorageField.getOffset());
      asm.emitADDI(T0, -1, T0);            // decrement top of MAGIC
      asm.emitSTWoffset(T0,
                        THREAD_REGISTER,
                        Entrypoints.scratchStorageField.getOffset()); // MAGIC + X in scratch field
      fr.resolve(asm);
      asm.emitLFDoffset(F1, THREAD_REGISTER, Entrypoints.scratchStorageField.getOffset()); // F1 is MAGIC + X
      asm.emitFSUB(F1, F1, F0);            // F1 is X
      pushFloat(F1);                         // float(X) is on stack
    }
  }

  @Override
  protected void emit_i2d() {
    if (VM.BuildFor64Addr) {
      popInt(T0);               //TO is X  (an int)
      pushLong(T0, T0);
      popDouble(F0);              // load long
      asm.emitFCFID(F0, F0);      // convert it
      pushDouble(F0);  // store the float
    } else {
      popInt(T0);                               // T0 is X (an int)
      asm.emitLFDtoc(F0, Entrypoints.IEEEmagicField.getOffset(), T1);  // F0 is MAGIC
      pushDouble(F0);               // MAGIC on stack
      pokeInt(T0, 1);               // if 0 <= X, MAGIC + X
      asm.emitCMPI(T0, 0);                   // is X < 0
      ForwardReference fr = asm.emitForwardBC(GE); // ow, handle X < 0
      popInt(T0);               // T0 is top of MAGIC
      asm.emitADDI(T0, -1, T0);               // decrement top of MAGIC
      pushInt(T0);               // MAGIC + X is on stack
      fr.resolve(asm);
      popDouble(F1);               // F1 is MAGIC + X
      asm.emitFSUB(F1, F1, F0);               // F1 is X
      pushDouble(F1);               // float(X) is on stack
    }
  }

  @Override
  protected void emit_l2i() {
    discardSlot();
  }

  @Override
  protected void emit_l2f() {
    popLong(T0, VM.BuildFor64Addr ? T0 : T1);
    generateSysCall(8, Entrypoints.sysLongToFloatIPField);
    pushFloat(F0);
  }

  @Override
  protected void emit_l2d() {
    popLong(T0, VM.BuildFor64Addr ? T0 : T1);
    generateSysCall(8, Entrypoints.sysLongToDoubleIPField);
    pushDouble(F0);
  }

  @Override
  protected void emit_f2i() {
    popFloat(F0);
    asm.emitFCMPU(F0, F0);
    ForwardReference fr1 = asm.emitForwardBC(NE);
    // Normal case: F0 == F0 therefore not a NaN
    asm.emitFCTIWZ(F0, F0);
    if (VM.BuildFor64Addr) {
      pushLowDoubleAsInt(F0);
    } else {
      asm.emitSTFDoffset(F0, THREAD_REGISTER, Entrypoints.scratchStorageField.getOffset());
      asm.emitLIntOffset(T0, THREAD_REGISTER, Entrypoints.scratchStorageField.getOffset().plus(4));
      pushInt(T0);
    }
    ForwardReference fr2 = asm.emitForwardB();
    fr1.resolve(asm);
    // A NaN => 0
    asm.emitLVAL(T0, 0);
    pushInt(T0);
    fr2.resolve(asm);
  }

  @Override
  protected void emit_f2l() {
    popFloat(F0);
    generateSysCall(4, Entrypoints.sysFloatToLongIPField);
    pushLong(T0, VM.BuildFor64Addr ? T0 : T1);
  }

  @Override
  protected void emit_f2d() {
    popFloat(F0);
    pushDouble(F0);
  }

  @Override
  protected void emit_d2i() {
    popDouble(F0);
    asm.emitFCTIWZ(F0, F0);
    pushLowDoubleAsInt(F0);
  }

  @Override
  protected void emit_d2l() {
    popDouble(F0);
    generateSysCall(8, Entrypoints.sysDoubleToLongIPField);
    pushLong(T0, VM.BuildFor64Addr ? T0 : T1);
  }

  @Override
  protected void emit_d2f() {
    popDouble(F0);
    asm.emitFRSP(F0, F0);
    pushFloat(F0);
  }

  @Override
  protected void emit_i2b() {
    popByteAsInt(T0);
    pushInt(T0);
  }

  @Override
  protected void emit_i2c() {
    popCharAsInt(T0);
    pushInt(T0);
  }

  @Override
  protected void emit_i2s() {
    popShortAsInt(T0);
    pushInt(T0);
  }

  /*
  * comparison ops
  */

  @Override
  protected void emit_regular_lcmp() {
    popLong(T3, T2);
    popLong(T1, T0);

    ForwardReference fr_end_1;
    ForwardReference fr_end_2;
    if (VM.BuildFor64Addr) {
      asm.emitCMPD(T0, T2);
      ForwardReference fr1 = asm.emitForwardBC(LT);
      ForwardReference fr2 = asm.emitForwardBC(GT);
      asm.emitLVAL(T0, 0);      // a == b
      fr_end_1 = asm.emitForwardB();
      fr1.resolve(asm);
      asm.emitLVAL(T0, -1);      // a <  b
      fr_end_2 = asm.emitForwardB();
      fr2.resolve(asm);
    } else {
      asm.emitCMP(T1, T3);      // ah ? al
      ForwardReference fr1 = asm.emitForwardBC(LT);
      ForwardReference fr2 = asm.emitForwardBC(GT);
      asm.emitCMPL(T0, T2);      // al ? bl (logical compare)
      ForwardReference fr3 = asm.emitForwardBC(LT);
      ForwardReference fr4 = asm.emitForwardBC(GT);
      asm.emitLVAL(T0, 0);      // a == b
      fr_end_1 = asm.emitForwardB();
      fr1.resolve(asm);
      fr3.resolve(asm);
      asm.emitLVAL(T0, -1);      // a <  b
      fr_end_2 = asm.emitForwardB();
      fr2.resolve(asm);
      fr4.resolve(asm);
    }
    asm.emitLVAL(T0, 1);      // a >  b
    fr_end_1.resolve(asm);
    fr_end_2.resolve(asm);
    pushInt(T0);
  }

  @Override
  protected void emit_regular_DFcmpGL(boolean single, boolean unorderedGT) {
    if (single) {
      popFloat(F1);
      popFloat(F0);
    } else {
      popDouble(F1);
      popDouble(F0);
    }
    asm.emitLVAL(T0, unorderedGT ? -1 : 1); // pre-load T0
    asm.emitFCMPU(F0, F1);
    ForwardReference golden = asm.emitForwardBC(unorderedGT ? LT : GT); // value in T0 is good
    ForwardReference equals = asm.emitForwardBC(EQ); // branch and zero T0
    asm.emitLVAL(T0, unorderedGT ? 1 : -1); // value is either unordered or GT/LT
    ForwardReference unordered = asm.emitForwardB();
    equals.resolve(asm);
    asm.emitLVAL(T0, 0);
    golden.resolve(asm);
    unordered.resolve(asm);
    pushInt(T0);
  }

  /*
  * branching
  */

  /**
   * @param bc the branch condition
   * @return assembler constant equivalent for the branch condition
   */
  @Pure
  private int mapCondition(BranchCondition bc) {
    switch (bc) {
      case EQ: return EQ;
      case NE: return NE;
      case LT: return LT;
      case GE: return GE;
      case GT: return GT;
      case LE: return LE;
      default:
        if (VM.VerifyAssertions) VM._assert(VM.NOT_REACHED);
        return -1;
    }
  }

  @Override
  @Inline(value = Inline.When.ArgumentsAreConstant, arguments = {2})
  protected void emit_if(int bTarget, BranchCondition bc) {
    popInt(T0);
    asm.emitADDICr(T0, T0, 0); // compares T0 to 0 and sets CR0
    genCondBranch(mapCondition(bc), bTarget);
  }

  @Override
  @Inline(value = Inline.When.ArgumentsAreConstant, arguments = {2})
  protected void emit_if_icmp(int bTarget, BranchCondition bc) {
    popInt(T1);
    popInt(T0);
    asm.emitCMP(T0, T1);    // sets CR0
    genCondBranch(mapCondition(bc), bTarget);
  }

  @Override
  protected void emit_if_acmpeq(int bTarget) {
    popAddr(T1);
    popAddr(T0);
    asm.emitCMPLAddr(T0, T1);    // sets CR0
    genCondBranch(EQ, bTarget);
  }

  @Override
  protected void emit_if_acmpne(int bTarget) {
    popAddr(T1);
    popAddr(T0);
    asm.emitCMPLAddr(T0, T1);    // sets CR0
    genCondBranch(NE, bTarget);
  }

  @Override
  protected void emit_ifnull(int bTarget) {
    popAddr(T0);
    asm.emitLVAL(T1, 0);
    asm.emitCMPLAddr(T0, T1);
    genCondBranch(EQ, bTarget);
  }

  @Override
  protected void emit_ifnonnull(int bTarget) {
    popAddr(T0);
    asm.emitLVAL(T1, 0);
    asm.emitCMPLAddr(T0, T1);
    genCondBranch(NE, bTarget);
  }

  @Override
  protected void emit_goto(int bTarget) {
    int mTarget = bytecodeMap[bTarget];
    asm.emitB(mTarget, bTarget);
  }

  @Override
  protected void emit_jsr(int bTarget) {
    ForwardReference fr = asm.emitForwardBL();
    fr.resolve(asm); // get PC into LR...
    int start = asm.getMachineCodeIndex();
    int delta = 4;
    asm.emitMFLR(T1);           // LR +  0
    asm.emitADDI(T1, delta * INSTRUCTION_WIDTH, T1);   // LR +  4
    pushAddr(T1);   // LR +  8
    asm.emitBL(bytecodeMap[bTarget], bTarget); // LR + 12
    int done = asm.getMachineCodeIndex();
    if (VM.VerifyAssertions) VM._assert((done - start) == delta);
  }

  @Override
  protected void emit_ret(int index) {
    short location = getGeneralLocalLocation(index);

    if (!isRegister(location)) {
      copyMemToReg(ADDRESS_TYPE, locationToOffset(location), T0);
      location = T0.value();
    }
    asm.emitMTLR(GPR.lookup(location));
    asm.emitBCLR();
  }

  @Override
  protected void emit_tableswitch(int defaultval, int low, int high) {
    int bTarget = biStart + defaultval;
    int mTarget = bytecodeMap[bTarget];
    int n = high - low + 1;       // n = number of normal cases (0..n-1)
    int firstCounter = edgeCounterIdx; // only used if options.PROFILE_EDGE_COUNTERS;

    if (options.PROFILE_EDGE_COUNTERS) {
      edgeCounterIdx += n + 1; // allocate n+1 counters

      // Load counter array for this method
      loadCounterArray(T2);
    }

    popInt(T0);  // T0 is index
    if (Assembler.fits(-low, 16)) {
      asm.emitADDI(T0, -low, T0);
    } else {
      asm.emitLVAL(T1, low);
      asm.emitSUBFC(T0, T1, T0);
    }
    asm.emitLVAL(T3, n);
    asm.emitCMPL(T0, T3);

    if (options.PROFILE_EDGE_COUNTERS) {
      ForwardReference fr = asm.emitForwardBC(LT); // jump around jump to default target
      incEdgeCounter(T2, S0, T3, firstCounter + n);
      asm.emitB(mTarget, bTarget);
      fr.resolve(asm);
    } else {
      // conditionally jump to default target
      if (bTarget - SHORT_FORWARD_LIMIT < biStart) {
        asm.emitShortBC(GE, mTarget, bTarget);
      } else {
        asm.emitBC(GE, mTarget, bTarget);
      }
    }
    ForwardReference fr1 = asm.emitForwardBL();
    for (int i = 0; i < n; i++) {
      int offset = bcodes.getTableSwitchOffset(i);
      bTarget = biStart + offset;
      mTarget = bytecodeMap[bTarget];
      asm.emitSwitchCase(i, mTarget, bTarget);
    }
    bcodes.skipTableSwitchOffsets(n);
    fr1.resolve(asm);
    asm.emitMFLR(T1);         // T1 is base of table
    asm.emitSLWI(T0, T0, LOG_BYTES_IN_INT); // convert to bytes
    if (options.PROFILE_EDGE_COUNTERS) {
      incEdgeCounterIdx(T2, S0, T3, firstCounter, T0);
    }
    asm.emitLIntX(T0, T0, T1); // T0 is relative offset of desired case
    asm.emitADD(T1, T1, T0); // T1 is absolute address of desired case
    asm.emitMTCTR(T1);
    asm.emitBCCTR();
  }

  @Override
  protected void emit_lookupswitch(int defaultval, int npairs) {
    if (options.PROFILE_EDGE_COUNTERS) {
      // Load counter array for this method
      loadCounterArray(T2);
    }

    popInt(T0); // T0 is key
    for (int i = 0; i < npairs; i++) {
      int match = bcodes.getLookupSwitchValue(i);
      if (Assembler.fits(match, 16)) {
        asm.emitCMPI(T0, match);
      } else {
        asm.emitLVAL(T1, match);
        asm.emitCMP(T0, T1);
      }
      int offset = bcodes.getLookupSwitchOffset(i);
      int bTarget = biStart + offset;
      int mTarget = bytecodeMap[bTarget];
      if (options.PROFILE_EDGE_COUNTERS) {
        // Flip conditions so we can jump over the increment of the taken counter.
        ForwardReference fr = asm.emitForwardBC(NE);
        // Increment counter & jump to target
        incEdgeCounter(T2, S0, T3, edgeCounterIdx++);
        asm.emitB(mTarget, bTarget);
        fr.resolve(asm);
      } else {
        if (bTarget - SHORT_FORWARD_LIMIT < biStart) {
          asm.emitShortBC(EQ, mTarget, bTarget);
        } else {
          asm.emitBC(EQ, mTarget, bTarget);
        }
      }
    }
    bcodes.skipLookupSwitchPairs(npairs);
    int bTarget = biStart + defaultval;
    int mTarget = bytecodeMap[bTarget];
    if (options.PROFILE_EDGE_COUNTERS) {
      incEdgeCounter(T2, S0, T3, edgeCounterIdx++);
    }
    asm.emitB(mTarget, bTarget);
  }

  /*
  * returns (from function; NOT ret)
  */

  @Override
  protected void emit_ireturn() {
    if (method.isSynchronized()) genSynchronizedMethodEpilogue();
    peekInt(T0, 0);
    genEpilogue();
  }

  @Override
  protected void emit_lreturn() {
    if (method.isSynchronized()) genSynchronizedMethodEpilogue();
    peekLong(T0, VM.BuildFor64Addr ? T0 : T1, 0);
    genEpilogue();
  }

  @Override
  protected void emit_freturn() {
    if (method.isSynchronized()) genSynchronizedMethodEpilogue();
    peekFloat(F0, 0);
    genEpilogue();
  }

  @Override
  protected void emit_dreturn() {
    if (method.isSynchronized()) genSynchronizedMethodEpilogue();
    peekDouble(F0, 0);
    genEpilogue();
  }

  @Override
  protected void emit_areturn() {
    if (method.isSynchronized()) genSynchronizedMethodEpilogue();
    peekAddr(T0, 0);
    genEpilogue();
  }

  @Override
  protected void emit_return() {
    if (method.isSynchronized()) genSynchronizedMethodEpilogue();
    if (method.isObjectInitializer() && method.getDeclaringClass().declaresFinalInstanceField()) {
      /* JMM compliance. Emit StoreStore barrier */
      asm.emitSYNC();
    }
    genEpilogue();
  }

  /*
  * field access
  */

  @Override
  protected void emit_unresolved_getstatic(FieldReference fieldRef) {
    emitDynamicLinkingSequence(T0, fieldRef, true);
    TypeReference fieldType = fieldRef.getFieldContentsType();
    if (NEEDS_OBJECT_GETSTATIC_BARRIER && fieldType.isReferenceType()) {
      Barriers.compileGetstaticBarrier(this, fieldRef.getId());
      pushAddr(T0);
    } else if (fieldRef.getSize() <= BYTES_IN_INT) { // field is one word
      asm.emitLIntX(T1, T0, JTOC);
      pushInt(T1);
    } else { // field is two words (double or long ( or address on PPC64))
      if (VM.VerifyAssertions) VM._assert(fieldRef.getSize() == BYTES_IN_LONG);
      if (VM.BuildFor64Addr && fieldRef.getNumberOfStackSlots() == 1) { // address only 1 stackslot!!!
        asm.emitLDX(T1, T0, JTOC);
        pushAddr(T1);
      } else {
        asm.emitLFDX(F0, T0, JTOC);
        pushDouble(F0);
      }
    }

    // JMM: could be volatile (post-barrier when first operation)
    // LoadLoad and LoadStore barriers.
    asm.emitHWSYNC();
  }

  @Override
  protected void emit_resolved_getstatic(FieldReference fieldRef) {
    RVMField field = fieldRef.peekResolvedField();
    Offset fieldOffset = field.getOffset();
    TypeReference fieldType = fieldRef.getFieldContentsType();
    if (NEEDS_OBJECT_GETSTATIC_BARRIER && fieldType.isReferenceType() && !field.isUntraced()) {
      Barriers.compileGetstaticBarrierImm(this, fieldOffset, fieldRef.getId());
      pushAddr(T0);
    } else if (fieldRef.getSize() <= BYTES_IN_INT) { // field is one word
      asm.emitLIntToc(T0, fieldOffset);
      pushInt(T0);
    } else { // field is two words (double or long ( or address on PPC64))
      if (VM.VerifyAssertions) VM._assert(fieldRef.getSize() == BYTES_IN_LONG);
      if (VM.BuildFor64Addr && fieldRef.getNumberOfStackSlots() == 1) {    //address only 1 stackslot!!!
        asm.emitLAddrToc(T0, fieldOffset);
        pushAddr(T0);
      } else {
        asm.emitLFDtoc(F0, fieldOffset, T0);
        pushDouble(F0);
      }
    }

    if (field.isVolatile()) {
      // JMM: post-barrier when first operation
      // LoadLoad and LoadStore barriers.
      asm.emitHWSYNC();
    }
  }

  @Override
  protected void emit_unresolved_putstatic(FieldReference fieldRef) {
    // JMM: could be volatile (pre-barrier when second operation)
    // StoreStore barrier.
    asm.emitSYNC();

    emitDynamicLinkingSequence(T1, fieldRef, true);
    if (NEEDS_OBJECT_PUTSTATIC_BARRIER && !fieldRef.getFieldContentsType().isPrimitiveType()) {
      Barriers.compilePutstaticBarrier(this, fieldRef.getId()); // NOTE: offset is in T0 from emitDynamicLinkingSequence
      discardSlots(1);
    } else if (fieldRef.getSize() <= BYTES_IN_INT) { // field is one word
      popInt(T0);
      asm.emitSTWX(T0, T1, JTOC);
    } else { // field is two words (double or long (or address on PPC64))
      if (VM.VerifyAssertions) VM._assert(fieldRef.getSize() == BYTES_IN_LONG);
      if (VM.BuildFor64Addr && fieldRef.getNumberOfStackSlots() == 1) {    //address only 1 stackslot!!!
        popAddr(T0);
        asm.emitSTDX(T0, T1, JTOC);
      } else {
        popDouble(F0);
        asm.emitSTFDX(F0, T1, JTOC);
      }
    }

    // JMM: Could be volatile, post-barrier when first operation
    // StoreLoad barrier.
    asm.emitHWSYNC();
  }

  @Override
  protected void emit_resolved_putstatic(FieldReference fieldRef) {
    RVMField field = fieldRef.peekResolvedField();
    Offset fieldOffset = field.getOffset();

    if (field.isVolatile()) {
      // JMM: (pre-barrier when second operation)
      // StoreStore barrier.
      asm.emitSYNC();
    }

    if (NEEDS_OBJECT_PUTSTATIC_BARRIER && !fieldRef.getFieldContentsType().isPrimitiveType() && !field.isUntraced()) {
      Barriers.compilePutstaticBarrierImm(this, fieldOffset, fieldRef.getId());
      discardSlots(1);
    } else if (fieldRef.getSize() <= BYTES_IN_INT) { // field is one word
      popInt(T0);
      asm.emitSTWtoc(T0, fieldOffset, T1);
    } else { // field is two words (double or long (or address on PPC64))
      if (VM.VerifyAssertions) VM._assert(fieldRef.getSize() == BYTES_IN_LONG);
      if (VM.BuildFor64Addr && fieldRef.getNumberOfStackSlots() == 1) {    //address only 1 stackslot!!!
        popAddr(T0);
        asm.emitSTDtoc(T0, fieldOffset, T1);
      } else {
        popDouble(F0);
        asm.emitSTFDtoc(F0, fieldOffset, T0);
      }
    }

    if (field.isVolatile()) {
      // JMM: post-barrier when first operation
      // StoreLoad barrier.
      asm.emitHWSYNC();
    }
  }

  @Override
  protected void emit_unresolved_getfield(FieldReference fieldRef) {
    TypeReference fieldType = fieldRef.getFieldContentsType();
    // T1 = field offset from emitDynamicLinkingSequence()
    emitDynamicLinkingSequence(T1, fieldRef, true);
    if (NEEDS_OBJECT_GETFIELD_BARRIER && fieldType.isReferenceType()) {
      Barriers.compileGetfieldBarrier(this, fieldRef.getId());
      discardSlots(1);
      pushAddr(T0);
    } else {
      // T2 = object reference
      popAddr(T2);
      if (fieldType.isReferenceType() || fieldType.isWordLikeType()) {
        // 32/64bit reference/word load
        asm.emitLAddrX(T0, T1, T2);
        pushAddr(T0);
      } else if (fieldType.isBooleanType()) {
        // 8bit unsigned load
        asm.emitLBZX(T0, T1, T2);
        pushInt(T0);
      } else if (fieldType.isByteType()) {
        // 8bit signed load
        asm.emitLBZX(T0, T1, T2);
        asm.emitEXTSB(T0, T0);
        pushInt(T0);
      } else if (fieldType.isShortType()) {
        // 16bit signed load
        asm.emitLHAX(T0, T1, T2);
        pushInt(T0);
      } else if (fieldType.isCharType()) {
        // 16bit unsigned load
        asm.emitLHZX(T0, T1, T2);
        pushInt(T0);
      } else if (fieldType.isIntType() || fieldType.isFloatType()) {
        // 32bit load
        asm.emitLIntX(T0, T1, T2);
        pushInt(T0);
      } else {
        // 64bit load
        if (VM.VerifyAssertions) VM._assert(fieldType.isLongType() || fieldType.isDoubleType());
        asm.emitLFDX(F0, T1, T2);
        pushDouble(F0);
      }
    }

    // JMM: Could be volatile; post-barrier when first operation
    // LoadLoad and LoadStore barriers.
    asm.emitHWSYNC();
  }

  @Override
  protected void emit_resolved_getfield(FieldReference fieldRef) {
    RVMField field = fieldRef.peekResolvedField();
    TypeReference fieldType = fieldRef.getFieldContentsType();
    Offset fieldOffset = field.getOffset();
    if (NEEDS_OBJECT_GETFIELD_BARRIER && fieldType.isReferenceType() && !field.isUntraced()) {
      Barriers.compileGetfieldBarrierImm(this, fieldOffset, fieldRef.getId());
      discardSlots(1);
      pushAddr(T0);
    } else {
      popAddr(T1); // T1 = object reference
      if (fieldType.isReferenceType() || fieldType.isWordLikeType()) {
        // 32/64bit reference/word load
        asm.emitLAddrOffset(T0, T1, fieldOffset);
        pushAddr(T0);
      } else if (fieldType.isBooleanType()) {
        // 8bit unsigned load
        asm.emitLBZoffset(T0, T1, fieldOffset);
        pushInt(T0);
      } else if (fieldType.isByteType()) {
        // 8bit signed load
        asm.emitLBZoffset(T0, T1, fieldOffset);
        asm.emitEXTSB(T0, T0); // sign extend
        pushInt(T0);
      } else if (fieldType.isShortType()) {
        // 16bit signed load
        asm.emitLHAoffset(T0, T1, fieldOffset);
        pushInt(T0);
      } else if (fieldType.isCharType()) {
        // 16bit unsigned load
        asm.emitLHZoffset(T0, T1, fieldOffset);
        pushInt(T0);
      } else if (fieldType.isIntType() || fieldType.isFloatType()) {
        // 32bit load
        asm.emitLIntOffset(T0, T1, fieldOffset);
        pushInt(T0);
      } else {
        // 64bit load
        if (VM.VerifyAssertions) VM._assert(fieldType.isLongType() || fieldType.isDoubleType());
        asm.emitLFDoffset(F0, T1, fieldOffset);
        pushDouble(F0);
      }
    }

    if (field.isVolatile()) {
      // JMM: post-barrier when first operation
      // LoadLoad and LoadStore barriers.
      asm.emitHWSYNC();
    }
  }

  @Override
  protected void emit_unresolved_putfield(FieldReference fieldRef) {
    // JMM: could be volatile (pre-barrier when second operation)
    // StoreStore barrier.
    asm.emitSYNC();

    TypeReference fieldType = fieldRef.getFieldContentsType();
    // T2 = field offset from emitDynamicLinkingSequence()
    emitDynamicLinkingSequence(T2, fieldRef, true);
    if (fieldType.isReferenceType()) {
      // 32/64bit reference store
      if (NEEDS_OBJECT_PUTFIELD_BARRIER) {
        // NOTE: offset is in T2 from emitDynamicLinkingSequence
        Barriers.compilePutfieldBarrier(this, fieldRef.getId());
        discardSlots(2);
      } else {
        popAddr(T0);                // T0 = address value
        popAddr(T1);                // T1 = object reference
        asm.emitSTAddrX(T0, T1, T2);
      }
    } else if (NEEDS_BOOLEAN_PUTFIELD_BARRIER && fieldType.isBooleanType()) {
      Barriers.compilePutfieldBarrierBoolean(this, fieldRef.getId());
    } else if (NEEDS_BYTE_PUTFIELD_BARRIER && fieldType.isByteType()) {
      Barriers.compilePutfieldBarrierByte(this, fieldRef.getId());
    } else if (NEEDS_CHAR_PUTFIELD_BARRIER && fieldType.isCharType()) {
      Barriers.compilePutfieldBarrierChar(this, fieldRef.getId());
    } else if (NEEDS_DOUBLE_PUTFIELD_BARRIER && fieldType.isDoubleType()) {
      Barriers.compilePutfieldBarrierDouble(this, fieldRef.getId());
    } else if (NEEDS_FLOAT_PUTFIELD_BARRIER && fieldType.isFloatType()) {
      Barriers.compilePutfieldBarrierFloat(this, fieldRef.getId());
    } else if (NEEDS_INT_PUTFIELD_BARRIER && fieldType.isIntType()) {
      Barriers.compilePutfieldBarrierInt(this, fieldRef.getId());
    } else if (NEEDS_LONG_PUTFIELD_BARRIER && fieldType.isLongType()) {
      Barriers.compilePutfieldBarrierLong(this, fieldRef.getId());
    } else if (NEEDS_SHORT_PUTFIELD_BARRIER && fieldType.isShortType()) {
      Barriers.compilePutfieldBarrierShort(this, fieldRef.getId());
    } else if (NEEDS_WORD_PUTFIELD_BARRIER && fieldType.isWordType()) {
      Barriers.compilePutfieldBarrierWord(this, fieldRef.getId());
    } else if (NEEDS_ADDRESS_PUTFIELD_BARRIER && fieldType.isAddressType()) {
      Barriers.compilePutfieldBarrierAddress(this, fieldRef.getId());
    } else if (NEEDS_OFFSET_PUTFIELD_BARRIER && fieldType.isOffsetType()) {
      Barriers.compilePutfieldBarrierOffset(this, fieldRef.getId());
    } else if (NEEDS_EXTENT_PUTFIELD_BARRIER && fieldType.isExtentType()) {
      Barriers.compilePutfieldBarrierExtent(this, fieldRef.getId());
    } else if (fieldType.isWordLikeType()) {
      // 32/64bit word store
      popAddr(T0);                // T0 = value
      popAddr(T1);                // T1 = object reference
      asm.emitSTAddrX(T0, T1, T2);
    } else if (fieldType.isBooleanType() || fieldType.isByteType()) {
      // 8bit store
      popInt(T0); // T0 = value
      popAddr(T1); // T1 = object reference
      asm.emitSTBX(T0, T1, T2);
    } else if (fieldType.isShortType() || fieldType.isCharType()) {
      // 16bit store
      popInt(T0); // T0 = value
      popAddr(T1); // T1 = object reference
      asm.emitSTHX(T0, T1, T2);
    } else if (fieldType.isIntType() || fieldType.isFloatType()) {
      // 32bit store
      popInt(T0); // T0 = value
      popAddr(T1); // T1 = object reference
      asm.emitSTWX(T0, T1, T2);
    } else {
      // 64bit store
      if (VM.VerifyAssertions) VM._assert(fieldType.isLongType() || fieldType.isDoubleType());
      popDouble(F0);     // F0 = doubleword value
      popAddr(T1);       // T1 = object reference
      asm.emitSTFDX(F0, T1, T2);
    }

    // JMM: Could be volatile; post-barrier when first operation
    // StoreLoad barrier.
    asm.emitHWSYNC();
  }

  @Override
  protected void emit_resolved_putfield(FieldReference fieldRef) {
    RVMField field = fieldRef.peekResolvedField();
    Offset fieldOffset = field.getOffset();
    TypeReference fieldType = fieldRef.getFieldContentsType();

    if (field.isVolatile()) {
      // JMM: pre-barrier when second operation
      // StoreStore barrier.
      asm.emitSYNC();
    }

    if (fieldType.isReferenceType()) {
      // 32/64bit reference store
      if (NEEDS_OBJECT_PUTFIELD_BARRIER && !field.isUntraced()) {
        Barriers.compilePutfieldBarrierImm(this, fieldOffset, fieldRef.getId());
        discardSlots(2);
      } else {
        popAddr(T0); // T0 = address value
        popAddr(T1); // T1 = object reference
        asm.emitSTAddrOffset(T0, T1, fieldOffset);
      }
    } else if (NEEDS_BOOLEAN_PUTFIELD_BARRIER && fieldType.isBooleanType()) {
      Barriers.compilePutfieldBarrierBooleanImm(this, fieldOffset, fieldRef.getId());
    } else if (NEEDS_BYTE_PUTFIELD_BARRIER && fieldType.isByteType()) {
      Barriers.compilePutfieldBarrierByteImm(this, fieldOffset, fieldRef.getId());
    } else if (NEEDS_CHAR_PUTFIELD_BARRIER && fieldType.isCharType()) {
      Barriers.compilePutfieldBarrierCharImm(this, fieldOffset, fieldRef.getId());
    } else if (NEEDS_DOUBLE_PUTFIELD_BARRIER && fieldType.isDoubleType()) {
      Barriers.compilePutfieldBarrierDoubleImm(this, fieldOffset, fieldRef.getId());
    } else if (NEEDS_FLOAT_PUTFIELD_BARRIER && fieldType.isFloatType()) {
      Barriers.compilePutfieldBarrierFloatImm(this, fieldOffset, fieldRef.getId());
    } else if (NEEDS_INT_PUTFIELD_BARRIER && fieldType.isIntType()) {
      Barriers.compilePutfieldBarrierIntImm(this, fieldOffset, fieldRef.getId());
    } else if (NEEDS_LONG_PUTFIELD_BARRIER && fieldType.isLongType()) {
      Barriers.compilePutfieldBarrierLongImm(this, fieldOffset, fieldRef.getId());
    } else if (NEEDS_SHORT_PUTFIELD_BARRIER && fieldType.isShortType()) {
      Barriers.compilePutfieldBarrierShortImm(this, fieldOffset, fieldRef.getId());
    } else if (NEEDS_WORD_PUTFIELD_BARRIER && fieldType.isWordType()) {
      Barriers.compilePutfieldBarrierWordImm(this, fieldOffset, fieldRef.getId());
    } else if (NEEDS_ADDRESS_PUTFIELD_BARRIER && fieldType.isAddressType()) {
      Barriers.compilePutfieldBarrierAddressImm(this, fieldOffset, fieldRef.getId());
    } else if (NEEDS_OFFSET_PUTFIELD_BARRIER && fieldType.isOffsetType()) {
      Barriers.compilePutfieldBarrierOffsetImm(this, fieldOffset, fieldRef.getId());
    } else if (NEEDS_EXTENT_PUTFIELD_BARRIER && fieldType.isExtentType()) {
      Barriers.compilePutfieldBarrierExtentImm(this, fieldOffset, fieldRef.getId());
    } else if (fieldType.isWordLikeType()) {
      // 32/64bit word store
      popAddr(T0);                // T0 = value
      popAddr(T1);                // T1 = object reference
      asm.emitSTAddrOffset(T0, T1, fieldOffset);
    } else if (fieldType.isBooleanType() || fieldType.isByteType()) {
      // 8bit store
      popInt(T0); // T0 = value
      popAddr(T1); // T1 = object reference
      asm.emitSTBoffset(T0, T1, fieldOffset);
    } else if (fieldType.isShortType() || fieldType.isCharType()) {
      // 16bit store
      popInt(T0); // T0 = value
      popAddr(T1); // T1 = object reference
      asm.emitSTHoffset(T0, T1, fieldOffset);
    } else if (fieldType.isIntType() || fieldType.isFloatType()) {
      // 32bit store
      popInt(T0); // T0 = value
      popAddr(T1); // T1 = object reference
      asm.emitSTWoffset(T0, T1, fieldOffset);
    } else {
      // 64bit store
      if (VM.VerifyAssertions) VM._assert(fieldType.isLongType() || fieldType.isDoubleType());
      popDouble(F0);     // F0 = doubleword value
      popAddr(T1);       // T1 = object reference
      asm.emitSTFDoffset(F0, T1, fieldOffset);
    }
    if (field.isVolatile()) {
      // JMM: post-barrier when first operation
      // StoreLoad barrier.
      asm.emitHWSYNC();
    }
  }

  /*
   * method invocation
   */

  @Override
  protected void emit_unresolved_invokevirtual(MethodReference methodRef) {
    int objectIndex = methodRef.getParameterWords(); // +1 for "this" parameter, -1 to load it
    emitDynamicLinkingSequence(T2, methodRef, true); // leaves method offset in T2
    peekAddr(T0, objectIndex);
    asm.baselineEmitLoadTIB(T1, T0); // load TIB
    asm.emitLAddrX(T2, T2, T1);
    asm.emitMTCTR(T2);
    genMoveParametersToRegisters(true, methodRef);
    asm.emitBCCTRL();
    genPopParametersAndPushReturnValue(true, methodRef);
  }

  @Override
  protected void emit_resolved_invokevirtual(MethodReference methodRef) {
    int objectIndex = methodRef.getParameterWords(); // +1 for "this" parameter, -1 to load it
    peekAddr(T0, objectIndex);
    asm.baselineEmitLoadTIB(T1, T0); // load TIB
    Offset methodOffset = methodRef.peekResolvedMethod().getOffset();
    asm.emitLAddrOffset(T2, T1, methodOffset);
    asm.emitMTCTR(T2);
    genMoveParametersToRegisters(true, methodRef);
    asm.emitBCCTRL();
    genPopParametersAndPushReturnValue(true, methodRef);
  }

  @Override
  protected void emit_resolved_invokespecial(MethodReference methodRef, RVMMethod target) {
    if (target.isObjectInitializer()) { // invoke via method's jtoc slot
      asm.emitLAddrToc(T0, target.getOffset());
    } else { // invoke via class's tib slot
      if (VM.VerifyAssertions) VM._assert(!target.isStatic());
      asm.emitLAddrToc(T0, target.getDeclaringClass().getTibOffset());
      asm.emitLAddrOffset(T0, T0, target.getOffset());
    }
    asm.emitMTCTR(T0);
    genMoveParametersToRegisters(true, methodRef);
    asm.emitBCCTRL();
    genPopParametersAndPushReturnValue(true, methodRef);
  }

  @Override
  protected void emit_unresolved_invokespecial(MethodReference methodRef) {
    // must be a static method; if it was a super then declaring class _must_ be resolved
    emitDynamicLinkingSequence(T2, methodRef, true); // leaves method offset in T2
    asm.emitLAddrX(T0, T2, JTOC);
    asm.emitMTCTR(T0);
    genMoveParametersToRegisters(true, methodRef);
    asm.emitBCCTRL();
    genPopParametersAndPushReturnValue(true, methodRef);
  }

  @Override
  protected void emit_unresolved_invokestatic(MethodReference methodRef) {
    emitDynamicLinkingSequence(T2, methodRef, true);                  // leaves method offset in T2
    asm.emitLAddrX(T0, T2, JTOC); // method offset left in T2 by emitDynamicLinkingSequence
    asm.emitMTCTR(T0);
    genMoveParametersToRegisters(false, methodRef);
    asm.emitBCCTRL();
    genPopParametersAndPushReturnValue(false, methodRef);
  }

  @Override
  protected void emit_resolved_invokestatic(MethodReference methodRef) {
    Offset methodOffset = methodRef.peekResolvedMethod().getOffset();
    asm.emitLAddrToc(T0, methodOffset);
    asm.emitMTCTR(T0);
    genMoveParametersToRegisters(false, methodRef);
    asm.emitBCCTRL();
    genPopParametersAndPushReturnValue(false, methodRef);
  }

  @Override
  protected void emit_invokeinterface(MethodReference methodRef) {
    int count = methodRef.getParameterWords() + 1; // +1 for "this" parameter
    RVMMethod resolvedMethod = null;
    resolvedMethod = methodRef.peekInterfaceMethod();

    // (1) Emit dynamic type checking sequence if required to
    // do so inline.
    if (VM.BuildForIMTInterfaceInvocation) {
      if (methodRef.isMiranda()) {
        // TODO: It's not entirely clear that we can just assume that
        //       the class actually implements the interface.
        //       However, we don't know what interface we need to be checking
        //       so there doesn't appear to be much else we can do here.
      } else {
        if (resolvedMethod == null) {
          // Can't successfully resolve it at compile time.
          // Call uncommon case typechecking routine to do the right thing when this code actually executes.
          asm.emitLAddrToc(T0, Entrypoints.unresolvedInvokeinterfaceImplementsTestMethod.getOffset());
          asm.emitMTCTR(T0);
          asm.emitLVAL(T0, methodRef.getId());            // id of method reference we are trying to call
          peekAddr(T1, count - 1);           // the "this" object
          asm.emitBCCTRL();                  // throw exception, if link error
        } else {
          RVMClass interfaceClass = resolvedMethod.getDeclaringClass();
          int interfaceIndex = interfaceClass.getDoesImplementIndex();
          int interfaceMask = interfaceClass.getDoesImplementBitMask();

          peekAddr(T0, count - 1);                              // the "this" object
          asm.baselineEmitLoadTIB(T0, T0);         // TIB of "this" object
          asm.emitLAddr(T0, TIB_DOES_IMPLEMENT_INDEX << LOG_BYTES_IN_ADDRESS, T0); // implements bit vector

          if (DynamicTypeCheck.MIN_DOES_IMPLEMENT_SIZE <= interfaceIndex) {
            // must do arraybounds check of implements bit vector
            asm.emitLIntOffset(T1, T0, ObjectModel.getArrayLengthOffset()); // T1 gets array length
            asm.emitLVAL(T2, interfaceIndex);
            asm.emitCMPL(T2, T1);
            ForwardReference fr1 = asm.emitForwardBC(LT);  // if in bounds, jump around trap.  TODO: would like to encode "y" bit that this branch is expected to be takem.
            asm.emitTWI(31, GPR.R12, MUST_IMPLEMENT_TRAP); // encoding of TRAP_ALWAYS MUST_IMPLEMENT_INTERFACE
            fr1.resolve(asm);
          }

          // Test the appropriate bit and if set, branch around another trap imm
          asm.emitLInt(T1, interfaceIndex << LOG_BYTES_IN_INT, T0);
          if ((interfaceMask & 0xffff) == interfaceMask) {
            asm.emitANDI(S0, T1, interfaceMask);
          } else {
            if (VM.VerifyAssertions) VM._assert((interfaceMask & 0xffff0000) == interfaceMask);
            asm.emitANDIS(S0, T1, interfaceMask);
          }
          ForwardReference fr2 = asm.emitForwardBC(NE);     // TODO: encode "y" bit that branch is likely taken.
          asm.emitTWI(31, GPR.R12, MUST_IMPLEMENT_TRAP); // encoding of TRAP_ALWAYS MUST_IMPLEMENT_INTERFACE
          fr2.resolve(asm);
        }
      }
    }
    // (2) Emit interface invocation sequence.
    if (VM.BuildForIMTInterfaceInvocation) {
      InterfaceMethodSignature sig = InterfaceMethodSignature.findOrCreate(methodRef);
      genMoveParametersToRegisters(true, methodRef); // T0 is "this"
      asm.baselineEmitLoadTIB(S0, T0);
      asm.emitLAddr(S0, TIB_INTERFACE_DISPATCH_TABLE_INDEX << LOG_BYTES_IN_ADDRESS, S0); // Load the IMT base into S0
      asm.emitLAddrOffset(S0, S0, sig.getIMTOffset());                  // the method address
      asm.emitMTCTR(S0);
      asm.emitLVAL(S1, sig.getId());      // pass "hidden" parameter in S1 scratch  register
      asm.emitBCCTRL();
    } else {
      int itableIndex = -1;
      if (VM.BuildForITableInterfaceInvocation && resolvedMethod != null) {
        // get the index of the method in the Itable
        itableIndex =
            InterfaceInvocation.getITableIndex(resolvedMethod.getDeclaringClass(),
                                                  methodRef.getName(),
                                                  methodRef.getDescriptor());
      }
      if (itableIndex == -1) {
        // itable index is not known at compile-time.
        // call "invokeInterface" to resolve object + method id into method address
        int methodRefId = methodRef.getId();
        asm.emitLAddrToc(T0, Entrypoints.invokeInterfaceMethod.getOffset());
        asm.emitMTCTR(T0);
        peekAddr(T0, count - 1); // object
        asm.emitLVAL(T1, methodRefId);        // method id
        asm.emitBCCTRL();       // T0 := resolved method address
        asm.emitMTCTR(T0);
        genMoveParametersToRegisters(true, methodRef);
        asm.emitBCCTRL();
      } else {
        // itable index is known at compile-time.
        // call "findITable" to resolve object + interface id into
        // itable address
        asm.emitLAddrToc(T0, Entrypoints.findItableMethod.getOffset());
        asm.emitMTCTR(T0);
        peekAddr(T0, count - 1);     // object
        asm.baselineEmitLoadTIB(T0, T0);
        asm.emitLVAL(T1, resolvedMethod.getDeclaringClass().getInterfaceId());    // interface id
        asm.emitBCCTRL();   // T0 := itable reference
        asm.emitLAddr(T0, itableIndex << LOG_BYTES_IN_ADDRESS, T0); // T0 := the method to call
        asm.emitMTCTR(T0);
        genMoveParametersToRegisters(true, methodRef);        //T0 is "this"
        asm.emitBCCTRL();
      }
    }
    genPopParametersAndPushReturnValue(true, methodRef);
  }

  /*
   * other object model functions
   */

  @Override
  protected void emit_resolved_new(RVMClass typeRef) {
    int instanceSize = typeRef.getInstanceSize();
    Offset tibOffset = typeRef.getTibOffset();
    int whichAllocator = MemoryManager.pickAllocator(typeRef, method);
    int align = ObjectModel.getAlignment(typeRef);
    int offset = ObjectModel.getOffsetForAlignment(typeRef, false);
    int site = MemoryManager.getAllocationSite(true);
    asm.emitLAddrToc(T0, Entrypoints.resolvedNewScalarMethod.getOffset());
    asm.emitMTCTR(T0);
    asm.emitLVAL(T0, instanceSize);
    asm.emitLAddrToc(T1, tibOffset);
    asm.emitLVAL(T2, typeRef.hasFinalizer() ? 1 : 0);
    asm.emitLVAL(T3, whichAllocator);
    asm.emitLVAL(T4, align);
    asm.emitLVAL(T5, offset);
    asm.emitLVAL(T6, site);
    asm.emitBCCTRL();
    pushAddr(T0);
  }

  @Override
  protected void emit_unresolved_new(TypeReference typeRef) {
    int site = MemoryManager.getAllocationSite(true);
    asm.emitLAddrToc(T0, Entrypoints.unresolvedNewScalarMethod.getOffset());
    asm.emitMTCTR(T0);
    asm.emitLVAL(T0, typeRef.getId());
    asm.emitLVAL(T1, site);
    asm.emitBCCTRL();
    pushAddr(T0);
  }

  @Override
  protected void emit_resolved_newarray(RVMArray array) {
    int width = array.getLogElementSize();
    Offset tibOffset = array.getTibOffset();
    int headerSize = ObjectModel.computeArrayHeaderSize(array);
    int whichAllocator = MemoryManager.pickAllocator(array, method);
    int site = MemoryManager.getAllocationSite(true);
    int align = ObjectModel.getAlignment(array);
    int offset = ObjectModel.getOffsetForAlignment(array, false);
    asm.emitLAddrToc(T0, Entrypoints.resolvedNewArrayMethod.getOffset());
    asm.emitMTCTR(T0);
    peekInt(T0, 0);                    // T0 := number of elements
    asm.emitLVAL(T1, width);         // T1 := log element size
    asm.emitLVAL(T2, headerSize);    // T2 := header bytes
    asm.emitLAddrToc(T3, tibOffset);  // T3 := tib
    asm.emitLVAL(T4, whichAllocator);// T4 := allocator
    asm.emitLVAL(T5, align);
    asm.emitLVAL(T6, offset);
    asm.emitLVAL(T7, site);           // T7 := site
    asm.emitBCCTRL();
    pokeAddr(T0, 0);
  }

  @Override
  protected void emit_unresolved_newarray(TypeReference typeRef) {
    int site = MemoryManager.getAllocationSite(true);
    asm.emitLAddrToc(T0, Entrypoints.unresolvedNewArrayMethod.getOffset());
    asm.emitMTCTR(T0);
    peekInt(T0, 0);                // T0 := number of elements
    asm.emitLVAL(T1, typeRef.getId());      // T1 := id of type ref
    asm.emitLVAL(T2, site);
    asm.emitBCCTRL();
    pokeAddr(T0, 0);
  }

  @Override
  protected void emit_multianewarray(TypeReference typeRef, int dimensions) {
    asm.emitLAddrToc(T0, ArchEntrypoints.newArrayArrayMethod.getOffset());
    asm.emitMTCTR(T0);
    asm.emitLVAL(T0, method.getId());
    asm.emitLVAL(T1, dimensions);
    asm.emitLVAL(T2, typeRef.getId());
    asm.emitSLWI(T3, T1, LOG_BYTES_IN_ADDRESS); // number of bytes of array dimension args
    asm.emitADDI(T3, spTopOffset, T3);             // offset from FP to expression stack top
    asm.emitBCCTRL();
    discardSlots(dimensions);
    pushAddr(T0);
  }

  @Override
  protected void emit_arraylength() {
    popAddr(T0);
    asm.emitLIntOffset(T1, T0, ObjectModel.getArrayLengthOffset());
    pushInt(T1);
  }

  @Override
  protected void emit_athrow() {
    asm.emitLAddrToc(T0, Entrypoints.athrowMethod.getOffset());
    asm.emitMTCTR(T0);
    peekAddr(T0, 0);
    asm.emitBCCTRL();
  }

  @Override
  protected void emit_checkcast(TypeReference typeRef) {
    asm.emitLAddrToc(T0, Entrypoints.checkcastMethod.getOffset());
    asm.emitMTCTR(T0);
    peekAddr(T0, 0); // checkcast(obj, klass) consumes obj
    asm.emitLVAL(T1, typeRef.getId());
    asm.emitBCCTRL();               // but obj remains on stack afterwords
  }

  @Override
  protected void emit_checkcast_resolvedInterface(RVMClass type) {
    int interfaceIndex = type.getDoesImplementIndex();
    int interfaceMask = type.getDoesImplementBitMask();

    peekAddr(T0, 0);            // load the object being checked
    asm.emitCMPAddrI(T0, 0);    // check for null
    ForwardReference isNull = asm.emitForwardBC(EQ);

    asm.baselineEmitLoadTIB(T0, T0);         // TIB of "this" object
    asm.emitLAddr(T0, TIB_DOES_IMPLEMENT_INDEX << LOG_BYTES_IN_ADDRESS, T0); // implements bit vector

    if (DynamicTypeCheck.MIN_DOES_IMPLEMENT_SIZE <= interfaceIndex) {
      // must do arraybounds check of implements bit vector
      asm.emitLIntOffset(T1, T0, ObjectModel.getArrayLengthOffset()); // T1 gets array length
      asm.emitLVAL(T2, interfaceIndex);
      asm.emitCMPL(T2, T1);
      ForwardReference fr1 = asm.emitForwardBC(LT);      // if in bounds, jump around trap.  TODO: would like to encode "y" bit that this branch is expected to be takem.
      asm.emitTWI(31, GPR.R12, CHECKCAST_TRAP); // encoding of TRAP_ALWAYS CHECKCAST
      fr1.resolve(asm);
    }

    // Test the appropriate bit and if set, branch around another trap imm
    asm.emitLInt(T1, interfaceIndex << LOG_BYTES_IN_INT, T0);
    if ((interfaceMask & 0xffff) == interfaceMask) {
      asm.emitANDI(S0, T1, interfaceMask);
    } else {
      if (VM.VerifyAssertions) VM._assert((interfaceMask & 0xffff0000) == interfaceMask);
      asm.emitANDIS(S0, T1, interfaceMask);
    }
    ForwardReference fr2 = asm.emitForwardBC(NE);      // TODO: encode "y" bit that branch is likely taken.
    asm.emitTWI(31, GPR.R12, CHECKCAST_TRAP); // encoding of TRAP_ALWAYS CHECKCAST
    fr2.resolve(asm);
    isNull.resolve(asm);
  }

  @Override
  protected void emit_checkcast_resolvedClass(RVMClass type) {
    int LHSDepth = type.getTypeDepth();
    int LHSId = type.getId();

    peekAddr(T0, 0);            // load the object being checked
    asm.emitCMPAddrI(T0, 0);    // check for null
    ForwardReference isNull = asm.emitForwardBC(EQ);

    asm.baselineEmitLoadTIB(T0, T0);       // TIB of "this" object
    asm.emitLAddr(T0, TIB_SUPERCLASS_IDS_INDEX << LOG_BYTES_IN_ADDRESS, T0); // superclass display
    if (DynamicTypeCheck.MIN_SUPERCLASS_IDS_SIZE <= LHSDepth) {
      // must do arraybounds check of superclass display
      asm.emitLIntOffset(T1, T0, ObjectModel.getArrayLengthOffset()); // T1 gets array length
      asm.emitLVAL(T2, LHSDepth);
      asm.emitCMPL(T2, T1);
      ForwardReference fr1 = asm.emitForwardBC(LT);      // if in bounds, jump around trap.  TODO: would like to encode "y" bit that this branch is expected to be takem.
      asm.emitTWI(31, GPR.R12, CHECKCAST_TRAP); // encoding of TRAP_ALWAYS CHECKCAST
      fr1.resolve(asm);
    }

    // Load id from display at required depth and compare against target id.
    asm.emitLHZ(T0, LHSDepth << LOG_BYTES_IN_CHAR, T0);
    if (Assembler.fits(LHSId, 16)) {
      asm.emitCMPI(T0, LHSId);
    } else {
      asm.emitLVAL(T1, LHSId);
      asm.emitCMP(T0, T1);
    }
    ForwardReference fr2 = asm.emitForwardBC(EQ);      // TODO: encode "y" bit that branch is likely taken.
    asm.emitTWI(31, GPR.R12, CHECKCAST_TRAP); // encoding of TRAP_ALWAYS CHECKCAST
    fr2.resolve(asm);
    isNull.resolve(asm);
  }

  @Override
  protected void emit_checkcast_final(RVMType type) {
    peekAddr(T0, 0);            // load the object being checked
    asm.emitCMPAddrI(T0, 0);    // check for null
    ForwardReference isNull = asm.emitForwardBC(EQ);

    asm.baselineEmitLoadTIB(T0, T0);       // TIB of "this" object
    asm.emitLAddrToc(T1, type.getTibOffset());          // TIB of LHS type
    asm.emitCMP(T0, T1);                                // TIBs equal?
    ForwardReference fr = asm.emitForwardBC(EQ);       // TODO: encode "y" bit that branch is likely taken.
    asm.emitTWI(31, GPR.R12, CHECKCAST_TRAP); // encoding of TRAP_ALWAYS CHECKCAST
    fr.resolve(asm);
    isNull.resolve(asm);
  }

  @Override
  protected void emit_instanceof(TypeReference typeRef) {
    asm.emitLAddrToc(T0, Entrypoints.instanceOfMethod.getOffset());
    asm.emitMTCTR(T0);
    peekAddr(T0, 0);
    asm.emitLVAL(T1, typeRef.getId());
    asm.emitBCCTRL();
    pokeInt(T0, 0);
  }

  @Override
  protected void emit_instanceof_resolvedInterface(RVMClass type) {
    int interfaceIndex = type.getDoesImplementIndex();
    int interfaceMask = type.getDoesImplementBitMask();

    // load object from stack and check for null
    popAddr(T0);
    asm.emitCMPAddrI(T0, 0);
    ForwardReference isNull = asm.emitForwardBC(EQ);

    // get implements bit vector from object's TIB
    asm.baselineEmitLoadTIB(T0, T0);
    asm.emitLAddr(T0, TIB_DOES_IMPLEMENT_INDEX << LOG_BYTES_IN_ADDRESS, T0);

    ForwardReference outOfBounds = null;
    if (DynamicTypeCheck.MIN_DOES_IMPLEMENT_SIZE <= interfaceIndex) {
      // must do arraybounds check of implements bit vector
      asm.emitLIntOffset(T1, T0, ObjectModel.getArrayLengthOffset()); // T1 gets array length
      asm.emitLVAL(T2, interfaceIndex);
      asm.emitCMPL(T1, T2);
      outOfBounds = asm.emitForwardBC(LE);
    }

    // Test the appropriate bit and if set, set T0 to true (1)
    asm.emitLInt(T1, interfaceIndex << LOG_BYTES_IN_INT, T0);
    if ((interfaceMask & 0xffff) == interfaceMask) {
      asm.emitANDI(S0, T1, interfaceMask);
    } else {
      if (VM.VerifyAssertions) VM._assert((interfaceMask & 0xffff0000) == interfaceMask);
      asm.emitANDIS(S0, T1, interfaceMask);
    }

    ForwardReference notMatched = asm.emitForwardBC(EQ);
    asm.emitLVAL(T0, 1);
    ForwardReference done = asm.emitForwardB();

    // set T1 to 0 (false)
    isNull.resolve(asm);
    if (outOfBounds != null) outOfBounds.resolve(asm);
    notMatched.resolve(asm);
    asm.emitLVAL(T0, 0);

    // push T0, containing the result of the instanceof comparision, to the stack.
    done.resolve(asm);
    pushInt(T0);
  }

  @Override
  protected void emit_instanceof_resolvedClass(RVMClass type) {
    int LHSDepth = type.getTypeDepth();
    int LHSId = type.getId();

    // load object from stack and check for null
    popAddr(T0);
    asm.emitCMPAddrI(T0, 0);
    ForwardReference isNull = asm.emitForwardBC(EQ);

    // get superclass display from object's TIB
    asm.baselineEmitLoadTIB(T0, T0);
    asm.emitLAddr(T0, TIB_SUPERCLASS_IDS_INDEX << LOG_BYTES_IN_ADDRESS, T0);

    ForwardReference outOfBounds = null;
    if (DynamicTypeCheck.MIN_SUPERCLASS_IDS_SIZE <= LHSDepth) {
      // must do arraybounds check of superclass display
      asm.emitLIntOffset(T1, T0, ObjectModel.getArrayLengthOffset()); // T1 gets array length
      asm.emitLVAL(T2, LHSDepth);
      asm.emitCMPL(T1, T2);
      outOfBounds = asm.emitForwardBC(LE);
    }

    // Load id from display at required depth and compare against target id; set T0 to 1 (true) if matched
    asm.emitLHZ(T0, LHSDepth << LOG_BYTES_IN_CHAR, T0);
    if (Assembler.fits(LHSId, 16)) {
      asm.emitCMPI(T0, LHSId);
    } else {
      asm.emitLVAL(T1, LHSId);
      asm.emitCMP(T0, T1);
    }
    ForwardReference notMatched = asm.emitForwardBC(NE);
    asm.emitLVAL(T0, 1);
    ForwardReference done = asm.emitForwardB();

    // set T0 to 0 (false)
    isNull.resolve(asm);
    if (outOfBounds != null) outOfBounds.resolve(asm);
    notMatched.resolve(asm);
    asm.emitLVAL(T0, 0);

    // push T0, containing the result of the instanceof comparision, to the stack.
    done.resolve(asm);
    pushInt(T0);
  }

  @Override
  protected void emit_instanceof_final(RVMType type) {
    popAddr(T0);                // load object from stack
    asm.emitCMPAddrI(T0, 0);    // check for null
    ForwardReference isNull = asm.emitForwardBC(EQ);

    // compare TIB of object to desired TIB and set T0 to 1 (true) if equal
    asm.baselineEmitLoadTIB(T0, T0);       // TIB of "this" object
    asm.emitLAddrToc(T1, type.getTibOffset());          // TIB of LHS type
    asm.emitCMP(T0, T1);                                // TIBs equal?
    ForwardReference notMatched = asm.emitForwardBC(NE);
    asm.emitLVAL(T0, 1);
    ForwardReference done = asm.emitForwardB();

    // set T1 to 0 (false)
    isNull.resolve(asm);
    notMatched.resolve(asm);
    asm.emitLVAL(T0, 0);

    // push T0, containing the result of the instanceof comparision, to the stack.
    done.resolve(asm);
    pushInt(T0);
  }

  @Override
  protected void emit_monitorenter() {
    peekAddr(T0, 0);
    asm.emitNullCheck(T0);
    asm.emitLAddrOffset(S0, JTOC, Entrypoints.lockMethod.getOffset());
    asm.emitMTCTR(S0);
    asm.emitBCCTRL();
    discardSlot();
  }

  @Override
  protected void emit_monitorexit() {
    peekAddr(T0, 0);
    asm.emitLAddrOffset(S0, JTOC, Entrypoints.unlockMethod.getOffset());
    asm.emitMTCTR(S0);
    asm.emitBCCTRL();
    discardSlot();
  }

  // offset of i-th local variable with respect to FP
  private int localOffset(int i) {
    int offset = startLocalOffset - (i << LOG_BYTES_IN_STACKSLOT);
    if (VM.VerifyAssertions) VM._assert(offset < 0x8000);
    return offset;
  }

  @Uninterruptible
  public static boolean isRegister(short location) {
    return location > 0;
  }

  public static MachineRegister asRegister(byte type, short location) {
    if (type == FLOAT_TYPE || type == DOUBLE_TYPE) {
      return FPR.lookup(location);
    } else {
      return GPR.lookup(location);
    }
  }

  @Uninterruptible
  public static int locationToOffset(short location) {
    return -location;
  }

  @Uninterruptible
  public static short offsetToLocation(int offset) {
    return (short)-offset;
  }

  @Inline
  private void copyRegToReg(byte srcType, MachineRegister src, MachineRegister dest) {
    if ((srcType == FLOAT_TYPE) || (srcType == DOUBLE_TYPE)) {
      asm.emitFMR((FPR)dest, (FPR)src);
    } else {
      asm.emitMR((GPR)dest, (GPR)src);
      if ((VM.BuildFor32Addr) && (srcType == LONG_TYPE)) {
        asm.emitMR(((GPR)dest).nextGPR(), ((GPR)src).nextGPR());
      }
    }
  }

  @Inline
  private void copyRegToMem(byte srcType, MachineRegister src, int dest) {
    if (srcType == FLOAT_TYPE) {
      asm.emitSTFS((FPR)src, dest - BYTES_IN_FLOAT, FP);
    } else if (srcType == DOUBLE_TYPE) {
      asm.emitSTFD((FPR)src, dest - BYTES_IN_DOUBLE, FP);
    } else if (srcType == INT_TYPE) {
      asm.emitSTW((GPR)src, dest - BYTES_IN_INT, FP);
    } else if ((VM.BuildFor32Addr) && (srcType == LONG_TYPE)) {
      asm.emitSTW((GPR)src, dest - BYTES_IN_LONG, FP);
      asm.emitSTW(((GPR)src).nextGPR(), dest - BYTES_IN_LONG + 4, FP);
    } else { //default
      asm.emitSTAddr((GPR)src, dest - BYTES_IN_ADDRESS, FP);
    }
  }

  @Inline
  private void copyMemToReg(byte srcType, int src, MachineRegister dest) {
    if (srcType == FLOAT_TYPE) {
      asm.emitLFS((FPR)dest, src - BYTES_IN_FLOAT, FP);
    } else if (srcType == DOUBLE_TYPE) {
      asm.emitLFD((FPR)dest, src - BYTES_IN_DOUBLE, FP);
    } else if (srcType == INT_TYPE) {
      asm.emitLInt((GPR)dest, src - BYTES_IN_INT, FP); //KV SignExtend!!!
    } else if ((VM.BuildFor32Addr) && (srcType == LONG_TYPE)) {
      asm.emitLWZ((GPR)dest, src - BYTES_IN_LONG, FP);
      asm.emitLWZ(((GPR)dest).nextGPR(), src - BYTES_IN_LONG + 4, FP);
    } else { //default
      asm.emitLAddr((GPR)dest, src - BYTES_IN_ADDRESS, FP);
    }
  }

  @Inline
  private void copyMemToMem(byte srcType, int src, int dest) {
    if (VM.BuildFor64Addr) {
      if ((srcType == FLOAT_TYPE) || (srcType == INT_TYPE)) {
        //32-bit value
        asm.emitLWZ(GPR.R0, src - BYTES_IN_INT, FP);
        asm.emitSTW(GPR.R0, dest - BYTES_IN_INT, FP);
      } else {
        //64-bit value
        asm.emitLAddr(GPR.R0, src - BYTES_IN_ADDRESS, FP);
        asm.emitSTAddr(GPR.R0, dest - BYTES_IN_ADDRESS, FP);
      }
    } else { //BuildFor32Addr
      if ((srcType == DOUBLE_TYPE) || (srcType == LONG_TYPE)) {
        //64-bit value
        asm.emitLFD(FIRST_SCRATCH_FPR, src - BYTES_IN_DOUBLE, FP);
        asm.emitSTFD(FIRST_SCRATCH_FPR, dest - BYTES_IN_DOUBLE, FP);
      } else {
        //32-bit value
        asm.emitLWZ(GPR.R0, src - BYTES_IN_INT, FP);
        asm.emitSTW(GPR.R0, dest - BYTES_IN_INT, FP);
      }
    }
  }

  /**
   * The workhorse routine that is responsible for copying values from
   * one slot to another. Every value is in a <i>location</i> that
   * represents either a numbered register or an offset from the frame
   * pointer (registers are positive numbers and offsets are
   * negative). This method will generate register moves, memory stores,
   * or memory loads as needed to get the value from its source location
   * to its target. This method also understands how to do a few conversions
   * from one type of value to another (for instance float to word).
   *
   * @param srcType the type of the source (e.g. <code>INT_TYPE</code>)
   * @param src the source location
   * @param destType the type of the destination
   * @param dest the destination location
   */
  @Inline
  private void copyByLocation(byte srcType, short src, byte destType, short dest) {
    if (src == dest && srcType == destType) {
      return;
    }

    final boolean srcIsRegister = isRegister(src);
    final boolean destIsRegister = isRegister(dest);

    if (srcType == destType) {
      if (srcIsRegister) {
        if (destIsRegister) {
          // register to register move
          copyRegToReg(srcType, asRegister(srcType,src), asRegister(destType,dest));
        } else {
          // register to memory move
          copyRegToMem(srcType, asRegister(srcType,src), locationToOffset(dest));
        }
      } else {
        if (destIsRegister) {
          // memory to register move
          copyMemToReg(srcType, locationToOffset(src), asRegister(destType,dest));
        } else {
          // memory to memory move
          copyMemToMem(srcType, locationToOffset(src), locationToOffset(dest));
        }
      }

    } else { // no matching types
      if ((srcType == DOUBLE_TYPE) && (destType == LONG_TYPE) && srcIsRegister && !destIsRegister) {
        asm.emitSTFD(FPR.lookup(src), locationToOffset(dest) - BYTES_IN_DOUBLE, FP);
      } else if ((srcType == LONG_TYPE) && (destType == DOUBLE_TYPE) && destIsRegister && !srcIsRegister) {
        asm.emitLFD(FPR.lookup(dest), locationToOffset(src) - BYTES_IN_LONG, FP);
      } else if ((srcType == INT_TYPE) && (destType == LONGHALF_TYPE) && srcIsRegister && VM.BuildFor32Addr) {
        //Used as Hack if 1 half of long is spilled
        if (destIsRegister) {
          asm.emitMR(GPR.lookup(dest), GPR.lookup(src));
        } else {
          asm.emitSTW(GPR.lookup(src), locationToOffset(dest) - BYTES_IN_LONG, FP); // lo mem := lo register (== hi word)
        }
      } else if ((srcType == LONGHALF_TYPE) && (destType == INT_TYPE) && !srcIsRegister && VM.BuildFor32Addr) {
        //Used as Hack if 1 half of long is spilled
        if (destIsRegister) {
          asm.emitLWZ(GPR.lookup(dest).nextGPR(), locationToOffset(src) - BYTES_IN_INT, FP);
        } else {
          asm.emitLWZ(GPR.R0, locationToOffset(src) - BYTES_IN_INT, FP);
          asm.emitSTW(GPR.R0, locationToOffset(dest) - BYTES_IN_INT, FP);
        }
      } else
        // implement me
        if (VM.VerifyAssertions) {
          VM.sysWrite("copyByLocation error. src=");
          VM.sysWrite(src);
          VM.sysWrite(", srcType=");
          VM.sysWrite(srcType);
          VM.sysWrite(", dest=");
          VM.sysWrite(dest);
          VM.sysWrite(", destType=");
          VM.sysWrite(destType);
          VM.sysWriteln();
          VM._assert(NOT_REACHED);
        }
    }
  }

  private void emitDynamicLinkingSequence(GPR reg, MemberReference ref, boolean couldBeZero) {
    int memberId = ref.getId();
    Offset memberOffset = Offset.fromIntZeroExtend(memberId << LOG_BYTES_IN_INT);
    Offset tableOffset = Entrypoints.memberOffsetsField.getOffset();
    if (couldBeZero) {
      Offset resolverOffset = Entrypoints.resolveMemberMethod.getOffset();
      int label = asm.getMachineCodeIndex();

      // load offset table
      asm.emitLAddrToc(reg, tableOffset);
      asm.emitLIntOffset(reg, reg, memberOffset);

      // test for non-zero offset and branch around call to resolver
      asm.emitCMPI(reg, NEEDS_DYNAMIC_LINK);         // reg ?= NEEDS_DYNAMIC_LINK, is field's class loaded?
      ForwardReference fr1 = asm.emitForwardBC(NE);
      asm.emitLAddrToc(T0, resolverOffset);
      asm.emitMTCTR(T0);
      asm.emitLVAL(T0, memberId);            // id of member we are resolving
      asm.emitBCCTRL();                              // link; will throw exception if link error
      asm.emitB(label);                   // go back and try again
      fr1.resolve(asm);
    } else {
      // load offset table
      asm.emitLAddrToc(reg, tableOffset);
      asm.emitLIntOffset(reg, reg, memberOffset);
    }
  }

  // Gen bounds check for array load/store bytecodes.
  // Does implicit null check and array bounds check.
  // Bounds check can always be implicit becuase array length is at negative offset from obj ptr.
  // Kills S0.
  // on return: T0 => base, T1 => index.
  private void genBoundsCheck() {
    popInt(T1);      // T1 is array index
    popAddr(T0);     // T0 is array ref
    asm.emitLIntOffset(S0, T0, ObjectModel.getArrayLengthOffset());  // T2 is array length
    asm.emitTWLLE(S0, T1);      // trap if index < 0 or index >= length
  }

  // Gen bounds check for array load/store bytecodes.
  // Does implicit null check and array bounds check.
  // Bounds check can always be implicit becuase array length is at negative offset from obj ptr.
  // Kills S0.
  private void genBoundsCheck(int arrayIndexSlot, int arrayRefSlot) {
    peekInt(T1, arrayIndexSlot);
    peekAddr(T0, arrayRefSlot);
    asm.emitLIntOffset(S0, T0, ObjectModel.getArrayLengthOffset());
    asm.emitTWLLE(S0, T1); // trap if index < 0 or index >= length
  }

  // Emit code to buy a stackframe, store incoming parameters,
  // and acquire method synchronization lock.
  //
  private void genPrologue() {
    if (klass.hasBridgeFromNativeAnnotation()) {
      JNICompiler.generateGlueCodeForJNIMethod(asm, method);
    }

    // Generate trap if new frame would cross guard page.
    //
    if (isInterruptible) {
      asm.emitStackOverflowCheck(frameSize);                            // clobbers R0, S0
    }

    // Buy frame.
    //
    asm.emitSTAddrU(FP,
                    -frameSize,
                    FP); // save old FP & buy new frame (trap if new frame below guard page) !!TODO: handle frames larger than 32k when addressing local variables, etc.

    // If this is a "dynamic bridge" method, then save all registers except GPR0, FPR0, JTOC, and FP.
    //
    if (klass.hasDynamicBridgeAnnotation()) {
      int offset = frameSize;
      for (int i = LAST_NONVOLATILE_FPR.value(); i >= FIRST_VOLATILE_FPR.value(); --i) {
        asm.emitSTFD(FPR.lookup(i), offset -= BYTES_IN_DOUBLE, FP);
      }
      for (int i = LAST_NONVOLATILE_GPR.value(); i >= FIRST_VOLATILE_GPR.value(); --i) {
        asm.emitSTAddr(GPR.lookup(i), offset -= BYTES_IN_ADDRESS, FP);
      }

      // round up first, save scratch FPRs
      offset = Memory.alignDown(offset - STACKFRAME_ALIGNMENT + 1, STACKFRAME_ALIGNMENT);

      for (int i = LAST_SCRATCH_FPR.value(); i >= FIRST_SCRATCH_FPR.value(); --i) {
        asm.emitSTFD(FPR.lookup(i), offset -= BYTES_IN_DOUBLE, FP);
      }
      for (int i = LAST_SCRATCH_GPR.value(); i >= FIRST_SCRATCH_GPR.value(); --i) {
        asm.emitSTAddr(GPR.lookup(i), offset -= BYTES_IN_ADDRESS, FP);
      }
    } else {
      // save non-volatile registers.
      int offset = frameSize;
      for (int i = lastFloatStackRegister; i >= FIRST_FLOAT_LOCAL_REGISTER.value(); --i) {
        asm.emitSTFD(FPR.lookup(i), offset -= BYTES_IN_DOUBLE, FP);
      }
      for (int i = lastFixedStackRegister; i >= FIRST_FIXED_LOCAL_REGISTER.value(); --i) {
        asm.emitSTAddr(GPR.lookup(i), offset -= BYTES_IN_ADDRESS, FP);
      }
    }

    // Fill in frame header.
    //
    asm.emitLVAL(S0, compiledMethod.getId());
    asm.emitMFLR(GPR.R0);
    asm.emitSTW(S0, STACKFRAME_METHOD_ID_OFFSET.toInt(), FP);                   // save compiled method id
    asm.emitSTAddr(GPR.R0,
                   frameSize + STACKFRAME_RETURN_ADDRESS_OFFSET.toInt(),
                   FP); // save LR !!TODO: handle discontiguous stacks when saving return address

    // Setup locals.
    //
    genMoveParametersToLocals();                  // move parameters to locals

    // Perform a thread switch if so requested.
    /* defer generating prologues which may trigger GC, see emit_deferred_prologue*/
    if (method.isForOsrSpecialization()) {
      return;
    }

    genThreadSwitchTest(RVMThread.PROLOGUE); //           (BaselineExceptionDeliverer WONT release the lock (for synchronized methods) during prologue code)

    // Acquire method syncronization lock.  (BaselineExceptionDeliverer will release the lock (for synchronized methods) after  prologue code)
    //
    if (method.isSynchronized()) {
      genSynchronizedMethodPrologue();
    }
  }

  @Override
  protected void emit_deferred_prologue() {
    if (VM.VerifyAssertions) VM._assert(method.isForOsrSpecialization());
    genThreadSwitchTest(RVMThread.PROLOGUE);

    /* donot generate sync for synced method because we are reenter
     * the method in the middle.
     */
    //  if (method.isSymchronized()) genSynchronizedMethodPrologue();
  }

  // Emit code to acquire method synchronization lock.
  //
  private void genSynchronizedMethodPrologue() {
    if (method.isStatic()) { // put java.lang.Class object into T0
      Offset klassOffset = Offset.fromIntSignExtend(Statics.findOrCreateObjectLiteral(klass.getClassForType()));
      asm.emitLAddrToc(T0, klassOffset);
    } else { // first local is "this" pointer
      copyByLocation(ADDRESS_TYPE, getGeneralLocalLocation(0), ADDRESS_TYPE, T0.value());
    }
    asm.emitLAddrOffset(S0, JTOC, Entrypoints.lockMethod.getOffset()); // call out...
    asm.emitMTCTR(S0);                                  // ...of line lock
    asm.emitBCCTRL();
    lockOffset = BYTES_IN_INT * (asm.getMachineCodeIndex() - 1); // after this instruction, the method has the monitor
  }

  // Emit code to release method synchronization lock.
  //
  private void genSynchronizedMethodEpilogue() {
    if (method.isStatic()) { // put java.lang.Class for RVMType into T0
      Offset klassOffset = Offset.fromIntSignExtend(Statics.findOrCreateObjectLiteral(klass.getClassForType()));
      asm.emitLAddrToc(T0, klassOffset);
    } else { // first local is "this" pointer
      copyByLocation(ADDRESS_TYPE, getGeneralLocalLocation(0), ADDRESS_TYPE, T0.value());
    }
    asm.emitLAddrOffset(S0, JTOC, Entrypoints.unlockMethod.getOffset());  // call out...
    asm.emitMTCTR(S0);                                     // ...of line lock
    asm.emitBCCTRL();
  }

  // Emit code to discard stackframe and return to caller.
  //
  private void genEpilogue() {
    if (klass.hasDynamicBridgeAnnotation()) { // Restore non-volatile registers.
      // we never return from a DynamicBridge frame
      asm.emitTAddrWI(-1);
    } else {
      // Restore non-volatile registers.
      int offset = frameSize;
      for (int i = lastFloatStackRegister; i >= FIRST_FLOAT_LOCAL_REGISTER.value(); --i) {
        asm.emitLFD(FPR.lookup(i), offset -= BYTES_IN_DOUBLE, FP);
      }
      for (int i = lastFixedStackRegister; i >= FIRST_FIXED_LOCAL_REGISTER.value(); --i) {
        asm.emitLAddr(GPR.lookup(i), offset -= BYTES_IN_ADDRESS, FP);
      }

      if (frameSize <= 0x8000) {
        asm.emitADDI(FP, frameSize, FP); // discard current frame
      } else {
        asm.emitLAddr(FP, 0, FP);           // discard current frame
      }
      asm.emitLAddr(S0, STACKFRAME_RETURN_ADDRESS_OFFSET.toInt(), FP);
      asm.emitMTLR(S0);
      asm.emitBCLR(); // branch always, through link register
    }
  }

  @Override
  protected void ending_method() {
    asm.noteEndOfBytecodes();
  }

  /**
   * Emit the code to load the counter array into the given register.
   * May call a read barrier so will kill all temporaries.
   *
   * @param reg The register to hold the counter array.
   */
  private void loadCounterArray(GPR reg) {
    if (NEEDS_OBJECT_ALOAD_BARRIER) {
      asm.emitLAddrToc(T0, Entrypoints.edgeCountersField.getOffset());
      asm.emitLVAL(T1, getEdgeCounterIndex());
      Barriers.compileArrayLoadBarrier(this);
      if (reg != T0) {
        asm.emitORI(reg, T0, 0);
      }
    } else {
      // Load counter array for this method
      asm.emitLAddrToc(reg, Entrypoints.edgeCountersField.getOffset());
      asm.emitLAddrOffset(reg, reg, getEdgeCounterOffset());
    }
  }

  /**
   * Emit the code for a bytecode level conditional branch
   * @param cc the condition code to branch on
   * @param bTarget the target bytecode index
   */
  private void genCondBranch(int cc, int bTarget) {
    if (options.PROFILE_EDGE_COUNTERS) {
      // Allocate 2 counters, taken and not taken
      int entry = edgeCounterIdx;
      edgeCounterIdx += 2;

      // Load counter array for this method
      loadCounterArray(T0);

      // Flip conditions so we can jump over the increment of the taken counter.
      ForwardReference fr = asm.emitForwardBC(Assembler.flipCode(cc));

      // Increment taken counter & jump to target
      incEdgeCounter(T0, T1, T2, entry + EdgeCounts.TAKEN);
      asm.emitB(bytecodeMap[bTarget], bTarget);

      // Not taken
      fr.resolve(asm);
      incEdgeCounter(T0, T1, T2, entry + EdgeCounts.NOT_TAKEN);
    } else {
      if (bTarget - SHORT_FORWARD_LIMIT < biStart) {
        asm.emitShortBC(cc, bytecodeMap[bTarget], bTarget);
      } else {
        asm.emitBC(cc, bytecodeMap[bTarget], bTarget);
      }
    }
  }

  /**
   * increment an edge counter.
   * @param counters register containing base of counter array
   * @param scratch scratch register
   * @param scratchForXER scratch register that will hold contents of XER
   * @param counterIdx index of counter to increment
   */
  private void incEdgeCounter(GPR counters, GPR scratch, GPR scratchForXER, int counterIdx) {
    asm.emitLInt(scratch, counterIdx << 2, counters);
    emitEdgeCounterIncrease(scratch, scratchForXER);
    asm.emitSTW(scratch, counterIdx << 2, counters);
  }

  private void incEdgeCounterIdx(GPR counters, GPR scratch, GPR scratchForXER, int base, GPR counterIdx) {
    asm.emitADDI(counters, base << 2, counters);
    asm.emitLIntX(scratch, counterIdx, counters);
    emitEdgeCounterIncrease(scratch, scratchForXER);
    asm.emitSTWX(scratch, counterIdx, counters);
  }

  private void emitEdgeCounterIncrease(GPR scratch, GPR scratchForXER) {
    asm.emitADDICr(scratch, scratch, 1);
    // spr for XER is 00000 00001 which is reversed to 00001 00000
    final int sprForXER = 1 << 5;
    // Move XER into scratch
    asm.emitMFSPR(scratchForXER, sprForXER);
    // XOR it to invert carry, carry is in position 34 in XER
    asm.emitXORIS(scratchForXER, scratchForXER, 1 << 13);
    // Move adjusted XER back
    asm.emitMTSPR(scratchForXER, sprForXER);
    // Carry is 0 if value overflowed and 1 otherwise, so
    // 1 will be subtracted if the value overflowed, otherwise
    // it will be unchanged
    asm.emitADDME(scratch, scratch);
  }

  /**
   * @param whereFrom is this thread switch from a PROLOGUE, BACKEDGE, or EPILOGUE?
   */
  private void genThreadSwitchTest(int whereFrom) {
    if (isInterruptible) {
      ForwardReference fr;
      // yield if takeYieldpoint is non-zero.
      asm.emitLIntOffset(S0, THREAD_REGISTER, Entrypoints.takeYieldpointField.getOffset());
      asm.emitCMPI(S0, 0);
      if (whereFrom == RVMThread.PROLOGUE) {
        // Take yieldpoint if yieldpoint flag is non-zero (either 1 or -1)
        fr = asm.emitForwardBC(EQ);
        asm.emitLAddrToc(S0, Entrypoints.yieldpointFromPrologueMethod.getOffset());
      } else if (whereFrom == RVMThread.BACKEDGE) {
        // Take yieldpoint if yieldpoint flag is >0
        fr = asm.emitForwardBC(LE);
        asm.emitLAddrToc(S0, Entrypoints.yieldpointFromBackedgeMethod.getOffset());
      } else { // EPILOGUE
        // Take yieldpoint if yieldpoint flag is non-zero (either 1 or -1)
        fr = asm.emitForwardBC(EQ);
        asm.emitLAddrToc(S0, Entrypoints.yieldpointFromEpilogueMethod.getOffset());
      }
      asm.emitMTCTR(S0);
      asm.emitBCCTRL();
      fr.resolve(asm);

      if (VM.BuildForAdaptiveSystem && options.INVOCATION_COUNTERS) {
        int id = compiledMethod.getId();
        InvocationCounts.allocateCounter(id);
        asm.emitLAddrToc(T0, AosEntrypoints.invocationCountsField.getOffset());
        asm.emitLVAL(T1, compiledMethod.getId() << LOG_BYTES_IN_INT);
        asm.emitLIntX(T2, T0, T1);
        asm.emitADDICr(T2, T2, -1);
        asm.emitSTWX(T2, T0, T1);
        ForwardReference fr2 = asm.emitForwardBC(GT);
        asm.emitLAddrToc(T0, AosEntrypoints.invocationCounterTrippedMethod.getOffset());
        asm.emitMTCTR(T0);
        asm.emitLVAL(T0, id);
        asm.emitBCCTRL();
        fr2.resolve(asm);
      }
    }
  }

  // parameter stuff //

  // store parameters from registers into local variables of current method.

  private void genMoveParametersToLocals() {
    int spillOff = frameSize + STACKFRAME_HEADER_SIZE;
    short gp = FIRST_VOLATILE_GPR.value();
    short fp = FIRST_VOLATILE_FPR.value();

    int localIndex = 0;
    short srcLocation;
    short dstLocation;
    byte type;

    if (!method.isStatic()) {
      if (gp > LAST_VOLATILE_GPR.value()) {
        spillOff += BYTES_IN_STACKSLOT;
        srcLocation = offsetToLocation(spillOff);
      } else {
        srcLocation = gp++;
      }
      type = ADDRESS_TYPE;
      dstLocation = getGeneralLocalLocation(localIndex++);
      copyByLocation(type, srcLocation, type, dstLocation);
    }

    TypeReference[] types = method.getParameterTypes();
    for (int i = 0; i < types.length; i++, localIndex++) {
      TypeReference t = types[i];
      if (t.isLongType()) {
        type = LONG_TYPE;
        dstLocation = getGeneralLocalLocation(localIndex++);
        if (gp > LAST_VOLATILE_GPR.value()) {
          spillOff += (VM.BuildFor64Addr ? BYTES_IN_STACKSLOT : 2 * BYTES_IN_STACKSLOT);
          srcLocation = offsetToLocation(spillOff);
          copyByLocation(type, srcLocation, type, dstLocation);
        } else {
          srcLocation = gp++;
          if (VM.BuildFor32Addr) {
            gp++;
            if (srcLocation == LAST_VOLATILE_GPR.value()) {
              copyByLocation(INT_TYPE, srcLocation, LONGHALF_TYPE, dstLocation); //low memory, low reg
              spillOff += BYTES_IN_STACKSLOT;
              copyByLocation(LONGHALF_TYPE, offsetToLocation(spillOff), INT_TYPE, dstLocation); //high mem, high reg
              continue;
            }
          }
          copyByLocation(type, srcLocation, type, dstLocation);
        }
      } else if (t.isFloatType()) {
        type = FLOAT_TYPE;
        dstLocation = getFloatLocalLocation(localIndex);
        if (fp > LAST_VOLATILE_FPR.value()) {
          spillOff += BYTES_IN_STACKSLOT;
          srcLocation = offsetToLocation(spillOff);
        } else {
          srcLocation = fp++;
        }
        copyByLocation(type, srcLocation, type, dstLocation);
      } else if (t.isDoubleType()) {
        type = DOUBLE_TYPE;
        dstLocation = getFloatLocalLocation(localIndex++);
        if (fp > LAST_VOLATILE_FPR.value()) {
          spillOff += (VM.BuildFor64Addr ? BYTES_IN_STACKSLOT : 2 * BYTES_IN_STACKSLOT);
          srcLocation = offsetToLocation(spillOff);
        } else {
          srcLocation = fp++;
        }
        copyByLocation(type, srcLocation, type, dstLocation);
      } else if (t.isIntLikeType()) {
        type = INT_TYPE;
        dstLocation = getGeneralLocalLocation(localIndex);
        if (gp > LAST_VOLATILE_GPR.value()) {
          spillOff += BYTES_IN_STACKSLOT;
          srcLocation = offsetToLocation(spillOff);
        } else {
          srcLocation = gp++;
        }
        copyByLocation(type, srcLocation, type, dstLocation);
      } else { // t is object
        type = ADDRESS_TYPE;
        dstLocation = getGeneralLocalLocation(localIndex);
        if (gp > LAST_VOLATILE_GPR.value()) {
          spillOff += BYTES_IN_STACKSLOT;
          srcLocation = offsetToLocation(spillOff);
        } else {
          srcLocation = gp++;
        }
        copyByLocation(type, srcLocation, type, dstLocation);
      }
    }
  }

  // load parameters into registers before calling method "m".
  private void genMoveParametersToRegisters(boolean hasImplicitThisArg, MethodReference m) {
    spillOffset = STACKFRAME_HEADER_SIZE;
    int gp = FIRST_VOLATILE_GPR.value();
    int fp = FIRST_VOLATILE_FPR.value();
    int stackIndex = m.getParameterWords();
    if (hasImplicitThisArg) {
      if (gp > LAST_VOLATILE_GPR.value()) {
        genSpillSlot(stackIndex);
      } else {
        peekAddr(GPR.lookup(gp++), stackIndex);
      }
    }
    for (TypeReference t : m.getParameterTypes()) {
      if (t.isLongType()) {
        stackIndex -= 2;
        if (gp > LAST_VOLATILE_GPR.value()) {
          genSpillDoubleSlot(stackIndex);
        } else {
          if (VM.BuildFor64Addr) {
            peekLong(GPR.lookup(gp), GPR.lookup(gp), stackIndex);
            gp++;
          } else {
            peekInt(GPR.lookup(gp++), stackIndex);       // lo register := lo mem (== hi order word)
            if (gp > LAST_VOLATILE_GPR.value()) {
              genSpillSlot(stackIndex + 1);
            } else {
              peekInt(GPR.lookup(gp++), stackIndex + 1);  // hi register := hi mem (== lo order word)
            }
          }
        }
      } else if (t.isFloatType()) {
        stackIndex -= 1;
        if (fp > LAST_VOLATILE_FPR.value()) {
          genSpillSlot(stackIndex);
        } else {
          peekFloat(FPR.lookup(fp++), stackIndex);
        }
      } else if (t.isDoubleType()) {
        stackIndex -= 2;
        if (fp > LAST_VOLATILE_FPR.value()) {
          genSpillDoubleSlot(stackIndex);
        } else {
          peekDouble(FPR.lookup(fp++), stackIndex);
        }
      } else if (t.isIntLikeType()) {
        stackIndex -= 1;
        if (gp > LAST_VOLATILE_GPR.value()) {
          genSpillSlot(stackIndex);
        } else {
          peekInt(GPR.lookup(gp++), stackIndex);
        }
      } else { // t is object
        stackIndex -= 1;
        if (gp > LAST_VOLATILE_GPR.value()) {
          genSpillSlot(stackIndex);
        } else {
          peekAddr(GPR.lookup(gp++), stackIndex);
        }
      }
    }
    if (VM.VerifyAssertions) VM._assert(stackIndex == 0);
  }

  // push return value of method "m" from register to operand stack.
  private void genPopParametersAndPushReturnValue(boolean hasImplicitThisArg, MethodReference m) {
    TypeReference t = m.getReturnType();
    discardSlots(m.getParameterWords() + (hasImplicitThisArg ? 1 : 0));
    if (!t.isVoidType()) {
      if (t.isLongType()) {
        pushLong(FIRST_VOLATILE_GPR, VM.BuildFor64Addr ? FIRST_VOLATILE_GPR : (FIRST_VOLATILE_GPR.nextGPR()));
      } else if (t.isFloatType()) {
        pushFloat(FIRST_VOLATILE_FPR);
      } else if (t.isDoubleType()) {
        pushDouble(FIRST_VOLATILE_FPR);
      } else if (t.isIntLikeType()) {
        pushInt(FIRST_VOLATILE_GPR);
      } else { // t is object
        pushAddr(FIRST_VOLATILE_GPR);
      }
    }
  }

  private void genSpillSlot(int stackIndex) {
    peekAddr(GPR.R0, stackIndex);
    asm.emitSTAddr(GPR.R0, spillOffset, FP);
    spillOffset += BYTES_IN_STACKSLOT;
  }

  private void genSpillDoubleSlot(int stackIndex) {
    peekDouble(FPR.FR0, stackIndex);
    asm.emitSTFD(FPR.FR0, spillOffset, FP);
    if (VM.BuildFor64Addr) {
      spillOffset += BYTES_IN_STACKSLOT;
    } else {
      spillOffset += 2 * BYTES_IN_STACKSLOT;
    }
  }

  @Override
  protected void emit_loadretaddrconst(int bcIndex) {
    asm.emitBL(1, 0);
    asm.emitMFLR(T1);                   // LR +  0
    asm.registerLoadReturnAddress(bcIndex);
    asm.emitADDI(T1, bcIndex << LOG_BYTES_IN_INT, T1);
    pushAddr(T1);   // LR +  8
  }

  /**
   * Emit code to invoke a compiled method (with known jtoc offset).
   * Treat it like a resolved invoke static, but take care of
   * this object in the case.<p>
   *
   * I havenot thought about GCMaps for invoke_compiledmethod
   * TODO: Figure out what the above GCMaps comment means and fix it!
   */
  @Override
  protected void emit_invoke_compiledmethod(CompiledMethod cm) {
    Offset methOffset = cm.getOsrJTOCoffset();
    asm.emitLAddrToc(T0, methOffset);
    asm.emitMTCTR(T0);
    boolean takeThis = !cm.method.isStatic();
    MethodReference ref = cm.method.getMemberRef().asMethodReference();
    genMoveParametersToRegisters(takeThis, ref);
    asm.emitBCCTRL();
    genPopParametersAndPushReturnValue(takeThis, ref);
  }

  @Override
  protected ForwardReference emit_pending_goto(int bTarget) {
    return asm.generatePendingJMP(bTarget);
  }

  //*************************************************************************
  //                             MAGIC
  //*************************************************************************

  /*
   *  Generate inline machine instructions for special methods that cannot be
   *  implemented in java bytecodes. These instructions are generated whenever
   *  we encounter an "invokestatic" bytecode that calls a method with a
   *  signature of the form "static native Magic.xxx(...)".
   *
   * NOTE: when adding a new "methodName" to "generate()", be sure to also
   * consider how it affects the values on the stack and update
   * "checkForActualCall()" accordingly.
   * If no call is actually generated, the map will reflect the status of the
   * locals (including parameters) at the time of the call but nothing on the
   * operand stack for the call site will be mapped.
   */

  /** Generate inline code sequence for specified method.
   * @param methodToBeCalled method whose name indicates semantics of code to be generated
   * @return true if there was magic defined for the method
   */
  private boolean generateInlineCode(MethodReference methodToBeCalled) {
    Atom methodName = methodToBeCalled.getName();

    if (methodToBeCalled.isSysCall()) {
      TypeReference[] args = methodToBeCalled.getParameterTypes();

      // (1) Set up arguments according to OS calling convention, excluding the first
      // which is not an argument to the native function but the address of the function to call
      int paramWords = methodToBeCalled.getParameterWords();
      int gp = FIRST_OS_PARAMETER_GPR.value();
      int fp = FIRST_OS_PARAMETER_FPR.value();
      int stackIndex = paramWords - 1;
      int paramBytes = ((VM.BuildFor64Addr ? args.length : paramWords) - 1) * BYTES_IN_STACKSLOT;
      int callee_param_index = -BYTES_IN_STACKSLOT - paramBytes;

      for (int i = 1; i < args.length; i++) {
        TypeReference t = args[i];
        if (t.isLongType()) {
          stackIndex -= 2;
          callee_param_index += BYTES_IN_LONG;
          if (VM.BuildFor64Addr) {
            if (gp <= LAST_OS_PARAMETER_GPR.value()) {
              peekLong(GPR.lookup(gp), GPR.lookup(gp), stackIndex);
              gp++;
            } else {
              peekLong(S0, S0, stackIndex);
              asm.emitSTD(S0, callee_param_index - BYTES_IN_LONG, FP);
            }
          } else {
            if (VM.BuildForLinux) {
              /* NOTE: following adjustment is not stated in SVR4 ABI, but
               * was implemented in GCC.
               */
              gp += (gp + 1) & 0x01; // if gpr is even, gpr += 1
            }
            if (gp <= LAST_OS_PARAMETER_GPR.value()) {
              peekInt(GPR.lookup(gp++), stackIndex);
            }   // lo register := lo mem (== hi order word)
            if (gp <= LAST_OS_PARAMETER_GPR.value()) {
              peekInt(GPR.lookup(gp++), stackIndex + 1);    // hi register := hi mem (== lo order word)
            } else {
              peekLong(S0, S1, stackIndex);
              asm.emitSTW(S0, callee_param_index - BYTES_IN_LONG, FP);
              asm.emitSTW(S1, callee_param_index - BYTES_IN_INT, FP);
            }
          }
        } else if (t.isFloatType()) {
          stackIndex -= 1;
          callee_param_index += BYTES_IN_STACKSLOT;
          if (fp <= LAST_OS_PARAMETER_FPR.value()) {
            peekFloat(FPR.lookup(fp++), stackIndex);
          } else {
            peekFloat(FIRST_SCRATCH_FPR, stackIndex);
            asm.emitSTFS(FIRST_SCRATCH_FPR, callee_param_index - BYTES_IN_FLOAT, FP);
          }
        } else if (t.isDoubleType()) {
          stackIndex -= 2;
          callee_param_index += BYTES_IN_DOUBLE;
          if (fp <= LAST_OS_PARAMETER_FPR.value()) {
            peekDouble(FPR.lookup(fp++), stackIndex);
          } else {
            peekDouble(FIRST_SCRATCH_FPR, stackIndex);
            asm.emitSTFD(FIRST_SCRATCH_FPR, callee_param_index - BYTES_IN_DOUBLE, FP);
          }
        } else if (t.isIntLikeType()) {
          stackIndex -= 1;
          callee_param_index += BYTES_IN_STACKSLOT;
          if (gp <= LAST_OS_PARAMETER_GPR.value()) {
            peekInt(GPR.lookup(gp++), stackIndex);
          } else {
            peekInt(S0, stackIndex);
            asm.emitSTAddr(S0, callee_param_index - BYTES_IN_ADDRESS, FP);// save int zero-extended to be sure
          }
        } else { // t is object
          stackIndex -= 1;
          callee_param_index += BYTES_IN_STACKSLOT;
          if (gp <= LAST_OS_PARAMETER_GPR.value()) {
            peekAddr(GPR.lookup(gp++), stackIndex);
          } else {
            peekAddr(S0, stackIndex);
            asm.emitSTAddr(S0, callee_param_index - BYTES_IN_ADDRESS, FP);
          }
        }
      }
      if (VM.VerifyAssertions) {
        VM._assert(stackIndex == 0);
      }

      // (2) Call it
      peekAddr(S0, paramWords - 1); // Load addres of function into S0
      generateSysCall(paramBytes); // make the call

      // (3) Pop Java expression stack
      discardSlots(paramWords);

      // (4) Push return value (if any)
      TypeReference rtype = methodToBeCalled.getReturnType();
      if (rtype.isIntLikeType()) {
        pushInt(T0);
      } else if (rtype.isWordLikeType() || rtype.isReferenceType()) {
        pushAddr(T0);
      } else if (rtype.isDoubleType()) {
        pushDouble(FIRST_OS_PARAMETER_FPR);
      } else if (rtype.isFloatType()) {
        pushFloat(FIRST_OS_PARAMETER_FPR);
      } else if (rtype.isLongType()) {
        pushLong(T0, VM.BuildFor64Addr ? T0 : T1);
      }
      return true;
    }

    if (methodToBeCalled.getType() == TypeReference.Address) {
      // Address.xyz magic

      TypeReference[] types = methodToBeCalled.getParameterTypes();

      // Loads all take the form:
      // ..., Address, [Offset] -> ..., Value

      if (methodName == MagicNames.loadAddress ||
          methodName == MagicNames.loadObjectReference ||
          methodName == MagicNames.loadWord) {

        if (types.length == 0) {
          popAddr(T0);                  // pop base
          asm.emitLAddr(T0, 0, T0);    // *(base)
          pushAddr(T0);                 // push *(base)
        } else {
          popOffset(T1);                // pop offset
          popAddr(T0);                  // pop base
          asm.emitLAddrX(T0, T1, T0);   // *(base+offset)
          pushAddr(T0);                 // push *(base+offset)
        }
        return true;
      }

      if (methodName == MagicNames.loadChar) {

        if (types.length == 0) {
          popAddr(T0);                  // pop base
          asm.emitLHZ(T0, 0, T0);       // load with zero extension.
          pushInt(T0);                  // push *(base)
        } else {
          popOffset(T1);                // pop offset
          popAddr(T0);                  // pop base
          asm.emitLHZX(T0, T1, T0);     // load with zero extension.
          pushInt(T0);                  // push *(base+offset)
        }
        return true;
      }

      if (methodName == MagicNames.loadShort) {

        if (types.length == 0) {
          popAddr(T0);                  // pop base
          asm.emitLHA(T0, 0, T0);       // load with sign extension.
          pushInt(T0);                  // push *(base)
        } else {
          popOffset(T1);                // pop offset
          popAddr(T0);                  // pop base
          asm.emitLHAX(T0, T1, T0);     // load with sign extension.
          pushInt(T0);                  // push *(base+offset)
        }
        return true;
      }

      if (methodName == MagicNames.loadByte) {
        if (types.length == 0) {
          popAddr(T0);                  // pop base
          asm.emitLBZ(T0, 0, T0);       // load with zero extension.
          asm.emitEXTSB(T0, T0);        // sign extend
          pushInt(T0);                  // push *(base)
        } else {
          popOffset(T1);                // pop offset
          popAddr(T0);                  // pop base
          asm.emitLBZX(T0, T1, T0);     // load with zero extension.
          asm.emitEXTSB(T0, T0);        // sign extend
          pushInt(T0);                  // push *(base+offset)
        }
        return true;
      }

      if (methodName == MagicNames.loadInt || methodName == MagicNames.loadFloat) {

        if (types.length == 0) {
          popAddr(T0);                  // pop base
          asm.emitLInt(T0, 0, T0);     // *(base)
          pushInt(T0);                  // push *(base)
        } else {
          popOffset(T1);                // pop offset
          popAddr(T0);                  // pop base
          asm.emitLIntX(T0, T1, T0);    // *(base+offset)
          pushInt(T0);                  // push *(base+offset)
        }
        return true;
      }

      if (methodName == MagicNames.loadDouble || methodName == MagicNames.loadLong) {

        if (types.length == 0) {
          popAddr(T1);                  // pop base
          asm.emitLFD(F0, 0, T1);      // *(base)
          pushDouble(F0);               // push double
        } else {
          popOffset(T2);                // pop offset
          popAddr(T1);                  // pop base
          asm.emitLFDX(F0, T1, T2);    // *(base+offset)
          pushDouble(F0);               // push *(base+offset)
        }
        return true;
      }

      // Prepares all take the form:
      // ..., Address, [Offset] -> ..., Value

      if ((methodName == MagicNames.prepareInt) ||
          (VM.BuildFor32Addr && (methodName == MagicNames.prepareWord)) ||
          (VM.BuildFor32Addr && (methodName == MagicNames.prepareObjectReference)) ||
          (VM.BuildFor32Addr && (methodName == MagicNames.prepareAddress))) {
        if (types.length == 0) {
          popAddr(T0);                             // pop base
          asm.emitLWARX(T0, GPR.R0, T0);           // *(base), setting reservation address
          // this Integer is not sign extended !!
          pushInt(T0);                             // push *(base+offset)
        } else {
          popOffset(T1);                              // pop offset
          popAddr(T0);                             // pop base
          asm.emitLWARX(T0, T1, T0);              // *(base+offset), setting reservation address
          // this Integer is not sign extended !!
          pushInt(T0);                             // push *(base+offset)
        }
        return true;
      }

      if ((methodName == MagicNames.prepareLong) ||
          (VM.BuildFor64Addr && (methodName == MagicNames.prepareWord)) ||
          (VM.BuildFor64Addr && (methodName == MagicNames.prepareObjectReference)) ||
          (VM.BuildFor64Addr && (methodName == MagicNames.prepareAddress))) {
        if (types.length == 0) {
          popAddr(T0);                             // pop base
          asm.emitLDARX(T0, GPR.R0, T0);           // *(base), setting reservation address
          // this Integer is not sign extended !!
          pushAddr(T0);                             // push *(base+offset)
        } else {
          popOffset(T1);                              // pop offset
          popAddr(T0);                             // pop base
          if (VM.BuildFor64Addr) {
            asm.emitLDARX(T0, T1, T0);              // *(base+offset), setting reservation address
          } else {
            // TODO: handle 64bit prepares in 32bit environment
          }
          // this Integer is not sign extended !!
          pushAddr(T0);                             // push *(base+offset)
        }
        return true;
      }

      // Attempts all take the form:
      // ..., Address, OldVal, NewVal, [Offset] -> ..., Success?

      if (methodName == MagicNames.attempt &&
          ((types[0] == TypeReference.Int) ||
           (VM.BuildFor32Addr && (types[0] == TypeReference.Address)) ||
           (VM.BuildFor32Addr && (types[0] == TypeReference.Word)))) {
        if (types.length == 2) {
          popInt(T2);                            // pop newValue
          discardSlot();                         // ignore oldValue
          popAddr(T0);                           // pop base
          asm.emitSTWCXr(T2, GPR.R0, T0);        // store new value and set CR0
          asm.emitLVAL(T0, 0);                  // T0 := false
          ForwardReference fr = asm.emitForwardBC(NE);             // skip, if store failed
          asm.emitLVAL(T0, 1);                  // T0 := true
          fr.resolve(asm);
          pushInt(T0);                           // push success of store
        } else {
          popOffset(T1);                         // pop offset
          popInt(T2);                            // pop newValue
          discardSlot();                         // ignore oldValue
          popAddr(T0);                           // pop base
          asm.emitSTWCXr(T2, T1, T0);           // store new value and set CR0
          asm.emitLVAL(T0, 0);                  // T0 := false
          ForwardReference fr = asm.emitForwardBC(NE);             // skip, if store failed
          asm.emitLVAL(T0, 1);                  // T0 := true
          fr.resolve(asm);
          pushInt(T0);                           // push success of store
        }
        return true;
      }

      if (methodName == MagicNames.attempt &&
          ((types[0] == TypeReference.Long) ||
           (VM.BuildFor64Addr && (types[0] == TypeReference.Address)) ||
           (VM.BuildFor64Addr && (types[0] == TypeReference.Word)))) {
        if (types.length == 2) {
          popAddr(T2);                             // pop newValue
          discardSlot();                           // ignore oldValue
          popAddr(T0);                             // pop base
          asm.emitSTDCXr(T2, GPR.R0, T0);          // store new value and set CR0
          asm.emitLVAL(T0, 0);                   // T0 := false
          ForwardReference fr = asm.emitForwardBC(NE);  // skip, if store failed
          asm.emitLVAL(T0, 1);                   // T0 := true
          fr.resolve(asm);
          pushInt(T0);                           // push success of store
        } else {
          popOffset(T1);                           // pop offset
          popAddr(T2);                             // pop newValue
          discardSlot();                           // ignore oldValue
          popAddr(T0);                             // pop base
          if (VM.BuildFor64Addr) {
            asm.emitSTDCXr(T2, T1, T0);            // store new value and set CR0
          } else {
            // TODO: handle 64bit attempts in 32bit environment
          }
          asm.emitLVAL(T0, 0);                   // T0 := false
          ForwardReference fr = asm.emitForwardBC(NE);             // skip, if store failed
          asm.emitLVAL(T0, 1);                   // T0 := true
          fr.resolve(asm);
          pushInt(T0);                           // push success of store
        }
        return true;
      }

      // Stores all take the form:
      // ..., Address, Value, [Offset] -> ...
      if (methodName == MagicNames.store) {

        if (types[0] == TypeReference.Word ||
            types[0] == TypeReference.ObjectReference ||
            types[0] == TypeReference.Address) {
          if (types.length == 1) {
            popAddr(T1);                 // pop newvalue
            popAddr(T0);                 // pop base
            asm.emitSTAddrX(T1, GPR.R0, T0); // *(base) = newvalue
          } else {
            popOffset(T1);               // pop offset
            popAddr(T2);                 // pop newvalue
            popAddr(T0);                 // pop base
            asm.emitSTAddrX(T2, T1, T0); // *(base+offset) = newvalue
          }
          return true;
        }

        if (types[0] == TypeReference.Byte || types[0] == TypeReference.Boolean) {
          if (types.length == 1) {
            popInt(T1);                  // pop newvalue
            popAddr(T0);                 // pop base
            asm.emitSTBX(T1, GPR.R0, T0);// *(base) = newvalue
          } else {
            popOffset(T1);               // pop offset
            popInt(T2);                  // pop newvalue
            popAddr(T0);                 // pop base
            asm.emitSTBX(T2, T1, T0);    // *(base+offset) = newvalue
          }
          return true;
        }

        if (types[0] == TypeReference.Int || types[0] == TypeReference.Float) {
          if (types.length == 1) {
            popInt(T1);                  // pop newvalue
            popAddr(T0);                 // pop base
            asm.emitSTWX(T1, GPR.R0, T0);// *(base+offset) = newvalue
          } else {
            popOffset(T1);               // pop offset
            popInt(T2);                  // pop newvalue
            popAddr(T0);                 // pop base
            asm.emitSTWX(T2, T1, T0);    // *(base+offset) = newvalue
          }
          return true;
        }

        if (types[0] == TypeReference.Short || types[0] == TypeReference.Char) {
          if (types.length == 1) {
            popInt(T1);                  // pop newvalue
            popAddr(T0);                 // pop base
            asm.emitSTHX(T1, GPR.R0, T0); // *(base) = newvalue
          } else {
            popOffset(T1);               // pop offset
            popInt(T2);                  // pop newvalue
            popAddr(T0);                 // pop base
            asm.emitSTHX(T2, T1, T0);    // *(base+offset) = newvalue
          }
          return true;
        }

        if (types[0] == TypeReference.Double || types[0] == TypeReference.Long) {
          if (types.length == 1) {
            popLong(T2, T1);                      // pop newvalue low and high
            popAddr(T0);                          // pop base
            if (VM.BuildFor32Addr) {
              asm.emitSTW(T2, 0, T0);             // *(base) = newvalue low
              asm.emitSTW(T1, BYTES_IN_INT, T0);  // *(base+4) = newvalue high
            } else {
              asm.emitSTD(T1, 0, T0);           // *(base) = newvalue
            }
          } else {
            popOffset(T1);                        // pop offset
            popLong(T3, T2);                      // pop newvalue low and high
            popAddr(T0);                          // pop base
            if (VM.BuildFor32Addr) {
              asm.emitSTWX(T3, T1, T0);           // *(base+offset) = newvalue low
              asm.emitADDI(T1, BYTES_IN_INT, T1); // offset += 4
              asm.emitSTWX(T2, T1, T0);           // *(base+offset) = newvalue high
            } else {
              asm.emitSTDX(T2, T1, T0);           // *(base+offset) = newvalue
            }
          }
          return true;
        }
      }
    }

    if (methodName == MagicNames.getFramePointer) {
      pushAddr(FP);
    } else if (methodName == MagicNames.getCallerFramePointer) {
      popAddr(T0);                               // pop  frame pointer of callee frame
      asm.emitLAddr(T1, STACKFRAME_FRAME_POINTER_OFFSET.toInt(), T0); // load frame pointer of caller frame
      pushAddr(T1);                               // push frame pointer of caller frame
    } else if (methodName == MagicNames.setCallerFramePointer) {
      popAddr(T1); // value
      popAddr(T0); // fp
      asm.emitSTAddr(T1, STACKFRAME_FRAME_POINTER_OFFSET.toInt(), T0); // *(address+SFPO) := value
    } else if (methodName == MagicNames.getCompiledMethodID) {
      popAddr(T0);                           // pop  frame pointer of callee frame
      asm.emitLInt(T1, STACKFRAME_METHOD_ID_OFFSET.toInt(), T0); // load compiled method id
      pushInt(T1);                           // push method ID
    } else if (methodName == MagicNames.setCompiledMethodID) {
      popInt(T1); // value
      popAddr(T0); // fp
      asm.emitSTW(T1, STACKFRAME_METHOD_ID_OFFSET.toInt(), T0); // *(address+SNIO) := value
    } else if (methodName == MagicNames.getNextInstructionAddress) {
      popAddr(T0);                                  // pop  frame pointer of callee frame
      asm.emitLAddr(T1, STACKFRAME_RETURN_ADDRESS_OFFSET.toInt(), T0); // load frame pointer of caller frame
      pushAddr(T1);                                  // push frame pointer of caller frame
    } else if (methodName == MagicNames.getReturnAddressLocation) {
      popAddr(T0);                                  // pop  frame pointer of callee frame
      asm.emitLAddr(T1, STACKFRAME_FRAME_POINTER_OFFSET.toInt(), T0);    // load frame pointer of caller frame
      asm.emitADDI(T2, STACKFRAME_RETURN_ADDRESS_OFFSET, T1); // get location containing ret addr
      pushAddr(T2);                                  // push frame pointer of caller frame
    } else if (methodName == MagicNames.getTocPointer || methodName == MagicNames.getJTOC) {
      pushAddr(JTOC);
    } else if (methodName == MagicNames.getThreadRegister) {
      pushAddr(THREAD_REGISTER);
    } else if (methodName == MagicNames.setThreadRegister) {
      popAddr(THREAD_REGISTER);
    } else if (methodName == MagicNames.getTimeBase) {
      if (VM.BuildFor64Addr) {
        asm.emitMFTB(T1);      // T1 := time base
      } else {
        int label = asm.getMachineCodeIndex();
        asm.emitMFTBU(T0);                      // T0 := time base, upper
        asm.emitMFTB(T1);                      // T1 := time base, lower
        asm.emitMFTBU(T2);                      // T2 := time base, upper
        asm.emitCMP(T0, T2);                  // T0 == T2?
        asm.emitBC(NE, label);               // lower rolled over, try again
      }
      pushLong(T0, T1);
    } else if (methodName == MagicNames.invokeClassInitializer) {
      popAddr(T0); // t0 := address to be called
      asm.emitMTCTR(T0);
      asm.emitBCCTRL();          // call
    } else if (methodName == MagicNames.invokeMethodReturningVoid) {
      generateMethodInvocation(); // call method
    } else if (methodName == MagicNames.invokeMethodReturningInt) {
      generateMethodInvocation(); // call method
      pushInt(T0);       // push result
    } else if (methodName == MagicNames.invokeMethodReturningLong) {
      generateMethodInvocation(); // call method
      pushLong(T0, VM.BuildFor64Addr ? T0 : T1);       // push result
    } else if (methodName == MagicNames.invokeMethodReturningFloat) {
      generateMethodInvocation(); // call method
      pushFloat(F0);     // push result
    } else if (methodName == MagicNames.invokeMethodReturningDouble) {
      generateMethodInvocation(); // call method
      pushDouble(F0);     // push result
    } else if (methodName == MagicNames.invokeMethodReturningObject) {
      generateMethodInvocation(); // call method
      pushAddr(T0);       // push result
    } else if (methodName == MagicNames.addressArrayCreate) {
      RVMArray type = methodToBeCalled.getType().resolve().asArray();
      emit_resolved_newarray(type);
    } else if (methodName == MagicNames.addressArrayLength) {
      emit_arraylength();
    } else if (methodName == MagicNames.addressArrayGet) {
      genBoundsCheck();
      if (VM.BuildFor32Addr || methodToBeCalled.getType() == TypeReference.CodeArray) {
        asm.emitSLWI(T1, T1, LOG_BYTES_IN_INT);  // convert index to offset
        asm.emitLIntX(T2, T0, T1);  // load desired int array element
        pushInt(T2);
      } else {
        asm.emitSLDI(T1, T1, LOG_BYTES_IN_ADDRESS);  // convert index to offset
        asm.emitLAddrX(T2, T0, T1);  // load desired array element
        pushAddr(T2);
      }
    } else if (methodName == MagicNames.addressArraySet) {
      popAddr(T2);                                   // T2 is value to store
      genBoundsCheck();
      if (VM.BuildFor32Addr || methodToBeCalled.getType() == TypeReference.CodeArray) {
        asm.emitSLWI(T1, T1, LOG_BYTES_IN_INT); // convert index to offset
        asm.emitSTWX(T2, T0, T1); // store 32-bit value in array
      } else {
        asm.emitSLDI(T1, T1, LOG_BYTES_IN_ADDRESS);  // convert index to offset
        asm.emitSTAddrX(T2, T0, T1);                  // store value in array
      }
    } else if (methodName == MagicNames.getIntAtOffset) {
      popOffset(T1); // pop offset
      popAddr(T0); // pop object
      asm.emitLIntX(T0, T1, T0); // *(object+offset)
      pushInt(T0); // push *(object+offset)
    } else if (methodName == MagicNames.getFloatAtOffset) {
      popOffset(T1); // pop offset
      popAddr(T0); // pop object
      asm.emitLWZX(T0, T1, T0); // *(object+offset)
      pushInt(T0); // push *(object+offset),
//    asm.emitLFSX  (F0, T1, T0); // *(object+offset)
//    pushFloat(F0);
    } else if (methodName == MagicNames.getObjectAtOffset ||
               methodName == MagicNames.getWordAtOffset ||
               methodName == MagicNames.getAddressAtOffset ||
               methodName == MagicNames.getOffsetAtOffset ||
               methodName == MagicNames.getExtentAtOffset ||
               methodName == MagicNames.getTIBAtOffset) {
      if (methodToBeCalled.getParameterTypes().length == 3) {
        discardSlot(); // discard locationMetadata parameter
      }
      popOffset(T1); // pop offset
      popAddr(T0); // pop object
      asm.emitLAddrX(T0, T1, T0); // *(object+offset)
      pushAddr(T0); // push *(object+offset)
    } else if (methodName == MagicNames.getUnsignedByteAtOffset) {
      popOffset(T1);   // pop offset
      popAddr(T0);   // pop object
      asm.emitLBZX(T0, T1, T0);   // load byte with zero extension.
      pushInt(T0);    // push *(object+offset)
    } else if (methodName == MagicNames.getByteAtOffset) {
      popOffset(T1);   // pop offset
      popAddr(T0);   // pop object
      asm.emitLBZX(T0, T1, T0);   // load byte with zero extension.
      asm.emitEXTSB(T0, T0); // sign extend
      pushInt(T0);    // push *(object+offset)
    } else if (methodName == MagicNames.getCharAtOffset) {
      popOffset(T1);   // pop offset
      popAddr(T0);   // pop object
      asm.emitLHZX(T0, T1, T0);   // load char with zero extension.
      pushInt(T0);    // push *(object+offset)
    } else if (methodName == MagicNames.getShortAtOffset) {
      popOffset(T1);   // pop offset
      popAddr(T0);   // pop object
      asm.emitLHAX(T0, T1, T0);   // load short with sign extension.
      pushInt(T0);    // push *(object+offset)
    } else if (methodName == MagicNames.setIntAtOffset || methodName == MagicNames.setFloatAtOffset) {
      if (methodToBeCalled.getParameterTypes().length == 4) {
        discardSlot(); // discard locationMetadata parameter
      }
      popInt(T2); // pop newvalue
      popOffset(T1); // pop offset
      popAddr(T0); // pop object
      asm.emitSTWX(T2, T1, T0); // *(object+offset) = newvalue
    } else if (methodName == MagicNames.setObjectAtOffset || methodName == MagicNames.setWordAtOffset ||
        methodName == MagicNames.setAddressAtOffset || methodName == MagicNames.setOffsetAtOffset ||
        methodName == MagicNames.setExtentAtOffset) {
      if (methodToBeCalled.getParameterTypes().length == 4) {
        discardSlot(); // discard locationMetadata parameter
      }
      popAddr(T2); // pop newvalue
      popOffset(T1); // pop offset
      popAddr(T0); // pop object
      asm.emitSTAddrX(T2, T1, T0); // *(object+offset) = newvalue
    } else if (methodName == MagicNames.setByteAtOffset || methodName == MagicNames.setBooleanAtOffset) {
      if (methodToBeCalled.getParameterTypes().length == 4) {
        discardSlot(); // discard locationMetadata parameter
      }
      popInt(T2); // pop newvalue
      popOffset(T1); // pop offset
      popAddr(T0); // pop object
      asm.emitSTBX(T2, T1, T0); // *(object+offset) = newvalue
    } else if (methodName == MagicNames.setCharAtOffset || methodName == MagicNames.setShortAtOffset) {
      if (methodToBeCalled.getParameterTypes().length == 4) {
        discardSlot(); // discard locationMetadata parameter
      }
      popInt(T2); // pop newvalue
      popOffset(T1); // pop offset
      popAddr(T0); // pop object
      asm.emitSTHX(T2, T1, T0); // *(object+offset) = newvalue
    } else if (methodName == MagicNames.getLongAtOffset || methodName == MagicNames.getDoubleAtOffset) {
      popOffset(T2); // pop offset
      popAddr(T1); // pop object
      asm.emitLFDX(F0, T1, T2);
      pushDouble(F0);
    } else if ((methodName == MagicNames.setLongAtOffset) || (methodName == MagicNames.setDoubleAtOffset)) {
      if (methodToBeCalled.getParameterTypes().length == 4) {
        discardSlot(); // discard locationMetadata parameter
      }
      popLong(T3, T2);
      popOffset(T1); // pop offset
      popAddr(T0); // pop object
      if (VM.BuildFor32Addr) {
        asm.emitSTWX(T3, T1, T0); // *(object+offset) = newvalue low
        asm.emitADDI(T1, BYTES_IN_INT, T1); // offset += 4
        asm.emitSTWX(T2, T1, T0); // *(object+offset) = newvalue high
      } else {
        asm.emitSTDX(T2, T1, T0); // *(object+offset) = newvalue
      }
    } else if (methodName == MagicNames.getMemoryInt) {
      popAddr(T0); // address
      asm.emitLInt(T0, 0, T0); // *address
      pushInt(T0); // *sp := *address
    } else if (methodName == MagicNames.getMemoryWord || methodName == MagicNames.getMemoryAddress) {
      popAddr(T0); // address
      asm.emitLAddr(T0, 0, T0); // *address
      pushAddr(T0); // *sp := *address
    } else if (methodName == MagicNames.setMemoryInt) {
      popInt(T1); // value
      popAddr(T0); // address
      asm.emitSTW(T1, 0, T0); // *address := value
    } else if (methodName == MagicNames.setMemoryWord) {
      if (methodToBeCalled.getParameterTypes().length == 3) {
        discardSlot(); // discard locationMetadata parameter
      }
      popAddr(T1); // value
      popAddr(T0); // address
      asm.emitSTAddr(T1, 0, T0); // *address := value
    } else if ((methodName == MagicNames.prepareInt) ||
        (VM.BuildFor32Addr && (methodName == MagicNames.prepareObject)) ||
        (VM.BuildFor32Addr && (methodName == MagicNames.prepareAddress)) ||
        (VM.BuildFor32Addr && (methodName == MagicNames.prepareWord))) {
      popOffset(T1); // pop offset
      popAddr(T0); // pop object
      asm.emitLWARX(T0, T1, T0); // *(object+offset), setting thread's reservation address
      // this Integer is not sign extended !!
      pushInt(T0); // push *(object+offset)
    } else if ((methodName == MagicNames.prepareLong) ||
        (VM.BuildFor64Addr && (methodName == MagicNames.prepareObject)) ||
        (VM.BuildFor64Addr && (methodName == MagicNames.prepareAddress)) ||
        (VM.BuildFor64Addr && (methodName == MagicNames.prepareWord))) {
      popOffset(T1); // pop offset
      popAddr(T0); // pop object
      if (VM.BuildFor64Addr) {
        asm.emitLDARX(T0, T1, T0); // *(object+offset), setting thread's reservation address
      } else {
        // TODO: handle 64bit prepares in 32bit environment
      }
      pushAddr(T0); // push *(object+offset)
    } else if ((methodName == MagicNames.attemptInt) ||
        (VM.BuildFor32Addr && (methodName == MagicNames.attemptObject)) ||
        (VM.BuildFor32Addr && (methodName == MagicNames.attemptObjectReference)) ||
        (VM.BuildFor32Addr && (methodName == MagicNames.attemptAddress)) ||
        (VM.BuildFor32Addr && (methodName == MagicNames.attemptWord))) {
      popInt(T2);  // pop newValue
      discardSlot(); // ignore oldValue
      popOffset(T1);  // pop offset
      popAddr(T0);  // pop object
      asm.emitSTWCXr(T2, T1, T0); // store new value and set CR0
      asm.emitLVAL(T0, 0);  // T0 := false
      ForwardReference fr = asm.emitForwardBC(NE); // skip, if store failed
      asm.emitLVAL(T0, 1);   // T0 := true
      fr.resolve(asm);
      pushInt(T0);  // push success of conditional store
    } else if (methodName == MagicNames.attemptLong) {
      popLong(T3, T2); // pop newValue
      discardSlots(2); // ignore oldValue which is a long and thus takes 2 slots
      popOffset(T1);  // pop offset
      popAddr(T0);  // pop object
      if (VM.BuildFor64Addr) {
        asm.emitSTDCXr(T2, T1, T0); // store new value and set CR0
      } else {
        // TODO: handle 64bit attempts in 32bit environment
      }
      asm.emitLVAL(T0, 0);  // T0 := false
      ForwardReference fr = asm.emitForwardBC(NE); // skip, if store failed
      asm.emitLVAL(T0, 1);   // T0 := true
      fr.resolve(asm);
      pushInt(T0);  // push success of conditional store
    } else if (VM.BuildFor64Addr && ((methodName == MagicNames.attemptObject) ||
        (methodName == MagicNames.attemptObjectReference) ||
        (methodName == MagicNames.attemptAddress) ||
        (methodName == MagicNames.attemptWord))) {
      popAddr(T2);  // pop newValue
      discardSlot(); // ignore oldValue
      popOffset(T1);  // pop offset
      popAddr(T0);  // pop object
      asm.emitSTDCXr(T2, T1, T0); // store new value and set CR0
      asm.emitLVAL(T0, 0);  // T0 := false
      ForwardReference fr = asm.emitForwardBC(NE); // skip, if store failed
      asm.emitLVAL(T0, 1);   // T0 := true
      fr.resolve(asm);
      pushInt(T0);  // push success of conditional store
    } else if (methodName == MagicNames.saveThreadState) {
      peekAddr(T0, 0); // T0 := address of Registers object
      asm.emitLAddrToc(S0, ArchEntrypoints.saveThreadStateInstructionsField.getOffset());
      asm.emitMTCTR(S0);
      asm.emitBCCTRL(); // call out of line machine code
      discardSlot();  // pop arg
    } else if (methodName == MagicNames.threadSwitch) {
      peekAddr(T1, 0); // T1 := address of Registers of new thread
      peekAddr(T0, 1); // T0 := address of previous RVMThread object
      asm.emitLAddrToc(S0, ArchEntrypoints.threadSwitchInstructionsField.getOffset());
      asm.emitMTCTR(S0);
      asm.emitBCCTRL();
      discardSlots(2);  // pop two args
    } else if (methodName == MagicNames.restoreHardwareExceptionState) {
      peekAddr(T0, 0); // T0 := address of Registers object
      asm.emitLAddrToc(S0, ArchEntrypoints.restoreHardwareExceptionStateInstructionsField.getOffset());
      asm.emitMTLR(S0);
      asm.emitBCLR(); // branch to out of line machine code (does not return)
    } else if (methodName == MagicNames.returnToNewStack) {
      peekAddr(FP, 0);                                  // FP := new stackframe
      asm.emitLAddr(S0, STACKFRAME_RETURN_ADDRESS_OFFSET.toInt(), FP); // fetch...
      asm.emitMTLR(S0);                                         // ...return address
      asm.emitBCLR();                                           // return to caller
    } else if (methodName == MagicNames.dynamicBridgeTo) {
      if (VM.VerifyAssertions) VM._assert(klass.hasDynamicBridgeAnnotation());

      // fetch parameter (address to branch to) into CT register
      //
      peekAddr(T0, 0);
      asm.emitMTCTR(T0);

      // restore volatile and non-volatile registers
      // (note that these are only saved for "dynamic bridge" methods)
      //
      int offset = frameSize;

      // restore non-volatile and volatile fprs
      for (int i = LAST_NONVOLATILE_FPR.value(); i >= FIRST_VOLATILE_FPR.value(); --i) {
        asm.emitLFD(FPR.lookup(i), offset -= BYTES_IN_DOUBLE, FP);
      }

      // restore non-volatile gprs
      for (int i = LAST_NONVOLATILE_GPR.value(); i >= FIRST_NONVOLATILE_GPR.value(); --i) {
        asm.emitLAddr(GPR.lookup(i), offset -= BYTES_IN_ADDRESS, FP);
      }

      // skip saved thread-id, thread, and scratch registers
      offset -= (FIRST_NONVOLATILE_GPR.value() - LAST_VOLATILE_GPR.value() - 1) * BYTES_IN_ADDRESS;

      // restore volatile gprs
      for (int i = LAST_VOLATILE_GPR.value(); i >= FIRST_VOLATILE_GPR.value(); --i) {
        asm.emitLAddr(GPR.lookup(i), offset -= BYTES_IN_ADDRESS, FP);
      }

      // pop stackframe
      asm.emitLAddr(FP, 0, FP);

      // restore link register
      asm.emitLAddr(S0, STACKFRAME_RETURN_ADDRESS_OFFSET.toInt(), FP);
      asm.emitMTLR(S0);

      asm.emitBCCTR(); // branch always, through count register
    } else if (methodName == MagicNames.objectAsAddress ||
               methodName == MagicNames.addressAsByteArray ||
               methodName == MagicNames.addressAsObject ||
               methodName == MagicNames.addressAsTIB ||
               methodName == MagicNames.objectAsType ||
               methodName == MagicNames.objectAsShortArray ||
               methodName == MagicNames.objectAsIntArray ||
               methodName == MagicNames.objectAsThread ||
               methodName == MagicNames.floatAsIntBits ||
               methodName == MagicNames.intBitsAsFloat ||
               methodName == MagicNames.doubleAsLongBits ||
               methodName == MagicNames.longBitsAsDouble) {
      // no-op (a type change, not a representation change)
    } else if (methodName == MagicNames.getObjectType) {
      popAddr(T0);                   // get object pointer
      asm.baselineEmitLoadTIB(T0, T0);
      asm.emitLAddr(T0, TIB_TYPE_INDEX << LOG_BYTES_IN_ADDRESS, T0); // get "type" field from type information block
      pushAddr(T0);                   // *sp := type
    } else if (methodName == MagicNames.getArrayLength) {
      popAddr(T0);                   // get object pointer
      asm.emitLIntOffset(T0, T0, ObjectModel.getArrayLengthOffset()); // get array length field
      pushInt(T0);                   // *sp := length
    } else if (methodName == MagicNames.synchronizeInstructionCache) {
      asm.emitISYNC();
    } else if (methodName == MagicNames.pause) {
      // NO-OP
    } else if (methodName == MagicNames.combinedLoadBarrier) {
      asm.emitHWSYNC();
    } else if (methodName == MagicNames.storeStoreBarrier) {
      asm.emitSYNC();
    } else if (methodName == MagicNames.fence) {
      asm.emitHWSYNC();
    } else if (methodName == MagicNames.illegalInstruction) {
      asm.emitIllegalInstruction();
    } else if (methodName == MagicNames.dcbst) {
      popAddr(T0);    // address
      asm.emitDCBST(GPR.R0, T0);
    } else if (methodName == MagicNames.dcbt || methodName == MagicNames.prefetch) {
      popAddr(T0);    // address
      asm.emitDCBT(GPR.R0, T0);
    } else if (methodName == MagicNames.dcbtst) {
      popAddr(T0);    // address
      asm.emitDCBTST(GPR.R0, T0);
    } else if (methodName == MagicNames.dcbz) {
      popAddr(T0);    // address
      asm.emitDCBZ(GPR.R0, T0);
    } else if (methodName == MagicNames.dcbzl) {
      popAddr(T0);    // address
      asm.emitDCBZL(GPR.R0, T0);
    } else if (methodName == MagicNames.icbi) {
      popAddr(T0);    // address
      asm.emitICBI(GPR.R0, T0);
    } else if (methodName == MagicNames.sqrt) {
      TypeReference argType = method.getParameterTypes()[0];
      if (argType == TypeReference.Float) {
        popFloat(F0);
        asm.emitFSQRTS(F0, F0);
        pushFloat(F0);
      } else {
        if (VM.VerifyAssertions)
          VM._assert(argType == TypeReference.Double);
        popDouble(F0);
        asm.emitFSQRT(F0, F0);
        pushDouble(F0);
      }
    } else if (methodName == MagicNames.getInlineDepth ||
               methodName == MagicNames.isConstantParameter) {
      emit_iconst(0);
    } else if (methodName == MagicNames.getCompilerLevel) {
      emit_iconst(-1);
    } else if (methodName == MagicNames.getFrameSize) {
      emit_iconst(frameSize);
    } else if (methodName == MagicNames.wordToInt ||
               methodName == MagicNames.wordToAddress ||
               methodName == MagicNames.wordToOffset ||
               methodName == MagicNames.wordToObject ||
               methodName == MagicNames.wordFromObject ||
               methodName == MagicNames.wordToObjectReference ||
               methodName == MagicNames.wordToExtent ||
               methodName == MagicNames.wordToWord ||
               methodName == MagicNames.codeArrayAsObject ||
               methodName == MagicNames.tibAsObject) {
      // no-op
    } else if (methodName == MagicNames.wordToLong) {
      asm.emitLVAL(T0, 0);
      pushAddr(T0);
    } else if (methodName == MagicNames.wordFromInt || methodName == MagicNames.wordFromIntSignExtend) {
      if (VM.BuildFor64Addr) {
        popInt(T0);
        pushAddr(T0);
      } // else no-op
    } else if (methodName == MagicNames.wordFromIntZeroExtend) {
      if (VM.BuildFor64Addr) {
        asm.emitLWZ(T0, spTopOffset + BYTES_IN_STACKSLOT - BYTES_IN_INT, FP);
        pokeAddr(T0, 0);
      } // else no-op
    } else if (methodName == MagicNames.wordFromLong) {
      discardSlot();
    } else if (methodName == MagicNames.wordPlus) {
      if (VM.BuildFor64Addr && (methodToBeCalled.getParameterTypes()[0] == TypeReference.Int)) {
        popInt(T0);
      } else {
        popAddr(T0);
      }
      popAddr(T1);
      asm.emitADD(T2, T1, T0);
      pushAddr(T2);
    } else if (methodName == MagicNames.wordMinus || methodName == MagicNames.wordDiff) {
      if (VM.BuildFor64Addr && (methodToBeCalled.getParameterTypes()[0] == TypeReference.Int)) {
        popInt(T0);
      } else {
        popAddr(T0);
      }
      popAddr(T1);
      asm.emitSUBFC(T2, T0, T1);
      pushAddr(T2);
    } else if (methodName == MagicNames.wordEQ) {
      generateAddrComparison(false, EQ);
    } else if (methodName == MagicNames.wordNE) {
      generateAddrComparison(false, NE);
    } else if (methodName == MagicNames.wordLT) {
      generateAddrComparison(false, LT);
    } else if (methodName == MagicNames.wordLE) {
      generateAddrComparison(false, LE);
    } else if (methodName == MagicNames.wordGT) {
      generateAddrComparison(false, GT);
    } else if (methodName == MagicNames.wordGE) {
      generateAddrComparison(false, GE);
    } else if (methodName == MagicNames.wordsLT) {
      generateAddrComparison(true, LT);
    } else if (methodName == MagicNames.wordsLE) {
      generateAddrComparison(true, LE);
    } else if (methodName == MagicNames.wordsGT) {
      generateAddrComparison(true, GT);
    } else if (methodName == MagicNames.wordsGE) {
      generateAddrComparison(true, GE);
    } else if (methodName == MagicNames.wordIsZero || methodName == MagicNames.wordIsNull) {
      // unsigned comparison generating a boolean
      popAddr(T0);
      asm.emitLVAL(T1, 0);
      asm.emitLVAL(T2, 1);
      asm.emitCMPLAddr(T0, T1);
      ForwardReference fr = asm.emitForwardBC(EQ);
      asm.emitLVAL(T2, 0);
      fr.resolve(asm);
      pushInt(T2);
    } else if (methodName == MagicNames.wordIsMax) {
      // unsigned comparison generating a boolean
      popAddr(T0);
      asm.emitLVAL(T1, -1);
      asm.emitLVAL(T2, 1);
      asm.emitCMPLAddr(T0, T1);
      ForwardReference fr = asm.emitForwardBC(EQ);
      asm.emitLVAL(T2, 0);
      fr.resolve(asm);
      pushInt(T2);
    } else if (methodName == MagicNames.wordZero || methodName == MagicNames.wordNull) {
      asm.emitLVAL(T0, 0);
      pushAddr(T0);
    } else if (methodName == MagicNames.wordOne) {
      asm.emitLVAL(T0, 1);
      pushAddr(T0);
    } else if (methodName == MagicNames.wordMax) {
      asm.emitLVAL(T0, -1);
      pushAddr(T0);
    } else if (methodName == MagicNames.wordAnd) {
      popAddr(T0);
      popAddr(T1);
      asm.emitAND(T2, T1, T0);
      pushAddr(T2);
    } else if (methodName == MagicNames.wordOr) {
      popAddr(T0);
      popAddr(T1);
      asm.emitOR(T2, T1, T0);
      pushAddr(T2);
    } else if (methodName == MagicNames.wordNot) {
      popAddr(T0);
      asm.emitLVAL(T1, -1);
      asm.emitXOR(T2, T1, T0);
      pushAddr(T2);
    } else if (methodName == MagicNames.wordXor) {
      popAddr(T0);
      popAddr(T1);
      asm.emitXOR(T2, T1, T0);
      pushAddr(T2);
    } else if (methodName == MagicNames.wordLsh) {
      popInt(T0);
      popAddr(T1);
      asm.emitSLAddr(T2, T1, T0);
      pushAddr(T2);
    } else if (methodName == MagicNames.wordRshl) {
      popInt(T0);
      popAddr(T1);
      asm.emitSRAddr(T2, T1, T0);
      pushAddr(T2);
    } else if (methodName == MagicNames.wordRsha) {
      popInt(T0);
      popAddr(T1);
      asm.emitSRA_Addr(T2, T1, T0);
      pushAddr(T2);
    } else {
      return false;
    }
    return true;
  }

  /** Emit code to perform an unsigned comparison on 2 address values
   * @param cc condition to test
   */
  private void generateAddrComparison(boolean signed, int cc) {
    popAddr(T1);
    popAddr(T0);
    asm.emitLVAL(T2, 1);
    if (signed) {
      asm.emitCMPAddr(T0, T1);
    } else {
      asm.emitCMPLAddr(T0, T1);
    }
    ForwardReference fr = asm.emitForwardBC(cc);
    asm.emitLVAL(T2, 0);
    fr.resolve(asm);
    pushInt(T2);
  }

  //----------------//
  // implementation //
  //----------------//

  /**
   * Generate code to invoke arbitrary method with arbitrary parameters/return value.
   * We generate inline code that calls "OutOfLineMachineCode.reflectiveMethodInvokerInstructions"
   * which, at runtime, will create a new stackframe with an appropriately sized spill area
   * (but no register save area, locals, or operand stack), load up the specified
   * fpr's and gpr's, call the specified method, pop the stackframe, and return a value.
   */
  private void generateMethodInvocation() {
    // On entry the stack looks like this:
    //
    //                       hi-mem
    //            +-------------------------+    \
    //            |         code[]          |     |
    //            +-------------------------+     |
    //            |         gprs[]          |     |
    //            +-------------------------+     |- java operand stack
    //            |         fprs[]          |     |
    //            +-------------------------+     |
    //            |         fprMeta[]       |     |
    //            +-------------------------+     |
    //            |         spills[]        |     |
    //            +-------------------------+    /

    // fetch parameters and generate call to method invoker
    //
    asm.emitLAddrToc(S0, ArchEntrypoints.reflectiveMethodInvokerInstructionsField.getOffset());
    peekAddr(T0, 4);        // t0 := code
    asm.emitMTCTR(S0);
    peekAddr(T1, 3);        // t1 := gprs
    peekAddr(T2, 2);        // t2 := fprs
    peekAddr(T3, 1);        // t3 := fprMeta
    peekAddr(T4, 0);        // t4 := spills
    asm.emitBCCTRL();
    discardSlots(5);       // pop parameters
  }

  /**
   * Generate call and return sequence to invoke a C function through the
   * boot record field specificed by target.
   * Caller handles parameter passing and expression stack
   * (setting up args, pushing return, adjusting stack height).
   *
   * <pre>
   *  Create a linkage area that's compatible with RS6000 "C" calling conventions.
   * Just before the call, the stack looks like this:
   *
   *                     hi-mem
   *            +-------------------------+  . . . . . . . .
   *            |          ...            |                  \
   *            +-------------------------+                   |
   *            |          ...            |    \              |
   *            +-------------------------+     |             |
   *            |       (int val0)        |     |  java       |- java
   *            +-------------------------+     |-  operand   |   stack
   *            |       (int val1)        |     |    stack    |    frame
   *            +-------------------------+     |             |
   *            |          ...            |     |             |
   *            +-------------------------+     |             |
   *            |      (int valN-1)       |     |             |
   *            +-------------------------+    /              |
   *            |          ...            |                   |
   *            +-------------------------+                   |
   *            |                         | <-- spot for this frame's callee's return address
   *            +-------------------------+                   |
   *            |          MI             | <-- this frame's method id
   *            +-------------------------+                   |
   *            |       saved FP          | <-- this frame's caller's frame
   *            +-------------------------+  . . . . . . . . /
   *            |      saved JTOC         |
   *            +-------------------------+  . . . . . . . . . . . . . .
   *            | parameterN-1 save area  | +  \                         \
   *            +-------------------------+     |                         |
   *            |          ...            | +   |                         |
   *            +-------------------------+     |- register save area for |
   *            |  parameter1 save area   | +   |    use by callee        |
   *            +-------------------------+     |                         |
   *            |  parameter0 save area   | +  /                          |  rs6000
   *            +-------------------------+                               |-  linkage
   *        +20 |       TOC save area     | +                             |    area
   *            +-------------------------+                               |
   *        +16 |       (reserved)        | -    + == used by callee      |
   *            +-------------------------+      - == ignored by callee   |
   *        +12 |       (reserved)        | -                             |
   *            +-------------------------+                               |
   *         +8 |       LR save area      | +                             |
   *            +-------------------------+                               |
   *         +4 |       CR save area      | +                             |
   *            +-------------------------+                               |
   *  FP ->  +0 |       (backlink)        | -                             |
   *            +-------------------------+  . . . . . . . . . . . . . . /
   *
   * Notes:
   * 1. parameters are according to host OS calling convention.
   * 2. space is also reserved on the stack for use by callee
   *    as parameter save area
   * 3. parameters are pushed on the java operand stack left to right
   *    java conventions) but if callee saves them, they will
   *    appear in the parameter save area right to left (C conventions)
   */
  private void generateSysCall(int parametersSize, RVMField target) {
    // acquire toc and ip from bootrecord
    asm.emitLAddrToc(S0, Entrypoints.the_boot_recordField.getOffset());
    asm.emitLAddrOffset(S0, S0, target.getOffset());
    generateSysCall(parametersSize);
  }

  /**
   * Generate a sys call where the address of the function or (when build for 64-bit
   * PowerPC ELF ABI) function descriptor have been loaded into S0 already
   */
  private void generateSysCall(int parametersSize) {
    int linkageAreaSize = parametersSize + BYTES_IN_STACKSLOT + (6 * BYTES_IN_STACKSLOT);

    if (VM.BuildFor32Addr) {
      asm.emitSTWU(FP, -linkageAreaSize, FP);        // create linkage area
    } else {
      asm.emitSTDU(FP, -linkageAreaSize, FP);        // create linkage area
    }
    asm.emitSTAddr(JTOC, linkageAreaSize - BYTES_IN_STACKSLOT, FP);      // save JTOC
    if (VM.BuildForPower64ELF_ABI) {
      /* GPR0 is pointing to the function descriptor, so we need to load the TOC and IP from that */
      // Load TOC (Offset one word)
      asm.emitLAddrOffset(JTOC, S0, Offset.fromIntSignExtend(BYTES_IN_STACKSLOT));
      // Load IP (offset 0)
      asm.emitLAddrOffset(S0, S0, Offset.zero());
    }
    // call it
    asm.emitMTCTR(S0);
    asm.emitBCCTRL();

    // cleanup
    asm.emitLAddr(JTOC, linkageAreaSize - BYTES_IN_STACKSLOT, FP);    // restore JTOC
    asm.emitADDI(FP, linkageAreaSize, FP);        // remove linkage area
  }
}

