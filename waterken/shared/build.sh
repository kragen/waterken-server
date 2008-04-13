#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    CLASSPATH='..\joe-e\bin;..\ref_send\bin;..\web_send\bin'
else
    CLASSPATH='../joe-e/bin:../ref_send/bin:../web_send/bin'
fi

echo 'Building shared project...'
rm -rf bin/
mkdir -p bin
javac -classpath $CLASSPATH -d bin/ `find src/ -name '*.java'` $@
