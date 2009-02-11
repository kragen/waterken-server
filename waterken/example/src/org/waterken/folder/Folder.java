// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.folder;

import org.ref_send.custom.Query;
import org.ref_send.custom.Update;

/**
 * A [ name =&gt; value ] mapping.
 */
public interface
Folder<T> extends Query<T>, Update<T,Void> {}
