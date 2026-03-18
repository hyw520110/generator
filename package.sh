#!/bin/bash

# 通用打包脚本
# 支持指定 Maven profile，并自动分析服务依赖顺序，更新部署脚本。

set -euo pipefail  # 严格错误处理模式

# --- 全局变量定义 ---
readonly SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
readonly PROJECT_ROOT_DIR="$SCRIPT_DIR"
readonly DEFAULT_MAVEN_PROFILE="dev"
readonly TOOLS_DIR="${PROJECT_ROOT_DIR}/.tools"
readonly MVND_DIR="${TOOLS_DIR}/mvnd"
readonly DEP_FILE="$PROJECT_ROOT_DIR/.service-order.conf"
# 全局变量
MAVEN_PROFILE="${DEFAULT_MAVEN_PROFILE}"
TARGET_MODULE=""
OFFLINE_MODE=false
CLEAN_BUILD=true
SKIP_TESTS=true
PARALLEL_THREADS="2C"
BUILD_COMMAND=""
VERBOSE_MODE=false
maven_output=""
OS=$(uname -s)
cpu_cores=2  # 默认值

# 功能开关
ENABLE_DEPENDENCY_ANALYSIS=false  # 是否启用依赖分析（默认关闭）

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
  --verbose, -v       详细输出模式
  --list-modules      列出所有可用模块
  --dry-run          只显示将要执行的命令，不实际执行
  -h, --help         显示此帮助信息

示例:
  $0                                    # 使用默认配置构建所有模块
  $0 -P prod                              # 使用 prod profile 构建
  $0 -m user                          # 构建 user 相关模块
  $0 -P prod --offline -m payment        # 离线构建 payment 模块
  $0 --no-clean --with-tests          # 不清理且执行测试
  $0 --list-modules                   # 列出所有可用模块
  $0 --dry-run prod -m user           # 预览构建命令

环境变量:
  JAVA_HOME           Java 安装目录
  MAVEN_OPTS          Maven JVM 选项
  ENABLE_DEPENDENCY_ANALYSIS  启用依赖分析（默认：false）

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
    echo -e "$(date '+%Y-%m-%d %H:%M:%S') ${YELLOW}[WARN]${NC} $*"
}

log_debug() {
    if [[ "$VERBOSE_MODE" == true ]]; then
        echo -e "$(date '+%Y-%m-%d %H:%M:%S') ${BLUE}[DEBUG]${NC} $*"
    fi
}

# 错误退出函数
die() {
    log_error "$*"
    log_error "构建失败"
    exit 1
}
# 清理函数
cleanup() {
    local exit_code=$?
    if [ $exit_code -ne 0 ]; then
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

    if [[ "$dry_run" == true ]]; then
        perform_dry_run
        exit 0
    fi

}

# 统计可部署模块数量
count_deployable_modules() {
    local -n pom_files_ref=$1
    local count=0
    
    for pom_file in "${pom_files_ref[@]}"; do
        local module_dir=$(dirname "$pom_file")
        local relative_path=""
        if [ "$module_dir" != "$PROJECT_ROOT_DIR" ]; then
            relative_path=$(echo "$module_dir" | sed "s|^$PROJECT_ROOT_DIR/||")
        fi
        
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
            
            if [ -d "$module_path" ] && has_assembly_plugin "$module_path/pom.xml"; then
                count=$((count + 1))
            fi
        done < <(grep -E "<module>[^<]*</module>" "$pom_file" | sed -E "s/.*<module>([^<]+)<\/module>.*/\1/")
    done
    
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

    # 遍历所有包含模块的 pom.xml 文件并显示
    for pom_file in "${pom_files[@]}"; do
        local module_dir=$(dirname "$pom_file")
        local relative_path=""
        if [ "$module_dir" != "$PROJECT_ROOT_DIR" ]; then
            relative_path=$(echo "$module_dir" | sed "s|^$PROJECT_ROOT_DIR/||")
        fi
        
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
                if has_assembly_plugin "$module_path/pom.xml"; then
                    printf "  %-30s [✓]\n" "$full_module_path"
                else
                    printf "  %-30s [ ]\n" "$full_module_path"
                fi
            fi
        done < <(grep -E "<module>[^<]*</module>" "$pom_file" | sed -E "s/.*<module>([^<]+)<\/module>.*/\1/")
    done

    # 显示统计信息
    echo "说明: [✓] = 包含 assembly plugin（$deployable_count 个可部署）"
}

