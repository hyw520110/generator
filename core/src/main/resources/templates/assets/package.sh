#!/usr/bin/env bash

# 通用打包脚本
# 支持指定 Maven profile，并自动分析服务依赖顺序，更新部署脚本。

set -euo pipefail  # 严格错误处理模式

# --- 全局变量定义 ---
readonly SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# 如果脚本在根目录，PROJECT_ROOT_DIR 就是 SCRIPT_DIR；否则是上一级目录
if [ -f "$SCRIPT_DIR/pom.xml" ]; then
  readonly PROJECT_ROOT_DIR="$SCRIPT_DIR"
else
  readonly PROJECT_ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
fi
readonly DEFAULT_MAVEN_PROFILE="dev"
readonly TOOLS_DIR="${PROJECT_ROOT_DIR}/.tools"
readonly MVND_DIR="${TOOLS_DIR}/mvnd"
readonly DEP_FILE="$PROJECT_ROOT_DIR/.service-order.conf"
readonly DEP_FILE_IGNORE_ENTRY=".service-order.conf"
# 全局变量
MAVEN_PROFILE="${DEFAULT_MAVEN_PROFILE}"
TARGET_MODULE=""
OFFLINE_MODE=false
CLEAN_BUILD=true
SKIP_TESTS=true
FLATTEN_SKIP="${FLATTEN_SKIP:-true}"
PARALLEL_THREADS="2C"
BUILD_COMMAND=""
VERBOSE_MODE=false
FAILURE_REPORTED=false
OS=$(uname -s)
cpu_cores=2  # 默认值

# 获取CPU核心数的函数
get_cpu_cores() {
    local cores=2  # 默认值
    case "$OS" in
        Linux)
            if command -v nproc >/dev/null 2>&1; then
                cores=$(nproc)
            fi
            ;;
        Darwin)
            if command -v sysctl >/dev/null 2>&1; then
                cores=$(sysctl -n hw.logicalcpu 2>/dev/null || echo 2)
            fi
            ;;
        *)
            cores=2
            ;;
    esac
    echo "$cores"
}

# 初始化CPU核心数
cpu_cores=$(get_cpu_cores)

# 显示帮助信息
show_help() {
    cat << EOF
用法: $0 [选项] [Maven Profile]

参数:
  Maven Profile        Maven 构建配置文件 (默认: dev)

选项:
  -m, --module=<modules>   指定要构建的模块（支持逗号分隔的多个模块名，支持模糊匹配）
  -P, --profile=<profile>  指定 Maven profile（默认: dev）
  --offline           离线构建模式
  --no-clean          跳过清理阶段
  --with-tests        执行测试
  --threads=<num>     并行线程数（默认: 2C）
  --verbose, -v       详细输出模式，实时显示完整 Maven 输出
  --list-modules      列出所有可用模块
  --list-deployable   输出可部署服务清单，默认 table 格式
  --format=<format>   配合 --list-deployable 使用，支持 table、tsv
  --dry-run          只显示将要执行的命令，不实际执行
  -h, --help         显示此帮助信息

示例:
  $0                                    # 使用默认配置构建所有模块
  $0 -P prod                              # 使用 prod profile 构建
  $0 -m <module>                       # 构建指定模块
  $0 -P prod --offline -m <module>     # 离线构建指定模块
  $0 --no-clean --with-tests          # 不清理且执行测试
  $0 --list-modules                   # 列出所有可用模块
  $0 --list-deployable --format=tsv   # 输出 CI 可解析的可部署服务清单
  $0 --dry-run prod -m <module>       # 预览构建命令

环境变量:
  JAVA_HOME           Java 安装目录
  MAVEN_OPTS          Maven JVM 选项
  FLATTEN_SKIP        是否跳过 flatten-maven-plugin（默认: true）

EOF
}
# --- 2. 颜色定义 ---
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "$(date '+%Y-%m-%d %H:%M:%S') ${GREEN}[INFO]${NC} $*"
}

log_error() {
    echo -e "$(date '+%Y-%m-%d %H:%M:%S') ${RED}[ERROR]${NC} $*" >&2
}

log_warn() {
    echo -e "$(date '+%Y-%m-%d %H:%M:%S') ${YELLOW}[WARN]${NC} $*" >&2
}

log_debug() {
    if [[ "$VERBOSE_MODE" == true ]]; then
        echo -e "$(date '+%Y-%m-%d %H:%M:%S') ${BLUE}[DEBUG]${NC} $*" >&2
    fi
}

# 错误退出函数
die() {
    log_error "$*"
    FAILURE_REPORTED=true
    exit 1
}
# 清理函数
cleanup() {
    local exit_code=$?
    if [ $exit_code -ne 0 ] && [[ "${FAILURE_REPORTED:-false}" != true ]]; then
        log_error "构建过程异常退出，退出码: $exit_code"
    fi
    # 清理临时文件
    if [ -n "${BUILD_LOG_FILE:-}" ] && [ -f "$BUILD_LOG_FILE" ]; then
        rm -f "$BUILD_LOG_FILE"
    fi
}

# 设置信号处理
trap cleanup EXIT INT TERM


