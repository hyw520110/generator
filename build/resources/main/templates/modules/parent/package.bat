@echo off

%~d0
cd %~dp0

mvn clean package -Dmaven.test.skip=true 2>&1

if %ERRORLEVEL%  NEQ 0 (echo "Set the environment variable :MAVEN_HOME and PATH")

pause