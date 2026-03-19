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

# =============================================================================
# 后端启动逻辑 (Java)
# =============================================================================
wait_for_port() {
    local port=$1
    local name=$2
    local timeout=60
    local count=0
    printf "${GREEN}[INFO]${NC} 正在等待 %s 就绪 (端口: %s) " "$name" "$port"
    while ! nc -z localhost "$port" >/dev/null 2>&1; do
        printf "."
        sleep 1
        count=$((count + 1))
        if [ $count -ge $timeout ]; then
            printf "${RED} [超时]${NC}\n"
            echo_error "$name 启动超时，请检查后端配置"
            return 1
        fi
    done
    printf "${GREEN} [OK]${NC}\n"
}

start_backend() {
    echo_info ">>> 正在启动后端服务 (静默模式)..."
    
    # 获取后端端口 (从 vite.config.js 的代理配置中提取，默认为 8081)
    BACKEND_PORT=$(grep "target:" vite.config.js | awk -F: '{print $3}' | tr -d "', " | head -n 1)
    [ -z "$BACKEND_PORT" ] && BACKEND_PORT="8081"

    if [ $IS_DEV_MODE -eq 0 ]; then
        echo_info "模式：开发模式 (Maven)"
        mvn spring-boot:run -Dspring-boot.run.arguments="${JAVA_CMD_ARGS[*]}" > /dev/null 2>&1 &
    else
        echo_info "模式：部署模式 (JAR)"
        ARTIFACT_ID="@project.artifactId@"
        APP_NAME=$(ls -t $LIB_DIR/${ARTIFACT_ID}-*.jar 2>/dev/null | head -n 1)
        if [ "$ARTIFACT_ID" = "@project.artifactId@" ] || [ -z "$APP_NAME" ]; then
            APP_NAME=$(ls -t $LIB_DIR/*.jar 2>/dev/null | head -n 1)
        fi
        java $JAVA_OPTS -jar "$APP_NAME" "${JAVA_CMD_ARGS[@]}" > /dev/null 2>&1 &
    fi
    BACKEND_PID=$!
    
    # 等待后端就绪
    wait_for_port "$BACKEND_PORT" "后端服务"
}

# =============================================================================
# 前端启动逻辑 (Node/Vue)
# =============================================================================
open_browser() {
    local url=$1
    (
        sleep 2
        echo_info "尝试打开浏览器: $url"
        if [ "$(uname)" = "Darwin" ]; then
            open "$url"
        elif [ "$(expr substr $(uname -s) 1 5)" = "Linux" ]; then
            xdg-open "$url" 2>/dev/null || echo_warn "无法自动打开浏览器，请手动访问: $url"
        fi
    ) &
}

start_frontend() {
    echo_info ">>> 正在启动前端服务 (终端模式)..."
    
    if [ $IS_DEV_MODE -eq 0 ]; then
        # 基于哈希的依赖检查
        local files_to_hash="package.json"
        [ -f "yarn.lock" ] && files_to_hash="$files_to_hash yarn.lock"
        [ -f "package-lock.json" ] && files_to_hash="$files_to_hash package-lock.json"
        
        local current_hash=$(cat $files_to_hash 2>/dev/null | (md5sum 2>/dev/null || md5) | awk '{print $1}')
        local stored_hash=$(cat .dep_hash 2>/dev/null || echo "")

        if [ ! -d "node_modules" ] || [ "$current_hash" != "$stored_hash" ]; then
            echo_warn "检测到依赖变更，正在安装前端依赖 (详情见: $LOG_DIR/frontend_install.log)..."
            if command -v yarn > /dev/null 2>&1; then
                if yarn install > "$LOG_DIR/frontend_install.log" 2>&1; then
                    echo "$current_hash" > .dep_hash
                else
                    echo_error "前端依赖安装失败，请检查日志: $LOG_DIR/frontend_install.log"
                    return 1
                fi
            else
                if npm install > "$LOG_DIR/frontend_install.log" 2>&1; then
                    echo "$current_hash" > .dep_hash
                else
                    echo_error "前端依赖安装失败，请检查日志: $LOG_DIR/frontend_install.log"
                    return 1
                fi
            fi
        fi

        local port=$(grep "port:" vite.config.js | awk -F: '{print $2}' | tr -d ', ' | head -n 1)
        [ -z "$port" ] && port="8000"
        
        open_browser "http://localhost:$port"
        
        if command -v yarn > /dev/null 2>&1; then
            echo_info "执行: yarn dev"
            yarn dev
        else
            echo_info "执行: npm run dev"
            npm run dev
        fi
    else
        echo_info "模式：部署模式 (静态资源) 已由后端托管。"
    fi
}
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