# 解析命令行参数
parse_arguments() {
    local dry_run=false
    local list_modules=false
    local list_deployable=false
    local output_format="table"
    local show_help=false

    while [[ $# -gt 0 ]]; do
        case $1 in
            -m|--module=*)
                if [[ "$1" == -m ]]; then
                    # 处理 -m module 形式
                    if [ $# -gt 1 ]; then
                        TARGET_MODULE="$2"
                        shift 2
                    else
                        die "选项 -m 需要一个参数"
                    fi
                elif [[ "$1" == -m=* ]]; then
                    # 处理 -m=module 形式
                    TARGET_MODULE="${1#-m=}"
                    shift
                else
                    # 处理 --module=module 形式
                    TARGET_MODULE="${1#--module=}"
                    shift
                fi
                ;;
            -P*)
                # 处理 -Ptest, -P=test 和 -P test 形式
                if [[ "$1" == -P* ]] && [[ "$1" != "-P" ]]; then
                    MAVEN_PROFILE="${1#-P}"
                    # 如果以=开头，提取等号后的值
                    if [[ "$MAVEN_PROFILE" == =* ]]; then
                        MAVEN_PROFILE="${MAVEN_PROFILE#=}"
                        if [[ -z "$MAVEN_PROFILE" ]]; then
                            die "选项 -P 需要一个参数"
                        fi
                    fi
                    shift
                elif [[ "$1" == "-P" ]]; then
                    if [ $# -gt 1 ]; then
                        MAVEN_PROFILE="$2"
                        shift 2
                    else
                        die "选项 -P 需要一个参数"
                    fi
                fi
                ;;
            --profile=*)
                MAVEN_PROFILE="${1#--profile=}"
                shift
                ;;
            --offline)
                OFFLINE_MODE=true
                shift
                ;;
            --no-clean)
                CLEAN_BUILD=false
                shift
                ;;
            --with-tests)
                SKIP_TESTS=false
                shift
                ;;
            -v|--verbose)
                VERBOSE_MODE=true
                shift
                ;;
            --threads=*)
                local thread_value="${1#--threads=}"
                # 验证线程参数格式（数字或带C后缀的格式）
                if [[ "$thread_value" =~ ^[0-9]+C?$ ]]; then
                    PARALLEL_THREADS="$thread_value"
                else
                    die "无效的线程参数: $thread_value。应为数字或带C后缀的格式（如 4 或 2C）"
                fi
                shift
                ;;
            --list-modules)
                list_modules=true
                shift
                ;;
            --list-deployable)
                list_deployable=true
                shift
                ;;
            --format=*)
                output_format="${1#--format=}"
                shift
                ;;
            --dry-run)
                dry_run=true
                shift
                ;;
            -h|--help)
                show_help=true
                shift
                ;;
            *)
                # 如果参数不是选项，可能是一个 Maven profile
                if [[ "$1" =~ ^-[a-zA-Z] ]]; then
                    die "未知选项: $1。使用 -h 或 --help 查看帮助信息。"
                else
                    # 将未识别的参数作为 Maven profile（保留原有逻辑）
                    if [[ -z "$MAVEN_PROFILE" ]]; then
                        MAVEN_PROFILE="$1"
                    fi
                    shift
                fi
                ;;
        esac
    done
    
    # 处理特殊操作
    if [[ "$show_help" == true ]]; then
        show_help
        exit 0
    fi
    
    if [[ "$list_modules" == true ]]; then
        list_available_modules
        exit 0
    fi

    if [[ "$list_deployable" == true ]]; then
        list_deployable_modules "$output_format"
        exit 0
    fi

    if [[ "$dry_run" == true ]]; then
        perform_dry_run
        exit 0
    fi

}

# 从 pom.xml 中提取模块列表到临时文件
extract_modules_from_pom() {
    local pom_file=$1
    local temp_file=$2
    grep -E "<module>[^<]*</module>" "$pom_file" 2>/dev/null | sed -E "s/.*<module>([^<]+)<\/module>.*/\1/" > "$temp_file" || true
}

collect_reactor_pom_files() {
    local pom_file=$1
    local target_ref=$2
    local abs_pom
    abs_pom="$(cd "$(dirname "$pom_file")" && pwd)/$(basename "$pom_file")"

    local temp_file
    temp_file=$(mktemp)
    extract_modules_from_pom "$abs_pom" "$temp_file"
    if [ ! -s "$temp_file" ]; then
        rm -f "$temp_file"
        return 0
    fi

    eval "local -a existing_poms=(\"\${${target_ref}[@]:-}\")"
    local existing
    for existing in "${existing_poms[@]}"; do
        if [ "$existing" = "$abs_pom" ]; then
            rm -f "$temp_file"
            return 0
        fi
    done
    eval "${target_ref}+=(\"\$abs_pom\")"

    local pom_dir
    pom_dir=$(dirname "$abs_pom")
    while IFS= read -r module_name; do
        local child_pom="$pom_dir/$module_name/pom.xml"
        if [ -f "$child_pom" ]; then
            collect_reactor_pom_files "$child_pom" "$target_ref"
        fi
    done < "$temp_file"
    rm -f "$temp_file"
}

# 统计可部署模块数量
count_deployable_modules() {
    eval "local -a pom_files_ref=(\"\${${1}[@]:-}\")"
    local count=0
    local temp_file=$(mktemp)
    
    for pom_file in "${pom_files_ref[@]}"; do
        local module_dir=$(dirname "$pom_file")
        local relative_path=""
        if [ "$module_dir" != "$PROJECT_ROOT_DIR" ]; then
            relative_path=$(echo "$module_dir" | sed "s|^$PROJECT_ROOT_DIR/||")
        fi
        
        extract_modules_from_pom "$pom_file" "$temp_file"
        while IFS= read -r module_name; do
            local full_module_path
            if [ -z "$relative_path" ]; then
                full_module_path="$module_name"
            else
                full_module_path="$relative_path/$module_name"
            fi
            
            local module_path
            if [ -z "$relative_path" ]; then
                module_path="$PROJECT_ROOT_DIR/$module_name"
            else
                module_path="$PROJECT_ROOT_DIR/$relative_path/$module_name"
            fi
            
            if [ -d "$module_path" ] && is_deployable_module "$module_path"; then
                count=$((count + 1))
            fi
        done < "$temp_file"
    done
    
    rm -f "$temp_file"
    echo $count
}

