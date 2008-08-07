function _Page_() {}
_Page_.onLoad  = function () {
    var inputPane = window.document.getElementById('inputPane');
    inputPane.focus();
};
_Page_.onInputKeydown = function (x) {
    var outputPane = window.document.getElementById('outputPane');
    var inputPane = window.document.getElementById('inputPane');
    if (x.shiftKey) {
        if (13 === x.keyCode) {
            var pszRows = inputPane.getAttribute('rows');
            var nPlusRows = parseInt(pszRows) + 1;
            inputPane.setAttribute('rows', '' + nPlusRows);
        }
    } else {
        if (13 === x.keyCode) {
            _Page_.current = inputPane.value;
            inputPane.value = '';
            _Page_.outputText(_Page_.current, 'echo');
            window.location.href = 'javascript:' +
                'try {' +
                    '_Page_.output(eval(_Page_.current));' +
                '} catch (e) {' +
                    '_Page_.error(e);' +
                '};' +
                'void 0';
            setTimeout(function () {
                inputPane.value = '';
                inputPane.setAttribute('rows', '10');
            }, 0);
        }
    }
};
_Page_.current = '';
_Page_.output = function (result) {
    if (undefined !== result) {
        _Page_.outputText(JSON.stringify(result, function (key, value) {
            return 'number' === typeof value && !isFinite(value)
                ? {
                    $: [ 'org.ref_send.promise.Rejected' ],
                    reason: { $: [ 'NaN' ] }
                }
            : value;
        }, ' '), 'fulfilled');
    }
};
_Page_.error = function (e) {
    if (e.message) {
        _Page_.outputText(e.message, 'rejected');
    } else {
        _Page_.outputText(e.toString(), 'rejected');
    }
};
_Page_.outputText = function (text, className) {
    var outputPane = window.document.getElementById('outputPane');
    if (outputPane) {
        var echoPane = window.document.createElement('div');
        echoPane.className = className;
        echoPane.appendChild(window.document.createTextNode(text));
        outputPane.appendChild(echoPane);
    }
};
function echo(text) {
    _Page_.outputText(text, 'stdout');
};
