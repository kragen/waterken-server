// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.folder;

import static org.ref_send.test.Logic.and;
import static org.ref_send.test.Logic.was;

import org.joe_e.array.ByteArray;
import org.joe_e.array.ConstArray;
import org.ref_send.list.List;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Task;
import org.ref_send.promise.Volatile;

/**
 * Tests a {@link Folder}.
 */
public final class
SetGet {
    private SetGet() {}
    
    static public Promise<Boolean>
    make(final Eventual _, final Folder x) {
        final Folder x_ = _._(x);

        ConstArray<Volatile<Boolean>> r = new ConstArray<Volatile<Boolean>>();
        
        final ByteArray a = ByteArray.array((byte)'a');
        x_.run("a", a);
        r = r.with(_.when(x_.get("a"), was(a)));
        
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
        final Promise<Boolean> result =
            make(new Eventual(work.appender()), FolderMaker.make());
        while (!work.isEmpty()) { work.pop().run(); }
        if (!result.cast()) { throw new Exception("test failed"); }
    }
}
