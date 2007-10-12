// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.eq;

import static org.ref_send.promise.Fulfilled.ref;
import static org.ref_send.test.Logic.and;

import java.io.Serializable;
import java.util.ArrayList;

import org.joe_e.Struct;
import org.joe_e.Token;
import org.ref_send.list.List;
import org.ref_send.promise.NaN;
import org.ref_send.promise.Promise;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Loop;
import org.ref_send.promise.eventual.Task;
import org.ref_send.test.Test;

/**
 * Checks invariants of the ref_send API.
 */
public final class
Main extends Struct implements Test, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * eventual operator
     */
    private final Eventual _;

    /**
     * Constructs an instance.
     * @param _ eventual operator
     */
    public
    Main(final Eventual _) {
        this._ = _;
    }
    
    // Command line interface

    /**
     * Executes the test.
     * @param args  ignored
     * @throws Exception    test failed
     */
    static public void
    main(final String[] args) throws Exception {
        final List<Task> work = List.list();
        final Eventual _ = new Eventual(new Token(), new Loop<Task>() {
            public void
            run(final Task task) { work.append(task); }
        });
        final Test test = new Main(_);
        final Promise<Boolean> result = test.start();
        while (!work.isEmpty()) { work.pop().run(); }
        if (!result.cast()) { throw new Exception("test incomplete"); }
    }
    
    // org.ref_send.test.Test interface

    /**
     * Starts a {@link #test test}.
     */
    public Promise<Boolean>
    start() throws Exception {
        final ArrayList<Promise<Boolean>> r = new ArrayList<Promise<Boolean>>();
        
        // check double NaN handling
        final Promise<Double> NaN = ref(Double.NaN);
        check(NaN.equals(NaN));
        check(!NaN.equals(ref(Double.NaN)));
        try {
            NaN.cast();
            check(false);
        } catch (final NaN e) {}
        r.add(_.when(NaN, new Do<Double,Promise<Boolean>>() {
            public Promise<Boolean>
            fulfill(final Double arg) throws Exception {
                throw new Exception();
            }
            public Promise<Boolean>
            reject(final Exception reason) throws Exception {
                if (reason instanceof NaN) { return ref(true); }
                throw reason;
            }
        }));
        
        return and(_, r.toArray(new Promise[r.size()]));
    }
    
    static private void
    check(final boolean valid) throws Exception {
        if (!valid) { throw new Exception(); }
    }
}
