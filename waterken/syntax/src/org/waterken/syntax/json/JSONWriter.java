// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax.json;

import java.io.IOException;
import java.io.Writer;

/**
 * A SAX-like API for generating JSON text.
 * <p>
 * A client can only output a syntactically correct JSON text, or leave the
 * {@link JSONWriter} in a {@linkplain #isWritten detectable} error state. The
 * implementation does <em>not</em> enforce the constraint that names within an
 * object SHOULD be unique.
 * </p>
 * <p>For example, to output the JSON text:</p>
 * <pre>
 * {
 *   "title" : "I Can Has Cheezburger?",
 *   "src" : { "@" : "http://www.example.com/image/481989943" },
 *   "height" : 125,
 *   "width" : 100,
 *   "tags" : [ "lolcat", "food" ],
 *   "score" : 9.5
 * }
 * </pre>
 * <p>, write code:</p>
 * <pre>
 * final Writer text = &hellip;
 * final JSONWriter top = JSONWriter.make(text);
 * final JSONWriter.ObjectWriter o = top.startObject();
 * o.startMember("title").writeString("I Can Has Cheezburger?");
 * o.startMember("src").writeLink("http://www.example.com/image/481989943");
 * o.startMember("height").writeInt(125);
 * o.startMember("width").writeInt(100);
 * final JSONWriter.ArrayWriter tags = o.startMember("tags").startArray();
 * tags.startElement().writeString("lolcat");
 * tags.startElement().writeString("food");
 * tags.finish();
 * o.startMember("score").writeDouble(9.5);
 * o.finish();
 * if (!top.isWritten()) { throw new NullPointerException(); }
 * text.flush();
 * text.close();
 * </pre>
 * <p>
 * An invalid sequence of calls to this API will cause a
 * {@link NullPointerException} to be thrown. For example, calling
 * writeString() twice in succession will throw a {@link NullPointerException}.
 * </p>
 */
