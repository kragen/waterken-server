// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.folder;

import static org.ref_send.promise.Eventual.ref;

import java.io.Serializable;

import org.joe_e.array.ByteArray;
import org.joe_e.array.PowerlessArray;
import org.ref_send.promise.Promise;

/**
 * A {@link Folder} implementation.
 */
public final class
FolderMaker {
    private FolderMaker() {}
    
    static public Folder
    make() {
        class FolderX implements Folder, Serializable {
            static private final long serialVersionUID = 1L;
            
            private PowerlessArray<String> names = PowerlessArray.array();
            private PowerlessArray<ByteArray> values = PowerlessArray.array();

            public Promise<ByteArray>
            get(final String name) {
                for (int i = names.length(); 0 != i--;) {
                    if (names.get(i).equals(name)) {
                        return ref(values.get(i));
                    }
                }
                return ref(null);
            }

            public Void
            run(final String name, final ByteArray value) {
                for (int i = names.length(); 0 != i--;) {
                    if (names.get(i).equals(name)) {
                        names = names.without(i);
                        values = values.without(i);
                    }
                }
                names = names.with(name);
                values = values.with(value);
                return null;
            }
            
        }
        return new FolderX();
    }
}
