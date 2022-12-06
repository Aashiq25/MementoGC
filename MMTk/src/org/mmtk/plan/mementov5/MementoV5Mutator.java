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

import org.mmtk.plan.generational.GenMutator;
import org.mmtk.plan.generational.copying.GenCopyMutator;
import org.mmtk.policy.MarkSweepLocal;
import org.mmtk.policy.Space;
import org.mmtk.utility.alloc.Allocator;
import org.mmtk.vm.VM;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;

/**
 * This class implements <i>per-mutator thread</i> behavior and state for
 * the <code>GenMS</code> two-generational copying collector.<p>
 *
 * Specifically, this class defines mutator-time semantics specific to the
 * mature generation (<code>GenMutator</code> defines nursery semantics).
 * In particular the mature space allocator is defined (for mutator-time
 * allocation into the mature space via pre-tenuring), and the mature space
 * per-mutator thread collection time semantics are defined (rebinding
 * the mature space allocator).<p>
 *
 * See {@link MementoV5} for a description of the <code>GenMS</code> algorithm.
 *
 * @see MementoV5
 * @see MementoV5Collector
 * @see GenMutator
 * @see org.mmtk.plan.StopTheWorldMutator
 * @see org.mmtk.plan.MutatorContext
 */
@Uninterruptible
public class MementoV5Mutator extends GenCopyMutator {
  /******************************************************************
   * Instance fields
   */

  /**
   * The allocator for the mark-sweep mature space (the mutator may
   * "pretenure" objects into this space which is otherwise used
   * only by the collector)
   */
  private final MarkSweepLocal mature;


  /****************************************************************************
   *
   * Initialization
   */

  /**
   * Constructor
   */
  public MementoV5Mutator() {
    mature = new MarkSweepLocal(MementoV5.msSpace);
  }

  /****************************************************************************
   *
   * Mutator-time allocation
   */

  /**
   * {@inheritDoc}
   */
  @Override
  @Inline
  public final Address alloc(int bytes, int align, int offset, int allocator, int site) {
    if (allocator == MementoV5.ALLOC_OLD_GEN) {
      return mature.alloc(bytes, align, offset);
    }
    return super.alloc(bytes, align, offset, allocator, site);
  }

  @Override
  @Inline
  public final void postAlloc(ObjectReference ref, ObjectReference typeRef,
      int bytes, int allocator) {
    if (allocator == MementoV5.ALLOC_OLD_GEN) {
      MementoV5.msSpace.initializeHeader(ref, true);
    } else {
      super.postAlloc(ref, typeRef, bytes, allocator);
    }
  }

  @Override
  public Allocator getAllocatorFromSpace(Space space) {
    if (space == MementoV5.msSpace) return mature;
    return super.getAllocatorFromSpace(space);
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
    if (global().traceFullHeap()) {
      if (phaseId == MementoV5.PREPARE) {
        super.collectionPhase(phaseId, primary);
        if (global().gcFullHeap) mature.prepare();
        return;
      }

      if (phaseId == MementoV5.RELEASE) {
        if (global().gcFullHeap) mature.release();
        super.collectionPhase(phaseId, primary);
        return;
      }
    }

    super.collectionPhase(phaseId, primary);
  }

  @Override
  public void flush() {
    super.flush();
    mature.flush();
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
