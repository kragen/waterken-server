@echo off
echo 'Building persistence project...'
jar cmf REPORT.MF ..\report.jar X.class
jar cmf TOUCH.MF ..\touch.jar X.class
mkdir bin
javac -d bin ^
  -classpath "..\joe-e\bin;..\ref_send\bin;..\log\bin" ^
  src\org\waterken\base32\Base32.java ^
  src\org\waterken\base32\InvalidBase32.java ^
  src\org\waterken\base32\package-info.java ^
  src\org\waterken\cache\Cache.java ^
  src\org\waterken\cache\CacheReference.java ^
  src\org\waterken\cache\package-info.java ^
  src\org\waterken\db\Creator.java ^
  src\org\waterken\db\CyclicGraph.java ^
  src\org\waterken\db\Database.java ^
  src\org\waterken\db\DatabaseManager.java ^
  src\org\waterken\db\Effect.java ^
  src\org\waterken\db\package-info.java ^
  src\org\waterken\db\ProhibitedCreation.java ^
  src\org\waterken\db\ProhibitedModification.java ^
  src\org\waterken\db\Root.java ^
  src\org\waterken\db\Service.java ^
  src\org\waterken\db\Transaction.java ^
  src\org\waterken\db\TransactionMonitor.java ^
  src\org\waterken\jos\BigDecimalWrapper.java ^
  src\org\waterken\jos\BigIntegerWrapper.java ^
  src\org\waterken\jos\ConstructorWrapper.java ^
  src\org\waterken\jos\Faulting.java ^
  src\org\waterken\jos\FieldWrapper.java ^
  src\org\waterken\jos\JODB.java ^
  src\org\waterken\jos\JODBManager.java ^
  src\org\waterken\jos\MacInputStream.java ^
  src\org\waterken\jos\MacOutputStream.java ^
  src\org\waterken\jos\MethodWrapper.java ^
  src\org\waterken\jos\package-info.java ^
  src\org\waterken\jos\Report.java ^
  src\org\waterken\jos\Slicer.java ^
  src\org\waterken\jos\Splice.java ^
  src\org\waterken\jos\SubstitutionStream.java ^
  src\org\waterken\jos\SymbolicLink.java ^
  src\org\waterken\jos\Touch.java ^
  src\org\waterken\project\package-info.java ^
  src\org\waterken\project\Project.java ^
  src\org\waterken\store\package-info.java ^
  src\org\waterken\store\Store.java ^
  src\org\waterken\store\StoreMaker.java ^
  src\org\waterken\store\Update.java ^
  src\org\waterken\store\folder\Folder.java ^
  src\org\waterken\store\folder\package-info.java ^
  src\org\waterken\store\folder\SynchedFileOutputStream.java ^
  src\org\waterken\thread\Concurrent.java ^
  src\org\waterken\thread\package-info.java ^
  src\org\waterken\thread\Sleep.java ^
  src\org\waterken\thread\Yield.java
