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

import org.mmtk.plan.generational.Gen;
import org.mmtk.plan.generational.GenCollector;
import org.mmtk.plan.Plan;
import org.mmtk.plan.TraceLocal;
import org.mmtk.policy.CopyLocal;
import org.mmtk.policy.MarkSweepLocal;
import org.mmtk.policy.Space;
import org.mmtk.utility.ForwardingWord;
import org.mmtk.utility.HeaderByte;
import org.mmtk.utility.Log;
import org.mmtk.utility.alloc.Allocator;
import org.mmtk.vm.VM;

import org.vmmagic.unboxed.*;
import org.vmmagic.pragma.*;

/**
 * This class implements <i>per-collector thread</i> behavior and state for
 * the <code>GenCopy</code> two-generational copying collector.<p>
 *
 * Specifically, this class defines semantics specific to the collection of
 * the mature generation (<code>GenCollector</code> defines nursery semantics).
 * In particular the mature space allocator is defined (for collection-time
 * allocation into the mature space), and the mature space per-collector thread
 * collection time semantics are defined.<p>
 *
 * @see MementoV4 for a description of the <code>GenCopy</code> algorithm.
 *
 * @see MementoV4
 * @see MementoV4Mutator
 * @see GenCollector
 * @see org.mmtk.plan.StopTheWorldCollector
 * @see org.mmtk.plan.CollectorContext
 */
@Uninterruptible
public class MementoV4Collector extends GenCollector {

  /******************************************************************
   * Instance fields
   */

  /** The allocator for the mature space */
  private final CopyLocal mature;
  
  private final MarkSweepLocal oldGen;

  /** The trace object for full-heap collections */
  private final MementoV4MatureTraceLocal matureTrace;

  /****************************************************************************
   *
   * Initialization
   */

  /**
   * Constructor
   */
  public MementoV4Collector() {
    mature = new CopyLocal(MementoV4.survivorSpace);
    oldGen = new MarkSweepLocal(MementoV4.oldGenSpace);
    matureTrace = new MementoV4MatureTraceLocal(global().matureTrace, this);
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
  public Address allocCopy(ObjectReference original, int bytes,
      int align, int offset, int allocator) {
    if (allocator == Plan.ALLOC_LOS) {
      if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(Allocator.getMaximumAlignedSize(bytes, align) > Plan.MAX_NON_LOS_COPY_BYTES);
      return los.alloc(bytes, align, offset);
    } else {
      if (VM.VERIFY_ASSERTIONS) {
        VM.assertions._assert(bytes <= Plan.MAX_NON_LOS_COPY_BYTES);
        VM.assertions._assert(allocator == MementoV4.ALLOC_MATURE_MINORGC ||
            allocator == MementoV4.ALLOC_MATURE_MAJORGC);
      }
      if (allocator == MementoV4.ALLOC_MATURE_MAJORGC) {
      	return oldGen.alloc(bytes, align, offset);
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
    else if (Space.isInSpace(MementoV4.OLDGEN, object)) {
    	MementoV4.oldGenSpace.postCopy(object, allocator == MementoV4.ALLOC_MATURE_MAJORGC);
    }
    else if (MementoV4.IGNORE_REMSETS)
      MementoV4.immortalSpace.traceObject(getCurrentTrace(), object); // FIXME this does not look right
    if (Gen.USE_OBJECT_BARRIER)
      HeaderByte.markAsUnlogged(object);
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
  	mature.getSpace().printUsageMB();
    if (global().traceFullHeap()) {
      if (phaseId == MementoV4.PREPARE) {
      	matureTrace.prepare();
      	oldGen.prepare();
        super.collectionPhase(phaseId, primary);
      }
      if (phaseId == MementoV4.CLOSURE) {
        matureTrace.completeTrace();
        return;
      }
      if (phaseId == MementoV4.RELEASE) {
        matureTrace.release();
        super.collectionPhase(phaseId, primary);
        mature.getSpace().printUsageMB();
        return;
      }
    }
    
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

  /** Show the status of the mature allocator. */
  protected final void showMature() {
    mature.show();
  }

  @Override
  public final TraceLocal getFullHeapTrace() {
    return matureTrace;
  }
}
