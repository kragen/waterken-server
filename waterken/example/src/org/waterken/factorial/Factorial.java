// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.factorial;

import static org.ref_send.promise.Fulfilled.ref;

import org.ref_send.promise.Promise;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.eventual.Eventual;

/**
 * A tail recursive factorial implementation.
 */
public final class
Factorial {

    private
    Factorial() {}
    
    /**
     * Computes a factorial.
     * @param _ eventual operator
     * @param n <code>&gt;= 0</code>
     * @return promise for the factorial of <code>n</code>
     */
    static public Promise<Integer>
    factorial(final Eventual _, final int n) {
        if (n < 0) { return new Rejected<Integer>(new Exception()); }
        final Recursion loop_ = _._(new Recursion() {
            public Promise<Integer>
            run(final Recursion loop_, final int n, final int acc) {
                if (n == 0) { return ref(acc); }
                return loop_.run(loop_, n - 1, n * acc);
            }
        });
        return loop_.run(loop_, n, 1);
    }

    /**
     * The inner loop of a tail recursive factorial implementation.
     */
    static public interface
    Recursion {
        Promise<Integer>
        run(Recursion loop_, int n, int acc);
    }
}
