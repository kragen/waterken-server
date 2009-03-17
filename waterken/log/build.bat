@echo off
echo 'Building log project...'
mkdir bin
javac -d bin ^
  -classpath "..\joe-e\bin;..\ref_send\bin" ^
  src\org\ref_send\log\*.java ^
  src\org\waterken\trace\*.java ^
  src\org\waterken\trace\application\*.java
