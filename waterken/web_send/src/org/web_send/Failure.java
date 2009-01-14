// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.web_send;

import org.joe_e.Powerless;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Indicates a failed HTTP request.
 */
public class
Failure extends NullPointerException implements Powerless, Record {
    static private final long serialVersionUID = 1L;

    /**
     * entity size expected to trigger a <code>413</code> response: {@value}
     */
    static public final int maxEntitySize = 256 * 1024;
    
    /**
     * HTTP status code
     */
    public final String status;
    
    /**
     * HTTP reason phrase
     */
    public final String phrase;
    
    /**
     * Constructs an instance.
     * @param status    {@link #status}
     * @param phrase    {@link #phrase}
     */
    public @deserializer
    Failure(@name("status") final String status,
            @name("phrase") final String phrase) {
        this.status = status;
        this.phrase = phrase;
    }
}
