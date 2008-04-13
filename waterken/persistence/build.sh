#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    CLASSPATH='..\joe-e\bin;..\ref_send\bin;..\web_send\bin;..\log\bin'
else
    CLASSPATH='../joe-e/bin:../ref_send/bin:../web_send/bin:../log/bin'
fi

echo 'Building persistence project...'
rm -rf bin/
mkdir -p bin
javac -classpath $CLASSPATH -d bin/ `find src/ -name '*.java'` $@
./jars.sh
