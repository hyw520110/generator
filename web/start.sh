#!/bin/bash

# 通用启动脚本
# 支持启动查看日志并监听启动成功或启动失败自动退出

set -euo pipefail  # 严格模式：遇到错误立即退出，未定义变量报错，管道错误传播

# --- 公共变量定义 ---
#获取当前IP忽略的网卡
NETWORK_IGNORED=uengine0,xdroid0,br*,ve*
#当前内网网卡
NETWORK_PREFERRED=eth0

# --- 全局变量定义 ---
readonly CURRENT_DIR="$(cd "$(dirname "$0")" && pwd)"
readonly BASE_DIR="${CURRENT_DIR%/*}"
readonly SERVICES_BASE_DIR="$HOME/webapps"
readonly APP_NAME="$(basename "${BASE_DIR}")"
readonly LOG_DIR="$HOME/logs"
readonly LOG_FILE="${LOG_DIR}/${APP_NAME}.log"
readonly PID_FILE="${BASE_DIR}/${APP_NAME}.pid"
readonly DEFAULT_STARTUP_TIMEOUT=30
readonly DEFAULT_JVM_OPTS="-Xms512m -Xmx1024m"
readonly DEFAULT_DEBUG_PORT=5005
readonly DEFAULT_LOG_CONTEXT=50
readonly DEFAULT_SERVICE_DEPENDENCY_ORDER=("order-service" "recommend-service" "logistics-service" "payment-service" "product-service" "user-service" "web-service" "message-service" "gateway-service" "task-scheduling-service" "admin-service")

# --- 跨平台工具函数 ---
# 检测操作系统类型
get_os_type() {
    uname -s
}

# 跨平台获取文件大小
get_file_size() {
    local file="$1"
    if [[ "$(get_os_type)" == "Darwin" ]]; then
        stat -f%z "$file" 2>/dev/null || echo "0"
    else
        stat -c%s "$file" 2>/dev/null || echo "0"
    fi
}

# 跨平台获取ISO 8601格式时间戳
get_iso_timestamp() {
    if [[ "$(get_os_type)" == "Darwin" ]]; then
        date +%Y-%m-%dT%H:%M:%S%z
    else
        date --iso-8601=seconds
    fi
}

# --- 运行模式与构建工具检测 ---
# 优先使用 mvnd（Maven Daemon）
if command -v mvnd >/dev/null 2>&1; then
    MVN="mvnd"
else
    MVN="mvn"
fi

# 检测运行模式：开发模式（存在 src 或 pom.xml）或部署模式
# 参数：检测的目录（默认为脚本所在目录）
detect_mode() {
    local check_dir="${1:-$CURRENT_DIR}"
    if [ -d "$check_dir/src" ] || [ -f "$check_dir/pom.xml" ]; then
        echo "dev"
    else
        echo "deploy"
    fi
}

# 应用运行模式（会被命令行参数覆盖）
APP_MODE=""
# 端口排除模式（默认排除以222开头的5位端口，即22200-22299）
PORT_EXCLUDE_PATTERN="${PORT_EXCLUDE_PATTERN:-^222[0-9]{2}$}"
success_keywords="Started .*Application in|Tomcat started on port|Application startup completed|Started .*Application[.]kt|Netty started on port|已启动|应用已启动|服务已启动|Application running|started successfully|服务已运行"
error_keywords="Failed to start|Error starting ApplicationContext|Application startup failed|java[.]lang[.]OutOfMemoryError|Caused by:.*Exception|BindException|BeanCreationException|failed to load|Injection of autowired dependencies failed|UnsatisfiedDependencyException"
# 通用日志函数
log_message() {
    local level=$1
    shift
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    case "$level" in
        "ERROR")
            # 使用红色显示整个日志行
            printf "\033[31m%s [%s] %s\033[0m\n" "$timestamp" "$level" "$*" >&2
            ;;
        "WARN")
            # 使用黄色显示整个日志行
            printf "\033[33m%s [%s] %s\033[0m\n" "$timestamp" "$level" "$*" >&2
            ;;
        *)
            # INFO和其他级别正常显示，也重定向到 stderr
            printf "%s [%s] %s\n" "$timestamp" "$level" "$*" >&2
            ;;
    esac
}

log_info() { log_message "INFO" "$@"; }
log_warn() { log_message "WARN" "$@"; }
log_error() { log_message "ERROR" "$@"; }

