// Copyright 2008 Waterken Inc. under the terms of the MIT X license found at
// http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;

/**
 * A text reader that keeps track of the current line and column number, as well
 * as the most recent character {@linkplain #read read}.
 */
public final class
SourceReader {
    private final Reader in;
    private       int line; 
    private       int column;
    private       int head;
    
    /**
     * Constructs an instance.
     * @param in    underlying character stream
     */
    public
    SourceReader(final Reader in) {
        this.in = in;
        line = 0;       // position (0, 0) means nothing has been read yet
        column = 0;
        head = '\n';    // will put the stream at (1, 1) on first read operation
    }
    
    /**
     * Gets the line number of the {@linkplain #getHead head} character.
     */
    public int
    getLine() { return line; }
    
    /**
     * Gets the column number of the {@linkplain #getHead head} character.
     */
    public int
    getColumn() { return column; }
    
    /**
     * Gets the last {@link #read read} character, or {@code -1} for EOF.
     */
    public int
    getHead() { return head; }
    
    /**
     * all Unicode line terminators
     */
    static public final String newLine = "\n\r\u0085\u000C\u2028\u2029";

    /**
     * Gets the next character in the stream.
     * @throws EOFException attempt to read past EOF
     * @throws IOException  any I/O problem
     */
    public int
    read() throws EOFException, IOException {
        if (-1 == head) { throw new EOFException(); }
        
        final int next = in.read();
        if (-1 == newLine.indexOf(head) || ('\r' == head && '\n' == next)) {
            column += 1;
        } else {
            line += 1;
            column = 1;
        }
        return head = next;
    }
    
    /**
     * Closes the text stream.
     * @throws IOException  any I/O problem
     */
    public void
    close() throws IOException {
        if (-1 != head) {
            column += 1;
            head = -1;
        }
        in.close();
    }
}