# 列出所有可用模块
list_available_modules() {
    log_info "应用服务模块列表:"

    # 查找所有包含模块定义的 pom.xml 文件
    local pom_files=()
    find_pom_files_with_modules pom_files

    # 先统计可部署模块数量
    local deployable_count
    deployable_count=$(count_deployable_modules pom_files)

    local temp_file=$(mktemp)

    # 遍历所有包含模块的 pom.xml 文件并显示
    for pom_file in "${pom_files[@]}"; do
        local module_dir=$(dirname "$pom_file")
        local relative_path=""
        if [ "$module_dir" != "$PROJECT_ROOT_DIR" ]; then
            relative_path=$(echo "$module_dir" | sed "s|^$PROJECT_ROOT_DIR/||")
        fi
        
        extract_modules_from_pom "$pom_file" "$temp_file"
        while IFS= read -r module_name; do
            local full_module_path
            if [ -z "$relative_path" ]; then
                full_module_path="$module_name"
            else
                full_module_path="$relative_path/$module_name"
            fi
            
            local module_path
            if [ -z "$relative_path" ]; then
                module_path="$PROJECT_ROOT_DIR/$module_name"
            else
                module_path="$PROJECT_ROOT_DIR/$relative_path/$module_name"
            fi
            
            if [ -d "$module_path" ]; then
                if is_deployable_module "$module_path"; then
                    printf "  %-40s [✓]\n" "$full_module_path"
                else
                    printf "  %-40s [ ]\n" "$full_module_path"
                fi
            fi
        done < "$temp_file"
    done

    rm -f "$temp_file"

    # 显示统计信息
    echo "说明: [✓] = 可部署模块（assembly plugin 或 Spring Boot 应用，$deployable_count 个可部署）"
}

helm_key_for_service() {
    local service_name=$1
    echo "$service_name"
}

module_path_from_dir() {
    local module_dir=$1
    echo "$module_dir" | sed "s|^$PROJECT_ROOT_DIR/||"
}

collect_deployable_module_rows_unsorted() {
    local pom_files=()
    find_pom_files_with_modules pom_files

    local temp_file
    temp_file=$(mktemp)
    local order=0

    for pom_file in "${pom_files[@]}"; do
        local module_dir
        module_dir=$(dirname "$pom_file")
        local relative_path=""
        if [ "$module_dir" != "$PROJECT_ROOT_DIR" ]; then
            relative_path=$(echo "$module_dir" | sed "s|^$PROJECT_ROOT_DIR/||")
        fi

        extract_modules_from_pom "$pom_file" "$temp_file"
        while IFS= read -r module_name; do
            local full_module_path
            if [ -z "$relative_path" ]; then
                full_module_path="$module_name"
            else
                full_module_path="$relative_path/$module_name"
            fi

            local module_path="$PROJECT_ROOT_DIR/$full_module_path"
            if [ -d "$module_path" ] && is_deployable_module "$module_path"; then
                local service_name
                service_name=$(get_artifact_id_from_pom "$module_path/pom.xml")
                local helm_key
                helm_key=$(helm_key_for_service "$service_name")
                order=$((order + 1))
                printf '%s\t%s\t%s\t%s\n' "$order" "$full_module_path" "$service_name" "$helm_key"
            fi
        done < "$temp_file"
    done

    rm -f "$temp_file"
}

row_service_name() {
    echo "$1" | awk -F '\t' '{print $3}'
}

row_module_path() {
    echo "$1" | awk -F '\t' '{print $2}'
}

service_api_artifact() {
    local service_name=$1
    if [[ "$service_name" == *-service ]]; then
        echo "${service_name%-service}-api"
    else
        echo "${service_name}-api"
    fi
}

row_runtime_dependencies() {
    local current_row=$1
    shift
    local current_service
    current_service=$(row_service_name "$current_row")
    local module_path
    module_path=$(row_module_path "$current_row")
    local pom_file="$PROJECT_ROOT_DIR/$module_path/pom.xml"
    [ -f "$pom_file" ] || return 0

    local candidate_row
    for candidate_row in "$@"; do
        local candidate_service
        candidate_service=$(row_service_name "$candidate_row")
        [ "$candidate_service" = "$current_service" ] && continue

        local api_artifact
        api_artifact=$(service_api_artifact "$candidate_service")
        if grep -q "<artifactId>${api_artifact}</artifactId>" "$pom_file"; then
            echo "$candidate_service"
        fi
    done
}

