@echo off

setlocal enabledelayedexpansion
rem jre公网/内网下载地址
set url=http://download.oracle.com/otn-pub/java/jdk/8u144-b01/090f390dda5b47b9b721c7dfaa008135/jre-8u144-windows-x64.tar.gz?AuthParam=1504671349_880553e661c425ff6681836f22ae90df
rem 压缩包内的文件夹名
set dirName=jre1.8.0_144
set javaVersion=${javaVersion}

%~d0
cd %~dp0
rem 如jre/jdk没安装时,用wget下载jre用7z解压压缩包.如已安装jre但版本低于1.8,想使用jre8,把以下环境变量中的;%path%去掉即可
set path=%~dp0\wget;%~dp0\7z;%path%

if "%1"=="" (
set debug=
) else (
set debug=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%1
)

rem 执行java命令看执行是否成功，执行失败：下载jre设置环境变量，启动服务;执行成功:直接开启服务
java -fullversion
if !errorlevel! GTR 0 goto downloadJdk
rem 检查jdk版本
for /f tokens^=2-5^ delims^=.-_^" %j in ('java -fullversion 2^>^&1') do @set "jver=%j%k%l%m"
if %jver% LSS %javaVersion% goto downloadJdk

goto run

:downloadJdk
if not exist %dirName% (
	echo begin download %dirName%...
	if not exist jre8.tar.gz (
		wget %url% -O jre8.tar.gz
	)
	7z x  jre8.tar.gz
	7z x jre8.tar -y
	del /F /S /Q  jre8.tar
)

set path=%~dp0\%dirName%\bin;%path%
goto run

 
:run
echo start ${projectName}-${moduleName} service...
java %debug% -jar ../lib/${projectName}-${moduleName}.jar -Ddubbo.application.logger=slf4j