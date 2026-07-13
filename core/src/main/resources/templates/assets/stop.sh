#!/bin/bash

# 通用停止脚本 - 支持优雅停止和强制终止
# 支持多种停止策略和完善的错误处理

set -euo pipefail

# 全局变量定义
SERVICES_BASE_DIR="$HOME/webapps"
CURRENT_DIR="$(cd "$(dirname "$0")" && pwd)"
BASE_DIR="${CURRENT_DIR%/*}"
APP_NAME="$(basename "$BASE_DIR")"
PID_FILE="${BASE_DIR}/${APP_NAME}.pid"
# 优雅停止超时时间
readonly GRACEFUL_TIMEOUT=30
# 强制停止超时时间
readonly FORCE_TIMEOUT=10

# Nacos 配置
NACOS_SERVER=${NACOS_SERVER:-"nacos-server:8848"} # Nacos 服务器地址:端口
NACOS_CONTEXT_PATH=${NACOS_CONTEXT_PATH:-"/nacos"} # Nacos 的 Context Path
NACOS_NAMESPACE=${NACOS_NAMESPACE:-"public"}       # Nacos 命名空间
NACOS_GROUP=${NACOS_GROUP:-"DEFAULT_GROUP"}        # Nacos 分组

# 日志检测配置
LOG_CHECK_INTERVAL=${LOG_CHECK_INTERVAL:-0.5}      # 日志检测间隔（秒）
LOG_UNCHANGED_THRESHOLD=${LOG_UNCHANGED_THRESHOLD:-3}  # 日志无变化阈值（次数）
LOG_CHECK_TIMEOUT=${LOG_CHECK_TIMEOUT:-5}          # 日志检测超时时间（秒）

# 日志与帮助函数
show_help() {
    cat << EOF
用法: $0 [选项]

选项:
  -f, --force             强制停止（直接发送KILL信号）
  -i, --interactive       交互模式（需要用户确认）
  -s, --status            显示应用状态
  -h, --help              显示此帮助信息
  -n, --app-name <name>   (高级) 指定要停止的应用名称
  --base-dir <path>       (高级) 指定目标应用的根目录
  --all                   (批量) 停止位于 '$SERVICES_BASE_DIR' 目录下的所有服务
  --log-check             启用日志检测模式进行优雅停止（默认启用）
  --no-log-check          禁用日志检测模式，直接发送TERM信号

示例:
  $0                      # 优雅停止当前应用
  $0 -f                   # 强制停止当前应用
  $0 -i                   # 交互式停止当前应用
  $0 -s                   # 查看当前应用状态
  $0 --all                # 批量停止所有服务（优雅停止）
  $0 --all -f             # 批量强制停止所有服务
  $0 --app-name myapp --base-dir /path/to/app  # 停止指定应用
  $0 --log-check          # 启用日志检测的优雅停止
  $0 --no-log-check       # 不使用日志检测的优雅停止

环境变量:
  SERVICES_BASE_DIR       服务基础目录（默认: $HOME/webapps）
  GRACEFUL_TIMEOUT        优雅停止超时时间（默认: ${GRACEFUL_TIMEOUT}秒）
  FORCE_TIMEOUT           强制停止超时时间（默认: ${FORCE_TIMEOUT}秒）
  LOG_CHECK_INTERVAL      日志检测间隔（默认: ${LOG_CHECK_INTERVAL}秒）
  LOG_UNCHANGED_THRESHOLD 日志无变化阈值（默认: ${LOG_UNCHANGED_THRESHOLD}次）
  LOG_CHECK_TIMEOUT       日志检测超时时间（默认: ${LOG_CHECK_TIMEOUT}秒）

说明:
  优雅停止会先从 Nacos 注销服务，然后根据模式发送TERM信号，等待应用完成清理后自行退出；
  如果启用日志检测模式，会监控应用日志活动，当日志在指定时间内无变化时发送TERM信号；
  如果优雅停止超时，则会自动执行强制停止（发送KILL信号）。
  批量停止 (--all) 会扫描 '$SERVICES_BASE_DIR' 目录下所有正在运行的服务，
  并根据模式逐个停止（优雅或强制）。

EOF
}

log_info() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') [INFO] $*"
}

log_error() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') [ERROR] $*" >&2
}