# 错误退出函数
die() {
    log_error "$*"
    exit 1
}
# 显示帮助信息
show_help() {
    cat << EOF
通用启动脚本，用于启动应用并监听启动状态

用法: $0 [选项]

选项:
  -f, --force                 强制重启（如果应用已在运行，则先停止再启动）
  -c, --console               控制台模式（前台启动，日志直接输出到终端）
  -j, --jvm-opts <opts>        指定JVM选项，默认"$DEFAULT_JVM_OPTS"
  -t, --timeout <秒>           启动超时时间，默认为${DEFAULT_STARTUP_TIMEOUT}秒
  -p, --debug-port <端口>      调试端口，默认为$DEFAULT_DEBUG_PORT
  --skywalking-opts <opts>    SkyWalking代理选项
  --no-startup-log            启动过程中不输出应用日志，只显示等待进度条，但启动失败时仍会输出异常日志
  --log-context <行数>         指定异常日志输出的上下文行数，默认为${DEFAULT_LOG_CONTEXT}
  --debug                     启用调试模式
  -h, --help                  显示此帮助信息
  --all                       (批量) 按依赖顺序启动 '$SERVICES_BASE_DIR' 目录下的所有服务

运行模式（自动检测）:
  开发模式                    当目录包含 pom.xml 或 src/ 目录时自动启用，使用 mvn spring-boot:run
  部署模式                    当目录包含 lib/*.jar 时自动启用，使用 java -jar

环境变量:
  PORT_EXCLUDE_PATTERN        端口排除正则表达式，默认排除以222开头的5位端口（22200-22299）
                              例如：export PORT_EXCLUDE_PATTERN="^222[0-9]{2}$"
  MVN                         Maven 命令，默认自动检测 mvnd 或 mvn

EOF
}
# 脚本退出时运行的清理函数
cleanup() {
    # 由于使用了进程替换，tail -f 会自动终止，这里不需要额外处理
    # 可以在此添加其他必要的清理逻辑
    return
}

# 设置信号处理
trap cleanup EXIT INT TERM

# 检查并创建必要目录
setup_directories() {
    if [ ! -d "$LOG_DIR" ]; then
        mkdir -p "$LOG_DIR" || die "无法创建日志目录: $LOG_DIR"
    fi

    if [ ! -w "$LOG_DIR" ]; then
        die "日志目录不可写: $LOG_DIR"
    fi
}

# 根据工作目录检测进程（更精确）
check_process_by_workdir() {
    local pid=$(ps -ef 2>/dev/null | grep java | grep "$BASE_DIR" | grep -v grep | awk '{print $2}' | head -n 1)
    if [ -n "$pid" ]; then
        echo "$pid"
        return 0
    fi
    return 1
}

# 查找JAR文件
find_jar_file() {
    local jar_file
    jar_file=$(find "$BASE_DIR/lib" -maxdepth 1 -name "*${APP_NAME}*.jar" -type f | head -n 1)

    if [ -z "$jar_file" ]; then
        # 尝试查找其他可能的JAR文件
        jar_file=$(find "$BASE_DIR/lib" -maxdepth 1 -name "*.jar" -type f | head -n 1)
        if [ -z "$jar_file" ]; then
            die "在 $BASE_DIR/lib 目录下未找到任何 JAR 文件"
        fi
        log_warn "未找到匹配应用名的JAR文件，使用: $jar_file"
    fi

    echo "$jar_file"
}

# 检查应用是否已在运行
check_if_running() {
    local force_restart=${1:-false}
    local pid=""
    local found_by=""
    
    # 策略1: 检查PID文件
    if [ -f "$PID_FILE" ]; then
        local file_pid
        file_pid=$(cat "$PID_FILE")
        if kill -0 "$file_pid" 2>/dev/null; then
            pid="$file_pid"
            found_by="PID文件"
        else
            log_warn "发现过期的PID文件，将删除"
            rm -f "$PID_FILE"
        fi
    fi
    
    # 策略2: 通过工作目录检测（更精确，避免误杀其他Java进程）
    if [ -z "$pid" ]; then
        local workdir_pid
        if workdir_pid=$(check_process_by_workdir); then
            pid="$workdir_pid"
            found_by="工作目录检测"
            # 更新PID文件
            echo "$pid" > "$PID_FILE"
        fi
    fi
    
    if [ -n "$pid" ]; then
        if [ "$force_restart" = true ]; then
            log_info "应用 $APP_NAME 已在运行 (PID: $pid, 来源: $found_by)，使用 -f 参数将强制停止后重启..."
            # 调用 stop.sh -f 来停止应用
            local stop_script="${CURRENT_DIR}/stop.sh"
            if [ -f "$stop_script" ]; then
                log_info "正在执行: $stop_script -f"
                "$stop_script" -f
                # 等待一段时间确保应用已完全停止
                sleep 3
            else
                log_error "停止脚本不存在: $stop_script"
                die "无法停止已运行的应用"
            fi
        else
            die "应用 $APP_NAME 已在运行 (PID: $pid, 来源: $found_by)"
        fi
    fi
}

sed_file() {
    local pattern="$1"
    local replacement="$2"
    local file="$3"

    if [[ "$(uname -s)" == "Darwin"* ]]; then
        sed -i '' "s#${pattern}#${replacement}#" "$file"
        else
         sed -i "s#${pattern}#${replacement}#" "$file"
     fi
}
# 提取版本号的函数
extract_major_version() {
     local filename=$1
     # 从类似 "spring-cloud-gateway-server-4.1.4.jar" 的文件名中提取主版本号
     local version=$(basename "$filename" .jar | sed 's/.*-\([0-9]\+\)\..*/\1/')
     echo "$version" 2>/dev/null || echo ""
}
# 构建JVM选项
build_jvm_opts() {
    local jvm_opts="${1:-${CUSTOM_JVM_OPTS:-$DEFAULT_JVM_OPTS}}"

    # 动态解析当前服务目录下的 bootstrap.yaml 来获取QOS端口
    # qus官方文档：https://cn.dubbo.apache.org/zh-cn/overview/mannual/java-sdk/reference-manual/qos/overview/
    local bootstrap_file
    bootstrap_file=$(find "$BASE_DIR" -maxdepth 1 -name "bootstrap.y*ml" | head -n 1)
    local qos_opts=""
    if [[ -f "$bootstrap_file" ]]; then
        local qos_port
        # 使用健壮的awk命令来解析端口
        qos_port=$(awk '/qos:/ {found=1} found && /port:/ {print $2; exit}' "$bootstrap_file" | tr -d '[:space:]')
        if [[ "$qos_port" =~ ^[0-9]+$ ]]; then
            qos_opts="-Ddubbo.application.qos-enable=true -Ddubbo.application.qos-port=$qos_port"
        fi
    fi

    jvm_opts="$jvm_opts $qos_opts"

    # 获取内网网卡名
    if [ -z "${NETWORK_PREFERRED:-}" ]; then
        NETWORK_PREFERRED=$(ip route | grep default | awk '{print $5}' | head -n 1)
    fi

    # 添加网络接口相关的JVM参数
    jvm_opts="$jvm_opts -Dspring.cloud.inetutils.ignored-interfaces=$NETWORK_IGNORED -Dnacos.inetutils.ignored-interfaces=$NETWORK_IGNORED -Dnacos.inetutils.use-only-site-local-interfaces=true -Ddubbo.network.interface.ignored=$NETWORK_IGNORED -Ddubbo.network.interface.preferred=$NETWORK_PREFERRED -Dnacos.config.longPollTimeout=60000 -Dnacos.config.retryTime=3000 -Dnacos.config.maxRetryCount=3"

    # 检测Java版本，根据不同版本添加相应的JVM参数
    local java_version
    local major_version=""
    if command -v java >/dev/null 2>&1; then
        java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
        # 提取主版本号，处理不同的Java版本格式（例如"1.8.0_301", "11.0.12", "17.0.1"等）
        if [[ $java_version =~ ^1\.([0-9]+)\. ]]; then
            # Java 8及更早版本的格式 (1.x.x_x)
            major_version="${BASH_REMATCH[1]}"
        elif [[ $java_version =~ ^([0-9]+)\. ]]; then
            # Java 9及更高版本的格式 (x.x.x)
            major_version="${BASH_REMATCH[1]}"
        else
            # 直接尝试获取
            major_version=$(echo "$java_version" | cut -d'.' -f1)
        fi
        log_info "检测到Java版本: $java_version (主版本: $major_version)"
    fi

    # 根据Java版本添加相应的JVM参数
    if [ -n "$major_version" ]; then
        # 基础共用参数（所有版本）
        jvm_opts="$jvm_opts -XX:+HeapDumpOnOutOfMemoryError"
        
        if [ "$major_version" -eq 8 ]; then
            # Java 8 参数
            jvm_opts="$jvm_opts -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
            
        elif [ "$major_version" -ge 11 ] && [ "$major_version" -lt 17 ]; then
            # Java 11 参数
            jvm_opts="$jvm_opts -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
            # 启用ZGC（可选，如果需要低延迟GC）
            # jvm_opts="$jvm_opts -XX:+UnlockExperimentalVMOptions -XX:+UseZGC"
            
        elif [ "$major_version" -ge 17 ] && [ "$major_version" -lt 21 ]; then
            # Java 17 参数
            jvm_opts="$jvm_opts -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
            # 启用ZGC（Java 17生产环境推荐）
            # jvm_opts="$jvm_opts -XX:+UseZGC"
            
        elif [ "$major_version" -ge 21 ]; then
            # Java 21+ 参数
            jvm_opts="$jvm_opts -XX:+UseZGC -XX:+ZGenerational"
            # 虚拟线程相关（如果应用使用虚拟线程）
            # jvm_opts="$jvm_opts -Djdk.virtualThreadScheduler.parallelism=16"
        fi
        
        # Java 17+ 模块化系统开放参数（Spring Boot常用反射）
        if [ "$major_version" -ge 17 ]; then
            jvm_opts="$jvm_opts --add-opens java.base/java.lang=ALL-UNNAMED"
            jvm_opts="$jvm_opts --add-opens java.base/java.util=ALL-UNNAMED"
            jvm_opts="$jvm_opts --add-opens java.base/java.lang.reflect=ALL-UNNAMED"
            jvm_opts="$jvm_opts --add-opens java.base/java.text=ALL-UNNAMED"
            jvm_opts="$jvm_opts --add-opens java.desktop/java.awt.font=ALL-UNNAMED"
        fi
    fi

    # 添加调试选项
    if [ "${DEBUG_MODE:-}" = "debug" ]; then
        local debug_port="${DEBUG_PORT:-$DEFAULT_DEBUG_PORT}"
        local debug_opts="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$debug_port"
        jvm_opts="$jvm_opts $debug_opts"
    fi

    # 开发模式下跳过 SkyWalking 以提升启动速度并减少日志干扰
    if [ "${APP_MODE:-}" = "dev" ]; then
        log_info "开发模式：跳过 SkyWalking 代理加载"
    else
        # 自动搜索SkyWalking agent并添加相关参数
        local skywalking_agent_jar=""
        local common_skywalking_dirs=("$HOME" "/usr/local" "/opt")

        for dir in "${common_skywalking_dirs[@]}"; do
            # 在每个目录下递归搜索 skywalking-agent.jar
            skywalking_agent_jar=$(find "$dir" -name "skywalking-agent.jar" -type f 2>/dev/null | head -n 1)
            if [ -n "$skywalking_agent_jar" ]; then
                break
            fi
        done

        if [ -n "$skywalking_agent_jar" ]; then
        # 获取SkyWalking agent目录
        local skywalking_agent_dir=$(dirname "$skywalking_agent_jar")
        local skywalking_dir=$(dirname "$skywalking_agent_dir")
        if [ ! -d "$skywalking_dir/$APP_NAME" ]; then
        	cp -r $skywalking_agent_dir $skywalking_dir/$APP_NAME
        fi
       	# 判断当前应用是否Spring Cloud Gateway(查找spring-cloud-gateway-server-*.jar或spring-webflux-*.jar是否存在)
    	if [ -d "$BASE_DIR/lib" ]; then
    		gateway_jars=$(find "$BASE_DIR/lib" -name "spring-cloud-gateway-server-*.jar" -type f | head -n 1)
    		webflux_jars=$(find "$BASE_DIR/lib" -name "spring-webflux-*.jar" -type f | head -n 1)
    		if [ -n "$gateway_jars" ] || [ -n "$webflux_jars" ]; then
    			# 文件名spring-cloud-gateway-server-4.1.4.jar中获取主版本号也就是4
    			local gateway_major_version=$(extract_major_version "$gateway_jars")
    			local webflux_major_version=$(extract_major_version "$webflux_jars")
    			local gateway_plugin=$(ls $skywalking_dir/$APP_NAME/optional-plugins/apm-spring-cloud-gateway-${gateway_major_version}.x-plugin-*.jar 2>/dev/null | head -n 1)
    			local webflux_plugin=$(ls $skywalking_dir/$APP_NAME/optional-plugins/apm-spring-webflux-${webflux_major_version}.x-plugin-*.jar 2>/dev/null | head -n 1)
        		log_info "检测到当前应用为 Spring Cloud Gateway 类型，安装对应插件:$gateway_plugin $webflux_plugin"
        		cp $gateway_plugin  $skywalking_dir/$APP_NAME/plugins/
        		cp $webflux_plugin $skywalking_dir/$APP_NAME/plugins/
    		fi
    	fi
        # 修改客户端配置文件agent.service_name=${SW_AGENT_NAME:Your_ApplicationName}
		if [ -d $skywalking_dir/$APP_NAME/config ]; then
			local agent_config=$skywalking_dir/$APP_NAME/config/agent.config
			sed_file "agent.service_name=.*" "agent.service_name=\$\{SW_AGENT_NAME:$APP_NAME\}" $agent_config
			
			# 仅部署模式下检测 SkyWalking 连接
			if [ "$APP_MODE" = "deploy" ]; then
				local sw_host="sw-server"
				# 检测主机名sw-server是否通，不通的话提示输入skywalking服务端的IP
				while ! ping -c 1 -W 5 $sw_host >/dev/null 2>&1; do
					log_warn "无法连接到 SkyWalking 服务主机 sw-server"
					echo "请输入 SkyWalking 服务端的 IP 地址或主机名（直接回车将保留默认值 sw-server）:"
					read -r user_input
					if [ -n "$user_input" ]; then
						sw_host="$user_input"
					fi
				done
				sed_file "collector.backend_service=.*" "collector.backend_service=\$\{SW_AGENT_COLLECTOR_BACKEND_SERVICES:$sw_host:11800\}" $agent_config
			else
				# 开发模式使用默认配置，不检测连接
				sed_file "collector.backend_service=.*" "collector.backend_service=\$\{SW_AGENT_COLLECTOR_BACKEND_SERVICES:sw-server:11800\}" $agent_config
			fi
		fi	
        # 构建SkyWalking参数
        local skywalking_opts="-javaagent:$skywalking_dir/$APP_NAME/skywalking-agent.jar -Dskywalking.agent.service_name=$APP_NAME"
        #log_info "skywalking_opts: $skywalking_opts"
        jvm_opts="$jvm_opts $skywalking_opts"
        fi
    fi

    # 添加SkyWalking代理选项（来自命令行参数，仅部署模式有效）
    if [ "${APP_MODE:-}" != "dev" ] && [ -n "${SKYWALKING_AGENT_OPTS:-}" ]; then
        jvm_opts="$jvm_opts $SKYWALKING_AGENT_OPTS"
    fi

    echo "$jvm_opts"
}
# 获取应用端口（返回所有监听端口，用空格分隔）
get_process_port() {
    local pid=$1
    local ports=""
    local filtered_ports=""

    if ! [[ "$pid" =~ ^[0-9]+$ ]]; then
        return
    fi

    # 优先使用ss命令，更现代高效
    if command -v ss >/dev/null 2>&1; then
        ports=$(ss -tlpn | grep "pid=$pid" | awk '{print $4}' | sed 's/.*://' | sort -u | tr '\n' ' ' | sed 's/ $//')
    fi

    # 如果ss命令不可用，则回退到netstat
    if [ -z "$ports" ] && command -v netstat >/dev/null 2>&1; then
        ports=$(netstat -tlpn | grep "$pid/java" | awk '{print $4}' | sed 's/.*://' | sort -u | tr '\n' ' ' | sed 's/ $//')
    fi

    # 过滤掉排除的端口
    if [ -n "$ports" ] && [ -n "${PORT_EXCLUDE_PATTERN:-}" ]; then
        for port in $ports; do
            if ! [[ "$port" =~ $PORT_EXCLUDE_PATTERN ]]; then
                filtered_ports="$filtered_ports$port "
            fi
        done
        ports="${filtered_ports% }"
    fi

    echo "$ports"
}

