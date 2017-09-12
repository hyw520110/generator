@echo off
	%~d0
	cd %~dp0

if "%1"=="eclipse" goto eclipse
if "%1"=="install" goto install
if "%1"=="deploy" goto deploy
if "%1"=="dependency" goto dependency
if "%1"=="clean" goto clean

:eclipse
	%~d0
	cd %~dp0
	echo  生成eclipse工程文件和工程classpath文件,执行完后才可以导入eclipse
	mvn eclipse:clean eclipse:myeclipse  -DdownloadSources=true -e
	pauuse
exit

:install
	%~d0
	cd %~dp0
	echo  打包当前工程的源码(不包含测试源码和测试资源文件),安装到本地maven库,供本地其他工程调用
	set /p release=默认(回车或y)打包安装快照版(开发或测试),安装正式/生产版(n)?:

	if "%release%"=="n" (
		mvn clean:clean install -Pprod	
	) else (
		mvn clean:clean install -Dmaven.test.skip=true  -Pexdev	
	)

	pauuse
exit

:deploy
	echo  发布人员用来发布里程碑版本，发布前务必获取最新源码,其他人员勿操作!
	goto start
exit

:start
set /p var=确定发布(y/n)?:
if "%var%"=="y"  goto yes
if "%var%"=="n"  goto no
if  1==1  goto continue

:yes
	%~d0
	cd %~dp0
	echo  打包当前工程的源码,安装到本地maven库,发布到内部nexus私服库
	set /p release=默认发布快照/开发版(回车或y),发布正式/生产版(n)?:

	if "%release%"=="n" (
		mvn  clean install  deploy -Pprod
	) else (
		mvn  clean install deploy  -Dmaven.test.skip=true -Pexdev
	)

	pause
exit

:no
	
exit

:continue
	echo 输入错误!请重输!输入y或n:
	goto start
exit

:dependency
	%~d0
	cd %~dp0
	echo  输出工程依赖树信息，并导出到文本文件dependency.txt中
	mvn   dependency:tree > dependency.txt
	 
exit

:clean
	%~d0
	cd %~dp0
	mvn eclipse:clean clean:clean

exit