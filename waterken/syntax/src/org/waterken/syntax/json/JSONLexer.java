// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax.json;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;

/**
 * A JSON token reader.
 */
public final class
JSONLexer {
    
    /**
     * A text reader that keeps track of the line and column number, as well as
     * the last character read.
     */
    static private final class
    Stream {
        private final Reader in;
        private       int line;
        private       int column;
        private       int head;
        
        protected
        Stream(final Reader in) {
            this.in = in;
            line = 0;
            column = 0;
            head = '\n';
        }
        
        protected int
        getLine() { return line; }
        
        protected int
        getColumn() { return column; }
        
        protected int
        getHead() { return head; }
        
        /**
         * all Unicode line terminators
         */
        static protected final String newLine = "\n\r\u0085\u000C\u2028\u2029";
        
        protected int
        read() throws IOException {
            if (-1 == head) { throw new EOFException(); }
            
            final int next = in.read();
            if (-1 == newLine.indexOf(head) || ('\r' == head && '\n' == next)) {
                ++column;
            } else {
                ++line;
                column = 1;
            }
            return head = next;
        }
        
        public void
        close() throws IOException {
            head = -1;
            in.close();
        }
    }
    
    private final Stream s;
    private       int line;
    private       int column;
    private       String head;
    
    /**
     * Constructs an instance.
     * @param in    UTF-8 input stream
     */
    public
    JSONLexer(final Reader in) {
        s = new Stream(in);
        line = s.getLine();
        column = s.getColumn();
        head = null;
    }
    
    // org.waterken.syntax.json.JSONLexer interface
    
    /**
     * Gets the starting line of the last {@linkplain #next read} token.
     */
    public int
    getStartLine() { return line; }
    
    /**
     * Gets the starting column of the last {@linkplain #next read} token.
     */
    public int
    getStartColumn() { return column; }
    
    /**
     * Gets the last {@linkplain #next read} token.
     * @return last token, or <code>null</code> if EOF
     */
    public String
    getHead() { return head; }
    
    /**
     * Move to the next token in the input stream.
     * @return newly read token, or <code>null</code> if EOF
     * @throws IOException  any I/O error
     * @throws Exception    invalid character escape
     */
    public String
    next() throws IOException, Exception {
        final int c = skipWhitespace(s);
        line = s.getLine();
        column = s.getColumn();
        switch (c) {
        case -1:
            head = null;
            break;
        case ',':
            head = ",";
            s.read();
            break;
        case '{':
            head = "{";
            s.read();
            break;
        case ':':
            head = ":";
            s.read();
            break;
        case '}':
            head = "}";
            s.read();
            break;
        case '[':
            head = "[";
            s.read();
            break;
        case ']':
            head = "]";
            s.read();
            break;
        case '\"':
            head = readString(s);
            break;
        default:
            head = readKeyword(s);
        }
        return head;
    }
    
    public void
    close() throws IOException {
        s.close();
    }
    
    // rest of implementation consists of static helper functions
    
    static private final String whitespace = " \t" + Stream.newLine;
    
    static private int
    skipWhitespace(final Stream s) throws IOException {
        int c = s.getHead();
        while (whitespace.indexOf(c) != -1) {
            c = s.read();
        }
        return c;
    }
    
    static private final String delimiter = whitespace + ",{:}[]\"";
    
    static private String
    readKeyword(final Stream s) throws IOException {
        final StringBuilder r = new StringBuilder();
        int c = s.getHead();
        do {
            r.append((char)c);
            c = s.read();
        } while (-1 != c && delimiter.indexOf(c) == -1);
        return r.toString();
    }
    
    static private String
    readString(final Stream s) throws Exception {
        final StringBuilder r = new StringBuilder();
        r.append((char)s.getHead());
        while (true) {
            final int c = s.read();
            if ('\\' == c) {
                r.append(readEscape(s));
            } else {
                r.append((char)c);
                if ('\"' == c) {
                    s.read();
                    return r.toString();
                }
            }
        }
    }
    
    static private char
    readEscape(final Stream s) throws Exception {
        switch (s.read()) {
        case '\"': return '\"';
        case '\\': return '\\';
        case '/': return '/';
        case 'b': return '\b';
        case 'f': return '\f';
        case 'n': return '\n';
        case 'r': return '\r';
        case 't': return '\t';
        case 'u': return readUnicodeEscape(s);
        default: throw new Exception("0x" + Integer.toHexString(s.getHead()));
        }
    }
    
    static private char
    readUnicodeEscape(final Stream s) throws Exception {
        return (char)((hex(s.read()) << 12) |
                      (hex(s.read()) <<  8) |
                      (hex(s.read()) <<  4) |
                      (hex(s.read())      ) );
    }
    
    static private int
    hex(final int c) throws Exception {
        if ('0' <= c && '9' >= c) {        return (c - '0'     ) & 0x0F; }
        else if ('A' <= c && 'F' >= c) {   return (c - 'A' + 10) & 0x0F; }
        else if ('a' <= c && 'f' >= c) {   return (c - 'a' + 10) & 0x0F; }
        else { throw new Exception("0x" + Integer.toHexString(c)); }
    }
}