// dom.js
// 2008-07-19

/*global ADSAFE */

/*jslint nomen: false, browser: true */

/*members "", "#", "&", "*", "+", ".", "/", ":checked",
    ":disabled", ":enabled", ":even", ":hidden", ":odd", ":tag", ":text",
    ":unchecked", ":visible", ">", "[", "[!=", "[$=", "[*=", "[=", "[^=",
    "[~=", ___dom___, ___nodes___, ___star___, "_adsafe mark_", a, abbr,
    acronym, address, append, appendChild, area, b, bdo, big, blockquote,
    br, button, canvas, caption, center, check, checked, childNodes, cite,
    className, clone, cloneNode, code, col, colgroup, combine, concat,
    count, createElement, createTextNode, currentStyle, dd, defaultView,
    del, dfn, dir, disabled, div, dl, dt, em, empty, enable, ephemeral,
    exec, explode, fieldset, filter, firstChild, font, form, getCheck,
    getClass, getComputedStyle, getElementById, getElementsByTagName,
    getMark, getName, getStyle, getTagName, getValue, h1, h2, h3, h4, h5,
    h6, hr, i, img, indexOf, input, ins, insertBefore, kbd, label, legend,
    length, li, map, mark, menu, name, nextSibling, nodeName, nodeValue,
    object, ol, on, op, optgroup, option, p, parentNode, pre, prepend,
    prototype, push, q, remove, removeChild, replace, replaceNode, row,
    samp, select, slice, small, span, strong, style, sub, sup, table, tag,
    tagName, tbody, td, test, text, textarea, tfoot, th, thead, toLowerCase,
    toUpperCase, tr, tt, type, u, ul, value, var, visibility, window
*/


ADSAFE.___dom___ = function () {

    var tagName = {

// These are elements that may be created with the .tag(tagName) method.

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

    function walkTheDOM(node, func, skip) {

// Recursively traverse the DOM tree, starting with the node, in document
// order, calling the func on each node visisted.

        if (skip) {
            func(node);
        }
        node = node.firstChild;
        while (node) {
            walkTheDOM(node, func);
            node = node.nextSibling;
        }
    }

    function purgeAllEventHandlers(node) {
        walkTheDOM(node, function (node) {
            if (node.on) {
                node.on = null;
            }
        });
    }


    var defaultView = document.defaultView,
        name,
        value,
        flipflop,
        result,
        star;


    function parse_query(text, id) {

// Convert a query string into an array of op/name/value selectors.
// A query string is a sequence of triples wrapped in brackets; or names,
// possibly prefixed by # . & > +, or :option, or * or /. A triple is a name,
// and operator (one of [=, [!=, [*=, [~=, [$=, or [^=) and a value.

// A name must be all lower case and may contain digits, -, or _.

        var match,          // A match array
            query = [],     // The resulting query array
            selector,
            qx = /^\s*(?:([\*\/])|\[\s*([a-z][0-9a-z_\-]*)\s*(?:([!*~$\^]?\=)\s*([0-9A-Za-z_\-*%&;.\/:!]+)\s*)?\]|#\s*([A-Z]+_[A-Z0-9_]+)|:\s*([a-z]+)|([.&>+])\s*([a-z][0-9a-z_\-]*)|([a-z][0-9a-z_\-]*))\s*/;

// Loop over all of the selectors in the text.

        do {

// The qx teases the components of one selector out of the text, ignoring
// whitespace.

//          match[0]  the whole selector
//          match[1]  * /
//          match[2]  attribute name
//          match[3]  = != *= ~= $= ^=
//          match[4]  attribute value
//          match[5]  id
//          match[6]  option
//          match[7]  . & > +
//          match[8]  name
//          match[9]  name

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
                    throw new Error("ADsafe: Bad query: " + text);
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

// The selector is one of the > . & +.

            } else if (match[7]) {
                selector = {
                    op: match[7],
                    name: match[8]
                };

// The selector is a tag name.

            } else {
                selector = {
                    op: '',
                    name: match[9]
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

    function getStyle(node) {

// getStyle is a function that returns the computed style object for a node.

        return node.currentStyle || defaultView.getComputedStyle(node);
    }


    var pecker = {
        '.': function (node) {
            return (' ' + node.className + ' ').indexOf(' ' + name + ' ') >= 0;
        },
        '&': function (node) {
            return node.name === name;
        },
        '+': function (node) {
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
            return node.tagName && getStyle(node).visibility === 'visible';
        },
        ':hidden': function (node) {
            return node.tagName && getStyle(node).visibility !== 'visible';
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

    function Bunch(nodes) {
        this.___nodes___ = nodes;
        this.___star___ = star;
    }

    Bunch.prototype = {
        check: function (check) {
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
                    while (node.firstChild) {
                        purgeAllEventHandlers(node);
                        node.removeChild(node.firstChild);
                    }
                }
            } else {
                for (i = 0; i < b.length; i += 1) {
                    node = b[i];
                    while (node.firstChild) {
                        purgeAllEventHandlers(node);
                        node.removeChild(node.firstChild);
                    }
                }
            }
            return this;
        },
        enable: function (enable) {
            if (this === this.window) {
                throw new Error('ADsafe error.');
            }
            var b = this.___nodes___, i, node;
            if (enable instanceof Array) {
                if (enable.length !== b.length) {
                    throw new Error('ADsafe: Array length: ' +
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
                    a[i] = getStyle(node)[name];
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
        q: function (text) {
            star = this.___star___;
            return new Bunch(quest(parse_query(text), this.___nodes___));
        },
        remove: function () {
            this.replace();
        },
        replace: function (replacement) {
            var b = this.___nodes___, flag = false, i, j, newnode, node, parent, rep;
            if (b.length === 0) {
                return;
            }
            purgeAllEventHandlers(b);
            if (!replacement ||
                    replacement.length === 0 ||
                    replacement.___nodes___.length === 0) {
                for (i = 0; i < b.length; i += 1) {
                    node = b[i];
                    purgeAllEventHandlers(node);
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
                    purgeAllEventHandlers(node);
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
                    purgeAllEventHandlers(node);
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
        value: function (check) {
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
                        node.value = value[i];
                    } else if (node.nodeName === '#text') {
                        node.nodeValue = value[i];
                    }
                }
            } else {
                for (i = 0; i < b.length; i += 1) {
                    node = b[i];
                    if (node.tagName) {
                        node.value = value;
                    } else if (node.nodeName === '#text') {
                        node.nodeValue = value;
                    }
                }
            }
            return this;
        }
    };


    return function (dom, id) {
        var ephemeral;
        if (dom.tagName !== 'DIV') {
            throw new Error('ADsafe: Bad node.');
        }
        return {
            q: function (text) {
                star = false;
                var query = parse_query(text, id);
                if (typeof hunter[query[0].op] !== 'function') {
                    throw new Error('ADsafe: Bad query: ' + query[0]);
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
                ephemeral = bunch.___nodes___.length ? bunch : null;
            },
            tag: function (tag, type, name) {
                var node;
                if (tagName[tag] !== true) {
                    throw new Error('ADsafe: Bad tag: ' + tag);
                }
                node = document.createElement(tag);
                if (type) {
                    node.type = type;
                }
                if (name) {
                    node.name = name;
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
    };
}();