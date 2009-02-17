#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    CLASSPATH='..\joe-e\bin;..\ref_send\bin;..\log\bin;..\persistence\bin;..\network\bin;..\shared\bin'
else
    CLASSPATH='../joe-e/bin:../ref_send/bin:../log/bin:../persistence/bin:../network/bin:../shared/bin'
fi

echo 'Building dns project...'
rm -rf bin/
mkdir -p bin
javac -classpath $CLASSPATH -d bin/ `find src/ -name '*.java'` $@