collect_deployable_module_rows() {
    local raw_rows=()
    while IFS= read -r row; do
        [ -n "$row" ] && raw_rows+=("$row")
    done < <(collect_deployable_module_rows_unsorted)

    local all_services=" "
    local row
    for row in "${raw_rows[@]}"; do
        all_services+="$(row_service_name "$row") "
    done

    local remaining=("${raw_rows[@]}")
    local emitted_rows=()
    local emitted_services=" "

    while [ ${#remaining[@]} -gt 0 ]; do
        local next_remaining=()
        local progressed=false

        for row in "${remaining[@]}"; do
            local ready=true
            local dep
            while IFS= read -r dep; do
                [ -z "$dep" ] && continue
                if [[ "$all_services" == *" $dep "* ]] && [[ "$emitted_services" != *" $dep "* ]]; then
                    ready=false
                    break
                fi
            done < <(row_runtime_dependencies "$row" "${raw_rows[@]}")

            if [ "$ready" = true ]; then
                emitted_rows+=("$row")
                emitted_services+="$(row_service_name "$row") "
                progressed=true
            else
                next_remaining+=("$row")
            fi
        done

        if [ "$progressed" = false ]; then
            if [ ${#next_remaining[@]} -gt 0 ]; then
                emitted_rows+=("${next_remaining[@]}")
            fi
            break
        fi

        if [ ${#next_remaining[@]} -gt 0 ]; then
            remaining=("${next_remaining[@]}")
        else
            break
        fi
    done

    local order=0
    for row in "${emitted_rows[@]}"; do
        order=$((order + 1))
        printf '%s\t%s\t%s\t%s\n' "$order" "$(row_module_path "$row")" "$(row_service_name "$row")" "$(echo "$row" | awk -F '\t' '{print $4}')"
    done
}

list_deployable_modules() {
    local output_format="${1:-table}"

    case "$output_format" in
        table)
            printf '%-5s %-45s %-25s %-25s\n' "序号" "模块路径" "服务名" "Helm Key"
            collect_deployable_module_rows | while IFS=$'\t' read -r order module_path service_name helm_key; do
                printf '%-5s %-45s %-25s %-25s\n' "$order" "$module_path" "$service_name" "$helm_key"
            done
            ;;
        tsv)
            collect_deployable_module_rows
            ;;
        *)
            die "不支持的输出格式: $output_format。支持 table、tsv"
            ;;
    esac
}

# 预览构建命令
perform_dry_run() {
    setup_build_environment true
    local maven_command
    maven_command=$(build_maven_command)

    # 显示配置
    echo "构建配置:"
    echo "  Maven Profile: $MAVEN_PROFILE"
    echo "  目标模块: ${TARGET_MODULE:-"所有模块"}"
    echo "  离线模式: $OFFLINE_MODE"
    echo "  清理构建: $CLEAN_BUILD"
    echo "  跳过测试: $SKIP_TESTS"
    echo "  并行线程: $PARALLEL_THREADS"
    echo "  详细输出: $VERBOSE_MODE"
    echo

    echo "将要执行的命令:"
    echo "  $maven_command"
    echo

    if [ -n "$TARGET_MODULE" ]; then
        echo "匹配的模块:"
        local matched
        matched=$(find_matching_modules "$TARGET_MODULE")
        if [ -n "$matched" ]; then
            echo "$matched" | while read -r module; do
                echo "  - $module"
            done
        else
            echo "(无) 将构建全部模块"
        fi
    fi
}

# 检查 Java 版本
check_java_version() {
    log_debug "检查 Java 环境..."
    if ! command -v java >/dev/null 2>&1; then
        die "未检测到 Java 运行时环境。请确保 Java 已正确安装并配置 PATH。"
    fi

    if ! java -version >/dev/null 2>&1; then
        die "未检测到有效的 Java 运行时环境 (或仅存在 macOS 默认存根)。请确保 JDK 已正确安装并配置 PATH。"
    fi

    local current_raw
    current_raw=$(java -version 2>&1 | grep -E "(openjdk version|java version)" | awk -F'"' '{print $2}' | head -1 || true)

    local current_version
    current_version=$(echo "$current_raw" | awk -F. '{ if ($1 == "1") print $2; else print $1 }')

	    local required_version=""
	    if [ -f "$PROJECT_ROOT_DIR/pom.xml" ]; then
	        required_version=$(grep -E "<(java.version|maven.compiler.release)>" "$PROJECT_ROOT_DIR/pom.xml" \
	            | head -1 \
	            | sed -E 's/.*<(java.version|maven.compiler.release)>([^<]+)<\/(java.version|maven.compiler.release)>.*/\2/' \
	            || true)
	    fi

    if [ -z "${current_version}" ]; then
        die "无法检测 Java 版本"
    fi

    if [ -n "${required_version}" ] && [ "${current_version}" -lt "${required_version}" ]; then
        die "Java 版本不匹配: 当前 ${current_version}，需要 ${required_version} 或更高版本"
    fi

    log_debug "Java 版本检查通过: ${current_version}"

    # 显示 Java 环境信息
    if [[ "$VERBOSE_MODE" == true ]]; then
        log_debug "JAVA_HOME: ${JAVA_HOME:-"未设置"}"
        log_debug "Java 路径: $(which java)"
    fi
}

# 获取操作系统类型
get_os_type() {
    case "$(uname -s)" in
        Darwin) echo 'macos' ;;
        Linux) echo 'linux' ;;
        CYGWIN*|MINGW*|MSYS*) echo 'windows' ;;
        *) echo 'unknown' ;;
    esac
}

is_mvnd_usable() {
    local mvnd_bin=$1
    "$mvnd_bin" --version >/dev/null 2>&1
}

# 检查和安装 mvnd
setup_maven_daemon() {
    log_debug "配置 Maven 构建工具..."

    # 检查项目本地 mvnd
    if [ -f "$MVND_DIR/bin/mvnd" ] && is_mvnd_usable "$MVND_DIR/bin/mvnd"; then
        BUILD_COMMAND="$MVND_DIR/bin/mvnd"
        log_debug "使用项目本地 mvnd: $BUILD_COMMAND"
        return 0
    fi

    # 检查全局 mvnd
    if command -v mvnd >/dev/null 2>&1 && is_mvnd_usable "mvnd"; then
        BUILD_COMMAND="mvnd"
        log_debug "使用全局 mvnd: $(which mvnd)"
        return 0
    elif command -v mvnd >/dev/null 2>&1; then
        log_warn "检测到 mvnd 但当前环境不可用，回退到 Maven"
    fi

    # Maven 可用时不自动安装 mvnd，避免默认打包触发额外下载；显式 USE_MVND=true 时才尝试安装。
    if [[ "${USE_MVND:-false}" == "true" ]] && [ -f "$SCRIPT_DIR/mvnd.sh" ]; then
        log_info "尝试安装 mvnd..."
        if bash "$SCRIPT_DIR/mvnd.sh"; then
            # 重新检查
            if command -v mvnd >/dev/null 2>&1 && is_mvnd_usable "mvnd"; then
                BUILD_COMMAND="mvnd"
                log_info "mvnd 安装成功"
                return 0
            fi
        fi
    fi

    # 回退到 Maven
    if command -v mvn >/dev/null 2>&1; then
        BUILD_COMMAND="mvn"
        log_debug "使用 Maven: $(which mvn)"
        return 0
    fi

    die "未找到 Maven 或 mvnd，请先安装"
}

# 检查 assembly plugin
has_assembly_plugin() {
    local pom_file=$1
    if [ ! -f "$pom_file" ]; then
        return 1
    fi
    grep -q "<artifactId>maven-assembly-plugin</artifactId>" "$pom_file"
}

# 检查 Spring Boot 插件
has_spring_boot_plugin() {
    local pom_file=$1
    if [ ! -f "$pom_file" ]; then
        return 1
    fi
    grep -q "<artifactId>spring-boot-maven-plugin</artifactId>" "$pom_file"
}

has_main_class_config() {
    local pom_file=$1
    if [ ! -f "$pom_file" ]; then
        return 1
    fi
    grep -q "<mainClass>" "$pom_file"
}

has_application_entrypoint() {
    local module_dir=$1
    if [ ! -d "$module_dir/src/main/java" ]; then
        return 1
    fi
    find "$module_dir/src/main/java" -type f -name "*Application.java" -print -quit | grep -q .
}

