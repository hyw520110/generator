#!/bin/sh

# =============================================================================
# 代码生成器启动脚本
# 自动检测运行模式：开发模式/部署模式
# 支持命令行参数透传至 CmdGenerator
# =============================================================================
# 用法:
#   交互式模式:
#     ./run.sh                          # 自动检测模式启动
#     ./run.sh 5005                     # 启动 + 调试端口
#
#   快速模式 (极简参数):
#     ./run.sh --quick --db-password 123456
#
#   查看帮助:
#     ./run.sh --help
# =============================================================================

set -e

# 设置语言环境
export LANG=en_US.UTF-8

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

# =============================================================================
# 参数解析
# =============================================================================
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

# =============================================================================
# 启动应用
# =============================================================================
if [ $IS_DEV_MODE -eq 0 ]; then
    echo_info "=== 开发模式 ==="
    if [ ! -d "$CLASSES_DIR" ]; then
     mvn clean compile 
    fi
	rm -rf ./demo ./logs
    # 使用Maven exec插件运行，自动处理classpath和依赖
    # 将Java命令行参数转换为Maven exec插件的参数格式
    if [ ${#JAVA_CMD_ARGS[@]} -gt 0 ]; then
        mvn exec:java -Dexec.args="${JAVA_CMD_ARGS[*]}"
    else
        mvn exec:java
    fi
else
    echo_info "=== 部署模式 ==="
    JAR_FILE="$LIB_DIR/$APP_NAME"
    java $JAVA_OPTS $DEBUG_OPTS -jar "$JAR_FILE" "${JAVA_CMD_ARGS[@]}"
fi

echo_info "启动完成!"
