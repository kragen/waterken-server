// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Selfless;
import org.joe_e.Struct;
import org.joe_e.array.PowerlessArray;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.waterken.uri.Header;

/**
 * A MIME Media-Type.
 */
public class
MediaType extends Struct implements Powerless, Selfless, Record, Serializable {
    static private final long serialVersionUID = 1L;

    public final String type;
    public final String subtype;
    public final PowerlessArray<Header> parameters;
    
    public @deserializer
    MediaType(@name("type") final String type,
    		  @name("subtype") final String subtype,
    		  @name("parameters") final PowerlessArray<Header> parameters) {
    	this.type = type;
    	this.subtype = subtype;
    	this.parameters = parameters;
    }
    
    public
    MediaType(final String type, final String subtype,
    		  final Header... parameter) {
    	this(type, subtype, PowerlessArray.array(parameter));
    }
    
    static public MediaType
    decode(final String text) {
    	final int end = text.length();
    	int i = 0;
    	
    	final int beginType = i;
    	i = TokenList.skip(TokenList.token, text, i, end);
    	final int endType = i;
    	final String type = text.substring(beginType, endType);

    	if ('/' != text.charAt(i++)) { throw new RuntimeException(); }
    	
    	final int beginSubtype = i;
    	i = TokenList.skip(TokenList.token, text, i, end);
    	final int endSubtype = i;
    	final String subtype = text.substring(beginSubtype, endSubtype);
    	
        final PowerlessArray.Builder<Header> params = PowerlessArray.builder();
        i = TokenList.parseParameters(text, i, end, params);
        if (i != end) { throw new RuntimeException(); }
    	
    	return new MediaType(type, subtype, params.snapshot());
    }
    
    // java.lang.Object interface
    
    public String
    toString() { return type + '/' + subtype + TokenList.encode(parameters); }
    
    // org.waterken.http.MediaType interface
    
    /**
     * Gets the value of a {@link #parameters parameter}.
     * @param name		parameter name
     * @param otherwise	default parameter value
     * @return corresponding value, or <code>otherwise</code> if unspecified
     */
    public String
    get(final String name, final String otherwise) {
    	for (final Header h : parameters) {
    		if (TokenList.equivalent(h.name, name)) { return h.value; }
    	}
    	return otherwise;
    }
    
    public boolean
    contains(final MediaType x) {
    	return TokenList.equivalent(type, x.type) &&
    		   TokenList.equivalent(subtype, x.subtype);
    }
}
