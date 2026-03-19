#!/bin/sh

# =============================================================================
# Web 代码生成器启动脚本 (支持前后端)
# 自动检测运行模式：开发模式 (Maven/Node) / 部署模式 (Jar/Static)
# =============================================================================
# 用法:
#   ./startup.sh                         # 同时启动前后端 (默认)
#   ./startup.sh --backend               # 仅启动后端
#   ./startup.sh --frontend              # 仅启动前端
#   ./startup.sh --debug-port 5005       # 启动并开启调试
# =============================================================================

set -e

# 设置环境
export LANG=en_US.UTF-8

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
APP_DIR="$SCRIPT_DIR"
LOG_DIR="$APP_DIR/logs"
LIB_DIR="$APP_DIR/lib"
CLASSES_DIR="$APP_DIR/target/classes"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo_info() { printf "${GREEN}[INFO]${NC} %s\n" "$1"; }
echo_warn() { printf "${YELLOW}[WARN]${NC} %s\n" "$1"; }
echo_error() { printf "${RED}[ERROR]${NC} %s\n" "$1"; }

show_help() {
    echo "用法: ./startup.sh [选项]"
    echo ""
    echo "选项:"
    echo "  --backend        仅启动后端服务 (Spring Boot)"
    echo "  --frontend       仅启动前端服务 (Vue/Node)"
    echo "  --debug-port     设置后端调试端口 (例如: 5005)"
    echo "  -h, --help       显示此帮助信息"
    echo ""
    echo "默认行为: 同时在后台启动前后端服务。"
    exit 0
}

# 检测运行模式
detect_mode() {
    if [ -d "$APP_DIR/src" ] || [ -f "$APP_DIR/pom.xml" ]; then echo 0; else echo 1; fi
}

IS_DEV_MODE=$(detect_mode)
START_BACKEND=true
START_FRONTEND=true
DEBUG_PORT=""
JAVA_CMD_ARGS=()

# 参数解析
while [ $# -gt 0 ]; do
    case "$1" in
        --backend) START_FRONTEND=false ;;
        --frontend) START_BACKEND=false ;;
        --debug-port) shift; DEBUG_PORT="$1" ;;
        -h|--help) show_help ;;
        *) JAVA_CMD_ARGS+=("$1") ;;
    esac
    shift
done

cd "$APP_DIR"
[ ! -d "$LOG_DIR" ] && mkdir -p "$LOG_DIR"

# =============================================================================
# 后端启动逻辑 (Java)
# =============================================================================
start_backend() {
    echo_info ">>> 正在启动后端服务..."
    
    # Java 检查与版本判定
    if ! command -v java > /dev/null 2>&1; then echo_error "未检测到 Java"; return 1; fi
    JAVA_VERSION_STR=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    MAJOR_VERSION=$(echo "$JAVA_VERSION_STR" | awk -F. '{print $1}')
    [ "$MAJOR_VERSION" = "1" ] && JAVA_VERSION=$(echo "$JAVA_VERSION_STR" | awk -F. '{print $2}') || JAVA_VERSION="$MAJOR_VERSION"

    JAVA_OPTS="-server -Xms512M -Xmx512M -Djava.io.tmpdir=$LOG_DIR/"
    [ -n "$DEBUG_PORT" ] && JAVA_OPTS="$JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=$DEBUG_PORT"

    if [ $IS_DEV_MODE -eq 0 ]; then
        echo_info "模式：开发模式 (Maven)"
        mvn spring-boot:run -Dspring-boot.run.arguments="${JAVA_CMD_ARGS[*]}" &
    else
        echo_info "模式：部署模式 (JAR)"
        # 使用 Maven 占位符，由生成项目的 Maven 构建时自动替换
        ARTIFACT_ID="@project.artifactId@"
        APP_NAME=$(ls -t $LIB_DIR/${ARTIFACT_ID}-*.jar 2>/dev/null | head -n 1)
        # 如果 Maven 未过滤（例如直接从源码运行），则回退到模糊匹配
        if [ "$ARTIFACT_ID" = "@project.artifactId@" ] || [ -z "$APP_NAME" ]; then
            APP_NAME=$(ls -t $LIB_DIR/*.jar 2>/dev/null | head -n 1)
        fi
        java $JAVA_OPTS -jar "$APP_NAME" "${JAVA_CMD_ARGS[@]}" &
    fi
    BACKEND_PID=$!
    echo_info "后端服务已在后台启动 (PID: $BACKEND_PID)"
}

# =============================================================================
# 前端启动逻辑 (Node/Vue)
# =============================================================================
start_frontend() {
    echo_info ">>> 正在启动前端服务..."
    
    if [ $IS_DEV_MODE -eq 0 ]; then
        # 开发模式：使用 yarn 或 npm 启动
        if command -v yarn > /dev/null 2>&1; then
            echo_info "使用 yarn 启动前端..."
            yarn run serve &
        elif command -v npm > /dev/null 2>&1; then
            echo_info "使用 npm 启动前端..."
            npm run serve &
        else
            echo_error "未检测到 yarn 或 npm，无法启动前端开发环境"
            return 1
        fi
        FRONTEND_PID=$!
        echo_info "前端开发服务器已在后台启动 (PID: $FRONTEND_PID)"
    else
        echo_info "模式：部署模式 (静态资源)"
        echo_info "前端资源已整合在后端 JAR 的静态目录中，无需独立启动。"
    fi
}

# 执行启动
[ "$START_BACKEND" = true ] && start_backend
[ "$START_FRONTEND" = true ] && start_frontend

echo_info "启动指令已下达。请通过日志查看具体运行状态。"
echo_info "日志目录: $LOG_DIR"

# 捕获退出信号
trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; echo_info '服务已停止';" EXIT

# 保持脚本运行，监听 PID
wait
