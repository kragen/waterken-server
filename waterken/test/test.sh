#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    CLASSPATH='..\joe-e\bin;..\ref_send\bin;..\web_send\bin;..\network\bin;bin'
else
    CLASSPATH='../joe-e/bin:../ref_send/bin:../web_send/bin:../network/bin:bin'
fi

echo 'Testing network project...'
java -classpath $CLASSPATH org.waterken.test.uri.Main
