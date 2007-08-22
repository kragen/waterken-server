// Copyright 2004-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.uri;

/**
 * A filename domain.
 */
public final class
Filename {

    private
    Filename() {}

    /**
     * Extracts the filename extension.
     * @param name  filename
     * @return extension, or <code>""</code> if none
     */
    static public String
    ext(final String name) {
        final int dot = name.indexOf('.');
        return -1 != dot ? name.substring(dot) : "";
    }

    /**
     * Extracts the filename key.
     * @param name  filename
     * @return filename, less the extension
     */
    static public String
    key(final String name) {
        final int dot = name.indexOf('.');
        return -1 != dot ? name.substring(0, dot) : name;
    }
}
