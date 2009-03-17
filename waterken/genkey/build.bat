@echo off
echo 'Building genkey project...'
CALL jars.bat
mkdir bin
javac -d bin ^
  -classpath "..\joe-e\bin;..\ref_send\bin;..\network\bin;..\log\bin;..\persistence\bin;..\syntax\bin;..\remote\bin;..\shared\bin;..\dns\bin" ^
  src\org\waterken\genkey\GenKey.java ^
  src\org\waterken\genkey\package-info.java ^
  src\org\waterken\genkey\maker\Maker.java ^
  src\org\waterken\genkey\maker\package-info.java