# 检查端口是否监听（支持多个端口，用空格分隔）
check_port_listening() {
    local ports="$1"
    local timeout=1
    local all_ok=true

    # 如果没有端口，返回成功
    if [ -z "$ports" ]; then
        return 0
    fi

    # 遍历所有端口进行检查
    for port in $ports; do
        if ! [[ "$port" =~ ^[0-9]+$ ]]; then
            continue
        fi

        # 跳过排除的端口
        if [ -n "${PORT_EXCLUDE_PATTERN:-}" ] && [[ "$port" =~ $PORT_EXCLUDE_PATTERN ]]; then
            continue
        fi

        local port_ok=false
        if command -v ss >/dev/null 2>&1; then
            if ss -tln | grep -q ":${port} "; then
                port_ok=true
            fi
        elif command -v netstat >/dev/null 2>&1; then
            if netstat -tln | grep -q ":${port} "; then
                port_ok=true
            fi
        else
            # 使用timeout和nc进行端口检查，作为备用方案
            if timeout "$timeout" bash -c "</dev/null >/dev/tcp/localhost/$port" 2>/dev/null; then
                port_ok=true
            fi
        fi

        if [ "$port_ok" = false ]; then
            all_ok=false
            break
        fi
    done

    [ "$all_ok" = true ] && return 0 || return 1
}

