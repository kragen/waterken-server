/*
 * Copyright 2007-2009 Tyler Close under the terms of the MIT X license found
 * at http://www.opensource.org/licenses/mit-license.html
 *
 * ref_send.js version: 2009-05-06
 */
"use strict";
ADSAFE.lib('Q', function () {

    function reject(reason, $) {
        if (undefined !== $) {
            reason.$ = $;
        }
        return function (op, arg1, arg2, arg3) {
            if (undefined === op) {
                return { '!' : reason };
            }
            if ('WHEN' === op) { return arg2 ? arg2(reason) : reject(reason); }
            return arg1 ? arg1(reject(reason)) : reject(reason);
        };
    }

    function ref(value) {
        if (null === value || undefined === value) {
            return reject(new ReferenceError(), [ 'NaO' ]);
        }
        if ('number' === typeof value && !isFinite(value)) {
            return reject(new RangeError(), [ 'NaN' ]);
        }
        return function (op, arg1, arg2, arg3) {
            if (undefined === op) { return value; }

            var r;
            if ('WHEN' === op) {
                r = value;
            } else if ('GET' === op) {
                r = undefined === arg2 ? value : ADSAFE.get(value, arg2);
            } else if ('POST' === op) {
                r = ADSAFE.invoke(value, arg2, arg3);
            } else {
                r = reject(new TypeError(), [ 'NaO' ]);
            }
            return arg1 ? arg1(r) : r;
        };
    }

    var enqueue = (function () {
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
    }());

    /**
     * Enqueues a promise operation.
     *
     * The above functions, reject() and ref(), each construct a kind of
     * promise. Other libraries can provide other kinds of promises by
     * implementing the same API. A promise is a function with signature:
     * function (op, arg1, arg2, arg3). The first argument determines the
     * interpretation of the remaining arguments. The following cases must be
     * handled:
     *
     * 'op' is undefined:
     *  Return the most resolved current value of the promise.
     *
     * 'op' is "WHEN":
     *  'arg1': callback to invoke with the fulfilled value of the promise
     *  'arg2': callback to invoke with the rejection reason for the promise
     *
     * 'op' is "GET":
     *  'arg1': callback to invoke with the value of the named property
     *  'arg2': name of the property to read
     *
     * 'op' is "POST":
     *  'arg1': callback to invoke with the return value from the invocation
     *  'arg2': name of the method to invoke
     *  'arg3': array of invocation arguments
     *
     * 'op' is unrecognized:
     *  'arg1': callback to invoke with a rejected promise
     */
    function forward(p, op, arg1, arg2, arg3) {
        enqueue(function () { p(op, arg1, arg2, arg3); });
    }

    /**
     * Gets the corresponding promise for a given reference.
     */
    function promise(value) {
        return 'function' === typeof value ? value : ref(value);
    }

    function defer() {
        var value = reject(new Error(), [ 'NaO' ]);
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

        /**
         * Enqueues a task to be run in a future turn.
         * @param task  function to invoke later
         */
        run: enqueue,

        /**
         * Constructs a rejected promise.
         * @param reason    Error object describing the failure
         * @param $         optional type info to add to reason
         */
        reject: reject,

        /**
         * Constructs a promise for an immediate reference.
         * @param value immediate reference
         */
        ref: ref,

        /**
         * Constructs a ( promise, resolver ) pair.
         *
         * The resolver is a callback to invoke with a more resolved value for
         * the promise. To fulfill the promise, simply invoke the resolver with
         * an immediate reference. To reject the promise, invoke the resolver
         * with the return from a call to reject(). To put the promise in the
         * same state as another promise, invoke the resolver with that other
         * promise.
         */
        defer: defer,

        /**
         * Gets the current value of a promise.
         * @param value promise or immediate reference to evaluate
         */
        near: function (value) {
            return 'function' === typeof value ? value() : value;
        },

        /**
         * Registers an observer on a promise.
         * @param value     promise or immediate reference to observe
         * @param fulfilled function to be called with the resolved value
         * @param rejected  function to be called with the rejection reason
         * @return promise for the return value from the invoked callback
         */
        when: function (value, fulfilled, rejected) {
            var r = defer();
            var done = false;   // ensure the untrusted promise makes at most a
                                // single call to one of the callbacks
            forward(promise(value), 'WHEN', function (x) {
                if (done) { throw new Error(); }
                done = true;
                r.resolve(ref(x)('WHEN', fulfilled, rejected));
            }, function (reason) {
                if (done) { throw new Error(); }
                done = true;
                if (!reason) {
                    reason = new Error();
                }
                r.resolve(rejected ? rejected(reason) : reject(reason));
            });
            return r.promise;
        },

        /**
         * Gets the value of a property in a future turn.
         * @param value promise or immediate reference to get property from
         * @param noun  name of property to get
         * @return promise for the property value
         */
        get: function (value, noun) {
            var r = defer();
            forward(promise(value), 'GET', r.resolve, noun);
            return r.promise;
        },

        /**
         * Invokes a method in a future turn.
         * @param value promise or immediate reference to invoke
         * @param verb  name of method to invoke
         * @param argv  array of invocation arguments
         * @return promise for the return value
         */
        post: function (value, verb, argv) {
            var r = defer();
            forward(promise(value), 'POST', r.resolve, verb, argv);
            return r.promise;
        }
    };
});
