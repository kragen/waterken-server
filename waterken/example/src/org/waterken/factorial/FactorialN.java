// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.factorial;

import static org.ref_send.test.Logic.was;

import org.ref_send.list.List;
import org.ref_send.promise.Promise;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Task;

/**
 * Eventual invocation tests.
 */
public final class
FactorialN {
    private FactorialN() {}
    
    /**
     * Runs a unit test.
     * @param _ eventual operator
     * @param n number to compute factorial of
     */
    static public Promise<Boolean>
    make(final Eventual _, final int n) {
        int r = 1;
        for (int i = n; i > 0; --i) {
            r *= i;
        }
        return _.when(Factorial.make(_, n), was(r));
    }
    
    // Command line interface

    /**
     * Executes the test.
     * @param args  ignored
     * @throws Exception    test failed
     */
    static public void
    main(final String[] args) throws Exception {
        final int n = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        
        final List<Task<?>> work = List.list();
        final Promise<Boolean> result = make(new Eventual(work.appender()), n);
        while (!work.isEmpty()) { work.pop().run(); }
        if (!result.cast()) { throw new Exception("test failed"); }
    }
}
