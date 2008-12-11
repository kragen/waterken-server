// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.vat;

import org.joe_e.Immutable;
import org.ref_send.promise.eventual.Receiver;

/**
 * A side-effect of an {@linkplain Transaction#update update} transaction.
 */
public interface
Effect<S> extends Receiver<Vat<S>>, Immutable {}
