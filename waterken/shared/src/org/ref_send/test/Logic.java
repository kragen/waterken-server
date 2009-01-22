// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.test;

import static org.ref_send.promise.Fulfilled.ref;

import java.io.Serializable;

import org.joe_e.array.ConstArray;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Channel;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Resolver;

/**
 * Test condition operations.
 */
public final class
Logic {

    private
    Logic() {}

    /**
     * Creates a when block that compares against an expected value.
     * @param <T> referent type
     * @param expected  expected value
     */
    static public <T> Do<T,Promise<Boolean>>
    was(final T expected) {
        class Was extends Do<T,Promise<Boolean>> implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final T value) throws Exception {
                if (expected.equals(value)) { return ref(true); }
                throw new Exception();
            }
        }
        return new Was();
    }

    /**
     * Create a promise for the logical AND of multiple boolean promises.
     * @param _     eventual operator
     * @param tests	each boolean promise
     * @return promise for the logical AND of each <code>condition</code>
     */
    static public Promise<Boolean>
    and(final Eventual _, final ConstArray<? extends Volatile<Boolean>> tests) {
        if (0 == tests.length()) { return ref(true); }
        final Channel<Boolean> answer = _.defer();
        final int[] todo = { tests.length() };
        final Resolver<Boolean> resolver = answer.resolver;
        for (final Volatile<Boolean> test : tests) {
            class AND extends Do<Boolean,Void> implements Serializable {
                static private final long serialVersionUID = 1L;

                public Void
                fulfill(final Boolean value) {
                    if (value) {
                        if (0 == --todo[0]) {
                            resolver.run(true);
                        }
                    } else {
                        resolver.run(false);
                    }
                    return null;
                }
                public Void
                reject(final Exception e) {
                    resolver.reject(e);
                    return null;
                }
            }
            _.when(test, new AND());
        }
        return answer.promise;
    }
}
