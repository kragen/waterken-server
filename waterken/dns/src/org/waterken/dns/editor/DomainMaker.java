// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import static org.ref_send.promise.eventual.Eventual.near;
import static org.ref_send.var.Variable.var;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.joe_e.array.ConstArray;
import org.joe_e.array.PowerlessArray;
import org.ref_send.var.Factory;
import org.ref_send.var.Variable;
import org.waterken.dns.Domain;
import org.waterken.dns.Resource;
import org.waterken.menu.Menu;
import org.waterken.menu.MenuMaker;
import org.web_send.graph.Framework;

/**
 * A {@link Domain} implementation.
 */
public final class
DomainMaker {
    
    private
    DomainMaker() {}
    
    /**
     * Constructs an instance.
     * @param framework model framework
     */
    static public DomainMaster
    build(final Framework framework) {
        final Menu<Variable<Resource>> answers =
            MenuMaker.make(8, new ResourceVariableFactory());
        class DomainX extends Struct implements Domain, Serializable {
            static private final long serialVersionUID = 1L;

            public PowerlessArray<Resource>
            getAnswers() {
                final ConstArray<Variable<Resource>> entries =
                    near(answers.getEntries());
                final Resource[] r = new Resource[entries.length()];
                int i = 0;
                for (final Variable<Resource> x : entries) { r[i++] = x.get(); }
                return PowerlessArray.array(r);
            }
        }
        framework.publisher.bind(Domain.name, new DomainX());
        return new DomainMaster(framework.destruct, answers,
                                new ExtensionX(framework, answers));
    }
    
    static final class
    ResourceVariableFactory extends Factory<Variable<Resource>>
                            implements Powerless, Serializable {
        static private final long serialVersionUID = 1L;

        public Variable<Resource>
        run() {
            final Resource initial = new Resource(Resource.A, Resource.IN,
                ResourceGuard.minTTL, ByteArray.array(new byte[]{ 127,0,0,1 })); 
            return var(initial, new ResourceGuard());
        }
    }
}
