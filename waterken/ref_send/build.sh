#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    CLASSPATH='..\joe-e\bin'
else
    CLASSPATH='../joe-e/bin'
fi

echo 'Building ref_send project...'
rm -rf bin/
mkdir -p bin
javac -classpath $CLASSPATH -d bin/ `find src/ -name '*.java'` $@
