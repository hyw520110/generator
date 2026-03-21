#!/bin/sh

# =============================================================================
# Web 后端服务启动脚本
# 支持前台/后台启动，自动检测开发/部署模式
# =============================================================================
# 用法:
#   ./run.sh                    # 前台启动（默认）
#   ./run.sh -d, --daemon       # 后台启动
#   ./run.sh -f, --force        # 强制重启（先停止已有服务）
#   ./run.sh --debug-port 5005  # 开启远程调试
#   ./run.sh -h, --help         # 显示帮助
# =============================================================================

set -e

export LANG=en_US.UTF-8

# 优先使用 mvnd（Maven Daemon）
if command -v mvnd >/dev/null 2>&1; then
    MVN="mvnd"
else
    MVN="mvn"
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
APP_DIR="$SCRIPT_DIR"
LOG_DIR="$APP_DIR/logs"
LIB_DIR="$APP_DIR/lib"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo_info() { printf "${GREEN}[INFO]${NC} %s\n" "$1"; }
echo_warn() { printf "${YELLOW}[WARN]${NC} %s\n" "$1"; }
echo_error() { printf "${RED}[ERROR]${NC} %s\n" "$1"; }

show_help() {
    echo "用法: ./run.sh [选项]"
    echo ""
    echo "选项:"
    echo "  -d, --daemon       后台启动模式"
    echo "  -f, --force        强制重启（先停止已有服务）"
    echo "  --debug-port PORT  设置远程调试端口 (例如: 5005)"
    echo "  -h, --help         显示此帮助信息"
    echo ""
    echo "默认: 前台启动，日志输出到控制台"
    exit 0
}

# 检测运行模式：开发模式（存在 src 或 pom.xml）或部署模式
detect_mode() {
    if [ -d "$APP_DIR/src" ] || [ -f "$APP_DIR/pom.xml" ]; then
        echo "dev"
    else
        echo "deploy"
    fi
}

# 参数解析
DAEMON=false
FORCE=false
DEBUG_PORT=""

while [ $# -gt 0 ]; do
    case "$1" in
        -d|--daemon) DAEMON=true ;;
        -f|--force) FORCE=true ;;
        --debug-port) shift; DEBUG_PORT="$1" ;;
        -h|--help) show_help ;;
        *) echo_warn "未知参数: $1" ;;
    esac
    shift
done

# 强制重启模式：先停止已有服务
if [ "$FORCE" = true ]; then
    echo_info "强制重启模式：先停止已有服务..."
    "$SCRIPT_DIR/stop.sh" -f
fi

cd "$APP_DIR"
[ ! -d "$LOG_DIR" ] && mkdir -p "$LOG_DIR"

# 检测模式
MODE=$(detect_mode)

# 从配置文件提取端口
get_backend_port() {
    local conf_dir="src/main/resources"
    local port=""
    
    if [ -f "$conf_dir/application.properties" ]; then
        port=$(grep "server.port" "$conf_dir/application.properties" | awk -F= '{print $2}' | tr -d '[:space:]')
    elif [ -f "$conf_dir/application.yml" ]; then
        port=$(grep "port:" "$conf_dir/application.yml" | head -n 1 | awk '{print $2}' | tr -d '[:space:]')
    fi
    
    # 默认端口
    echo "${port:-8081}"
}

BACKEND_PORT=$(get_backend_port)

# 检查 Java
if ! command -v java > /dev/null 2>&1; then
    echo_error "未检测到 Java，请先安装 Java"
    exit 1
fi

JAVA_VERSION_STR=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
MAJOR_VERSION=$(echo "$JAVA_VERSION_STR" | awk -F. '{print $1}')
[ "$MAJOR_VERSION" = "1" ] && JAVA_VERSION=$(echo "$JAVA_VERSION_STR" | awk -F. '{print $2}') || JAVA_VERSION="$MAJOR_VERSION"

JAVA_OPTS="-server -Xms512M -Xmx512M -Djava.io.tmpdir=$LOG_DIR/"

if [ -n "$DEBUG_PORT" ]; then
    JAVA_OPTS="$JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=$DEBUG_PORT"
    echo_info "远程调试已启用，端口: $DEBUG_PORT"
fi

# 启动后端
start_backend() {
    if [ "$MODE" = "dev" ]; then
        if [ "$DAEMON" = true ]; then
            # 后台启动
            nohup $MVN spring-boot:run > "$LOG_DIR/backend.log" 2>&1 &
            echo $! > "$LOG_DIR/backend.pid"
        else
            # 前台启动
            $MVN spring-boot:run
        fi
    else
        # 查找 JAR 文件
        ARTIFACT_ID="@project.artifactId@"
        APP_JAR=$(ls -t $LIB_DIR/${ARTIFACT_ID}-*.jar 2>/dev/null | head -n 1)
        
        if [ "$ARTIFACT_ID" = "@project.artifactId@" ] || [ -z "$APP_JAR" ]; then
            APP_JAR=$(ls -t $LIB_DIR/*.jar 2>/dev/null | head -n 1)
        fi
        
        if [ -z "$APP_JAR" ]; then
            echo_error "未找到可执行的 JAR 文件: $LIB_DIR/"
            exit 1
        fi
        
        if [ "$DAEMON" = true ]; then
            # 后台启动
            nohup java $JAVA_OPTS -jar "$APP_JAR" > "$LOG_DIR/backend.log" 2>&1 &
            echo $! > "$LOG_DIR/backend.pid"
        else
            # 前台启动
            java $JAVA_OPTS -jar "$APP_JAR"
        fi
    fi
}

# 后台模式时等待服务就绪
wait_for_port() {
    local port=$1
    local timeout=$2
    local count=0
    
    while ! nc -z localhost "$port" >/dev/null 2>&1; do
        printf "\r${GREEN}[INFO]${NC} 等待服务就绪 [%ds]" "$count"
        sleep 1
        count=$((count + 1))
        if [ $count -ge $timeout ]; then
            printf "\n"
            echo_error "服务启动超时 (${timeout}s)，请检查日志: $LOG_DIR/backend.log"
            return 1
        fi
    done
    printf "\r${GREEN}[INFO]${NC} 服务已就绪！(%ds)    \n" "$count"
}

# 执行启动
if [ "$DAEMON" = true ]; then
    start_backend
    wait_for_port "$BACKEND_PORT" 120
else
    start_backend
fi
