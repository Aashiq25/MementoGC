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
package org.mmtk.plan.memento;

import org.mmtk.plan.*;
import org.mmtk.plan.generational.Gen;
import org.mmtk.plan.generational.GenCollector;
import org.mmtk.policy.CopyLocal;
import org.mmtk.utility.ForwardingWord;
import org.mmtk.utility.HeaderByte;
import org.mmtk.utility.alloc.Allocator;
import org.mmtk.vm.VM;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;

/**
 * This class implements <i>per-collector thread</i> behavior and state
 * for the <i>NoGC</i> plan, which simply allocates (without ever collecting
 * until the available space is exhausted.<p>
 *
 * Specifically, this class <i>would</i> define <i>NoGC</i> collection time semantics,
 * however, since this plan never collects, this class consists only of stubs which
 * may be useful as a template for implementing a basic collector.
 *
 * @see MementoGC
 * @see MementoGCMutator
 * @see CollectorContext
 */
@Uninterruptible
public class MementoGCCollector extends GenCollector {

  /************************************************************************
   * Instance fields
   */

  /**
   *
   */
  private final MementoGCTraceLocal trace;
  protected final CopyLocal mature;


  public MementoGCCollector() {
    mature = new CopyLocal(MementoGC.toSpace());
    trace = new MementoGCTraceLocal(global().trace, this);
  }

  @Override
  @Inline
  public Address allocCopy(ObjectReference original, int bytes,
      int align, int offset, int allocator) {
    if (allocator == Plan.ALLOC_LOS) {
      if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(Allocator.getMaximumAlignedSize(bytes, align) > Plan.MAX_NON_LOS_COPY_BYTES);
      return los.alloc(bytes, align, offset);
    } else {
      if (VM.VERIFY_ASSERTIONS) {
        VM.assertions._assert(bytes <= Plan.MAX_NON_LOS_COPY_BYTES);
        VM.assertions._assert(allocator == MementoGC.ALLOC_MATURE_MINORGC ||
            allocator == MementoGC.ALLOC_MATURE_MAJORGC);
      }
      return mature.alloc(bytes, align, offset);
    }
  }

  /**
   * {@inheritDoc}<p>
   *
   * In this case we clear any bits used for this object's GC metadata.
   */
  @Override
  @Inline
  public final void postCopy(ObjectReference object, ObjectReference typeRef,
      int bytes, int allocator) {
    ForwardingWord.clearForwardingBits(object);
    if (allocator == Plan.ALLOC_LOS)
      Plan.loSpace.initializeHeader(object, false);
    else if (MementoGC.IGNORE_REMSETS)
      MementoGC.immortalSpace.traceObject(getCurrentTrace(), object); // FIXME this does not look right
    if (Gen.USE_OBJECT_BARRIER)
      HeaderByte.markAsUnlogged(object);
  }


  /****************************************************************************
   * Collection
   */

  /**
   * Perform a garbage collection
   */
  
  @Inline
  @Override
  public final void collectionPhase(short phaseId, boolean primary) {
    // VM.assertions.fail("GC Triggered in memento Plan.");
    if (global().traceFullHeap()) {
      if (phaseId == MementoGC.PREPARE) {
        super.collectionPhase(phaseId, primary);
        if (global().gcFullHeap) mature.rebind(MementoGC.toSpace());
      }
      if (phaseId == MementoGC.CLOSURE) {
        trace.completeTrace();
        return;
      }
      if (phaseId == MementoGC.RELEASE) {
        trace.release();
        super.collectionPhase(phaseId, primary);
        return;
      }
    }
    super.collectionPhase(phaseId, primary);
  }

  /****************************************************************************
   * Miscellaneous
   */

  /** @return The active global plan as a <code>NoGC</code> instance. */
  private static MementoGC global() {
    return (MementoGC) VM.activePlan.global();
  }

  /** Show the status of the mature allocator. */
  protected final void showMature() {
    mature.show();
  }

  @Override
  public final TraceLocal getFullHeapTrace() {
    return trace;
  }
}
