#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    CLASSPATH='..\joe-e\bin;..\ref_send\bin;..\web_send\bin;..\shared\bin;bin'
else
    CLASSPATH='../joe-e/bin:../ref_send/bin:../web_send/bin:../shared/bin:bin'
fi

echo 'Testing example project...'
java -classpath $CLASSPATH org.waterken.bang.Main
java -classpath $CLASSPATH org.waterken.bounce.Main
java -classpath $CLASSPATH org.waterken.eq.Main
java -classpath $CLASSPATH org.waterken.factorial.Main
java -classpath $CLASSPATH org.waterken.serial.Main
