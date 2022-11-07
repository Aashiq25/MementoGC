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
import org.mmtk.policy.ImmortalSpace;
import org.mmtk.policy.CopySpace;
import org.mmtk.utility.heap.VMRequest;
import org.mmtk.vm.VM;
import org.vmmagic.pragma.*;


/**
 * This class implements the global state of a a simple allocator
 * without a collector.
 */
@Uninterruptible
public class MementoGC extends StopTheWorld {

  /*****************************************************************************
   * Class variables
   */

  /** Switch between a contiguous and discontiguous nursery (experimental) */
  static final boolean USE_DISCONTIGUOUS_NURSERY = false;

  /**
   * Fraction of available virtual memory to give to the nursery (if contiguous)
   */
  protected static final float NURSERY_VM_FRACTION = 0.15f;


  public static final int ALLOC_MATURE = StopTheWorld.ALLOCATORS + 1;


  /* The nursery space is where all new objects are allocated by default */
  private static final VMRequest vmRequest = USE_DISCONTIGUOUS_NURSERY ? VMRequest.discontiguous()
      : VMRequest.highFraction(NURSERY_VM_FRACTION);
  public static final CopySpace nurserySpace = new CopySpace("nursery", false, vmRequest);
  
  public static final int NURSERY = nurserySpace.getDescriptor();

  static CopySpace matureSpace = new CopySpace("ss", false, VMRequest.discontiguous());
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
    /*
    if (phaseId == PREPARE) {
    }
    if (phaseId == CLOSURE) {
    }
    if (phaseId == RELEASE) {
    }
    super.collectionPhase(phaseId);
    */
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
    return (nurserySpace.reservedPages() + super.getPagesUsed());
  }


  /*****************************************************************************
   * Miscellaneous
   */

  /**
   * {@inheritDoc}
   */
  @Interruptible
  @Override
  protected void registerSpecializedMethods() {
    super.registerSpecializedMethods();
  }
}
