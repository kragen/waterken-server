// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.folder;

import org.joe_e.array.ByteArray;
import org.ref_send.data.Query;
import org.ref_send.data.Update;

/**
 * A [ name =&gt; byte array ] mapping.
 */
public interface
Folder extends Query<ByteArray>, Update<ByteArray,Void> {}
