#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    SRC='joe-e\src;ref_send\src;web_send\src;example\src'
else
    SRC='joe-e/src:ref_send/src:web_send/src:example/src'
fi
if [ "$1" = 'ref_send' ]
then
    NAME="ref_send"
    VERSION="1.7"
else
    NAME="waterken server"
    MAJOR=4
    MINOR=`cat minor-version.txt`
    VERSION="$MAJOR.$MINOR"
    if [ "$OS" = 'Windows_NT' ]
    then
        SRC="$SRC;network\src;src;persistence\src;remote\src;dns\src;server\src"
    else
        SRC="$SRC:network/src:src:persistence/src:remote/src:dns/src:server/src"
    fi
fi
SLOGAN='defensive programming in Java'
BOTTOM='<a href="https://lists.sourceforge.net/lists/listinfo/waterken-server">Submit a bug or feature, or get help</a><p>Copyright 1998-2007 Waterken Inc. under the terms of the <a href="http://www.opensource.org/licenses/mit-license.html">MIT X license</a>.</p>'

rm -rf javadoc/
mkdir -p javadoc
javadoc \
    -sourcepath $SRC \
    -d 'javadoc' \
    -doctitle "$NAME API $VERSION" \
    -windowtitle "$NAME API" \
    -header "$NAME API $VERSION <br> $SLOGAN" \
    -footer "$NAME API $VERSION <br> $SLOGAN" \
    -bottom "$BOTTOM" \
    -quiet \
    -public \
    -linksource \
    -use \
    -group 'Joe-E Language' 'org.joe_e' \
    -group 'Pass-By-Construction' 'org.ref_send' \
    -group 'Eventual Control Flow' 'org.ref_send.promise*' \
    -group 'Example Applications' \
        'org.waterken.all:org.waterken.bang:org.waterken.bounce:org.waterken.eq:org.waterken.factorial:org.waterken.put:org.waterken.serial:org.ref_send.test' \
    -group 'Collection' 'org.joe_e.array:org.ref_send.list' \
    -group 'Reflection' 'org.joe_e.reflect:org.ref_send.type' \
    -group 'I/O' 'org.joe_e.charset:org.joe_e.file' \
    -group 'Web Datatypes' 'org.web_send*' \
    -group 'Orthogonal Persistence' \
           'org.waterken.model:org.waterken.jos:org.waterken.thread' \
    -group 'Networking' 'org.waterken.*' \
    -subpackages 'org'