is_deployable_module() {
    local module_dir=$1
    local pom_file="$module_dir/pom.xml"
    if [ ! -f "$pom_file" ]; then
        return 1
    fi
    if has_assembly_plugin "$pom_file"; then
        return 0
    fi
    if ! has_main_class_config "$pom_file" && ! has_spring_boot_plugin "$pom_file"; then
        return 1
    fi
    has_application_entrypoint "$module_dir"
}

get_artifact_id_from_pom() {
    local pom_file=$1
    awk '
        /<parent>/ { in_parent=1 }
        /<\/parent>/ { in_parent=0; next }
        !in_parent && match($0, /<artifactId>[^<]+<\/artifactId>/) {
            value=$0
            sub(/.*<artifactId>/, "", value)
            sub(/<\/artifactId>.*/, "", value)
            print value
            exit
        }
    ' "$pom_file"
}

# 查找所有包含模块定义的 pom.xml 文件
find_pom_files_with_modules() {
    collect_reactor_pom_files "$PROJECT_ROOT_DIR/pom.xml" "$1"
    eval "log_debug \"找到所有包含 <module> 的 pom.xml 文件数量: \${#${1}[@]}\""
}

# 处理单个模块的完全匹配
match_module_exact() {
    local module_pattern=$1
    eval "local -a pom_files_ref=(\"\${${2}[@]:-}\")"
    local matched_modules_ref_name=$3
    local found=false
    local temp_file=$(mktemp)

    for pom_file in "${pom_files_ref[@]}"; do
        local module_dir=$(dirname "$pom_file")
        local relative_path=""
        if [ "$module_dir" != "$PROJECT_ROOT_DIR" ]; then
            relative_path=$(echo "$module_dir" | sed "s|^$PROJECT_ROOT_DIR/||")
        fi
        
        extract_modules_from_pom "$pom_file" "$temp_file"
        while IFS= read -r module_name; do
            if [[ "$module_name" == "$module_pattern" ]]; then
                local full_module_path
                if [ -z "$relative_path" ]; then
                    full_module_path="$module_name"
                else
                    full_module_path="$relative_path/$module_name"
                fi
                eval "${matched_modules_ref_name}+=(\"\$full_module_path\")"
                found=true
                log_debug "完全匹配到模块: '$module_name'，完整路径: '$full_module_path'"
            fi
        done < "$temp_file"
    done

    rm -f "$temp_file"

    if [[ "$found" == true ]]; then
        return 0  # 成功匹配
    else
        return 1  # 未匹配
    fi
}

# 处理单个模块的模糊匹配
match_module_fuzzy() {
    local module_pattern=$1
    eval "local -a pom_files_ref=(\"\${${2}[@]:-}\")"
    local matched_modules_ref_name=$3

    log_debug "未完全匹配到 '$module_pattern'，尝试模糊匹配..."
    local found=false
    local temp_file=$(mktemp)

    for pom_file in "${pom_files_ref[@]}"; do
        local module_dir=$(dirname "$pom_file")
        local relative_path=""
        if [ "$module_dir" != "$PROJECT_ROOT_DIR" ]; then
            relative_path=$(echo "$module_dir" | sed "s|^$PROJECT_ROOT_DIR/||")
        fi
        
        extract_modules_from_pom "$pom_file" "$temp_file"
        while IFS= read -r module_name; do
            # 检查模块名是否包含用户输入的模式（如 service-api 包含 api）
            if [[ "$module_name" == *"$module_pattern"* ]]; then
                local full_module_path
                if [ -z "$relative_path" ]; then
                    full_module_path="$module_name"
                else
                    full_module_path="$relative_path/$module_name"
                fi
                eval "${matched_modules_ref_name}+=(\"\$full_module_path\")"
                log_debug "模块名包含模式匹配到: '$module_name'，完整路径: '$full_module_path'"
                found=true
            # 或者检查完整路径是否包含用户输入的模式（如 services/example/service-api 包含 service-api）
            else
                local full_module_path
                if [ -z "$relative_path" ]; then
                    full_module_path="$module_name"
                else
                    full_module_path="$relative_path/$module_name"
                fi
                if [[ "$full_module_path" == *"$module_pattern"* ]]; then
                    eval "${matched_modules_ref_name}+=(\"\$full_module_path\")"
                    log_debug "完整路径包含模式匹配到: '$module_name'，完整路径: '$full_module_path'"
                    found=true
                fi
            fi
        done < "$temp_file"
    done

    rm -f "$temp_file"

    if [[ "$found" == true ]]; then
        return 0  # 成功匹配
    else
        return 1  # 未匹配
    fi
}

# 查找匹配的模块（支持模糊匹配模块名）
find_matching_modules() {
    local target_modules="$1"
    log_debug "进入 find_matching_modules 函数，目标模块: $target_modules"

    # 1. 判断模块参数是否为空
    if [ -z "$target_modules" ]; then
        log_debug "目标模块参数为空，返回空字符串。"
        return 0  # 无模块参数，直接返回
    fi

    # 2. 处理逗号分隔的模块列表
    IFS=',' read -ra MODULE_ARRAY <<< "$target_modules"
    local matched_modules=()
    log_debug "待匹配的模块模式: ${MODULE_ARRAY[*]}"

    # 3. 获取所有包含 <module> 的 pom.xml 文件（绝对路径）
    local all_pom_files=()
    find_pom_files_with_modules all_pom_files

    # 4. 遍历每个模块名进行匹配
    for module_pattern in "${MODULE_ARRAY[@]}"; do
        module_pattern=$(echo "$module_pattern" | tr -d ' ')
        # 验证模块名不为空
        if [ -z "$module_pattern" ]; then
            log_warn "跳过空模块名"
            continue
        fi
        log_debug "正在处理模块模式: '$module_pattern'"
    
        # 先尝试完全匹配模块名
        if match_module_exact "$module_pattern" all_pom_files matched_modules; then
            continue
        fi
    
        # 若未找到完全匹配，再启用模糊匹配
        if ! match_module_fuzzy "$module_pattern" all_pom_files matched_modules; then
            log_error "⚠️ 未找到模块: $module_pattern"
            log_error "   可使用 --list-modules 查看所有可用模块"
            die "模块匹配失败"
        fi
    done

    # 6. 去重并输出匹配结果（逗号分隔）
    local joined_modules=$(printf ',%s' "${matched_modules[@]}" | sed 's/^,//' | sort -u)
    log_debug "最终匹配到的去重模块列表: '$joined_modules'"

    # 7. 输出结果（可用于 mvn -pl 参数）
    echo "$joined_modules"
}

