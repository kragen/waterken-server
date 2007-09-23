#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    CLASSPATH='..\joe-e\bin;..\ref_send\bin;..\web_send\bin;..\network\bin;..\persistence\bin;bin'
else
    CLASSPATH='../joe-e/bin:../ref_send/bin:../web_send/bin:../network/bin:../persistence/bin:bin'
fi

echo 'Testing remote project...'
java -classpath $CLASSPATH org.waterken.id.exports.Main
