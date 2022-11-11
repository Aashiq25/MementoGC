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

import org.mmtk.plan.TraceLocal;
import org.mmtk.plan.Trace;
import org.mmtk.policy.Space;

import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;

/**
 * This class implements the thread-local functionality for a
 * transitive closure over a coping/mark-sweep hybrid collector.
 */
@Uninterruptible
public final class MementoV2TraceLocal extends TraceLocal {

  /**
   * @param trace the global trace class to use
   */
  public MementoV2TraceLocal(Trace trace) {
    super(MementoV2.SCAN_MEMENTO, trace);
  }

  /****************************************************************************
   *
   * Externally visible Object processing and tracing
   */

  /**
   * Is the specified object reachable?
   */
  @Override
  public boolean isLive(ObjectReference object) {
    if (object.isNull()) return false;
    if (Space.isInSpace(MementoV2.EDEN1, object)) {
      return MementoV2.edenSpace1.isLive(object);
    }
    if (Space.isInSpace(MementoV2.EDEN2, object)) {
        return MementoV2.edenSpace2.isLive(object);
      }
    if (Space.isInSpace(MementoV2.SURVIVOR, object)) {
      return MementoV2.survivorSpace.isLive(object);
    }
    return super.isLive(object);
  }

  /**
   * {@inheritDoc}<p>
   *
   * In this instance, we refer objects in the mark-sweep space to the
   * msSpace for tracing, and defer to the superclass for all others.
   */
  @Inline
  @Override
  public ObjectReference traceObject(ObjectReference object) {
    if (object.isNull()) return object;
    if (Space.isInSpace(MementoV2.EDEN1, object))
      return MementoV2.edenSpace1.traceObject(this, object, MementoV2.ALLOC_SURVIVOR);
    if (Space.isInSpace(MementoV2.EDEN2, object))
        return MementoV2.edenSpace2.traceObject(this, object, MementoV2.ALLOC_SURVIVOR);
    if (Space.isInSpace(MementoV2.SURVIVOR, object))
      return MementoV2.survivorSpace.traceObject(this, object);
    return super.traceObject(object);
  }

  /**
   * Will this object move from this point on, during the current collection ?
   *
   * @param object The object to query.
   */
  @Override
  public boolean willNotMoveInCurrentCollection(ObjectReference object) {
    return !(Space.isInSpace(MementoV2.EDEN1, object) && Space.isInSpace(MementoV2.EDEN2, object));
  }
}
