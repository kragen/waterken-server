// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.serial;

import static org.ref_send.promise.Fulfilled.ref;
import static org.ref_send.test.Logic.and;
import static org.ref_send.test.Logic.was;

import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.array.ConstArray;
import org.ref_send.list.List;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Task;
import org.ref_send.test.Test;
import org.web_send.graph.Framework;

/**
 * Eventual invocation tests.
 * <p>
 * This class provides an introduction to eventual operations by using them to
 * test the elements of a series that isn't produced until later. Within these
 * lines, time and space are not just curved, they're bent. You should also
 * probably save this example until later.
 * </p>
 */
public final class
Main extends Struct implements Test, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * eventual operator
     */
    private final Eventual _;

    /**
     * Constructs an instance
     * @param _ eventual operator
     */
    public
    Main(final Eventual _) {
        this._ = _;
    }
    
    /**
     * Constructs an instance.
     * @param framework vat permissions
     */
    static public Test
    build(final Framework framework) {
        return new Main(framework._);
    }
    
    // Command line interface

    /**
     * Executes the test.
     * @param args  ignored
     * @throws Exception    test failed
     */
    static public void
    main(final String[] args) throws Exception {
        final List<Task<?>> work = List.list();
        final Eventual _ = new Eventual(work.appender());
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
    start() { return test(subject(), 4); }
    
    // org.waterken.serial.Main interface
    
    /**
     * Creates a new test subject.
     */
    public Series<Integer>
    subject() { return Serial.make(_); }
    
    /**
     * Tests a {@link Series}.
     * @param x empty series
     * @param n number of test iterations
     */
    public Promise<Boolean>
    test(final Series<Integer> x, final int n) {
        /*
         * Check that the first n integers in the series will be the numbers
         * from 0 through n.
         */
    	final ConstArray.Builder<Volatile<Boolean>> r = ConstArray.builder(3);
        for (int i = 0; i != n; ++i) {
        	r.append(_.when(x.consume(), was(i)));
        }
        
        /*
         * Append the numbers 0 through n to the series.
         */
        for (int i = 0; i != n; ++i) {
            x.produce(ref(i));
        }
        
        return and(_, r.snapshot());
    }
}
