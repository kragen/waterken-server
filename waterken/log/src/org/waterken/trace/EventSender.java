// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.trace;

import java.io.Serializable;
import java.lang.reflect.Member;

import org.joe_e.Equatable;
import org.ref_send.log.Comment;
import org.ref_send.log.Event;
import org.ref_send.log.Got;
import org.ref_send.log.Problem;
import org.ref_send.log.Resolved;
import org.ref_send.log.Returned;
import org.ref_send.log.Sent;
import org.ref_send.log.SentIf;
import org.ref_send.promise.Log;
import org.ref_send.promise.Receiver;

/**
 * Event logging infrastructure.
 */
public final class
EventSender {

    private
    EventSender() {}
    
    /**
     * Constructs a log event generator.
     * @param stderr    log event output factory
     * @param mark      event counter
     * @param tracer    stack tracer
     */
    static public Log
    makeLog(final Receiver<Event> stderr,
            final Marker mark, final Tracer tracer) {
        class LogX implements Log, Equatable, Serializable {
            static private final long serialVersionUID = 1L;

            public void
            comment(final String text) {
                stderr.run(new Comment(mark.run(), tracer.traceHere(), text));
            }
            
            public void
            problem(final Exception reason) {
                stderr.run(new Problem(mark.run(),tracer.traceException(reason),
                                       tracer.readException(reason), reason));
            }

            public void
            got(final String message, final Member member) {
                stderr.run(new Got(mark.run(),
                    null!=member ? tracer.traceMember(member) : null, message));
            }

            public void
            returned(final String message) {
                stderr.run(new Returned(mark.run(), null, message));
            }

            public void
            sent(final String message) {
                stderr.run(new Sent(mark.run(), tracer.traceHere(), message));
            }

            public void
            resolved(final String condition) {
                stderr.run(new Resolved(mark.run(), tracer.traceHere(),
                                        condition));
            }

            public void
            sentIf(final String message, final String condition) {
                stderr.run(new SentIf(mark.run(), tracer.traceHere(),
                                      message, condition));
            }
        }
        return new LogX();
    }
}
