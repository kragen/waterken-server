// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.array.ConstArray;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * The set of remote hosts with pending requests.
 */
final class
Outbound implements Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * A host request queue.
     */
    static final class
    Entry extends Struct implements Record, Serializable {
        static private final long serialVersionUID = 1L;

        /**
         * remote host identifier
         */
        public final String peer;
        
        /**
         * corresponding request queue
         */
        public final Pipeline msgs;
       
        /**
         * Constructs an instance.
         * @param peer  {@link #peer}
         * @param msgs  {@link #msgs}
         */
        public @deserializer
        Entry(@name("peer") final String peer,
              @name("msgs") final Pipeline msgs) {
            this.peer = peer;
            this.msgs = msgs;
        }
    }
    
    /**
     * pending peers
     */
    private ConstArray<Entry> pending;
    
    Outbound() {
        pending = ConstArray.array();
    }
    
    // org.waterken.remote.http.Outbound interface
    
    /**
     * pending peers
     */
    ConstArray<Entry>
    getPending() { return pending; }

    Pipeline
    find(final String peer) {
        for (final Entry x : pending) {
            if (x.peer.equals(peer)) { return x.msgs; }
        }
        return null;
    }
    
    void
    add(final String peer, final Pipeline msgs) {
        pending = pending.with(new Entry(peer, msgs));
    }
    
    void
    remove(final String peer) {
        final Entry[] next = new Entry[pending.length() - 1];
        int i = 0;
        for (final Entry x : pending) {
            if (!peer.equals(x.peer)) {
                next[i++] = x;
            }
        }
        pending = ConstArray.array(next);
    }
}
