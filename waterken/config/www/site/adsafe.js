// adsafe.js
// 2008-08-07

// This file implements the core ADSAFE runtime. A site may add additional
// methods to this object understanding that those methods will be made
// available to guest code.

/*global ADSAFE */

/*jslint browser: true, bitwise: false, nomen: false */


"use strict";

ADSAFE = function () {

    var adsafe_id,      // The id of the current widget
        adsafe_lib,     // The script libraries loaded by the current widget
        allow_focus,
        defaultView = document.defaultView,
        ephemeral,
        flipflop,       // Used in :even/:odd processing
        name,
        result,
        star,
        value;


//  These member names are banned from guest scripts. The ADSAFE.get and
// ADSAFE.put methods will not allow access to these properties.

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

    function error(message) {
        debugger ;
        throw {
            name: "ADsafe",
            message: message || "ADsafe violation."
        };
    }


//  Firefox implemented some of its array methods carelessly. If a method is
//  called as a function it returns the global object. ADsafe cannot tolerate
//  that, so we wrap the methods to make them safer and slower.

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

// Add a filter method to Array.

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
//  that start or end with _.

    var reject = function (object, name) {
        return typeof object !== 'object' || banned[name] === true ||
            (typeof name !== 'number' && (typeof name !== 'string' ||
            name.charAt(0) === '_' || name.slice(-1) === '_'));
    };

// We identify four classes of browser events: still, bubbling, sparkling,
// and custom. Still and custom events do not propogate. Bubbling and sparkling
// propogate. Still (except blur) and sparkling events give focus.
// Bubbling and custom events do not give focus.

    var browser_event_class = {
        blur:       'still',
        change:     'still',
        click:      'sparkling',
        dblclick:   'sparkling',
        focus:      'still',
        keypress:   'sparkling',
        mouseover:  'bubbling',
        mouseout:   'bubbling'
    };

    var makeableTagName = {

// This is the whitelist of elements that may be created with the .tag(tagName)
// method.

        a         : true,
        abbr      : true,
        acronym   : true,
        address   : true,
        area      : true,
        b         : true,
        bdo       : true,
        big       : true,
        blockquote: true,
        br        : true,
        button    : true,
        canvas    : true,
        caption   : true,
        center    : true,
        cite      : true,
        code      : true,
        col       : true,
        colgroup  : true,
        dd        : true,
        del       : true,
        dfn       : true,
        dir       : true,
        div       : true,
        dl        : true,
        dt        : true,
        em        : true,
        fieldset  : true,
        font      : true,
        form      : true,
        h1        : true,
        h2        : true,
        h3        : true,
        h4        : true,
        h5        : true,
        h6        : true,
        hr        : true,
        i         : true,
        img       : true,
        input     : true,
        ins       : true,
        kbd       : true,
        label     : true,
        legend    : true,
        li        : true,
        map       : true,
        menu      : true,
        object    : true,
        ol        : true,
        optgroup  : true,
        option    : true,
        p         : true,
        pre       : true,
        q         : true,
        samp      : true,
        select    : true,
        small     : true,
        span      : true,
        strong    : true,
        sub       : true,
        sup       : true,
        table     : true,
        tbody     : true,
        td        : true,
        textarea  : true,
        tfoot     : true,
        th        : true,
        thead     : true,
        tr        : true,
        tt        : true,
        u         : true,
        ul        : true,
        'var'     : true
    };

    function getStyleObject(node) {

// getStyleObject is a function that returns the computed style object for
// a node.

        return node.currentStyle || defaultView.getComputedStyle(node);
    }



    function walkTheDOM(node, func, skip) {

// Recursively traverse the DOM tree, starting with the node, in document
// source order, calling the func on each node visisted.

        if (skip) {
            func(node);
        }
        node = node.firstChild;
        while (node) {
            walkTheDOM(node, func);
            node = node.nextSibling;
        }
    }

    function purge_event_handlers(node) {
        walkTheDOM(node, function (node) {
            node.on = node.change = node.focus = node.blur = null;
        });
    }


    function parse_query(text, id) {

// Convert a query string into an array of op/name/value selectors.
// A query string is a sequence of triples wrapped in brackets; or names,
// possibly prefixed by # . & > _, or :option, or * or /. A triple is a name,
// and operator (one of [=, [!=, [*=, [~=, [$=, or [^=) and a value.

// A name must be all lower case and may contain digits, -, or _.

        var match,          // A match array
            query = [],     // The resulting query array
            selector,
            qx = /^\s*(?:([\*\/])|\[\s*([a-z][0-9a-z_\-]*)\s*(?:([!*~$\^]?\=)\s*([0-9A-Za-z_\-*%&;.\/:!]+)\s*)?\]|#\s*([A-Z]+_[A-Z0-9]+)|:\s*([a-z]+)|([.&_>]?)\s*([a-z][0-9a-z\-]*))\s*/;

// Loop over all of the selectors in the text.

        do {

// The qx teases the components of one selector out of the text, ignoring
// whitespace.

//          match[0]  the whole selector
//          match[1]  * /
//          match[2]  attribute name
//          match[3]  = != *= ~= $= ^=
//          match[4]  attribute value
//          match[5]  # id
//          match[6]  : option
//          match[7]  . & _ >
//          match[8]      name

            match = qx.exec(text);
            if (!match) {
                throw new Error("ADsafe: Bad query:" + text);
            }

// Make a selector object and stuff it in the query.

            if (match[1]) {

// The selector is * or /

                selector = {
                    op: match[1]
                };
            } else if (match[2]) {

// The selector is in brackets.

                selector = match[3] ? {
                    op: '[' + match[3],
                    name: match[2],
                    value: match[4]
                } : {
                    op: '[',
                    name: match[2]
                };
            } else if (match[5]) {

// The selector is an id.

                if (query.length > 0 || !id || match[5].length <= id.length ||
                        match[5].slice(0, id.length) !== id) {
                    error("ADsafe: Bad query: " + text);
                }
                selector = {
                    op: '#',
                    name: match[5]
                };

// The selector is a colon.

            } else if (match[6]) {
                selector = {
                    op: ':' + match[6]
                };

// The selector is one of > . & _ or a naked tag name

            } else {
                selector = {
                    op: match[7],
                    name: match[8]
                };
            }

// Add the selector to the query.

            query.push(selector);

// Remove the selector from the text. If there is more text, have another go.

            text = text.slice(match[0].length);
        } while (text);
        return query;
    }


    var hunter = {

// These functions implement the hunter behaviors.

        '': function (node) {
            var e = node.getElementsByTagName(name), i;
            for (i = 0; i < e.length; i += 1) {
                result.push(e[i]);
            }
        },
        '>': function (node) {
            node = node.firstChild;
            name = name.toUpperCase();
            while (node) {
                if (node.tagName === name) {
                    result.push(node);
                }
                node = node.nextSibling;
            }
        },
        '#': function (node) {
            var n = document.getElementById(name);
            if (n.tagName) {
                result.push(n);
            }
        },
        '/': function (node) {
            var e = node.childNodes, i;
            for (i = 0; i < e.length; i += 1) {
                result.push(e[i]);
            }
        },
        '*': function (node) {
            star = true;
            walkTheDOM(node, function (node) {
                result.push(node);
            }, true);
        }
    };

    var pecker = {
        '.': function (node) {
            return (' ' + node.className + ' ').indexOf(' ' + name + ' ') >= 0;
        },
        '&': function (node) {
            return node.name === name;
        },
        '_': function (node) {
            return node.type === name;
        },
        '[': function (node) {
            return typeof node[name] === 'string';
        },
        '[=': function (node) {
            var member = node[name];
            return typeof member === 'string' && member === value;
        },
        '[!=': function (node) {
            var member = node[name];
            return typeof member === 'string' && member !== value;
        },
        '[^=': function (node) {
            var member = node[name];
            return typeof member === 'string' &&
                member.slice(0, member.length) === value;
        },
        '[$=': function (node) {
            var member = node[name];
            return typeof member === 'string' &&
                member.slice(-member.length) === value;
        },
        '[*=': function (node) {
            var member = node[name];
            return typeof member === 'string' &&
                member.slice.indexOf(value) >= 0;
        },
        '[~=': function (node) {
            var member = node[name];
            return typeof member === 'string' &&
                (' ' + member + ' ').slice.indexOf(' ' + value + ' ') >= 0;
        },
        ':checked': function (node) {
            return node.checked;
        },
        ':unchecked': function (node) {
            return node.tagName && !node.checked;
        },
        ':enabled': function (node) {
            return node.tagName && !node.disabled;
        },
        ':disabled': function (node) {
            return node.tagName && node.disabled;
        },
        ':visible': function (node) {
            return node.tagName && getStyleObject(node).visibility === 'visible';
        },
        ':hidden': function (node) {
            return node.tagName && getStyleObject(node).visibility !== 'visible';
        },
        ':text': function (node) {
            return node.nodeName === '#text';
        },
        ':tag': function (node) {
            return node.tagName;
        },
        ':odd': function (node) {
            if (node.tagName) {
                flipflop = !flipflop;
                return flipflop;
            } else {
                return false;
            }
        },
        ':even': function (node) {
            var f;
            if (node.tagName) {
                f = flipflop;
                flipflop = !flipflop;
                return f;
            } else {
                return false;
            }
        }
    };

    function quest(query, nodes) {
        var selector, func, i, j, q0, r;

// Step through each selector.

        for (i = 0; i < query.length; i += 1) {
            selector = query[i];
            name = selector.name;
            func = hunter[selector.op];

// There are two kinds of selectors: hunters and peckers. If this is a hunter,
// loop through the the nodes, passing each node to the hunter function.
// Accumulate all the nodes it finds.

            if (typeof func === 'function') {
                if (star) {
                    throw new Error("ADsafe: Query violation: *" +
                            selector.op + (selector.name || ''));
                }
                result = [];
                for (j = 0; j < nodes.length; j += 1) {
                    func(nodes[j]);
                }
            } else {

// If this is a pecker, get its function. There is a special case for
// the :first and :rest selectors because they are so simple.

                value = selector.value;
                flipflop = false;
                func = pecker[selector.op];
                if (typeof func !== 'function') {
                    switch (selector.op) {
                    case ':first':
                        result = [nodes[0]];
                        break;
                    case ':rest':
                        result = nodes.slice(1);
                        break;
                    default:
                        throw new Error('ADsafe: Query violation: :' + selector.op);
                    }
                } else {

// For the other selectors, make an array of nodes that are filtered by
// the pecker function.

                    result = nodes.filter(func);
                }
            }
            nodes = result;
        }
        return result;
    }

    function make_root(dom, id) {

// A Bunch is a container that holds zero or more dom nodes.
// It has many useful methods.

        function Bunch(nodes) {
            this.___nodes___ = nodes;
            this.___star___ = star && nodes.length > 1;
            star = false;
        }

        var random = 0xADFACEC0,
            do_event = function (e) {
                var the_event,
                    the_target,
                    the_actual_event = e || event,
                    type = the_actual_event.type,
                    bec = browser_event_class[type],
                    bubble = bec !== 'still';
                allow_focus =
                    (bec === 'still' && type !== 'blur') || bec === 'sparkling';
                the_target =
                    the_actual_event.target || the_actual_event.srcElement;
                the_event = {
                    altKey: e.altKey,
                    ctrlKey: e.ctrlKey,
                    shiftKey: e.shiftKey,
                    charCode: e.charCode,
                    keyCode: e.keyCode,
                    preventDefault: function () {
                        if (the_actual_event.preventDefault) {
                            the_actual_event.preventDefault();
                        }
                        the_actual_event.returnValue = false;
                    },
                    stopPropagation: function () {
                        if (the_actual_event.stopPropagation) {
                            the_actual_event.stopPropagation();
                        }
                        the_actual_event.cancelBubble = true;
                    },
                    target: new Bunch([the_target]),
                    type: type
                };
                for (;;) {
                    if (the_target['___adsafe root___']) {
                        if (the_actual_event.stopPropagation) {
                            the_actual_event.stopPropagation();
                        }
                        the_actual_event.cancelBubble = true;
                        return;
                    }
                    if (the_target.on && the_target.on[the_event.type]) {
                        var b = new Bunch([the_target]);
                        b.fire(the_event);
                        if (!bubble || the_actual_event.cancelBubble === true) {
                            break;
                        }
                    }
                    the_target = the_target.parentNode;
                }
                return;
            },
            generic_handler = function (e) {
                try {
                    do_event(e);
                } catch (ex) {
                    allow_focus = false;
                }
                allow_focus = false;
            },
            keypress_handler  = function (e) {
                try {
                    e = e || event;
                    do_event(e);
                    if (e.returnValue !== false && e.charCode === 27) {
                        dom.ephemeral();
                    }
                } catch (ex) {
                    allow_focus = false;
                }
                allow_focus = false;
            };
        if (dom.tagName !== 'DIV') {
            error('ADsafe: Bad node.');
        }

// Mark the node as a root. This prevents event bubbling from propogating
// past it.

        dom['___adsafe root___'] = '___adsafe root___';

        Bunch.prototype = {
            check: function (check) {
                if (this === this.window) {
                    throw new Error('ADsafe error.');
                }
                var b = this.___nodes___, i, node;
                if (value instanceof Array) {
                    if (value.length !== b.length) {
                        error('ADsafe: Array length: ' +
                                b.length + '-' + value.length);
                    }
                    for (i = 0; i < b.length; i += 1) {
                        node = b[i];
                        if (node.tagName) {
                            node.checked = value[i];
                        }
                    }
                } else {
                    for (i = 0; i < b.length; i += 1) {
                        node = b[i];
                        if (node.tagName) {
                            node.checked = value;
                        }
                    }
                }
                return this;
            },
            count: function () {
                return this.___nodes___.length;
            },
            empty: function () {
                if (this === this.window) {
                    error('ADsafe error.');
                }
                var b = this.___nodes___, i, node;
                if (value instanceof Array) {
                    if (value.length !== b.length) {
                        error('ADsafe: Array length: ' +
                                b.length + '-' + value.length);
                    }
                    for (i = 0; i < b.length; i += 1) {
                        node = b[i];
                        while (node.firstChild) {
                            purge_event_handlers(node);
                            node.removeChild(node.firstChild);
                        }
                    }
                } else {
                    for (i = 0; i < b.length; i += 1) {
                        node = b[i];
                        while (node.firstChild) {
                            purge_event_handlers(node);
                            node.removeChild(node.firstChild);
                        }
                    }
                }
                return this;
            },
            enable: function (enable) {
                if (this === this.window) {
                    error('ADsafe error.');
                }
                var b = this.___nodes___, i, node;
                if (enable instanceof Array) {
                    if (enable.length !== b.length) {
                        error('ADsafe: Array length: ' +
                                b.length + '-' + enable.length);
                    }
                    for (i = 0; i < b.length; i += 1) {
                        node = b[i];
                        if (node.tagName) {
                            node.disabled = !enable[i];
                        }
                    }
                } else {
                    for (i = 0; i < b.length; i += 1) {
                        node = b[i];
                        if (node.tagName) {
                            node.disabled = !enable;
                        }
                    }
                }
                return this;
            },
            explode: function () {
                var a = [], b = this.___nodes___, i, node;
                for (i = 0; i < b.length; i += 1) {
                    a[i] = new Bunch(b[i]);
                }
                return a;
            },
            fire: function (event) {

    // Fire an event on an object. The event can be either
    // a string containing the name of the event, or an
    // object containing a type property containing the
    // name of the event. Handlers registered by the 'on'
    // method that match the event name will be invoked.

                var array,
                    b,
                    func,
                    i,
                    j,
                    n,
                    node,
                    on,
                    type;

                if (typeof event === 'string') {
                    type = event;
                    event = {type: type};
                } else if (typeof event === 'object') {
                    type = event.type;
                } else {
                    error();
                }
                b = this.___nodes___;
                n = b.length;
                for (i = 0; i < n; i += 1) {
                    node = b[i];
                    on = node.on;

    // If an array of handlers exist for this event, then
    // loop through it and execute the handlers in order.

                    if (on.hasOwnProperty(type)) {
                        array = on[type];
                        for (j = 0; j < array.length; j += 1) {

    // Invoke a handler. Pass the event object.

                            array[j].call(event, event);
                        }
                    }
                }
                return this;
            },
            focus: function () {
                var b = this.___nodes___;
                if (b.length !== 1 || !allow_focus) {
                    error();
                }
                b[0].focus();
            },
            getCheck: function () {
                var a = [], b = this.___nodes___, i;
                for (i = 0; i < b.length; i += 1) {
                    a[i] = b[i].checked;
                }
                return a.length === 1 ? a[0] : a;
            },
            getClass: function () {
                var a = [], b = this.___nodes___, i;
                for (i = 0; i < b.length; i += 1) {
                    a[i] = b[i].className;
                }
                return a.length === 1 ? a[0] : a;
            },
            getMark: function () {
                var a = [], b = this.___nodes___, i;
                for (i = 0; i < b.length; i += 1) {
                    a[i] = b[i]['_adsafe mark_'];
                }
                return a.length === 1 ? a[0] : a;
            },
            getName: function () {
                var a = [], b = this.___nodes___, i;
                for (i = 0; i < b.length; i += 1) {
                    a[i] = b[i].name;
                }
                return a.length === 1 ? a[0] : a;
            },
            getStyle: function (name) {
                var a = [], b = this.___nodes___, i, node;
                for (i = 0; i < b.length; i += 1) {
                    node = b[i];
                    if (node.tagName) {
                        a[i] = getStyleObject(node)[name];
                    }
                }
                return a.length === 1 ? a[0] : a;
            },
            getTagName: function () {
                var a = [], b = this.___nodes___, i, name;
                for (i = 0; i < b.length; i += 1) {
                    name = b[i].tagName;
                    a[i] = typeof name === 'string' ? name.toLowerCase() : name;
                }
                return a.length === 1 ? a[0] : a;
            },
            getValue: function () {
                var a = [], b = this.___nodes___, i, node;
                for (i = 0; i < b.length; i += 1) {
                    node = b[i];
                    if (node.nodeName === '#text') {
                        a[i] = node.nodeValue;
                    } else if (node.tagName) {
                        a[i] = node.value;
                    }
                }
                return a.length === 1 ? a[0] : a;
            },
            mark: function (value) {
                if (this === this.window || /url/i.test(value)) {
                    error('ADsafe error.');
                }
                var b = this.___nodes___, i, node;
                if (value instanceof Array) {
                    if (value.length !== b.length) {
                        error('ADsafe: Array length: ' +
                                b.length + '-' + value.length);
                    }
                    for (i = 0; i < b.length; i += 1) {
                        node = b[i];
                        if (node.tagName) {
                            node['_adsafe mark_'] = value[i];
                        }
                    }
                } else {
                    for (i = 0; i < b.length; i += 1) {
                        node = b[i];
                        if (node.tagName) {
                            node['_adsafe mark_'] = value;
                        }
                    }
                }
                return this;
            },
            on: function (type, func) {
                if (typeof type !== 'string' || typeof func !== 'function') {
                    error();
                }

// The bunch must contain exactly one node.
// I can't make sense of the other cases.

                var b = this.___nodes___,
                    ontype = 'on' + type;
                if (b.length !== 1) {
                    error();
                }

// Some events do not propogate, so for those I have to put the master event
// handler on the instances. These include 'change', 'focus', and 'blur'.

                switch (browser_event_class[type]) {
                case 'still':
                    if (b[0][ontype] !== generic_handler) {
                        b[0][ontype] = generic_handler;
                    }
                    break;
                case 'bubbling':
                    if (dom[ontype] !== generic_handler) {
                        dom[ontype] = generic_handler;
                    }
                    break;
                case 'sparkling':
                    if (type === 'keypress') {
                        if (dom.onkeypress !== keypress_handler) {
                            dom.onkeypress = keypress_handler;
                        }
                    } else if (dom[ontype] !== generic_handler) {
                        dom[ontype] = generic_handler;
                    }
                    break;
                }

// Register an event. Put the function in a handler array, making one if it
// doesn't yet exist for this type.

                var on = b[0].on;
                if (!on) {
                    on = {};
                    b[0].on = on;
                }
                if (on.hasOwnProperty(type)) {
                    on[type].push(func);
                } else {
                    on[type] = [func];
                }
                return this;
            },
            q: function (text) {
                star = this.___star___;
                return new Bunch(quest(parse_query(text, id), this.___nodes___));
            },
            remove: function () {
                this.replace();
            },
            replace: function (replacement) {
                var b = this.___nodes___, flag = false, i, j, newnode, node, parent, rep;
                if (b.length === 0) {
                    return;
                }
                purge_event_handlers(b);
                if (!replacement ||
                        replacement.length === 0 ||
                        replacement.___nodes___.length === 0) {
                    for (i = 0; i < b.length; i += 1) {
                        node = b[i];
                        purge_event_handlers(node);
                        if (node.parentNode) {
                            node.parentNode.removeChild(node);
                        }
                    }
                } else if (replacement instanceof Array) {
                    if (replacement.length !== b.length) {
                        throw new Error('ADsafe: Array length: ' +
                                b.length + '-' + value.length);
                    }
                    for (i = 0; i < b.length; i += 1) {
                        node = b[i];
                        parent = node.parentNode;
                        purge_event_handlers(node);
                        if (parent) {
                            rep = replacement[i].___nodes___;
                            if (rep.length > 0) {
                                newnode = rep[0];
                                parent.replaceNode(newnode);
                                for (j = 1; j < rep.length; j += 1) {
                                    node = newnode;
                                    newnode = rep[j];
                                    parent.insertBefore(newnode, node.nextSibling);
                                }
                            } else {
                                parent.removeChild(node);
                            }
                        }
                    }
                } else {
                    rep = replacement[i].___nodes___;
                    for (i = 0; i < b.length; i += 1) {
                        node = b[i];
                        purge_event_handlers(node);
                        if (node.parentNode) {
                            newnode = flag ? rep[0].cloneNode(true) : rep[0];
                            parent.replaceNode(newnode);
                            for (j = 1; j < rep.length; j += 1) {
                                node = newnode;
                                newnode = flag ? rep[j].clone(true) : rep[j];
                                parent.insertBefore(newnode, node.nextSibling);
                            }
                            flag = true;
                        }
                    }
                }
                return this;
            },
            select: function () {
                var b = this.___nodes___;
                if (b.length !== 1 || !allow_focus) {
                    error();
                }
                b[0].focus();
                b[0].select();
            },
            style: function (name, value) {
                if (this === this.window || /url/i.test(value)) {
                    throw new Error('ADsafe error.');
                }
                var b = this.___nodes___, i, node;
                if (value instanceof Array) {
                    if (value.length !== b.length) {
                        throw new Error('ADsafe: Array length: ' +
                                b.length + '-' + value.length);
                    }
                    for (i = 0; i < b.length; i += 1) {
                        node = b[i];
                        if (node.tagName) {
                            node.style[name] = value[i];
                        }
                    }
                } else {
                    for (i = 0; i < b.length; i += 1) {
                        node = b[i];
                        if (node.tagName) {
                            node.style[name] = value;
                        }
                    }
                }
                return this;
            },
            value: function (value) {
                if (this === this.window) {
                    throw new Error('ADsafe error.');
                }
                var b = this.___nodes___, i, node;
                if (value instanceof Array) {
                    if (value.length !== b.length) {
                        throw new Error('ADsafe: Array length: ' +
                                b.length + '-' + value.length);
                    }
                    for (i = 0; i < b.length; i += 1) {
                        node = b[i];
                        if (node.tagName) {
                            if (typeof node.value === 'string') {
                                node.value = value[i];
                            } else {
                                while (node.firstChild) {
                                    purge_event_handlers(node);
                                    node.removeChild(node.firstChild);
                                }
                                node.appendChild(document.createTextNode(String(value[i])));
                            }
                        } else if (node.nodeName === '#text') {
                            node.nodeValue = value[i];
                        }
                    }
                } else {
                    for (i = 0; i < b.length; i += 1) {
                        node = b[i];
                        if (node.tagName) {
                            if (typeof node.value === 'string') {
                                node.value = value;
                            } else {
                                while (node.firstChild) {
                                    purge_event_handlers(node);
                                    node.removeChild(node.firstChild);
                                }
                                node.appendChild(document.createTextNode(String(value)));
                            }
                        } else if (node.nodeName === '#text') {
                            node.nodeValue = value;
                        }
                    }
                }
                return this;
            }
        };

// Return an ADsafe dom object.

        return {
            q: function (text) {
                star = false;
                var query = parse_query(text, id);
                if (typeof hunter[query[0].op] !== 'function') {
                    error('ADsafe: Bad query: ' + query[0]);
                }
                return new Bunch(quest(query, [dom]));
            },
            combine: function (array) {
                if (!array || !array.length) {
                    throw new Error('ADsafe: Bad combination.');
                }
                var b = array[0].___nodes___, i;
                for (i = i; i < array.length; i += 1) {
                    b = b.concat(array[i].___nodes___);
                }
                return new Bunch(b);
            },
            count: function () {
                return 1;
            },
            ephemeral: function (bunch) {
                if (ephemeral) {
                    ephemeral.remove();
                }
                ephemeral = bunch.___nodes___.length > 0 ? bunch : null;
            },
            random: function () {
                random ^= random << 1;
                random ^= random >> 3;
                random ^= random << 10;
                return (random + 2147483648) / 4294967296;
            },
            remove: function () {
                purge_event_handlers(dom);
                dom.parent.removeElement(dom);
                dom = null;
            },
            tag: function (tag, type, name) {
                var node;
                if (makeableTagName[tag] !== true) {
                    error('ADsafe: Bad tag: ' + tag);
                }
                node = document.createElement(tag);
                if (name) {
                    node.autocomplete = 'off';
                    node.name = name;
                }
                if (type) {
                    node.type = type;
                }
                return new Bunch(node);
            },
            text: function (text) {
                var a, i;
                if (text instanceof Array) {
                    a = [];
                    for (i = 0; i < text.length; i += 1) {
                        a[i] = document.createTextNode(String(text[i]));
                    }
                    return a;
                }
                return new Bunch(document.createTextNode(String(text)));
            },
            append: function (bunch) {
                var b = bunch.___nodes___, i;
                for (i = 0; i < b.length; i += 1) {
                    dom.appendChild(b[i]);
                }
                return dom;
            },
            prepend: function (bunch) {
                var b = bunch.___nodes___, i;
                for (i = 0; i < b.length; i += 1) {
                    dom.insertBefore(b[i], dom.firstChild);
                }
                return dom;
            },
            row: function (values) {
                return dom.tag('tr').append(
                    dom.tag('td')
                        .clone(false, value.length)
                        .append(dom.text(value)));
            }
        };
    }


//  Return the ADSAFE object.

    return {

//  ADSAFE.get retrieves a value from an object.

        get: function (object, name, name2) {
            var value;
            if (!reject(object, name)) {
                value = object[name];
            }
            if (name2 === undefined) {
                return value;
            }
            if (!reject(value, name2)) {
                return value[name2];
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

            try {
                allow_focus = true;
                f(make_root(dom, id), adsafe_lib);
            } catch (e) {
                allow_focus = false;
                error();
            }
            dom = null;
            adsafe_lib = null;
            allow_focus = false;
        }

    };
}();