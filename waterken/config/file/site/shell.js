/*
 * Copyright 2009 Tyler Close under the terms of the MIT X license found at
 * http://www.opensource.org/licenses/mit-license.html
 *
 * shell.js version: 2009-06-04
 */
/*global WScript, ActiveXObject */
/*jslint white: false, nomen: false, strict: false, bitwise: true, eqeqeq: true,
         immed: true, newcap: true, plusplus: true, regexp: true, undef: true*/

/**
 * ADsafe global object accessible to libraries and shell code
 */
var ADSAFE;

/**
 * Load a library in the global scope.
 */
function _include_(_path_) {
    eval((function() {
        var fso = new ActiveXObject("Scripting.FileSystemObject");
        var f = fso.GetFile(_path_);
        var s = f.OpenAsTextStream(1);
        var r = s.ReadAll();
        s.Close();
        return r;
    }()));
}

_include_('json2.js');
(function () {

    var _todo_ = [];        // pending task list
    var _timeouts_ = [];    // pending timeouts
    var _banned_ = {
        'arguments'     : true,
        callee          : true,
        caller          : true,
        constructor     : true,
        'eval'          : true,
        prototype       : true,
        unwatch         : true,
        valueOf         : true,
        watch           : true
    };
    var _lib_ = {};    // loading ADsafe libraries

    function _reject_(object, name) {
        return typeof object !== 'object' || _banned_[name] ||
            (typeof name !== 'number' &&
                (typeof name !== 'string' || name.charAt(0) === '_'));
    }
    function _comment_(text) {
        var lines= String(text).split(/\r\n|\n|\r|\u0085|\u000C|\u2028|\u2029/);
        for (var i = 0; i !== lines.length; i += 1) {
            WScript.StdOut.Write('// ');
            WScript.StdOut.WriteLine(lines[+i]);
        }
        WScript.StdOut.WriteLine();
    }
    ADSAFE = {

        lib: function (name, loader) {
            if (Object.hasOwnProperty.call(_lib_, name)) { throw new Error(); }
            _lib_[name] = loader(_lib_);
        },

        log: function (s) {
            WScript.StdOut.WriteLine();
            _comment_(s);
        },

        later: function (task, timeout) {
            if (typeof task !== 'function') { throw new Error(); }

            if (timeout) {
                var timestamp = (new Date()).getTime() + Math.max(0, timeout);
                var i = _timeouts_.length;
                while (0 !== i && timestamp < _timeouts_[i - 1].timestamp) {
                    i -= 1;
                }
                _timeouts_.splice(i, 0, {
                    timestamp: timestamp,
                    task: task
                });
            } else {
                _todo_.push(task);
            }
        },

        isArray: Array.isArray || function (value) {
            return Object.prototype.toString.apply(value) === '[object Array]';
        },

        get: function (object, name) {
            if (arguments.length !== 2) { throw new Error(); }
            if (_reject_(object, name)) { throw new Error(); }

            return object[name];
        },

        set: function (object, name, value) {
            if (arguments.length !== 3) { throw new Error(); }
            if (_reject_(object, name)) { throw new Error(); }

            object[name] = value;
        },

        remove: function (object, name) {
            if (arguments.length !== 2) { throw new Error(); }
            if (_reject_(object, name)) { throw new Error(); }

            delete object[name];
        }
    };
    _include_('ref_send.js');
    _include_('web_send.js');
    (function () {
        // load user specified ADsafe libraries
        for (var i = 0; i !== WScript.Arguments.length; i += 1) {
            _include_(WScript.Arguments[+i]);
        }
    }());
    var lib = _lib_;    // ADsafe libraries accessible to shell code
    _lib_ = null;       // done loading libraries
    function _echo_(x) {
        var text;
        try {
            text = JSON.stringify(x, function (key, value) {
                if ('function' === typeof value) {
                    value = value();
                    var href = lib.web._url(value);
                    if (null !== href) {
                        var r = {};
                        for (var k in value) {
                            if (Object.hasOwnProperty.call(value, k)) {
                                if ('@' !== k) {
                                    r[k] = value[k];
                                }
                            }
                        }
                        r['@'] = href;
                        return r;
                    }

                    if ('function' === typeof value &&
                            Object.hasOwnProperty.call(value, 'reason')) {
                        return {
                            '!' : value('WHEN', null, function (e) {return e;})
                        };
                    }
                }
                if ('number' === typeof value && !isFinite(value)) {
                    return { '!' : { 'class': [ 'NaN' ] } };
                }
                if (value && Object.hasOwnProperty.call(value, '@')) {
                    throw new Error('forged reference');
                }
                return value;
            }, ' ');
        } catch (e) {
            text = JSON.stringify({ '!' : e }, null, ' ');
        }
        if (undefined === text) { return; }
        _comment_(text);

    }
    while (true) {
        WScript.StdOut.Write('; ');

        /*
         * Cscript has a single thread, no event queue and only blocking I/O.
         * Network callbacks only seem to fire during a Sleep(), so we'll take
         * a second to poll for events and pump the task queue.  Hopefully the
         * user is still contemplating the return from the previous expression
         * or is happily typing away at the console.
         */
        (function () {
            var latency = 1000; // ms to delay before processing user input
            while (true) {
                // empty the task queue
                while (_todo_.length) {
                    try {
                        _todo_.shift()();
                    } catch (e) {
                        ADSAFE.log(JSON.stringify({ '!' : e }, null, ' '));
                    }
                }

                if (0 >= latency) { break; }

                // allow any pending network events to fire
                WScript.Sleep(100);
                latency -= 100;

                // check for expired timeouts
                var now = (new Date()).getTime();
                while (_timeouts_.length && _timeouts_[0].timestamp <= now) {
                    _todo_.push(_timeouts_.shift().task);
                }
            }
        }());

        /*
         * Get another expression from the user. A multi-line expression is
         * created by terminating incomplete lines with an open bracket, binary
         * operator, comma or semi-colon.  Trailing whitespace is tolerated.
         */
        var _code_ = '';
        while (true) {
            _code_ += WScript.StdIn.ReadLine();
            if (!/[({\[,;.?:|&\^+\-*%\/=]\s*$/.test(_code_)) { break; }
            WScript.StdOut.Write('  ');
        }
        var _value_;
        try {
            _value_ = eval(_code_);
        } catch (_reason_) {
            _value_ = { '!' : _reason_ };
        }
        _echo_(_value_);
    }
}());
