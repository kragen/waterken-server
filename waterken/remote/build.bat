@echo off
echo 'Building remote project...'
jar cmf SERVE.MF ..\serve.jar X.class
jar cmf SPAWN.MF ..\spawn.jar X.class
mkdir bin
javac %1 -d bin ^
  -classpath "..\joe-e\bin;..\ref_send\bin;..\network\bin;..\log\bin;..\persistence\bin;..\syntax\bin" ^
  src\org\waterken\http\dump\*.java ^
  src\org\waterken\remote\*.java ^
  src\org\waterken\remote\http\*.java ^
  src\org\waterken\remote\mux\*.java ^
  src\org\waterken\server\*.java