# 预览构建命令
perform_dry_run() {
    # 显示配置
    echo "构建配置:"
    echo "  Maven Profile: $MAVEN_PROFILE"
    #echo "  目标模块: ${TARGET_MODULE:-"所有模块"}"
    echo "  离线模式: $OFFLINE_MODE"
    echo "  清理构建: $CLEAN_BUILD"
    echo "  跳过测试: $SKIP_TESTS"
    echo "  并行线程: $PARALLEL_THREADS"
    echo "  详细输出: $VERBOSE_MODE"
    echo

    # 模拟构建命令构造
    setup_build_environment
    local maven_command
    maven_command=$(build_maven_command)

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

    local current_version
    current_version=$(java -version 2>&1 | grep -E "(openjdk version|java version)" | awk -F'"' '{print $2}' | cut -d. -f1)

    local required_version
    if [ -f "$PROJECT_ROOT_DIR/pom.xml" ]; then
        required_version=$(grep -E "<java.version>" "$PROJECT_ROOT_DIR/pom.xml" | head -1 | sed -E 's/.*<java.version>([^<]+)<\/java.version>.*/\1/')
    fi

    if [ -z "$current_version" ]; then
        die "无法检测 Java 版本"
    fi

    if [ -n "$required_version" ] && [ "$current_version" != "$required_version" ]; then
        die "Java 版本不匹配: 当前 $current_version，需要 $required_version"
    fi

    log_info "Java 版本检查通过: $current_version"

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

# 检查和安装 mvnd
setup_maven_daemon() {
    log_debug "配置 Maven 构建工具..."

    # 检查项目本地 mvnd
    if [ -f "$MVND_DIR/bin/mvnd" ]; then
        BUILD_COMMAND="$MVND_DIR/bin/mvnd"
        log_info "使用项目本地 mvnd: $BUILD_COMMAND"
        return 0
    fi

    # 检查全局 mvnd
    if command -v mvnd >/dev/null 2>&1; then
        BUILD_COMMAND="mvnd"
        log_info "使用全局 mvnd: $(which mvnd)"
        return 0
    fi

    # 尝试安装 mvnd
    if [ -f "$SCRIPT_DIR/mvnd.sh" ]; then
        log_info "尝试安装 mvnd..."
        if bash "$SCRIPT_DIR/mvnd.sh"; then
            # 重新检查
            if command -v mvnd >/dev/null 2>&1; then
                BUILD_COMMAND="mvnd"
                log_info "mvnd 安装成功"
                return 0
            fi
        fi
    fi

    # 回退到 Maven
    if command -v mvn >/dev/null 2>&1; then
        BUILD_COMMAND="mvn"
        log_info "使用 Maven: $(which mvn)"
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

# 查找所有包含模块定义的 pom.xml 文件
find_pom_files_with_modules() {
    local -n pom_files_ref=$1
    mapfile -t pom_files_ref < <(
        find "$PROJECT_ROOT_DIR" -type f -name "pom.xml" ! -path "*/target/*" -print0 |
        xargs -0 grep -l "<module>" |
        sort -u
    )
    log_debug "找到所有包含 <module> 的 pom.xml 文件数量: ${#pom_files_ref[@]}"
}

# 处理单个模块的完全匹配
match_module_exact() {
    local module_pattern=$1
    local -n pom_files_ref=$2
    local -n matched_modules_ref=$3
    local found=false

    for pom_file in "${pom_files_ref[@]}"; do
        local module_dir=$(dirname "$pom_file")
        local relative_path=""
        if [ "$module_dir" != "$PROJECT_ROOT_DIR" ]; then
            relative_path=$(echo "$module_dir" | sed "s|^$PROJECT_ROOT_DIR/||")
        fi
        
        while IFS= read -r module_name; do
            if [[ "$module_name" == "$module_pattern" ]]; then
                local full_module_path
                if [ -z "$relative_path" ]; then
                    full_module_path="$module_name"
                else
                    full_module_path="$relative_path/$module_name"
                fi
                matched_modules_ref+=("$full_module_path")
                found=true
                log_debug "完全匹配到模块: '$module_name'，完整路径: '$full_module_path'"
            fi
        done < <(grep -E "<module>[^<]*</module>" "$pom_file" | sed -E "s/.*<module>([^<]+)<\/module>.*/\1/")
    done

    if [[ "$found" == true ]]; then
        return 0  # 成功匹配
    else
        return 1  # 未匹配
    fi
}

# 处理单个模块的模糊匹配
match_module_fuzzy() {
    local module_pattern=$1
    local -n pom_files_ref=$2
    local -n matched_modules_ref=$3

    log_debug "未完全匹配到 '$module_pattern'，尝试模糊匹配..."
    local found=false
    for pom_file in "${pom_files_ref[@]}"; do
        local module_dir=$(dirname "$pom_file")
        local relative_path=""
        if [ "$module_dir" != "$PROJECT_ROOT_DIR" ]; then
            relative_path=$(echo "$module_dir" | sed "s|^$PROJECT_ROOT_DIR/||")
        fi
        
        while IFS= read -r module_name; do
            # 检查模块名是否包含用户输入的模式（如 user-api 包含 api）
            if [[ "$module_name" == *"$module_pattern"* ]]; then
                local full_module_path
                if [ -z "$relative_path" ]; then
                    full_module_path="$module_name"
                else
                    full_module_path="$relative_path/$module_name"
                fi
                matched_modules_ref+=("$full_module_path")
                log_debug "模块名包含模式匹配到: '$module_name'，完整路径: '$full_module_path'"
                found=true
            # 或者检查完整路径是否包含用户输入的模式（如 domain-services/user/user-api 包含 user-api）
            else
                local full_module_path
                if [ -z "$relative_path" ]; then
                    full_module_path="$module_name"
                else
                    full_module_path="$relative_path/$module_name"
                fi
                if [[ "$full_module_path" == *"$module_pattern"* ]]; then
                    matched_modules_ref+=("$full_module_path")
                    log_debug "完整路径包含模式匹配到: '$module_name'，完整路径: '$full_module_path'"
                    found=true
                fi
            fi
        done < <(grep -E "<module>[^<]*</module>" "$pom_file" | sed -E "s/.*<module>([^<]+)<\/module>.*/\1/")
    done

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

    # 跳过测试
    if [[ "$SKIP_TESTS" == true ]]; then
        maven_args+=("-Dmaven.test.skip=true")
    fi

    # 并行构建参数
    # mvn 使用 -T 参数，mvnd 使用 --threads 参数
    if [ -n "$PARALLEL_THREADS" ]; then
        if [[ "$BUILD_COMMAND" == *"mvn"* ]]; then
            maven_args+=("-T" "$PARALLEL_THREADS")
        elif [[ "$BUILD_COMMAND" == *"mvnd"* ]]; then
            maven_args+=("--threads" "$PARALLEL_THREADS")
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

    # 详细输出
    if [[ "$VERBOSE_MODE" == true ]]; then
        maven_args+=("-X")
    fi

    echo "$BUILD_COMMAND ${maven_args[*]}"
}

# 设置构建环境
setup_build_environment() {
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

    log_info "Maven Profile: $MAVEN_PROFILE"
    log_debug "构建工具: $BUILD_COMMAND"
    log_debug "目标模块: ${TARGET_MODULE:-"所有模块"}"
    log_info "离线模式: $OFFLINE_MODE"
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

    # 使用 tee 同时输出到屏幕和文件
    # set -o pipefail 已在脚本开头启用，所以如果 maven 失败，管道返回码会体现
    
    # 数组展开执行命令，并通过管道传给 tee
    # 注意：为了让 PIPESTATUS 正常工作，这里需要小心处理
    # bash 的 pipefail 会让管道中任意一个非0退出码导致整体返回非0
    
    # 执行命令
    # 注意：mvn 可能会产生大量输出，tee 保证用户能看到实时进度
    CMD_ARRAY=($maven_command)
    "${CMD_ARRAY[@]}" 2>&1 | tee "$BUILD_LOG_FILE"
    
    # 获取 pipe 的返回状态 (这里依靠 set -e -o pipefail)
    # 但由于 set -e，如果命令失败脚本会直接退出到 cleanup
    # 我们想要手动处理错误，所以暂时关闭 set -e
    set +e
    maven_exit_code=${PIPESTATUS[0]}
    set -e

    local build_end_time
    build_end_time=$(date +%s)
    local build_duration=$((build_end_time - build_start_time))

    if [ $maven_exit_code -ne 0 ]; then
        log_error "构建失败（耗时: ${build_duration}s）"
        # 错误已经在屏幕上显示了，这里不需要再次 grep 显示
        return 1
    fi

    log_info "构建成功（耗时: ${build_duration}s）"
    return 0
}

# 显示构建错误
show_build_errors() {
    local maven_output=$1

    log_error "=== 构建错误信息 ==="

    # 提取错误信息
    local errors
    errors=$(echo "$maven_output" | grep -E "\[ERROR\]" | head -10)

    if [ -n "$errors" ]; then
        echo "$errors"
    fi
}

# 分析构建输出并更新依赖配置文件
analyze_build_output() {
    if [ -n "$TARGET_MODULE" ]; then
        log_debug "指定了模块构建，跳过依赖配置文件更新"
        return 0
    fi

    log_info "分析构建输出并更新依赖配置文件..."
    
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
# 提取构建顺序
extract_build_order_from_log() {
    # 1. 从构建日志文件中提取模块顺序
    local raw_order=$(cat "$BUILD_LOG_FILE" | awk '/Reactor Summary/,/BUILD SUCCESS/' | grep -E "^\[INFO\] " | awk '{gsub(/SUCCESS.*/, ""); print $2}' | grep -v "^$" | grep -Ev "Reactor|BUILD")
    
    # 2. 获取父模块名称（第一个模块通常是父模块）
    local parent_module=$(echo "$raw_order" | head -n 1)
    
    # 3. 过滤掉父模块
    local filtered_order=$(echo "$raw_order" | grep -v "^$parent_module$")

    # 4. 找出所有包含mainClass的pom文件（这些是潜在的应用服务）
    local pom_files=$(find "$PROJECT_ROOT_DIR" -type f -name "pom.xml" ! -path "*/target/*" -exec sh -c '
        if grep -q "<mainClass>" "$1"; then
            echo "$1"
        fi
    ' sh {} \;)

    # 5. 提取部署顺序
    local deploy_order=()
    while IFS= read -r module_name; do
        while IFS= read -r pom_file; do
            if [ -n "$pom_file" ] && [ -f "$pom_file" ]; then
                # 提取前15行作为header
                local header=$(head -n 15 "$pom_file")

                # 检查是否包含完全匹配的artifactId，并且前后2行不包含parent标签
                if echo "$header" | grep -q "^[[:space:]]*<artifactId>$module_name</artifactId>[[:space:]]*$" && 
                   ! echo "$header" | grep -B2 -A2 "^[[:space:]]*<artifactId>$module_name</artifactId>[[:space:]]*$" | grep -q "<parent>"; then
                    deploy_order+=("$module_name")
                    break  # 找到匹配就退出内层循环
                fi
            fi
        done <<< "$pom_files"
    done <<< "$filtered_order"

    # 6. 输出结果
    printf '%s\n' "${deploy_order[@]}"
}

# 生成并写入依赖顺序配置文件
write_dependency_file() {
    local build_order=$1
    log_info "正在生成服务依赖顺序文件: $DEP_FILE"
    # 格式化输出为 bash 数组定义
    local services=$(echo "$build_order" | tr '\n' ' ' | sed 's/ *$//')
    # 写入文件
    {
        echo "# 由 package.sh 在 $(date) 自动生成,请勿手动编辑此文件"
        echo "readonly SERVICE_DEPENDENCY_ORDER=(\"${services// /\" \"}\")"
    } > "$DEP_FILE"
    
    grep "SERVICE_DEPENDENCY_ORDER" $DEP_FILE | awk -F'=' '{print $2}'
}


# 主函数
main() {
    log_info "项目根目录: $PROJECT_ROOT_DIR"

    # 解析参数
    parse_arguments "$@"

    # 设置构建环境
    setup_build_environment

    # 预处理配置文件
    # preprocess_config_files

    # 执行构建
    if execute_build; then
        # 根据开关决定是否执行依赖分析
        if [[ "$ENABLE_DEPENDENCY_ANALYSIS" == true ]]; then
            analyze_build_output
        fi
        exit 0
    else
        die "构建失败"
    fi
}

# 脚本入口
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi