#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    SRC='joe-e\src;ref_send\src;shared\src;example\src;log\src;syntax\src'
    OVERVIEW='ref_send\src\overview.html'
else
    SRC='joe-e/src:ref_send/src:shared/src:example/src:log/src:syntax/src'
    OVERVIEW='ref_send/src/overview.html'
fi
NAME="ref_send"
MINOR=`cat minor-version.txt`
SLOGAN='defensive programming in Java'
BOTTOM='<a href="https://lists.sourceforge.net/lists/listinfo/waterken-server">Submit a bug or feature, or get help</a><p>Copyright 1998-2007 Waterken Inc. under the terms of the <a href="http://www.opensource.org/licenses/mit-license.html">MIT X license</a>.</p>'

(cd ..; rm -rf javadoc/; mkdir -p javadoc; javadoc \
    -sourcepath $SRC \
    -d 'javadoc' \
    -doctitle "$NAME API $VERSION" \
    -windowtitle "$NAME API" \
    -header "$NAME API $VERSION <br> $SLOGAN" \
    -footer "$NAME API $VERSION <br> $SLOGAN" \
    -bottom "$BOTTOM" \
    -overview "$OVERVIEW" \
    -quiet \
    -public \
    -linksource \
    -use \
    -group 'Core API' 'org.joe_e:org.ref_send:org.ref_send.promise' \
    -group 'Example Applications' 'org.ref_send.test:org.waterken.*' \
    -group 'Collection' 'org.joe_e.array:org.joe_e.var:org.ref_send.list:org.ref_send.scope' \
    -group 'Reflection' 'org.joe_e.reflect:org.joe_e.taming:org.ref_send.type' \
    -group 'I/O' 'org.joe_e.charset:org.joe_e.file' \
    -group 'Logging' 'org.ref_send.log:org.waterken.trace*' \
    -group 'JSON' 'org.waterken.syntax*' \
    -subpackages 'org' $@)
