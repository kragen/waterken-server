// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.bounce;

import static org.ref_send.promise.Eventual.near;
import static org.ref_send.test.Logic.join;
import static org.ref_send.test.Logic.was;

import java.io.Serializable;

import org.joe_e.array.ByteArray;
import org.ref_send.list.List;
import org.ref_send.promise.Do;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Failure;
import org.ref_send.promise.Promise;

/**
 * An argument passing test.
 */
public final class
Pitch {
    private Pitch() { /**/ }
    
    /**
     * Runs a unit test.
     * @param _ eventual operator
     * @param x test subject
     */
    static public Promise<?>
    make(final Eventual _, final Wall x) {
        final Wall x_ = _._(x);

        class Re extends Do<AllTypes,Promise<?>> implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<?>
            fulfill(final AllTypes a) { return _.when(x_.bounce(a), was(a)); }
        }
        final Promise<?> a = _.when(x_.getAll(), new Re());
        final Promise<?> b =
            _.when(x_.bounce(near(Bounce.make(_).getAll())), new Re());

        final ByteArray payload =
            ByteArray.array(new byte[] { 0,1,2,3,4,5,6,7,8,9 });
        final Promise<?> c = _.when(x_.bounce(payload), was(payload));
        
        final ByteArray maxPayload =
        	ByteArray.array(new byte[Failure.maxEntitySize]);
        final Promise<?> d = _.when(x_.bounce(maxPayload), was(maxPayload));

        return join(_, a, b, c, d);
    }
    
    // Command line interface

    /**
     * Executes the test.
     * @param args  ignored
     * @throws Exception    test failed
     */
    static public void
    main(final String[] args) throws Exception {
        final List<Promise<?>> work = List.list();
        final Eventual _ = new Eventual(work.appender());
        final Promise<?> result = make(_, Bounce.make(_));
        while (!work.isEmpty()) { work.pop().call(); }
        result.call();
    }
}