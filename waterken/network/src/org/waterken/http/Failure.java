// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http;

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
     * 404 Not Found
     */
    static public final Failure notFound = new Failure("404", "Not Found");
    
    /**
     * 410 Gone
     */
    static public final Failure gone = new Failure("410", "Gone");
    
    /**
     * 415 Unsupported Media Type
     */
    static public final Failure unsupported =
        new Failure("415", "Unsupported Media Type");

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
        super(phrase);
        this.status = status;
        this.phrase = phrase;
    }
}
