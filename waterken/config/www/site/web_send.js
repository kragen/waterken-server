// Copyright 2007-2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
//
// web_send.js version: 2008-08-06
//
// This library doesn't actually pass the ADsafe verifier, but rather is
// designed to provide a safe interface to the network, that can be
// loaded as an ADsafe library.
ADSAFE.lib('web', function () {
    function reject(reason) {
        var self = function (op, arg1, arg2, arg3) {
            if (undefined === op) {
                return {
                    $: [ 'org.ref_send.promise.Rejected' ],
                    reason: reason
                };
            }
            if ('WHEN' === op) { return arg2 ? arg2(reason) : self; }
            return arg1(self);
        };
        return self;
    }

    var unsealedURLref = null;
    function proxy(URLref) {
        var self = function (op, arg1, arg2, arg3) {
            if (undefined === op) { unsealedURLref = URLref; return self; }
            if ('WHEN' === op) {
                // TODO
            } else {
                send(URLref, op, arg1, arg2, arg3);
            }
        };
        return self;
    }
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
                    value = {
                        $: [ 'org.ref_send.promise.Rejected' ],
                        reason: { $: [ 'NaN' ] }
                    };
                }
                break;
            }
            return value;
        }, ' ');
    }
    function resolveURI(base, href) {
        if ('' === href) {
            var iRef = base.indexOf('#');
            return -1 !== iRef ? base.substring(0, iRef) : base;
        }
        if (/^#/.test(href)) {
            var iRef = base.indexOf('#');
            base = -1 !== iRef ? base.substring(0, iRef) : base;
            return base + href;
        }
        if (/^[a-zA-Z][\w\-\.\+]*:/.test(href)) { return href; }
        if (/^\/\//.test(href)) {
            return /^[a-zA-Z][\w\-\.\+]*:/.exec(base)[0] + href;
        }
        if (/^\//.test(href)) {
            return /^[a-zA-Z][\w\-\.\+]*:\/\/[^\/]*\//.exec(base)[0] +
                   href.substring(1);
        }
        var iQuery = base.indexOf('?');
        base = -1 !== iQuery ? base.substring(0, iQuery) : base;
        if (/^\?/.test(href)) { return base + href; }
        base = base.substring(0, base.lastIndexOf('/') + 1);
        var parts = /^([a-zA-Z][\w\-\.\+]*:\/\/[^\/]*\/)(.*)$/.exec(base);
        var host = parts[1];
        var path = parts[2];
        while (true) {
            if ('../' === href.substring(0, '../'.length)) {
                path = path.substring(0, path.lastIndexOf('/', path.length-2) + 1);
                href = href.substring('../'.length);
            } else if ('./' === href.substring(0, './'.length)) {
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
    function deserialize(base, http) {
        switch (http.status) {
        case 200:
        case 201:
        case 202:
        case 203:
            var contentType = http.getResponseHeader('Content-Type');
            if (!/^application\/jsonrequest(;|$)/i.test(contentType) &&
                             !/^text\/plain(;|$)/i.test(contentType)) {
                return {
                    $: [ 'org.web_send.Entity' ],
                    type: contentType,
                    text: http.responseText
                };
            }
            return JSON.parse(http.responseText, function (key, value) {
                if (null === value) { return value; }
                if ('object' !== typeof value) { return value; }
                if (value.hasOwnProperty('@')) {
                    return proxy(resolveURI(base, value['@']));
                }
                if (value.hasOwnProperty('$')) {
                    var $ = value.$;
                    for (var i = 0; i !== $.length; ++i) {
                        if ($[i] === 'org.ref_send.promise.Rejected') {
                            return reject(value.reason);
                        }
                    }
                }
                return value;
            })[0];
        case 204:
        case 205:
            return null;
        case 303:
            var see = http.getResponseHeader('Location');
            return see ? proxy(see) : null;
        default:
            return reject({
                $: [ 'org.web_send.Failure',
                     'org.ref_send.promise.Indeterminate' ],
                status: http.status,
                phrase: http.statusText
            });
        }
    }
    function request(URLref, member) {
        var iRef = URLref.indexOf('#');
        var ref = -1 !== iRef ? URLref.substring(iRef + 1) : undefined;
        var URL = -1 !== iRef ? URLref.substring(0, iRef) : URLref;
        var iQuery = URL.indexOf('?');
        var target = -1 !== iQuery ? URL.substring(0, iQuery) : URL;
        target = target.substring(0, target.lastIndexOf('/') + 1);
        target += '?';
        if (undefined !== member) {
            target += 'p=' + encodeURIComponent(member) + '&';
        }
        if (undefined != ref) {
            target += 's=' + ref;
        }
        return target;
    }
    var send = (function () {
        var active = false;
        var pending = [];
        var http;
        if (window.XMLHttpRequest) {
            http = new XMLHttpRequest();
        } else {
            http = new ActiveXObject('MSXML2.XMLHTTP.3.0');
        }
        var output = function () {
            var m = pending[0];
            http.open(m.op, request(m.URLref, m.member), true);
            http.onreadystatechange = function () {
                if (4 !== http.readyState) { return; }
                if (m !== pending.shift()) { throw null.error; }
                if (0 === pending.length) {
                    active = false;
                } else {
                    ADSAFE.later(output);
                }

                if (404 === http.status && -1 !== m.URLref.indexOf('?src=')) {
                    // TODO
                    return;
                }
                m.resolve(deserialize(m.URLref, http));
            };
            if (undefined === m.argv) {
                http.send(null);
            } else {
                http.setRequestHeader('Content-Type', 'text/plain');
                http.send(serialize(m.argv));
            }
        };
        return function (URLref, op, resolve, member, argv) {
            pending.push({
                URLref: URLref,
                op: op,
                resolve: resolve,
                member: member,
                argv: argv
            });
            if (!active) {
                ADSAFE.later(output);
                active = true;
            }
        };
    }) ();

    return {
        page: proxy(window.document.location.toString()),
        proxy: proxy,
        crack: function (p) {
            unsealedURLref = null;
            if ('function' === typeof p) { p() }
            var r = unsealedURLref;
            unsealedURLref = null;
            return r;
        }
    };
});