# 构建 Maven 命令
build_maven_command() {
    local maven_args=()

    # 基础命令
    if [[ "$CLEAN_BUILD" == true ]]; then
        maven_args+=("clean")
    fi
    maven_args+=("package")
    maven_args+=("-B" "-ntp")

    # 跳过测试
    if [[ "$SKIP_TESTS" == true ]]; then
        maven_args+=("-Dmaven.test.skip=true")
    fi

    # CI/本地打包通常不应改写 .flattened-pom.xml；发布流程需要执行 flatten 时可设置 FLATTEN_SKIP=false。
    if [[ "$FLATTEN_SKIP" == true ]]; then
        maven_args+=("-Dflatten.skip=true")
    fi

    # 并行构建参数
    # mvn 使用 -T 参数，mvnd 使用 --threads 参数
    if [ -n "$PARALLEL_THREADS" ]; then
        if [[ "$BUILD_COMMAND" == *"mvnd"* ]]; then
            maven_args+=("--threads" "$PARALLEL_THREADS")
        elif [[ "$BUILD_COMMAND" == *"mvn"* ]]; then
            maven_args+=("-T" "$PARALLEL_THREADS")
        fi
    fi

    # Maven profile (总是添加，除非是默认的dev profile)
    if [ "$MAVEN_PROFILE" != "$DEFAULT_MAVEN_PROFILE" ]; then
        maven_args+=("-P$MAVEN_PROFILE")
    fi

    # 离线模式
    if [[ "$OFFLINE_MODE" == true ]]; then
        maven_args+=("--offline")
    fi

    # 模块选择
    if [ -n "$TARGET_MODULE" ]; then
        local modules_to_build=$(find_matching_modules "$TARGET_MODULE" | tr '\n' ',' | sed 's/,$//')
        if [ -n "$modules_to_build" ]; then
            maven_args+=("-pl" "$modules_to_build" "-am")
        else
            log_warn "构建全部模块"
        fi
    fi

    echo "$BUILD_COMMAND ${maven_args[*]}"
}

# 设置构建环境
setup_build_environment() {
    local quiet_config="${1:-false}"

    # 检查项目根目录
    if [ ! -f "$PROJECT_ROOT_DIR/pom.xml" ]; then
        die "未找到根 pom.xml 文件: $PROJECT_ROOT_DIR/pom.xml"
    fi
    # 检查 Java 环境
    check_java_version
    # 配置 Maven 工具
    setup_maven_daemon
    # 重新获取CPU核心数以确保准确性
    cpu_cores=$(get_cpu_cores)
	# 计算推荐线程数：1C 是最安全的选择，避免OOM
	PARALLEL_THREADS="1C"
    # 设置 Maven 环境变量
    export MAVEN_OPTS="${MAVEN_OPTS:-"-Xmx2048m"}"

    log_debug "构建工具: $BUILD_COMMAND"
    log_debug "目标模块: ${TARGET_MODULE:-"所有模块"}"
    log_debug "离线模式: $OFFLINE_MODE"
    log_debug "并行线程: $PARALLEL_THREADS"
}

# 预处理配置文件
preprocess_config_files() {
    log_debug "预处理配置文件..."

    # 处理 assembly.xml 中的路径变量
    local assembly_file="$PROJECT_ROOT_DIR/assembly.xml"
    if [ -f "$assembly_file" ]; then
        log_debug "处理 assembly.xml 路径变量"
        # 创建备份文件
        if ! cp "$assembly_file" "${assembly_file}.bak"; then
            log_warn "无法创建 $assembly_file 的备份文件"
        fi
        # 执行替换操作
        if sed -i "s#\${basedir}#$PROJECT_ROOT_DIR#" "$assembly_file"; then
            log_debug "成功处理 assembly.xml 路径变量"
            # 删除备份文件
            rm -f "${assembly_file}.bak"
        else
            log_error "处理 assembly.xml 路径变量失败"
            # 恢复备份文件
            if [ -f "${assembly_file}.bak" ]; then
                mv "${assembly_file}.bak" "$assembly_file"
                log_info "已恢复 assembly.xml 备份文件"
            fi
            die "配置文件处理失败"
        fi
    fi
}

# 显示构建失败摘要
print_build_failure_excerpt() {
    local log_file=$1

    log_error "=== 构建错误摘要 ==="
    if grep -E "^\[ERROR\]|Exception in thread|BUILD FAILURE|Failed to execute goal|Compilation failure|Could not resolve|Could not transfer|Non-resolvable|No tests matching pattern" "$log_file" >/dev/null 2>&1; then
        grep -E "^\[ERROR\]|Exception in thread|BUILD FAILURE|Failed to execute goal|Compilation failure|Could not resolve|Could not transfer|Non-resolvable|No tests matching pattern" "$log_file" \
            | tail -n 80 >&2 || true
    else
        log_error "未匹配到标准错误行，输出日志末尾:"
        tail -n 80 "$log_file" >&2 || true
    fi
}

print_reactor_summary() {
    local log_file=$1
    local root_artifact
    root_artifact=$(get_artifact_id_from_pom "$PROJECT_ROOT_DIR/pom.xml")

    if [ ! -f "$log_file" ]; then
        return 0
    fi

    local summary
    summary=$(awk '
        /Reactor Summary/ { in_summary=1; next }
        in_summary && /BUILD (SUCCESS|FAILURE)/ { exit }
        in_summary && /^\[INFO\] -+$/ { next }
        in_summary && /^\[INFO\] $/ { next }
        in_summary && /^\[INFO\]/ {
            line=$0
            sub(/^\[INFO\] /, "", line)
            if (match(line, /(SUCCESS|FAILURE|SKIPPED)[[:space:]]+\[[^]]+\][[:space:]]*$/)) {
                status=substr(line, RSTART, RLENGTH)
                name=substr(line, 1, RSTART - 1)
                gsub(/[ .]+$/, "", name)
                gsub(/[[:space:]]+/, " ", status)
                if (name == root_artifact) {
                    print name ": " status
                } else {
                    print "  - " name ": " status
                }
            }
        }
    ' root_artifact="$root_artifact" "$log_file")

    if [ -n "$summary" ]; then
        log_info "模块结果:"
        echo "$summary"
    fi
}

