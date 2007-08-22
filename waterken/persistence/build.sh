#!/bin/sh
if [ "$OS" = 'Windows_NT' ]
then
    CLASSPATH='..\joe-e\bin;..\ref_send\bin;..\web_send\bin'
else
    CLASSPATH='../joe-e/bin:../ref_send/bin:../web_send/bin'
fi

echo 'Building persistence project...'
rm -rf bin/*
javac -classpath $CLASSPATH -d bin/ `find src/ -name '*.java'` $@
jar cmf REPORT.MF ../report.jar X.class
