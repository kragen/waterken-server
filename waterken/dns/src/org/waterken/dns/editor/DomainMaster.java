// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.waterken.dns.Domain;

/**
 * A {@link Domain} administrator permissions.
 */
public class
DomainMaster extends Struct implements Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * public interface
     */
    public final Domain published;
    
    /**
     * corresponding destructor
     */
    public final Runnable destruct;
    
    /**
     * {@link Domain#getAnswers() answers} editor
     */
    public final Section answers;
    
    /**
     * Constructs an instance.
     * @param published {@link #published}
     * @param destruct  {@link #destruct}
     * @param answers   {@link #answers}
     */
    public @deserializer
    DomainMaster(@name("published") final Domain published,
                 @name("destruct") final Runnable destruct,
                 @name("answers") final Section answers) {
        this.published = published;
        this.destruct = destruct;
        this.answers = answers;
    }
}
