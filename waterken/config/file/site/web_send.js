/*
 * Copyright 2007-2009 Tyler Close under the terms of the MIT X license found
 * at http://www.opensource.org/licenses/mit-license.html
 *
 * web_send.js version: 2009-04-27
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

    function proxy(URLref) {
        var self = function (op, arg1, arg2, arg3) {
            if (undefined === op) {
                unsealedURLref = URLref;
                return self;
            }
            if (/#o=/.test(URLref)) {
                send(URLref, 'GET', function (x) {
                    ('function'===typeof x?x:lib.Q.ref(x))(op,arg1,arg2,arg3);
                });
            } else {
                if ('WHEN' === op) {
                    arg1(self);
                } else {
                    send(URLref, op, arg1, arg2, arg3);
                }
            }
        };
        return self;
    }

    /**
     * Produce the JSON text for a JSON object.
     * @param argv  JSON object to serialize
     */
    function serialize(argv) {
        return JSON.stringify(argv, function (key, value) {
            switch (typeof value) {
            case 'function':
                unsealedURLref = null;
                value = value();
                if (null !== unsealedURLref) {
                    value = { '@' : unsealedURLref };
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
        base = /^[^#]*/.exec(base)[0];  // never include base fragment

        if ('' === href) { return base; }
        if (/^#/.test(href)) { return base + href; }
        if (/^[a-zA-Z][\w\-\.\+]*:/.test(href)) { return href; }
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
        var parts = /^([a-zA-Z][\w\-\.\+]*:\/\/[^\/]*\/)(.*)$/.exec(base);
        var host = parts[1];
        var path = parts[2];
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
                if (null === value) { return value; }
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
            if (null !== r && 'object' === typeof r && r.hasOwnProperty('=')) {
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
     * Enqueues an HTTP request.
     * @param URLref    target URLref
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
            var urlref = /([^#]*)(.*)/.exec(m.URLref);
            var url = urlref[1];
            var sep = /\?/.test(url) ? '&' : '?';
            if (undefined !== m.q) {
                url += sep + 'q=' + encodeURIComponent(m.q);
                sep = '&';
            }
            if (m.session && undefined !== m.session.x) {
                url += sep + 'x=' + encodeURIComponent(m.session.x);
                sep = '&';
                url += sep + 'w=' + m.session.w;
            }
            if (urlref[2]) {
                url += urlref[2].replace('#', sep);
            }
            http.open(m.op, url, true);
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

                m.resolve(deserialize(m.URLref, http));
            };
            if (undefined === m.argv) {
                http.send(null);
            } else {
                http.setRequestHeader('Content-Type', 'text/plain');
                http.send(serialize(m.argv));
            }

            // TODO: monitor the request with a local timeout
        };
        return function (URLref, op, resolve, q, argv) {
            var session = null;
            if ('POST' === op) {
                var origin = resolveURI(URLref, '/');
                session = ADSAFE.get(sessions, origin);
                if (!session) {
                    session = {
                        w: 1
                    };
                    ADSAFE.set(sessions, origin, session);
                    pending.push({
                        URLref: resolveURI(URLref, '#s=sessions'),
                        op: 'POST',
                        q: 'create',
                        argv: [],
                        resolve: function (value) {
                            session.x = value.key;
                        }
                    });
                }
            }
            pending.push({
                session: session,
                URLref: URLref,
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
         */
        href: function (elements, target) {
            if (null === target) {
                elements.___nodes___.filter(function (element) {
                    element.removeAttribute('href');
                    element.onclick = undefined;
                });
            } else {
                var href = crack(target);
                if (null === href) { return false; }
                elements.___nodes___.filter(function (element) {
                    element.setAttribute('href', href);

                    // do page navigation, even if fragment is only difference
                    element.onclick = function () {
                        // TODO: do original fragment navigation
                        window.location.assign(href);
                    };
                });
            }
            return true;
        },

        /**
         * Sets the 'src' attribute.
         * @param elements  bunch of elements to modify
         * @param target    remote promise
         */
        src: function (img, target) {
            if (null === target) {
                elements.___nodes___.filter(function (element) {
                    element.removeAttribute('src');
                });
            } else {
                var src = crack(target);
                if (null === src) { return false; }
                elements.___nodes___.filter(function (element) {
                    element.setAttribute('src', src);
                });
            }
            return true;
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
         * @param URLref    URL reference to wrap
         */
        _proxy: proxy,

        /**
         * Extracts the URLref contained within a remote promise.
         * @param p remote promise to crack
         * @return the URLref, or <code>null</code> if not a remote promise
         */
        _crack: crack
    };
});
