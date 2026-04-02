#!/bin/bash

set -e

# 设置语言环境
export LANG=en_US.UTF-8

# 强制使用 mvn（mvnd 不支持交互式输入）
MVN="mvn"

# 动态检测CPU核心数（用于并行编译）
get_cpu_cores() {
    if command -v nproc >/dev/null 2>&1; then
        nproc
    elif [ -f /proc/cpuinfo ]; then
        grep -c ^processor /proc/cpuinfo
    elif command -v sysctl >/dev/null 2>&1; then
        sysctl -n hw.ncpu 2>/dev/null || echo 2
    else
        echo 2
    fi
}
CPU_CORES=$(get_cpu_cores)

# 脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
APP_DIR="$SCRIPT_DIR"
LOG_DIR="$APP_DIR/logs"
CONF_DIR="$APP_DIR/conf"
LIB_DIR="$APP_DIR/lib"
CLASSES_DIR="$APP_DIR/target/classes"

# 应用名称（动态检测）
APP_NAME=""

# 全局变量
IS_DEV_MODE=""
DEBUG_PORT=""
CMD_ARGS=""

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo_info() { printf "${GREEN}[INFO]${NC} %s\n" "$1"; }
echo_warn() { printf "${YELLOW}[WARN]${NC} %s\n" "$1"; }
echo_error() { printf "${RED}[ERROR]${NC} %s\n" "$1"; }

# 检测运行模式
detect_mode() {
    if [ -d "$APP_DIR/src" ] || [ -f "$APP_DIR/pom.xml" ]; then echo 0; else echo 1; fi
}

IS_DEV_MODE=$(detect_mode)

if [ ! -d "$APP_DIR" ]; then
    echo_error "目录不存在：$APP_DIR"
    exit 1
fi

cd "$APP_DIR"

# 动态检测 JAR (仅部署模式)
if [ $IS_DEV_MODE -eq 1 ]; then
    if [ ! -d "$LIB_DIR" ]; then echo_error "lib 目录不存在"; exit 1; fi
    APP_NAME=$(ls -t $LIB_DIR/generator-cmd-*.jar 2>/dev/null | head -n 1)
    if [ -z "$APP_NAME" ]; then echo_error "未找到 Jar 文件"; exit 1; fi
    APP_NAME=$(basename "$APP_NAME")
    echo_info "检测到 JAR 文件：$APP_NAME"
fi

[ ! -d "$LOG_DIR" ] && mkdir -p "$LOG_DIR"

# Java 检查
if ! command -v java > /dev/null 2>&1; then echo_error "未检测到 Java"; exit 1; fi

JAVA_VERSION_STR=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
# 正确处理不同格式的版本号：1.8.0_xxx, 11.x.x, 17.x.x, 21.x.x
MAJOR_VERSION=$(echo "$JAVA_VERSION_STR" | awk -F. '{print $1}')
if [ "$MAJOR_VERSION" = "1" ]; then
    # 旧版本格式：1.8.0_xxx -> 使用第二个字段作为实际版本号
    JAVA_VERSION=$(echo "$JAVA_VERSION_STR" | awk -F. '{print $2}')
else
    # 新版本格式：17.0.2 -> 使用第一个字段
    JAVA_VERSION="$MAJOR_VERSION"
fi

JAVA_OPTS="-server -Xms512M -Xmx512M -XX:MetaspaceSize=64M -XX:MaxMetaspaceSize=128M -Djava.io.tmpdir=$LOG_DIR/"

if [ "$JAVA_VERSION" -ge 21 ]; then
    JAVA_OPTS="$JAVA_OPTS -XX:+UseZGC -XX:+ZGenerational"
    echo_info "使用 ZGC with Generational (JDK $JAVA_VERSION)"
elif [ "$JAVA_VERSION" -ge 17 ]; then
    JAVA_OPTS="$JAVA_OPTS -XX:+UseZGC"
    echo_info "使用 ZGC (JDK $JAVA_VERSION)"
elif [ "$JAVA_VERSION" -ge 11 ]; then
    JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
    echo_info "使用 G1 GC (JDK $JAVA_VERSION)"
elif [ "$JAVA_VERSION" -ge 8 ]; then
    JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
    echo_info "使用 G1 GC (JDK $JAVA_VERSION)"
else
    JAVA_OPTS="$JAVA_OPTS -XX:+UseConcMarkSweepGC"
    echo_info "使用 CMS GC (JDK $JAVA_VERSION)"
fi

# 解析调试端口参数（如果提供），其他参数直接传递给Java程序
JAVA_CMD_ARGS=()
i=0
while [ $i -lt $# ]; do
    i=$((i+1))
    eval "arg=\${$i}"
    
    if [ "$arg" = "--debug-port" ]; then
        i=$((i+1))
        eval "DEBUG_PORT=\${$i}"
    else
        JAVA_CMD_ARGS+=("$arg")
    fi
done

if [ -n "$DEBUG_PORT" ]; then
    DEBUG_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=$DEBUG_PORT"
    echo_info "调试模式端口：$DEBUG_PORT"
fi

# 启动应用
if [ $IS_DEV_MODE -eq 0 ]; then
    echo_info "=== 开发模式 ==="
    # 先编译core和cmd模块
    cd "$APP_DIR/.."
    $MVN compile -pl core,cmd -am -q
    cd "$APP_DIR"
    
	rm -rf ./demo ./logs
    
    # 获取classpath
    TEMP_CP=$(mktemp)
    $MVN dependency:build-classpath -Dmdep.outputFile=$TEMP_CP -q
    CP=$(cat $TEMP_CP)
    rm $TEMP_CP
    
    # 添加classes目录到classpath
    FULL_CP="$APP_DIR/target/classes:$CP"
    # 从pom.xml动态获取主类
    MAIN_CLASS=$(grep -oP '(?<=<main.class>)[^<]+' "$APP_DIR/../pom.xml" 2>/dev/null || grep -oP '(?<=<main.class>)[^<]+' "$APP_DIR/pom.xml" 2>/dev/null || echo "org.hyw.tools.generator.cmd.CmdGenerator")
    
    # 重置终端stty设置，确保输入正确处理（修复^M回车问题）
    stty sane
    stty icrnl
    stty icanon
    
    # 直接运行java程序
    if [ ${#JAVA_CMD_ARGS[@]} -gt 0 ]; then
        java -cp "$FULL_CP" $JAVA_OPTS $MAIN_CLASS "${JAVA_CMD_ARGS[@]}"
    else
        java -cp "$FULL_CP" $JAVA_OPTS $MAIN_CLASS
    fi
else
    echo_info "=== 部署模式 ==="
    JAR_FILE="$LIB_DIR/$APP_NAME"
    java $JAVA_OPTS $DEBUG_OPTS -jar "$JAR_FILE" "${JAVA_CMD_ARGS[@]}"
fi

echo_info "启动完成!"