// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * An HTTP message.
 */
public class 
Message<Head extends Powerless> extends Struct
                                implements Powerless, Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * either {@link Request} or {@link Response}
     */
    public final Head head;
    
    /**
     * message-body
     */
    public final ByteArray body;
    
    /**
     * Constructs an instance.
     * @param head  {@link #head}
     * @param body  {@link #body}
     */
    public @deserializer
    Message(@name("head") final Head head,
            @name("body") final ByteArray body) {
        this.head = head;
        this.body = body;
    }
}
