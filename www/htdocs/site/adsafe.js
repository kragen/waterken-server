// adsafe.js
// 2008-07-19

// This file implements the core ADSAFE runtime. A site may add additional
// methods to this object understanding that those methods will be made
// available to guest code.

/*global ADSAFE */

/*jslint browser: true */

/*members ___dom___, apply, arguments, call, callee, caller, charAt,
    constructor, eval, filter, get, getElementById, getElementsByTagName,
    go, id, invoke, later, length, lib, message, name, prototype, push,
    removeChild, set, tagName, unwatch, valueOf, watch, window
*/


ADSAFE = function () {

    var adsafe_id, adsafe_lib;

//  These member names are banned. The ADSAFE.get and ADSAFE.put methods will
//  not allow access to these properties.

    var banned = {
        apply           : true,
        'arguments'     : true,
        call            : true,
        callee          : true,
        caller          : true,
        constructor     : true,
        'eval'          : true,
        prototype       : true,
        unwatch         : true,
        valueOf         : true,
        watch           : true
    };

//  The error function is called if there is a violation or confusion.
//  It throws an exception.

    function error() {
        throw {
            name: "ADsafe",
            message: "ADsafe violation."
        };
    }

//  Firefox implemented some of its array methods carelessly. If a method is
//  called as a function it returns the global object. ADsafe cannot tolerate
//  that, so we wrap the methods to make them safer ad slower.

    function mozilla(name) {
        var method = Array.prototype[name];
        Array.prototype[name] = function () {
            if (this === this.window) {
                error();
            }
            return method.apply(this, arguments);
        };
    }

    mozilla('concat');
    mozilla('reverse');
    mozilla('sort');

    if (typeof Array.prototype.filter !== 'function') {
        Array.prototype.filter = function (func) {
            var result = [], i, length = this.length;
            for (i = 0; i < length; i += 1) {
                if (func(this[i])) {
                    result.push(this[i]);
                }
            }
            return result;
        };
    }

//  The reject function enforces the restriction on get and put.
//  It allows access only to objects and arrays. It does not allow use of
//  the banned names, or names that are not strings or numbers, or strings
//  that start with _.

    var reject = function (object, name) {
        return typeof object !== 'object' || banned[name] === true ||
            (typeof name !== 'number' && (typeof name !== 'string' ||
            name.charAt(0) === '_'));
    };

//  Return the ADSAFE object.

    return {


//  ADSAFE.get retrieves a value from an object.

        get: function (object, name) {
            if (!reject(object, name)) {
                return object[name];
            }
            error();
        },


//  ADSAFE.put stores a value in an object. It will not store functions.

        set: function (object, name, value) {
            if (!reject(object, name) && typeof value !== 'function') {
                object[name] = value;
                return;
            }
            error();
        },

//  ADSAFE.invoke invokes a method on an object. It takes an object,
//  a method name, and an array of arguments.

        invoke: function (object, name, argv) {
            return ADSAFE.get(object, name).apply(object, argv);
        },

//  ADSAFE.later calls a function at a later time.

        later: function (func, timeout) {
            if (typeof func === 'function') {
                setTimeout(func, timeout || 0);
            } else {
                error();
            }
        },


//  ADSAFE.id allows a guest widget to indicate that it wants to load
//  ADsafe approved libraries.

        id: function (id) {

//  Calls to ADSAFE.id must be balanced with calls to ADSAFE.go.
//  Only one id can be active at a time.

            if (adsafe_id) {
                error();
            }
            adsafe_id = id;
            adsafe_lib = {};
        },


//  ADSAFE.lib allows an approved ADsafe library to make itself available
//  to a widget. The library provides a name and a function. The result of
//  calling that function will be made available to the widget via the name.

        lib: function (name, f) {
            if (!adsafe_id) {
                error();
            }
            adsafe_lib[name] = f();
        },


//  ADSAFE.go allows a guest widget to get access to a wrapped dom node and
//  approved ADsafe libraries. It is passed an id and a function. The function
//  will be passed the wrapped dom node and an object containing the libraries.

        go: function (id, f) {
            var dom, i, scripts;

//  If ADSAFE.id was called, the id better match.

            if (adsafe_id && adsafe_id !== id) {
                error();
            }

//  Get the dom node for the widget's div container.

            dom = document.getElementById(id);
            if (dom.tagName !== 'DIV') {
                error();
            }
            adsafe_id = null;

//  Delete the scripts held in the div. They have all run, so we don't need
//  them any more. If the div had no scripts, then something is wrong.
//  This provides some protection against mishaps due to weakness in the
//  document.getElementById function.

            scripts = dom.getElementsByTagName('script');
            if (scripts.length === 0) {
                error();
            }
            for (i = 0; i < scripts.length; i += 1) {
                dom.removeChild(scripts[i]);
            }

//  Call the supplied function.

            if (adsafe_lib) {
                f(ADSAFE.___dom___(dom, id), adsafe_lib);
            } else {
                f(ADSAFE.___dom___(dom, id));
            }
            dom = null;
            adsafe_lib = null;
        }

    };
}();