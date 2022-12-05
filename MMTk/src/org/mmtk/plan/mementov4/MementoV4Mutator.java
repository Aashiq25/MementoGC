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
package org.mmtk.plan.mementov4;

import org.mmtk.plan.generational.GenMutator;
import org.mmtk.policy.CopyLocal;
import org.mmtk.policy.MarkSweepLocal;
import org.mmtk.policy.Space;
import org.mmtk.utility.Log;
import org.mmtk.utility.alloc.Allocator;
import org.mmtk.vm.VM;

import org.vmmagic.unboxed.*;
import org.vmmagic.pragma.*;

/**
 * This class implements <i>per-mutator thread</i> behavior and state for
 * the <code>GenCopy</code> two-generational copying collector.<p>
 *
 * Specifically, this class defines mutator-time semantics specific to the
 * mature generation (<code>GenMutator</code> defines nursery semantics).
 * In particular the mature space allocator is defined (for mutator-time
 * allocation into the mature space via pre-tenuring), and the mature space
 * per-mutator thread collection time semantics are defined (rebinding
 * the mature space allocator).<p>
 *
 * @see MementoV4 for a description of the <code>GenCopy</code> algorithm.
 *
 * @see MementoV4
 * @see MementoV4Collector
 * @see GenMutator
 * @see org.mmtk.plan.StopTheWorldMutator
 * @see org.mmtk.plan.MutatorContext
 */
@Uninterruptible
public class MementoV4Mutator extends GenMutator {
  /******************************************************************
   * Instance fields
   */

  /**
   * The allocator for the copying mature space (the mutator may
   * "pretenure" objects into this space otherwise used only by
   * the collector)
   */
  private final CopyLocal mature;
  
  private final MarkSweepLocal oldGen;

  /****************************************************************************
   *
   * Initialization
   */

  /**
   * Constructor
   */
  public MementoV4Mutator() {
    mature = new CopyLocal();
    oldGen = new MarkSweepLocal(MementoV4.oldGenSpace);
  }

  /**
   * Called before the MutatorContext is used, but after the context has been
   * fully registered and is visible to collection.
   */
  @Override
  public void initMutator(int id) {
    super.initMutator(id);
    mature.rebind(MementoV4.survivorSpace);
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
    if (allocator == MementoV4.ALLOC_MATURE) {
    	Log.writeln("Allocating mature");
      return mature.alloc(bytes, align, offset);
    }
    return super.alloc(bytes, align, offset, allocator, site);
  }

  @Override
  @Inline
  public final void postAlloc(ObjectReference object, ObjectReference typeRef,
      int bytes, int allocator) {
    // nothing to be done
    if (allocator == MementoV4.ALLOC_MATURE) return;
    super.postAlloc(object, typeRef, bytes, allocator);
  }

  @Override
  public final Allocator getAllocatorFromSpace(Space space) {
  	Log.write("AM I used? : getAllocatorFromSpace Space name: ");
  	Log.writeln(space.getName());
    if (space == MementoV4.survivorSpace) return mature;
    if (space == MementoV4.oldGenSpace) return oldGen;
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
  public void collectionPhase(short phaseId, boolean primary) {
  	Log.write("[MC4] Collection phase phaseId: ");
  	Log.writeln(phaseId);
//    if (global().traceFullHeap()) {
//    		if (phaseId == MementoV4.PREPARE) {
//    			super.collectionPhase(phaseId, primary);
//    			if(global().gcFullHeap) {
//    				oldGen.prepare();
//    			}
//    		}
//      if (phaseId == MementoV4.RELEASE) {
//        super.collectionPhase(phaseId, primary);
		/*
		 * //// if (global().gcFullHeap) { //// Log.writeln("CP Mut OG"); ////
		 * oldGen.release(); //// }
		 *///        return;
//      }
//    }

    super.collectionPhase(phaseId, primary);
  }

  /*****************************************************************************
   *
   * Miscellaneous
   */

  /** @return The active global plan as a <code>GenCopy</code> instance. */
  private static MementoV4 global() {
    return (MementoV4) VM.activePlan.global();
  }

}
