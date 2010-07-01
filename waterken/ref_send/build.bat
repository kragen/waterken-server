@echo off
echo 'Building ref_send project...'
mkdir bin
javac -d bin ^
  -classpath "..\joe-e\bin" ^
  src\org\ref_send\*.java ^
  src\org\ref_send\list\*.java ^
  src\org\ref_send\promise\*.java ^
  src\org\ref_send\scope\*.java ^
  src\org\ref_send\type\*.java
