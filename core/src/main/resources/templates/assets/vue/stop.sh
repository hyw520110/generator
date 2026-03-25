#!/bin/sh

# =============================================================================
# 服务停止脚本 (前后端)
# 支持优雅停止 (SIGTERM) -> 超时/强制模式下自动转为强制停止 (SIGKILL)
# 根据脚本目录查找进程，防止误杀其他项目进程
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

# 从 properties 文件提取 server.port
extract_port_from_properties() {
    local file="$1"
    grep -E "^[^#]*server\.port\s*=" "$file" 2>/dev/null | \
        sed 's/.*server\.port\s*=\s*//' | \
        tr -d '[:space:]' | \
        grep -E '^[0-9]+$'
}

# 从 yml/yaml 文件提取 server.port
extract_port_from_yaml() {
    local file="$1"
    awk '
        /^server:/ { in_server=1; next }
        in_server && /^[^[:space:]]/ { in_server=0 }
        in_server && /^[[:space:]]+port:[[:space:]]*[0-9]+/ {
            match($0, /[0-9]+/)
            print substr($0, RSTART, RLENGTH)
            exit
        }
    ' "$file" 2>/dev/null
}

# 动态查找所有配置文件并提取端口号
discover_backend_ports() {
    local ports=""
    local primary_port=""
    
    # 查找 application.properties (排除 target、bin、node_modules)
    local app_props=$(find "$SCRIPT_DIR" \
        \( -type d -name "target" -o -type d -name "bin" -o -type d -name "node_modules" \) -prune \
        -o -name "application.properties" -type f -print 2>/dev/null | head -n 1)
    
    if [ -n "$app_props" ] && [ -f "$app_props" ]; then
        primary_port=$(extract_port_from_properties "$app_props")
    fi
    
    # 查找所有 properties 文件
    while IFS= read -r file; do
        [ -z "$file" ] && continue
        local port=$(extract_port_from_properties "$file")
        if [ -n "$port" ]; then
            case " $ports " in
                *" $port "*) ;;
                *) ports="$ports $port" ;;
            esac
        fi
    done <<EOF
$(find "$SCRIPT_DIR" \
    \( -type d -name "target" -o -type d -name "bin" -o -type d -name "node_modules" \) -prune \
    -o -name "*.properties" -type f -print 2>/dev/null)
EOF
    
    # 查找所有 yml/yaml 文件
    while IFS= read -r file; do
        [ -z "$file" ] && continue
        local port=$(extract_port_from_yaml "$file")
        if [ -n "$port" ]; then
            case " $ports " in
                *" $port "*) ;;
                *) ports="$ports $port" ;;
            esac
        fi
    done <<EOF
$(find "$SCRIPT_DIR" \
    \( -type d -name "target" -o -type d -name "bin" -o -type d -name "node_modules" \) -prune \
    -o \( -name "*.yml" -o -name "*.yaml" \) -type f -print 2>/dev/null)
EOF
    
    ports=$(echo $ports | tr -s ' ' | sed 's/^ //;s/ $//')
    
    if [ -n "$primary_port" ]; then
        ports=$(echo " $ports " | sed "s/ $primary_port / /" | tr -s ' ' | sed 's/^ //;s/ $//')
        ports="$primary_port $ports"
        ports=$(echo $ports | tr -s ' ' | sed 's/^ //;s/ $//')
    fi
    
    echo "$ports"
}

# 提取前端端口
get_frontend_port() {
    local port=$(grep "port:" "$SCRIPT_DIR/vite.config.js" 2>/dev/null | awk -F: '{print $2}' | tr -d ', ' | head -n 1)
    echo "${port:-8000}"
}

# 根据脚本目录查找 Java 进程 PID
find_java_pid_by_dir() {
    local search_dir="$1"
    search_dir="${search_dir%/}"
    
    # 查找 Java 进程，匹配 classpath 包含当前脚本目录
    # 排除 grep 自身和 mvnd daemon 进程
    ps -ef | grep -E "java.*$search_dir" | grep -v "grep" | grep -v "mvnd-daemon" | grep -v "mvnd.agent" | awk '{print $2}' | head -n 1
}

# 根据端口查找进程 PID（仅查找属于当前目录的进程）
find_pid_by_port() {
    local port="$1"
    local search_dir="$SCRIPT_DIR"
    
    local pids=$(lsof -ti :$port 2>/dev/null)
    
    if [ -z "$pids" ]; then
        echo ""
        return
    fi
    
    for pid in $pids; do
        local cmdline=$(ps -p $pid -o command= 2>/dev/null)
        if echo "$cmdline" | grep -q "$search_dir"; then
            echo "$pid"
            return
        fi
    done
    
    echo ""
}

# 停止后端服务
stop_backend() {
    local PID=""
    
    # 优先从 PID 文件读取
    if [ -f "$SCRIPT_DIR/logs/backend.pid" ]; then
        PID=$(cat "$SCRIPT_DIR/logs/backend.pid")
        if [ -n "$PID" ]; then
            local cmdline=$(ps -p $PID -o command= 2>/dev/null)
            if [ -z "$cmdline" ] || ! echo "$cmdline" | grep -q "$SCRIPT_DIR"; then
                PID=""
                rm -f "$SCRIPT_DIR/logs/backend.pid"
            fi
        fi
    fi
    
    # 方法2：根据脚本目录查找 Java 进程（最精确的方式）
    if [ -z "$PID" ]; then
        PID=$(find_java_pid_by_dir "$SCRIPT_DIR")
        if [ -n "$PID" ]; then
            echo_info "通过目录匹配发现进程 (PID: $PID)"
        fi
    fi
    
    # 方法3：通过端口号查找（需要验证属于当前目录）
    if [ -z "$PID" ]; then
        PORTS=$(discover_backend_ports)
        
        if [ -n "$PORTS" ]; then
            for PORT in $PORTS; do
                PID=$(find_pid_by_port $PORT)
                if [ -n "$PID" ]; then
                    echo_info "在端口 $PORT 发现进程 (PID: $PID)"
                    break
                fi
            done
        fi
        
        if [ -z "$PID" ]; then
            for DEFAULT_PORT in 8080 8081 8082 8083; do
                PID=$(find_pid_by_port $DEFAULT_PORT)
                if [ -n "$PID" ]; then
                    echo_info "在默认端口 $DEFAULT_PORT 发现进程 (PID: $PID)"
                    break
                fi
            done
        fi
    fi

    if [ -z "$PID" ]; then
        echo_info "未检测到正在运行的后端服务。"
        return 0
    fi

    echo_info "检测到后端服务 (PID: $PID)，正在尝试优雅停止..."
    kill -15 $PID 2>/dev/null

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
[ "$STOP_BACKEND" = true ] && stop_backend
[ "$STOP_FRONTEND" = true ] && stop_frontend

exit 0
