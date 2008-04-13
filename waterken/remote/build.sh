#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    CLASSPATH='..\joe-e\bin;..\ref_send\bin;..\web_send\bin;..\network\bin;..\shared\bin;..\persistence\bin'
else
    CLASSPATH='../joe-e/bin:../ref_send/bin:../web_send/bin:../network/bin:../shared/bin:../persistence/bin'
fi

echo 'Building remote project...'
rm -rf bin/
mkdir -p bin
javac -classpath $CLASSPATH -d bin/ `find src/ -name '*.java'` $@
