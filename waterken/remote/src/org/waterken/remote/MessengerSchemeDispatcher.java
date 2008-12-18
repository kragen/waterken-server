// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.joe_e.Struct;
import org.ref_send.promise.eventual.Do;
import org.waterken.uri.URI;
import org.waterken.vat.Root;

/**
 * A URI scheme based {@link Messenger} dispatcher.
 */
public final class
MessengerSchemeDispatcher extends Struct implements Messenger, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * local address space
     */
    private final Root local;
    
    /**
     * Constructs an instance.
     * @param local local address space
     */
    public
    MessengerSchemeDispatcher(final Root local) {
        this.local = local;
    }
    
    // org.waterken.remote.Messenger interface
    
    public Object
    invoke(final String href, final Object proxy,
           final Method method, final Object... arg) {
        return dispatch(href).invoke(href, proxy, method, arg);
    }

    public <R> R
    when(final String href, final Class<?> R, final Do<Object,R> observer) {
        return dispatch(href).when(href, R, observer);
    }
    
    /**
     * root name prefix for scheme specific messengers
     */
    static public final String prefix = ".scheme-";

    private Messenger
    dispatch(final String href) {
        return local.fetch(MessageSink.sink, prefix + URI.scheme(href));
    }
}
