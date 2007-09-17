// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import static org.ref_send.promise.Fulfilled.ref;

import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.array.PowerlessArray;
import org.ref_send.Slot;
import org.ref_send.promise.Fulfilled;
import org.waterken.dns.Domain;
import org.waterken.dns.Resource;
import org.web_send.graph.Framework;

/**
 * An {@link Administrator} implementation.
 */
public final class
Editor {
    
    private
    Editor() {}
    
    /**
     * Constructs an instance.
     * @param framework model framework
     */
    static public Fulfilled<Administrator<Resource>>
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
        framework.exports.bind(Domain.name, published);
        return ref(new Administrator<Resource>(
                published, framework.destruct, answers));
    }
}
