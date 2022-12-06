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

import org.mmtk.plan.StopTheWorld;
import org.mmtk.plan.Trace;
import org.mmtk.plan.TransitiveClosure;
import org.mmtk.plan.generational.copying.GenCopy;
import org.mmtk.policy.MarkSweepSpace;
import org.mmtk.policy.Space;
import org.mmtk.utility.heap.VMRequest;
import org.mmtk.utility.options.Options;
import org.mmtk.vm.VM;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;

/**
 * This class implements the functionality of a two-generation copying
 * collector where <b>the higher generation is a mark-sweep space</b>
 * (free list allocation, mark-sweep collection).  Nursery collections
 * occur when either the heap is full or the nursery is full.  The
 * nursery size is determined by an optional command line argument.
 * If undefined, the nursery size is "infinite", so nursery
 * collections only occur when the heap is full (this is known as a
 * flexible-sized nursery collector).  Thus both fixed and flexible
 * nursery sizes are supported.  Full heap collections occur when the
 * nursery size has dropped to a statically defined threshold,
 * <code>NURSERY_THRESHOLD</code><p>
 *
 * See the Jones &amp; Lins GC book, chapter 7 for a detailed discussion
 * of generational collection and section 7.3 for an overview of the
 * flexible nursery behavior ("The Standard ML of New Jersey
 * collector"), or go to Appel's paper "Simple generational garbage
 * collection and fast allocation." SP&amp;E 19(2):171--183, 1989.<p>
 *
 *
 * For general comments about the global/local distinction among classes refer
 * to Plan.java and PlanLocal.java.
 */
@Uninterruptible
public class MementoV5 extends GenCopy {

  /*****************************************************************************
   *
   * Class fields
   */

  /** The mature space, which for GenMS uses a mark sweep collection policy. */
  public static final MarkSweepSpace msSpace = new MarkSweepSpace("ms", VMRequest.discontiguous());

  public static final int MS = msSpace.getDescriptor();

  public static final int ALLOC_OLD_GEN = StopTheWorld.ALLOCATORS + 4;

  public static final int SCAN_OLD_GEN  = 2;
  
  /****************************************************************************
   *
   * Instance fields
   */
  
  public MementoV5() {
    super();
    Options.noReferenceTypes.setDefaultValue(true);
  }
  
  /** The trace class for a full-heap collection */
  public final Trace oldGenTrace = new Trace(metaDataSpace);

  /*****************************************************************************
   *
   * Collection
   */

  /**
   * {@inheritDoc}
   */
  @Inline
  @Override
  public final void collectionPhase(short phaseId) {
  	msSpace.printUsageMB();
    if (traceOldGen()) {
      if (phaseId == PREPARE) {
        super.collectionPhase(phaseId);
        oldGenTrace.prepare();
        msSpace.prepare(true);
        return;
      }

      if (phaseId == CLOSURE) {
        oldGenTrace.prepare();
        return;
      }
      if (phaseId == RELEASE) {
        oldGenTrace.release();
        msSpace.release();
        super.collectionPhase(phaseId);
        return;
      }
    }
    super.collectionPhase(phaseId);
  }

  @Inline
  public boolean traceOldGen() {
    return traceFullHeap() && msSpace.reservedPages() > 10;
  }

  /*****************************************************************************
   *
   * Accounting
   */

  /**
   * Return the number of pages reserved for use given the pending
   * allocation.
   */
  @Inline
  @Override
  public int getPagesUsed() {
    return msSpace.reservedPages() + super.getPagesUsed();
  }


  /*****************************************************************************
   *
   * Miscellaneous
   */

  @Override
  public boolean willNeverMove(ObjectReference object) {
    if (Space.isInSpace(MS, object))
      return true;
    if (Space.isInSpace(GenCopy.toSpaceDesc(), object)) {
      return false;
    }
    return super.willNeverMove(object);
  }

  @Override
  @Interruptible
  protected void registerSpecializedMethods() {
    TransitiveClosure.registerSpecializedScan(SCAN_OLD_GEN, MementoV5TraceLocal.class);
    super.registerSpecializedMethods();
  }
}
