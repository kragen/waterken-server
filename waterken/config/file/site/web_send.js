/*
 * Copyright 2007-2009 Tyler Close under the terms of the MIT X license found
 * at http://www.opensource.org/licenses/mit-license.html
 *
 * web_send.js version: 2009-04-30
 *
 * This library doesn't actually pass the ADsafe verifier, but rather is
 * designed to provide a controlled interface to the network, that can be
 * loaded as an ADsafe library. Clients of this library have permission to send
 * requests to the window target, and any target returned in a request
 * response.  Clients *cannot* construct a remote promise from whole cloth by
 * providing a URL. In this way, a server can control the client's network
 * access by controlling what remote targets are made available to the client.
 *
 * In addition to messaging, the client is also permitted to navigate the
 * window and read/write the window title.
 */
"use strict";
ADSAFE.lib('web', function (lib) {

    /**
     * secret slot to extract the URL from a promise
     * <p>
     * Invoking a promise puts the URL in the slot.
     * </p>
     */
    var unsealedURLref = null;

    function proxy(target) {
        var self = function (op, arg1, arg2, arg3) {
            if (undefined === op) {
                unsealedURLref = target;
                return self;
            }
            if (/#o=/.test(target)) {
                send(target, 'GET', function (x) {
                    ('function'===typeof x?x:lib.Q.ref(x))(op,arg1,arg2,arg3);
                });
            } else {
                if ('WHEN' === op) {
                    arg1(self);
                } else {
                    send(target, op, arg1, arg2, arg3);
                }
            }
        };
        return self;
    }

    /**
     * Produces a relative URL reference.
     * @param base  absolute base URLref
     * @param href  absolute target URLref
     */
    function relateURI(base, href) {
        var baseOP  = /^([a-zA-Z][\w\-\.\+]*:\/\/[^\/]*\/)([^\?#]*)/.exec(base);
        var hrefOPR = /^([a-zA-Z][\w\-\.\+]*:\/\/[^\/]*\/)([^\?#]*)(.*)$/.
                                                                     exec(href);
        if (!baseOP || !hrefOPR || baseOP[1] !== hrefOPR[1]) { return href; }

        // determine the common parent folder
        var basePath = baseOP[2].split('/');
        var hrefPath = hrefOPR[2].split('/');
        var maxMatch = Math.min(basePath.length, hrefPath.length) - 1;
        var i = 0;
        while (i !== maxMatch && basePath[i] === hrefPath[i]) { ++i; }

        // wind up to the common parent folder
        var cd = '';
        for (var n = basePath.length - i - 1; 0 !== n--;) { cd += '../'; }
        if ('' === cd) {
            cd = './';
        }
        return cd + hrefPath.slice(i).join('/') + hrefOPR[3];
    }

    /**
     * Produce the JSON text for a JSON value.
     * @param base  absolute base URLref
     * @param arg   JSON value to serialize
     */
    function serialize(base, arg) {
        if (null === arg || 'object' !== typeof arg) {
            arg = { '=' : arg };
        }
        return JSON.stringify(arg, function (key, value) {
            if (undefined === value || null === value) { return value; }
            switch (typeof value) {
            case 'function':
                unsealedURLref = null;
                value = value();
                if (null !== unsealedURLref) {
                    value = { '@' : relateURI(base, unsealedURLref) };
                }
                unsealedURLref = null;
                break;
            case 'number':
                if (!isFinite(value)) {
                    value = { '!' : { $: [ 'NaN' ] } };
                }
                break;
            }
            return value;
        }, ' ');
    }

    /**
     * Resolves a relative URL reference.
     * @param base  absolute base URL
     * @param href  relative URL to resolve
     */
    function resolveURI(base, href) {
        if (/^[a-zA-Z][\w\-\.\+]*:/.test(href)) { return href; }

        base = /^[^#]*/.exec(base)[0];  // never include base fragment
        if ('' === href) { return base; }
        if (/^#/.test(href)) { return base + href; }
        if (/^\/\//.test(href)) {
            return /^[a-zA-Z][\w\-\.\+]*:/.exec(base)[0] + href;
        }
        if (/^\//.test(href)) {
            return /^[a-zA-Z][\w\-\.\+]*:\/\/[^\/]*/.exec(base)[0] + href;
        }

        base = /^[^\?]*/.exec(base)[0]; // drop base query
        if (/^\?/.test(href)) { return base + href; }

        // unwind relative path operators
        base = base.substring(0, base.lastIndexOf('/') + 1);
        var baseOR = /^([a-zA-Z][\w\-\.\+]*:\/\/[^\/]*\/)(.*)$/.exec(base);
        var host = baseOR[1];
        var path = baseOR[2];
        while (true) {
            if (/^\.\.\//.test(href)) {
                path = path.substring(0, path.lastIndexOf('/',path.length-2)+1);
                href = href.substring('../'.length);
            } else if (/^\.\//.test(href)) {
                href = href.substring('./'.length);
            } else {
                break;
            }
        }
        if (/^\.\.(#|\?|$)/.test(href)) {
            path = path.substring(0, path.lastIndexOf('/', path.length-2) + 1);
            href = href.substring('..'.length);
        }
        if (/^\.(#|\?|$)/.test(href)) {
            href = href.substring('.'.length);
        }
        return host + path + href;
    }

    /**
     * Deserializes the return value from an HTTP response.
     * @param base  base URL for request
     * @param http  HTTP response
     */
    function deserialize(base, http) {
        switch (http.status) {
        case 200:
        case 201:
        case 202:
        case 203:
            var contentType = http.getResponseHeader('Content-Type');
            if (/^application\/do-not-execute$/i.test(contentType)) {
                return http.responseText;
            }
            var r = JSON.parse(http.responseText, function (key, value) {
                if (undefined === value || null === value) { return value; }
                if ('object' !== typeof value) { return value; }
                if (value.hasOwnProperty('=')) { return value['=']; }
                if (value.hasOwnProperty('@')) {
                    return proxy(resolveURI(base, value['@']));
                }
                if (value.hasOwnProperty('!')) {
                    return lib.Q.reject(value['!']);
                }
                return value;
            });
            if (undefined !== r && null !== r &&
                'object' === typeof r && r.hasOwnProperty('=')) {
                r = r['='];
            }
            return r;
        case 204:
        case 205:
            return null;
        case 303:
            var see = http.getResponseHeader('Location');
            return see ? proxy(resolveURI(base, see)) : null;
        default:
            return lib.Q.reject({
                $: [ 'org.ref_send.promise.Failure', 'NaO' ],
                status: http.status,
                phrase: http.statusText
            });
        }
    }

    /**
     * Constructs a Request-URI for a web-key with options.
     * @param target    target URLref
     * @param q         optional client-specified query
     * @param session   optional session arguments.
     */
    function makeRequestURI(target, q, session) {
        var requestQuery = '';
        if (undefined !== q) {
            requestQuery = '?q=' + encodeURIComponent(q);
        }
        if (session && undefined !== session.x) {
            requestQuery += '' === requestQuery ? '?' : '&';
            requestQuery += 'x=' + encodeURIComponent(session.x);
            requestQuery += '&w=' + session.w;
        }
        var upqf = /([^\?#]*)([^#]*)(.*)/.exec(target);
        if (upqf[2]) {
            requestQuery += '' === requestQuery ? '?' : '&';
            requestQuery += upqf[2].substring(1);
        }
        if (upqf[3]) {
            requestQuery += '' === requestQuery ? '?' : '&';
            requestQuery += upqf[3].substring(1);
        }
        return upqf[1] + requestQuery;
    }

    /**
     * Enqueues an HTTP request.
     * @param target    target URLref
     * @param op        HTTP verb
     * @param resolve   response resolver
     * @param q         query parameter value
     * @param argv      JSON value for request body
     */
    var send = (function () {
        var active = false;
        var pending = [];
        var http;
        if (window.XMLHttpRequest) {
            http = new XMLHttpRequest();
        } else {
            http = new ActiveXObject('MSXML2.XMLHTTP.3.0');
        }
        var sessions = { /* origin => session */ };

        var output = function () {
            var m = pending[0];
            var requestURI = makeRequestURI(m.target, m.q, m.session);
            http.open(m.op, requestURI, true);
            http.onreadystatechange = function () {
                if (4 !== http.readyState) { return; }
                if (m !== pending.shift()) { throw new Error(); }
                if (0 === pending.length) {
                    active = false;
                } else {
                    ADSAFE.later(output);
                }
                if (m.session) {
                    m.session.w += 1;
                }

                m.resolve(deserialize(requestURI, http));
            };
            if (undefined === m.argv) {
                http.send(null);
            } else {
                http.setRequestHeader('Content-Type', 'text/plain');
                http.send(serialize(requestURI, m.argv));
            }

            // TODO: monitor the request with a local timeout
        };
        return function (target, op, resolve, q, argv) {
            var session = null;
            if ('POST' === op) {
                var origin = resolveURI(target, '/');
                session = ADSAFE.get(sessions, origin);
                if (!session) {
                    session = {
                        w: 1
                    };
                    ADSAFE.set(sessions, origin, session);
                    pending.push({
                        target: resolveURI(target, '?q=create&s=sessions'),
                        op: 'POST',
                        argv: [],
                        resolve: function (value) {
                            session.x = value.key;
                        }
                    });
                }
            }
            pending.push({
                session: session,
                target: target,
                op: op,
                resolve: resolve,
                q: q,
                argv: argv
            });
            if (!active) {
                ADSAFE.later(output);
                active = true;
            }
        };
    }) ();

    function crack(p) {
        unsealedURLref = null;
        if ('function' === typeof p) { p(); }
        var r = unsealedURLref;
        unsealedURLref = null;
        return r;
    }

    return {

        /**
         * Gets a promise for the window's current location.
         */
        getLocation: function () { return proxy(window.location.href); },

        /**
         * Navigate the window.
         * @param target    remote promise for new location
         * @return <code>true</code> if navigation successful,
         *         else <code>false</code>
         */
        navigate: function (target) {
            var href = crack(target);
            if (null === href) { return false; }
            window.location.assign(href);
            return true;
        },

        /**
         * Sets the 'href' attribute.
         * @param elements  bunch of elements to modify
         * @param target    remote promise
         * @return number of elements modified
         */
        href: function (elements, target) {
            var n = 0;
            if (null === target) {
                elements.___nodes___.filter(function (node) {
                    node.removeAttribute('href');
                    node.onclick = undefined;
                    n += 1;
                });
            } else {
                var href = crack(target);
                if (null !== href) {
                    elements.___nodes___.filter(function (node) {
                        switch (node.tagName.toUpperCase()) {
                        case 'A':
                            node.setAttribute('href', href);

                            // navigate even if fragment is only difference
                            node.onclick = function () {
                                // TODO: do original fragment navigation
                                window.location.assign(href);
                            };

                            n += 1;
                            break;
                        }
                    });
                }
            }
            return n;
        },

        /**
         * Sets the 'src' attribute.
         * @param elements  bunch of elements to modify
         * @param target    remote promise
         * @return number of elements modified
         */
        src: function (img, target) {
            var n = 0;
            if (null === target) {
                elements.___nodes___.filter(function (node) {
                    node.removeAttribute('src');
                    n += 1;
                });
            } else {
                var src = crack(target);
                if (null !== src) {
                    elements.___nodes___.filter(function (node) {
                        switch (node.tagName.toUpperCase()) {
                        case 'IMG':
                        case 'INPUT':
                            node.setAttribute('src', makeRequestURI(src));
                            n += 1;
                            break;
                        }
                    });
                }
            }
            return n;
        },

        /**
         * Gets the document title.
         */
        getTitle: function () { return window.document.title; },

        /**
         * Sets the document title.
         * @param text  new title text
         */
        setTitle: function (text) { window.document.title = text; },

        // Non-ADsafe API

        /**
         * Constructs a remote promise.
         * @param base  optional remote promise for base URLref
         * @param href  URLref to wrap
         * @param args  optional query argument map
         */
        _proxy: function (base, href, args) {
            var url = resolveURI(crack(base), href);
            if (args) {
                if ("object" !== typeof args) { throw new TypeError(); }
                var query = '?';
                for (k in args) { if (Object.hasOwnProperty.call(args, k)) {
                    if ('?' !== query) {
                        query += '&';
                    }
                    query += encodeURIComponent(String(k)) + '=' +
                             encodeURIComponent(String(ADSAFE.get(args, k)));
                } }
                url = resolveURI(url, query);
            }
            return proxy(url);
        },

        /**
         * Extracts the URLref contained within a remote promise.
         * @param promise remote promise to crack
         * @param target  optional remote promise for base URL
         * @return the URLref, or <code>null</code> if not a remote promise
         */
        _crack: function (promise, target) {
            var href = crack(promise);
            if (null === href || !target) { return href; }
            var base = crack(target);
            if (null === base) { return href; }
            return relateURI(base, href);
        }
    };
});
