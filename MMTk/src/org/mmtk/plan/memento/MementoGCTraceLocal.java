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

import org.mmtk.plan.Trace;
import org.mmtk.plan.TraceLocal;
import org.mmtk.policy.Space;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;
import org.mmtk.utility.Log;

/**
 * This class implements the thread-local core functionality for a transitive
 * closure over the heap graph.
 */
@Uninterruptible
public final class MementoGCTraceLocal extends TraceLocal {

  /**
   * @param trace the associated global trace
   */
  public MementoGCTraceLocal(Trace trace) {
    super(trace);
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
    if (Space.isInSpace(MementoGC.NURSERY, object)) {
      return MementoGC.nurserySpace.isLive(object);
    }
    return super.isLive(object);
  }

  @Inline
  @Override
  public ObjectReference traceObject(ObjectReference object) {
    if (object.isNull()) return object;
    if (Space.isInSpace(MementoGC.NURSERY, object)) {
      Log.writeln("Moving to Mature");
      return MementoGC.nurserySpace.traceObject(this, object, MementoGC.ALLOC_MATURE);
    }
    return super.traceObject(object);
  }
}
