#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    CLASSPATH='..\joe-e\bin;..\ref_send\bin;bin'
else
    CLASSPATH='../joe-e/bin:../ref_send/bin:bin'
fi

echo 'Testing network project...'
java -classpath $CLASSPATH org.waterken.uri.Main
