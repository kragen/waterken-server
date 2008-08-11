// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax.json;

import org.joe_e.Powerless;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Signals invalid JSON text.
 */
public class
BadFormat extends Exception implements Powerless, Record {
    static private final long serialVersionUID = 1L;

    /**
     * path to the source code
     */
    public final String source;

    /**
     * line number
     */
    public final int line;
    
    /**
     * column number
     */
    public final int column;
    
    /**
     * Constructs an instance.
     * @param source    {@link #source}
     * @param line      {@link #line}
     * @param column    {@link #column}
     */
    public @deserializer
    BadFormat(@name("source") final String source,
              @name("line") final int line,
              @name("column") final int column,
              @name("cause") final Exception cause) {
        super("<" + source + "> ( " + line + ", " + column + " ) : ", cause);
        this.source = source;
        this.line = line;
        this.column = column;
    }
}
