@echo off
echo 'Building joe-e project...'
mkdir bin
javac -d bin ^
  src\org\joe_e\Equatable.java ^
  src\org\joe_e\ErrorHandler.java ^
  src\org\joe_e\Immutable.java ^
  src\org\joe_e\inert.java ^
  src\org\joe_e\IsJoeE.java ^
  src\org\joe_e\JoeE.java ^
  src\org\joe_e\Powerless.java ^
  src\org\joe_e\Selfless.java ^
  src\org\joe_e\Struct.java ^
  src\org\joe_e\SystemExit.java ^
  src\org\joe_e\Token.java ^
  src\org\joe_e\array\ArrayBuilder.java ^
  src\org\joe_e\array\ArrayIterator.java ^
  src\org\joe_e\array\BooleanArray.java ^
  src\org\joe_e\array\ByteArray.java ^
  src\org\joe_e\array\CharArray.java ^
  src\org\joe_e\array\ConstArray.java ^
  src\org\joe_e\array\DoubleArray.java ^
  src\org\joe_e\array\FloatArray.java ^
  src\org\joe_e\array\ImmutableArray.java ^
  src\org\joe_e\array\IntArray.java ^
  src\org\joe_e\array\LongArray.java ^
  src\org\joe_e\array\PowerlessArray.java ^
  src\org\joe_e\array\ShortArray.java ^
  src\org\joe_e\charset\ASCII.java ^
  src\org\joe_e\charset\URLEncoding.java ^
  src\org\joe_e\charset\UTF8.java ^
  src\org\joe_e\file\Filesystem.java ^
  src\org\joe_e\file\InvalidFilenameException.java ^
  src\org\joe_e\reflect\Proxies.java ^
  src\org\joe_e\reflect\Reflection.java ^
  src\org\joe_e\taming\Policy.java ^
  src\org\joe_e\var\Milestone.java ^
  src\org\joe_e\var\package-info.java
