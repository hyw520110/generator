@echo off
setlocal enabledelayedexpansion

:: =============================================================================
:: Web 代码生成器启动脚本 (Windows)
:: 支持开发模式 (Maven/Node) / 部署模式 (Jar/Static)
:: =============================================================================

set "SCRIPT_DIR=%~dp0"
set "APP_DIR=%SCRIPT_DIR%"
set "LOG_DIR=%APP_DIR%logs"
set "LIB_DIR=%APP_DIR%lib"

set "START_BACKEND=true"
set "START_FRONTEND=true"
set "DEBUG_PORT="
set "JAVA_CMD_ARGS="

:: 参数解析
:parse_args
if "%~1"=="" goto end_parse
if "%~1"=="--backend" (set "START_FRONTEND=false") & shift & goto parse_args
if "%~1"=="--frontend" (set "START_BACKEND=false") & shift & goto parse_args
if "%~1"=="--debug-port" (set "DEBUG_PORT=%~2") & shift & shift & goto parse_args
if "%~1"=="-h" goto show_help
if "%~1"=="--help" goto show_help
set "JAVA_CMD_ARGS=%JAVA_CMD_ARGS% %1"
shift
goto parse_args

:show_help
echo 用法: startup.bat [选项]
echo.
echo 选项:
echo   --backend        仅启动后端服务 (Spring Boot)
echo   --frontend       仅启动前端服务 (Vue/Node)
echo   --debug-port     设置后端调试端口 (例如: 5005)
echo   -h, --help       显示此帮助信息
exit /b 0

:end_parse
cd /d "%APP_DIR%"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

:: 检测模式 (判断是否有 src 目录)
set "IS_DEV_MODE=1"
if exist "%APP_DIR%src" set "IS_DEV_MODE=0"

:: 启动后端
if "%START_BACKEND%"=="true" (
    echo [INFO] 正在启动后端服务...
    if "%IS_DEV_MODE%"=="0" (
        echo [INFO] 模式：开发模式 (Maven)
        start "Backend-Dev" cmd /c "mvn spring-boot:run -Dspring-boot.run.arguments=%JAVA_CMD_ARGS%"
    ) else (
        echo [INFO] 模式：部署模式 (JAR)
        set "ARTIFACT_ID=@project.artifactId@"
        :: 如果未被 Maven 过滤或找不到包，则模糊匹配
        if "!ARTIFACT_ID!"=="@project.artifactId@" (
            for /f "delims=" %%i in ('dir /b /s /t:c "%LIB_DIR%\*.jar" 2^>nul') do set "APP_NAME=%%i"
        ) else (
            for /f "delims=" %%i in ('dir /b /s /t:c "%LIB_DIR%\!ARTIFACT_ID!-*.jar" 2^>nul') do set "APP_NAME=%%i"
        )
        
        set "JAVA_OPTS=-server -Xms512M -Xmx512M -Djava.io.tmpdir=%LOG_DIR%"
        if not "%DEBUG_PORT%"=="" set "JAVA_OPTS=!JAVA_OPTS! -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=%DEBUG_PORT%"
        
        start "Backend-Deploy" java !JAVA_OPTS! -jar "!APP_NAME!" %JAVA_CMD_ARGS%
    )
)

:: 启动前端
if "%START_FRONTEND%"=="true" (
    echo [INFO] 正在启动前端服务...
    if "%IS_DEV_MODE%"=="0" (
        where yarn >nul 2>&1
        if %ERRORLEVEL% equ 0 (
            echo [INFO] 使用 yarn 启动前端...
            start "Frontend-Dev" cmd /c "yarn dev"
        ) else (
            echo [INFO] 使用 npm 启动前端...
            start "Frontend-Dev" cmd /c "npm run dev"
        )
    ) else (
        echo [INFO] 模式：部署模式 (静态资源)已由后端托管。
    )
)

echo [INFO] 启动指令已下达，服务在后台运行。
pause
