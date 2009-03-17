@echo off
echo 'Building shared project...'
mkdir bin
javac -d bin ^
  -classpath "..\joe-e\bin;..\ref_send\bin" ^
  src\org\ref_send\test\*.java
