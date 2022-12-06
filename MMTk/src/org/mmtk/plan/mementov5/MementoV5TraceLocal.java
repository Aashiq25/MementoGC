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

import org.mmtk.plan.Trace;
import org.mmtk.plan.generational.Gen;
import org.mmtk.plan.generational.GenCollector;
import org.mmtk.plan.generational.GenMatureTraceLocal;
import org.mmtk.plan.generational.copying.GenCopy;
import org.mmtk.plan.generational.copying.GenCopyMatureTraceLocal;
import org.mmtk.policy.Space;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;

/**
 * This class implements the core functionality for a transitive
 * closure over the heap graph, specifically in a Generational Mark-Sweep
 * collector.
 */
@Uninterruptible
public final class MementoV5TraceLocal extends GenCopyMatureTraceLocal {

  /**
   * @param global the global trace class to use
   * @param plan the state of the generational collector
   */
  public MementoV5TraceLocal(int specializedScan, Trace global, GenCollector plan) {
    super(specializedScan, global, plan);
  }


  @Override
  @Inline
  public ObjectReference traceObject(ObjectReference object) {
    if (object.isNull()) return object;

    if (Space.isInSpace(MementoV5.MS, object)) {
      return MementoV5.msSpace.traceObject(this, object);
    }

    return super.traceObject(object);
  }

  @Override
  public boolean isLive(ObjectReference object) {
    if (object.isNull()) return false;
    if (Space.isInSpace(MementoV5.MS, object)) {
      return MementoV5.msSpace.isLive(object);
    }

    return super.isLive(object);
  }

  @Override
  public boolean willNotMoveInCurrentCollection(ObjectReference object) {
    if (Space.isInSpace(MementoV5.MS, object)) {
      return true;
    }
    return super.willNotMoveInCurrentCollection(object);
  }
}
