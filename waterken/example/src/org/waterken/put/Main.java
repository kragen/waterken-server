// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.put;

import static org.ref_send.Slot.var;
import static org.ref_send.promise.Fulfilled.ref;
import static org.ref_send.test.Logic.and;
import static org.ref_send.test.Logic.was;

import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.Token;
import org.ref_send.Variable;
import org.ref_send.list.List;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Loop;
import org.ref_send.promise.eventual.Task;
import org.ref_send.test.Test;

/**
 * A {@link Variable} test.
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
        if (!result.cast()) { throw new Exception("test failed"); }
    }
    
    // org.ref_send.test.Test interface

    /**
     * Starts a {@link #test test}.
     */
    public Promise<Boolean>
    start() throws Exception { return test(subject(), (byte)0); }
    
    // org.waterken.put.Main interface
    
    /**
     * Creates a new test subject.
     */
    public Variable<Volatile<Byte>>
    subject() {
        final Volatile<Byte> zero = ref((byte)0);
        return var(zero);
    }
    
    /**
     * Tests a {@link Variable}.
     * @param x test subject
     * @param n initial {@link Variable#get value}
     */
    public Promise<Boolean>
    test(final Variable<Volatile<Byte>> x, final Byte n) throws Exception {
        final Variable<Volatile<Byte>> x_ = _._(x);
        final Promise<Boolean> zero = _.when(x_.get(), was(n));
        x_.put(ref((byte)1));
        final Promise<Boolean> one = _.when(x_.get(), was((byte)1));
        return and(_, zero, one);
    }
}
