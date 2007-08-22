#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    CLASSPATH='..\joe-e\bin;..\ref_send\bin;..\web_send\bin;..\network\bin;..\persistence\bin'
else
    CLASSPATH='../joe-e/bin:../ref_send/bin:../web_send/bin:../network/bin:..\persistence\bin'
fi

echo 'Building remote project...'
rm -rf bin/*
javac -classpath $CLASSPATH -d bin/ `find src/ -name '*.java'` $@
