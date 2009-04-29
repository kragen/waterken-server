// Copyright 2004-2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.uri;

/**
 * <i>U</i>niform <i>R</i>esource <i>I</i>dentifier manipulation.
 */
public final class
URI {
    private URI() {}

    /**
     * Extracts the <code>scheme</code> component.
     * @param href   URI
     * @return <code>scheme</code>, or <code>""</code> if unspecified
     */
    static public String
    scheme(final String href) {
        final int last = schemeLast(href);
        return -1 != last ? Header.toLowerCase(href.substring(0, last)) : "";
    }

    static private int
    schemeLast(final String href) {
        final int len = href.length();
        if (0 == len || !isSchemeStartSymbol(href.charAt(0))) { return -1; }
        int last = 1;
        while (len!=last && isSchemeComponentSymbol(href.charAt(last))){++last;}
        return last == len || ':' != href.charAt(last) ? -1 : last;
    }

    static private boolean
    isSchemeStartSymbol(final char c) {
        return ('a' <= c && 'z' >= c) || ('A' <= c && 'Z' >= c);
    }

    static private boolean
    isSchemeComponentSymbol(final char c) {
        return ('a' <= c && 'z' >= c) || ('A' <= c && 'Z' >= c) ||
               ('0' <= c && '9' >= c) || '+' == c || '.' == c || '-' == c;
    }

    /**
     * Extracts the <code>authority</code> component.
     * @param href   URI
     * @return <code>authority</code>, or <code>""</code> if unspecified
     */
    static public String
    authority(final String href) {
        final int first = schemeLast(href) + 1;
        final int last = authorityLast(href, first);
        return first != last ? href.substring(first + 2, last) : "";
    }

    static private int
    authorityLast(final String href, final int first) {
        if (!href.startsWith("//", first)) { return first; }
        final int slash = href.indexOf('/', first + 2);
        final int last = hierarchyLast(href, first + 2);
        return -1 != slash && slash < last ? slash : last;
    }

    static private int
    hierarchyLast(final String href, final int first) {
        final int question = href.indexOf('?', first);
        final int hash = href.indexOf('#', first);
        return -1 == question
            ? (-1 == hash ? href.length() : hash)
        : (-1 == hash ? question : (question < hash ? question : hash));
    }

    /**
     * Extracts the rootless <code>path</code> component.
     * @param href   URI
     * @return rootless <code>path</code>
     */
    static public String
    path(final String href) {
        final int slash = serviceLast(href);
        final int first = href.startsWith("/", slash) ? slash + 1 : slash;
        final int last = hierarchyLast(href, first);
        return href.substring(first, last);
    }

    static private int
    serviceLast(final String href) {
        return authorityLast(href, schemeLast(href) + 1);
    }

    /**
     * Extracts the <code>query</code> component.
     * @param otherwise default value
     * @param href       URI
     * @return <code>query</code>
     */
    static public String
    query(final String otherwise, final String href) {
        final int question = href.indexOf('?');
        if (-1 == question) { return otherwise; }
        final int hash = href.indexOf('#');
        return -1 == hash
            ? href.substring(question + 1)
        : (question < hash ? href.substring(question + 1, hash) : otherwise);
    }

    /**
     * Extracts the <code>fragment</code> component.
     * @param otherwise default value
     * @param href       URI
     * @return <code>fragment</code>
     */
    static public String
    fragment(final String otherwise, final String href) {
        final int hash = href.indexOf('#');
        return -1 != hash ? href.substring(hash + 1) : otherwise;
    }

    /**
     * Extracts the proxy request URI.
     * @param href   URI
     * @return <code>href</code>, stripped of any <code>fragment</code>
     */
    static public String
    proxy(final String href) {
        final int hash = href.indexOf('#');
        return -1 == hash ? href : href.substring(0, hash);
    }

    /**
     * Extracts the remote service identifier.
     * @param href   URI
     * @return <code>scheme</code> and <code>authority</code>
     */
    static public String
    service(final String href) { return href.substring(0, serviceLast(href)); }

    /**
     * Extracts the request URI.
     * @param href   URI
     * @return <code>path</code> and <code>query</code>
     */
    static public String
    request(final String href) {
        final int first = serviceLast(href);
        final int last = href.indexOf('#', first);
        return -1 == last ? href.substring(first) : href.substring(first, last);
    }

    /**
     * Resolves a relative URI string.
     * @param base  trusted URI
     * @param href  untrusted URI
     * @return resolved and trusted absolute URI
     * @throws InvalidURI   rejected <code>relative</code>
     */
    static public String
    resolve(final String base, final String href) throws InvalidURI {
        if ("".equals(href)) { return proxy(base); }

        final String hierarchy;     // trusted scheme : hier-part
        final String tail;          // untrusted ? query # fragment
        if (href.startsWith("#")) {
            hierarchy = proxy(base);
            tail = href;
        } else if (href.startsWith("?")) {
            hierarchy = base.substring(0, hierarchyLast(base, 0));
            tail = href;
        } else {
            final int relativePathFirst;
            final String root;      // trusted scheme : authority /
            final String folder;    // untrusted base path
            if (-1 != schemeLast(href)) {
                final int authorityLast = serviceLast(href);
                for (int i = 0; authorityLast != i; ++i) {
                    final char c = href.charAt(i);
                    if (!(URI.unreserved(c) || URI.subdelim(c) ||
                          "@/:[]%".indexOf(c) != -1)) {throw new InvalidURI();}
                }
                relativePathFirst = href.startsWith("/", authorityLast)
                    ? authorityLast + 1 : authorityLast;
                root = href.substring(0, relativePathFirst);
                folder = "";
            } else if (href.startsWith("//")) {
                final int authorityLast = authorityLast(href,"//".length());
                for (int i = "//".length(); authorityLast != i; ++i) {
                    final char c = href.charAt(i);
                    if (!(URI.unreserved(c) || URI.subdelim(c) ||
                          "@:[]%".indexOf(c) != -1)) { throw new InvalidURI(); }
                }
                relativePathFirst = href.startsWith("/", authorityLast)
                    ? authorityLast + 1 : authorityLast;
                root = base.substring(0, schemeLast(base) + 1) +
                       href.substring(0, relativePathFirst);
                folder = "";
            } else if (href.startsWith("/")) {
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
                hierarchyLast(href, relativePathFirst);
            final String relativePath =
                href.substring(relativePathFirst, relativePathLast);
            hierarchy = root + Path.vet(folder + relativePath);
            tail = href.substring(relativePathLast);
        }
        final int hash = tail.indexOf('#');
        if (tail.startsWith("?")) {
            final int queryLast = -1 == hash ? tail.length() : hash;
            for (int i = queryLast; 1 != i--;) {
                if (!qchar(tail.charAt(i))) { throw new InvalidURI(); }
            }
        }
        if (-1 != hash) {
            for (int i = tail.length(); hash != --i;) {
                if (!qchar(tail.charAt(i))) { throw new InvalidURI(); }
            }
        }
        return hierarchy + tail;
    }

    /**
     * Encodes an absolute URI relative to a base URI.
     * @param base  base URI
     * @param href  target URI
     * @return relative URI string from base to target
     */
    static public String
    relate(final String base, final String href) {
        final int first = serviceLast(base);
        if (!base.regionMatches(0, href, 0, first + 1)) { return href; }
        
        // determine the common parent folder
        final int last = hierarchyLast(base, first);
        final String path = base.substring(first, last);
        int i = 0;
        int j = path.indexOf('/');
        while (-1 != j && path.regionMatches(i, href, first + i, j + 1 - i)) {
            j = path.indexOf('/', i = j + 1);
        }
        
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
        buffer.append(href.substring(first + i));
        return buffer.toString();
    }
    
    static private boolean
    qchar(final char c) { return pchar(c) || '/' == c || '?' == c; }
    
    static protected boolean
    pchar(char c) { return unreserved(c)||subdelim(c)||"%:@".indexOf(c) != -1; }
    
    static private boolean
    unreserved(char c) {return alpha(c) || digit(c) || "-._~".indexOf(c) != -1;}
    
    static private boolean
    alpha(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'); }
    
    static private boolean
    digit(final char c) { return c >= '0' && c <= '9'; } 
    
    static private boolean
    subdelim(final char c) { return "!$&'()*+,;=".indexOf(c) != -1; }
}
