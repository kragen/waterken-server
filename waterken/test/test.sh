#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    CLASSPATH='..\joe-e\bin;..\ref_send\bin;..\network\bin;..\log\bin;..\persistence\bin;bin'
else
    CLASSPATH='../joe-e/bin:../ref_send/bin:../network/bin:../log/bin:../persistence/bin:bin'
fi

echo 'Testing org.waterken.uri...'
java -classpath $CLASSPATH org.waterken.test.uri.Main
