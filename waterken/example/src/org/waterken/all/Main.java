// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.all;

import static org.ref_send.test.Logic.and;

import java.io.Serializable;
import java.util.ArrayList;

import org.joe_e.Struct;
import org.ref_send.Variable;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.test.Test;
import org.waterken.bang.Bang;
import org.waterken.bang.Drum;
import org.waterken.bounce.Bounce;
import org.waterken.bounce.Wall;
import org.waterken.put.Put;
import org.web_send.graph.Framework;
import org.web_send.graph.Host;

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
     * @param framework model framework
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
        final Host mine = framework.dependent;
        final ArrayList<Promise<Boolean>> r = new ArrayList<Promise<Boolean>>();
        
        r.add(new org.waterken.eq.Main(_).start());
        
        final Promise<Wall> wall = mine.share("wall", Bounce.class.getName());
        final Wall wall_ = _.cast(Wall.class, wall);
        r.add(new org.waterken.bounce.Main(_).test(wall_));
        
        final Promise<Drum> drum = mine.share("drum", Bang.class.getName());
        final Drum drum_ = _.cast(Drum.class, drum);
        r.add(new org.waterken.bang.Main(_).test(drum_, 0));
        
        final Promise<Variable<Volatile<Byte>>> slot =
            mine.share("slot", Put.class.getName());
        final Variable<Volatile<Byte>> slot_ = _.cast(Variable.class, slot);
        r.add(new org.waterken.put.Main(_).test(slot_, (byte)0));

        return and(_, r.toArray(new Promise[r.size()]));
    }
}
