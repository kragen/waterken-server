@echo off
echo 'Building syntax project...'
mkdir bin
javac -d bin ^
  -classpath "..\joe-e\bin;..\ref_send\bin" ^
  src\org\ref_send\scope\Layout.java ^
  src\org\ref_send\scope\package-info.java ^
  src\org\ref_send\scope\Scope.java ^
  src\org\waterken\syntax\BadSyntax.java ^
  src\org\waterken\syntax\Deserializer.java ^
  src\org\waterken\syntax\Exporter.java ^
  src\org\waterken\syntax\Importer.java ^
  src\org\waterken\syntax\package-info.java ^
  src\org\waterken\syntax\Serializer.java ^
  src\org\waterken\syntax\Syntax.java ^
  src\org\waterken\syntax\config\Config.java ^
  src\org\waterken\syntax\config\package-info.java ^
  src\org\waterken\syntax\json\Java.java ^
  src\org\waterken\syntax\json\JSONDeserializer.java ^
  src\org\waterken\syntax\json\JSONLexer.java ^
  src\org\waterken\syntax\json\JSONParser.java ^
  src\org\waterken\syntax\json\JSONSerializer.java ^
  src\org\waterken\syntax\json\JSONWriter.java ^
  src\org\waterken\syntax\json\package-info.java
