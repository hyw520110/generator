@echo off

setlocal enabledelayedexpansion
rem jre公网/内网下载地址
rem http://download.oracle.com/otn-pub/java/jdk/8u144-b01/090f390dda5b47b9b721c7dfaa008135/jre-8u144-windows-x64.tar.gz?AuthParam=1504671349_880553e661c425ff6681836f22ae90df
set url=ftp://192.168.40.113/dev-tools/JDK/jre-8u144-windows-x64.tar.gz
rem 压缩包内的文件夹名
set dirName=jre1.8.0_144

%~d0
cd %~dp0
rem 如jre/jdk没安装时,用wget下载jre用7z解压压缩包.如已安装jre但想使用下载的jre8,把以下环境变量中的;%path%去掉即可
set path=%~dp0\wget;%~dp0\7z;%path%

rem 执行java命令看执行是否成功，执行失败：下载jre设置环境变量，启动服务;执行成功:直接开启服务
java -version 
if !errorlevel! GTR  0 (
	goto downloadJdk
) else (
	goto start
)

:downloadJdk
if not exist %dirName% (
	echo begin download %dirName%...
	if not exist jre8.tar (
		wget %url% -O jre8.tar.gz
		7z x  jre8.tar.gz
		del /F /S /Q  jre8.tar.gz
	)
	7z x jre8.tar -y
	del /F /S /Q  jre8.tar
)

set path=%~dp0\%dirName%\bin;%path%

 
:start
echo start ${project.build.finalName} service...
java -jar ../lib/${project.build.finalName}.jar
