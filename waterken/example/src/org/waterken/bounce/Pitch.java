// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.bounce;

import static org.ref_send.promise.Fulfilled.near;
import static org.ref_send.test.Logic.and;
import static org.ref_send.test.Logic.was;

import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.joe_e.array.ConstArray;
import org.ref_send.list.List;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Task;
import org.ref_send.test.Test;

/**
 * An argument passing test.
 */
public final class
Pitch {
    private Pitch() {}
    
    /**
     * Constructs an instance.
     * @param _ eventual operator
     * @param x test subject
     */
    static public Test
    make(final Eventual _, final Wall x) {
        final Wall x_ = _._(x);
        class TestX extends Struct implements Test, Serializable {
            static private final long serialVersionUID = 1L;
            
            public Promise<Boolean>
            run() {
                ConstArray<Volatile<Boolean>> r = ConstArray.array();

                class Re extends Do<AllTypes,Promise<Boolean>>
                         implements Serializable {
                    static private final long serialVersionUID = 1L;

                    public Promise<Boolean>
                    fulfill(final AllTypes a) {
                        return _.when(x_.bounce(a), was(a));
                    }
                }
                r = r.with(_.when(x_.getAll(), new Re()));
                final AllTypes a = near(Bounce.make(_).getAll());
                r = r.with(_.when(x_.bounce(a), was(a)));

                final ByteArray payload =
                    ByteArray.array(new byte[] { 0,1,2,3,4,5,6,7,8,9 });
                r = r.with(_.when(x_.bounce(payload), was(payload)));

                return and(_, r);
            }
        }
        return new TestX();
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
        final Test test = make(_, Bounce.make(_));
        final Promise<Boolean> result = test.run();
        while (!work.isEmpty()) { work.pop().run(); }
        if (!result.cast()) { throw new Exception("test failed"); }
    }
}