// Copyright 2007-2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.all;

import static org.ref_send.test.Logic.and;

import org.joe_e.array.ConstArray;
import org.ref_send.list.List;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Vat;
import org.waterken.bang.Bang;
import org.waterken.bang.Beat;
import org.waterken.bang.Drum;
import org.waterken.bounce.Bounce;
import org.waterken.bounce.Pitch;
import org.waterken.bounce.Wall;
import org.waterken.eq.SoundCheck;
import org.waterken.serial.PopPushN;

/**
 * Runs all tests.
 */
public final class
All {
    private All() {}

    /**
     * Constructs an instance.
     * @param _ eventual operator
     */
    static public Promise<Boolean>
    make(final Eventual _) throws Exception {
        final ConstArray.Builder<Promise<Boolean>> r = ConstArray.builder();
        
        _.log.comment("testing EQ operations on promises");
        r.append(SoundCheck.make(_));
        
        _.log.comment("testing argument passing");
        final Vat<Wall> wall = _.spawn("wall", Bounce.class);
        r.append(Pitch.make(_, wall.top));
        
        _.log.comment("testing message pipelining");
        final Vat<Drum> drum = _.spawn("drum", Bang.class);
        r.append(Beat.make(_, drum.top));
        
        _.log.comment("testing promise resolution");
        r.append(PopPushN.make(_, 4));

        return and(_, r.snapshot());
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
        final Promise<Boolean> result = make(new Eventual(work.appender()));
        while (!work.isEmpty()) { work.pop().call(); }
        if (!result.call()) { throw new Exception("test failed"); }
    }
}