log_warn() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') [WARN] $*"
}

log_debug() {
    # DEBUG日志默认不输出，除非设置了DEBUG环境变量
    if [ "${DEBUG:-0}" = "1" ]; then
        echo "$(date '+%Y-%m-%d %H:%M:%S') [DEBUG] $*" >&2
    fi
}

die() {
    log_error "$*"
    exit 1
}

# 辅助函数 (部分从 start.sh 引入)

# 交互式确认 (从 start.sh 引入)
prompt_confirmation() {
    local message="$1"
    local timeout=${2:-15}

    echo "$message (y/N) [${timeout}秒后默认取消]"

    if read -t $timeout -n 1 -r REPLY;
        then
        echo
        [[ $REPLY =~ ^[Yy]$ ]]
    else
        echo
        return 1
    fi
}

# 从进程命令行参数中提取端口
extract_port_from_process_args() {
    local pid=$1
    local java_cmd
    java_cmd=$(ps -p "$pid" -o args= 2>/dev/null || true)

    # 查找 --server.port= 或 -Dserver.port= 模式
    if [[ "$java_cmd" =~ --server\.port=([0-9]+) ]]; then
        local port="${BASH_REMATCH[1]}"
        # 检查端口是否应该被排除
        if ! is_port_excluded "$port"; then
            echo "$port"
            return 0
        fi
    elif [[ "$java_cmd" =~ -Dserver\.port=([0-9]+) ]]; then
        local port="${BASH_REMATCH[1]}"
        # 检查端口是否应该被排除
        if ! is_port_excluded "$port"; then
            echo "$port"
            return 0
        fi
    elif [[ "$java_cmd" =~ --port=([0-9]+) ]]; then
        local port="${BASH_REMATCH[1]}"
        # 检查端口是否应该被排除
        if ! is_port_excluded "$port"; then
            echo "$port"
            return 0
        fi
    fi

    return 1
}

# 检查端口是否应该被排除
is_port_excluded() {
    local port=$1
    # 排除222*开头的端口
    if [[ "$port" =~ ^222 ]]; then
        return 0  # 端口应该被排除
    fi
    return 1  # 端口不应该被排除
}

# 从 lsof 输出中提取端口
extract_port_from_lsof() {
    local pid=$1
    local specific_port_only=${2:-false}

    if command -v lsof >/dev/null 2>&1; then
        if [ "$specific_port_only" = true ]; then
            # 获取单个端口
            local lsof_output
            lsof_output=$(lsof -i -P -n -p "$pid" | grep LISTEN | head -n 1 || true)
            if [ -n "$lsof_output" ]; then
                # 提取端口号 (格式如: java 12345 user 123u IPv4 123456 0t0 TCP *:8080 (LISTEN))
                local port
                port=$(echo "$lsof_output" | awk '{print $9}' | sed 's/.*://')
                # 检查端口是否应该被排除
                if ! is_port_excluded "$port"; then
                    echo "$port"
                    return 0
                fi
            fi
        else
            # 获取所有端口并优先选择非系统端口，排除222*开头的端口
            local all_ports
            all_ports=$(lsof -i -P -n -p "$pid" | grep LISTEN | awk '{print $9}' | sed 's/.*://' | grep -E '^[0-9]+$' | sort -n)

            # 过滤掉222*开头的端口
            local filtered_ports
            filtered_ports=$(echo "$all_ports" | while read -r port; do
                if [ -n "$port" ] && ! is_port_excluded "$port"; then
                    echo "$port"
                fi
            done)

            # 优先选择非系统端口
            local main_port
            main_port=$(echo "$filtered_ports" | head -n 1)
            if [ -n "$main_port" ] && [ "$main_port" -gt 1024 ]; then
                echo "$main_port"
                return 0
            else
                # 否则获取第一个可用端口
                local first_port
                first_port=$(echo "$filtered_ports" | head -n 1)
                if [ -n "$first_port" ]; then
                    echo "$first_port"
                    return 0
                fi
            fi
        fi
    fi
    return 1
}

