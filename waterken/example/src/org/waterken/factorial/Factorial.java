// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.factorial;

import org.ref_send.promise.Eventual;
import org.ref_send.promise.Promise;

/**
 * A tail recursive factorial implementation.
 */
public final class
Factorial {
    private Factorial() {}
    
    /**
     * Computes a factorial.
     * @param _ eventual operator
     * @param n <code>&gt;= 0</code>
     * @return promise for the factorial of <code>n</code>
     */
    static public Promise<Integer>
    make(final Eventual _, final int n) {
        if (n < 0) { return Eventual.reject(new Exception()); }
        /*
         * We'll simulate tail recursive calls by doing eventual invocations of
         * our loop function. Since the call is eventual, the current stack
         * frame is popped right away and the loop's stack frame isn't created
         * until the next event loop turn. The same trick is used inside the
         * loop itself. The chain of promises produced by these calls also
         * collapses during each event loop turn, so that there's always only
         * a constant number of objects referenced. The efficiency of this
         * probably isn't as good as in a VM designed for tail recursion, but
         * may be good enough for some purposes.
         */
        final Recursion loop_ = _._(new Recursion() {
            public Promise<Integer>
            run(final Recursion loop_, final int n, final int acc) {
                if (n == 0) { return Eventual.ref(acc); }
                return loop_.run(loop_, n - 1, n * acc);
            }
        });
        return loop_.run(loop_, n, 1);
    }

    /**
     * The inner loop of a tail recursive factorial implementation.
     */
    static public interface
    Recursion { Promise<Integer> run(Recursion loop_, int n, int acc); }
}
