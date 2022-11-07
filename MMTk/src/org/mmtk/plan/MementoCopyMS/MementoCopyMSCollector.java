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
package org.mmtk.plan.MementoCopyMS;

import org.mmtk.plan.*;
import org.mmtk.policy.CopyLocal;
import org.mmtk.policy.LargeObjectLocal;
import org.mmtk.policy.MarkSweepLocal;
import org.mmtk.utility.Log;
import org.mmtk.utility.alloc.Allocator;
import org.mmtk.vm.VM;

import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;

/**
 * This class implements <i>per-collector thread</i> behavior
 * and state for the <i>CopyMS</i> plan.<p>
 *
 * Specifically, this class defines <i>CopyMS</i>
 * collection behavior (through <code>trace</code> and
 * the <code>collectionPhase</code> method), and
 * collection-time allocation into the mature space.
 *
 * @see MementoCopyMS
 * @see MementoCopyMSMutator
 * @see StopTheWorldCollector
 * @see CollectorContext
 */
@Uninterruptible
public class MementoCopyMSCollector extends StopTheWorldCollector {

  /****************************************************************************
   * Instance fields
   */

  /**
   *
   */
  private final MarkSweepLocal mature;
  private final MementoCopyMSTraceLocal trace;

  protected final LargeObjectLocal los;

  /****************************************************************************
   *
   * Initialization
   */

  /**
   * Create a new (local) instance.
   */
  public MementoCopyMSCollector() {
    los = new LargeObjectLocal(Plan.loSpace);
    mature = new MarkSweepLocal(MementoCopyMS.survivorSpace);
    trace = new MementoCopyMSTraceLocal(global().trace);
 }

  /****************************************************************************
   *
   * Collection-time allocation
   */

  /**
   * {@inheritDoc}
   */
  @Override
  @Inline
  public final Address allocCopy(ObjectReference original, int bytes,
      int align, int offset, int allocator) {
	  Log.write("### Alloc Copy Bytes: ");
	    Log.write(bytes);
	    Log.write(" Allocator: ");
	    Log.write(allocator);
	    Log.writeln();
    if (allocator == Plan.ALLOC_LOS) {
      if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(Allocator.getMaximumAlignedSize(bytes, align) > Plan.MAX_NON_LOS_COPY_BYTES);
      return los.alloc(bytes, align, offset);
    } else {
      if (VM.VERIFY_ASSERTIONS) {
        VM.assertions._assert(bytes <= Plan.MAX_NON_LOS_COPY_BYTES);
        VM.assertions._assert(allocator == MementoCopyMS.ALLOC_SURVIVOR);
      }
      return mature.alloc(bytes, align, offset);
    }
  }

  @Override
  @Inline
  public final void postCopy(ObjectReference object, ObjectReference typeRef,
      int bytes, int allocator) {
    if (allocator == Plan.ALLOC_LOS)
      Plan.loSpace.initializeHeader(object, false);
    else
      MementoCopyMS.survivorSpace.postCopy(object, true);
  }

  /****************************************************************************
   *
   * Collection
   */

  /**
   * {@inheritDoc}
   */
  @Override
  @Inline
  public final void collectionPhase(short phaseId, boolean primary) {
    if (phaseId == MementoCopyMS.PREPARE) {
      super.collectionPhase(phaseId, primary);
      mature.prepare();
      trace.prepare();
      return;
    }

    if (phaseId == MementoCopyMS.CLOSURE) {
      trace.completeTrace();
      return;
    }

    if (phaseId == MementoCopyMS.RELEASE) {
      mature.release();
      trace.release();
      super.collectionPhase(phaseId, primary);
      return;
    }

    super.collectionPhase(phaseId, primary);
  }

  /****************************************************************************
   *
   * Miscellaneous
   */

  /** @return the active global plan as an <code>MS</code> instance. */
  @Inline
  private static MementoCopyMS global() {
    return (MementoCopyMS) VM.activePlan.global();
  }

  /** @return The current trace instance. */
  @Override
  public final TraceLocal getCurrentTrace() {
    return trace;
  }

}