# 从 netstat 输出中提取端口
extract_port_from_netstat() {
    local pid=$1
    local specific_port_only=${2:-false}

    if command -v netstat >/dev/null 2>&1; then
        if [ "$specific_port_only" = true ]; then
            # 获取单个端口
            local netstat_output
            netstat_output=$(netstat -tlnp 2>/dev/null | grep LISTEN | grep "$pid" | head -n 1 || true)
            if [ -n "$netstat_output" ]; then
                # 提取端口号 (格式如: tcp6 0 0 :::8080 :::* LISTEN 12345/java)
                local port
                port=$(echo "$netstat_output" | awk '{print $4}' | sed 's/.*://')
                # 检查端口是否应该被排除
                if ! is_port_excluded "$port"; then
                    echo "$port"
                    return 0
                fi
            fi
        else
            # 获取所有端口并优先选择非系统端口，排除222*开头的端口
            local all_ports
            all_ports=$(netstat -tlnp 2>/dev/null | grep LISTEN | grep "$pid" | awk '{print $4}' | sed 's/.*://' | grep -E '^[0-9]+$' | sort -n)

            # 过滤掉222*开头的端口
            local filtered_ports
            filtered_ports=$(echo "$all_ports" | while read -r port; do
                if [ -n "$port" ] && ! is_port_excluded "$port"; then
                    echo "$port"
                fi
            done)

            # 优先选择非系统端口
            local main_port
            main_port=$(echo "$filtered_ports" | head -n 1)
            if [ -n "$main_port" ] && [ "$main_port" -gt 1024 ]; then
                echo "$main_port"
                return 0
            else
                # 否则获取第一个可用端口
                local first_port
                first_port=$(echo "$filtered_ports" | head -n 1)
                if [ -n "$first_port" ]; then
                    echo "$first_port"
                    return 0
                fi
            fi
        fi
    fi
    return 1
}

# 获取应用端口 (从 start.sh 引入)
get_application_port() {
    local port=""
    local pid_info
    if pid_info=$(get_application_pid); then
        local pid
        pid=$(echo "$pid_info" | cut -d'|' -f1)

        # 1. 首先尝试从进程命令行参数中获取端口
        if [ -z "$port" ]; then
            port=$(extract_port_from_process_args "$pid" 2>/dev/null || true)
        fi

        # 2. 如果失败，尝试使用 lsof 获取单个端口
        if [ -z "$port" ]; then
            port=$(extract_port_from_lsof "$pid" true 2>/dev/null || true)
        fi

        # 3. 如果失败，尝试使用 netstat 获取单个端口
        if [ -z "$port" ]; then
            port=$(extract_port_from_netstat "$pid" true 2>/dev/null || true)
        fi

        # 4. 如果失败，尝试使用 lsof 获取多个端口并选择合适的端口
        if [ -z "$port" ]; then
            port=$(extract_port_from_lsof "$pid" false 2>/dev/null || true)
        fi

        # 5. 如果失败，尝试使用 netstat 获取多个端口并选择合适的端口
        if [ -z "$port" ]; then
            port=$(extract_port_from_netstat "$pid" false 2>/dev/null || true)
        fi
    fi

    echo "$port"
}

# 检查端口是否监听 (从 start.sh 引入)
check_port_listening() {
    local port=$1
    if ! [[ "$port" =~ ^[0-9]+$ ]]; then
        return 1
    fi

    if command -v ss >/dev/null 2>&1;
        then ss -tln | grep -q ":${port} "
    elif command -v netstat >/dev/null 2>&1;
        then netstat -tln | grep -q ":${port} "
    else
        return 1 # 如果没有ss或netstat，则无法检查
    fi
}

# 获取应用进程ID
get_application_pid() {
    local pid=""
    local method=""

    # 策略1: pgrep 关键字
    pid=$(pgrep -f "java.*${APP_NAME}.*.jar" 2>/dev/null | head -n 1 || echo "")
    if [ -n "$pid" ]; then
        method="pgrep关键字"
        echo "$pid|$method"
        return 0
    fi

    # 策略2: PID文件
    if [ -f "$PID_FILE" ]; then
        local file_pid
        file_pid=$(cat "$PID_FILE" 2>/dev/null || echo "")
        if [ -n "$file_pid" ] && kill -0 "$file_pid" 2>/dev/null; then
            pid=$file_pid
            method="PID文件"
            echo "$pid|$method"
            return 0
        else
            [ -n "$file_pid" ] && log_warn "PID文件中的进程 ($file_pid) 不存在或已过期，将删除PID文件"
            rm -f "$PID_FILE"
        fi
    fi
    
    # 策略3: ps 命令
    pid=$(ps -ef | grep "java" | grep "$APP_NAME" | grep -v grep | awk '{print $2}' | head -n 1 || echo "")
    if [ -n "$pid" ]; then
        method="ps命令"
        echo "$pid|$method"
        return 0
    fi

    return 1
}

