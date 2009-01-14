// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.db;

import java.io.FileNotFoundException;

import org.joe_e.Immutable;
import org.ref_send.promise.Promise;
import org.ref_send.promise.eventual.Log;
import org.ref_send.promise.eventual.Receiver;
import org.ref_send.promise.eventual.Task;

/**
 * A persistent object graph.
 */
public abstract class
Vat<S> {

    // known root names
    
    /**
     * {@link ClassLoader}, initialized by vat
     */
    static public final String code = ".code";
    
    /**
     * {@link Creator}, initialized by vat
     */
    static public final String creator = ".creator";
    
    /**
     * {@linkplain Receiver destructor}, initialized by vat
     */
    static public final String destruct = ".destruct";

    /**
     * {@link Effect} {@link Receiver output} processed only if the current
     * {@link Vat#enter transaction} commits, initialized by vat
     */
    static public final String effect = ".effect";
    
    /**
     * vat URI, initialized by vat
     */
    static public final String here = ".here";
    
    /**
     * {@linkplain Log logger}, initialized by vat
     */
    static public final String log = ".log";
    
    /**
     * always bound to <code>null</code>, initialized by vat
     */
    static public final String nothing = "";
    
    /**
     * project name, initialized by vat
     */
    static public final String project = ".project";
    
    /**
     * {@link Task} to generate an HTTP entity-tag identifying all the state
     * accessed by the current transaction, initialized by vat
     */
    static public final String tagger = ".tagger";

    /**
     * {@link Vat#extend} {@link Task} to run each time vat is loaded
     */
    static public final String wake = ".wake";

    // org.waterken.vat.Vat interface
    
    /**
     * session state shared across all vats
     */
    public final S session;
    
    /**
     * pending tasks associated with this vat
     */
    public final Receiver<Service> service;
    
    /**
     * Constructs an instance.
     */
    protected
    Vat(final S session, final Receiver<Service> service) {
        this.session = session;
        this.service = service;
    }
    
    /**
     * Gets the project name for this vat.
     */
    public abstract String
    getProject() throws Exception;

    /**
     * Processes a transaction within this vat.
     * <p>
     * The implementation MUST ensure only one transaction is active in the vat
     * at any time. An invocation from another thread MUST block until the vat
     * becomes available. A recursive invocation from the same thread MUST throw
     * an {@link Exception}.
     * </p>
     * <p>
     * If {@linkplain Transaction#run invocation} of the <code>body</code>
     * causes an {@link Error}, the transaction MUST be aborted. When a
     * transaction is aborted, all modifications to objects in the vat MUST be
     * discarded. For subsequent transactions, it MUST be as if the aborted
     * transaction was never attempted.
     * </p>
     * <p>
     * The implementation MUST NOT rely on the {@link Transaction#isQuery}
     * argument accurately describing the transaction's behavior. If
     * {@link Transaction#query} is specified, the implementation MUST check
     * that the constraints are met; if not, the transaction MUST be aborted.
     * </p>
     * <p>
     * The <code>body</code> MUST NOT retain references to any of the objects
     * in the vat beyond completion of the transaction. The vat implementation
     * can rely on the <code>body</code> being well-behaved in this respect.
     * An identifier for an object in the vat may be retained across
     * transactions by either {@linkplain Root#link linking}, or
     * {@linkplain Root#export exporting} it.
     * </p>
     * <p>
     * If invocation of this method returns normally, all modifications to
     * objects in the vat MUST be committed. Only if the current transaction
     * commits will the {@linkplain Root#effect enqueued} {@link Effect}s be
     * {@linkplain #enter executed}; otherwise, the implementation MUST discard
     * them. The effects MUST be executed in the same order as they were
     * enqueued. Effects from a subsequent transaction MUST NOT be executed
     * until all effects from the current transaction have been executed. An
     * {@link Effect} MUST NOT access objects in the vat, but may schedule
     * additional effects.
     * </p>
     * @param <R> <code>body</code>'s return type
     * @param body transaction body
     * @return promise for <code>body</code>'s return
     * @throws FileNotFoundException    vat no longer exists
     * @throws Exception                problem completing the transaction,
     *                                  which may or may not be committed
     */
    public abstract <R extends Immutable> Promise<R>
    enter(Transaction<R> body) throws Exception;
}
