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
        super(phrase);
        this.status = status;
        this.phrase = phrase;
    }
    
    // org.web_send.Failure interface
    
    /**
     * 404 Not Found
     */
    static public Failure
    notFound() { return new Failure("404", "Not Found"); }

    /**
     * 410 Gone
     */
    static public Failure
    gone() { return new Failure("410", "Gone"); }
    
    /**
     * 413 Request Entity Too Large
     */
    static public Failure
    tooBig() { return new Failure("413", "Request Entity Too Large"); }
    
    /**
     * 415 Unsupported Media Type
     */
    static public Failure
    notSupported() { return new Failure("415", "Unsupported Media Type"); }
}
