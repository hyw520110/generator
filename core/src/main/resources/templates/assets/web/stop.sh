#!/bin/sh

# =============================================================================
# 前端服务停止脚本
# 支持优雅停止 (SIGTERM) -> 超时/强制模式下自动转为强制停止 (SIGKILL)
# =============================================================================
# 用法:
#   ./stop.sh              # 停止前端服务
#   ./stop.sh -f           # 强制停止
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo_info() { printf "${GREEN}[INFO]${NC} %s\n" "$1"; }
echo_warn() { printf "${YELLOW}[WARN]${NC} %s\n" "$1"; }

FORCE=false

# 参数解析
while [ $# -gt 0 ]; do
    case "$1" in
        -f|--force) FORCE=true ;;
        *) ;;
    esac
    shift
done

# 提取前端端口
get_frontend_port() {
    local port=$(grep "port:" "$SCRIPT_DIR/vite.config.js" 2>/dev/null | awk -F: '{print $2}' | tr -d ', ' | head -n 1)
    echo "${port:-8000}"
}

# 停止前端服务
stop_frontend() {
    local port=$(get_frontend_port)
    
    PID=$(lsof -ti :$port 2>/dev/null | head -n 1)

    if [ -z "$PID" ]; then
        echo_info "未检测到正在运行的前端服务 (端口 $port)。"
        return 0
    fi

    echo_info "检测到前端服务 (PID: $PID, 端口: $port)，正在尝试停止..."
    kill -15 $PID 2>/dev/null

    local count=0
    while ps -p $PID > /dev/null 2>&1; do
        sleep 1
        count=$((count + 1))
        
        if [ $count -ge 5 ]; then
            if [ "$FORCE" = true ]; then
                echo_warn "强制停止前端服务..."
                kill -9 $PID 2>/dev/null
            else
                echo_warn "前端停止超时，进程仍在运行。如需强制结束请加 -f 参数。"
                return 1
            fi
            break
        fi
    done

    echo_info "前端服务已成功停止。"
}

# 执行停止
stop_frontend

exit 0