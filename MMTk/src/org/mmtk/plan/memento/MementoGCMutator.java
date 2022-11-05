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

import org.mmtk.plan.generational.GenMutator;
import org.mmtk.policy.CopyLocal;
import org.mmtk.policy.Space;
import org.mmtk.utility.alloc.Allocator;
import org.mmtk.vm.VM;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;

/**
 * This class implements <i>per-mutator thread</i> behavior and state
 * for the <i>NoGC</i> plan, which simply allocates (without ever collecting
 * until the available space is exhausted.<p>
 *
 * Specifically, this class defines <i>NoGC</i> mutator-time allocation
 * through a bump pointer (<code>def</code>) and includes stubs for
 * per-mutator thread collection semantics (since there is no collection
 * in this plan, these remain just stubs).
 *
 * @see MementoGC
 * @see MementoGCCollector
 * @see org.mmtk.plan.StopTheWorldMutator
 * @see MutatorContext
 */
@Uninterruptible
public class MementoGCMutator extends GenMutator {

  /************************************************************************
   * Instance fields
   */

  /**
   *
   */
  protected final CopyLocal mature;


  public MementoGCMutator() {
    mature = new CopyLocal();
  }

  @Override
  public void initMutator(int id) {
    super.initMutator(id);
    mature.rebind(MementoGC.toSpace());
  }

  /****************************************************************************
   * Mutator-time allocation
   */

  /**
   * {@inheritDoc}
   */
  @Inline
  @Override
  public Address alloc(int bytes, int align, int offset, int allocator, int site) {
    if (allocator == MementoGC.ALLOC_DEFAULT) {
      return nursery.alloc(bytes, align, offset);
    }else if (allocator == MementoGC.ALLOC_MATURE){
      return mature.alloc(bytes, align, offset);
    }
    return super.alloc(bytes, align, offset, allocator, site);
  }

  @Inline
  @Override
  public void postAlloc(ObjectReference ref, ObjectReference typeRef,
      int bytes, int allocator) {
        if (allocator == MementoGC.ALLOC_MATURE || allocator == MementoGC.ALLOC_DEFAULT ) return;
        super.postAlloc(ref, typeRef, bytes, allocator);
  }

  @Override
  public Allocator getAllocatorFromSpace(Space space) {
    if (space == MementoGC.nurserySpace) return nursery;
    else if (space == MementoGC.nurserySpace) return mature;
    return super.getAllocatorFromSpace(space);
  }


  /****************************************************************************
   * Collection
   */

  /**
   * {@inheritDoc}
   */
  @Inline
  @Override
  public final void collectionPhase(short phaseId, boolean primary) {
    VM.assertions.fail("GC Triggered in MementoGC Plan.");
    if (global().traceFullHeap()) {
      if (phaseId == MementoGC.RELEASE) {
        super.collectionPhase(phaseId, primary);
        if (global().gcFullHeap) mature.rebind(MementoGC.toSpace());
        return;
      }
    }

    super.collectionPhase(phaseId, primary);
  }

  private static MementoGC global() {
    return (MementoGC) VM.activePlan.global();
  }
}
