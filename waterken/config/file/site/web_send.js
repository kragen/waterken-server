/*
 * Copyright 2007-2009 Tyler Close under the terms of the MIT X license found
 * at http://www.opensource.org/licenses/mit-license.html
 *
 * web_send.js version: 2009-05-12
 *
 * This library doesn't actually pass the ADsafe verifier, but rather is
 * designed to provide a controlled interface to the network, that can be
 * loaded as an ADsafe library. Clients of this library have permission to send
 * requests to the window target, and any target returned in a request
 * response.  ADsafe verified clients *cannot* construct a remote reference
 * from whole cloth by providing a URL. In this way, a server can control the
 * client's network access by controlling what remote references are made
 * available to the client.
 *
 * In addition to messaging, the client is also permitted to read/write the
 * window title and navigate the window to any received remote target.
 */
"use strict";
ADSAFE.lib('web', function (lib) {

    /**
     * Does an object define a given key?
     */
    function includes(map, key) {
        return map && Object.hasOwnProperty.call(map, key);
    }

    /**
     * secret slot to extract the URL from a remote reference
     * <p>
     * Invoking a remote reference puts the URL in the slot.
     * </p>
     */
    var unsealedURLref = null;

    /**
     * value returned on a 404 server response
     */
    var notYetPumpkin = lib.Q.reject({
        $: [ 'org.ref_send.promise.Failure', 'NaO' ],
        status: 404,
        phrase: 'Not Found'
    });

    /**
     * Constructs a remote reference.
     * @param href  absolute URLref for target resource
     */
    function sealURLref(href) {
        var cache = null;
        var resolved = false;
        var self = function (op, arg1, arg2, arg3) {
            if (undefined === op) {
                unsealedURLref = href;
                return resolved ? cache() : self;
            }
            if (/#o=/.test(href)) {
                if (!cache) {
                    var pr = lib.Q.defer();
                    cache = pr.promise;
                    var a = 0;
                    var b = 1 * 1000;
                    var retry = function (x) {
                        if (notYetPumpkin === x) {
                            ADSAFE.later(function () {
                                send(self, href, 'GET', retry);
                            }, b);
                            var c = Math.min(a + b, 60 * 60 * 1000);
                            a = b;
                            b = c;
                        } else {
                            pr.resolve(x);
                            resolved = true;
                        }
                    };
                    send(self, href, 'GET', retry);
                }
                cache(op, arg1, arg2, arg3);
            } else {
                send(self, href, op, arg1, arg2, arg3);
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
            case 'object':
                if (includes(value, '@')) {throw new Error('forged reference');}
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
        var baseOP = /^([a-zA-Z][\w\-\.\+]*:\/\/[^\/]*\/)(.*)$/.exec(base);
        var origin = baseOP[1];
        var path = baseOP[2];
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
        return origin + path + href;
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
            if (/^application\/do-not-execute(?=;|$)/i.test(contentType)) {
                return http.responseText;
            }
            return JSON.parse(http.responseText, function (key, value) {
                if (includes(value, '!')) { return lib.Q.reject(value['!']); }
                if (includes(value, '@')) {
                    return sealURLref(resolveURI(base, value['@']));
                }
                if (includes(value, '=')) { return value['=']; }
                return value;
            });
        case 204:
        case 205:
            return null;
        case 303:
            var see = http.getResponseHeader('Location');
            return see ? sealURLref(resolveURI(base, see)) : null;
        case 404:
            return notYetPumpkin;
        default:
            return lib.Q.reject({
                $: [ 'org.ref_send.promise.Failure', 'NaO' ],
                status: http.status,
                phrase: http.statusText
            });
        }
    }

    /**
     * Constructs a Request-URI with options.
     * @param href  target URLref
     * @param q     optional client-specified query
     * @param x     optional session key
     * @param w     optional message window number
     */
    function makeRequestURI(href, q, x, w) {
        var requestQuery = '';
        if (undefined !== q && null !== q) {
            requestQuery = '?q=' + encodeURIComponent(String(q));
        }
        if (x) {
            requestQuery += ('' === requestQuery) ? '?' : '&';
            requestQuery += 'x=' + encodeURIComponent(String(x));
            requestQuery += '&w=' + encodeURIComponent(String(w));
        }
        var pqf = /([^\?#]*)([^#]*)(.*)/.exec(href);
        if (pqf[2]) {
            requestQuery += ('' === requestQuery) ? '?' : '&';
            requestQuery += pqf[2].substring(1);
        }
        if (pqf[3]) {
            var args = pqf[3].substring(1).split('&');
            for (var i = 0; i != args.length; ++i) {
                if (/^=/.test(args[i])) {
                    requestQuery += '#' + args[i].substring(1);
                    if (i + 1 !== args.length) {
                        requestQuery += '&' + args.slice(i + 1).join('&');
                    }
                    break;
                }
                requestQuery += ('' === requestQuery) ? '?' : '&';
                requestQuery += args[i];
            }
        }
        return pqf[1] + requestQuery;
    }

    /**
     * Constructs a pending request queue.
     */
    function makeSession() {
        var x = null;               // session id
        var w = 0;                  // number of received responses
        var pending = [];           // pending requests
        var initialized = false;    // session initialization request queued?
        var connection = null;      // current connection

        function makeConnection(timeout) {
            if (undefined === timeout) {
                timeout = 15 * 1000;
            }

            var http;
            if (window.XMLHttpRequest) {
                http = new XMLHttpRequest();
            } else {
                http = new ActiveXObject('Microsoft.XMLHTTP');
            }
            var heartbeat = (new Date()).getTime();
            var self = function () {
                if (self !== connection) { return; }

                var m = pending[0];
                if ('WHEN' === m.op) {
                    pending.shift();
                    if (0 === pending.length) {
                        connection = null;
                    } else {
                        ADSAFE.later(self);
                    }

                    m.resolve(m.target);
                    return;
                }

                var requestURI = makeRequestURI(
                    m.href, m.q, m.idempotent ? null : x, w);
                http.open(m.op, /^[^#]*/.exec(requestURI)[0], true);
                http.onreadystatechange = function () {
                    if (3 === http.readyState || 4 === http.readyState) {
                        heartbeat = (new Date()).getTime();
                    }
                    if (self !== connection) { return; }

                    if (4 !== http.readyState) { return; }
                    if (http.status < 200 || http.status >= 500) { return; }

                    if (m !== pending.shift()) { throw new Error(); }
                    w += 1;
                    if (0 === pending.length) {
                        connection = null;
                    } else {
                        ADSAFE.later(self);
                    }

                    m.resolve(deserialize(requestURI, http));
                };
                if (undefined === m.argv) {
                    http.send(null);
                } else {
                    try {
                        /*
                         * Use Content-Type "text/plain" so that a POST request
                         * remains a 'simple method' in the rules for
                         * cross-domain XHR.
                         */
                        http.setRequestHeader('Content-Type', 'text/plain');
                    } catch (e) {}
                    http.send(serialize(requestURI, m.argv));
                }
            };
            if (timeout) { (function () {
                var watcher = function () {
                    if (connection !== self) { return; }

                    var delta = ((new Date()).getTime()) - heartbeat;
                    if (delta >= timeout) {
                        if (x || pending[0].idempotent) {
                            connection = makeConnection(
                                Math.min(10 * timeout, 60 * 60 * 1000));
                            ADSAFE.later(connection);
                            try { http.abort(); } catch (e) {}
                        }
                    } else {
                        ADSAFE.later(watcher, timeout - delta);
                    }
                };
                ADSAFE.later(watcher, timeout);
            }()); }
            return self;
        }

        return function (target, href, op, resolve, q, argv) {
            var idempotent = 'GET' === op || 'HEAD' === op ||
                             'PUT' === op || 'DELETE' === op ||
                             'OPTIONS' === op || 'TRACE' === op ||
                             'WHEN' === op;
            if (!idempotent && !initialized) {
                pending.push({
                    idempotent: true,
                    href: resolveURI(href, '?q=create&s=sessions'),
                    op: 'POST',
                    argv: [],
                    resolve: function (value) {
                        x = value.key;
                    }
                });
                initialized = true;
            }
            pending.push({
                idempotent: idempotent,
                target: target,
                href: href,
                op: op,
                resolve: resolve,
                q: q,
                argv: argv
            });
            if (!connection) {
                connection = makeConnection();
                ADSAFE.later(connection);
            }
        };
    }

    /**
     * Enqueues an HTTP request.
     * @param target    target reference
     * @param href      target URLref
     * @param op        HTTP verb
     * @param resolve   response resolver
     * @param q         query string argument
     * @param argv      JSON value for request body
     */
    var send = (function () {
        var sessions = { /* origin => session */ };
        return function (target, href, op, resolve, q, argv) {
            var origin = resolveURI(href, '/');
            var session = ADSAFE.get(sessions, origin);
            if (!session) {
                session = makeSession();
                ADSAFE.set(sessions, origin, session);
            }
            return session(target, href, op, resolve, q, argv);
        };
    }());

    function unsealURLref(p) {
        unsealedURLref = null;
        if ('function' === typeof p) { p(); }
        var r = unsealedURLref;
        unsealedURLref = null;
        return r;
    }

    return {

        /**
         * Gets a remote reference for the window's current location.
         */
        getLocation: function () { return sealURLref(window.location.href); },

        /**
         * Navigate the window.
         * @param target    remote reference for new location
         * @return <code>true</code> if navigation successful,
         *         else <code>false</code>
         */
        navigate: function (target) {
            var href = unsealURLref(target);
            if (null === href) { return false; }
            window.location.assign(href);
            return true;
        },

        /**
         * Sets the 'href' attribute.
         * @param elements  bunch of elements to modify
         * @param target    remote reference
         * @return number of elements modified
         */
        href: function (elements, target) {
            var n = 0;
            if (null === target) {
                elements.___nodes___.filter(function (node) {
                    node.removeAttribute('href');
                    n += 1;
                });
            } else {
                var href = unsealURLref(target);
                if (null !== href) {
                    elements.___nodes___.filter(function (node) {
                        switch (node.tagName.toUpperCase()) {
                        case 'A':
                            node.setAttribute('href', href);
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
         * @param target    remote reference
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
                var src = unsealURLref(target);
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
         * Constructs a remote reference.
         * @param base  optional remote reference for base URLref
         * @param href  URLref to wrap
         * @param args  optional query argument map
         */
        _ref: function (base, href, args) {
            var url = resolveURI(unsealURLref(base), href);
            if (undefined !== args && null !== args) {
                var query = '?';
                if ('object' === typeof args) {
                    for (k in args) { if (includes(args, k)) {
                        if ('?' !== query) {
                            query += '&';
                        }
                        query += encodeURIComponent(String(k)) + '=' +
                                 encodeURIComponent(String(ADSAFE.get(args,k)));
                    } }
                } else {
                    query += args;
                }
                url = resolveURI(url, query);
            }
            return sealURLref(url);
        },

        /**
         * Extracts the URLref contained within a remote reference.
         * @param arg       remote reference to extract URLref from
         * @param target    optional remote reference for base URL
         * @return the URLref, or <code>null</code> if not a remote reference
         */
        _url: function (arg, target) {
            var href = unsealURLref(arg);
            if (null === href || !target) { return href; }
            var base = unsealURLref(target);
            if (null === base) { return href; }
            return relateURI(base, href);
        }
    };
});
