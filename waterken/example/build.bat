@echo off
echo 'Building example project...'
mkdir bin
javac -d bin ^
  -classpath "..\joe-e\bin;..\ref_send\bin;..\shared\bin" ^
  src\org\waterken\all\*.java ^
  src\org\waterken\bang\*.java ^
  src\org\waterken\bounce\*.java ^
  src\org\waterken\eq\*.java ^
  src\org\waterken\factorial\*.java ^
  src\org\waterken\serial\*.java
