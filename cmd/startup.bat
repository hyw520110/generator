@echo off
setlocal enabledelayedexpansion

REM =============================================================================
REM 代码生成器启动脚本 (Windows)
REM 自动检测运行模式：开发模式/部署模式
REM 支持命令行参数透传至 CmdGenerator
REM =============================================================================
REM 用法:
REM   交互式模式:
REM     startup.bat                          REM 自动检测模式启动
REM     startup.bat 5005                     REM 启动 + 调试端口
REM     startup.bat my.yaml                  REM 启动 + 自定义配置
REM     startup.bat my.yaml 5005             REM 启动 + 自定义配置 + 调试端口
REM
REM   非交互式模式 (直接生成):
REM     startup.bat --quick ^
REM       --db-type mysql --db-ip 192.168.1.100 --db-port 3306 ^
REM       --db-user root --db-password 123456 --db-name mydb ^
REM       --output-dir /output/demo --package com.example.demo
REM
REM   查看帮助:
REM     startup.bat --help
REM =============================================================================

chcp 936 >nul
cd /d %~dp0

set APP_DIR=%cd%
set LOG_DIR=%APP_DIR%\logs
set CONF_DIR=%APP_DIR%\conf
set LIB_DIR=%APP_DIR%\lib
set CLASSES_DIR=%APP_DIR%\target\classes

REM 应用名称 (Maven 占位符替换)
set APP_NAME=generator-cmd-1.0.1-SNAPSHOT.jar

REM 全局变量：运行模式 (避免重复检测)
set RUN_MODE=

REM 调试端口 (可选参数)
set DEBUG_PORT=
set CONFIG_FILE=

REM 命令行参数收集 (用于透传给 CmdGenerator)
set CMD_ARGS=

REM 检测是否包含新式参数 (-- 开头的参数)
set HAS_NEW_ARGS=false
for %%a in (%*) do (
    set ARG=%%a
    if "!ARG:~0,2!"=="--" set HAS_NEW_ARGS=true
)

if "%HAS_NEW_ARGS%"=="true" (
    REM 新式参数模式：直接透传所有参数
    set CMD_ARGS=%*
    
    REM 检查是否包含配置文件参数
    for %%a in (%*) do (
        if "!CONFIG_FILE!"=="" (
            set CHECK_ARG=%%a
            if "!CHECK_ARG:~0,2!"=="--" (
                if "!LAST_ARG!"=="--config" set CONFIG_FILE=%%a
            )
        )
        set LAST_ARG=%%a
    )
) else (
    REM 旧版位置参数模式：保持向后兼容
    set ARG1=%1
    set ARG2=%2
    
    if not "%ARG1%"=="" (
        echo !ARG1! | findstr /r "^[0-9][0-9]*" >nul
        if not errorlevel 1 (
            REM 第一个参数是纯数字 -> 调试端口
            set DEBUG_PORT=%1
        ) else (
            REM 第一个参数不是纯数字 -> 配置文件路径
            set CONFIG_FILE=%1
            if not "%ARG2%"=="" (
                echo !ARG2! | findstr /r "^[0-9][0-9]*" >nul
                if not errorlevel 1 (
                    set DEBUG_PORT=%2
                )
            )
        )
    )
    
    REM 旧版模式也透传参数
    set CMD_ARGS=%*
)

REM 全局变量：运行模式 (避免重复检测)
set RUN_MODE=

REM 检测运行模式 (结果缓存到全局变量)
if not "%RUN_MODE%"=="" (
    goto :mode_detected
)

set RUN_MODE=prod
if exist "%APP_DIR%\src" (
    set RUN_MODE=dev
)
if exist "%APP_DIR%\pom.xml" (
    set RUN_MODE=dev
)

:mode_detected

REM 创建日志目录
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

REM 检查 Java 版本
java -version 2>&1 | findstr /i "version" >nul
if %errorlevel% neq 0 (
    echo [ERROR] 未检测到 Java
    pause
    exit /b 1
)

echo [INFO] Java 版本信息:
java -version 2>&1 | findstr /i "version"

REM 检测 Java 版本
for /f "tokens=3" %%a in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VERSION=%%a
set JAVA_VERSION=%JAVA_VERSION:"=%

for /f "tokens=1,2 delims=." %%a in ("%JAVA_VERSION%") do (
    set MAJOR=%%a
    set MINOR=%%b
)

set JAVA_OPTS=-server -Xms512M -Xmx512M -Xmn200M -XX:MetaspaceSize=64M -XX:MaxMetaspaceSize=128M -Xss256k -XX:ParallelGCThreads=20 -Djava.io.tmpdir=%LOG_DIR%\

if "%MAJOR%"=="1" (
    if "%MINOR%"=="8" (
        set JAVA_OPTS=%JAVA_OPTS% -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled
        echo [INFO] 使用 CMS 垃圾回收器 ^(JDK 8^)
    )
) else (
    if %MAJOR% geq 11 (
        set JAVA_OPTS=%JAVA_OPTS% -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=16m
        echo [INFO] 使用 G1 垃圾回收器 ^(JDK %MAJOR%^)
    )
)

if not "%DEBUG_PORT%"=="" (
    set DEBUG_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=%DEBUG_PORT%
    echo [INFO] 调试模式已启用，端口：%DEBUG_PORT%
)

if "%RUN_MODE%"=="dev" (
    goto :dev_mode
) else (
    goto :prod_mode
)

:dev_mode
echo [INFO] === 开发模式 (自动检测) ===

if not exist "%CLASSES_DIR%" (
    echo [ERROR] classes 目录不存在：%CLASSES_DIR%
    echo [WARN] 请先执行：mvn compile
    pause
    exit /b 1
)

echo [INFO] 工作目录：%APP_DIR%
echo [INFO] 正在启动...

REM 开发模式使用 Maven exec:java (自动处理 classpath)
if not "%CMD_ARGS%"=="" (
    call mvn exec:java -Dexec.mainClass=org.hyw.tools.generator.cmd.CmdGenerator -Dexec.classpathScope=runtime -Dexec.cleanupDaemonThreads=false -Dexec.args="!CMD_ARGS!"
) else (
    call mvn exec:java -Dexec.mainClass=org.hyw.tools.generator.cmd.CmdGenerator -Dexec.classpathScope=runtime -Dexec.cleanupDaemonThreads=false
)
goto :end

:prod_mode
echo [INFO] === 部署模式 (自动检测) ===

set JAR_FILE=%LIB_DIR%\%APP_NAME%
if not exist "%JAR_FILE%" (
    echo [ERROR] Jar 文件不存在：%JAR_FILE%
    echo [WARN] 请先执行：mvn clean package
    pause
    exit /b 1
)

tasklist /FI "WINDOWTITLE eq %APP_NAME%*" 2>nul | find /i "%APP_NAME%" >nul
if not errorlevel 1 (
    echo [WARN] %APP_NAME% 已在运行中...
    goto :end
)

echo [INFO] 工作目录：%APP_DIR%
echo [INFO] Jar 文件：%JAR_FILE%
echo [INFO] 正在启动...

if not "%CMD_ARGS%"=="" (
    java %JAVA_OPTS% %DEBUG_OPTS% -jar "%JAR_FILE%" !CMD_ARGS!
) else (
    java %JAVA_OPTS% %DEBUG_OPTS% -jar "%JAR_FILE%" "%CONFIG_FILE%"
)

:end
echo [INFO] 启动完成!
