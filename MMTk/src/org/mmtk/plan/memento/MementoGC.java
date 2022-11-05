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

import org.mmtk.plan.*;
import org.mmtk.plan.generational.*;
import org.mmtk.policy.CopySpace;
import org.mmtk.utility.heap.VMRequest;
import org.mmtk.vm.VM;
import org.mmtk.policy.Space;
import org.vmmagic.pragma.*;


/**
 * This class implements the global state of a a simple allocator
 * without a collector.
 */
@Uninterruptible
public class MementoGC extends Gen {

  /*****************************************************************************
   * Class variables
   */
  static boolean hi = false;
  /** Switch between a contiguous and discontiguous nursery (experimental) */
  static final boolean USE_DISCONTIGUOUS_NURSERY = false;

  /**
   * Fraction of available virtual memory to give to the nursery (if contiguous)
   */
  protected static final float NURSERY_VM_FRACTION = 0.15f;


  /* The nursery space is where all new objects are allocated by default */
  private static final VMRequest vmRequest = USE_DISCONTIGUOUS_NURSERY ? VMRequest.discontiguous()
      : VMRequest.highFraction(NURSERY_VM_FRACTION);
  public static CopySpace nurserySpace = new CopySpace("nursery", false, vmRequest);
  
  public static final int NURSERY = nurserySpace.getDescriptor();

  /**
   * The high half of the copying mature space. We allocate into this space
   * when <code>hi</code> is <code>true</code>.
   */
  static CopySpace matureSpace = new CopySpace("ms",true, VMRequest.discontiguous());
  static final int MS = matureSpace.getDescriptor();

  /**
   *
   */
  // public static final ImmortalSpace noGCSpace = new ImmortalSpace("default", VMRequest.discontiguous());
  // public static final int NOGC = noGCSpace.getDescriptor();


  /*****************************************************************************
   * Instance variables
   */

  /**
   *
   */
  public final Trace trace = new Trace(metaDataSpace);

  @Override
  protected boolean copyMature() {
    return true;
  }

  /**
   * @return The semispace we are currently allocating into
   */
  static CopySpace toSpace() {
    return hi ? nurserySpace : matureSpace;
  }

  /**
   * @return Space descriptor for to-space.
   */
  static int toSpaceDesc() {
    return hi ? NURSERY : MS;
  }

  /**
   * @return The semispace we are currently copying from
   * (or copied from at last major GC)
   */
  static CopySpace fromSpace() {
    return hi ?  nurserySpace : matureSpace;
  }

  /**
   * @return Space descriptor for from-space
   */
  static int fromSpaceDesc() {
    return hi ? NURSERY : MS;
  }
  /*****************************************************************************
   * Collection
   */

  /**
   * {@inheritDoc}
   */
  @Inline
  @Override
  public final void collectionPhase(short phaseId) {
    if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(false);
    
    if (traceFullHeap()) {
      if (phaseId == PREPARE) {
        super.collectionPhase(phaseId);
        hi = !hi; // flip the semi-spaces
        nurserySpace.prepare(hi);
        matureSpace.prepare(!hi);
        nurseryTrace.prepare();
        return;
      }
      if (phaseId == CLOSURE) {
        nurseryTrace.prepare();
        return;
      }
      if (phaseId == RELEASE) {
        nurseryTrace.release();
        fromSpace().release();
        super.collectionPhase(phaseId);
        return;
      }
    }
    super.collectionPhase(phaseId);
  }

  /*****************************************************************************
   * Accounting
   */

  /**
   * {@inheritDoc}
   * The superclass accounts for its spaces, we just
   * augment this with the default space's contribution.
   */
  @Override
  public int getPagesUsed() {
    return toSpace().reservedPages() + super.getPagesUsed();
  }

  @Override
  public final int getCollectionReserve() {
    // we must account for the number of pages required for copying,
    // which equals the number of semi-space pages reserved
    return toSpace().reservedPages() + super.getCollectionReserve();
  }

  @Override
  public int getMaturePhysicalPagesAvail() {
    return toSpace().availablePhysicalPages() >> 1;
  }
  /*****************************************************************************
   * Miscellaneous
   */

  @Override
  @Inline
  public Space activeMatureSpace() {
    return toSpace();
  }

  @Override
  @Interruptible
  protected void registerSpecializedMethods() {
    TransitiveClosure.registerSpecializedScan(SCAN_MATURE, MementoGCTraceLocal.class);
    super.registerSpecializedMethods();
  }
}
