#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    CLASSPATH='..\joe-e\bin;..\ref_send\bin;..\web_send\bin;..\shared\bin;..\persistence\bin;..\log\bin;..\network\bin'
else
    CLASSPATH='../joe-e/bin:../ref_send/bin:../web_send/bin:../shared/bin:../persistence/bin:../log/bin:../network/bin'
fi

echo 'Building dns project...'
rm -rf bin/
mkdir -p bin
javac -classpath $CLASSPATH -d bin/ `find src/ -name '*.java'` $@
