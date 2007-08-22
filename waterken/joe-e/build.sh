#!/bin/sh

echo 'Building joe-e project...'
rm -rf bin/*
javac -d bin/ `find src/ -name '*.java'` $@
