#!/bin/sh

# =============================================================================
# Web 前端启动脚本
# 自动检测运行模式：开发模式 / 部署模式
# =============================================================================
# 用法:
#   ./startup.sh                         # 启动前端服务 (默认)
#   ./startup.sh -f                      # 强制重启
# =============================================================================

set -e

export LANG=en_US.UTF-8

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
APP_DIR="$SCRIPT_DIR"
LOG_DIR="$APP_DIR/logs"
DEP_HASH_FILE="$APP_DIR/.dep_hash"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo_info() { printf "${GREEN}[INFO]${NC} %s\n" "$1"; }
echo_warn() { printf "${YELLOW}[WARN]${NC} %s\n" "$1"; }
echo_error() { printf "${RED}[ERROR]${NC} %s\n" "$1"; }

# =============================================================================
# 代理检测与修复
# =============================================================================
check_and_fix_proxy() {
    local proxy_host=""
    local proxy_port=""
    local proxy_url=""

    # 检查 npm 代理配置
    local npm_proxy=$(npm config get proxy 2>/dev/null)
    local npm_https_proxy=$(npm config get https-proxy 2>/dev/null)

    if [ -n "$npm_proxy" ] && [ "$npm_proxy" != "null" ]; then
        proxy_url="$npm_proxy"
    elif [ -n "$npm_https_proxy" ] && [ "$npm_https_proxy" != "null" ]; then
        proxy_url="$npm_https_proxy"
    fi

    # 检查环境变量代理
    if [ -z "$proxy_url" ]; then
        if [ -n "$HTTP_PROXY" ]; then
            proxy_url="$HTTP_PROXY"
        elif [ -n "$HTTPS_PROXY" ]; then
            proxy_url="$HTTPS_PROXY"
        elif [ -n "$http_proxy" ]; then
            proxy_url="$http_proxy"
        elif [ -n "$https_proxy" ]; then
            proxy_url="$https_proxy"
        fi
    fi

    # 如果检测到代理，检查是否可用
    if [ -n "$proxy_url" ]; then
        # 解析代理地址 (支持 http://host:port 格式)
        proxy_url=$(echo "$proxy_url" | sed 's|^http://||' | sed 's|^https://||' | sed 's|/$||')
        proxy_host=$(echo "$proxy_url" | cut -d: -f1)
        proxy_port=$(echo "$proxy_url" | cut -d: -f2)

        if [ -n "$proxy_host" ] && [ -n "$proxy_port" ]; then
            echo_info "检测到代理配置：$proxy_host:$proxy_port"

            # 测试代理是否可用 (超时 2 秒)
            if nc -z -w 2 "$proxy_host" "$proxy_port" 2>/dev/null; then
                echo_info "代理连接正常"
            else
                echo_warn "代理 $proxy_host:$proxy_port 不可达，正在清除代理配置..."

                # 清除 npm/yarn 代理配置
                npm config delete proxy 2>/dev/null || true
                npm config delete https-proxy 2>/dev/null || true

                if command -v yarn > /dev/null 2>&1; then
                    yarn config delete proxy 2>/dev/null || true
                    yarn config delete https-proxy 2>/dev/null || true
                fi

                # 清除环境变量
                unset HTTP_PROXY HTTPS_PROXY http_proxy https_proxy ALL_PROXY all_proxy

                echo_info "代理配置已清除"
            fi
        fi
    fi
}

# 从配置文件中提取端口号
extract_ports_from_config() {
    local port_list=""
    
    # 查找脚本目录下所有 .properties, .yaml, .yml 文件
    local config_files=$(find "$SCRIPT_DIR" -maxdepth 1 -type f \( -name "*.properties" -o -name "*.yaml" -o -name "*.yml" \) 2>/dev/null)
    
    if [ -n "$config_files" ]; then
        for file in $config_files; do
            # 从 properties 文件中提取端口 (port=8080)
            local props_ports=$(grep -E "^[[:space:]]*port[[:space:]]*=" "$file" 2>/dev/null | \
                sed 's/.*=[[:space:]]*//' | tr -d '[:space:]' | grep -E '^[0-9]+$')
            
            # 从 yaml/yml 文件中提取端口 (port: 8080)
            local yaml_ports=$(grep -E "^[[:space:]]*port[[:space:]]*:" "$file" 2>/dev/null | \
                sed 's/.*:[[:space:]]*//' | tr -d '[:space:]' | grep -E '^[0-9]+$')
            
            if [ -n "$props_ports" ]; then
                port_list="$port_list $props_ports"
            fi
            if [ -n "$yaml_ports" ]; then
                port_list="$port_list $yaml_ports"
            fi
        done
    fi
    
    # 去重并返回
    echo "$port_list" | tr ' ' '\n' | sort -u | tr '\n' ' ' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'
}

