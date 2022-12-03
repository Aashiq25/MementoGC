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

import org.mmtk.policy.CopySpace;
import org.mmtk.policy.Space;
import org.mmtk.plan.generational.*;
import org.mmtk.plan.Trace;
import org.mmtk.plan.TransitiveClosure;
import org.mmtk.utility.Log;
import org.mmtk.utility.heap.VMRequest;
import org.mmtk.vm.VM;

import org.vmmagic.pragma.*;

/**
 * This class implements the functionality of a standard
 * two-generation copying collector.  Nursery collections occur when
 * either the heap is full or the nursery is full.  The nursery size
 * is determined by an optional command line argument.  If undefined,
 * the nursery size is "infinite", so nursery collections only occur
 * when the heap is full (this is known as a flexible-sized nursery
 * collector).  Thus both fixed and flexible nursery sizes are
 * supported.  Full heap collections occur when the nursery size has
 * dropped to a statically defined threshold,
 * <code>NURSERY_THRESHOLD</code>.<p>
 *
 * See the Jones &amp; Lins GC book, chapter 7 for a detailed discussion
 * of generational collection and section 7.3 for an overview of the
 * flexible nursery behavior ("The Standard ML of New Jersey
 * collector"), or go to Appel's paper "Simple generational garbage
 * collection and fast allocation." SP&amp;E 19(2):171--183, 1989.<p>
 *
 * All plans make a clear distinction between <i>global</i> and
 * <i>thread-local</i> activities.  Global activities must be
 * synchronized, whereas no synchronization is required for
 * thread-local activities.  Instances of Plan map 1:1 to "kernel
 * threads" (aka CPUs).  Thus instance
 * methods allow fast, unsychronized access to Plan utilities such as
 * allocation and collection.  Each instance rests on static resources
 * (such as memory and virtual memory resources) which are "global"
 * and therefore "static" members of Plan.  This mapping of threads to
 * instances is crucial to understanding the correctness and
 * performance properties of this plan.
 */
@Uninterruptible public class MementoV4 extends Gen {

  /****************************************************************************
   *
   * Class variables
   */

  // GC state

  /**
   * <code>true</code> if copying to "higher" semispace
   */

  /**
   * The low half of the copying mature space.  We allocate into this space
   * when <code>hi</code> is <code>false</code>.
   */
  static CopySpace survivorSpace = new CopySpace("survivor", false, VMRequest.highFraction(0.1f));
  static final int SURVIVOR = survivorSpace.getDescriptor();
  

  /**
   * The high half of the copying mature space. We allocate into this space
   * when <code>hi</code> is <code>true</code>.
   */
  static CopySpace oldGenSpace = new CopySpace("oldgen", false, VMRequest.highFraction(0.2f));
  static final int OLDGEN = oldGenSpace.getDescriptor();


  /****************************************************************************
   *
   * Instance fields
   */

  /**
   *
   */
  final Trace matureTrace;

  /**
   * Constructor
   */
  public MementoV4() {
    super();
    if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(!IGNORE_REMSETS); // Not supported for GenCopy
    matureTrace = new Trace(metaDataSpace);
  }

  /**
   * @return The semispace we are currently allocating into
   */
  static CopySpace toSpace() {
    return oldGenSpace;
  }

  /**
   * @return Space descriptor for to-space.
   */
  static int toSpaceDesc() {
    return OLDGEN;
  }

  /**
   * @return The semispace we are currently copying from
   * (or copied from at last major GC)
   */
  static CopySpace fromSpace() {
    return survivorSpace;
  }

  /**
   * @return Space descriptor for from-space
   */
  static int fromSpaceDesc() {
    return SURVIVOR;
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
  public void collectionPhase(short phaseId) {
    if (traceFullHeap()) {
    	Log.writeln("Coming intp Mv4 cp");
      if (phaseId == PREPARE) {
        super.collectionPhase(phaseId);
//        hi = !hi; // flip the semi-spaces
        survivorSpace.prepare(true);
        oldGenSpace.prepare(false);
        matureTrace.prepare();
        return;
      }
      if (phaseId == CLOSURE) {
        matureTrace.prepare();
        return;
      }
      if (phaseId == RELEASE) {
      	Log.writeln("MV4 CP Release");
        matureTrace.release();
        survivorSpace.release();
//        switchNurseryZeroingApproach(survivorSpace);
        super.collectionPhase(phaseId);
        return;
      }
    }
    super.collectionPhase(phaseId);
  }
  
  @Override
  public final boolean collectionRequired(boolean spaceFull, Space space) {
  	
  	
  	Log.writeln("Collection required for Mementov4");
  	Log.write("Memory usage: ");
  	nurserySpace.printUsageMB();
  	
  	return super.collectionRequired(spaceFull, space);
  }

  /*****************************************************************************
   *
   * Accounting
   */

  /**
   * Return the number of pages reserved for use given the pending
   * allocation.
   */
  @Override
  @Inline
  public int getPagesUsed() {
    return fromSpace().reservedPages() + super.getPagesUsed();
  }

  /**
   * Return the number of pages reserved for copying.
   *
   * @return the number of pages reserved for copying.
   */
  @Override
  public final int getCollectionReserve() {
    // we must account for the number of pages required for copying,
    // which equals the number of semi-space pages reserved
    return fromSpace().reservedPages() + super.getCollectionReserve();
  }

  @Override
  public int getMaturePhysicalPagesAvail() {
    return fromSpace().availablePhysicalPages() >> 1;
  }

  /**************************************************************************
   * Miscellaneous methods
   */

  /**
   * @return The mature space we are currently allocating into
   */
  @Override
  @Inline
  public Space activeMatureSpace() {
    return survivorSpace;
  }

  @Override
  @Interruptible
  protected void registerSpecializedMethods() {
    TransitiveClosure.registerSpecializedScan(SCAN_MATURE, MementoV4MatureTraceLocal.class);
    super.registerSpecializedMethods();
  }
}
