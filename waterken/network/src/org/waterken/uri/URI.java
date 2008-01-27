// Copyright 2004-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.uri;

/**
 * <i>U</i>niform <i>R</i>esource <i>I</i>dentifier manipulation.
 */
public final class
URI {

    private
    URI() {}

    /**
     * Extracts the <code>scheme</code> component.
     * @param otherwise default value
     * @param uri       absolute URI
     * @return <code>scheme</code>
     */
    static public String
    scheme(final String otherwise, final String uri) {
        final int last = schemeLast(uri);
        return -1 != last ? uri.substring(0, last).toLowerCase() : otherwise;
    }

    static private int
    schemeLast(final String uri) {
        final int len = uri.length();
        if (0 == len || !isStartSymbol(uri.charAt(0))) { return -1; }
        int last = 1;
        while (len != last && isComponentSymbol(uri.charAt(last))) { ++last; }
        return last == len || ':' != uri.charAt(last) ? -1 : last;
    }

    static private boolean
    isStartSymbol(final char c) {
        return ('a' <= c && 'z' >= c) || ('A' <= c && 'Z' >= c);
    }

    static private boolean
    isComponentSymbol(final char c) {
        return ('a' <= c && 'z' >= c) || ('A' <= c && 'Z' >= c) ||
               ('0' <= c && '9' >= c) || '+' == c || '.' == c || '-' == c;
    }

    /**
     * Extracts the <code>authority</code> component.
     * @param uri   absolute URI
     * @return <code>authority</code>
     */
    static public String
    authority(final String uri) {
        final int first = authorityFirst(uri, schemeLast(uri));
        final int last = authorityLast(uri, first);
        return uri.substring(first, last);
    }

    static private int
    authorityFirst(final String uri, int first) {
        ++first;    // Skip past the ':' separator.
        if (uri.startsWith("//", first)) {
            first += 2;
        }
        return first;
    }

    static private int
    authorityLast(final String uri, final int first) {
        final int last = uri.indexOf('/', first);
        return -1 != last ? last : hierarchyLast(uri, first);
    }

    static private int
    hierarchyLast(final String uri, final int first) {
        final int query = uri.indexOf('?', first);
        final int fragment = uri.indexOf('#', first);
        return -1 == query
            ? (-1 == fragment ? uri.length() : fragment)
        : (-1 == fragment
            ? query
        : (query < fragment ? query : fragment));
    }

    /**
     * Extracts the rootless <code>path</code> component.
     * @param uri   absolute URI
     * @return rootless <code>path</code>
     */
    static public String
    path(final String uri) {
        final int first = serviceLast(uri);
        final int last = hierarchyLast(uri, first);
        return last != first ? uri.substring(first + 1, last) : "";
    }

    static private int
    serviceLast(final String uri) {
        return authorityLast(uri, authorityFirst(uri, schemeLast(uri)));
    }

    /**
     * Extracts the <code>query</code> component.
     * @param otherwise default value
     * @param uri       absolute URI
     * @return <code>query</code>
     */
    static public String
    query(final String otherwise, final String uri) {
        final int start = uri.indexOf('?');
        if (-1 == start) { return otherwise; }
        final int end = uri.indexOf('#');
        return -1 == end
            ? uri.substring(start + 1)
        : (start < end ? uri.substring(start + 1, end) : otherwise);
    }

    /**
     * Extracts the <code>fragment</code> component.
     * @param otherwise default value
     * @param uri       absolute URI
     * @return <code>fragment</code>
     */
    static public String
    fragment(final String otherwise, final String uri) {
        final int start = uri.indexOf('#');
        return -1 != start ? uri.substring(start + 1) : otherwise;
    }

    /**
     * Extracts the proxy request URI.
     * @param uri   absolute URI
     * @return <code>uri</code>, stripped of any <code>fragment</code>
     */
    static public String
    proxy(final String uri) {
        final int fragmentStart = uri.indexOf('#');
        return -1 == fragmentStart ? uri : uri.substring(0, fragmentStart);
    }

    /**
     * Extracts the remote service identifier.
     * @param uri   absolute URI
     * @return <code>scheme</code> and <code>authority</code>
     */
    static public String
    service(final String uri) { return uri.substring(0, serviceLast(uri)); }

    /**
     * Extracts the request URI.
     * @param uri   absolute URI
     * @return <code>path</code> and <code>query</code>
     */
    static public String
    request(final String uri) {
        final int first = serviceLast(uri);
        final int last = uri.indexOf('#', first);
        return -1 == last ? uri.substring(first) : uri.substring(first, last);
    }

    /**
     * Resolves a relative URI string.
     * @param base      trusted absolute URI
     * @param relative  untrusted relative URI string
     * @return resolved and trusted absolute URI
     * @throws InvalidURI   rejected <code>relative</code>
     */
    static public String
    resolve(final String base, final String relative) throws InvalidURI {
        if ("".equals(relative)) { return proxy(base); }

        final String hierarchy;     // trusted scheme : hier-part
        final String tail;          // untrusted ? query # fragment
        if (relative.startsWith("#")) {
            hierarchy = proxy(base);
            tail = relative;
        } else if (relative.startsWith("?")) {
            hierarchy = base.substring(0, hierarchyLast(base, 0));
            tail = relative;
        } else {
            final int relativePathFirst;
            final String root;      // trusted scheme : authority /
            final String folder;    // untrusted base path
            if (-1 != schemeLast(relative)) {
                final int authorityLast = serviceLast(relative);
                for (int i = 0; authorityLast != i; ++i) {
                    final char c = relative.charAt(i);
                    if (!(URI.unreserved(c) || URI.subdelim(c) ||
                          "@/:[]%".indexOf(c) != -1)) {throw new InvalidURI();}
                }
                relativePathFirst = relative.startsWith("/", authorityLast)
                    ? authorityLast + 1 : authorityLast;
                root = relative.substring(0, relativePathFirst);
                folder = "";
            } else if (relative.startsWith("//")) {
                final int authorityLast = authorityLast(relative,"//".length());
                for (int i = "//".length(); authorityLast != i; ++i) {
                    final char c = relative.charAt(i);
                    if (!(URI.unreserved(c) || URI.subdelim(c) ||
                          "@:[]%".indexOf(c) != -1)) { throw new InvalidURI(); }
                }
                relativePathFirst = relative.startsWith("/", authorityLast)
                    ? authorityLast + 1 : authorityLast;
                root = base.substring(0, base.indexOf(':') + 1) +
                       relative.substring(0, relativePathFirst);
                folder = "";
            } else if (relative.startsWith("/")) {
                relativePathFirst = 1;
                root = service(base) + "/";
                folder = "";
            } else {
                relativePathFirst = 0;
                final int authorityLast = serviceLast(base);
                root = base.startsWith("/", authorityLast)
                    ? base.substring(0, authorityLast + 1)
                    : base.substring(0, authorityLast);
                final int basePathFirst = root.length();
                final int basePathLast = hierarchyLast(base, basePathFirst);
                final int folderLast = base.lastIndexOf('/', basePathLast);
                folder = folderLast < basePathFirst
                    ? "" : base.substring(basePathFirst, folderLast + 1);
            }

            // Resolve the relative URI string against the context.
            final int relativePathLast =
                hierarchyLast(relative, relativePathFirst);
            final String relativePath =
                relative.substring(relativePathFirst, relativePathLast);
            hierarchy = root + Path.vet(folder + relativePath);
            tail = relative.substring(relativePathLast);
        }
        final int hash = tail.indexOf('#');
        if (tail.startsWith("?")) {
            final int queryLast = -1 == hash ? tail.length() : hash;
            for (int i = queryLast; 1 != i--;) {
                final char c = tail.charAt(i);
                if (!(URI.pchar(c) ||
                      "/?".indexOf(c) != -1)) { throw new InvalidURI(); }
            }
        }
        if (-1 != hash) {
            for (int i = tail.length(); hash != --i;) {
                final char c = tail.charAt(i);
                if (!(URI.pchar(c) ||
                      "/?".indexOf(c) != -1)) { throw new InvalidURI(); }
            }
        }
        return hierarchy + tail;
    }

    /**
     * Encodes an absolute URI relative to a base URI.
     * @param base      absolute base URI
     * @param target    absolute target URI
     * @return relative URI string from base to target
     */
    static public String
    relate(final String base, final String target) {
        final int first = serviceLast(base);
        if (!base.regionMatches(0, target, 0, first)) { return target; }
        
        // determine the common parent folder
        final int last = hierarchyLast(base, first);
        final String path = base.substring(first, last);
        int i = 0;
        int j = path.indexOf('/');
        while (-1 != j && path.regionMatches(i, target, first + i, j + 1 - i)) {
            j = path.indexOf('/', i = j + 1);
        }
        if (-1 != j) {
            // wind up to the common base
            final StringBuilder buffer = new StringBuilder();
            if (0 == j) {
                j = path.indexOf('/', 1);
            }
            while (j != -1) {
                buffer.append("../");
                j = path.indexOf('/', j + 1);
            }
            if (0 == buffer.length()) {
                buffer.append("./");
            }
            buffer.append(target.substring(first + i));
            return buffer.toString();
        }
        
        // compare the last segment
        j = last - first;
        if (!(path.regionMatches(i, target, first + i, j - i) &&
             (last == target.length() || '?' == target.charAt(last) ||
              '#' == target.charAt(last)))) {
            return "./" + target.substring(first + i);
        }

        // compare the query
        int f = base.indexOf('#', last);
        if (-1 == f) {
            f = base.length();
        }
        return base.regionMatches(last, target, last, f - last) &&
               (f == target.length() || '#' == target.charAt(f))
            ? target.substring(f) : target.substring(last);
    }
    
    static boolean
    pchar(char c) { return unreserved(c)||subdelim(c)||"%:@".indexOf(c) != -1; }
    
    static boolean
    unreserved(char c) {return alpha(c) || digit(c) || "-._~".indexOf(c) != -1;}
    
    static boolean
    alpha(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'); }
    
    static boolean
    digit(final char c) { return c >= '0' && c <= '9'; } 
    
    static boolean
    reserved(final char c) { return gendelim(c) || subdelim(c); }
    
    static boolean
    subdelim(final char c) { return "!$&'()*+,;=".indexOf(c) != -1; }
    
    static boolean
    gendelim(final char c) { return ":/?#[]@".indexOf(c) != -1; }
}
