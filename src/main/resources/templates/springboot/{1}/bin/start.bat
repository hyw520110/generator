@echo off

setlocal enabledelayedexpansion
%~d0
cd %~dp0
set path=%~dp0\wget;%~dp0\7z;%path%

java -version 

if !errorlevel! GTR  0 (
	goto downloadJdk
) else (
	goto start
)

:downloadJdk
if not exist jre1.8.0_144 (
	echo begin download jre1.8.0_144...
	if not exist jre8.tar (
		rem http://download.oracle.com/otn-pub/java/jdk/8u144-b01/090f390dda5b47b9b721c7dfaa008135/jre-8u144-windows-x64.tar.gz?AuthParam=1504671349_880553e661c425ff6681836f22ae90df
		wget ftp://192.168.40.113/dev-tools/JDK/jre-8u144-windows-x64.tar.gz -O jre8.tar.gz
		7z x  jre8.tar.gz
		del /F /S /Q  jre8.tar.gz
	)
	7z x jre8.tar -y
	del /F /S /Q  jre8.tar
)

set path=%~dp0\jre1.8.0_144\bin;%path%

 
:start
echo start ${projectName}-${moduleName} service...
java -jar ../lib/${projectName}-${moduleName}.jar
