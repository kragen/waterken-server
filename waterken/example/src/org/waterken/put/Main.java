// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.put;

import static org.ref_send.promise.Fulfilled.ref;
import static org.ref_send.test.Logic.and;
import static org.ref_send.test.Logic.was;

import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.Token;
import org.ref_send.list.List;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Loop;
import org.ref_send.promise.eventual.Sink;
import org.ref_send.promise.eventual.Task;
import org.ref_send.test.Test;
import org.ref_send.var.Setter;
import org.ref_send.var.Variable;
import org.web_send.graph.Framework;

/**
 * A {@link Setter} test.
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
     * @param framework vat permissions
     */
    static public Test
    build(final Framework framework) {
        return new Main(framework._);
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
        }, new Sink());
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
    start() throws Exception { return test(ref(subject()), false); }
    
    // org.waterken.put.Main interface
    
    /**
     * Creates a new test subject.
     */
    public Variable<Boolean>
    subject() { return Put.make(); }
    
    /**
     * Tests a {@link Setter}.
     * @param x test subject
     * @param n initial {@link org.ref_send.var.Getter#get value}
     */
    public Promise<Boolean>
    test(final Volatile<Variable<Boolean>> x, final boolean n) {
        class Test extends Do<Variable<Boolean>,Promise<Boolean>>
                   implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final Variable<Boolean> v) {
                final Promise<Boolean> before =
                    _.when(_._(v.getter).get(), was(n));
                _._(v.setter).run(!n);
                final Promise<Boolean> after =
                    _.when(_._(v.getter).get(), was(!n));
                return and(_, before, after);
            }
        }
        return _.when(x, new Test());
    }
}
