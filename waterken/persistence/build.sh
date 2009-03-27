#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    CLASSPATH='..\joe-e\bin;..\ref_send\bin;..\log\bin;..\network\bin'
else
    CLASSPATH='../joe-e/bin:../ref_send/bin:../log/bin:../network/bin'
fi

echo 'Building persistence project...'
rm -rf bin/
mkdir -p bin
javac -classpath $CLASSPATH -d bin/ `find src/ -name '*.java'` $@
./jars.sh
