// Copyright 2007-2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
//
// ref_send.js version: 2008-07-21
"use subset cautious";
ADSAFE.lib("Q", function () {
    function error($) {
        var r;
        try { r = null.promise; } catch (e) { r = e; }
        if (undefined !== $) {
            r.$ = $;
        }
        return r;
    }
    function reject(reason) {
        var self = function (op, arg1, arg2, arg3) {
            if (undefined === op) {
                return {
                    $: [ 'org.ref_send.promise.Rejected' ],
                    reason: reason
                };
            }
            if ('WHEN' === op) { return arg2 ? arg2(reason) : self; }

            var r;
            if ('GET' === op) {
                r = self;
            } else if ('POST' === op) {
                r = self;
            } else {
                throw error();
            }
            return arg1(r);
        };
        return self;
    }
    function ref(value) {
        if (null === value || undefined === value) { return reject(error()); }
        if ('number' === typeof value && !isFinite(value)) {
            return reject(error([ 'NaN' ]));
        }
        return function (op, arg1, arg2, arg3) {
            if (undefined === op) { return value; }
            if ('WHEN' === op) { return arg1(value); }

            var r;
            if ('GET' === op) {
                if ('*' === arg2) {
                    r = value;
                } else {
                    r = ADSAFE.get(value, arg2);
                }
            } else if ('POST' === op) {
                r = ADSAFE.invoke(value, arg2, arg3);
            } else {
                throw error();
            }
            return arg1(r);
        };
    }
    var enqueue = function () {
        var active = false;
        var pending = [];
        var run = function () {
            var task = pending.shift();
            if (0 === pending.length) {
                active = false;
            } else {
                ADSAFE.later(run);
            }
            task();
        };
        return function (task) {
            pending.push(task);
            if (!active) {
                ADSAFE.later(run);
                active = true;
            }
        };
    } ();
    function forward(p, op, arg1, arg2, arg3) {
        enqueue(function () { p(op, arg1, arg2, arg3); });
    }
    function promise(value) {
        return 'function' === typeof value ? value : ref(value);
    }
    function defer() {
        var value = ref();
        var pending = [];
        return {
            promise: function (op, arg1, arg2, arg3) {
                if (undefined === op) { return value(); }
                if (null === pending) {
                    forward(value, op, arg1, arg2, arg3);
                } else {
                    pending.push({
                        op: op,
                        arg1: arg1,
                        arg2: arg2,
                        arg3: arg3
                    });
                }
            },
            resolve: function (p) {
                if (null === pending) { return; }

                var todo = pending;
                pending = null;
                value = promise(p);
                todo.filter(function (task) {
                    forward(value, task.op, task.arg1, task.arg2, task.arg3);
                });
            }
        };
    }

    return {
        enqueue: enqueue,
        error: error,
        reject: reject,
        ref: ref,
        defer: defer,
        near: function (value) {
            return 'function' === typeof value ? value() : value;
        },
        when: function (value, fulfilled, rejected) {
            var r = defer();
            var done = false;   // ensure the untrusted promise makes at most a
                                // single call to one of the callbacks
            forward(promise(value), 'WHEN', function (x) {
                if (done) { throw error(); }
                done = true;
                r.resolve(ref(x)('WHEN', fulfilled, rejected));
            }, function (reason) {
                if (done) { throw error(); }
                done = true;
                r.resolve(rejected(reason));
            });
            return r.promise;
        },
        get: function (value, noun) {
            var r = defer();
            forward(promise(value), 'GET', r.resolve, noun);
            return r.promise;
        },
        post: function (value, verb, argv) {
            var r = defer();
            forward(promise(value), 'POST', r.resolve, verb, argv);
            return r.promise;
        }
    };
});
