@echo off

set KEYWORD=${project.name}.jar

chcp 936
cd /d %~dp0
set JAVA_OPTS=-server -Xms512M -Xmx512M -Xmn200M -XX:MetaspaceSize=64M -XX:MaxMetaspaceSize=128M -Xss256k -XX:ParallelGCThreads=20 -XX:+UseConcMarkSweepGC -Djava.io.tmpdir=/logs/
java %JAVA_OPTS% -jar ..\lib\%KEYWORD%
