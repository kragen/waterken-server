var Shell = function () {

    function outputText(text, className) {
        var outputPane = window.document.getElementById('outputPane');
        if (outputPane) {
            var echoPane = window.document.createElement('div');
            echoPane.className = className;
            echoPane.appendChild(window.document.createTextNode(text));
            outputPane.appendChild(echoPane);
        }
    }

    return {
        current: '',

        onPageLoad: function() {
            window.document.body.innerHTML =
                '<div id="outputPane">' +
                '<p>This page is a Javascript shell. You can type Javascript code at the bottom ' +
                'of the page and it will be evaluated in the context of this page when you hit ' +
                '<kbd>ENTER</kbd>. A multi-line Javascript statement can be entered by typing ' +
                '<kbd>SHIFT-ENTER</kbd> between each line. You can print a text string by ' +
                'calling <code>Shell.echo()</code>. For example:</p>' +
                '<div class="echo">Shell.echo("Hello World!")</div>' +
                '<div class="stdout">Hello World!</div>' +
                '<p>You can get a remote reference to the server-side object referenced by this ' +
                'page\'s URL with the command:</p>' +
                '<div class="echo">var page_ = _.connect(window.location.toString())</div>' +
                '<p>You can get the list of methods implemented by the ' +
                'server-side object with the command:</p>' +
                '<div class="echo">var meta_ = page_.get("class").get("*");\n' +
                'meta_</div>' +
                '<div class="fulfilled"><i>&hellip;</i></div>' +
                '</div>' +
                '<textarea id="inputPane" wrap="off" rows="10" onkeypress="Shell.onInputKeydown(event)"></textarea>';

            var inputPane = window.document.getElementById('inputPane');
            inputPane.focus();
        },

        onInputKeydown: function (x) {
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
                    Shell.current = inputPane.value;
                    inputPane.value = '';
                    outputText(Shell.current, 'echo');
                    window.location.href = "javascript:try{Shell.output(eval(Shell.current));} catch (e) {Shell.error(e);}; void 0";
                    setTimeout(function () {
                        inputPane.value = '';
                        inputPane.setAttribute('rows', '10');
                    }, 0);
                }
            }
        },

        echo: function (text) {
            outputText(text, 'stdout');
        },

        output: function (result) {
            if (undefined !== result) {
                outputText(JSON.stringify(result, undefined, ' '), 'fulfilled');
            }
        },

        error: function (e) {
            if (e.message) {
                outputText(e.message, 'rejected');
            } else {
                outputText(e.toString(), 'rejected');
            }
        }
    };
} ();

