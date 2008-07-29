// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.vat;

import java.io.Serializable;

import org.joe_e.Equatable;
import org.joe_e.Struct;
import org.ref_send.log.Comment;
import org.ref_send.log.Event;
import org.ref_send.log.Got;
import org.ref_send.log.Resolved;
import org.ref_send.log.SentIf;
import org.ref_send.promise.eventual.Log;
import org.ref_send.promise.eventual.Loop;
import org.ref_send.promise.eventual.Receiver;
import org.ref_send.var.Factory;

/**
 * A log {@link Event} generator.
 */
public final class
EventGenerator {
    
    private
    EventGenerator() {}
    
    /**
     * Constructs an instance.
     * @param local local address space
     */
    static public Log
    make(final Root local) {
        final Factory<Receiver<Event>> erf = local.fetch(null, Root.events);
        final Tracer tracer = local.fetch(null, Root.tracer);
        final Loop<Effect> effect = local.fetch(null, Root.effect); 
        class LogX extends Struct implements Log, Serializable {
            static private final long serialVersionUID = 1L;
            
            public boolean
            isOn() { return null != erf.run(); }

            public void
            comment(final String text) {
                final Receiver<Event> er = erf.run();
                if (null == er) { return; }
                
                log(er, new Comment(local.anchor(), tracer.get(), text));
            }

            public void
            resolved(final Equatable condition) {
                final Receiver<Event> er = erf.run();
                if (null == er) { return; }
                
                log(er,new Resolved(local.anchor(),tracer.get(),id(condition)));
            }

            public void
            got(final Equatable message) {
                final Receiver<Event> er = erf.run();
                if (null == er) { return; }
                
                log(er, new Got(local.anchor(), tracer.get(), id(message)));
            }

            public void
            sentIf(final Equatable message, final Equatable condition) {
                final Receiver<Event> er = erf.run();
                if (null == er) { return; }
                
                log(er, new SentIf(local.anchor(), tracer.get(),
                                   id(message), id(condition)));
            }
            
            private String
            id(final Equatable object) {
                final int mlen = 128 / 5 + 1;
                final StringBuilder m = new StringBuilder(mlen);
                m.append(local.export(object));
                for (int i = mlen - m.length(); 0 != i--;) { m.append('a'); }
                return local.pipeline(m.toString());
            }
            
            private void
            log(final Receiver<Event> er, final Event e) {
                effect.run(new Effect() { public void run() { er.run(e); } });
            }
        }
        return new LogX();
    }
}
