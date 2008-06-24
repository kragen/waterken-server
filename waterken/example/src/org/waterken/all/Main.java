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
import org.ref_send.var.Variable;
import org.waterken.bang.Bang;
import org.waterken.bang.Drum;
import org.waterken.bounce.Bounce;
import org.waterken.bounce.Wall;
import org.waterken.put.Put;
import org.web_send.graph.Framework;

/**
 * Runs all tests.
 */
public final class
Main extends Struct implements Test, Serializable {
    static private final long serialVersionUID = 1L;
    
    private final Framework framework;
    
    private
    Main(final Framework framework) {
        this.framework = framework;
    }

    /**
     * Constructs an instance.
     * @param framework vat permissions
     */
    static public Test
    build(final Framework framework) {
        return new Main(framework);
    }
    
    // org.ref_send.test.Test interface

    /**
     * Starts all the tests.
     */
    public Promise<Boolean>
    start() throws Exception {
        final Eventual _ = framework._;
        final ConstArray.Builder<Volatile<Boolean>> r = ConstArray.builder(4);
        
        _.log.comment("testing EQ operations on promises");
        r.append(new org.waterken.eq.Main(_).start());
        
        _.log.comment("testing argument passing");
        final Wall wall_ = framework.publisher.spawn("wall", Bounce.class);
        r.append(new org.waterken.bounce.Main(_).test(wall_));
        
        _.log.comment("testing message pipelining");
        final Drum drum_ = framework.publisher.spawn("drum", Bang.class);
        r.append(new org.waterken.bang.Main(_).test(drum_, 0));
        
        _.log.comment("testing idempotent update");
        final Promise<Variable<Boolean>> slot = framework.spawn.run(Put.class);
        r.append(new org.waterken.put.Main(_).test(slot, false));

        return and(_, r.snapshot());
    }
}
