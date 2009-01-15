// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.factorial;

import static org.ref_send.test.Logic.was;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.list.List;
import org.ref_send.promise.Promise;
import org.ref_send.promise.eventual.Eventual;
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
     * Constructs an instance
     * @param _ eventual operator
     */
    public
    Main(final Eventual _) {
        this._ = _;
    }
    
    /**
     * Constructs an instance.
     * @param _ eventual operator
     */
    static public Test
    make(final Eventual _) {
        return new Main(_);
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
        final Eventual _ = new Eventual(work.appender());
        final Test test = new Main(_);
        final Promise<Boolean> result = test.start();
        while (!work.isEmpty()) { work.pop().run(); }
        if (!result.cast()) { throw new Exception("test failed"); }
    }
    
    // org.ref_send.test.Test interface

    /**
     * Starts a test.
     */
    public Promise<Boolean>
    start() { return _.when(Factorial.factorial(_, 4), was(4 * 3 * 2 * 1)); }
}
