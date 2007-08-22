// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
var HTML = {
    renderers: {}   // [ typename => renderer ]
};
HTML.escapeBody = function (text) {
    return text.replace(/&/g, '&amp;').
                replace(/</g, '&lt;').
                replace(/>/g, '&gt;');
};
HTML.escapeAttribute = function (text) {
    return this.escapeBody(text).replace(/\"/g, "&quot;");
};
HTML.dispatch = function () {
    var value = arguments[0];

    var type = typeof value;
    var todo = [ type ];
    for (var i = arguments.length; 1 !== i--;) {
        todo.push(arguments[i]);
    }
    if ('object' === type) {
        var schemas = value.$;
        if (undefined !== schemas) {
            for (var j = schemas.length; 0 !== j--;) {
                todo.push(schemas[j]);
            }
        } else if (undefined !== value['@']) {
            todo.push('@');
        }
    }
    var renderer = undefined;
    while (undefined === renderer && 0 !== todo.length) {
        renderer = this.renderers[todo.pop()];
    }
    if (undefined === renderer) {
        renderer = {
            fill: function (value) {
                document.title = type;
                document.body.innerHTML = this.render(value);
            },
            render: function (value) {
                return '<pre>'+HTML.escapeBody(value.toJSONString())+'</pre>';
            }
        };
    }
    return renderer;
};
HTML.display = function () {
    var fill = function (value) { HTML.dispatch(value).fill(value); };
    _.connect(document.location.toString()).describe().when(fill, fill);
};
HTML.renderers['string'] = {
    fill: function (value) {
        document.title = '...';
        document.body.innerHTML = this.render(value);
    },
    render: function (value) { return HTML.escapeBody(value); }
};
HTML.renderers['number'] = {
    fill: function (value) {
        document.title = value;
        document.body.innerHTML = this.render(value);
    },
    render: function (value) { return value.toString(); }
};
HTML.renderers['boolean'] = {
    fill: function (value) {
        document.title = value;
        document.body.innerHTML = this.render(value);
    },
    render: function (value) { return value.toString(); }
};
HTML.renderers['@'] = {
    fill: function (value) {
        window.location = value['@'];
    },
    render: function (value) {
        return '<a href="' + value['@'] + '">link</a>';
    }
};
HTML.renderers['object'] = {
    fill: function (value) {
        var schemas = value.$;
        document.title = '<' + (schemas ? schemas[0] : 'object') + '>';
        document.body.innerHTML = this.render(value);
    },
    render: function (s) {
        var text = [];
        text.push('<table>');
        for (var p in s) {
            if (s.hasOwnProperty(p)) {
                var o = s[p];
                var href;
                if (null !== o && 'object' === typeof o) {
                    href = o['@'];
                }

                text.push('<tr>');
                text.push('<th>');
                if (undefined !== href) {
                    text.push('<a href="' + HTML.escapeAttribute(href) + '">' +
                              HTML.escapeBody('' + p) + '</a>');
                } else {
                    text.push(HTML.escapeBody('' + p));
                }
                text.push('</th>');
                text.push('<td>');
                if (undefined === href) {
                    text.push(HTML.dispatch(o).render(o));
                }
                text.push('</td>');
                text.push('</tr>');
            }
        }
        text.push('</table>');
        return text.join('');
    }
};
HTML.renderers['org.ref_send.promise.Fulfilled'] = {
    fill: function (value) {
        HTML.dispatch(value.value).fill(value.value);
    },
    render: function (value) {
        HTML.dispatch(value.value).render(value.value);
    }
};
HTML.renderers['org.ref_send.promise.Rejected'] = {
    fill: function (value) {
        HTML.dispatch(value.reason, 'Error').
            fill(value.reason);
    },
    render: function (value) {
        HTML.dispatch(value.reason, 'Error').
            render(value.reason);
    }
};
HTML.renderers['org.ref_send.promise.Indeterminate'] = {
    fill: function (value) {
        document.title = 'not yet';
        document.body.innerHTML = '<p>The answer isn\'t known yet. ' +
            'Refresh this page to check again.</p>';
    },
    render: function (value) { return 'indeterminate'; }
};
HTML.renderers['org.ref_send.test.Test'] = {
    fill: function (value) {
        document.title = 'test';
        document.body.innerHTML =
            '<form method="POST" action="' + document.location + '">' +
            '<input type="button" name="start" value="start" ' +
                'onClick="return HTML.submitFORM(this)"/>' +
            '</form>';
    }
};
HTML.submitFORM = function (button) {
    button.disabled = true;
    var s_ = _.connect(button.form.action);
    var p = button.name;
    var o_;
    if (/^post$/i.test(button.form.method)) {
        o_ = s_.post(p, []);
    } else if (/^get$/i.test(button.form.method)) {
        o_ = s_.get(p);
    } else {
        o_ = null;
    }
    o_.observers.push(function (value_) {
        HTML.dispatch(value_).fill(value_);
    });
    return false;
};