# 启动应用
start_application() {
    local jar_file="$1"
    local jvm_opts="$2"
    local mode="${3:-deploy}"
    local foreground="${4:-false}"
    
    # 开发模式下使用脚本所在目录（模块目录），部署模式下使用 BASE_DIR
    if [ "$mode" = "dev" ]; then
        # 开发模式：在脚本所在目录（如 web/）执行，该目录应该有独立的 pom.xml
        cd "$CURRENT_DIR" || die "无法切换到模块目录: $CURRENT_DIR"
        log_info "构建工具: $MVN"
        log_info "工作目录: $(pwd)"
        log_info "启动${APP_NAME}: $MVN clean compile spring-boot:run"
        
        if [ "$foreground" = "true" ]; then
            # 前台启动 - 直接运行，日志输出到控制台
            # shellcheck disable=SC2086
            $MVN spring-boot:run -Dspring-boot.run.jvmArguments="$jvm_opts"
        else
            # 后台启动
            # shellcheck disable=SC2086
            nohup $MVN spring-boot:run -Dspring-boot.run.jvmArguments="$jvm_opts" >> "$LOG_FILE" 2>&1 &
            local app_pid=$!
            echo "$app_pid" > "$PID_FILE"
            log_info "$APP_NAME 应用已启动PID: $app_pid"
            
            # 等待一下让Maven启动Java进程
            sleep 5
            # 重新检测实际的Java进程PID（因为mvn会启动子进程）
            local java_pid
            java_pid=$(check_process_by_workdir)
            if [ -n "$java_pid" ] && [ "$java_pid" != "$app_pid" ]; then
                echo "$java_pid" > "$PID_FILE"
                log_info "Java进程PID: $java_pid"
            fi
        fi
    else
        # 部署模式：切换到应用根目录
        cd "$BASE_DIR" || die "无法切换到应用目录: $BASE_DIR"
        log_info "启动${APP_NAME}: java $jvm_opts -jar $jar_file"
        log_info "日志文件: $LOG_FILE"
        
        if [ "$foreground" = "true" ]; then
            # 前台启动
            # shellcheck disable=SC2086
            java $jvm_opts -jar "$jar_file"
        else
            # 后台启动
            # shellcheck disable=SC2086
            nohup java $jvm_opts -jar "$jar_file" >> "$LOG_FILE" 2>&1 &
            local app_pid=$!
            echo "$app_pid" > "$PID_FILE"
            log_info "$APP_NAME 应用已启动PID: $app_pid"
        fi
    fi
    
    return 0
}

