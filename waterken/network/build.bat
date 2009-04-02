@echo off
echo 'Building network project...'
mkdir bin
javac -d bin ^
  -classpath "..\joe-e\bin;..\ref_send\bin" ^
  src\org\waterken\archive\*.java ^
  src\org\waterken\archive\dir\*.java ^
  src\org\waterken\archive\n2v\*.java ^
  src\org\waterken\archive\n2v\cmd\*.java ^
  src\org\waterken\dns\*.java ^
  src\org\waterken\http\*.java ^
  src\org\waterken\http\mirror\*.java ^
  src\org\waterken\http\trace\*.java ^
  src\org\waterken\io\*.java ^
  src\org\waterken\io\bounded\*.java ^
  src\org\waterken\io\limited\*.java ^
  src\org\waterken\io\open\*.java ^
  src\org\waterken\net\*.java ^
  src\org\waterken\net\http\*.java ^
  src\org\waterken\udp\*.java ^
  src\org\waterken\uri\*.java
