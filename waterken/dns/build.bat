@echo off
echo 'Building dns project...'
mkdir bin
javac -d bin ^
  -classpath "..\joe-e\bin;..\ref_send\bin;..\network\bin;..\log\bin;..\persistence\bin;..\shared\bin" ^
  src\org\waterken\dns\editor\BadFormat.java ^
  src\org\waterken\dns\editor\HostMaker.java ^
  src\org\waterken\dns\editor\package-info.java ^
  src\org\waterken\dns\editor\Registrar.java ^
  src\org\waterken\dns\editor\RegistrarMaker.java ^
  src\org\waterken\dns\editor\ResourceVariable.java ^
  src\org\waterken\dns\editor\UnsupportedClass.java ^
  src\org\waterken\dns\editor\UnsupportedResource.java ^
  src\org\waterken\dns\editor\UnsupportedType.java ^
  src\org\waterken\dns\editor\redirectory\package-info.java ^
  src\org\waterken\dns\editor\redirectory\Redirectory.java ^
  src\org\waterken\dns\editor\redirectory\RedirectoryMaker.java ^
  src\org\waterken\dns\udp\NameServer.java ^
  src\org\waterken\dns\udp\package-info.java ^
  src\org\waterken\menu\Menu.java ^
  src\org\waterken\menu\package-info.java ^
  src\org\waterken\menu\TooMany.java