# 检查进程是否存在
is_process_running() {
    local pid=$1
    kill -0 "$pid" 2>/dev/null
}

# 等待进程停止
wait_for_process_stop() {
    local pid=$1
    local timeout=$2
    local interval=1
    local elapsed=0

    while [ $elapsed -lt $timeout ]; do
        if ! is_process_running "$pid"; then
            return 0
        fi

        sleep $interval
        elapsed=$((elapsed + interval))

        if [ $((elapsed % 5)) -eq 0 ]; then
            echo -ne "\r$(date '+%Y-%m-%d %H:%M:%S') [INFO] 等待进程停止... (${elapsed}/${timeout}秒)"
        fi
    done
    echo # 换行
    return 1
}

find_latest_log_file() {
	local latest_log_file=$(find "$1" -name "*${APP_NAME}*.log" -type f -not -path "*/tmp/*" -not -path "*/temp/*" -not -path "*/cache/*" -printf '%T@ %p\n' 2>/dev/null | sort -n | tail -1 | cut -d' ' -f2-)
    if [ -n "$latest_log_file" ] && [ -f "$latest_log_file" ]; then
        echo "$latest_log_file"
        return 0
    fi
    return 1
}
# 获取应用日志文件路径
get_log_file_path() {
    local default_log_file="${BASE_DIR}/logs/${APP_NAME}/${APP_NAME}_default.log"
    if [ -f "$default_log_file" ]; then
        echo "$default_log_file"
        return 0
    fi
    # 在应用目录下查找最新的日志文件
    local latest_log_file=$(find_latest_log_file "$BASE_DIR")
    if [ -n "$latest_log_file" ] && [ -f "$latest_log_file" ]; then
        echo "$latest_log_file"
        return 0
    fi

    # 在用户目录下查找包含应用名称的日志文件
    if [ -d "$HOME/logs" ]; then
        local user_log_file=$(find_latest_log_file "$HOME")
        if [ -n "$user_log_file" ] && [ -f "$user_log_file" ]; then
            echo "$user_log_file"
            return 0
        fi
    fi
    # 没有找到日志文件则返回空
    return 1
}

# 日志检测函数
log_check_stop() {
    local pid=$1
    local log_file
    log_file=$(get_log_file_path)

    if [ -z "$log_file" ] || [ ! -f "$log_file" ]; then
        log_warn "无法找到日志文件，跳过日志检测，直接发送TERM信号"
        # 直接发送TERM信号然后等待
        kill -TERM "$pid" || {
            log_error "发送TERM信号失败"
            return 1
        }
        return 0
    fi

    log_info "开始日志检测 ($(basename "$log_file"))"

    # 记录初始日志大小和时间
    local initial_size
    initial_size=$(stat -c%s "$log_file" 2>/dev/null || echo "0")
    local unchanged_count=0
    local elapsed=0
    local start_time
    start_time=$(date +%s.%N)

    # 保存初始几行内容用于比较
    local initial_content
    initial_content=$(tail -n 20 "$log_file" 2>/dev/null || echo "")

    while [ "${elapsed:-0}" -lt "${LOG_CHECK_TIMEOUT:-5}" ]; do
        sleep $LOG_CHECK_INTERVAL
        local calc_elapsed=$(echo "$(date +%s.%N) - $start_time" | bc -l 2>/dev/null)
        if [ -n "$calc_elapsed" ]; then
            elapsed=${calc_elapsed%.*}  # 取整数部分
        else
            elapsed=$(echo "$elapsed $LOG_CHECK_INTERVAL" | awk '{print int($1 + $2)}')  # 备用计算方式
        fi

        local current_size
        current_size=$(stat -c%s "$log_file" 2>/dev/null || echo "0")
        local current_content
        current_content=$(tail -n 20 "$log_file" 2>/dev/null || echo "")

        if [ "$current_size" -eq "$initial_size" ] && [ "$current_content" = "$initial_content" ]; then
            # 日志没有变化
            unchanged_count=$((unchanged_count + 1))
            if [ "${unchanged_count:-0}" -ge "${LOG_UNCHANGED_THRESHOLD:-3}" ]; then
                local log_check_duration
                log_check_duration=$(echo "$LOG_CHECK_INTERVAL $LOG_UNCHANGED_THRESHOLD" | awk '{print $1 * $2}')
                log_info "日志在 ${log_check_duration} 秒内没有变化，发送TERM信号"
                kill -TERM "$pid" || {
                    log_error "发送TERM信号失败"
                    return 1
                }
                return 0
            fi
        else
            # 日志有变化，重置计数
            initial_size=$current_size
            initial_content="$current_content"
            unchanged_count=0
        fi

        if [ "${elapsed:-0}" -ge "${LOG_CHECK_TIMEOUT:-5}" ]; then
            break
        fi
    done

    log_info "日志检测超时(${LOG_CHECK_TIMEOUT}秒)，发送TERM信号"
    kill -TERM "$pid" || {
        log_error "发送TERM信号失败"
        return 1
    }
    return 0
}

