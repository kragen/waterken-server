#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    SRC='k2v\src'
    OVERVIEW='k2v\src\overview.html'
else
    SRC='k2v/src'
    OVERVIEW='k2v/src/overview.html'
fi
NAME="k2v"
MAJOR=1
MINOR=0
VERSION="$MAJOR.$MINOR"
SLOGAN='persistent, in process, hierarchical, key to value map'
BOTTOM='<a href="https://lists.sourceforge.net/lists/listinfo/waterken-server">Submit a bug or feature, or get help</a><p>Copyright 1998-2012 Waterken Inc. under the terms of the <a href="http://www.opensource.org/licenses/mit-license.html">MIT X license</a>.</p>'

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
    -group 'API' 'org.waterken.k2v' \
    -subpackages 'org' $@)
