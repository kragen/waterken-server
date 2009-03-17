@echo off
echo 'Building ref_send project...'
mkdir bin
javac -d bin ^
  -classpath "..\joe-e\bin" ^
  src\org\ref_send\deserializer.java ^
  src\org\ref_send\name.java ^
  src\org\ref_send\package-info.java ^
  src\org\ref_send\Record.java ^
  src\org\ref_send\list\List.java ^
  src\org\ref_send\list\package-info.java ^
  src\org\ref_send\promise\Channel.java ^
  src\org\ref_send\promise\Compose.java ^
  src\org\ref_send\promise\Deferred.java ^
  src\org\ref_send\promise\Detachable.java ^
  src\org\ref_send\promise\Do.java ^
  src\org\ref_send\promise\Eventual.java ^
  src\org\ref_send\promise\Failure.java ^
  src\org\ref_send\promise\Inline.java ^
  src\org\ref_send\promise\Invoke.java ^
  src\org\ref_send\promise\Log.java ^
  src\org\ref_send\promise\package-info.java ^
  src\org\ref_send\promise\Promise.java ^
  src\org\ref_send\promise\Receiver.java ^
  src\org\ref_send\promise\Rejected.java ^
  src\org\ref_send\promise\Resolver.java ^
  src\org\ref_send\promise\Vat.java ^
  src\org\ref_send\type\package-info.java ^
  src\org\ref_send\type\Typedef.java
