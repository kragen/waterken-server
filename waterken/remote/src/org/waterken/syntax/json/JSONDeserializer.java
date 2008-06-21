// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax.json;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Type;

import org.joe_e.Struct;
import org.joe_e.array.ConstArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.charset.UTF8;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.waterken.http.MediaType;
import org.waterken.http.TokenList;
import org.waterken.id.Importer;
import org.waterken.syntax.Deserializer;

/**
 * <a href="http://www.json.org/">JSON</a> deserialization.
 */
public final class
JSONDeserializer extends Struct implements Deserializer, Record, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * Constructs an instance.
     */
    public @deserializer
    JSONDeserializer() {}
    
    // org.waterken.syntax.Deserializer interface

    public ConstArray<?>
    run(final String base, final Importer connect, final ClassLoader code,
        final MediaType type, final InputStream content,
        final PowerlessArray<Type> parameters) throws Exception {
    	if (!TokenList.equivalent("UTF-8", type.get("charset", "UTF-8"))) {
    		throw new Exception("charset MUST be UTF-8");
    	}
        return new JSONParser(base, connect, code).
            parse(UTF8.input(content), parameters);
    }
}
