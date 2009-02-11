// Copyright 2007-2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.all;

import static org.ref_send.test.Logic.and;

import org.joe_e.array.ByteArray;
import org.joe_e.array.ConstArray;
import org.ref_send.list.List;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Vat;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Task;
import org.ref_send.promise.Volatile;
import org.waterken.bang.Bang;
import org.waterken.bang.Beat;
import org.waterken.bang.Drum;
import org.waterken.bounce.Bounce;
import org.waterken.bounce.Pitch;
import org.waterken.bounce.Wall;
import org.waterken.eq.SoundCheck;
import org.waterken.folder.Folder;
import org.waterken.folder.FolderMaker;
import org.waterken.folder.SetGet;
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
        ConstArray<Volatile<Boolean>> r = new ConstArray<Volatile<Boolean>>();
        
        _.log.comment("testing EQ operations on promises");
        r = r.with(SoundCheck.make(_));
        
        _.log.comment("testing argument passing");
        final Vat<Wall> wall = _.spawn("wall", Bounce.class);
        r = r.with(Pitch.make(_, wall.root));
        
        _.log.comment("testing message pipelining");
        final Vat<Drum> drum = _.spawn("drum", Bang.class);
        r = r.with(Beat.make(_, drum.root));
        
        _.log.comment("testing interpreted objects");
        final Vat<Folder<ByteArray>> folder =
            _.spawn("folder", FolderMaker.class);
        r = r.with(SetGet.make(_, folder.root));
        
        _.log.comment("testing promise resolution");
        r = r.with(PopPushN.make(_, 4));

        return and(_, r);
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
        final Promise<Boolean> result = make(new Eventual(work.appender()));
        while (!work.isEmpty()) { work.pop().run(); }
        if (!result.cast()) { throw new Exception("test failed"); }
    }
}
