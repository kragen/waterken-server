// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.store.n2v;

import java.io.File;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

final class 
FileGC extends WeakReference<ByteBuffer> {

    final File file;
    
    FileGC(final ByteBuffer referent, final ReferenceQueue<ByteBuffer> q,
           final File file) {
        super(referent, q);
        this.file = file;
    }
}
