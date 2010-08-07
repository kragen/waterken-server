/**
 * 
 */
package org.ref_send.promise;

import java.io.Serializable;

import org.ref_send.promise.Eventual.When;

/**
 * Keeps statistics about eventual operations.
 */
/*package*/ final class
Stats implements Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * number of tasks enqueued
     * <p>
     * This variable is only incremented and should never be allowed to wrap.
     * </p>
     */
    private long tasks;

    protected long
    newTask() {
        final long id = ++tasks;
        if (0 == id) { throw new AssertionError(); }
        return id;
    }

    /**
     * number of when blocks created
     * <p>
     * This variable is only incremented and should never be allowed to wrap.
     * </p>
     */
    private long whens;

    /**
     * pool of previously used when blocks
     * <p>
     * When blocks are recycled so that environments providing orthogonal
     * persistence don't accumulate lots of dead objects.
     * </p>
     */
    private Promise<When<?>> whenPool;

    protected @SuppressWarnings({"unchecked","rawtypes"}) <T> Promise<When<T>>
    allocWhen(final long condition) {
        final long message = ++whens;
        if (0 == message) { throw new AssertionError(); }

        final Promise<When<T>> r;
        final When<T> block;
        if (null == whenPool) {
            block = new When<T>();
            r = new Fulfilled<When<T>>(false, block);
        } else {
            r = (Promise)whenPool;
            block = (When)Eventual.near(r);
            whenPool = (Promise)block.next;
            block.next = null;
        }
        block.condition = condition;
        block.message = message;
        return r;
    }

    protected @SuppressWarnings({ "rawtypes", "unchecked" }) void
    freeWhen(final Promise pBlock, final When block) {
        block.condition = 0;
        block.message = 0;
        block.observer = null;
        block.next = (Promise)whenPool;
        whenPool = pBlock;
    }

    /**
     * number of promises {@linkplain Eventual#defer created}
     * <p>
     * This variable is only incremented and should never be allowed to wrap.
     * </p>
     */
    private long deferrals;

    protected long
    newDeferral() {
        final long id = ++deferrals;
        if (0 == id) { throw new AssertionError(); }
        return id;
    }
}
