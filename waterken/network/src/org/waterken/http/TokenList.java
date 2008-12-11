// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http;

import org.joe_e.array.ArrayBuilder;
import org.joe_e.array.PowerlessArray;
import org.waterken.uri.Header;

/**
 * Parses a <code>token</code> list.
 */
public final class
TokenList {

    private
    TokenList() {}

    /**
     * Encodes a list of <code>token</code>.
     * @param token each <code>token</code>
     * @return encoded <code>token</code> list
     */
    static public String
    encode(final String... token) {
        if (0 == token.length) { return ""; }
        if (1 == token.length) { return token[0]; }
        final StringBuilder r = new StringBuilder();
        r.append(token[0]);
        for (int i = 1; i != token.length; ++i) {
            r.append(", ");
            r.append(token[i]);
        }
        return r.toString();
    }

    /**
     * Decodes a <code>token</code> list.
     * @param list  <code>token</code> list
     * @return <code>token</code> array
     */
    static public PowerlessArray<String>
    decode(final String list) {
    	final PowerlessArray.Builder<String> r = PowerlessArray.builder();
        final int end = list.length();
        int i = 0;
        while (true) {
        	i = skip(whitespace, nothing, list, i, end);
            if (i == end) { break; }
            if (',' == list.charAt(i)) {
            	++i;
            	continue;	// null elements permitted
            } 

            // parse the token
            final int beginToken = i;
            i = skip(token, nothing, list, i, end);
            final int endToken = i;
            if (beginToken == endToken) { throw new RuntimeException(); }
            r.append(list.substring(beginToken, endToken));

            // discard the parameters
            final ArrayBuilder<Header> ignored = PowerlessArray.builder();
            i = parseParameters(list, i, end, ignored);
        }
        return r.snapshot();
    }
    
    /**
     * Decodes a <code>parameter</code> list.
     * @param list	<code>parameter</code> list
     * @param i     initial search position in <code>text</code>
     * @param end   maximum search position in <code>text</code>
     * @param out	<code>parameter</code> output stream
     * @return end of the parsed text
     */
    static protected int
    parseParameters(final String list, int i, final int end,
    				final ArrayBuilder<Header> out) {
        while (true) {
        	i = skip(whitespace, nothing, list, i, end);

            // check for token delimiter
            if (i == end || ',' == list.charAt(i)) { break; }

            // start parameter
            if (';' != list.charAt(i++)) { throw new RuntimeException(); }
        	i = skip(whitespace, nothing, list, i, end);

            // parse the name
            final int beginName = i;
            i = skip(token, nothing, list, i, end);
            final int endName = i;
            final String name = list.substring(beginName, endName);

            // start the value
        	i = skip(whitespace, nothing, list, i, end);
            if ('=' != list.charAt(i++)) { throw new RuntimeException(); }
        	i = skip(whitespace, nothing, list, i, end);

            // parse the value
            final int beginValue;
            final int endValue;
            if ('\"' == list.charAt(i)) {	// quoted-string value
            	beginValue = ++i;
                while (true) {
                    final char c = list.charAt(i);
                    if ('\\' == c) {
                    	final char escaped = list.charAt(++i);
                    	if (escaped > 127) { throw new RuntimeException(); }
                    } else if (!qdtext(c)) {
                    	break;
                    }
                    ++i;
                }
                endValue = i;
                if ('\"' != list.charAt(i++)) { throw new RuntimeException(); }
            } else {						// token value
            	beginValue = i;
            	i = skip(token, nothing, list, i, end);
                endValue = i;
            }
            final String value = list.substring(beginValue, endValue);
            out.append(new Header(name, value));
        }
    	return i;
    }
    
    static public    final String nothing = "";
    static public    final String whitespace = " \t\r\n";
    static public    final String digit = "1234567890";
    static private   final String separator = "()<>@,;:\\\"/[]?={} \t";
    static public    final String token;
    static {
    	final StringBuilder buffer = new StringBuilder(127);
    	for (char c = 32; c != 127; ++c) {
    		if (separator.indexOf(c) == -1) {
    			buffer.append(c);
    		}
    	}
    	token = buffer.toString();
    }
    static public    final String text;
    static {
        final StringBuilder buffer = new StringBuilder(127);
        for (char c = 33; c != 127; ++c) { buffer.append(c); }
        buffer.append(whitespace);
        text = buffer.toString();
    }
    
    static private boolean
    ctl(final char c) { return c <= 31 || c >= 127; }
    
    static private boolean
    qdtext(final char c) { return !ctl(c) && c != '\"' && c != '\\'; }

    /**
     * Finds the first non-matching character.
     * @param allowed       allowed character set
     * @param disallowed    disallowed character set
     * @param text          text string to search
     * @param i             initial search position in <code>text</code>
     * @param end           maximum search position in <code>text</code>
     * @return index of the first non-matching character, or <code>end</code>
     */
    static public int
    skip(final String allowed, final String disallowed,
         final String text, int i, final int end) {
        while (i != end &&
               allowed.indexOf(text.charAt(i)) != -1 &&
               disallowed.indexOf(text.charAt(i)) == -1) { ++i; }
        return i;
    }
    
    /**
     * Vets a text string.
     * @param allowed       allowed character set
     * @param disallowed    disallowed character set
     * @param text          text string to search
     * @throws Exception    <code>text</code> is not allowed
     */
    static public void
    vet(final String allowed, final String disallowed,
                              final String text) throws Exception {
    	final int end = text.length();
    	if (end != skip(allowed,disallowed,text,0,end)) {throw new Exception();}
    }

    /**
     * Finds the first instance of a named header.
     * @param otherwise default value
     * @param name      searched for header name
     * @param headers   each header
     * @return found header value, or the <code>otherwise</code> if not found
     */
    static public String
    find(final String otherwise, final String name,
                                 final Iterable<Header> headers) {
        for (final Header header : headers) {
            if (equivalent(name, header.name)) { return header.value; }
        }
        return otherwise;
    }
    
    /**
     * Lists all values of a named header.
     * @param name      searched for header name
     * @param headers   each header
     * @return comma separated list of corresponding values
     */
    static public String
    list(final String name, final Iterable<Header> headers) {
        final StringBuilder buffer = new StringBuilder();
        for (final Header header : headers) {
            if (equivalent(name, header.name)) {
                if (buffer.length() != 0) { buffer.append(","); }
                buffer.append(header.value);
            }
        }
        return buffer.toString();
    }
    
    /**
     * Compares two tokens.
     * @param a	first token
     * @param b	second token
     * @return <code>true</code> if equivalent, else <code>false</code>
     */
    static public boolean
    equivalent(final String a, final String b) {
    	boolean r = a.length() == b.length();
    	for (int i = a.length(); r && 0 != i--;) {
    		r = toLower(a.charAt(i)) == toLower(b.charAt(i));
    	}
    	return r;
    }
    
    static private char
    toLower(final char c) {
    	return c >= 'A' && c <= 'Z' ? (char)('a' + (c - 'A')) : c;
    }
}
