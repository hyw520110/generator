@echo off
REM GeneratorConfigTest 编译和执行脚本 (Windows 版本)
REM 用法: run_test.bat [选项]
REM 选项:
REM   --no-clean    不清理 demo 和 logs 目录
REM   --clean       强制清理 demo 和 logs 目录（默认）
REM   -h, --help    显示帮助信息

REM 切换到脚本所在目录
cd /d "%~dp0"

REM 默认开启清理功能
set CLEAN_DIRS=true

REM 解析参数
:parse_args
if "%~1"=="" goto done_parsing
if "%~1"=="--no-clean" (
    set CLEAN_DIRS=false
    shift
    goto parse_args
)
if "%~1"=="--clean" (
    set CLEAN_DIRS=true
    shift
    goto parse_args
)
if "%~1"=="-h" goto show_help
if "%~1"=="--help" goto show_help
echo 未知参数: %~1
echo 使用 -h 或 --help 查看帮助信息
exit /b 1

:show_help
echo GeneratorConfigTest 编译和执行脚本
echo.
echo 用法: %~nx0 [选项]
echo.
echo 选项:
echo   --no-clean    不清理 demo 和 logs 目录
echo   --clean       强制清理 demo 和 logs 目录（默认行为）
echo   -h, --help    显示此帮助信息
echo.
echo 示例:
echo   %~nx0              ^# 默认清理 demo 和 logs 目录
echo   %~nx0 --no-clean    ^# 不清理 demo 和 logs 目录
echo   %~nx0 --clean       ^# 显式清理 demo 和 logs 目录
exit /b 0

:done_parsing

REM 检查并安装 core 模块
echo 检查 Core 模块...

REM 检查上级目录的 core 目录
set "PARENT_DIR=%~dp0.."
set "CORE_DIR=%PARENT_DIR%\core"
set "CORE_INSTALL_SCRIPT=%CORE_DIR%\install.bat"

if exist "%CORE_DIR%" (
    if exist "%CORE_INSTALL_SCRIPT%" (
        echo ✓ 检测到 Core 模块目录: %CORE_DIR%
        echo ✓ 执行 Core 模块安装脚本...
        
        REM 调用 core/install.bat
        call "%CORE_INSTALL_SCRIPT%"
        if errorlevel 1 (
            echo.
            echo ❌ Core 模块安装失败！
            exit /b 1
        )
        echo.
        echo ✓ Core 模块检查完成
    ) else (
        echo ✓ Core 模块目录存在但 install.bat 缺失，跳过
    )
) else (
    echo ✓ Core 模块目录不存在，跳过
)

REM 清理函数
if "%CLEAN_DIRS%"=="true" (
    echo.
    echo 检查并清理目录...
    
    REM 清理 demo 目录
    if exist "demo" (
        echo 删除 demo 目录...
        rmdir /s /q demo
        echo ✓ demo 目录已删除
    ) else (
        echo ✓ demo 目录不存在，跳过
    )
    
    REM 清理 logs 目录
    if exist "logs" (
        echo 删除 logs 目录...
        rmdir /s /q logs
        echo ✓ logs 目录已删除
    ) else (
        echo ✓ logs 目录不存在，跳过
    )
) else (
    echo.
    echo 跳过目录清理（--no-clean 模式）
    echo.
)

echo 开始编译 GeneratorConfigTest...

REM 使用 Maven 编译测试代码
echo 执行: mvn test-compile
call mvn test-compile

if errorlevel 1 (
    echo ❌ 编译失败！
    exit /b 1
)

echo.
echo 编译成功！开始执行 GeneratorConfigTest...

REM 使用 Maven 运行指定的测试类
echo 执行: mvn test -Dtest=GeneratorConfigTest
call mvn test -Dtest=GeneratorConfigTest

if errorlevel 1 (
    echo.
    echo ❌ 测试执行失败！
    exit /b 1
)

echo.
echo ✅ 测试执行成功！
exit /b 0