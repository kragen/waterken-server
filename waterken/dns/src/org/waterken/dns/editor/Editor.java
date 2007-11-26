// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.array.PowerlessArray;
import org.ref_send.Slot;
import org.waterken.dns.Domain;
import org.waterken.dns.Resource;
import org.web_send.graph.Framework;

/**
 * An {@link DomainMaster} implementation.
 */
public final class
Editor {
    
    private
    Editor() {}
    
    /**
     * Constructs an instance.
     * @param framework model framework
     */
    static public DomainMaster
    build(final Framework framework) {
        final SectionX answers = new SectionX();
        class DomainX extends Struct implements Domain, Serializable {
            static private final long serialVersionUID = 1L;

            public PowerlessArray<Resource>
            getAnswers() {
                final Resource[] r= new Resource[answers.getEntries().length()];
                int i = 0;
                for (final Slot<Resource> x : answers.getEntries()) {
                    r[i++] = x.get();
                }
                return PowerlessArray.array(r);
            }
        }
        final Domain published = new DomainX();
        framework.publisher.bind(Domain.name, published);
        return new DomainMaster(published, framework.destruct, answers);
    }
}
