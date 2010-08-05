// Copyright 2007-2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.all;

import static org.ref_send.test.Logic.join;

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
import org.waterken.delayed.Forwarder;
import org.waterken.delayed.ForwarderMaker;
import org.waterken.delayed.Relay;
import org.waterken.eq.SoundCheck;
import org.waterken.pipelined.Pipelined;
import org.waterken.pipelined.PlugNPlay;
import org.waterken.pipelined.PlugNPlayMaker;
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
    static public Promise<?>
    make(final Eventual _) throws Exception {
        _.log.comment("testing EQ operations on promises");
        final Promise<?> a = SoundCheck.make(_);
        
        _.log.comment("testing argument passing");
        final Vat<Wall> wall = _.spawn("wall", Bounce.class);
        final Promise<?> b = Pitch.make(_, wall.top);
        
        _.log.comment("testing message pipelining");
        final Vat<Drum> drum = _.spawn("drum", Bang.class);
        final Promise<?> c = Beat.make(_, drum.top);
        
        _.log.comment("testing promise resolution");
        final Promise<?> d = PopPushN.make(_, 4);
        
        _.log.comment("testing delayed resolution");
        final Vat<Forwarder> forwarder = _.spawn("delay", ForwarderMaker.class);
        final Promise<?> e = Relay.make(_, forwarder.top);
        
        _.log.comment("testing promise pipelining");
        final Vat<PlugNPlay> player = _.spawn("pipeline", PlugNPlayMaker.class);
        final Promise<?> f = Pipelined.make(player.top);

        return join(_, a, b, c, d, e, f);
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
        final Promise<?> result = make(new Eventual(work.appender()));
        while (!work.isEmpty()) { work.pop().call(); }
        result.call();
    }
}
