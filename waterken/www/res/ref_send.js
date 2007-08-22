// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
var _ = function () {
    function enqueue(task) { setTimeout(task, 0); }

    function Promise() {}

    // a rejected promise
    function Rejected(reason) {
        this.reason = reason;
    }
    Rejected.prototype = new Promise();
    Rejected.prototype.toJSONString = function () {
        return '{"$":' + this.$.toJSONString() +
               ',"reason":' + this.reason.toJSONString() + '}';
    };
    Rejected.prototype.$ = [ 'org.ref_send.promise.Rejected' ];
    Rejected.prototype.cast = function () { throw this.reason; };
    Rejected.prototype.when = function (fulfill, reject) {
        if (undefined !== reject) {
            var reason = this.reason;
            enqueue(function () { reject(reason); });
        }
    };
    Rejected.prototype.get = function () { return new Rejected(this.reason); };
    Rejected.prototype.post = function () { return new Rejected(this.reason); };

    // a fulfilled promise
    function Fulfilled(value) {
        this.value = value;
    }
    Fulfilled.prototype = new Promise();
    Fulfilled.prototype.toJSONString = function () {
        return this.value.toJSONString();
    };
    Fulfilled.prototype.$ = [ 'org.ref_send.promise.Fulfilled' ];
    Fulfilled.prototype.cast = function () { return this.value; };
    Fulfilled.prototype.when = function (fulfill, reject) {
        var value = this.value;
        enqueue(function () { fulfill(value); });
    };

    var indeterminate_ = new Rejected({
        $: [ 'org.ref_send.promise.Indeterminate' ]
    });
    function ref(value) {
        if (undefined === value) { return indeterminate_; }
        if (null === value) { return indeterminate_; }
        if (Promise === value.constructor) { return value; }
        return new Fulfilled(value);
    }

    // a deferred promise
    function Tail() {
        this.value_ = indeterminate_;
        this.observers = [];
    }
    Tail.prototype = new Promise();
    Tail.prototype.toJSONString = function () {
        return this.value_.toJSONString();
    };
    Tail.prototype.cast = function () { return this.value_.cast(); };
    Tail.prototype.when = function (fulfill, reject) {
        if (undefined === this.observers) {
            this.value_.when(fulfill, reject);
        } else {
            this.observers.push(function (value_) {
                value_.when(fulfill, reject);
            });
        }
    };

    // a promise resolver
    function Resolver() {}
    Resolver.prototype = {};
    Resolver.prototype.fulfill = function (value) { this.resolve(ref(value)); };
    Resolver.prototype.reject = function (reason) {
        this.resolve(new Rejected(reason));
    };

    // a deferred promise resolver
    function Head(tail) {
        this.tail = tail;
    }
    Head.prototype = new Resolver();
    Head.prototype.resolve = function (value_) {
        if (undefined === this.tail) { return; }
        this.tail.value_ = value_;
        var observers = this.tail.observers;
        delete this.tail.observers;
        delete this.tail;
        for (var i = 0; i !== observers.length; ++i) {
            observers[i](value_);
        }
    };

    // a remote promise
    function Remote(URL) {
        this['@'] = URL;
    }
    Remote.prototype = new Promise();
    Remote.prototype.cast = function () { return this; };

    // finish off the promise implementations
    Fulfilled.prototype.get = function (noun) {
        var p_ = new Tail();
        var r = new Head(p_);
        var target = this.value;
        enqueue(function () { r.fulfill(target[noun]); });
        return p_;
    };
    Fulfilled.prototype.post = function (verb, argv) {
        var p_ = new Tail();
        var r = new Head(p_);
        var target = this.value;
        enqueue(function () {
            var f = target[verb];
            if (undefined === f) { return r.resolve(indeterminate_); }
            var x;
            try {
                x = f.apply(target, argv);
            } catch (reason) { return r.reject(reason); }
            r.fulfill(x);
        });
        return p_;
    };
    Tail.prototype.get = function (noun) {
        if (undefined === this.observers || indeterminate_ !== this.value_) {
            return this.value_.get(noun);
        }
        var p_ = new Tail();
        var r = new Head(p_);
        this.observers.push(function (value_) {
            r.resolve(value_.get(noun));
        });
        return p_;
    };
    Tail.prototype.post = function (verb, argv) {
        if (undefined === this.observers || indeterminate_ !== this.value_) {
            return this.value_.post(verb, argv);
        }
        var p_ = new Tail();
        var r = new Head(p_);
        this.observers.push(function (value_) {
            r.resolve(value_.post(verb, argv));
        });
        return p_;
    };

    function Message(method, URL, argv, receive) {
        this.method = method;
        this.URL = URL;
        this.argv = argv;
        this.receive = receive;
    }
    function makeHost() {
        var http;
        if (window.XMLHttpRequest) {
            http = new XMLHttpRequest();
        } else {
            http = new ActiveXObject('MSXML2.XMLHTTP.3.0');
        }
        var active = false;
        var pending = [ ];  // [ Message ]
        var output = function () {
            var m = pending[0];
            http.open(m.method, m.URL, true);
            http.onreadystatechange = function () {
                if (4 !== http.readyState) { return; }
                if (m !== pending.shift()) { throw 'problem'; }
                if (0 === pending.length) {
                    active = false;
                } else {
                    enqueue(output);
                }
                m.receive(http);
            };
            if (null === m.argv) {
                http.send(null);
            } else {
                http.setRequestHeader('Content-Type', 'application/json');
                http.send(m.argv.toJSONString());
            }
        };

        return {
            send: function (msg) {
                pending.push(msg);
                if (!active) {
                    enqueue(output);
                    active = true;
                }
            }
        };
    }
    var origin = makeHost();

    function resolveURI(base, url) {
        // not complete, but good enough for URLs returned by our server
        if ('' === url) { return base; }
        if (/^[a-zA-Z][\w\-\.\+]*:/.test(url)) { return url; }
        if (/^\//.test(url)) {
            var service = /^.*:\/\/.*\//.exec(base);
            if (null === service) { return url; }
            return service[0] + url.substring(1);
        }
        base = base.substring(0, base.lastIndexOf('/') + 1);
        if (/^\.\//.test(url)) { return base + url.substring(2); }
        while ('../' === url.substring(0, '../'.length)) {
            base = base.substring(0, base.lastIndexOf('/', base.length - 2)+1);
            url = url.substring('../'.length);
        }
        return base + url;
    }
    function deserialize(base, http) {
        if (200 === http.status || 201 === http.status ||
            202 === http.status || 203 === http.status) {
            return http.responseText.parseJSON(function (key, value) {
                if (null === value) { return value; }
                if ('object' !== typeof value) { return value; }
                if (value.hasOwnProperty('@')) {
                    return new Remote(resolveURI(base, value['@']));
                }
                if (value.hasOwnProperty('$')) {
                    var $ = value.$;
                    for (var i = 0; i !== $.length; ++i) {
                        if ($[i] === 'org.ref_send.promise.Rejected') {
                            return new Rejected(value.reason);
                        }
                    }
                }
                return value;
            })[0];
        }
        if (204 === http.status || 205 === http.status) {
            return null;
        }
        if (303 === http.status) {
            var see = http.getResponseHeader('Location');
            return see ? new Remote(see) : null;
        }
        return new Rejected({
            $: [ 'org.waterken.http.Failure',
                 'org.ref_send.promise.Indeterminate' ],
            status: http.status,
            phrase: http.statusText
        });
    }

    Remote.prototype.describe = function () {
        var urlref = this['@'];
        var iFragment = urlref.indexOf('#');
        var url = -1 !== iFragment ? urlref.substring(0, iFragment) : urlref;
        var iQuery = url.indexOf('?');
        var path = -1 !== iQuery ? url.substring(0, iQuery) : url;
        var iName = path.lastIndexOf('/') + 1;
        var base = path.substring(0, iName);
        var o;
        if (-1 === iFragment) {
            o = path.substring(iName);
        } else {
            o = urlref.substring(iFragment + 1);
        }
        var query = -1 === iQuery ? '?' : url.substring(iQuery) + '&';
        var target = base + 'describe' + query + 'o=' + o;

        var p_ = new Tail();
        var r = new Head(p_);
        origin.send(new Message('GET', target, null, function (http) {
            var base = target.substring(0, target.indexOf('?'));
            r.fulfill(deserialize(base, http));
        }));
        return p_;
    };
    Remote.prototype.when = function (fulfill, reject) {
        var urlref = this['@'];
        var iFragment = urlref.indexOf('#');
        var url = -1 !== iFragment ? urlref.substring(0, iFragment) : urlref;
        var iQuery = url.indexOf('?src=');
        if (-1 === iQuery || -1 === iFragment) {
            var value = this;
            enqueue(function () { fulfill(value); });
        } else {
            var i = iQuery + '?src='.length;
            var j = url.indexOf('&', i);
            var src = -1 !== j ? url.substring(i, j) : url.substring(i);
            var base = resolveURI(url.substring(0, iQuery),
                                  decodeURIComponent(src));
            var o = urlref.substring(iFragment + 1);
            var target = resolveURI(base, '?o=' + encodeURIComponent(o));
            origin.send(new Message('GET', target, null, function (http) {
                ref(deserialize(base, http)).when(fulfill, reject);
            }));
        }
    };
    Remote.prototype.get = function (noun) {
        var proxy = this;
        var urlref = proxy['@'];
        var iFragment = urlref.indexOf('#');
        if (-1 === iFragment) { return indeterminate_; }
        var target = urlref.substring(0, iFragment);
        target += -1 === target.indexOf('?') ? '?' : '&';
        target += 'p=' + encodeURIComponent(noun);
        target += '&s=' + encodeURIComponent(urlref.substring(iFragment + 1));

        var p_ = new Tail();
        var r = new Head(p_);
        origin.send(new Message('GET', target, null, function (http) {
            if (404 === http.status && -1 !== urlref.indexOf('?src=')) {
                proxy.when(function (value) {
                    r.resolve(ref(value).get(noun));
                }, function (reason) { r.reject(reason); });
            } else {
                var base = target.substring(0, target.indexOf('?'));
                r.fulfill(deserialize(base, http));
            }
        }));
        return p_;
    };
    Remote.prototype.post = function (verb, argv) {
        var proxy = this;
        var urlref = proxy['@'];
        var iFragment = urlref.indexOf('#');
        if (-1 === iFragment) { return indeterminate_; }
        var target = urlref.substring(0, iFragment);
        target += -1 === target.indexOf('?') ? '?' : '&';
        target += 'p=' + encodeURIComponent(verb);
        target += '&s=' + encodeURIComponent(urlref.substring(iFragment + 1));

        var p_ = new Tail();
        var r = new Head(p_);
        origin.send(new Message('POST', target, argv, function (http) {
            if (404 === http.status && -1 !== urlref.indexOf('?src=')) {
                proxy.when(function (value) {
                    r.resolve(ref(value).post(verb, argv));
                }, function (reason) { r.reject(reason); });
            } else {
                var base = target.substring(0, target.indexOf('?'));
                r.fulfill(deserialize(base, http));
            }
        }));
        return p_;
    };

    // export the public API
    return {
        enqueue: enqueue,
        _: ref,
        when: function (value, fulfill, reject) {
            var p_ = new Tail();
            var r = new Head(p_);
            ref(value).when(function (value) {
                var x;
                try {
                    x = fulfill(value);
                } catch (e) {
                    return r.reject(e);
                }
                return r.fulfill(x);
            }, function (reason) {
                if (undefined === reject) { return r.reject(reason); }
                var x;
                try {
                    x = reject(reason);
                } catch (e) {
                    return r.reject(e);
                }
                return r.fulfill(x);
            });
            return p_;
        },
        defer: function () {
            var p_ = new Tail();
            return { promise_: p_, resolver: new Head(p_) };
        },
        connect: function (URL) { return new Remote(URL); }
    };
} ();