# 优雅停止应用
graceful_stop() {
    local pid=$1

    log_info "进行优雅停止 (PID: $pid)"

    # 根据是否启用日志检测来决定停止方式
    if [ "${LOG_CHECK_ENABLED:-false}" = true ]; then
        log_check_stop "$pid"
    else
        # 传统方式：直接发送TERM信号
        kill -TERM "$pid" || {
            log_error "发送TERM信号失败"
            return 1
        }
    fi

    if wait_for_process_stop "$pid" "${GRACEFUL_TIMEOUT:-30}"; then
        log_info "应用已优雅停止"
        return 0
    else
        log_warn "优雅停止超时 (${GRACEFUL_TIMEOUT:-30}秒)"
        return 1
    fi
}

# 强制停止应用
force_stop() {
    local pid=$1

    log_warn "发送KILL信号进行强制停止 (PID: $pid)"
    kill -KILL "$pid" || {
        log_error "发送KILL信号失败"
        return 1
    }

    if wait_for_process_stop "$pid" "${FORCE_TIMEOUT:-10}"; then
        log_info "应用已强制停止"
        return 0
    else
        log_error "强制停止失败"
        return 1
    fi
}

# 清理与报告

# 清理资源
cleanup_resources() {
    # log_info "清理应用资源..."
    # 删除PID文件
    if [ -f "$PID_FILE" ]; then
        rm -f "$PID_FILE"
        log_info "已删除PID文件: $PID_FILE"
    fi

    # log_info "清理临时文件..."
    find "$BASE_DIR" -maxdepth 1 -name '*.tmp' -delete 2>/dev/null || true
    find /tmp -maxdepth 1 -name "${APP_NAME}_*" -delete 2>/dev/null || true

    # 释放端口资源
    release_port_resources

    # log_info "资源清理完成"
}

