#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    CLASSPATH='..\joe-e\bin;..\ref_send\bin;..\shared\bin;bin'
else
    CLASSPATH='../joe-e/bin:../ref_send/bin:../shared/bin:bin'
fi

echo 'Testing example project...'
java -classpath $CLASSPATH org.waterken.all.All
java -classpath $CLASSPATH org.waterken.factorial.FactorialN 4