# 检查启动状态
check_startup_status() {
    local timeout=$1
    local show_logs=$2 # true 显示日志, false 显示进度条

    local pid
    pid=$(cat "$PID_FILE")

    # ISO 8601 format (e.g., 2023-10-27T15:01:01+08:00)
    local start_ts
    start_ts=$(get_iso_timestamp)

    log_info "开始监听启动状态 (超时: ${timeout}s, 启动时间: ${start_ts})..." >&2

    # 等待日志文件出现
    local wait_count=0
    while [ ! -f "$LOG_FILE" ] && [ $wait_count -lt 20 ]; do # 10秒
        sleep 0.5
        ((wait_count++))
    done

    if [ ! -f "$LOG_FILE" ]; then
        touch "$LOG_FILE" || die "无法创建日志文件: $LOG_FILE"
    fi
    
    local log_start_size
    log_start_size=$(get_file_size "$LOG_FILE")
    local last_size=$log_start_size

    local elapsed=0
    while [ "$elapsed" -lt "$timeout" ]; do
        if [ "$show_logs" = false ]; then
            show_progress "$elapsed" "正在启动 $APP_NAME..."
        fi

        if ! kill -0 "$pid" 2>/dev/null; then
            if [ "$show_logs" = false ]; then echo >&2; fi
            log_error "应用进程 (PID: $pid) 意外终止！" >&2
            extract_error_log "$error_keywords" "$log_start_size" "$start_ts"
            return 1
        fi

        local current_size
        current_size=$(get_file_size "$LOG_FILE")

        if [ "$current_size" -gt "$last_size" ]; then
            local new_content
            new_content=$(tail -c "+$((last_size + 1))" "$LOG_FILE")
            
            if [ "$show_logs" = true ]; then
                echo -n "$new_content"
            fi

            # 使用awk同时根据时间戳和关键字过滤，通过预处理时间戳格式使其可比较
            local start_date_time="${start_ts%+*}"  # 去掉时区信息
            local start_date_time_clean="${start_date_time//T/ }"  # 将T替换为空格，形成 "YYYY-MM-DD HH:MM:SS" 格式
            local success_line=$(echo "$new_content" | awk -v start_dt="$start_date_time_clean" -v keywords="$success_keywords" '
                BEGIN { split(start_dt, s, " "); sd = s[1]; st = s[2]; }
                $0 ~ keywords && ($1 > sd || ($1 == sd && substr($2, 1, 8) > st)) { print $0; exit; }')
            
            if [ -n "$success_line" ]; then
                if [ "$show_logs" = false ]; then
                    # 结束进度条并换行
                    show_progress "$timeout" "启动成功"
                    printf "\r%-60s\n" " " >&2
                fi
                log_info "服务 $APP_NAME 日志显示启动成功！正在验证端口..." >&2
                local app_ports
                app_ports=$(get_process_port "$pid")
                if [ -n "$app_ports" ]; then
                    if check_port_listening "$app_ports"; then
                        log_info "端口 $app_ports 已成功监听。" >&2
                        return 0
                    else
                        log_error "服务日志显示已启动，但端口 $app_ports 未在监听。" >&2
                        return 1
                    fi
                else
                    log_warn "未检测到服务监听任何端口，跳过端口检查。" >&2
                    return 0
                fi
            fi

            # 使用awk同时根据时间戳和关键字过滤，通过预处理时间戳格式使其可比较
            local start_date_time="${start_ts%+*}"  # 去掉时区信息
            local start_date_time_clean="${start_date_time//T/ }"  # 将T替换为空格，形成 "YYYY-MM-DD HH:MM:SS" 格式
            local error_line
            error_line=$(echo "$new_content" | awk -v start_dt="$start_date_time_clean" -v keywords="$error_keywords" '
                BEGIN { split(start_dt, s, " "); sd = s[1]; st = s[2]; }
                $0 ~ keywords && ($1 > sd || ($1 == sd && substr($2, 1, 8) > st)) { print $0; exit; }')
            
            if [ -n "$error_line" ]; then
                if [ "$show_logs" = false ]; then
                    # 结束进度条并输出错误及上下文
                    printf "\r%-60s\n" " " >&2
                fi
                log_error "服务 $APP_NAME 启动失败！日志中发现错误:" >&2
                extract_error_log "$error_keywords" "$log_start_size" "$start_ts"
                return 1
            fi
            last_size=$current_size
        fi

        sleep 1
        elapsed=$((elapsed + 1))
    done

    if [ "$show_logs" = false ]; then echo >&2; fi 
    log_warn "服务 $APP_NAME 启动日志监听超时 (${timeout}秒)！" >&2
    log_info "将进行最终状态检查 (进程和端口)..." >&2

    # 超时后进行最终检查
    if ! kill -0 "$pid" 2>/dev/null; then
        log_error "最终检查失败：应用进程 (PID: $pid) 已不存在！" >&2
        extract_error_log "$error_keywords" "$log_start_size" "$start_ts"
        return 1
    fi

    local app_ports
    app_ports=$(get_process_port "$pid")
    if [ -n "$app_ports" ]; then
        if check_port_listening "$app_ports"; then
            log_info "最终检查成功：进程存活且端口 $app_ports 已监听。" >&2
            log_warn "虽然日志未输出成功关键字，但服务可能已启动。请手动确认服务状态。" >&2
            return 0
        else
            log_error "最终检查失败：进程存活，但端口 $app_ports 未在监听。" >&2
            extract_error_log "$error_keywords" "$log_start_size" "$start_ts"
            return 1
        fi
    else
        log_warn "未检测到服务监听任何端口，无法进行端口检查。" >&2
        log_info "最终检查通过：进程存活。" >&2
        log_warn "虽然日志未输出成功关键字，但服务可能已启动。请手动确认服务状态。" >&2
        return 0
    fi
}

# 验证应用状态
verify_application() {
    if [ ! -f "$PID_FILE" ]; then
        return 1
    fi

    local pid
    pid=$(cat "$PID_FILE")

    if ! kill -0 "$pid" 2>/dev/null; then
        log_error "应用进程不存在 (PID: $pid)"
        rm -f "$PID_FILE"
        return 1
    fi

    log_info "应用进程正常运行 (PID: $pid)"
    return 0
}

# 自定义进度条函数（无百分比显示，显示动画效果）
show_progress() {
    local indicator
    local elapsed=$1
    local msg="$2"
    local indicator_chars=('|' '/' '-' '\\')
    local indicator_index=$((elapsed % 4))
    indicator="${indicator_chars[$indicator_index]}"
    # 显示动画效果
    printf "\r${indicator} ${msg}%.0s" " "{1..50}
}


# 提取异常日志
extract_error_log() {
    local error_keywords=$1
    local start_offset=$2
    local start_ts=$3
    local ctx_lines=${LOG_CONTEXT:-$DEFAULT_LOG_CONTEXT}

    if [ ! -f "$LOG_FILE" ]; then
        log_warn "日志文件不存在: $LOG_FILE" >&2
        return
    fi

    # 从指定偏移量开始，根据时间戳与关键字过滤，匹配到每一行后继续输出其后的 ctx_lines 行
    tail -c "+$((start_offset + 1))" "$LOG_FILE" | awk -v start_ts="$start_ts" -v keywords="$error_keywords" -v ctx="$ctx_lines" '
        BEGIN { after=0 }
        {
            if ($1 >= start_ts && $0 ~ keywords) {
                printf "\033[31m%s\033[0m\n", $0
                after = ctx
                next
            }
            if (after > 0) {
                print $0
                after--
            }
        }
    ' >&2
}

# 批量启动所有服务
batch_start_all_services() {
    local passthrough_args=("$@")
    log_info "开始按依赖顺序批量启动位于 ${SERVICES_BASE_DIR} 下的所有服务..."
    
    if [ ! -d "$SERVICES_BASE_DIR" ]; then
        die "服务基础目录 ${SERVICES_BASE_DIR} 不存在。"
    fi

    local overall_status=0
    for service_name in "${SERVICE_DEPENDENCY_ORDER[@]}"; do
        local service_dir="${SERVICES_BASE_DIR}/${service_name}"
        
        # 使用 ps 和 grep 检查服务是否已在运行
        # 匹配类似 "java -jar /path/to/user-service/lib/user-service-1.0.jar" 的进程
        if ps -ef | grep "[j]ava" | grep "${service_name}" > /dev/null; then
            # 检查是否传递了 -f 参数
            local force_flag=false
            for arg in "${passthrough_args[@]}"; do
                if [[ "$arg" == "-f" || "$arg" == "--force" ]]; then
                    force_flag=true
                    break
                fi
            done
            
            if [ "$force_flag" = true ]; then
                log_info "服务 $service_name 似乎已在运行，由于指定了 -f 参数将强制重启..."
            else
                log_info "服务 $service_name 似乎已在运行，跳过。"
                continue
            fi
        fi

        if [ -d "$service_dir" ]; then
            local start_script_path="${service_dir}/bin/start.sh"
            if [ -f "$start_script_path" ]; then
                log_info "--- 准备启动服务: $service_name ---"
                # 调用该服务自己的启动脚本，并传递所有额外参数
                if ! "$start_script_path" "${passthrough_args[@]}"; then
                    log_error "启动服务 $service_name 失败！"
                    overall_status=1
                    # 默认不使用 fail-fast，继续尝试启动其他服务
                fi
            else
                log_warn "在 ${service_dir}/bin 中未找到 start.sh，跳过服务 $service_name。"
            fi
        fi
        # 如果服务目录不存在，则静默跳过
    done

    echo # 添加换行
    if [ "$overall_status" -eq 0 ]; then
        log_info "所有已找到的服务均已成功发出启动命令。"
    else
        die "部分服务启动失败，请检查以上日志。"
    fi
    return $overall_status
}

# 主函数
main() {
    # --- 0. 参数预处理 ---
    local all_args=("$@")
    local has_all_flag=false
    for arg in "${all_args[@]+${all_args[@]}}"; do
        if [[ "$arg" == "--all" ]]; then
            has_all_flag=true
            break
        fi
    done
    
    # --- 1. 前置检测：运行模式 ---
    # 先检测运行模式，开发模式下跳过某些多模块功能
    APP_MODE=$(detect_mode "$CURRENT_DIR")
    
    # --- 2. 多模块功能检查 ---
    # 仅在部署模式或批量模式下加载依赖顺序配置
    if [ "$APP_MODE" = "deploy" ] || [ "$has_all_flag" = true ]; then
        # 仅当 SERVICE_DEPENDENCY_ORDER 未定义时才加载它
        if [[ -z ${SERVICE_DEPENDENCY_ORDER+x} ]]; then
            # 从位于应用根目录的共享配置文件中加载服务依赖顺序
            if [ -f "${BASE_DIR}/.service-order.conf" ]; then
                source "${BASE_DIR}/.service-order.conf"
            else
                log_warn "未找到依赖顺序文件: ${BASE_DIR}/.service-order.conf"
                log_warn "将使用默认的硬编码顺序。请运行 package.sh 并重新部署以更新此文件。"
                SERVICE_DEPENDENCY_ORDER=$DEFAULT_SERVICE_DEPENDENCY_ORDER
            fi
        fi
    fi
    
    # --- 3. 参数解析 ---
    local batch_mode=false
    
    # 优先检查批量模式 --all
    for arg in "${all_args[@]+${all_args[@]}}"; do
        if [[ "$arg" == "--all" ]]; then
            batch_mode=true
            break
        fi
    done

    # 如果是批量模式，则委托给批量启动函数
    if [ "$batch_mode" = true ]; then
        local passthrough_args=()
        for arg in "${all_args[@]+${all_args[@]}}"; do
            # 过滤掉 --all 参数本身
            if [[ "$arg" != "--all" ]]; then
                passthrough_args+=("$arg")
            fi
        done
        batch_start_all_services "${passthrough_args[@]+${passthrough_args[@]}}"
        exit $?
    fi

    # --- 单服务启动逻辑 (原始逻辑) ---
    local jvm_opts=""
    local startup_timeout=${STARTUP_TIMEOUT:-$DEFAULT_STARTUP_TIMEOUT}
    local debug_port=""
    local skywalking_opts=""
    local debug_mode=false
    local no_startup_log=false
    local context_lines=$DEFAULT_LOG_CONTEXT
    local force_restart=false
    local console_mode=false

    if [ "${DEBUG_MODE:-}" = "debug" ]; then
        debug_mode=true
    fi

    # 解析单服务启动的参数
    while [[ $# -gt 0 ]]; do
        case "$1" in
            -f|--force) force_restart=true; shift 1 ;;
            -c|--console) console_mode=true; shift 1 ;;
            -j|--jvm-opts) jvm_opts="$2"; shift 2 ;;
            -t|--timeout) startup_timeout="$2"; shift 2 ;;
            -p|--debug-port) debug_port="$2"; shift 2 ;;
            --skywalking-opts) skywalking_opts="$2"; shift 2 ;;
            --no-startup-log) no_startup_log=true; shift 1 ;;
            --log-context) context_lines="$2"; shift 2 ;;
            --debug) debug_mode=true; shift 1 ;;
            -h|--help) show_help; exit 0 ;;
            *) die "未知参数: $1" ;;
        esac
    done
    
    # 开发模式下默认前台启动（除非显式指定 -d 参数，这里没有 -d 参数）
    if [ "$APP_MODE" = "dev" ] && [ "$console_mode" = false ]; then
        log_info "开发模式默认启用控制台模式（前台启动）"
        console_mode=true
    fi
    
    export LOG_CONTEXT="$context_lines"

    setup_directories
    check_if_running "$force_restart"
    
    local jar_file=""
    if [ "$APP_MODE" = "deploy" ]; then
        jar_file=$(find_jar_file)
    fi

    local final_jvm_opts=$(build_jvm_opts "$jvm_opts")

    # 启动应用（start_application内部会输出运行模式、构建工具等信息）
    start_application "$jar_file" "$final_jvm_opts" "$APP_MODE" "$console_mode" || die "启动应用失败"
    
    # 控制台模式下直接返回，不再执行后续启动状态检查
    if [ "$console_mode" = true ]; then
        return 0
    fi

    if [ "${NO_STARTUP_LOG:-}" = "true" ]; then
        no_startup_log=true
    fi
    
    local show_logs=true
    if [ "$no_startup_log" = true ]; then
        show_logs=false
    fi

    if check_startup_status "$startup_timeout" "$show_logs"; then
        if verify_application; then
            log_info "$APP_NAME 应用启动完成"
            exit 0
        else
            die "应用启动后验证失败"
        fi
    else
        rm -f "$PID_FILE"
        die "应用启动失败"
    fi
}

# 参数处理
case "${1:-}" in
    -h|--help)
        show_help
        exit 0
        ;;
    *)
        main "$@"
        ;;
esac
