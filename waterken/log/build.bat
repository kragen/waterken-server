@echo off
echo 'Building log project...'
mkdir bin
javac -d bin ^
  -classpath "..\joe-e\bin;..\ref_send\bin" ^
  src\org\ref_send\log\Anchor.java ^
  src\org\ref_send\log\CallSite.java ^
  src\org\ref_send\log\Comment.java ^
  src\org\ref_send\log\Event.java ^
  src\org\ref_send\log\Got.java ^
  src\org\ref_send\log\package-info.java ^
  src\org\ref_send\log\Problem.java ^
  src\org\ref_send\log\Resolved.java ^
  src\org\ref_send\log\Returned.java ^
  src\org\ref_send\log\Sent.java ^
  src\org\ref_send\log\SentIf.java ^
  src\org\ref_send\log\Trace.java ^
  src\org\ref_send\log\Turn.java ^
  src\org\waterken\trace\EventSender.java ^
  src\org\waterken\trace\Marker.java ^
  src\org\waterken\trace\package-info.java ^
  src\org\waterken\trace\Tracer.java ^
  src\org\waterken\trace\TurnCounter.java ^
  src\org\waterken\trace\application\ApplicationTracer.java ^
  src\org\waterken\trace\application\package-info.java