# Nacos 服务注销
unregister_from_nacos() {
    local port
    port=$(get_application_port 2>/dev/null || echo "")

    if [ -z "$port" ]; then
        log_warn "无法获取应用端口，跳过 Nacos 注销"
        return 0
    fi

    # 从应用的配置中获取服务名，如果没找到则使用 APP_NAME
    local service_name="$APP_NAME"  # 初始化为默认值
    local config_file_yml="${BASE_DIR}/config/application.yml"
    local config_file_yaml="${BASE_DIR}/config/application.yaml"

    if [ -f "$config_file_yml" ]; then
        local temp_service_name
        temp_service_name=$(grep -E "^\s*application:\s*name:\s*" "$config_file_yml" | sed -n 's/.*name:\s*//p' | tr -d ' \t' | head -n 1)
        if [ -n "$temp_service_name" ]; then
            service_name="$temp_service_name"
        fi
    elif [ -f "$config_file_yaml" ]; then
        local temp_service_name
        temp_service_name=$(grep -E "^\s*application:\s*name:\s*" "$config_file_yaml" | sed -n 's/.*name:\s*//p' | tr -d ' \t' | head -n 1)
        if [ -n "$temp_service_name" ]; then
            service_name="$temp_service_name"
        fi
    fi

    if [ "$service_name" = "$APP_NAME" ]; then
        # 尝试从 spring.application.name 获取
        if [ -f "$config_file_yml" ]; then
            local temp_service_name
            temp_service_name=$(grep -E "^\s*spring:\s*application:\s*name:\s*" "$config_file_yml" | sed -n 's/.*name:\s*//p' | tr -d ' \t' | head -n 1)
            if [ -n "$temp_service_name" ]; then
                service_name="$temp_service_name"
            fi
        elif [ -f "$config_file_yaml" ]; then
            local temp_service_name
            temp_service_name=$(grep -E "^\s*spring:\s*application:\s*name:\s*" "$config_file_yaml" | sed -n 's/.*name:\s*//p' | tr -d ' \t' | head -n 1)
            if [ -n "$temp_service_name" ]; then
                service_name="$temp_service_name"
            fi
        fi
    fi

    if [ "$service_name" != "$APP_NAME" ]; then
        log_debug "服务名: $service_name"
    else
        log_warn "应用名: $service_name"
    fi

    # 获取本机 IP 地址
    local ip
    ip=$(hostname -I | awk '{print $1}' 2>/dev/null || echo "127.0.0.1")

    log_info "从Nacos下线服务: $service_name, IP: $ip, 端口: $port"

    # Nacos 注销 API
    local unregister_url="http://${NACOS_SERVER}${NACOS_CONTEXT_PATH}/v1/ns/instance"
    local unregister_response=$(curl --noproxy "*" -s -w "\n%{http_code}" --max-time 10 -X DELETE "$unregister_url?serviceName=${service_name}&ip=${ip}&port=${port}&namespaceId=${NACOS_NAMESPACE}&groupName=${NACOS_GROUP}" 2>/dev/null)

    local http_code
    http_code=$(echo "$unregister_response" | tail -n1)
    local body
    body=$(echo "$unregister_response" | head -n -1)

    if [ "$http_code" == "200" ] || [ "$body" == "ok" ]; then
        log_info "成功从Nacos下线服务: $service_name"
    else
        log_warn "Nacos下线服务失败 (HTTP $http_code): $body"
        log_warn "服务可能未在 Nacos 中注册，或 Nacos 服务器不可达"
        # 即使注销失败也继续停止应用，因为这不影响应用本身的停止
    fi
    return 0
}

# 释放端口资源
release_port_resources() {
    local port
    port=$(get_application_port 2>/dev/null || echo "")

    if [ -n "$port" ]; then
        if check_port_listening "$port"; then
            log_warn "端口 $port 仍被占用，可能需要手动处理"
        else
            log_info "端口 $port 已释放"
        fi
    fi
}

# 生成停止报告
generate_stop_report() {
    local stop_result=$1
    local pid=$2
    local stop_method=$3
    local report_file="/tmp/${APP_NAME}_stop_report_$(date +%Y%m%d_%H%M%S).log"

    {
        echo "应用停止报告 "
        echo "时间: $(date)"
        echo "应用: $APP_NAME"
        echo "PID: $pid"
        echo "停止方法: $stop_method"
        echo "停止结果: $([ $stop_result -eq 0 ] && echo "成功" || echo "失败")"
        echo "基础目录: $BASE_DIR"
    } > "$report_file"

    log_info "停止报告已生成: $report_file"
}

# 显示应用状态
show_application_status() {
    log_info "应用状态信息 "
    log_info "应用名称: $APP_NAME"
    log_info "应用目录: $BASE_DIR"

    local pid
    if pid=$(get_application_pid);
        then
        log_info "进程状态: 运行中 (PID: $pid)"
        if command -v ps >/dev/null 2>&1;
            then
            log_info "进程信息:"
            ps -fp "$pid" 2>/dev/null | sed 's/^/  /'
        fi
        local memory_mb cpu_percent
        memory_mb=$(ps -o rss= -p "$pid" 2>/dev/null | awk '{print int($1/1024)}' || echo "N/A")
        cpu_percent=$(ps -o %cpu= -p "$pid" 2>/dev/null | tr -d ' ' || echo "N/A")
        log_info "资源使用: 内存=${memory_mb}MB, CPU=${cpu_percent}%"
        return 0
    else
        log_info "进程状态: 未运行"
        return 1
    fi
}

