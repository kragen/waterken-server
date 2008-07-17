// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax.json;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;

/**
 * 
 */
public final class
JSONLexer {
    
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
        
        public void
        close() throws IOException {
            in.close();
        }
        
        protected int
        advance() throws IOException {
            if (-1 == head) { throw new EOFException(); }
            if ('\n' == head) {
                ++line;
                column = 1;
            } else {
                ++column;
            }
            return head = in.read();
        }
    }
    
    private final Stream s;
    private       int line;
    private       int column;
    private       String head;
    
    public
    JSONLexer(final Reader in) {
        s = new Stream(in);
        line = s.getLine();
        column = s.getColumn();
        head = null;
    }
    
    public int
    getLine() { return line; }
    
    public int
    getColumn() { return column; }
    
    public String
    getHead() { return head; }
    
    public void
    close() throws IOException {
        s.close();
    }
    
    public String
    advance() throws Exception {
        final int c = skipWhitespace(s);
        line = s.getLine();
        column = s.getColumn();
        switch (c) {
        case -1:
            head = null;
            break;
        case ',':
            head = ",";
            s.advance();
            break;
        case '{':
            head = "{";
            s.advance();
            break;
        case ':':
            head = ":";
            s.advance();
            break;
        case '}':
            head = "}";
            s.advance();
            break;
        case '[':
            head = "[";
            s.advance();
            break;
        case ']':
            head = "]";
            s.advance();
            break;
        case '\"':
            head = readString(s);
            break;
        default:
            head = readKeyword(s);
        }
        return head;
    }
    
    static private final String whitespace = " \n\r\t";
    
    static private int
    skipWhitespace(final Stream s) throws IOException {
        int c = s.getHead();
        while (whitespace.indexOf(c) != -1) {
            c = s.advance();
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
            c = s.advance();
        } while (-1 != c && delimiter.indexOf(c) == -1);
        return r.toString();
    }
    
    static private String
    readString(final Stream s) throws Exception {
        final StringBuilder r = new StringBuilder();
        r.append((char)s.getHead());
        while (true) {
            final int c = s.advance();
            if ('\\' == c) {
                r.append(readEscape(s));
            } else {
                r.append((char)c);
                if ('\"' == c) {
                    s.advance();
                    return r.toString();
                }
            }
        }
    }
    
    static private char
    readEscape(final Stream s) throws Exception {
        switch (s.advance()) {
        case '\"': return '\"';
        case '\\': return '\\';
        case '/': return '/';
        case 'b': return '\b';
        case 'f': return '\f';
        case 'n': return '\n';
        case 'r': return '\r';
        case 't': return '\t';
        case 'u': return readUnicodeEscape(s);
        default: throw new Exception();
        }
    }
    
    static private char
    readUnicodeEscape(final Stream s) throws Exception {
        return (char)((hex(s.advance()) << 12) |
                      (hex(s.advance()) <<  8) |
                      (hex(s.advance()) <<  4) |
                      (hex(s.advance())      ) );
    }
    
    static private int
    hex(final int c) throws Exception {
        if ('0' <= c && '9' >= c) {
            return (c - '0') & 0x0F;
        } else if ('A' <= c && 'F' >= c) {
            return (c - 'A' + 10) & 0x0F;
        } else if ('a' <= c && 'f' >= c) {
            return (c - 'a' + 10) & 0x0F;
        } else {
            throw new Exception();
        }
    }
}