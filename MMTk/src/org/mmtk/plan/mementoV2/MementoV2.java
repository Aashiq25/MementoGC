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
package org.mmtk.plan.mementoV2;

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
public class MementoV2 extends StopTheWorld {

  /****************************************************************************
   * Constants
   */

  /****************************************************************************
   * Class variables
   */

  /**
   *
   */
  public static final CopySpace edenSpace1 = new CopySpace("eden1", false, VMRequest.highFraction(0.20f));
  public static final CopySpace edenSpace2 = new CopySpace("eden2", false, VMRequest.highFraction(0.20f));
  public static final CopySpace survivorSpace = new CopySpace("survivor", false, VMRequest.highFraction(0.40f));

  public static final int EDEN1 = edenSpace1.getDescriptor();
  public static final int EDEN2 = edenSpace2.getDescriptor();
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
  public MementoV2() {
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
      edenSpace1.prepare(true);
      edenSpace2.prepare(true);
      return;
    }
    if (phaseId == CLOSURE) {
      trace.prepare();
      return;
    }
    if (phaseId == RELEASE) {
      trace.release();
      survivorSpace.release();
      edenSpace1.release();
      edenSpace2.release();
      switchNurseryZeroingApproach(edenSpace1);
      switchNurseryZeroingApproach(edenSpace2);
      super.collectionPhase(phaseId);
      return;
    }

    super.collectionPhase(phaseId);
  }

  @Override
  public final boolean collectionRequired(boolean spaceFull, Space space) {
	Log.writeln("Collection required for Memento");
	Log.write("Eden 1 usage: ");
	edenSpace1.printUsageMB();
	Log.write("Eden 2 usage: ");
	edenSpace2.printUsageMB();
    boolean nurseryFull = (edenSpace1.reservedPages() + edenSpace2.reservedPages()) > (2 * 500
//    		Options.nurserySize.getMaxNursery()
    		);
    Log.write("Is nursery full: ");
    Log.writeln(nurseryFull);
    Log.write(" Space full: ");
    Log.write(spaceFull);
    boolean returnVal = super.collectionRequired(spaceFull, space);
    Log.write(" ReturnVal: ");
    Log.write(returnVal);
    return returnVal || nurseryFull;
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
      edenSpace1.reservedPages() + edenSpace2.reservedPages();
  }

  /**
   * Return the number of pages reserved for collection.
   * For mark sweep this is a fixed fraction of total pages.
   */
  @Override
  public int getCollectionReserve() {
    return edenSpace1.reservedPages() + edenSpace2.reservedPages() + super.getCollectionReserve();
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
    if (space == MementoV2.edenSpace1 || space == MementoV2.edenSpace2) {
      return SanityChecker.DEAD;
    }

    return space.isReachable(object) ? SanityChecker.ALIVE : SanityChecker.DEAD;
  }

  @Override
  @Interruptible
  protected void registerSpecializedMethods() {
    TransitiveClosure.registerSpecializedScan(SCAN_MEMENTO, MementoV2TraceLocal.class);
    super.registerSpecializedMethods();
  }

  @Interruptible
  @Override
  public void fullyBooted() {
    super.fullyBooted();
    edenSpace1.setZeroingApproach(Options.nurseryZeroing.getNonTemporal(), Options.nurseryZeroing.getConcurrent());
    edenSpace2.setZeroingApproach(Options.nurseryZeroing.getNonTemporal(), Options.nurseryZeroing.getConcurrent());

  }
}
