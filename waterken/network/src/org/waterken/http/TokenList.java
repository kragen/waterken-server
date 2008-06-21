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
    
    static protected String
    encode(final Iterable<Header> parameters) {
        final StringBuilder r = new StringBuilder();
        for (final Header h : parameters) {
        	r.append("; ");
        	r.append(h.name);
        	r.append('=');
        	r.append('\"');
        	for (int i = 0; i != h.value.length(); ++i) {
        		final char c = h.value.charAt(i);
        		if (!qdtext(c)) { r.append('\\'); }
        		r.append(c);
        	}
        	r.append('\"');
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
        	i = skip(whitespace, list, i, end);
            if (i == end) { break; }
            if (',' == list.charAt(i)) {
            	++i;
            	continue;	// null elements permitted
            } 

            // parse the token
            final int beginToken = i;
            i = skip(token, list, i, end);
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
        	i = skip(whitespace, list, i, end);

            // check for token delimiter
            if (i == end || ',' == list.charAt(i)) { break; }

            // start parameter
            if (';' != list.charAt(i++)) { throw new RuntimeException(); }
        	i = skip(whitespace, list, i, end);

            // parse the name
            final int beginName = i;
            i = skip(token, list, i, end);
            final int endName = i;
            final String name = list.substring(beginName, endName);

            // start the value
        	i = skip(whitespace, list, i, end);
            if ('=' != list.charAt(i++)) { throw new RuntimeException(); }
        	i = skip(whitespace, list, i, end);

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
            	i = skip(token, list, i, end);
                endValue = i;
            }
            final String value = list.substring(beginValue, endValue);
            out.append(new Header(name, value));
        }
    	return i;
    }
    
    static private   final String whitespace = " \t\r\n";
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
    
    static private boolean
    ctl(final char c) { return c <= 31 || c >= 127; }
    
    static private boolean
    qdtext(final char c) { return !ctl(c) && c != '\"' && c != '\\'; }

    /**
     * Finds the first non-matching character.
     * @param match	matching character set
     * @param text  text string to search
     * @param i     initial search position in <code>text</code>
     * @param end   maximum search position in <code>text</code>
     * @return index of the first non-matching character, or <code>end</code>
     */
    static public int
    skip(final String match, final String text, int i, final int end) {
        while (i != end && match.indexOf(text.charAt(i)) != -1) { ++i; }
        return i;
    }
    
    static public void
    vet(final String match, final String value) throws Exception {
    	final int end = value.length();
    	if (end != skip(match, value, 0, end)) { throw new Exception(); }
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
