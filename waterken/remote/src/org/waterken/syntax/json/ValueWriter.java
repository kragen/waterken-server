// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax.json;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * A JSON writer.
 */
/* package */ final class
ValueWriter {
    static protected final String newLine = "\r\n";
    static private   final String tab = "  ";
    
    private final String indent;
    private       Writer out;
    private       boolean written;
    
    protected
    ValueWriter(final String indent, final Writer out) {
        this.indent = indent;
        this.out = out;
        written = null == out;
    }
    
    protected boolean
    isWritten() { return written; }
    
    protected ObjectWriter
    startObject() throws IOException {
        final Writer out = this.out;
        this.out = null;
        out.write("{");
        return new ObjectWriter(out);
    }
    
    protected final class
    ObjectWriter {
        static private final String comma = "," + newLine;
        
        private final String inset;
        private       String separator;
        private       Writer out;
        private       ValueWriter member;
        
        protected
        ObjectWriter(final Writer out) {
            inset = indent + tab;
            separator = newLine;
            this.out = out;
            member = new ValueWriter(inset, null);
        }
        
        protected void
        close() throws IOException {
            if (!member.isWritten()) { throw new NullPointerException(); }
            
            out.write(newLine);
            out.write(indent);
            out.write("}");
            out = null;
            member = null;
            written = true;
        }
        
        protected ValueWriter
        startMember(final String name) throws IOException {
            if (!member.isWritten()) { throw new NullPointerException(); }
            
            out.write(separator);
            out.write(inset);
            writeStringTo(name, out);
            out.write(" : ");
            separator = comma;
            return member = new ValueWriter(inset, out);
        }
    }
    
    protected ArrayWriter
    startArray() throws IOException {
        final Writer out = this.out;
        this.out = null;
        out.write("[");
        return new ArrayWriter(out);
    }
    
    protected final class
    ArrayWriter {
        static private final String comma = ", ";
        
        private final String inset;
        private       String separator;
        private       Writer out;
        private       ValueWriter member;
        
        protected
        ArrayWriter(final Writer out) {
            inset = indent + tab;
            separator = " ";
            this.out = out;
            member = new ValueWriter(inset, null);
        }

        protected void
        close() throws IOException {
            if (!member.isWritten()) { throw new NullPointerException(); }
            
            out.write(" ]");
            out = null;
            member = null;
            written = true;
        }
        
        protected ValueWriter
        startElement() throws IOException {
            if (!member.isWritten()) { throw new NullPointerException(); }
            
            out.write(separator);
            separator = comma;
            return member = new ValueWriter(inset, out);
        }
    }
    
    protected void
    writeLink(final String URL) throws IOException {
        final Writer out = this.out;
        this.out = null;
        out.write("{ \"@\" : ");
        writeStringTo(URL, out);
        out.write(" }");
        written = true;
    }
    
    protected void
    writeNull() throws IOException {
        final Writer out = this.out;
        this.out = null;
        out.write("null");
        written = true;
    }
    
    protected void
    writeBoolean(final boolean value) throws IOException {
        final Writer out = this.out;
        this.out = null;
        out.write(value ? "true" : "false");
        written = true;
    }
    
    protected void
    writeByte(final byte value) throws IOException {
        final Writer out = this.out;
        this.out = null;
        out.write(Byte.toString(value));
        written = true;
    }
    
    protected void
    writeShort(final short value) throws IOException {
        final Writer out = this.out;
        this.out = null;
        out.write(Short.toString(value));
        written = true;
    }
    
    protected void
    writeInt(final int value) throws IOException {
        final Writer out = this.out;
        this.out = null;
        out.write(Integer.toString(value));
        written = true;
    }
    
    protected void
    writeLong(final long value) throws IOException {
        final Writer out = this.out;
        this.out = null;
        out.write(Long.toString(value));
        written = true;
    }
    
    protected void
    writeInteger(final BigInteger value) throws IOException {
        final Writer out = this.out;
        this.out = null;
        out.write(value.toString());
        written = true;
    }
    
    protected void
    writeFloat(final float value) throws IOException {
        final Writer out = this.out;
        this.out = null;
        out.write(Float.toString(value));
        written = true;
    }
    
    protected void
    writeDouble(final double value) throws IOException {
        final Writer out = this.out;
        this.out = null;
        out.write(Double.toString(value));
        written = true;
    }
    
    protected void
    writeDecimal(final BigDecimal value) throws IOException {
        final Writer out = this.out;
        this.out = null;
        out.write(value.toPlainString());
        written = true;
    }
    
    protected void
    writeString(final String value) throws IOException {
        final Writer out = this.out;
        this.out = null;
        writeStringTo(value, out);
        written = true;
    }
    
    static protected void
    writeStringTo(final String value, final Writer out) throws IOException {
        out.write("\"");
        final int len = value.length();
        for (int i = 0; i != len; ++i) {
            final char c = value.charAt(i);
            switch (c) {
            case '\"':
                out.write("\\\"");
                break;
            case '\\':
                out.write("\\\\");
                break;
            case '\b':
                out.write("\\b");
                break;
            case '\f':
                out.write("\\f");
                break;
            case '\n':
                out.write("\\n");
                break;
            case '\r':
                out.write("\\r");
                break;
            case '\t':
                out.write("\\t");
                break;
            case ' ':
                out.write(c);
                break;
            default:
                switch (Character.getType(c)) {
                case Character.UPPERCASE_LETTER:
                case Character.LOWERCASE_LETTER:
                case Character.TITLECASE_LETTER:
                case Character.MODIFIER_LETTER:
                case Character.OTHER_LETTER:
                case Character.NON_SPACING_MARK:
                case Character.ENCLOSING_MARK:
                case Character.COMBINING_SPACING_MARK:
                case Character.DECIMAL_DIGIT_NUMBER:
                case Character.LETTER_NUMBER:
                case Character.OTHER_NUMBER:
                case Character.DASH_PUNCTUATION:
                case Character.START_PUNCTUATION:
                case Character.END_PUNCTUATION:
                case Character.CONNECTOR_PUNCTUATION:
                case Character.OTHER_PUNCTUATION:
                case Character.MATH_SYMBOL:
                case Character.CURRENCY_SYMBOL:
                case Character.MODIFIER_SYMBOL:
                case Character.INITIAL_QUOTE_PUNCTUATION:
                case Character.FINAL_QUOTE_PUNCTUATION:
                    out.write(c);
                    break;
                default:
                    out.write("\\u");
                    final int u = c;
                    for (int shift = 16; 0 != shift;) {
                        shift -= 4;
                        final int h = (u >> shift) & 0x0F;
                        out.write(h < 10 ? '0' + h : 'A' + (h - 10));
                    }
                }
            }
        }
        out.write("\"");
    }
}
