// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.log;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.IntArray;
import org.joe_e.array.PowerlessArray;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * A source code location.
 */
public class
CallSite extends Struct implements Powerless, Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * path to the source code containing the call site
     */
    public final String source;
    
    /**
     * call site's human meaningful name within the {@linkplain #source}
     */
    public final String name;
    
    /**
     * call site's position within the {@linkplain #source} (optional)
     * <p>
     * The expected structure of this table defines a span from the start of the
     * relevant source code to the end. The first row in the table is the start
     * of the span and the second row is the end of the span. Each row lists the
     * line number followed by the column number. For example, a span of code
     * starting on line 5, column 8 and ending on line 6, column 12 is encoded
     * as:
     * </p>
     * <p>
     * <code>[ [ 5, 8 ], [ 6, 12 ] ]</code>
     * </p>
     * <p>
     * The delimited span is inclusive, meaning the character at line 6, column
     * 12 is included in the span defined above.
     * </p>
     * <p>
     * If the end of the span is unknown, it may be omitted. If the column
     * number is unknown, it may also be omitted. For example, in the case where
     * only the starting line number is known:
     * </p>
     * <p>
     * <code>[ [ 5 ] ]</code>
     * </p>
     * <p>
     * If source span information is unknown, this member is <code>null</code>.
     * </p>
     * <p>
     * Both lines and columns are numbered starting from one, so the first
     * character in a source file is at <code>[ 1, 1 ]</code>. A column is a
     * UTF-16 code unit, the same unit represented by a Java <code>char</code>.
     * Lines are separated by any character sequence considered a Unicode <a
     * href="http://en.wikipedia.org/wiki/Newline#Unicode">line terminator</a>.
     * </p>
     */
    public final PowerlessArray<IntArray> span;
    
    /**
     * Constructs an instance.
     * @param source    {@link #source}
     * @param name      {@link #name}
     * @param span      {@link #span}
     */
    public @deserializer
    CallSite(@name("source") final String source,
             @name("name") final String name,
             @name("span") final PowerlessArray<IntArray> span) {
        this.source = source;
        this.name = name;
        this.span = span;
    }
}