public final class
JSONWriter {
    static private final String newLine = "\r\n";
    static private final String tab = "  ";

    static private final class
    Prize<T> {
        private T value;

        protected
        Prize(final T value) {
            this.value = value;
        }

        protected T
        claim() {
            final T r = value;
            value = null;
            return r;
        }
    }

    static private final class
    Milestone {
        private boolean marked;

        protected
        Milestone(final boolean marked) {
            this.marked = marked;
        }

        protected boolean
        is() { return marked; }

        protected void
        mark() { marked = true; }
    }

    protected final boolean top;        // Is this the top level JSON container?
    protected final String indent;      // indentation for this JSON value
    private final Prize<Writer> output; // claimed by 1st called output method
    protected final Milestone written;  // marked after output method is done  

    protected
    JSONWriter(final boolean top, final String indent, final Writer out) {
        this.top = top;
        this.indent = indent;
        output = new Prize<Writer>(out);
        written = new Milestone(null == out);
    }
    
    /**
     * Constructs a JSON writer.
     * @param out   UTF-8 output stream
     */
    static public JSONWriter
    make(final Writer out) { return new JSONWriter(true, "", out); }

    /**
     * @return {@code true} if a single JSON value was successfully written,
     *         else {@code false}
     */
    public boolean
    isWritten() { return written.is(); }

    public ObjectWriter
    startObject() throws IOException {
        final Writer out = output.claim();
        out.write('{');
        return new ObjectWriter(out);
    }

    /**
     * A JSON <em>object</em> writer.
     */
    public final class
    ObjectWriter {
        static private final String comma = "," + newLine;

        private final String inset;         // indentation for each member
        private final Writer out;
        private       String prefix;        // current member separator prefix
        private       JSONWriter member;    // most recent member started, or
                                            // null if object is finished

        protected
        ObjectWriter(final Writer out) {
            inset = indent + tab;
            this.out = out;
            prefix = newLine;
            member = new JSONWriter(false, inset, null);
        }

        public void
        finish() throws IOException {
            if (!member.isWritten()) { throw new NullPointerException(); }

            member = null;      // prevent future calls to this object
            out.write(newLine);
            out.write(indent);
            out.write('}');
            if (top) { out.write(newLine); }
            written.mark();     // mark the containing value successful
        }

        public JSONWriter
        startMember(final String name) throws IOException {
            if (!member.isWritten()) { throw new NullPointerException(); }

            member = new JSONWriter(false, inset, out); // prevent calls until
                                                        // member is completed
            out.write(prefix);
            out.write(inset);
            writeStringTo(name, out);
            out.write(" : ");
            prefix = comma;
            return member;
        }
        
        /*
         * Every output method on a JSONWriter begins by deleting the output
         * stream from that writer. If the created JSON value is an object or an
         * array, the output stream is handed off to either an ObjectWriter or
         * an ArrayWriter. These JSON container writers hold onto the output
         * stream forever, but know whether or not they should be allowed to
         * write to it. Each container does this by remembering its most
         * recently created child value and only writing to the output stream if
         * that child has been written and the container itself has not been
         * finished. At any time, there may be multiple unfinished containers,
         * but only one of them could have a written child but not itself be
         * finished, since a JSON structure is a tree and an unfinished
         * container value is not marked as written.
         */
    }

    public ArrayWriter
    startArray() throws IOException {
        final Writer out = output.claim();
        out.write('[');
        return new ArrayWriter(out);
    }

    /**
     * A JSON <em>array</em> writer.
     */
    public final class
    ArrayWriter {
        static private final String comma = ", ";

        private final String inset;         // indentation for each element
        private final Writer out;
        private       String prefix;        // current element separator prefix
        private       JSONWriter element;   // most recent element started, or
                                            // null if array is finished

        protected
        ArrayWriter(final Writer out) {
            inset = indent + tab;
            this.out = out;
            prefix = " ";
            element = new JSONWriter(false, inset, null);
        }

        public void
        finish() throws IOException {
            if (!element.isWritten()) { throw new NullPointerException(); }

            element = null;     // prevent future calls to this object
            out.write(" ]");
            if (top) { out.write(newLine); }
            written.mark();     // mark the containing value successful
        }

        public JSONWriter
        startElement() throws IOException {
            if (!element.isWritten()) { throw new NullPointerException(); }

            element = new JSONWriter(false, inset, out); // prevent calls until
                                                         // element is completed
            out.write(prefix);
            prefix = comma;
            return element;
        }
    }

    public void
    writeLink(final String URL) throws IOException {
        final Writer out = output.claim();
        out.write("{ \"@\" : ");
        writeStringTo(URL, out);
        out.write(" }");
        if (top) { out.write(newLine); }
        written.mark();
    }

    public void
    writeNull() throws IOException {
        writePrimitive("null");
    }

    public void
    writeBoolean(final boolean value) throws IOException {
        writePrimitive(value ? "true" : "false");
    }

    public void
    writeInt(final int value) throws IOException {
        writePrimitive(Integer.toString(value));
    }
    
    /**
     * maximum magnitude of a JavaScript number: {@value}
     */
    static public final long maxMagnitude = 1L << 53;   // = 2^53
    
    /**
     * Signals a number that cannot be represented in JavaScript.
     */
    static public final ArithmeticException NaN = new ArithmeticException();

    public void
    writeLong(final long value) throws ArithmeticException, IOException {
        if (value > maxMagnitude) { throw NaN; }
        if (value < -maxMagnitude) { throw NaN; }

        writePrimitive(Long.toString(value));
    }

    public void
    writeFloat(final float value) throws ArithmeticException, IOException {
        if (Float.isNaN(value)) { throw NaN; }
        if (Float.isInfinite(value)) { throw NaN; }
        
        writePrimitive(Float.toString(value));
    }

    public void
    writeDouble(final double value) throws ArithmeticException, IOException {
        if (Double.isNaN(value)) { throw NaN; }
        if (Double.isInfinite(value)) { throw NaN; }
        
        writePrimitive(Double.toString(value));
    }
    
    private void
    writePrimitive(final String value) throws IOException {
        final Writer out = output.claim();
        if (top) {
            out.write("{ \"=\" : ");
            out.write(value);
            out.write(" }");
            out.write(newLine);
        } else {
            out.write(value);
        }
        written.mark();
    }

    public void
    writeString(final String value) throws IOException {
        final Writer out = output.claim();
        if (top) {
            out.write("{ \"=\" : ");
            writeStringTo(value, out);
            out.write(" }");
            out.write(newLine);
        } else {
            writeStringTo(value, out);
        }
        written.mark();
    }

    static protected void
    writeStringTo(final String value, final Writer out) throws IOException {
        out.write('\"');
        char previous = '\0';
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
            // begin: HTML escaping
            case '/':
                if ('<' == previous) { out.write('\\'); }
                out.write(c);
                break;
            // need at least the above check, but paranoia demands more
            case '<':
                out.write("\\u003C");
                break;
            case '>':
                out.write("\\u003E");
                break;
            // end: HTML escaping
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
                case Character.UNASSIGNED:
                case Character.SPACE_SEPARATOR:
                case Character.LINE_SEPARATOR:
                case Character.PARAGRAPH_SEPARATOR:
                case Character.CONTROL:                 // includes '\u0085'
                case Character.FORMAT:
                case Character.PRIVATE_USE:
                case Character.SURROGATE:
                case Character.OTHER_SYMBOL:
                default:
                    out.write("\\u");
                    final int unicode = c;
                    for (int shift = Character.SIZE; 0 != shift;) {
                        shift -= 4;
                        final int hex = (unicode >> shift) & 0x0F;
                        out.write(hex < 10 ? '0' + hex : 'A' + (hex - 10));
                    }
                }
            }
            previous = c;
        }
        out.write('\"');
    }
}
