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
import org.web_send.graph.Framework;

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
     * <p>
     * This method is called by the infrastructure code that manages the
     * lifecycle of databases. The method must have the exact declaration shown
     * below. The sole parameter is the set of permissions provided to the first
     * application object created in a new database. The return from this method
     * is the reference returned to the creator of the new database. In this
     * case, the database creator will get an eventual reference of type
     * {@link Test} to an instance of this class.
     * </p>
     * @param framework model framework
     */
    static public Test
    build(final Framework framework) {
        // A Main object only needs the eventual operator, so the rest of the
        // provided permissions are ignored.
        return new Main(framework._);
    }
    
    // Command line interface

    /**
     * Executes the test.
     * <p>
     * This class can also be run from the command line, to run tests against a
     * transient {@link Drum}. Most factory classes won't provide a command line
     * test suite and so won't have a {@link #main} method.
     * </p>
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
        /*
         * First, ensure that we have an eventual reference, since the caller
         * may have provided an immediate reference.
         */ 
        final Drum x_ = _._(x);
        
        /*
         * All tests are now done using the created eventual reference, the
         * provided reference is no longer used, since it can't be trusted to be
         * an eventual reference.
         */
        
        /*
         * Start by checking that the caller provided the correct value for the
         * current hit count on the provided drum. We get the initial hit count
         * by sending an eventual invocation of the getHits() method. If the
         * provided drum is in another database, this invocation will result in
         * an HTTP GET request being sent to the hosting server. The return from
         * the getHits() invocation is a promise for the number of hits. Using
         * the when() method, we register an observer on this promise, to
         * receive a notification after the HTTP GET response has come back. The
         * observer, constructed by the was() method, produces a promise for a
         * boolean, indicating whether or not the number of hits specified in
         * the HTTP GET response was the number expected. We'll hold onto this
         * promise and use it to produce the promise returned to our caller.
         */
        final Promise<Boolean> zero = _.when(x_.getHits(), was(n));
        
        /*
         * Increment the hit counter by sending an eventual invocation of the
         * bang() method. If the provided drum is in another database, this
         * invocation will result in an HTTP POST request being sent to the
         * hosting server.
         */
        x_.bang(1);
        
        /*
         * Requests sent on an eventual reference are sent in order, and so
         * another check of the drum's hit count will see a value 1 more than
         * the previous check.
         */
        final Promise<Boolean> one = _.when(x_.getHits(), was(n + 1));
        
        // We can queue up as many requests as we like...
        x_.bang(2);
        
        // ...and they will all be sent in order.
        final Promise<Boolean> three = _.when(x_.getHits(), was(n + 3));
        
        /*
         * We now have 3 promises for checks on the expected value of the drum's
         * hit count. We'll combine these 3 promises into 1 by doing an eventual
         * AND operation on them. The promise returned to our caller will
         * resolve as soon as any one of these 3 promises doesn't resolve to
         * true, or after all of them have resolved to true.
         */
        return and(_, zero, one, three);
        
        /*
         * In total, we've sent 3 GET requests and 2 POST requests and produced
         * a promise which will only resolve to true after all of these network
         * requests have completed successfully. If any of these communications
         * are interrupted, due to a server crash or lost connection, the
         * software will remember where it left off and resume as soon as
         * network connections can be re-established, which the software will
         * also periodically retry. Regardless of how often this process is
         * interrupted, or for how long, the hit count on the drum will only
         * be incremented by 3, the number specified by the algorithm above.
         */
    }
}
