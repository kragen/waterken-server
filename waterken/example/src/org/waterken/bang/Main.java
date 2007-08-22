// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.bang;

import static org.ref_send.test.Logic.and;
import static org.ref_send.test.Logic.was;

import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.Token;
import org.ref_send.list.List;
import org.ref_send.promise.Promise;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Loop;
import org.ref_send.promise.eventual.Task;
import org.ref_send.test.Test;

/**
 * Eventual invocation tests.
 */
public final class
Main extends Struct implements Test, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * eventual operator
     */
    private final Eventual _;

    /**
     * Constructs an instance.
     * @param _ eventual operator
     */
    public
    Main(final Eventual _) {
        this._ = _;
    }
    
    // Command line interface

    /**
     * Executes the test.
     * @param args  ignored
     * @throws Exception    test failed
     */
    static public void
    main(final String[] args) throws Exception {
        final List<Task> work = List.list();
        final Eventual _ = new Eventual(new Token(), new Loop<Task>() {
            public void
            run(final Task task) { work.append(task); }
        });
        final Test test = new Main(_);
        final Promise<Boolean> result = test.start();
        while (!work.isEmpty()) { work.pop().run(); }
        if (!result.cast()) { throw new Exception("test failed"); }
    }
    
    // org.ref_send.test.Test interface

    /**
     * Starts a {@link #test test}.
     */
    public Promise<Boolean>
    start() { return test(subject(), 0); }
    
    // org.waterken.bang.Main interface
    
    /**
     * Creates a new test subject.
     */
    public Drum
    subject() { return Bang.make(); }
    
    /**
     * Tests a {@link Drum}.
     * @param x test subject
     * @param n initial {@link Drum#getHits hit count}
     */
    public Promise<Boolean>
    test(final Drum x, final int n) {
        final Drum x_ = _._(x);
        final Promise<Boolean> zero = _.when(x_.getHits(), was(n));
        x_.bang(1);
        final Promise<Boolean> one = _.when(x_.getHits(), was(n + 1));
        x_.bang(2);
        final Promise<Boolean> three = _.when(x_.getHits(), was(n + 3));
        return and(_, zero, one, three);
    }
}
