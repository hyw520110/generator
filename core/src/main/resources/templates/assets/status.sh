#!/bin/bash

# 应用状态检查和管理脚本
# 显示所有应用状态列表，并支持通过编号或应用名进行批量启动/停止

set -euo pipefail

# --- 全局变量定义 ---
readonly CURRENT_DIR="$(cd "$(dirname "$0")" && pwd)"
SERVICES_BASE_DIR="${SERVICES_BASE_DIR:-$HOME/webapps}"

# --- 颜色定义 ---
readonly COLOR_RESET=$'\033[0m'
readonly COLOR_RED=$'\033[31m'
readonly COLOR_GREEN=$'\033[32m'
readonly COLOR_YELLOW=$'\033[33m'
readonly COLOR_BLUE=$'\033[34m'
readonly COLOR_MAGENTA=$'\033[35m'
readonly COLOR_CYAN=$'\033[36m'
readonly COLOR_WHITE=$'\033[37m'
readonly COLOR_BOLD=$'\033[1m'
readonly COLOR_DIM=$'\033[2m'

# --- 全局选项变量 ---
SHOW_COLOR=true
SORT_BY="index"  # index, status, start_time, duration
FILTER_STATUS=""  # running, stopped, or empty for all
SHOW_DETAILS=false
WATCH_INTERVAL=""  # --watch 模式刷新间隔（秒）

show_help() {
    cat << EOF
应用状态检查和管理脚本，用于查看和批量管理应用

用法: $0 [选项] [命令]

选项:
  -d, --base-dir <目录>      服务基础目录，默认: $HOME/webapps
  --color                    启用彩色输出 (默认)
  --no-color                 禁用彩色输出
  --sort-by <字段>           按字段排序: index(编号), status(状态), start_time(启动时间), duration(运行时长)
  --filter <状态>            过滤服务: running(运行), stopped(停止)
  --details                  显示详细信息
  --watch, -w [秒]           实时刷新模式，默认 2 秒刷新
  -h, --help                 显示此帮助信息

命令:
  list                       列出所有应用状态 (默认)
  start <应用名或编号>...    批量启动指定的应用
  stop <应用名或编号>...     批量停止指定的应用
  restart <应用名或编号>...  批量重启指定的应用（先停止再启动）
  health                     健康检查所有服务（/actuator/health）

示例:
  $0                         # 列出所有应用状态
  $0 list                    # 列出所有应用状态
  $0 --sort-by status list   # 按状态排序列出应用
  $0 --filter running list   # 只显示运行中的应用
  $0 --details list          # 显示详细信息
  $0 --watch 3               # 每 3 秒刷新状态列表
  $0 start <应用名>          # 启动指定应用
  $0 start 1 3 5            # 启动编号为1、3、5的应用
  $0 stop <应用名>          # 停止指定应用
  $0 stop 2 4               # 停止编号为2、4的应用
  $0 restart <应用名>       # 重启指定应用
  $0 restart 1 3            # 重启编号为1、3的应用
  $0 health                  # 健康检查所有服务

EOF
}

get_dependency_file() {
    if [ -f "$CURRENT_DIR/.service-order.conf" ]; then
        echo "$CURRENT_DIR/.service-order.conf"
    elif [ -f "${CURRENT_DIR%/*}/.service-order.conf" ]; then
        echo "${CURRENT_DIR%/*}/.service-order.conf"
    fi
}

# 通用日期格式化函数
format_current_time() {
    date '+%Y-%m-%d %H:%M:%S'
}

# 通用日志函数
log_message() {
    local level=$1
    shift
    case "$level" in
        "ERROR")
            printf "\033[31m%s\033[0m\n" "$(format_current_time) [$level] $*" >&2
            ;;
        "WARN")
            printf "\033[33m%s\033[0m\n" "$(format_current_time) [$level] $*" >&2
            ;;
        *)
            # INFO和其他级别正常显示
            echo "$(format_current_time) [$level] $*"
            ;;
    esac
}

