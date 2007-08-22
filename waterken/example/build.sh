#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    CLASSPATH='..\joe-e\bin;..\ref_send\bin'
else
    CLASSPATH='../joe-e/bin:../ref_send/bin'
fi

echo 'Building example project...'
rm -rf bin/*
javac -classpath $CLASSPATH -d bin/ `find src/ -name '*.java'` $@
