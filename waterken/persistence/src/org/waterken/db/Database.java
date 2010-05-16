// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.db;

import org.joe_e.Immutable;
import org.ref_send.promise.Log;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Receiver;
import org.waterken.store.DoesNotExist;

/**
 * A persistent object graph.
 */
public abstract class
Database<S> {

    // known root names
    
    /**
     * {@link ClassLoader}, initialized by database
     */
    static public final String code = ".code";
    
    /**
     * {@link Creator}, initialized by database
     */
    static public final String creator = ".creator";
    
    /**
     * {@linkplain Receiver destructor}, initialized by database
     */
    static public final String destruct = ".destruct";

    /**
     * {@link Effect} {@link Receiver output} processed only if the current
     * {@link Database#enter transaction} commits, initialized by database
     */
    static public final String effect = ".effect";
    
    /**
     * URI for this database, initialized by database
     */
    static public final String here = ".here";
    
    /**
     * {@linkplain Log}, initialized by database
     */
    static public final String log = ".log";
    
    /**
     * {@link TransactionMonitor}, initialized by database
     */
    static public final String monitor = ".monitor";
    
    /**
     * always bound to <code>null</code>, initialized by database
     */
    static public final String nothing = "";
    
    /**
     * project name, initialized by database
     */
    static public final String project = ".project";
    
    /**
     * root application object
     */
    static public final String top = ".top";

    /**
     * {@link Database#extend} {@link Promise task} run when database is opened
     */
    static public final String wake = ".wake";

    // org.waterken.db.Database interface

    /**
     * indicates a {@linkplain Database#enter transaction} only queries existing
     * state, and does not persist any new selfish objects
     * <p>
     * Infrastructure code uses this feature to prevent changes to application
     * objects during certain transactions.
     * </p>
     */
    public static final boolean query = true;

    /**
     * indicates a {@linkplain Database#enter transaction} is allowed to modify
     * existing state
     */
    public static final boolean update = false;
    
    /**
     * session state shared across all vats
     */
    public final S session;
    
    /**
     * pending tasks associated with this database
     */
    public final Receiver<Service> service;
    
    /**
     * pending timeouts associated with this database
     */
    public final Scheduler<Service> scheduler;
    
    /**
     * Constructs an instance.
     */
    protected
    Database(final S session,
             final Receiver<Service> service,
             final Scheduler<Service> timeouts) {
        this.session = session;
        this.service = service;
        this.scheduler = timeouts;
    }

    /**
     * Processes a transaction within this database.
     * <p>
     * The implementation MUST ensure only one transaction is active in the
     * database at any time. An invocation from another thread MUST block until
     * the database becomes available. A recursive invocation from the same
     * thread MUST throw an {@link Exception}.
     * </p>
     * <p>
     * If {@linkplain Transaction#run invocation} of the <code>body</code>
     * causes an {@link Error}, the transaction MUST be aborted. When a
     * transaction is aborted, all modifications to objects in the database MUST
     * be discarded. For subsequent transactions, it MUST be as if the aborted
     * transaction was never attempted.
     * </p>
     * <p>
     * The implementation MUST NOT rely on the <code>isQuery</code>
     * argument accurately describing the transaction's behavior. If
     * {@link #query} is specified, the implementation MUST check that the
     * constraints are met; if not, the transaction MUST be aborted.
     * </p>
     * <p>
     * The <code>body</code> MUST NOT retain references to any of the objects
     * in the database beyond completion of the transaction. The database
     * implementation can rely on the <code>body</code> being well-behaved in
     * this respect. An identifier for an object in the database may be retained
     * across transactions by either {@linkplain Root#link linking}, or
     * {@linkplain Root#export exporting} it.
     * </p>
     * <p>
     * If invocation of this method returns normally, all modifications to
     * objects in the database MUST be committed. Only if the current
     * transaction commits will the {@linkplain #effect enqueued}
     * {@link Effect}s be {@linkplain #enter executed}; otherwise, the
     * implementation MUST discard them. The effects MUST be executed in the
     * same order as they were enqueued. Effects from a subsequent transaction
     * MUST NOT be executed until all effects from the current transaction have
     * been executed. An {@link Effect} MUST NOT access objects in the database,
     * but may schedule additional effects.
     * </p>
     * @param <R> <code>body</code>'s return type
     * @param isQuery   either {@link #update} or {@link #query}
     * @param body transaction body
     * @return promise for <code>body</code>'s return
     * @throws DoesNotExist database no longer exists
     * @throws Exception problem completing the transaction, which may or may
     *                   not be committed
     */
    public abstract <R extends Immutable> Promise<R>
    enter(boolean isQuery, Transaction<R> body) throws DoesNotExist, Exception;
}