# 通用服务列表获取函数
get_service_list() {
    local service_source=$1
    local service_array=()

    local dependency_file
    dependency_file=$(get_dependency_file)
    if [ "$service_source" = "config" ] && [ -n "$dependency_file" ]; then
        source "$dependency_file"
        service_array=("${SERVICE_DEPENDENCY_ORDER[@]}")
        echo "${service_array[*]}"
        return
    fi

    if [ ! -d "$SERVICES_BASE_DIR" ]; then
        # 服务基础目录不存在，尝试从进程获取服务列表
        # 获取所有运行的Java应用的进程信息，按启动时间排序
        while IFS= read -r line; do
            local pid=$(echo "$line" | awk '{print $2}')
            # 提取命令部分，使用awk从第6个字段开始提取
            local command=$(echo "$line" | awk '{for(i=6; i<=NF; i++) printf "%s", $i; if(i<NF) printf " "}')
            # 提取服务名，使用SERVICES_BASE_DIR变量
            if [[ "$command" =~ .*$SERVICES_BASE_DIR/([^/]+)/.* ]]; then
                local service_name="${BASH_REMATCH[1]}"
                # 检查是否已添加此服务
                local exists=false
                for existing_service in "${service_array[@]}"; do
                    if [[ "$existing_service" == "$service_name" ]]; then
                        exists=true
                        break
                    fi
                done
                if [[ "$exists" == false ]]; then
                    service_array+=("$service_name")
                fi
            fi
        done < <(ps -eo pid,lstart,cmd | grep java | grep "$SERVICES_BASE_DIR" | grep -v grep | sort -k2,3 -k4,5n -k6,6n -k7,7n -k8,8n)

        # 如果没有从进程获取到服务，尝试更通用的Java进程识别
        if [ ${#service_array[@]} -eq 0 ]; then
            while IFS= read -r line; do
                local pid=$(echo "$line" | awk '{print $2}')
                # 提取命令部分，使用awk从第6个字段开始提取
                local command=$(echo "$line" | awk '{for(i=6; i<=NF; i++) printf "%s", $i; if(i<NF) printf " "}')
                # 尝试从启动命令中提取应用名
                if [[ "$command" =~ .*target/([^/]+)[.]jar ]]; then
                    local service_name="${BASH_REMATCH[1]}"
                    local exists=false
                    for existing_service in "${service_array[@]}"; do
                        if [[ "$existing_service" == "$service_name" ]]; then
                            exists=true
                            break
                        fi
                    done
                    if [[ "$exists" == false ]]; then
                        service_array+=("$service_name")
                    fi
                elif [[ "$command" =~ .*Application ]]; then
                    # 如果没有jar文件名，尝试从其他方式提取服务名
                    local service_name="java_app_$(echo "$command" | md5sum | cut -c1-8)"
                    local exists=false
                    for existing_service in "${service_array[@]}"; do
                        if [[ "$existing_service" == "$service_name" ]]; then
                            exists=true
                            break
                        fi
                    done
                    if [[ "$exists" == false ]]; then
                        service_array+=("$service_name")
                    fi
                fi
            done < <(ps -eo pid,lstart,cmd | grep java | grep -v grep | sort -k2,3 -k4,5n -k6,6n -k7,7n -k8,8n)
        fi
    else
        # 服务基础目录存在，列出目录中的所有服务
        for item in "$SERVICES_BASE_DIR"/*/; do
            if [ -d "$item" ]; then
                local service_name=$(basename "$item")
                service_array+=("$service_name")
            fi
        done
    fi

    echo "${service_array[*]}"
}

# 通用字符串去重，保留原始顺序。
# 兼容 macOS /bin/bash 3.2，不能使用 bash 4+ 的 declare -A。
unique_array() {
    local input_array=("$@")
    local unique_items=()

    for item in "${input_array[@]+"${input_array[@]}"}"; do
        local exists=false
        local unique_item
        for unique_item in "${unique_items[@]+"${unique_items[@]}"}"; do
            if [ "$unique_item" = "$item" ]; then
                exists=true
                break
            fi
        done
        [ "$exists" = false ] || continue
        unique_items+=("$item")
    done

    echo "${unique_items[@]+"${unique_items[@]}"}"
}

load_ordered_services() {
    local services=()
    if [ -n "$(get_dependency_file)" ]; then
        read -ra services <<< "$(get_service_list "config")"
    else
        read -ra services <<< "$(get_service_list "auto")"
    fi
    echo "${services[@]+"${services[@]}"}"
}

# 带颜色的输出函数
color_output() {
    local color=$1
    local text=$2
    if [ "$SHOW_COLOR" = true ]; then
        printf "%s%s%s\n" "${color}" "${text}" "${COLOR_RESET}"
    else
        echo "$text"
    fi
}

# 根据状态返回对应的颜色
get_status_color() {
    local status=$1
    case "$status" in
        "运行")
            echo "$COLOR_GREEN"
            ;;
        "停止")
            echo "$COLOR_RED"
            ;;
        *)
            echo "$COLOR_YELLOW"
            ;;
    esac
}

# 服务排序函数
sort_services() {
    local services=("$@")
    local sorted_services=()

    case "$SORT_BY" in
        "status")
            # 按状态排序
            local running_services=()
            local stopped_services=()

            for service in "${services[@]}"; do
                local service_pid=$(get_pid "$service")
                local status=$(get_status "$service" "$service_pid")
                if [ "$status" = "运行" ]; then
                    running_services+=("$service")
                else
                    stopped_services+=("$service")
                fi
            done

            # 合并数组
            sorted_services=("${running_services[@]}" "${stopped_services[@]}")
            ;;
        "start_time")
            # 按启动时间排序
            # 创建临时文件存储服务和启动时间戳
            local temp_file=$(mktemp)
            for service in "${services[@]}"; do
                local service_pid=$(get_pid "$service")
                local time_info=$(get_start_time_and_duration "$service_pid")
                local start_timestamp=$(echo "$time_info" | cut -d'|' -f2)
                echo "$start_timestamp $service" >> "$temp_file"
            done

            # 按时间戳排序
            while IFS= read -r line; do
                if [ -n "$line" ]; then
                    local service_name=$(echo "$line" | cut -d' ' -f2-)
                    sorted_services+=("$service_name")
                fi
            done < <(sort -n "$temp_file")

            rm -f "$temp_file"
            ;;
        "duration")
            # 按运行时长排序
            local temp_file=$(mktemp)
            for service in "${services[@]}"; do
                local service_pid=$(get_pid "$service")
                local time_info=$(get_start_time_and_duration "$service_pid")
                local duration_seconds=$(echo "$time_info" | cut -d'|' -f2)
                echo "$duration_seconds $service" >> "$temp_file"
            done

            # 按时长排序（降序）
            while IFS= read -r line; do
                if [ -n "$line" ]; then
                    local service_name=$(echo "$line" | cut -d' ' -f2-)
                    sorted_services+=("$service_name")
                fi
            done < <(sort -nr "$temp_file")

            rm -f "$temp_file"
            ;;
        *)
            # 默认按索引排序（原始顺序）
            sorted_services=("${services[@]}")
            ;;
    esac

    echo "${sorted_services[@]}"
}

# 过滤服务函数
filter_services() {
    local services=("$@")
    local filtered_services=()

    for service in "${services[@]}"; do
        local service_pid=$(get_pid "$service")
        local status=$(get_status "$service" "$service_pid")

        case "$FILTER_STATUS" in
            "running")
                if [ "$status" = "运行" ]; then
                    filtered_services+=("$service")
                fi
                ;;
            "stopped")
                if [ "$status" = "停止" ]; then
                    filtered_services+=("$service")
                fi
                ;;
            *)
                # 不过滤，添加所有服务
                filtered_services+=("$service")
                ;;
        esac
    done

    echo "${filtered_services[@]}"
}

log_info() { log_message "INFO" "$@"; }
log_warn() { log_message "WARN" "$@"; }
log_error() { log_message "ERROR" "$@"; }

# 获取服务的CPU使用率
get_cpu_usage() {
    local service_pid=$1
    if [ -z "$service_pid" ]; then
        echo "0.0"
        return
    fi

    if command -v ps >/dev/null 2>&1; then
        local cpu_usage=$(ps -p "$service_pid" -o %cpu= 2>/dev/null | tr -d ' ')
        if [ -n "$cpu_usage" ]; then
            echo "$cpu_usage"
        else
            echo "0.0"
        fi
    else
        echo "0.0"
    fi
}

# 获取服务的内存使用
get_memory_usage() {
    local service_pid=$1
    if [ -z "$service_pid" ]; then
        echo "0MB"
        return
    fi

    if command -v ps >/dev/null 2>&1; then
        # 获取RSS (Resident Set Size) 内存使用量（KB）
        local memory_kb=$(ps -p "$service_pid" -o rss= 2>/dev/null | tr -d ' ')
        if [ -n "$memory_kb" ]; then
            # 转换为MB
            local memory_mb=$((memory_kb / 1024))
            echo "${memory_mb}MB"
        else
            echo "0MB"
        fi
    else
        echo "0MB"
    fi
}

# 错误退出函数
die() {
    log_error "$*"
    exit 1
}

# 获取应用PID（PID 文件优先，进程名匹配备选）
get_pid() {
    local service_name=$1
    local service_dir="${SERVICES_BASE_DIR}/${service_name}"
    local pid_file="${service_dir}/${service_name}.pid"
    
    if [ -f "$pid_file" ]; then
        local pid
        pid=$(cat "$pid_file")
        # 验证PID是否仍然有效
        if kill -0 "$pid" 2>/dev/null; then
            echo "$pid"
            return
        else
            # PID文件存在但进程不存在，删除PID文件
            rm -f "$pid_file"
        fi
    fi
    
    # 备选：通过进程命令行匹配（支持 nohup / systemd / 无 PID 文件场景）
    if command -v pgrep >/dev/null 2>&1; then
        pgrep -f "java.*${service_name}" 2>/dev/null | head -n1
    fi
}

# 获取应用监听的端口
get_ports() {
    local service_name=$1
    local service_pid=$2
    local ports=""

    if [ -z "$service_pid" ]; then
        echo ""
        return
    fi

    # 优先使用能按 PID 查询监听端口的工具；macOS netstat 不支持 Linux 的 -tnlp。
    if command -v ss >/dev/null 2>&1; then
        ports=$(ss -tlpn 2>/dev/null | awk -v pid="$service_pid" '$0 ~ ("pid=" pid ",") {print $4}' | sed 's/.*://' | sort -u | tr '\n' ',' | sed 's/,$//')
    fi

    if [ -z "$ports" ] && command -v lsof >/dev/null 2>&1; then
        ports=$(lsof -a -p "$service_pid" -iTCP -sTCP:LISTEN -P -n 2>/dev/null | awk 'NR>1 {print $9}' | sed 's/.*://' | grep -E '^[0-9]+$' | sort -u | tr '\n' ',' | sed 's/,$//')
    fi

    if [ -z "$ports" ] && command -v netstat >/dev/null 2>&1 && [ "$(uname -s 2>/dev/null)" = "Linux" ]; then
        ports=$(netstat -tlnp 2>/dev/null | awk -v pid="$service_pid" '$0 ~ pid {print $4}' | sed 's/.*://' | sort -u | tr '\n' ',' | sed 's/,$//')
    fi

    if [ -z "$ports" ]; then
        # 如果无法通过系统命令获取端口，尝试从日志中获取端口
        local service_dir="${SERVICES_BASE_DIR}/${service_name}"
        local log_file="${HOME}/logs/${service_name}.log"
        if [ -f "$log_file" ]; then
            # 精确匹配 Spring Boot Tomcat/Netty 启动日志
            local port=$(grep -oP 'Tomcat started on port\(s\): \K\d+' "$log_file" 2>/dev/null | tail -n1)
            [ -z "$port" ] && port=$(grep -oP 'Netty started on port\(s\): \K\d+' "$log_file" 2>/dev/null | tail -n1)
            [ -n "$port" ] && ports="$port"
        fi
    fi

    echo "$ports"
}

# 格式化时间间隔为可读格式
format_duration() {
    local total_seconds=$1
    local days=0
    local hours=0
    local minutes=0
    local seconds=0

    if [ $total_seconds -ge 86400 ]; then
        days=$((total_seconds / 86400))
        total_seconds=$((total_seconds % 86400))
    fi

    if [ $total_seconds -ge 3600 ]; then
        hours=$((total_seconds / 3600))
        total_seconds=$((total_seconds % 3600))
    fi

    if [ $total_seconds -ge 60 ]; then
        minutes=$((total_seconds / 60))
        total_seconds=$((total_seconds % 60))
    fi

    seconds=$total_seconds

    # 构建运行时长字符串
    local duration_str=""
    if [ $days -gt 0 ]; then
        duration_str="${days}d "
    fi
    if [ $hours -gt 0 ]; then
        duration_str="${duration_str}${hours}h "
    fi
    if [ $minutes -gt 0 ]; then
        duration_str="${duration_str}${minutes}m "
    fi
    if [ $seconds -gt 0 ]; then
        duration_str="${duration_str}${seconds}s"
    fi

    # 如果所有时间单位都为0，至少显示0s
    if [ -z "$duration_str" ]; then
        duration_str="0s"
    fi

    echo "$duration_str"
}

# 将月份名称转换为数字，处理中英文月份
convert_month_to_number() {
    local month=$1
    local month_num=""

    case "$month" in
        "一月"|"Jan") month_num="01" ;;
        "二月"|"Feb") month_num="02" ;;
        "三月"|"Mar") month_num="03" ;;
        "四月"|"Apr") month_num="04" ;;
        "五月"|"May") month_num="05" ;;
        "六月"|"Jun") month_num="06" ;;
        "七月"|"Jul") month_num="07" ;;
        "八月"|"Aug") month_num="08" ;;
        "九月"|"Sep") month_num="09" ;;
        "十月"|"Oct") month_num="10" ;;
        "十一月"|"Nov") month_num="11" ;;
        "十二月"|"Dec") month_num="12" ;;
        *)
            # 如果是中文数字月份（如"12月"），提取数字并格式化
            if [[ "$month" =~ ^([0-9]+)月$ ]]; then
                month_num=$(printf "%02d" ${BASH_REMATCH[1]})
            else
                month_num="$month"
            fi
            ;;
    esac

    echo "$month_num"
}

# 获取应用启动时间和运行时长
get_start_time_and_duration() {
    local service_pid=$1

    if [ -z "$service_pid" ]; then
        echo "-|0|0s"
        return
    fi

    if command -v ps >/dev/null 2>&1; then
        # 获取进程启动时间
        local start_time_raw
        start_time_raw=$(LC_ALL=C ps -p "$service_pid" -o lstart= 2>/dev/null | sed 's/^[[:space:]]*//')
        if [ -n "$start_time_raw" ]; then
            local formatted_start_time
            local start_timestamp=""

            # GNU date 与 BSD/macOS date 参数不同，这里分别兼容。
            if start_timestamp=$(date -d "$start_time_raw" +%s 2>/dev/null); then
                formatted_start_time=$(date -d "@$start_timestamp" "+%Y-%m-%d %H:%M:%S" 2>/dev/null)
            elif start_timestamp=$(date -j -f "%a %b %e %T %Y" "$start_time_raw" +%s 2>/dev/null); then
                formatted_start_time=$(date -r "$start_timestamp" "+%Y-%m-%d %H:%M:%S" 2>/dev/null)
            else
                # 兜底解析 lstart 的标准字段：Day Month Date HH:MM:SS YYYY。
                local month=$(echo "$start_time_raw" | awk '{print $2}')
                local date=$(echo "$start_time_raw" | awk '{print $3}')
                local time=$(echo "$start_time_raw" | awk '{print $4}')
                local year=$(echo "$start_time_raw" | awk '{print $5}')
                local month_num=$(convert_month_to_number "$month")

                if [ ${#date} -eq 1 ]; then
                    date="0$date"
                fi

                formatted_start_time="$year-$month_num-$date $time"
                start_timestamp=$(date -d "$formatted_start_time" +%s 2>/dev/null || date -j -f "%Y-%m-%d %H:%M:%S" "$formatted_start_time" +%s 2>/dev/null || echo "")
            fi

            if [ -n "$start_timestamp" ] && [ "$start_timestamp" -gt 0 ]; then
                local current_timestamp=$(date +%s)
                local total_seconds=$((current_timestamp - start_timestamp))

                # 使用通用函数格式化运行时长
                local duration_str=$(format_duration $total_seconds)

                # 返回启动时间和运行时长，用 | 分隔
                echo "$formatted_start_time|$total_seconds|$duration_str"
            else
                # 如果时间戳转换失败，使用ps的etime作为备选方案
                local elapsed_time=$(ps -p "$service_pid" -o etime= 2>/dev/null)
                if [ -n "$elapsed_time" ]; then
                    # 解析 elapsed_time 格式 (可能为 dd-hh:mm:ss, hh:mm:ss, mm:ss, 或 dd 天)
                    local days=0
                    local hours=0
                    local minutes=0
                    local seconds=0

                    if [[ "$elapsed_time" =~ ^([0-9]+)-([0-9]+):([0-9]+):([0-9]+)$ ]]; then
                        # 格式: dd-hh:mm:ss
                        days=${BASH_REMATCH[1]}
                        hours=${BASH_REMATCH[2]}
                        minutes=${BASH_REMATCH[3]}
                        seconds=${BASH_REMATCH[4]}
                    elif [[ "$elapsed_time" =~ ^([0-9]+):([0-9]+):([0-9]+)$ ]]; then
                        # 格式: hh:mm:ss
                        hours=${BASH_REMATCH[1]}
                        minutes=${BASH_REMATCH[2]}
                        seconds=${BASH_REMATCH[3]}
                    elif [[ "$elapsed_time" =~ ^([0-9]+):([0-9]+)$ ]]; then
                        # 格式: mm:ss
                        minutes=${BASH_REMATCH[1]}
                        seconds=${BASH_REMATCH[2]}
                    elif [[ "$elapsed_time" =~ ^([0-9]+)-([0-9]+):([0-9]+)$ ]]; then
                        # 格式: dd-hh:mm
                        days=${BASH_REMATCH[1]}
                        hours=${BASH_REMATCH[2]}
                        minutes=${BASH_REMATCH[3]}
                    elif [[ "$elapsed_time" =~ ^([0-9]+)-00:00$ ]]; then
                        # 格式: dd-00:00 (表示天数)
                        days=${BASH_REMATCH[1]}
                    fi

                    # 计算总秒数
                    local total_seconds=$((days * 86400 + hours * 3600 + minutes * 60 + seconds))

                    # 使用通用函数格式化运行时长
                    local duration_str=$(format_duration $total_seconds)

                    # 返回启动时间和运行时长，用 | 分隔
                    echo "$formatted_start_time|$total_seconds|$duration_str"
                else
                    # 如果无法获取运行时间，只返回启动时间
                    echo "$formatted_start_time|0|0s"
                fi
            fi
        else
            echo "-|0|0s"
        fi
    else
        echo "-|0|0s"
    fi
}

# 检查应用状态
get_status() {
    local service_name=$1
    local service_pid=$2

    if [ -n "$service_pid" ]; then
        echo "运行"
    else
        echo "停止"
    fi
}

# 计算字符串的实际显示宽度
calculate_display_width() {
    local input="$1"
    if [ -z "$input" ]; then
        echo 0
        return
    fi

    # 使用 wc -m 获取字符数，wc -c 获取字节数
    local char_count=$(printf "%s" "$input" | wc -m)
    local byte_count=$(printf "%s" "$input" | wc -c)

    # 计算中文字符数量 (UTF-8编码下中文字符占3字节)
    # 这种计算方法对多字节字符有效
    local multi_byte_diff=$((byte_count - char_count))
    local chinese_chars=$((multi_byte_diff / 2))  # 每个中文字符比英文字符多2字节

    # 显示宽度：英文字符数量 + 中文字符数量 * 2
    local english_chars=$((char_count - chinese_chars))
    local display_width=$((english_chars + chinese_chars * 2))

    echo $display_width
}

# 列出所有应用状态
list_services() {
    log_info "正在获取应用状态列表..."

    # 从配置文件加载服务依赖顺序；缺省时从部署目录或进程发现。
    local service_source="配置文件"
    local service_array=()
    local dependency_file
    dependency_file=$(get_dependency_file)
    if [ -n "$dependency_file" ]; then
        # 使用配置文件中的服务列表
        read -ra service_array <<< "$(get_service_list "config")"
    else
        log_warn "未找到依赖顺序文件，按部署目录或进程发现服务"
        service_source="自动发现"
        read -ra service_array <<< "$(get_service_list "auto")"
    fi

    # 检查部署目录中实际存在的服务。若已有依赖顺序文件，则以配置为准，
    # 避免历史部署目录（如 admin/gateway/task）污染状态列表。
    local actual_services=()
    if [ -z "$dependency_file" ] && [ -d "$SERVICES_BASE_DIR" ]; then
        for item in "$SERVICES_BASE_DIR"/*/; do
            if [ -d "$item" ]; then
                local service_name=$(basename "$item")
                actual_services+=("$service_name")
            fi
        done
    fi

    # 合并所有服务（优先使用配置文件中的顺序或进程顺序，然后是实际存在的服务）
    local all_services=("${service_array[@]+"${service_array[@]}"}")
    for actual_service in "${actual_services[@]+"${actual_services[@]}"}"; do
        local found=false
        for configured_service in "${all_services[@]}"; do
            if [ "$configured_service" = "$actual_service" ]; then
                found=true
                break
            fi
        done
        if [ "$found" = false ]; then
            all_services+=("$actual_service")
        fi
    done

    # 去重并保留顺序
    read -ra unique_services <<< "$(unique_array "${all_services[@]+"${all_services[@]}"}")"

    # 应用过滤
    if [ -n "$FILTER_STATUS" ]; then
        read -ra unique_services <<< "$(filter_services "${unique_services[@]+"${unique_services[@]}"}")"
    fi

    # 应用排序
    if [ "$SORT_BY" != "index" ]; then
        read -ra unique_services <<< "$(sort_services "${unique_services[@]+"${unique_services[@]}"}")"
    fi

    # 预先计算每列的最大宽度，确保至少能容纳表头
    local header_index_width=$(calculate_display_width "编号")
    local header_service_name_width=$(calculate_display_width "应用名")
    local header_pid_width=$(calculate_display_width "PID")
    local header_ports_width=$(calculate_display_width "端口")
    local header_status_width=$(calculate_display_width "状态")
    local header_start_time_width=$(calculate_display_width "启动时间")
    local header_duration_width=$(calculate_display_width "运行时长")

    # 初始化每列的最大宽度为表头宽度
    local max_index_width=$header_index_width
    local max_service_name_width=$header_service_name_width
    local max_pid_width=$header_pid_width
    local max_ports_width=$header_ports_width
    local max_status_width=$header_status_width
    local max_start_time_width=$header_start_time_width
    local max_duration_width=$header_duration_width

    # 计算实际数据中的最大显示宽度
    local index=1
    for service_name in "${unique_services[@]+"${unique_services[@]}"}"; do
        # 计算编号显示宽度
        local current_index_width=$(calculate_display_width "$index")
        if [ $current_index_width -gt $max_index_width ]; then
            max_index_width=$current_index_width
        fi

        # 计算应用名显示宽度
        local current_service_name_width=$(calculate_display_width "$service_name")
        if [ $current_service_name_width -gt $max_service_name_width ]; then
            max_service_name_width=$current_service_name_width
        fi

        # 计算PID显示宽度
        local service_pid=$(get_pid "$service_name")
        local current_pid_width=1
        if [ -n "$service_pid" ]; then
            current_pid_width=$(calculate_display_width "$service_pid")
        fi
        if [ $current_pid_width -gt $max_pid_width ]; then
            max_pid_width=$current_pid_width
        fi

        # 计算端口显示宽度
        local ports=$(get_ports "$service_name" "$service_pid")
        local current_ports_width=$(calculate_display_width "$ports")
        if [ $current_ports_width -gt $max_ports_width ]; then
            max_ports_width=$current_ports_width
        fi

        # 计算状态显示宽度
        local status=$(get_status "$service_name" "$service_pid")
        local current_status_width=$(calculate_display_width "$status")
        if [ $current_status_width -gt $max_status_width ]; then
            max_status_width=$current_status_width
        fi

        # 计算时间信息
        local time_info=$(get_start_time_and_duration "$service_pid")
        local start_time=$(echo "$time_info" | cut -d'|' -f1)
        local duration_str=$(echo "$time_info" | cut -d'|' -f3)

        # 计算启动时间显示宽度
        local current_start_time_width=$(calculate_display_width "$start_time")
        if [ $current_start_time_width -gt $max_start_time_width ]; then
            max_start_time_width=$current_start_time_width
        fi

        # 计算运行时长显示宽度
        local current_duration_width=$(calculate_display_width "$duration_str")
        if [ $current_duration_width -gt $max_duration_width ]; then
            max_duration_width=$current_duration_width
        fi

        index=$((index + 1))
    done

    # 根据详细模式决定是否显示额外列
    if [ "$SHOW_DETAILS" = true ]; then
        # 计算额外列的宽度
        local header_cpu_width=$(calculate_display_width "CPU%")
        local header_memory_width=$(calculate_display_width "内存")
        local max_cpu_width=$header_cpu_width
        local max_memory_width=$header_memory_width

        # 重新计算宽度，包括额外列
        for service_name in "${unique_services[@]+"${unique_services[@]}"}"; do
            local service_pid=$(get_pid "$service_name")
            local cpu_usage=$(get_cpu_usage "$service_pid")
            local memory_usage=$(get_memory_usage "$service_pid")

            local current_cpu_width=$(calculate_display_width "$cpu_usage")
            if [ $current_cpu_width -gt $max_cpu_width ]; then
                max_cpu_width=$current_cpu_width
            fi

            local current_memory_width=$(calculate_display_width "$memory_usage")
            if [ $current_memory_width -gt $max_memory_width ]; then
                max_memory_width=$current_memory_width
            fi
        done

        # 为额外列增加一些填充
        max_cpu_width=$((max_cpu_width + 1))
        max_memory_width=$((max_memory_width + 1))

        # 使用动态宽度格式化输出（包含详细信息列）
        printf "%-${max_index_width}s %-$((max_service_name_width+3))s %-${max_pid_width}s %-$((max_ports_width+2))s %-${max_status_width}s %-${max_cpu_width}s %-${max_memory_width}s %-$((max_start_time_width+4))s %-${max_duration_width}s\n" "编号" "应用名" "PID" "端口" "状态" "CPU%" "内存" "启动时间" "运行时长"
        index=1
        for service_name in "${unique_services[@]+"${unique_services[@]}"}"; do
            local service_pid=$(get_pid "$service_name")
            local ports=$(get_ports "$service_name" "$service_pid")
            local status=$(get_status "$service_name" "$service_pid")
            local time_info=$(get_start_time_and_duration "$service_pid")
            local start_time=$(echo "$time_info" | cut -d'|' -f1)
            local duration_str=$(echo "$time_info" | cut -d'|' -f3)
            local cpu_usage=$(get_cpu_usage "$service_pid")
            local memory_usage=$(get_memory_usage "$service_pid")

            # 如果端口为空，显示"-"
            if [ -z "$ports" ]; then
                ports="-"
            fi

            # 根据颜色选项输出状态
            if [ "$SHOW_COLOR" = true ]; then
                local status_color=$(get_status_color "$status")
                printf "%-${max_index_width}s %-${max_service_name_width}s %-${max_pid_width}s %-${max_ports_width}s %-${max_status_width}s %-${max_cpu_width}s %-${max_memory_width}s %-${max_start_time_width}s %-${max_duration_width}s\n" "$index" "$service_name" "$service_pid" "$ports" "$(color_output "$status_color" "$status")" "$cpu_usage" "$memory_usage" "$start_time" "$duration_str"
            else
                printf "%-${max_index_width}s %-${max_service_name_width}s %-${max_pid_width}s %-${max_ports_width}s %-${max_status_width}s %-${max_cpu_width}s %-${max_memory_width}s %-${max_start_time_width}s %-${max_duration_width}s\n" "$index" "$service_name" "$service_pid" "$ports" "$status" "$cpu_usage" "$memory_usage" "$start_time" "$duration_str"
            fi
            index=$((index + 1))
        done
    else
        # 使用动态宽度格式化输出（基本模式）
        printf "%-${max_index_width}s %-$((max_service_name_width+3))s %-${max_pid_width}s %-$((max_ports_width+2))s %-${max_status_width}s %-$((max_start_time_width+4))s %-${max_duration_width}s\n" "编号" "应用名" "PID" "端口" "状态" "启动时间" "运行时长"
        index=1
        for service_name in "${unique_services[@]+"${unique_services[@]}"}"; do
            local service_pid=$(get_pid "$service_name")
            local ports=$(get_ports "$service_name" "$service_pid")
            local status=$(get_status "$service_name" "$service_pid")
            local time_info=$(get_start_time_and_duration "$service_pid")
            local start_time=$(echo "$time_info" | cut -d'|' -f1)
            local duration_str=$(echo "$time_info" | cut -d'|' -f3)
            # 如果端口为空，显示"-"
            if [ -z "$ports" ]; then
                ports="-"
            fi

            # 根据颜色选项输出状态
            if [ "$SHOW_COLOR" = true ]; then
                local status_color=$(get_status_color "$status")
                printf "%-${max_index_width}s %-${max_service_name_width}s %-${max_pid_width}s %-${max_ports_width}s %-${max_status_width}s %-${max_start_time_width}s %-${max_duration_width}s\n" "$index" "$service_name" "$service_pid" "$ports" "$(color_output "$status_color" "$status")" "$start_time" "$duration_str"
            else
                printf "%-${max_index_width}s %-${max_service_name_width}s %-${max_pid_width}s %-${max_ports_width}s %-${max_status_width}s %-${max_start_time_width}s %-${max_duration_width}s\n" "$index" "$service_name" "$service_pid" "$ports" "$status" "$start_time" "$duration_str"
            fi
            index=$((index + 1))
        done
    fi

    echo ""
    local service_source_desc
    if [ -n "$(get_dependency_file)" ]; then
        service_source_desc="配置文件"
    else
        service_source_desc="$service_source"
    fi

    # 显示过滤和排序信息
    local additional_info=""
    if [ -n "$FILTER_STATUS" ]; then
        case "$FILTER_STATUS" in
            "running") additional_info="(仅显示运行中服务)";;
            "stopped") additional_info="(仅显示已停止服务)";;
        esac
    fi
    if [ "$SORT_BY" != "index" ]; then
        local sort_desc=""
        case "$SORT_BY" in
            "status") sort_desc="按状态";;
            "start_time") sort_desc="按启动时间";;
            "duration") sort_desc="按运行时长";;
        esac
        if [ -n "$additional_info" ]; then
            additional_info="$additional_info, $sort_desc 排序"
        else
            additional_info="($sort_desc 排序)"
        fi
    fi

    if [ -n "$additional_info" ]; then
        log_info "总共 ${#unique_services[@]} 个应用 $additional_info"
    else
        log_info "总共 ${#unique_services[@]} 个应用"
    fi

    # 汇总行：运行数/停止数/总内存/总CPU
    local running_count=0 stopped_count=0 total_mem=0 total_cpu=0
    for service_name in "${unique_services[@]+"${unique_services[@]}"}"; do
        local service_pid=$(get_pid "$service_name")
        local status=$(get_status "$service_name" "$service_pid")
        if [ "$status" = "运行" ]; then
            running_count=$((running_count + 1))
            local cpu=$(get_cpu_usage "$service_pid")
            local mem=$(get_memory_usage "$service_pid")
            # 提取数值（去掉单位）
            local cpu_val=$(echo "$cpu" | sed 's/%$//')
            local mem_val=$(echo "$mem" | sed 's/MB$\|GB$\|KB$\|B$//')
            [ -n "$cpu_val" ] && total_cpu=$(echo "$total_cpu + $cpu_val" | bc -l 2>/dev/null || echo "$total_cpu")
            [ -n "$mem_val" ] && total_mem=$(echo "$total_mem + $mem_val" | bc -l 2>/dev/null || echo "$total_mem")
        else
            stopped_count=$((stopped_count + 1))
        fi
    done
    printf "────────────────────────────────────────────────────────────────────────────────\n"
    printf "汇总: %d 服务 | 运行: %d | 停止: %d | 总内存: %.1fMB | 总CPU: %.1f%%\n" \
        "${#unique_services[@]}" "$running_count" "$stopped_count" "$total_mem" "$total_cpu"
}

# 通过编号解析服务名
resolve_service_by_index() {
    local index=$1
    local service_array=()

    # 获取服务列表
    read -ra service_array <<< "$(load_ordered_services)"

    if ((index >= 1 && index <= ${#service_array[@]})); then
        echo "${service_array[$((index-1))]}"
    else
        echo ""
    fi
}

# 解析服务名列表（支持编号和名称混合）
parse_service_list() {
    local input_list=("$@")
    local resolved_services=()
    
    for item in "${input_list[@]}"; do
        # 检查是否为数字（编号）
        if [[ "$item" =~ ^[0-9]+$ ]]; then
            local service_name
            service_name=$(resolve_service_by_index "$item")
            if [ -n "$service_name" ]; then
                resolved_services+=("$service_name")
            else
                log_error "无效的编号: $item"
                exit 1
            fi
        else
            # 假设是服务名称
            resolved_services+=("$item")
        fi
    done
    
    echo "${resolved_services[@]}"
}

# 批量启动服务
start_services() {
    local services_to_start=("$@")
    
    log_info "开始批量启动服务: ${services_to_start[*]}"
    
    for service in "${services_to_start[@]}"; do
        local service_dir="${SERVICES_BASE_DIR}/${service}"
        
        if [ ! -d "$service_dir" ]; then
            log_error "服务目录不存在: $service_dir"
            continue
        fi
        
        local start_script="${service_dir}/bin/start.sh"
        if [ ! -f "$start_script" ]; then
            log_error "启动脚本不存在: $start_script"
            continue
        fi
        
        log_info "启动服务: $service"
        if timeout 120 "$start_script"; then
            log_info "服务 $service 启动命令已发送"
        else
            log_error "服务 $service 启动失败或超时（120s）"
        fi
    done
}

# 批量停止服务
stop_services() {
    local services_to_stop=("$@")
    
    log_info "开始批量停止服务: ${services_to_stop[*]}"
    
    # 反向停止，优先按照部署顺序的逆序；没有顺序配置时使用自动发现顺序。
    local service_array=()
    read -ra service_array <<< "$(load_ordered_services)"

    local selected_services=()
    for service in "${services_to_stop[@]}"; do
        selected_services+=("$service")
    done

    local reversed_services=()
    for ((i=${#service_array[@]}-1; i>=0; i--)); do
        local ordered_service="${service_array[$i]}"
        for service in "${selected_services[@]}"; do
            if [ "$service" = "$ordered_service" ]; then
                reversed_services+=("$service")
                break
            fi
        done
    done
    for service in "${selected_services[@]}"; do
        local found=false
        for ordered_service in "${reversed_services[@]}"; do
            if [ "$service" = "$ordered_service" ]; then
                found=true
                break
            fi
        done
        if [ "$found" = false ]; then
            reversed_services+=("$service")
        fi
    done
    
    for service in "${reversed_services[@]}"; do
        local service_dir="${SERVICES_BASE_DIR}/${service}"
        
        if [ ! -d "$service_dir" ]; then
            log_error "服务目录不存在: $service_dir"
            continue
        fi
        
        local stop_script="${service_dir}/bin/stop.sh"
        if [ ! -f "$stop_script" ]; then
            log_error "停止脚本不存在: $stop_script"
            continue
        fi
        
        log_info "停止服务: $service"
        if timeout 60 "$stop_script"; then
            log_info "服务 $service 停止命令已发送"
        else
            log_error "服务 $service 停止失败或超时（60s）"
        fi
    done
}

# 主函数
main() {
    # 参数解析
    local services_base_dir="$SERVICES_BASE_DIR"
    local command="list"
    local command_args=()

    while [[ $# -gt 0 ]]; do
        case "$1" in
            -d|--base-dir)
                services_base_dir="$2"
                SERVICES_BASE_DIR="$services_base_dir"
                shift 2
                ;;
            --color)
                SHOW_COLOR=true
                shift
                ;;
            --no-color)
                SHOW_COLOR=false
                shift
                ;;
            --sort-by)
                SORT_BY="$2"
                shift 2
                ;;
            --filter)
                FILTER_STATUS="$2"
                shift 2
                ;;
            --details)
                SHOW_DETAILS=true
                shift
                ;;
            --watch|-w)
                WATCH_INTERVAL="${2:-2}"
                [[ "$1" == "-w" || "$1" == "--watch" ]] && { shift; [[ "$1" =~ ^[0-9]+$ ]] && { WATCH_INTERVAL="$1"; shift; }; }
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            list|start|stop|restart|health)
                command="$1"
                shift
                # 收集命令参数
                while [[ $# -gt 0 && "$1" != -* ]]; do
                    command_args+=("$1")
                    shift
                done
                ;;
            *)
                die "未知参数或命令: $1。使用 -h 查看帮助。"
                ;;
        esac
    done
    
    # --watch 模式：循环刷新列表
    if [ -n "$WATCH_INTERVAL" ]; then
        while true; do
            clear 2>/dev/null || printf "\n"
            list_services
            sleep "$WATCH_INTERVAL"
        done
        exit 0
    fi

    case "$command" in
        list)
            list_services
            ;;
        start)
            if [ ! -d "$services_base_dir" ]; then
                die "服务基础目录不存在: $services_base_dir"
            fi
            if [ ${#command_args[@]} -eq 0 ]; then
                die "启动命令需要至少一个服务名或编号"
            fi
            local start_list
            start_list=$(parse_service_list "${command_args[@]}")
            start_services $start_list
            ;;
        stop)
            if [ ! -d "$services_base_dir" ]; then
                die "服务基础目录不存在: $services_base_dir"
            fi
            if [ ${#command_args[@]} -eq 0 ]; then
                die "停止命令需要至少一个服务名或编号"
            fi
            local stop_list
            stop_list=$(parse_service_list "${command_args[@]}")
            stop_services $stop_list
            ;;
        restart)
            if [ ! -d "$services_base_dir" ]; then
                die "服务基础目录不存在: $services_base_dir"
            fi
            if [ ${#command_args[@]} -eq 0 ]; then
                die "重启命令需要至少一个服务名或编号"
            fi
            local restart_list
            restart_list=$(parse_service_list "${command_args[@]}")
            log_info "重启服务: $restart_list"
            stop_services $restart_list
            sleep 2
            start_services $restart_list
            ;;
        health)
            local health_ok=true
            local dependency_file
            dependency_file=$(get_dependency_file)
            local service_array=()
            if [ -n "$dependency_file" ]; then
                read -ra service_array <<< "$(get_service_list "config")"
            else
                read -ra service_array <<< "$(get_service_list "auto")"
            fi
            for service in "${service_array[@]+"${service_array[@]}"}"; do
                local pid=$(get_pid "$service")
                if [ -z "$pid" ]; then
                    printf "  ❌ %-20s 未运行\n" "$service"
                    health_ok=false
                    continue
                fi
                local ports=$(get_ports "$service" "$pid")
                local port=$(echo "$ports" | cut -d',' -f1)
                if [ -n "$port" ] && curl -s --max-time 3 "http://localhost:$port/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
                    printf "  ✅ %-20s :%s  UP\n" "$service" "$port"
                elif [ -n "$port" ]; then
                    printf "  ⚠️  %-20s :%s  端口可达但健康检查失败\n" "$service" "$port"
                    health_ok=false
                else
                    printf "  ❓ %-20s PID=%s 无端口\n" "$service" "$pid"
                fi
            done
            [ "$health_ok" = true ] && log_info "所有服务健康检查通过" || die "部分服务健康检查未通过"
            ;;
        *)
            die "未知命令: $command。使用 -h 查看帮助。"
            ;;
    esac
}

# 执行主函数
main "$@"
