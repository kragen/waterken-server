// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.eventual.Do;
import org.waterken.uri.URI;

/**
 * A messenger that fails all messages.
 */
/* package */ final class
MessageSink extends Struct implements Messenger, Powerless, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * an instance
     */
    static protected final MessageSink sink = new MessageSink();
    
    private
    MessageSink() {}

    public <R> R
    when(final String href, final Remote proxy,
         final Class<?> R, final Do<Object, R> observer) {
        return new Rejected<R>(new UnsupportedScheme(URI.scheme(href)))._(R);
    }

    public Object
    invoke(final String href, final Object proxy,
           final Method method, final Object... arg) {
        return new Rejected<Object>(new UnsupportedScheme(URI.scheme(href))).
            _(method.getReturnType());
    }
}
