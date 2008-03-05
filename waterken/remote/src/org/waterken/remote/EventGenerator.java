// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote;

import java.io.Serializable;

import org.joe_e.Equatable;
import org.joe_e.Struct;
import org.ref_send.log.Comment;
import org.ref_send.log.Entry;
import org.ref_send.log.Anchor;
import org.ref_send.log.Got;
import org.ref_send.log.Resolved;
import org.ref_send.log.SentIf;
import org.ref_send.promise.eventual.Log;
import org.ref_send.promise.eventual.Loop;
import org.ref_send.var.Factory;
import org.ref_send.var.Receiver;
import org.waterken.vat.Effect;
import org.waterken.vat.Root;
import org.waterken.vat.Tracer;

/**
 * A log {@link Entry} generator.
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
        final Factory<Receiver<Entry>> erf =
            (Factory)local.fetch(null, Root.events);
        final Tracer tracer = (Tracer)local.fetch(null, Root.tracer);
        final Loop<Effect> effect = (Loop)local.fetch(null,Root.effect); 
        class LogX extends Struct implements Log, Serializable {
            static private final long serialVersionUID = 1L;
            
            public boolean
            isOn() { return null != erf.run(); }

            public void
            comment(final String text) {
                final Receiver<Entry> er = erf.run();
                if (null == er) { return; }
                
                log(er, new Comment(local.notice(), tracer.get(), text));
            }

            public void
            resolved(final Equatable condition) {
                final Receiver<Entry> er = erf.run();
                if (null == er) { return; }
                
                final Anchor event = local.notice();
                log(er, new Resolved(event, tracer.get(),
                    event.turn.loop + '#' + local.export(condition)));
            }

            public void
            got(final Equatable message) {
                final Receiver<Entry> er = erf.run();
                if (null == er) { return; }
                
                final Anchor event = local.notice();
                log(er, new Got(event, tracer.get(),
                    event.turn.loop + '#' + local.export(message)));
            }

            public void
            sentIf(final Equatable message, final Equatable condition) {
                final Receiver<Entry> er = erf.run();
                if (null == er) { return; }
                
                final Anchor event = local.notice();
                log(er, new SentIf(event, tracer.get(),
                    event.turn.loop + '#' + local.export(message),
                    event.turn.loop + '#' + local.export(condition)));
            }
            
            private void
            log(final Receiver<Entry> er, final Entry e) {
                effect.run(new Effect() { public void run() { er.run(e); } });
            }
        }
        return new LogX();
    }
}
