#!/bin/sh

# =============================================================================
# 服务停止脚本 (前后端)
# 支持优雅停止 (SIGTERM) -> 超时/强制模式下自动转为强制停止 (SIGKILL)
# =============================================================================
# 用法:
#   ./stop.sh              # 停止后端服务
#   ./stop.sh --frontend   # 仅停止前端服务
#   ./stop.sh --all        # 停止前后端服务
#   ./stop.sh -f           # 强制停止后端
#   ./stop.sh --all -f     # 强制停止所有
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo_info() { printf "${GREEN}[INFO]${NC} %s\n" "$1"; }
echo_warn() { printf "${YELLOW}[WARN]${NC} %s\n" "$1"; }

FORCE=false
STOP_BACKEND=false
STOP_FRONTEND=false

# 参数解析
while [ $# -gt 0 ]; do
    case "$1" in
        -f|--force) FORCE=true ;;
        --backend) STOP_BACKEND=true ;;
        --frontend) STOP_FRONTEND=true ;;
        --all) STOP_BACKEND=true; STOP_FRONTEND=true ;;
        *) STOP_BACKEND=true ;;
    esac
    shift
done

# 默认只停止后端
[ "$STOP_BACKEND" = false ] && [ "$STOP_FRONTEND" = false ] && STOP_BACKEND=true

# 提取前端端口
get_frontend_port() {
    local port=$(grep "port:" "$SCRIPT_DIR/vite.config.js" 2>/dev/null | awk -F: '{print $2}' | tr -d ', ' | head -n 1)
    echo "${port:-8000}"
}

# 停止后端服务
stop_backend() {
    local PID=""
    
    # 优先从 PID 文件读取
    if [ -f "$SCRIPT_DIR/logs/backend.pid" ]; then
        PID=$(cat "$SCRIPT_DIR/logs/backend.pid")
        # 验证 PID 是否有效
        if ! ps -p $PID > /dev/null 2>&1; then
            PID=""
            rm -f "$SCRIPT_DIR/logs/backend.pid"
        fi
    fi
    
    # 如果 PID 文件不存在，通过进程名查找
    if [ -z "$PID" ]; then
        # 提取 ArtifactID (匹配 Maven 占位符或默认值)
        ARTIFACT_ID="@project.artifactId@"
        [ "$ARTIFACT_ID" = "@project.artifactId@" ] && ARTIFACT_ID="generator-web"

        # 查找 PID (同时匹配 mvn 进程和 java -jar 进程)
        PID=$(ps -ef | grep -E "java.*$ARTIFACT_ID|spring-boot:run" | grep -v grep | awk '{print $2}')
    fi

    if [ -z "$PID" ]; then
        echo_info "未检测到正在运行的后端服务。"
        return 0
    fi

    echo_info "检测到后端服务 (PID: $PID)，正在尝试优雅停止..."
    kill -15 $PID 2>/dev/null

    # 等待停止 (最多 10 秒)
    local count=0
    while ps -p $PID > /dev/null 2>&1; do
        sleep 1
        count=$((count + 1))
        printf "\r正在等待进程结束... [%ds]" "$count"
        
        if [ $count -ge 10 ]; then
            printf "\n"
            if [ "$FORCE" = true ]; then
                echo_warn "优雅停止超时，执行强制杀掉 (kill -9)..."
                kill -9 $PID 2>/dev/null
            else
                echo_warn "优雅停止超时，进程仍在运行。如需强制结束请加 -f 参数。"
                return 1
            fi
            break
        fi
    done

    if ! ps -p $PID > /dev/null 2>&1; then
        printf "\n"
        echo_info "后端服务已成功停止。"
        rm -f "$SCRIPT_DIR/logs/backend.pid"
    fi
}

# 停止前端服务
stop_frontend() {
    local port=$(get_frontend_port)
    
    # 查找占用端口的进程
    PID=$(lsof -ti :$port 2>/dev/null | head -n 1)

    if [ -z "$PID" ]; then
        echo_info "未检测到正在运行的前端服务 (端口 $port)。"
        return 0
    fi

    echo_info "检测到前端服务 (PID: $PID, 端口: $port)，正在尝试停止..."
    kill -15 $PID 2>/dev/null

    # 等待停止 (最多 5 秒)
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
[ "$STOP_BACKEND" = true ] && stop_backend
[ "$STOP_FRONTEND" = true ] && stop_frontend

exit 0