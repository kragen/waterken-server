#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    CLASSPATH='..\joe-e\bin;..\ref_send\bin;..\network\bin;..\web_send\bin;..\shared\bin;..\log\bin;..\persistence\bin;..\remote\bin;..\dns\bin;..\server\bin'
else
    CLASSPATH='../joe-e/bin:../ref_send/bin:../network/bin:../web_send/bin:../shared/bin:../log/bin:../persistence/bin:../remote/bin:../dns/bin:../server/bin'
fi

echo 'Building test project...'
rm -rf bin/
mkdir -p bin
javac -classpath $CLASSPATH -d bin/ `find src/ -name '*.java'` $@
./jars.sh
