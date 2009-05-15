// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Information about a messaging session.
 */
public class
SessionInfo extends Struct implements Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * secret key for messaging
     */
    public final String sessionKey;
    
    /**
     * GUID for session logging
     */
    public final String sessionName;
    
    /**
     * Constructs an instance.
     * @param sessionKey    {@link #sessionKey}
     * @param sessionName   {@link #sessionName}
     */
    public @deserializer
    SessionInfo(@name("sessionKey") final String sessionKey,
                @name("sessionName") final String sessionName) {
        this.sessionKey = sessionKey;
        this.sessionName = sessionName;
    }
}
