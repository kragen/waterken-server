// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import org.joe_e.Powerless;

/**
 * Cannot {@linkplain Dispatch dispatch} an overloaded method name.
 */
public class
OverloadedMethodName extends NullPointerException implements Powerless {
    static private final long serialVersionUID = 1L;
}
