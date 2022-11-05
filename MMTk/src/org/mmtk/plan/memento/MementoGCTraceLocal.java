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

import org.mmtk.plan.generational.Gen;
import org.mmtk.plan.generational.GenCollector;
import org.mmtk.plan.generational.GenMatureTraceLocal;
import org.mmtk.plan.Trace;
import org.mmtk.policy.Space;
import org.mmtk.vm.VM;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;

/**
 * This class implements the thread-local core functionality for a transitive
 * closure over the heap graph.
 */
@Uninterruptible
public final class MementoGCTraceLocal extends GenMatureTraceLocal {

  /**
   * @param trace the associated global trace
   */
  public MementoGCTraceLocal(Trace trace, GenCollector plan) {
    super(trace,plan);
  }

  private static MementoGC global() {
    return (MementoGC) VM.activePlan.global();
  }
  /****************************************************************************
   * Externally visible Object processing and tracing
   */

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isLive(ObjectReference object) {
    if (object.isNull()) return false;
    if (Space.isInSpace(MementoGC.MS, object))
      return MementoGC.hi ? MementoGC.matureSpace.isLive(object) : true;
    if (Space.isInSpace(MementoGC.NURSERY, object))
      return MementoGC.hi ? true : MementoGC.nurserySpace.isLive(object);
    return super.isLive(object);
  }

  @Inline
  @Override
  public ObjectReference traceObject(ObjectReference object) {
    if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(global().traceFullHeap());
    if (object.isNull()) return object;

    if (Space.isInSpace(MementoGC.MS, object))
      return MementoGC.matureSpace.traceObject(this, object, Gen.ALLOC_MATURE_MAJORGC);
    if (Space.isInSpace(MementoGC.NURSERY, object))
      return MementoGC.nurserySpace.traceObject(this, object, Gen.ALLOC_MATURE_MAJORGC);
    return super.traceObject(object);
  }
  @Override
  public boolean willNotMoveInCurrentCollection(ObjectReference object) {
    if (Space.isInSpace(MementoGC.toSpaceDesc(), object)) {
      return true;
    }
    if (Space.isInSpace(MementoGC.fromSpaceDesc(), object)) {
      return false;
    }
    return super.willNotMoveInCurrentCollection(object);
  }
}
