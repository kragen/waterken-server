// Copyright 2010 Waterken Inc. under the terms of the MIT X license found at
// http://www.opensource.org/licenses/mit-license.html
package org.waterken.http;

import java.io.InputStream;

/**
 * Adds a header to all responses.
 */
public final class
WithHeader {
	private WithHeader() { /**/ }

	/**
	 * Constructs an instance.
	 * @param name		header name
	 * @param value		header value
	 * @param decorated	underlying client
	 */
	static public Client
	make(final String name, final String value, final Client decorated) {
		return new Client() {
		    public boolean
		    isStillWaiting() { return decorated.isStillWaiting(); }
		    
			public void
			receive(final Response head,
					final InputStream body) throws Exception {
				decorated.receive(head.with(name, value), body);
			}
		};
	}
}