# 主停止逻辑
stop_application() {
    local force_mode=${1:-false}
    local interactive=${2:-false}

    log_info "开始停止应用: $APP_NAME "

    local pid_info
    if ! pid_info=$(get_application_pid); then
        log_info "应用未运行或已停止"
        [ -f "$PID_FILE" ] && rm -f "$PID_FILE"
        return 0
    fi

    local pid
    local method
    pid=$(echo "$pid_info" | cut -d'|' -f1)
    method=$(echo "$pid_info" | cut -d'|' -f2)

    # log_info "通过[${method}]发现运行中的应用进程 (PID: $pid)"

    if [ "$interactive" = true ]; then
        if ! prompt_confirmation "确定要停止应用 $APP_NAME (PID: $pid) 吗？"; then
            log_info "用户取消停止操作"
            return 0
        fi
    fi

    # 优雅停止模式下先从 Nacos 注销服务
    if [ "$force_mode" = false ]; then
        log_info "执行 Nacos 服务优雅下线..."
        unregister_from_nacos
        # 稍等一下让注销生效
        sleep 2
    fi

    local stop_result=1
    local stop_method=""

    if [ "$force_mode" = true ]; then
        stop_method="force_kill"
        force_stop "$pid"
        stop_result=$?
    else
        stop_method="graceful_then_force"
        if graceful_stop "$pid"; then
            stop_result=0
        else
            log_warn "优雅停止失败，尝试强制停止..."
            force_stop "$pid"
            stop_result=$?
        fi
    fi

    if [ $stop_result -eq 0 ]; then
        log_debug "应用停止成功"
        cleanup_resources
    else
        log_error "应用停止失败"
    fi

    # generate_stop_report $stop_result "$pid" "$stop_method"
    return $stop_result
}

