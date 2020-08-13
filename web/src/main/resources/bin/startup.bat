@echo off

set KEYWORD=${project.name}.jar

chcp 936
cd /d %~dp0
cd ../
set JAVA_OPTS=-server -Xms512M -Xmx512M -Xmn200M -XX:MetaspaceSize=64M -XX:MaxMetaspaceSize=128M -Xss256k -XX:ParallelGCThreads=20 -XX:+UseConcMarkSweepGC -Djava.io.tmpdir=/logs/

java -version 2>&-

if not %errorlevel% == 0 (
	certutil -urlcache -split -f https://github.com/alibaba/dragonwell8/releases/download/v8.2.2-GA/Alibaba_Dragonwell_8.2.2-Experimental_Windows_x64.zip jdk8.zip
	rem bitsadmin /transfer n https://github.com/alibaba/dragonwell8/releases/download/v8.2.2-GA/Alibaba_Dragonwell_8.2.2-Experimental_Windows_x64.zip jdk8.zip
	expand jdk8.zip
)
java %JAVA_OPTS% -jar .\lib\%KEYWORD%
