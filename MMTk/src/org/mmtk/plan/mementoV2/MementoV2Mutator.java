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

import org.mmtk.plan.StopTheWorldMutator;
import org.mmtk.policy.CopyLocal;
import org.mmtk.policy.MarkSweepLocal;
import org.mmtk.policy.Space;
import org.mmtk.utility.Log;
import org.mmtk.utility.alloc.Allocator;
import org.mmtk.utility.options.Options;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;

/**
 * This class implements <i>per-mutator thread</i> behavior
 * and state for the <i>CopyMS</i> plan.<p>
 *
 * Specifically, this class defines <i>CopyMS</i> mutator-time
 * allocation into the nursery and mature space (through pre-tenuring).
 * Per-mutator thread collection semantics are also defined (flushing
 * and restoring per-mutator allocator state).
 *
 * @see MementoV2
 * @see MementoV2Collector
 * @see org.mmtk.plan.StopTheWorldMutator
 * @see org.mmtk.plan.MutatorContext
 */
@Uninterruptible
public class MementoV2Mutator extends StopTheWorldMutator {

  /****************************************************************************
   * Instance fields
   */

  /**
   *
   */
  private final MarkSweepLocal mature;
  private final CopyLocal nursery1, nursery2;

  /****************************************************************************
   *
   * Initialization
   */

  /**
   * Constructor
   */
  public MementoV2Mutator() {
    mature = new MarkSweepLocal(MementoV2.survivorSpace);
    nursery1 = new CopyLocal(MementoV2.edenSpace1);
    nursery2 = new CopyLocal(MementoV2.edenSpace2);
  }

  /****************************************************************************
   *
   * Mutator-time allocation
   */

  /**
   * {@inheritDoc}<p>
   *
   * This class handles the default allocator from the mark sweep space,
   * and delegates everything else to the superclass.
   */
  @Override
  @Inline
  public Address alloc(int bytes, int align, int offset, int allocator, int site) {
    Log.write("Alloc Bytes: ");
    Log.write(bytes);
    Log.write(" Allocator: ");
    Log.write(allocator);
    Log.writeln();
    if (allocator == MementoV2.ALLOC_DEFAULT) {
    	Log.write("Nursery 1 available physical page ");
    	Log.write(nursery1.getSpace().availablePhysicalPages());
    	Log.write(" Nursery 1 reservered page");
    	Log.write(nursery1.getSpace().reservedPages());
    	Log.writeln();
    	Log.write("Nursery 1 maxNursery: ");
    	Log.write( Options.nurserySize.getMaxNursery());
    	Log.writeln();	if (nursery1.getSpace().reservedPages() > 2000) {
    		Log.writeln(" Allocating in Nursery 2");
    		return nursery2.alloc(bytes, align, offset);
    	}
      Log.writeln(" Allocating in Nursery 1");
      return nursery1.alloc(bytes, align, offset);
    }
    if (allocator == MementoV2.ALLOC_SURVIVOR)
      return mature.alloc(bytes, align, offset);

    return super.alloc(bytes, align, offset, allocator, site);
  }

  /**
   * {@inheritDoc}<p>
   *
   * Initialize the object header for objects in the mark-sweep space,
   * and delegate to the superclass for other objects.
   */
  @Override
  @SuppressWarnings({"UnnecessaryReturnStatement"})
  @Inline
  public void postAlloc(ObjectReference ref, ObjectReference typeRef,
      int bytes, int allocator) {
    if (allocator == MementoV2.ALLOC_DEFAULT)
      return;
    else if (allocator == MementoV2.ALLOC_SURVIVOR)
      MementoV2.survivorSpace.initializeHeader(ref, true);
    else
      super.postAlloc(ref, typeRef, bytes, allocator);
  }

  @Override
  public Allocator getAllocatorFromSpace(Space space) {
    if (space == MementoV2.edenSpace1) return nursery1;
    if (space == MementoV2.edenSpace2) return nursery2;
    if (space == MementoV2.survivorSpace) return mature;
    return super.getAllocatorFromSpace(space);
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
  public final void collectionPhase(short phaseId, boolean primary) {
    if (phaseId == MementoV2.PREPARE) {
      super.collectionPhase(phaseId, primary);
      mature.prepare();
      return;
    }

    if (phaseId == MementoV2.RELEASE) {
      nursery1.reset();
      mature.release();
      super.collectionPhase(phaseId, primary);
      return;
    }

    super.collectionPhase(phaseId, primary);
  }

  @Override
  public void flush() {
    super.flush();
    mature.flush();
  }
}
