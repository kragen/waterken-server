@echo off
echo 'Building syntax project...'
mkdir bin
javac -d bin ^
  -classpath "..\joe-e\bin;..\ref_send\bin" ^
  src\org\ref_send\scope\*.java ^
  src\org\waterken\syntax\*.java ^
  src\org\waterken\syntax\config\*.java ^
  src\org\waterken\syntax\json\*.java
