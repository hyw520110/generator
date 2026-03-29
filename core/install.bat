@echo off
REM Core 模块安装脚本 (Windows 版本)
REM 功能：计算 src 目录和 pom.xml 的哈希值，与缓存比对，仅在变更时执行安装

REM 切换到脚本所在目录
cd /d "%~dp0"

REM 哈希缓存文件路径（存储在用户 .m2 目录下）
set "HASH_CACHE_DIR=%USERPROFILE%\.m2\generator"
set "HASH_CACHE_FILE=%HASH_CACHE_DIR%\core-hash.txt"

REM 优先使用 mvnd（Maven Daemon）
where mvnd >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    set "MVN=mvnd"
) else (
    set "MVN=mvn"
)

REM 动态检测CPU核心数
:get_cpu_cores
for /f "tokens=2 delims==" %%a in ('wmic cpu get NumberOfLogicalProcessors /value ^| find "="') do (
    set CPU_CORES=%%a
    goto :cpu_cores_found
)
:cpu_cores_found
if "%CPU_CORES%"=="" set CPU_CORES=2

REM 计算文件的哈希值
:calculate_file_hash
set "file_path=%~1"
if not exist "%file_path%" (
    echo.
    exit /b 0
)

REM 使用 certutil 计算哈希（Windows 内置）
certutil -hashfile "%file_path%" MD5 | find /v ":" | find /v "MD5 hash of" | find /v "successfully completed" > "%TEMP%\hash_temp.txt"
for /f "tokens=1" %%a in (%TEMP%\hash_temp.txt) do (
    set "file_hash=%%a"
    del "%TEMP%\hash_temp.txt"
    exit /b 0
)
del "%TEMP%\hash_temp.txt" 2>nul
exit /b 0

REM 计算目录的哈希值（简化版，只计算部分关键文件）
:calculate_dir_hash
set "dir_path=%~1"
if not exist "%dir_path%" (
    echo.
    exit /b 0
)

REM 获取目录下所有 Java 文件并计算哈希
set "temp_hash_file=%TEMP%\dir_hash_%RANDOM%.txt"
if exist "%temp_hash_file%" del "%temp_hash_file%"

for /r "%dir_path%" %%f in (*.java) do (
    certutil -hashfile "%%f" MD5 2>nul | find /v ":" | find /v "MD5 hash of" | find /v "successfully completed" >> "%temp_hash_file%"
)

REM 计算所有哈希的组合哈希
if exist "%temp_hash_file%" (
    certutil -hashfile "%temp_hash_file%" MD5 2>nul | find /v ":" | find /v "MD5 hash of" | find /v "successfully completed" > "%TEMP%\final_hash.txt"
    for /f "tokens=1" %%a in (%TEMP%\final_hash.txt) do (
        set "dir_hash=%%a"
    )
    del "%TEMP%\final_hash.txt"
    del "%temp_hash_file%"
    echo %dir_hash%
) else (
    echo.
)
exit /b 0

REM 创建缓存目录（如果不存在）
:create_cache_dir
if not exist "%HASH_CACHE_DIR%" (
    mkdir "%HASH_CACHE_DIR%"
)
exit /b 0

REM 读取缓存哈希值
:read_cached_hash
if exist "%HASH_CACHE_FILE%" (
    set /p cached_hash=<"%HASH_CACHE_FILE%"
) else (
    set "cached_hash="
)
exit /b 0

REM 保存缓存哈希值
:save_hash
set "hash_value=%~1"
call :create_cache_dir
echo %hash_value% > "%HASH_CACHE_FILE%"
exit /b 0

REM 计算当前哈希值
:calculate_current_hash
set "src_hash="
set "pom_hash="

REM 计算 src 目录哈希
if exist "src" (
    for /f "delims=" %%a in ('call :calculate_dir_hash src') do (
        set "src_hash=%%a"
    )
)

REM 计算 pom.xml 哈希
if exist "pom.xml" (
    for /f "delims=" %%a in ('call :calculate_file_hash pom.xml') do (
        set "pom_hash=%%a"
    )
)

REM 组合哈希值
if defined src_hash (
    if defined pom_hash (
        set "current_hash=%src_hash%-%pom_hash%"
    ) else (
        set "current_hash=%src_hash%-"
    )
) else (
    if defined pom_hash (
        set "current_hash=-%pom_hash%"
    ) else (
        set "current_hash="
    )
)
exit /b 0

REM 主逻辑
:main
echo Core 模块安装检查

REM 检查必要文件是否存在
if not exist "pom.xml" (
    echo ❌ 错误: pom.xml 不存在
    exit /b 1
)

REM 计算当前哈希值
call :calculate_current_hash

if "%current_hash%"=="" (
    echo ⚠️  警告: 无法计算哈希值，强制执行安装
    goto :execute_install
)

REM 读取缓存哈希值
call :read_cached_hash

REM 比对哈希值
if "%current_hash%"=="%cached_hash%" (
    echo ✓ Core 模块未变更，跳过安装
    exit /b 0
) else (
    echo ✓ 检测到 Core 模块变更，开始安装...
    if not "%cached_hash%"=="" (
        echo   原哈希: %cached_hash:~0,8%...
        echo   新哈希: %current_hash:~0,8%...
    )
    goto :execute_install
)

:execute_install
echo 开始安装 Core 模块
echo 使用 Maven: %MVN%
echo CPU 核心数: %CPU_CORES%
echo 并行编译: -T %CPU_CORES%C

call %MVN% clean compile install -Dmaven.test.skip=true -T %CPU_CORES%C

if %ERRORLEVEL% EQU 0 (
    echo ✅ Core 模块安装成功
    
    REM 保存新的哈希值
    call :calculate_current_hash
    call :save_hash %current_hash%
    echo ✓ 已更新哈希缓存
    exit /b 0
) else (
    echo ❌ Core 模块安装失败
    exit /b 1
)

REM 执行主逻辑
call :main