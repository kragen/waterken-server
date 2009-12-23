// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.markup;

import org.ref_send.scope.Layout;

/**
 * A reference with additional meta data.
 */
public final class
Link {
	private Link() {}

	/**
	 * A {@link Link} maker.
	 */
	static public final Layout<Link> Maker = Layout.define("href", "name");
}
