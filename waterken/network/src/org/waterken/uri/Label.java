// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.uri;

/**
 * A hostname label.
 */
public final class
Label {

    private
    Label() {}

    /**
     * Checks a hostname label for invalid characters.
     * @param label label to vet
     */
    static public void
    vet(final String label) throws InvalidLabel { vet(label,0,label.length()); }
    
    static void
    vet(final String s, final int i, final int j) throws InvalidLabel {
        final int len = j - i;
        if (1 > len) { throw new InvalidLabel(); }
        if (63 < len) { throw new InvalidLabel(); }
        if (!letter(s.charAt(i))) { throw new InvalidLabel(); }
        if (1 == len) { return; }
        if (!letterDig(s.charAt(j - 1))) { throw new InvalidLabel(); }
        for (int k = j - 1; i != --k;) {
            if (!letterDigHyp(s.charAt(k))) { throw new InvalidLabel(); }
        }
    }
    
    static private boolean
    letter(final char c) { return ('a'<=c && 'z'>=c) || ('A'<=c && 'Z'>=c); }
    
    static private boolean
    letterDig(final char c) { return letter(c) || ('0' <= c && '9' >= c); }
    
    static private boolean
    letterDigHyp(final char c) { return letterDig(c) || '-' == c; }
}
