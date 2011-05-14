// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import org.joe_e.array.PowerlessArray;
import org.waterken.http.Message;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.uri.Header;

/**
 * A task to be executed in message order, but with no corresponding message.
 */
/* package */ abstract class
Task extends Operation {
    static private final long serialVersionUID = 1L;

    protected
    Task(final boolean isQuery) {
        super(isQuery, false);
    }

    protected final @Override Message<Request>
    render(final String x, final long w, final int m) {
        return new Message<Request>(new Request("HTTP/1.1", "OPTIONS", "*",
            PowerlessArray.array(new Header[] {})), null);
    }

    protected final @Override void
    fulfill(final Pipeline.Position position, final Message<Response> ignored) {
        resolve(position);
    }

    protected final @Override void
    reject(final Pipeline.Position position, final Exception ignored) {
        resolve(position);
    }

    protected abstract void
    resolve(final Pipeline.Position position);
}
