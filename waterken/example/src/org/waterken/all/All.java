// Copyright 2007-2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.all;

import static org.ref_send.test.Logic.and;

import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.array.ConstArray;
import org.ref_send.list.List;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Task;
import org.ref_send.test.Test;
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
    static public Test
    make(final Eventual _) {
        class TestX extends Struct implements Test, Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            run() throws Exception {
                ConstArray<Volatile<Boolean>> r = ConstArray.array();
                
                _.log.comment("testing EQ operations on promises");
                r = r.with(SoundCheck.make(_).run());
                
                _.log.comment("testing argument passing");
                final Wall wall_ = _.spawn("wall", Bounce.class);
                r = r.with(Pitch.make(_, wall_).run());
                
                _.log.comment("testing message pipelining");
                final Drum drum_ = _.spawn("drum", Bang.class);
                r = r.with(Beat.make(_, drum_).run());
                
                _.log.comment("testing promise resolution");
                r = r.with(PopPushN.make(_, 4).run());

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
        final Test test = make(new Eventual(work.appender()));
        final Promise<Boolean> result = test.run();
        while (!work.isEmpty()) { work.pop().run(); }
        if (!result.cast()) { throw new Exception("test failed"); }
    }
}
