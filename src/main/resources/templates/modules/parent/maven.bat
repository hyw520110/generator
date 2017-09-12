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
	echo  ����eclipse�����ļ��͹���classpath�ļ�,ִ�����ſ��Ե���eclipse
	mvn eclipse:clean eclipse:myeclipse  -DdownloadSources=true -e
	pauuse
exit

:install
	%~d0
	cd %~dp0
	echo  �����ǰ���̵�Դ��(����������Դ��Ͳ�����Դ�ļ�),��װ������maven��,�������������̵���
	set /p release=Ĭ��(�س���y)�����װ���հ�(���������),��װ��ʽ/������(n)?:

	if "%release%"=="n" (
		mvn clean:clean install -Pprod	
	) else (
		mvn clean:clean install -Dmaven.test.skip=true  -Pexdev	
	)

	pauuse
exit

:deploy
	echo  ������Ա����������̱��汾������ǰ��ػ�ȡ����Դ��,������Ա�����!
	goto start
exit

:start
set /p var=ȷ������(y/n)?:
if "%var%"=="y"  goto yes
if "%var%"=="n"  goto no
if  1==1  goto continue

:yes
	%~d0
	cd %~dp0
	echo  �����ǰ���̵�Դ��,��װ������maven��,�������ڲ�nexus˽����
	set /p release=Ĭ�Ϸ�������/������(�س���y),������ʽ/������(n)?:

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
	echo �������!������!����y��n:
	goto start
exit

:dependency
	%~d0
	cd %~dp0
	echo  ���������������Ϣ�����������ı��ļ�dependency.txt��
	mvn   dependency:tree > dependency.txt
	 
exit

:clean
	%~d0
	cd %~dp0
	mvn eclipse:clean clean:clean

exit