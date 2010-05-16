// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.delayed;

import java.io.Serializable;

import org.ref_send.list.List;
import org.ref_send.promise.Deferred;
import org.ref_send.promise.Do;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Promise;

/**
 * A delayed resolution test.
 */
public final class
Relay {
    private Relay() {}
    
    static public Promise<?>
    make(final Eventual _, final Forwarder forwarder_) {
        class Return extends Do<Boolean,Promise<Boolean>>
                     implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final Boolean value) { return Eventual.ref(value); }
        }
        class Extract extends Do<Deferred<Boolean>,Promise<Boolean>>
                      implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final Deferred<Boolean> forward) throws Exception {
                return _.when(forward.promise, new Return());
            }
        }
        class Resolve extends Do<Deferred<Boolean>,Void> implements Serializable{
            static private final long serialVersionUID = 1L;
            
            public Void
            fulfill(final Deferred<Boolean> forward) throws Exception {
              forward.resolver.apply(true);
              return null;
            }
        }
        final Promise<Deferred<Boolean>> forward = forwarder_.forward(); 
        final Promise<Boolean> r = _.when(forward, new Extract());
        _.when(forward, new Resolve());
        return r;
    }

    static public void
    main(final String[] args) throws Exception {
        final List<Promise<?>> work = List.list();
        final Eventual _ = new Eventual(work.appender());
        final Promise<?> result = make(_, ForwarderMaker.make(_));
        while (!work.isEmpty()) { work.pop().call(); }
        result.call();
    }
}
