// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.bounce;

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
 * An argument passing test.
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
    start() { return test(subject()); }
    
    // org.waterken.bang.Main interface
    
    /**
     * Creates a new test subject.
     */
    public Wall
    subject() { return Bounce.make(); }
    
    /**
     * Tests a {@link Wall}.
     * @param x test subject
     */
    public Promise<Boolean>
    test(final Wall x) {
        final Wall x_ = _._(x);
        final AllTypes a = new AllTypes(
            true, Byte.MAX_VALUE, '?',
            Double.MAX_VALUE, Float.MAX_VALUE,
            Integer.MAX_VALUE, Long.MAX_VALUE, Short.MAX_VALUE,
            "Hello World!");
        return _.when(x_.bounce(a), was(a));
    }
}