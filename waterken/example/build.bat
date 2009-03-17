@echo off
echo 'Building example project...'
mkdir bin
javac -d bin ^
  -classpath "..\joe-e\bin;..\ref_send\bin;..\shared\bin" ^
  src\org\waterken\all\All.java ^
  src\org\waterken\all\package-info.java ^
  src\org\waterken\bang\Bang.java ^
  src\org\waterken\bang\Beat.java ^
  src\org\waterken\bang\Drum.java ^
  src\org\waterken\bang\DrumFactory.java ^
  src\org\waterken\bang\package-info.java ^
  src\org\waterken\bounce\AllTypes.java ^
  src\org\waterken\bounce\Bounce.java ^
  src\org\waterken\bounce\package-info.java ^
  src\org\waterken\bounce\Pitch.java ^
  src\org\waterken\bounce\Wall.java ^
  src\org\waterken\eq\package-info.java ^
  src\org\waterken\eq\Sneaky.java ^
  src\org\waterken\eq\SoundCheck.java ^
  src\org\waterken\factorial\Factorial.java ^
  src\org\waterken\factorial\FactorialN.java ^
  src\org\waterken\factorial\package-info.java ^
  src\org\waterken\serial\Element.java ^
  src\org\waterken\serial\package-info.java ^
  src\org\waterken\serial\PopPushN.java ^
  src\org\waterken\serial\Serial.java ^
  src\org\waterken\serial\Series.java
