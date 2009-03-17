@echo off
echo 'Building remote project...'
jar cmf SERVE.MF ..\serve.jar X.class
jar cmf SPAWN.MF ..\spawn.jar X.class
mkdir bin
javac -d bin ^
  -classpath "..\joe-e\bin;..\ref_send\bin;..\network\bin;..\log\bin;..\persistence\bin;..\syntax\bin" ^
  src\org\waterken\http\dump\Dump.java ^
  src\org\waterken\http\dump\package-info.java ^
  src\org\waterken\remote\Messenger.java ^
  src\org\waterken\remote\package-info.java ^
  src\org\waterken\remote\Remote.java ^
  src\org\waterken\remote\http\AMP.java ^
  src\org\waterken\remote\http\Callee.java ^
  src\org\waterken\remote\http\Caller.java ^
  src\org\waterken\remote\http\HTTP.java ^
  src\org\waterken\remote\http\Operation.java ^
  src\org\waterken\remote\http\Outbound.java ^
  src\org\waterken\remote\http\package-info.java ^
  src\org\waterken\remote\http\Pipeline.java ^
  src\org\waterken\remote\http\QueryOperation.java ^
  src\org\waterken\remote\http\ServerSideSession.java ^
  src\org\waterken\remote\http\SessionInfo.java ^
  src\org\waterken\remote\http\SessionMaker.java ^
  src\org\waterken\remote\http\UpdateOperation.java ^
  src\org\waterken\remote\http\VatInitializer.java ^
  src\org\waterken\remote\mux\Mux.java ^
  src\org\waterken\remote\mux\package-info.java ^
  src\org\waterken\remote\mux\Remoting.java ^
  src\org\waterken\remote\mux\UnknownScheme.java ^
  src\org\waterken\server\Credentials.java ^
  src\org\waterken\server\LastModified.java ^
  src\org\waterken\server\Loopback.java ^
  src\org\waterken\server\package-info.java ^
  src\org\waterken\server\Proxy.java ^
  src\org\waterken\server\Serve.java ^
  src\org\waterken\server\Settings.java ^
  src\org\waterken\server\Spawn.java ^
  src\org\waterken\server\SSL.java ^
  src\org\waterken\server\TCP.java ^
  src\org\waterken\server\UDP.java
