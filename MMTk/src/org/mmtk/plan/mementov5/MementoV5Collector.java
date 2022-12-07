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
package org.mmtk.plan.mementov5;

import org.mmtk.plan.Plan;
import org.mmtk.plan.TraceLocal;
import org.mmtk.plan.generational.Gen;
import org.mmtk.plan.generational.GenCollector;
import org.mmtk.plan.generational.copying.GenCopyCollector;
import org.mmtk.policy.MarkSweepLocal;
import org.mmtk.policy.Space;
import org.mmtk.utility.HeaderByte;
import org.mmtk.utility.Log;
import org.mmtk.utility.alloc.Allocator;
import org.mmtk.utility.statistics.Stats;
import org.mmtk.vm.VM;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;

/**
 * This class implements <i>per-collector thread</i> behavior and state for
 * the <code>GenMS</code> two-generational copying collector.
 * <p>
 *
 * Specifically, this class defines semantics specific to the collection of
 * the mature generation (<code>GenCollector</code> defines nursery semantics).
 * In particular the mature space allocator is defined (for collection-time
 * allocation into the mature space), and the mature space per-collector thread
 * collection time semantics are defined.
 * <p>
 *
 * @see MementoV5 for a description of the <code>GenMS</code> algorithm.
 *
 * @see MementoV5
 * @see MementoV5Mutator
 * @see GenCollector
 * @see org.mmtk.plan.StopTheWorldCollector
 * @see org.mmtk.plan.CollectorContext
 */
@Uninterruptible
public class MementoV5Collector extends GenCopyCollector {

  /*****************************************************************************
   *
   * Instance fields
   */

  /** The allocator for the mature space */
  private final MarkSweepLocal oldGen;
  private final MementoV5TraceLocal oldGenTrace;

  /**
   * Constructor
   */
  public MementoV5Collector() {
    oldGen = new MarkSweepLocal(MementoV5.msSpace);
    oldGenTrace = new MementoV5TraceLocal(MementoV5.SCAN_OLD_GEN, global().oldGenTrace, this);
  }

  /****************************************************************************
   * Collection-time allocation
   */

  /**
   * {@inheritDoc}
   */
  @Inline
  @Override
  public final Address allocCopy(ObjectReference original, int bytes,
      int align, int offset, int allocator) {
    if (Stats.GATHER_MARK_CONS_STATS) {
      if (Space.isInSpace(MementoV5.NURSERY, original))
        MementoV5.nurseryMark.inc(bytes);
    }

    if (allocator == Plan.ALLOC_LOS) {
      if (VM.VERIFY_ASSERTIONS)
        VM.assertions._assert(Allocator.getMaximumAlignedSize(bytes, align) > Plan.MAX_NON_LOS_COPY_BYTES);
      return los.alloc(bytes, align, offset);
    } else {
      if (VM.VERIFY_ASSERTIONS) {
        VM.assertions._assert(bytes <= Plan.MAX_NON_LOS_COPY_BYTES);
        VM.assertions._assert(allocator == MementoV5.ALLOC_MATURE_MINORGC ||
            allocator == MementoV5.ALLOC_MATURE_MAJORGC || allocator == MementoV5.ALLOC_OLD_GEN);
      }
      if (allocator == MementoV5.ALLOC_OLD_GEN) {
        global().mementoLog("[MementoV5Collector.java] Collector copyAlloc Allocator: ");
        global().mementoLog(allocator);
        global().mementoLog("\n");
        return oldGen.alloc(bytes, align, offset);
      }
      return super.allocCopy(original, bytes, align, offset, allocator);
    }
  }

  @Inline
  @Override
  public final void postCopy(ObjectReference object, ObjectReference typeRef,
      int bytes, int allocator) {
    if (allocator == MementoV5.ALLOC_OLD_GEN) {
      MementoV5.msSpace.postCopy(object, allocator == MementoV5.ALLOC_OLD_GEN);
    }
    super.postCopy(object, typeRef, bytes, allocator);
  }

  /*****************************************************************************
   *
   * Collection
   */

  /**
   * {@inheritDoc}
   */
  @Override
  @NoInline
  public void collectionPhase(short phaseId, boolean primary) {
    global().msSpace.printUsageMB();
    if (false && global().traceOldGen()) {
      if (phaseId == MementoV5.PREPARE) {
        super.collectionPhase(phaseId, primary);
        oldGenTrace.prepare();
        if (global().traceOldGen() && global().gcFullHeap)
          oldGen.prepare();
        return;
      }

      if (phaseId == MementoV5.CLOSURE) {
        oldGenTrace.completeTrace();
        return;
      }

      if (phaseId == MementoV5.RELEASE) {
        global().msSpace.printUsageMB();
        oldGenTrace.release();
        if (global().traceOldGen() && global().gcFullHeap) {
          oldGen.release();
        }
        super.collectionPhase(phaseId, primary);
        return;
      }
    }

    super.collectionPhase(phaseId, primary);
  }

  @Override
  @Inline
  public final TraceLocal getFullHeapTrace() {
    return super.getFullHeapTrace();
  }

  /****************************************************************************
   *
   * Miscellaneous
   */

  /** @return The active global plan as a <code>GenMS</code> instance. */
  @Inline
  private static MementoV5 global() {
    return (MementoV5) VM.activePlan.global();
  }
}
