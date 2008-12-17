// Copyright 2004-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.uri;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Selfless;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * A named string parameter.
 */
public final class
Header implements Powerless, Selfless, Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * name
     */
    public final String name;

    /**
     * value
     */
    public final String value;

    /**
     * Constructs an instance.
     * @param name  {@link #name}
     * @param value {@link #value}
     */
    public @deserializer
    Header(@name("name") final String name,
           @name("value") final String value) {
        this.name = name;
        this.value = value;
    }

    // java.lang.Object interface

    /**
     * Is the given object the same?
     * @param o compared to object
     * @return true if the same, else false
     */
    public boolean
    equals(final Object o) {
        if (!(o instanceof Header)) { return false; }
        final Header x = (Header)o;
        return (null != name ? name.equals(x.name) : null == x.name) &&
               (null != value ? value.equals(x.value) : null == x.value);
    }

    /**
     * Calculates the hash code.
     */
    public int
    hashCode() {
        return (null != name ? name.hashCode() : 0) +
               (null != value ? value.hashCode() : 0);
    }
    
    // org.waterken.uri.Header interface

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
    
    /**
     * Converts ASCII characters to lower case.
     * @param name  header name
     * @return lower case header name
     */
    static public String
    toLowerCase(final String name) {
        final int len = name.length();
        for (int i = 0; true; ++i) {
            if (len == i) { return name; }
            final char c = name.charAt(i);
            if (c != toLower(c)) { break; }
        }
        final StringBuilder buffer = new StringBuilder(len);
        for (int i = 0; i != len; ++i) {
            buffer.append(toLower(name.charAt(i)));
        }
        return buffer.toString();
    }
}
