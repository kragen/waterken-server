// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Constructor;

import org.joe_e.Token;
import org.joe_e.reflect.Reflection;
import org.ref_send.promise.Promise;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Loop;
import org.ref_send.promise.eventual.Task;
import org.ref_send.test.Test;
import org.waterken.model.Model;
import org.waterken.model.Root;
import org.waterken.model.Transaction;

/**
 * Constructs and runs a test.
 */
final class
Drive {

    private
    Drive() {}
    
    /**
     * The command line arguments are:
     * <ol>
     *  <li>database name</li>
     *  <li>{@link Test} class name</li>
     *  <li>project name</li>
     * </ol>
     * @param args  command line arguments
     */
    static public void
    main(final String[] args) throws Exception {
        final String name = args[0];
        final String typename = args[1];
        final String project = args.length > 2 ? args[2] : "";
        
        // Create the named model.
        final File id = JODB.create(new File(JODB.dbDirName), name, project);
        
        // Execute the test in the model.
        JODB.connect(id).enter(Model.change, new Transaction<Void>() {
            @SuppressWarnings("unchecked") public Void
            run(final Root local) throws Exception {
                final ClassLoader code=(ClassLoader)local.fetch(null,Root.code);
                final Class type = code.loadClass(typename);
                final Constructor constructor =
                    Reflection.constructor(type,new Class[] { Eventual.class });
                final Loop<Task> enqueue = (Loop)local.fetch(null,Root.enqueue);
                final Eventual _ = new Eventual(new Token(), enqueue);
                final Test test = (Test)Reflection.construct(constructor, _);
                final Promise<Boolean> result = test.start();
                _.when(result, new PrintResult());
                return null;
            }
        });
    }
    
    static final class
    PrintResult extends Do<Boolean,Void> implements Serializable {
        static private final long serialVersionUID = 1L;
        
        public Void
        fulfill(final Boolean arg) {
            System.out.println(arg);
            return null;
        }
        public Void
        reject(final Exception reason) {
            reason.printStackTrace();
            return null;
        }
    }
}
