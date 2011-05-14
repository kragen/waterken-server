// Copyright 2010 Waterken Inc. under the terms of the MIT X license found at
// http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.promise.Promise;

/**
 * Invokes {@link Task#resolve}.
 */
/* package */ final class
ResolveTask extends Struct implements Promise<Void>, Serializable {
    static private final long serialVersionUID = 1L;

    private final Task task;
    private final Pipeline.Position position;

    protected
    ResolveTask(final Task task, final Pipeline.Position position) {
        this.task = task;
        this.position = position;
    }

    public Void call() { task.resolve(position); return null; }
}
