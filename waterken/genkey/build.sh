#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    CLASSPATH='..\joe-e\bin;..\ref_send\bin;..\network\bin;..\log\bin;..\persistence\bin;..\syntax\bin;..\remote\bin;..\shared\bin;..\dns\bin'
else
    CLASSPATH='../joe-e/bin:../ref_send/bin:../network/bin:../log/bin:../persistence/bin:../syntax/bin:../remote/bin:../shared/bin:../dns/bin'
fi

echo 'Building genkey project...'
rm -rf bin/
mkdir -p bin
javac -classpath $CLASSPATH -d bin/ `find src/ -name '*.java'` $@
./jars.sh
