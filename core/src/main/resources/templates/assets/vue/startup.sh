#!/bin/sh

# =============================================================================
# Web 代码生成器启动脚本 (支持前后端)
# 自动检测运行模式：开发模式 / 部署模式
# =============================================================================
# 用法:
#   ./startup.sh                         # 同时启动前后端 (默认)
#   ./startup.sh --backend               # 仅启动后端
#   ./startup.sh --frontend              # 仅启动前端
#   ./startup.sh --debug-port 5005       # 启动并开启调试
#   ./startup.sh -f                      # 强制重启 (先停止已有进程)
# =============================================================================

set -e

export LANG=en_US.UTF-8

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
APP_DIR="$SCRIPT_DIR"
LOG_DIR="$APP_DIR/logs"

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
    echo "  --backend        仅启动后端服务"
    echo "  --frontend       仅启动前端服务"
    echo "  --debug-port     设置后端调试端口 (例如: 5005)"
    echo "  -f, --force      启动前停止已存在的进程 (前后端)"
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
FORCE_RESTART=false
DEBUG_PORT=""

# 参数解析
while [ $# -gt 0 ]; do
    case "$1" in
        --backend) START_FRONTEND=false ;;
        --frontend) START_BACKEND=false ;;
        --debug-port) shift; DEBUG_PORT="$1" ;;
        -f|--force) FORCE_RESTART=true ;;
        -h|--help) show_help ;;
        *) ;;
    esac
    shift
done

cd "$APP_DIR"
[ ! -d "$LOG_DIR" ] && mkdir -p "$LOG_DIR"

# 如果指定了强制启动，调用停止脚本
if [ "$FORCE_RESTART" = true ]; then
    chmod +x ./stop.sh
    if [ "$START_BACKEND" = true ] && [ "$START_FRONTEND" = true ]; then
        ./stop.sh --all -f
    elif [ "$START_BACKEND" = true ]; then
        ./stop.sh --backend -f
    elif [ "$START_FRONTEND" = true ]; then
        ./stop.sh --frontend -f
    fi
fi

# =============================================================================
# 后端启动逻辑 - 调用 run.sh
# =============================================================================
start_backend() {
    echo_info ">>> 正在启动后端服务..."
    chmod +x ./run.sh
    
    local run_args="--daemon"
    if [ -n "$DEBUG_PORT" ]; then
        run_args="$run_args --debug-port $DEBUG_PORT"
    fi
    
    ./run.sh $run_args
}

# =============================================================================
# 前端启动逻辑
# =============================================================================
start_frontend() {
    echo_info ">>> 正在启动前端服务 (终端模式)..."
    if [ $IS_DEV_MODE -eq 0 ]; then
        # 仅监测 package.json 的变更 (意图变更)
        local files_to_hash="package.json"

        # 跨平台计算 MD5
        local current_hash=$(cat $files_to_hash 2>/dev/null | (md5sum 2>/dev/null || md5) | awk '{print $1}')
        local stored_hash=$(cat .dep_hash 2>/dev/null || echo "")

        if [ ! -d "node_modules" ] || [ "$current_hash" != "$stored_hash" ]; then
            echo_warn "检测到依赖变更，正在安装前端依赖..."
            
            # 解决证书过期问题
            export NODE_TLS_REJECT_UNAUTHORIZED=0
            
            # 定义可用镜像列表
            local registries=("https://registry.npmmirror.com" "https://registry.npmjs.org" "https://registry.yarnpkg.com")
            local success=false

            for reg in "${registries[@]}"; do
                echo_info "尝试使用镜像源: $reg"
                
                if command -v yarn > /dev/null 2>&1; then
                    yarn config set strict-ssl false
                    if yarn install --registry "$reg"; then
                        success=true
                        echo "$current_hash" > .dep_hash
                        break
                    else
                        echo_warn "镜像 $reg 安装失败，清理缓存并尝试下一个..."
                        rm -f yarn.lock package-lock.json
                        yarn cache clean
                    fi
                else
                    npm config set strict-ssl false
                    if npm install --registry "$reg"; then
                        success=true
                        echo "$current_hash" > .dep_hash
                        break
                    else
                        echo_warn "镜像 $reg 安装失败，尝试下一个..."
                        rm -f package-lock.json
                    fi
                fi
            done

            if [ "$success" = false ]; then
                echo_error "所有镜像源均安装失败，请检查网络连接。"
                return 1
            fi
        fi

        local port=$(grep "port:" vite.config.js | awk -F: '{print $2}' | tr -d ', ' | head -n 1)
        [ -z "$port" ] && port="8000"
        
        echo_info "启动前端并监听端口: $port"
        echo_info "访问地址: http://localhost:$port"
        
        if command -v yarn > /dev/null 2>&1; then
            echo_info "执行: yarn dev"
            yarn dev --open
        else
            echo_info "执行: npm run dev"
            npm run dev -- --open
        fi
    else
        echo_info "模式：部署模式 (静态资源) 已由后端托管。"
    fi
}

# 执行启动
[ "$START_BACKEND" = true ] && start_backend
[ "$START_FRONTEND" = true ] && start_frontend

# 捕获退出信号，调用 stop.sh 停止服务
trap "echo_info '正在停止服务...'; chmod +x ./stop.sh; ./stop.sh --all 2>/dev/null; echo_info '服务已停止';" EXIT

# 保持脚本运行
wait