# 执行构建
execute_build() {
    local maven_command
    maven_command=$(build_maven_command)

    log_info "开始构建: $maven_command"

    # 切换到项目根目录
    cd "$PROJECT_ROOT_DIR" || die "无法切换到项目目录: $PROJECT_ROOT_DIR"

    # 创建临时文件保存日志
    BUILD_LOG_FILE=$(mktemp)
    log_debug "构建日志将保存在: $BUILD_LOG_FILE"

    local build_start_time
    build_start_time=$(date +%s)
    local maven_exit_code

    CMD_ARRAY=($maven_command)
    set +e
    if [[ "$VERBOSE_MODE" == true ]]; then
        "${CMD_ARRAY[@]}" 2>&1 | tee "$BUILD_LOG_FILE"
        maven_exit_code=${PIPESTATUS[0]}
    else
        "${CMD_ARRAY[@]}" > "$BUILD_LOG_FILE" 2>&1
        maven_exit_code=$?
    fi
    set -e

    local build_end_time
    build_end_time=$(date +%s)
    local build_duration=$((build_end_time - build_start_time))

    if [ $maven_exit_code -ne 0 ] || grep -q "Exception in thread" "$BUILD_LOG_FILE"; then
        log_error "构建失败（耗时: ${build_duration}s）"
        if [[ "$VERBOSE_MODE" != true ]]; then
            print_build_failure_excerpt "$BUILD_LOG_FILE"
            log_error "如需查看完整构建输出，请使用 --verbose 或 -v 重新执行。"
        fi
        FAILURE_REPORTED=true
        return 1
    fi

    if [[ "$VERBOSE_MODE" != true ]]; then
        print_reactor_summary "$BUILD_LOG_FILE"
    fi
    log_info "构建成功（耗时: ${build_duration}s）"
    return 0
}

# 分析构建输出并更新依赖配置文件
analyze_build_output() {
    if [ -n "$TARGET_MODULE" ]; then
        log_debug "指定了模块构建，跳过依赖配置文件更新"
        return 0
    fi

    log_debug "分析构建输出并更新依赖配置文件..."
    
    if [ ! -f "$BUILD_LOG_FILE" ]; then
        log_warn "找不到构建日志文件，跳过依赖分析"
        return 0
    fi

    # 从构建日志文件中提取构建顺序
    build_order=$(extract_build_order_from_log)

    if [ -n "$build_order" ]; then
        write_dependency_file "$build_order"
    else
        log_warn "未能提取构建顺序"
    fi
}

find_module_dir_by_artifact() {
    local target_artifact=$1
    local reactor_poms=()
    local parent_pom

    collect_reactor_pom_files "$PROJECT_ROOT_DIR/pom.xml" reactor_poms

    for parent_pom in "${reactor_poms[@]}"; do
        local parent_dir temp_file
        parent_dir=$(dirname "$parent_pom")
        temp_file=$(mktemp)
        extract_modules_from_pom "$parent_pom" "$temp_file"

        while IFS= read -r module_name; do
            local child_pom="${parent_dir}/${module_name}/pom.xml"
            if [ -f "$child_pom" ]; then
                local artifact_id
                artifact_id=$(get_artifact_id_from_pom "$child_pom")
                if [ "$artifact_id" = "$target_artifact" ]; then
                    rm -f "$temp_file"
                    dirname "$child_pom"
                    return 0
                fi
            fi
        done < "$temp_file"

        rm -f "$temp_file"
    done

    return 1
}

find_boot_jar_for_module() {
    local module_dir=$1
    local artifact_id=$2

    find "$module_dir/target" -maxdepth 1 -type f \
        -name "${artifact_id}-*.jar" \
        ! -name "original-*" \
        ! -name "*-sources.jar" \
        ! -name "*-javadoc.jar" \
        -print 2>/dev/null | sort | tail -n 1
}

copy_if_exists() {
    local source_path=$1
    local target_path=$2

    if [ -e "$source_path" ]; then
        mkdir -p "$(dirname "$target_path")"
        cp -R "$source_path" "$target_path"
    fi
}

create_boot_deploy_package() {
    local service_name=$1
    local module_dir=$2
    local jar_file=$3
    local jar_base version stage_dir package_root package_name package_file

    jar_base=$(basename "$jar_file" .jar)
    version="${jar_base#${service_name}-}"
    package_name="${service_name}-${version}-bin"
    package_file="${module_dir}/target/${package_name}.tar.gz"

    if [ -f "$package_file" ]; then
        return 0
    fi

    stage_dir=$(mktemp -d)
    package_root="${stage_dir}/${package_name}"
    mkdir -p "${package_root}/bin" "${package_root}/lib"

    for script_name in start.sh stop.sh status.sh; do
        if [ -f "${PROJECT_ROOT_DIR}/${script_name}" ]; then
            cp "${PROJECT_ROOT_DIR}/${script_name}" "${package_root}/bin/"
            chmod +x "${package_root}/bin/${script_name}"
        fi
    done

    cp "$jar_file" "${package_root}/lib/"

    local classes_dir="${module_dir}/target/classes"
    if [ -d "$classes_dir" ]; then
        mkdir -p "${package_root}/config"
        find "$classes_dir" -maxdepth 1 -type f \( -name "*.yml" -o -name "*.yaml" -o -name "*.xml" \) \
            -exec cp {} "${package_root}/config/" \; 2>/dev/null || true

        if [ -d "${classes_dir}/mapper" ]; then
            cp -R "${classes_dir}/mapper" "${package_root}/mapper"
        fi
    fi

    local resources_dir="${module_dir}/src/main/resources"
    if [ -d "$resources_dir" ]; then
        copy_if_exists "${resources_dir}/fonts" "${package_root}/fonts"
        copy_if_exists "${resources_dir}/images" "${package_root}/images"
        copy_if_exists "${resources_dir}/defaultImages" "${package_root}/defaultImages"
    fi

    tar -czf "$package_file" -C "$stage_dir" "$package_name"
    rm -rf "$stage_dir"
    log_info "已生成部署包: $package_file"
}

