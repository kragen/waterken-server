// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.waterken.dns.Domain;
import org.waterken.dns.Resource;
import org.waterken.menu.Menu;

/**
 * {@link Domain} administrator permissions.
 */
public class
DomainMaster extends Struct implements Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * corresponding destructor
     */
    public final Runnable destruct;
    
    /**
     * {@linkplain Domain#getAnswers() answers} editor
     */
    public final Menu<Resource> answers;
    
    /**
     * additional domain management features
     */
    public final Extension extension;
    
    /**
     * Constructs an instance.
     * @param destruct  {@link #destruct}
     * @param answers   {@link #answers}
     * @param extension {@link #extension}
     */
    public @deserializer
    DomainMaster(@name("destruct") final Runnable destruct,
                 @name("answers") final Menu<Resource> answers,
                 @name("extension") final Extension extension) {
        this.destruct = destruct;
        this.answers = answers;
        this.extension = extension;
    }
}
