// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

/**
 * A set of SSL credentials.
 */
interface
Credentials {

    /**
     * Gets the SSL context.
     * @return The SLL context.
     */
    SSLContext
    getContext() throws IOException, GeneralSecurityException;
}
