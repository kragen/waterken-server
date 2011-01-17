// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax.json;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;

import org.waterken.syntax.SourceReader;

/**
 * A JSON token reader.
 */
public final class
JSONLexer {
    private final SourceReader s;
    private       int line;
    private       int column;
    private       String head;
    
    /**
     * Constructs an instance.
     * @param in    UTF-8 input stream
     */
    public
    JSONLexer(final Reader in) {
        s = new SourceReader(in);
        line = s.getLine();
        column = s.getColumn();
        head = "";              // empty token indicates start of token stream
    }
    
    // org.waterken.syntax.json.JSONLexer interface
    
    /**
     * Gets the line number of the {@linkplain #getHead head} token.
     */
    public int
    getLine() { return line; }
    
    /**
     * Gets the column number of the {@linkplain #getHead head} token.
     */
    public int
    getColumn() { return column; }
    
    /**
     * Gets the most recently {@linkplain #next read} token.
     * @return most recent token, or {@code null} if EOF
     */
    public String
    getHead() { return head; }
    
    /**
     * Move to the next token in the input stream.
     * @return newly read token
     * @throws EOFException EOF
     * @throws IOException  any I/O error
     * @throws Exception    invalid character escape
     */
    public String
    next() throws EOFException, IOException, Exception {
        final int c = skipWhitespace(s);
        line = s.getLine();
        column = s.getColumn();
        switch (c) {
        case -1:
            head = null;
            throw new EOFException();
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
        final int c = skipWhitespace(s);
        line = s.getLine();
        column = s.getColumn();
        s.close();
        if (-1 != c) { throw new IllegalStateException(); }
    }
    
    // rest of implementation consists of static helper functions
    
    static private final String whitespace = " \t" + SourceReader.newLine;
    
    static private int
    skipWhitespace(final SourceReader s) throws IOException {
        int c = s.getHead();
        while (whitespace.indexOf(c) != -1) {
            c = s.read();
        }
        return c;
    }
    
    static private final String delimiter = whitespace + ",{:}[]\"";
    
    static private String
    readKeyword(final SourceReader s) throws IOException {
        final StringBuilder r = new StringBuilder();
        int c = s.getHead();
        do {
            r.append((char)c);
            c = s.read();
        } while (-1 != c && delimiter.indexOf(c) == -1);
        return r.toString();
    }
    
    static private String
    readString(final SourceReader s) throws Exception {
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
    readEscape(final SourceReader s) throws Exception {
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
    readUnicodeEscape(final SourceReader s) throws Exception {
        return (char)((hex(s.read()) << 12) |
                      (hex(s.read()) <<  8) |
                      (hex(s.read()) <<  4) |
                      (hex(s.read())      ) );
    }
    
    static private int
    hex(final int c) throws Exception {
        if ('0' <= c && '9' >= c) {        return c - '0'     ; }
        else if ('A' <= c && 'F' >= c) {   return c - 'A' + 10; }
        else if ('a' <= c && 'f' >= c) {   return c - 'a' + 10; }
        else { throw new Exception("0x" + Integer.toHexString(c)); }
    }
}