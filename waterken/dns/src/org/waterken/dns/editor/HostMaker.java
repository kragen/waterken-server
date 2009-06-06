// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import static org.ref_send.promise.Eventual.ref;

import java.io.Serializable;

import org.joe_e.array.ByteArray;
import org.joe_e.array.ConstArray;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Receiver;
import org.waterken.dns.Resource;
import org.waterken.menu.Copy;
import org.waterken.menu.Menu;
import org.waterken.menu.Snapshot;
import org.waterken.menu.TooMany;

/**
 * A {@link Resource} {@link Menu} maker.
 */
public final class
HostMaker {
    private HostMaker() {}

    /**
     * maximum number of {@link Resource}s per host
     */
    static public final int maxEntries = 8;
    
    /**
     * Constructs a {@link Resource} {@link Menu}.
     */
    static public Menu<ByteArray>
    make(final String title) {
        class Host implements Menu<ByteArray>, Serializable {
            static private final long serialVersionUID = 1L;

            private ConstArray<ResourceVariable> vars =
                ConstArray.array(new ResourceVariable[] {});

            public Promise<Snapshot<ByteArray>>
            getSnapshot() {
                final ConstArray.Builder<Copy<ByteArray>> r =
                    ConstArray.builder(vars.length());
                for (final ResourceVariable x : vars) {
                    r.append(new Copy<ByteArray>(x.get(), x));
                }
                return ref(new Snapshot<ByteArray>(title, r.snapshot()));
            }

            public Receiver<ByteArray>
            grow() {
                if (vars.length() == maxEntries) { throw new TooMany(); }
                
                final ResourceVariable var = new ResourceVariable();
                var.apply(Resource.localhost);
                vars = vars.with(var);
                return var;
            }
            
            public void
            remove(final Receiver<ByteArray> entry) {
                for (int i = vars.length(); 0 != i--;) {
                    if (vars.get(i).equals(entry)) {
                        vars = vars.without(i);
                        break;
                    }
                }
            }
        }
        return new Host();
    }
}
