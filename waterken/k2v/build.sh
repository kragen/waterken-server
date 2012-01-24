#!/bin/sh

echo 'Building k2v project...'
rm -rf bin/
mkdir -p bin
javac -d bin/ `find src/ -name '*.java'` $@
