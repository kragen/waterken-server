@echo off
echo 'Building joe-e project...'
mkdir bin
javac -d bin ^
  src\org\joe_e\*.java ^
  src\org\joe_e\array\*.java ^
  src\org\joe_e\charset\*.java ^
  src\org\joe_e\file\*.java ^
  src\org\joe_e\reflect\*.java ^
  src\org\joe_e\taming\*.java ^
  src\org\joe_e\var\*.java
