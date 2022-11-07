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
import org.mmtk.vm.VM;
import org.vmmagic.pragma.*;
import org.mmtk.utility.Log;
import org.mmtk.policy.CopyLocal;

/**
 * This class implements <i>per-collector thread</i> behavior and state
 * for the <i>NoGC</i> plan, which simply allocates (without ever collecting
 * until the available space is exhausted.<p>
 *
 * Specifically, this class <i>would</i> define <i>NoGC</i> collection time semantics,
 * however, since this plan never collects, this class consists only of stubs which
 * may be useful as a template for implementing a basic collector.
 *
 * @see MementoGC
 * @see MementoGCMutator
 * @see CollectorContext
 */
@Uninterruptible
public class MementoGCCollector extends StopTheWorldCollector {

  /************************************************************************
   * Instance fields
   */

  /**
   *
   */
  // private final MementoGCTraceLocal trace;
  // protected final TraceLocal currentTrace;


  // private final CopyLocal mature;

  private final MementoGCTraceLocal trace = new MementoGCTraceLocal(global().trace);
  protected final TraceLocal currentTrace = trace;

//   public MementoGCCollector() {
//     // mature = new CopyLocal(MementoGC.matureSpace);
//     trace = new MementoGCTraceLocal(global().trace);
//     currentTrace = trace;
//  }


  /****************************************************************************
   * Collection
   */

  /**
   * Perform a garbage collection
   */
  // @Override
  // public final void collect() {
  //   VM.assertions.fail("GC Triggered in memento Plan. Is -X:gc:ignoreSystemGC=true ?");
  // }

  @Inline
  @Override
  public final void collectionPhase(short phaseId, boolean primary) {
    VM.assertions.fail("GC Triggered in memento Plan. Is -X:gc:ignoreSystemGC=true ?");
    Log.writeln("GC Collection Triggered");

    if (phaseId == MementoGC.PREPARE) {
      trace.prepare();
      return;
    }
    if (phaseId == MementoGC.CLOSURE) {
      // if (!global().gcFullHeap) {
        trace.completeTrace();
      // }
      return;
    }

    if (phaseId == MementoGC.RELEASE) {
      // if (!global().traceFullHeap()) {
        trace.release();
      // }
      return;
    }

    super.collectionPhase(phaseId, primary);
  }

  /****************************************************************************
   * Miscellaneous
   */

  /** @return The active global plan as a <code>NoGC</code> instance. */
  @Inline
  private static MementoGC global() {
    return (MementoGC) VM.activePlan.global();
  }

  @Override
  public final TraceLocal getCurrentTrace() {
    // if (global().traceFullHeap()) return getFullHeapTrace();
    return currentTrace;
  }

  /** @return The trace to use when collecting the mature space */
  // public abstract TraceLocal getFullHeapTrace();
}
