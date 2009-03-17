@echo off
echo 'Building persistence project...'
jar cmf REPORT.MF ..\report.jar X.class
jar cmf TOUCH.MF ..\touch.jar X.class
mkdir bin
javac -d bin ^
  -classpath "..\joe-e\bin;..\ref_send\bin;..\log\bin" ^
  src\org\waterken\base32\*.java ^
  src\org\waterken\cache\*.java ^
  src\org\waterken\db\*.java ^
  src\org\waterken\jos\*.java ^
  src\org\waterken\project\*.java ^
  src\org\waterken\store\*.java ^
  src\org\waterken\store\folder\*.java ^
  src\org\waterken\thread\*.java