create_missing_boot_deploy_packages() {
    if [ ! -f "$DEP_FILE" ]; then
        log_warn "未找到服务依赖顺序文件，跳过 Spring Boot 部署包补生成"
        return 0
    fi

    # shellcheck disable=SC1090
    source "$DEP_FILE"

    if [[ -z ${SERVICE_DEPENDENCY_ORDER+x} ]] || [ ${#SERVICE_DEPENDENCY_ORDER[@]} -eq 0 ]; then
        log_warn "服务依赖顺序为空，跳过 Spring Boot 部署包补生成"
        return 0
    fi

    local service_name
    for service_name in "${SERVICE_DEPENDENCY_ORDER[@]}"; do
        local module_dir
        module_dir=$(find_module_dir_by_artifact "$service_name" || true)
        [ -z "$module_dir" ] && continue

        if has_assembly_plugin "${module_dir}/pom.xml"; then
            continue
        fi

        if ! is_deployable_module "$module_dir"; then
            continue
        fi

        local jar_file
        jar_file=$(find_boot_jar_for_module "$module_dir" "$service_name")
        if [ -z "$jar_file" ]; then
            log_warn "未找到 $service_name 的可执行 jar，无法生成部署包"
            continue
        fi

        create_boot_deploy_package "$service_name" "$module_dir" "$jar_file"
    done
}
# 提取构建顺序
extract_build_order_from_log() {
    # 1. 从构建日志文件中提取模块顺序
    local raw_order=$(cat "$BUILD_LOG_FILE" | awk '/Reactor Summary/,/BUILD SUCCESS/' | grep -E "^\[INFO\] " | awk '{gsub(/SUCCESS.*/, ""); print $2}' | grep -v "^$" | grep -Ev "Reactor|BUILD")
    
    # 2. 获取父模块名称（第一个模块通常是父模块）
    local parent_module=$(echo "$raw_order" | head -n 1)
    
    # 3. 过滤掉父模块
    local filtered_order=$(echo "$raw_order" | grep -v "^$parent_module$")

    # 4. 找出可部署 Spring Boot 应用模块。
    # 显式 <mainClass> 优先；历史模块如只声明 Boot 插件，也必须同时存在 *Application 启动类。
    local pom_files=()
    while IFS= read -r -d '' pom_file; do
        local module_dir
        module_dir=$(dirname "$pom_file")
        if is_deployable_module "$module_dir"; then
            pom_files+=("$pom_file")
        fi
    done < <(find "$PROJECT_ROOT_DIR" -type f -name "pom.xml" ! -path "*/target/*" -print0)

    # 5. 提取部署顺序
    local deploy_order=()
    while IFS= read -r module_name; do
        for pom_file in "${pom_files[@]}"; do
            if [ -n "$pom_file" ] && [ -f "$pom_file" ]; then
                local artifact_id
                artifact_id=$(get_artifact_id_from_pom "$pom_file")
                if [ "$artifact_id" = "$module_name" ]; then
                    deploy_order+=("$module_name")
                    break
                fi
            fi
        done
    done <<< "$filtered_order"

    # 6. 输出结果
    printf '%s\n' "${deploy_order[@]}"
}

ensure_dependency_file_ignored() {
    local gitignore_file="$PROJECT_ROOT_DIR/.gitignore"

    if [ ! -f "$gitignore_file" ]; then
        log_warn "未检测到 .gitignore，建议手动忽略 $DEP_FILE_IGNORE_ENTRY"
        return 0
    fi

    if grep -qxF "$DEP_FILE_IGNORE_ENTRY" "$gitignore_file"; then
        return 0
    fi

    {
        printf '\n'
        printf '# package.sh 自动生成的服务依赖顺序文件\n'
        printf '%s\n' "$DEP_FILE_IGNORE_ENTRY"
    } >> "$gitignore_file"
    log_info "已将 $DEP_FILE_IGNORE_ENTRY 添加到 .gitignore"
}

# 生成并写入依赖顺序配置文件
write_dependency_file() {
    local build_order=$1
    ensure_dependency_file_ignored

    local built_services=" "
    local built_service
    while IFS= read -r built_service; do
        [ -n "$built_service" ] && built_services+="$built_service "
    done <<< "$build_order"

    local ordered_services=()
    local module_paths=()
    local helm_keys=()
    local row
    while IFS=$'\t' read -r _order module_path service_name helm_key; do
        [ -z "$service_name" ] && continue
        if [[ "$built_services" != *" $service_name "* ]]; then
            continue
        fi
        ordered_services+=("$service_name")
        module_paths+=("$module_path")
        helm_keys+=("$helm_key")
    done < <(collect_deployable_module_rows)

    local services="${ordered_services[*]}"
    local module_path_values="${module_paths[*]}"
    local helm_key_values="${helm_keys[*]}"
    # 写入文件
    {
        echo "# 由 package.sh 在 $(date) 自动生成,请勿手动编辑此文件"
        echo "readonly SERVICE_DEPENDENCY_ORDER=(\"${services// /\" \"}\")"
        echo "readonly SERVICE_MODULE_PATHS=(\"${module_path_values// /\" \"}\")"
        echo "readonly SERVICE_HELM_KEYS=(\"${helm_key_values// /\" \"}\")"
    } > "$DEP_FILE"
    
    log_info "已生成服务依赖顺序文件: $DEP_FILE"
    log_debug "服务依赖顺序: $services"
}


# 主函数
main() {
    log_debug "项目根目录: $PROJECT_ROOT_DIR"

    # 解析参数
    parse_arguments "$@"

    # 设置构建环境
    setup_build_environment

    # 预处理配置文件
    # preprocess_config_files

    # 执行构建
    if execute_build; then
        analyze_build_output
        create_missing_boot_deploy_packages
        exit 0
    else
        exit 1
    fi
}

# 脚本入口
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
