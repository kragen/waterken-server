@echo off
echo 'Building dns project...'
mkdir bin
javac -d bin ^
  -classpath "..\joe-e\bin;..\ref_send\bin;..\network\bin;..\log\bin;..\persistence\bin;..\shared\bin" ^
  src\org\waterken\dns\editor\*.java ^
  src\org\waterken\dns\udp\*.java ^
  src\org\waterken\menu\*.java
