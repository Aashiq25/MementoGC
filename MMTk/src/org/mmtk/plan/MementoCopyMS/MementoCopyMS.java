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
import org.mmtk.policy.CopySpace;
import org.mmtk.policy.MarkSweepSpace;
import org.mmtk.policy.Space;
import org.mmtk.utility.Log;
import org.mmtk.utility.heap.VMRequest;
import org.mmtk.utility.options.Options;
import org.mmtk.utility.sanitychecker.SanityChecker;

import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.ObjectReference;

/**
 * This class implements the global state of a full-heap collector
 * with a copying nursery and mark-sweep mature space.  Unlike a full
 * generational collector, there is no write barrier, no remembered set, and
 * every collection is full-heap.<p>
 *
 * All plans make a clear distinction between <i>global</i> and
 * <i>thread-local</i> activities, and divides global and local state
 * into separate class hierarchies.  Global activities must be
 * synchronized, whereas no synchronization is required for
 * thread-local activities.  There is a single instance of Plan (or the
 * appropriate sub-class), and a 1:1 mapping of PlanLocal to "kernel
 * threads" (aka CPUs).  Thus instance
 * methods of PlanLocal allow fast, unsychronized access to functions such as
 * allocation and collection.<p>
 *
 * The global instance defines and manages static resources
 * (such as memory and virtual memory resources).  This mapping of threads to
 * instances is crucial to understanding the correctness and
 * performance properties of MMTk plans.
 */
@Uninterruptible
public class MementoCopyMS extends StopTheWorld {

  /****************************************************************************
   * Constants
   */

  /****************************************************************************
   * Class variables
   */

  /**
   *
   */
  public static final CopySpace edenSpace = new CopySpace("eden", false, VMRequest.highFraction(0.10f));
  public static final MarkSweepSpace survivorSpace = new MarkSweepSpace("survivor", VMRequest.discontiguous());

  public static final int EDEN = edenSpace.getDescriptor();
  public static final int SURVIVOR = survivorSpace.getDescriptor();

  public static final int ALLOC_EDEN = ALLOC_DEFAULT;
  public static final int ALLOC_SURVIVOR = StopTheWorld.ALLOCATORS + 1;

  public static final int SCAN_MEMENTO = 0;

  /****************************************************************************
   * Instance variables
   */

  /**
   *
   */
  public final Trace trace;

  /**
   * Constructor.
 */
  public MementoCopyMS() {
    trace = new Trace(metaDataSpace);
  }


  /*****************************************************************************
   * Collection
   */

  /**
   * {@inheritDoc}
   */
  @Override
  @Inline
  public final void collectionPhase(short phaseId) {
    if (phaseId == PREPARE) {
      super.collectionPhase(phaseId);
      trace.prepare();
      survivorSpace.prepare(true);
      edenSpace.prepare(true);
      return;
    }
    if (phaseId == CLOSURE) {
      trace.prepare();
      return;
    }
    if (phaseId == RELEASE) {
      trace.release();
      survivorSpace.release();
      edenSpace.release();
      switchNurseryZeroingApproach(edenSpace);
      super.collectionPhase(phaseId);
      return;
    }

    super.collectionPhase(phaseId);
  }

  @Override
  public final boolean collectionRequired(boolean spaceFull, Space space) {
	Log.writeln("Collection required for Memento");
	edenSpace.printUsageMB();
    boolean nurseryFull = edenSpace.reservedPages() > Options.nurserySize.getMaxNursery();

    return super.collectionRequired(spaceFull, space) || nurseryFull;
  }

  /*****************************************************************************
   *
   * Accounting
   */

  /**
   * {@inheritDoc}
   */
  @Override
  public int getPagesUsed() {
    return super.getPagesUsed() +
      survivorSpace.reservedPages() +
      edenSpace.reservedPages();
  }

  /**
   * Return the number of pages reserved for collection.
   * For mark sweep this is a fixed fraction of total pages.
   */
  @Override
  public int getCollectionReserve() {
    return edenSpace.reservedPages() + super.getCollectionReserve();
  }

  /**
   * @return The number of pages available for allocation, <i>assuming
   * all future allocation is to the nursery</i>.
   */
  @Override
  public final int getPagesAvail() {
    return (getTotalPages() - getPagesReserved()) >> 1;
  }

  @Override
  public int sanityExpectedRC(ObjectReference object, int sanityRootRC) {
    Space space = Space.getSpaceForObject(object);

    // Nursery
    if (space == MementoCopyMS.edenSpace) {
      return SanityChecker.DEAD;
    }

    return space.isReachable(object) ? SanityChecker.ALIVE : SanityChecker.DEAD;
  }

  @Override
  @Interruptible
  protected void registerSpecializedMethods() {
    TransitiveClosure.registerSpecializedScan(SCAN_MEMENTO, MementoCopyMSTraceLocal.class);
    super.registerSpecializedMethods();
  }

  @Interruptible
  @Override
  public void fullyBooted() {
    super.fullyBooted();
    edenSpace.setZeroingApproach(Options.nurseryZeroing.getNonTemporal(), Options.nurseryZeroing.getConcurrent());
  }
}