# 检测 Node 进程使用的端口号
detect_ports_from_processes() {
    local port_list=""
    
    # 检测 Node 进程端口 (前端)
    if command -v lsof > /dev/null 2>&1; then
        local node_ports=$(lsof -i -P -n 2>/dev/null | grep -i "listen" | grep -i "node" | \
            awk '{print $9}' | cut -d: -f2 | grep -E '^[0-9]+$' | sort -u)
        if [ -n "$node_ports" ]; then
            port_list="$port_list $node_ports"
        fi
    fi
    
    # 去重并返回
    echo "$port_list" | tr ' ' '\n' | sort -u | tr '\n' ' ' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'
}

# 获取所有端口号
get_all_ports() {
    local config_ports=$(extract_ports_from_config)
    local process_ports=$(detect_ports_from_processes)
    
    local all_ports="$config_ports $process_ports"
    
    # 去重并返回
    echo "$all_ports" | tr ' ' '\n' | grep -v '^$' | sort -u | tr '\n' ' ' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'
}

show_help() {
    echo "用法：$0 [选项]"
    echo ""
    echo "选项:"
    echo "  -f, --force      强制重启 (先停止已有进程)"
    echo "  -h, --help       显示此帮助信息"
    echo ""
    echo "默认行为：启动前端服务。"
    echo ""
    echo "当前配置:"
    echo "  脚本目录：$SCRIPT_DIR"
    echo "  日志目录：$LOG_DIR"
    
    # 显示检测到的端口
    local ports=$(get_all_ports)
    if [ -n "$ports" ]; then
        echo "  检测到的端口：$ports"
    fi
    
    exit 0
}

# 检测运行模式
detect_mode() {
    if [ -d "$APP_DIR/src" ]; then echo 0; else echo 1; fi
}

IS_DEV_MODE=$(detect_mode)
FORCE_RESTART=false

# 参数解析
while [ $# -gt 0 ]; do
    case "$1" in
        -f|--force) FORCE_RESTART=true ;;
        -h|--help) show_help ;;
        *) ;;
    esac
    shift
done

cd "$APP_DIR"
[ ! -d "$LOG_DIR" ] && mkdir -p "$LOG_DIR"

# 如果指定了强制启动，停止已有进程
if [ "$FORCE_RESTART" = true ]; then
    echo_info "正在停止已有前端进程..."
    pkill -f "node.*vite\|node.*vue-cli-service\|npm run dev\|yarn serve" 2>/dev/null || true
    sleep 2
fi

# =============================================================================
# 前端启动逻辑
# =============================================================================
start_frontend() {
    echo_info ">>> 正在启动前端服务..."
    if [ $IS_DEV_MODE -eq 0 ]; then
        # 检测并修复代理配置
        check_and_fix_proxy

        # 仅监测 package.json 的变更 (意图变更)
        local files_to_hash="package.json"

        # 跨平台计算 MD5
        local current_hash=$(cat "$files_to_hash" 2>/dev/null | (md5sum 2>/dev/null || md5) | awk '{print $1}')
        local stored_hash=$(cat "$DEP_HASH_FILE" 2>/dev/null || echo "")

        if [ ! -d "node_modules" ] || [ "$current_hash" != "$stored_hash" ]; then
            echo_warn "检测到依赖变更，正在安装前端依赖..."

            # 解决证书过期问题
            export NODE_TLS_REJECT_UNAUTHORIZED=0

            # 定义可用镜像列表
            local registries=("https://registry.npmmirror.com" "https://registry.npmjs.org" "https://registry.yarnpkg.com")
            local success=false

            for reg in "${registries[@]}"; do
                echo_info "尝试使用镜像源：$reg"

                if command -v yarn > /dev/null 2>&1; then
                    yarn config set strict-ssl false
                    if yarn install --registry "$reg"; then
                        success=true
                        echo "$current_hash" > "$DEP_HASH_FILE"
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
                        echo "$current_hash" > "$DEP_HASH_FILE"
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

        # 检测端口配置 (支持 vite.config.js 和 vue.config.js)
        local port=""
        if [ -f "vite.config.js" ]; then
            port=$(grep "port:" vite.config.js | awk -F: '{print $2}' | tr -d ', ' | head -n 1)
        elif [ -f "vue.config.js" ]; then
            port=$(grep "port:" vue.config.js | awk -F: '{print $2}' | tr -d ', ' | head -n 1)
        fi
        [ -z "$port" ] && port="8000"

        echo_info "启动前端并监听端口：$port"
        echo_info "访问地址：http://localhost:$port"

        # 检测启动命令 (支持 serve 和 dev)
        local start_cmd=""
        if [ -f "package.json" ]; then
            if grep -q '"serve"' package.json; then
                start_cmd="serve"
            elif grep -q '"dev"' package.json; then
                start_cmd="dev"
            fi
        fi

        if [ -z "$start_cmd" ]; then
            start_cmd="serve"  # 默认使用 serve
        fi

        if command -v yarn > /dev/null 2>&1; then
            echo_info "执行：yarn $start_cmd"
            yarn $start_cmd --open
        else
            echo_info "执行：npm run $start_cmd"
            npm run $start_cmd -- --open
        fi
    else
        echo_info "模式：部署模式 (静态资源)"
    fi
}

# 执行启动
start_frontend

echo_info "前端服务已启动"