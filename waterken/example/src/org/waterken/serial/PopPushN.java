// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.serial;

import static org.ref_send.promise.Eventual.ref;
import static org.ref_send.test.Logic.join;
import static org.ref_send.test.Logic.was;

import org.joe_e.array.ConstArray;
import org.ref_send.list.List;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Promise;

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
PopPushN {
    private PopPushN() {}
    
    /**
     * Runs a unit test.
     * @param _ eventual operator
     * @param n number of test iterations
     */
    static public Promise<?>
    make(final Eventual _, final int n) {
        final Series<Integer> x = Serial.make(_);
        
        /*
         * Check that the first n integers in the series will be the
         * numbers from 0 through n.
         */
        final ConstArray.Builder<Promise<?>> r = ConstArray.builder();
        for (int i = 0; i != n; ++i) {
            r.append(_.when(x.consume(), was(i)));
        }
        
        /*
         * Append the numbers 0 through n to the series.
         */
        for (int i = 0; i != n; ++i) {
            x.produce(ref(i));
        }
        
        return join(_, r.snapshot().toArray(new Object[0]));
    }
    
    // Command line interface

    /**
     * Executes the test.
     * @param args  argument string
     * @throws Exception    test failed
     */
    static public void
    main(final String[] args) throws Exception {
        final int n = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        
        final List<Promise<?>> work = List.list();
        final Promise<?> result = make(new Eventual(work.appender()), n);
        while (!work.isEmpty()) { work.pop().call(); }
        result.call();
    }
}
