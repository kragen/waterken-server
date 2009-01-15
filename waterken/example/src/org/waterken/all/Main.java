// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.all;

import static org.ref_send.test.Logic.and;

import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.array.ConstArray;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.test.Test;
import org.waterken.bang.Bang;
import org.waterken.bang.Drum;
import org.waterken.bounce.Bounce;
import org.waterken.bounce.Wall;
import org.web_send.graph.Vat;

/**
 * Runs all tests.
 */
public final class
Main extends Struct implements Test, Serializable {
    static private final long serialVersionUID = 1L;
    
    private final Eventual _;
    private final Vat vat;
    
    private
    Main(final Eventual _, final Vat vat) {
        this._ = _;
        this.vat = vat;
    }

    /**
     * Constructs an instance.
     * @param _     eventual operator
     * @param vat   containing vat
     */
    static public Test
    make(final Eventual _, final Vat vat) {
        return new Main(_, vat);
    }
    
    // org.ref_send.test.Test interface

    /**
     * Starts all the tests.
     */
    public Promise<Boolean>
    start() throws Exception {
        final ConstArray.Builder<Volatile<Boolean>> r = ConstArray.builder(4);
        
        _.log.comment("testing EQ operations on promises");
        r.append(new org.waterken.eq.Main(_).start());
        
        _.log.comment("testing argument passing");
        final Wall wall_ = vat.publisher.spawn("wall", Bounce.class);
        r.append(new org.waterken.bounce.Main(_).test(wall_));
        
        _.log.comment("testing message pipelining");
        final Drum drum_ = vat.publisher.spawn("drum", Bang.class);
        r.append(new org.waterken.bang.Main(_).test(drum_, 0));

        return and(_, r.snapshot());
    }
}