# 批量停止所有服务
batch_stop_all_services() {
    local force_mode=${1:-false}
    log_info "开始批量扫描位于 ${SERVICES_BASE_DIR} 下的服务..."
    
    if [ ! -d "$SERVICES_BASE_DIR" ]; then
        log_warn "服务基础目录 ${SERVICES_BASE_DIR} 不存在。"
        return 1
    fi

    local service_dirs
    service_dirs=$(find "$SERVICES_BASE_DIR" -maxdepth 1 -mindepth 1 -type d || true)
    if [ -z "$service_dirs" ]; then
        log_info "在 ${SERVICES_BASE_DIR} 目录下未找到任何服务子目录。"
        return 0
    fi

    local services_to_stop=()
    # 1. 收集所有正在运行的服务信息
    for service_dir in $service_dirs; do
        local temp_app_name
        local temp_base_dir
        local temp_pid_file
        temp_app_name=$(basename "$service_dir")
        temp_base_dir=$service_dir
        temp_pid_file="${temp_base_dir}/${temp_app_name}.pid"
        
        local pid_info
        pid_info=$(APP_NAME="$temp_app_name" BASE_DIR="$temp_base_dir" PID_FILE="$temp_pid_file" get_application_pid || true)
        
        if [ -n "$pid_info" ]; then
            local pid
            pid=$(echo "$pid_info" | cut -d'|' -f1)
            local port
            port=$(APP_NAME="$temp_app_name" BASE_DIR="$temp_base_dir" get_application_port || true)
            services_to_stop+=("$temp_app_name|$pid|${port:-N/A}")
        fi
    done

    # 2. 打印统计信息
    local total_services=${#services_to_stop[@]}
    if [ "$total_services" -eq 0 ]; then
        log_info "未发现正在运行的服务。"
        return 0
    fi
    
    log_info "扫描到 ${total_services} 个正在运行的服务:"
    printf "%-25s %-10s %-8s\n" "服务名" "PID" "端口"
    printf "%-25s %-10s %-8s\n" "-------------------------" "----------" "--------"
    for service_info in "${services_to_stop[@]}"; do
        IFS='|' read -r service_name pid port <<< "$service_info"
        printf "%-25s %-10s %-8s\n" "$service_name" "$pid" "$port"
    done

    # 3. 根据是否为强制模式，选择不同的停止策略
    if [ "$force_mode" = true ]; then
        # --- 批量强制停止 ---
        log_info "将【批量强制】停止以上所有服务..."
        local pids_to_kill=()
        for service_info in "${services_to_stop[@]}"; do
            pids_to_kill+=("$(echo "$service_info" | cut -d'|' -f2)")
        done
        
        if [ ${#pids_to_kill[@]} -gt 0 ]; then
            if kill -9 "${pids_to_kill[@]}"; then
                log_info "批量强制停止命令已执行。"
            else
                log_error "批量强制停止命令执行失败。"
            fi
        fi
        
        # log_info "清理PID文件..."
        for service_info in "${services_to_stop[@]}"; do
            local service_name
            service_name=$(echo "$service_info" | cut -d'|' -f1)
            local service_pid_file="${SERVICES_BASE_DIR}/${service_name}/${service_name}.pid"
            [ -f "$service_pid_file" ] && rm -f "$service_pid_file"
        done
    else
        # --- 逐个优雅停止 ---
        log_info "将按顺序【逐个】停止以上服务..."
        local failed_stops=0
        for service_info in "${services_to_stop[@]}"; do
            IFS='|' read -r service_name pid port <<< "$service_info"
            
            printf "  %-28s (PID: %-10s) ... " "停止中: ${service_name}" "$pid"
            
            local stop_result=1
            if ! graceful_stop "$pid"; then
                if force_stop "$pid"; then stop_result=0; fi
            else
                stop_result=0
            fi

            if [ "$stop_result" -eq 0 ]; then
                printf "\e[32m成功\e[0m\n"
                local service_pid_file="${SERVICES_BASE_DIR}/${service_name}/${service_name}.pid"
                [ -f "$service_pid_file" ] && rm -f "$service_pid_file"
            else
                printf "\e[31m失败\e[0m\n"
                failed_stops=$((failed_stops + 1))
            fi
        done

        echo
        if [ "$failed_stops" -gt 0 ]; then
            log_error "${failed_stops} 个服务停止失败，请检查上面的日志。"
            return 1
        fi
    fi

    log_info "所有服务均已成功停止。"
    return 0
}

# 脚本入口
main() {
    local force_mode=false
    local interactive=false
    local show_status=false
    local target_app_name=""
    local target_base_dir=""
    local batch_mode=false
    local log_check_enabled=true  # 默认启用日志检测

    # 如果没有参数，直接执行默认停止
    if [ $# -eq 0 ]; then
        LOG_CHECK_ENABLED=$log_check_enabled
        stop_application false false
        exit $?
    fi

    while [[ $# -gt 0 ]]; do
        case $1 in
            -f|--force) force_mode=true; shift ;;
            -i|--interactive) interactive=true; shift ;;
            -s|--status) show_status=true; shift ;;
            -h|--help) show_help; exit 0 ;;
            -n|--app-name) target_app_name="$2"; shift 2 ;;
            --base-dir) target_base_dir="$2"; shift 2 ;;
            --all) batch_mode=true; shift ;;
            --log-check) log_check_enabled=true; shift ;;
            --no-log-check) log_check_enabled=false; shift ;;
            *)
                log_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done

    # 批量模式
    if [ "$batch_mode" = true ]; then
        batch_stop_all_services "$force_mode"
        exit $?
    fi

    # 如果传递了高级参数，则覆盖默认的应用上下文
    if [ -n "$target_app_name" ]; then
        APP_NAME="$target_app_name"
    fi
    if [ -n "$target_base_dir" ]; then
        BASE_DIR="$target_base_dir"
        # 如果只提供了 base-dir，则从中推断 app-name
        if [ -z "$target_app_name" ]; then
            APP_NAME=$(basename "$target_base_dir")
        fi
    fi
    # 基于最终的BASE_DIR重新计算PID_FILE路径
    PID_FILE="${BASE_DIR}/${APP_NAME}.pid"

    # 设置日志检测全局变量
    LOG_CHECK_ENABLED=$log_check_enabled

    if [ "$show_status" = true ]; then
        show_application_status
        exit $?
    fi

    stop_application "$force_mode" "$interactive"
    exit $?
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
