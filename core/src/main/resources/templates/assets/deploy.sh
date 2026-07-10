#!/bin/bash

set -Eeuo pipefail

# 全局配置与计时初始化
readonly SCRIPT_START_TIME=$(date +%s)
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/pom.xml" ] || [ -f "$SCRIPT_DIR/package.sh" ]; then
    readonly CURRENT_DIR="$SCRIPT_DIR"
elif [ -f "$SCRIPT_DIR/../pom.xml" ] || [ -f "$SCRIPT_DIR/../package.sh" ]; then
    readonly CURRENT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
else
    readonly CURRENT_DIR="$SCRIPT_DIR"
fi
readonly PROJECT_NAME="${PROJECT_NAME:-$(basename "$CURRENT_DIR")}"
readonly BUILD_FILE="$CURRENT_DIR/package.sh"
readonly DEPENDENCY_FILE="${CURRENT_DIR}/.service-order.conf"
# SSH 优化：增加连接复用与保活
readonly SSH_OPTS='-o StrictHostKeyChecking=no -o ConnectTimeout=10 -o ServerAliveInterval=30 -o ControlMaster=auto -o ControlPath=/tmp/ssh-%r@%h:%p -o ControlPersist=60s'
readonly RSYNC_SSH="ssh $SSH_OPTS"
readonly SCRIPTS_AND_CONFIGS=("start.sh" "stop.sh" "status.sh" "skywalking-agent.sh")
readonly BACKUPS_TO_KEEP_DEFAULT=5

# 默认配置
DEFAULT_DEPLOY_TMP_DIR="$HOME/tmp"
DEFAULT_DEPLOY_BASE_DIR="$HOME/webapps"
DEFAULT_JVM_OPTS="-Xms512m -Xmx1024m"

# 全局状态变量
GZ_FILE_DIR="${GZ_FILE_DIR:-$DEFAULT_DEPLOY_TMP_DIR}"
DEPLOY_BASE_DIR="${DEPLOY_BASE_DIR:-$DEFAULT_DEPLOY_BASE_DIR}"
SERVICE_NAMES="${SERVICE_NAMES:-}"
REMOTES_STR="${REMOTES_STR:-}"
CUSTOM_JVM_OPTS="${CUSTOM_JVM_OPTS:-$DEFAULT_JVM_OPTS}"
CUSTOM_BACKUP_DIR="${CUSTOM_BACKUP_DIR:-}"
STARTUP_TIMEOUT="${STARTUP_TIMEOUT:-60}"
KEEP_BACKUPS="${KEEP_BACKUPS:-$BACKUPS_TO_KEEP_DEFAULT}"
LOG_LEVEL="${LOG_LEVEL:-INFO}"

# 标志位
FAIL_FAST="${FAIL_FAST:-false}"
PERFORM_ROLLBACK="${PERFORM_ROLLBACK:-false}"
FORCE_DEPLOYMENT="${FORCE_DEPLOYMENT:-false}"
FORCE_BUILD="${FORCE_BUILD:-false}"
NO_STARTUP_LOG="${NO_STARTUP_LOG:-false}"
DRY_RUN_CLEANUP="${DRY_RUN_CLEANUP:-false}"
IS_DEV_MODE=false
IS_REMOTE_EXECUTION=false
USE_HASH_DETECTION=true
HAS_DETECTED_CHANGES=false

# 缓存变量
SOURCE_HASHES_MANIFEST="${DEFAULT_DEPLOY_TMP_DIR}/.${PROJECT_NAME}.manifest"
REMOTE_MANIFEST_PATH="${HOME}/tmp/.deploy.manifest"
CACHED_PRUNE_EXPR=""
MODULE_LIST=()

show_help() {
    cat << EOF
部署脚本, 支持本地和远程增量部署。

用法: ./deploy.sh [选项]

选项:
  -s, --services <列表>    需部署的服务名列表，支持多个(,分隔)，不指定默认所有。
  -r, --remotes <列表>     远程主机列表，格式: "user1@host1,user2@host2"。
  --src-dir <目录>         服务包(.tar.gz)源目录。默认: ~/tmp。
  -d, --deploy-dir <目录>  基础部署目录。默认: ~/webapps。
  -j, --jvm <参数>         指定JVM参数。
  --fail-fast             首个服务部署失败时立即终止。
  --rollback              部署失败时自动回滚到上一个版本。
  --backup-dir <目录>      指定备份目录。
  --keep-backups <数量>    保留的备份数量 (默认: 5)。
  --dry-run-cleanup       仅显示备份清理信息而不实际删除。
  --wait <秒>              服务启动等待超时时间 (默认: 60秒)。
  -n, --no-startup-log     启动时不打印应用日志。
  -f, --force              强制部署，忽略版本哈希检查。
  --force-build           强制重新构建所有受影响模块。
  --log-level <级别>       日志级别 (DEBUG, INFO, WARN, ERROR)。
  -h, --help               显示此帮助信息。

EOF
}

# 哈希命令检测与包装
if command -v sha256sum >/dev/null 2>&1; then
    HASH_CMD="sha256sum"
elif command -v shasum >/dev/null 2>&1; then
    HASH_CMD="shasum -a 256"
elif command -v md5sum >/dev/null 2>&1; then
    HASH_CMD="md5sum"
elif command -v md5 >/dev/null 2>&1; then
    HASH_CMD="md5 -q"
else
    echo "错误: 未找到支持的哈希命令 (sha256sum/md5sum/shasum/md5)" >&2; exit 1
fi

# 错误处理函数
error_handler() {
    local code=$1; local line=$2; local command="$3"
    if [ $code -ne 0 ]; then
        printf "\n\033[31m%s\033[0m\n" "$(date '+%Y-%m-%d %H:%M:%S') [ERROR] 命令在第 ${line} 行执行失败，退出码 ${code}: ${command}" >&2
    fi
}
trap 'error_handler $? $LINENO "$BASH_COMMAND"' ERR


# 通用辅助函数
log_message() {
    local level=$1; shift
    local levels="DEBUG INFO WARN ERROR"
    local actual_idx="${levels%%$level*}"; local min_idx="${levels%%$LOG_LEVEL*}"
    if [ ${#actual_idx} -ge ${#min_idx} ]; then
        case "$level" in
            "ERROR") printf "\n\033[31m%s\033[0m\n" "$(date '+%Y-%m-%d %H:%M:%S') [$level] $*" >&2 ;;
            "WARN")  printf "\n\033[33m%s\033[0m\n" "$(date '+%Y-%m-%d %H:%M:%S') [$level] $*" >&2 ;;
            *)       echo "$(date '+%Y-%m-%d %H:%M:%S') [$level] $*" >&2 ;;
        esac
    fi
}
log_debug() { log_message "DEBUG" "$@"; }
log_info()  { log_message "INFO" "$@"; }
log_warn()  { log_message "WARN" "$@"; }
log_error() { log_message "ERROR" "$@"; }
die()       { log_error "$*"; exit 1; }

check_command() {
    for cmd in "$@"; do command -v "$cmd" >/dev/null 2>&1 || die "必需的命令 $cmd 未找到"; done
}

find_support_file() {
    local name=$1
    if [ -f "$CURRENT_DIR/$name" ]; then
        echo "$CURRENT_DIR/$name"
    elif [ -f "$CURRENT_DIR/scripts/$name" ]; then
        echo "$CURRENT_DIR/scripts/$name"
    elif [ -f "$SCRIPT_DIR/$name" ]; then
        echo "$SCRIPT_DIR/$name"
    fi
}

should_backup_service() {
    if [ "$IS_DEV_MODE" = true ] && [ "${DEV_MODE_BACKUP:-false}" != "true" ]; then
        return 1
    fi

    return 0
}

safe_remove_tree() {
    local target=$1

    if [ -z "$target" ] || [ "$target" = "/" ] || [ "$target" = "$HOME" ] || [ "$target" = "$DEPLOY_BASE_DIR" ]; then
        log_error "拒绝清理高风险目录: ${target:-<empty>}"
        return 1
    fi

    [ -d "$target" ] || return 0

    local empty_dir="${DEPLOY_BASE_DIR}/.deploy-empty"
    mkdir -p "$empty_dir"
    rsync -a --delete "$empty_dir"/ "$target"/
    rmdir "$target" 2>/dev/null || true
}

restore_backup_snapshot() {
    local service_name=$1
    local deploy_dir=$2
    local backup_dir=$3

    [ -n "$backup_dir" ] || return 1
    if [ ! -d "$backup_dir" ]; then
        log_warn "备份目录不存在，无法恢复: $backup_dir"
        return 1
    fi

    mkdir -p "$deploy_dir"
    if ! rsync -a --delete "$backup_dir"/ "$deploy_dir"/; then
        log_warn "恢复 $service_name 的备份目录失败: $backup_dir -> $deploy_dir"
        return 1
    fi

    log_warn "已从备份恢复 $service_name: $deploy_dir"
}

format_file_size() {
    local b=$1
    if [ $b -ge 1073741824 ]; then echo "$(($b/1073741824)) GB"
    elif [ $b -ge 1048576 ]; then echo "$(($b/1048576)) MB"
    elif [ $b -ge 1024 ]; then echo "$(($b/1024)) KB"
    else echo "$b bytes"; fi
}

format_transfer_speed() {
    local bps=$1
    if [ $bps -ge 1048576 ]; then echo "$(($bps/1048576)) MB/s"
    elif [ $bps -ge 1024 ]; then echo "$(($bps/1024)) KB/s"
    else echo "$bps B/s"; fi
}

join_array_comma() { local IFS=','; echo "$*"; }

get_prune_expr() {
    [ -n "$CACHED_PRUNE_EXPR" ] && echo "$CACHED_PRUNE_EXPR" && return
    local excludes=(".git" "node_modules" "target" "dist" ".*" "unpackage")
    if [ -f "$CURRENT_DIR/.gitignore" ]; then
        while IFS= read -r line;
        do
            line=$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
            [[ -z "$line" || "$line" =~ ^# || "$line" =~ ^! ]] && continue
            local dir_name=$(echo "$line" | sed 's|^/||' | sed 's|^\*\*/||' | sed 's|/\*\*$||' | sed 's|/$||')
            [ -n "$dir_name" ] && excludes+=("$dir_name")
        done < "$CURRENT_DIR/.gitignore"
    fi
    local unique_excludes=($(printf "%s\n" "${excludes[@]}" | sort -u))
    local expr=""
    for dir in "${unique_excludes[@]}"; do
        local safe_dir=$(echo "$dir" | sed "s/'/'\\''/g")
        local part; [[ "$safe_dir" == *"/"* ]] && part="-path '*/${safe_dir}*'" || part="-name '$safe_dir'"
        [ -z "$expr" ] && expr="$part" || expr="$expr -o $part"
    done
    CACHED_PRUNE_EXPR=$(echo "$expr" | sed 's/\*\*+/*/g'); echo "$CACHED_PRUNE_EXPR"
}

init_module_path_cache() {
    log_debug "正在扫描项目模块结构..."
    local expr=$(get_prune_expr); local tmp_file=$(mktemp)
    eval "find \"$CURRENT_DIR\" \( $expr \) -prune -o -name \"pom.xml\" -type f -print" > "$tmp_file"
    while read -r pom;
    do
        local dir=$(dirname "$pom"); local name=$(basename "$dir"); local safe_name=${name//[^a-zA-Z0-9_]/_}
        eval "MODULE_PATH_${safe_name}='$dir'"; MODULE_LIST+=("$name")
        local art_id=$(sed -n '/<project/,/<dependencies/p' "$pom" | sed -n 's/.*<artifactId>\([^<]*\)<\/artifactId>.*/\1/p' | head -n 1)
        if [ -n "$art_id" ]; then
            local safe_art_id=${art_id//[^a-zA-Z0-9_]/_}; eval "MODULE_PATH_${safe_art_id}='$dir'"
            [ "$name" != "$art_id" ] && MODULE_LIST+=("$art_id")
        fi
    done < "$tmp_file"; rm -f "$tmp_file"
}

find_service_module_path() {
    local name=$1; local safe_name=${name//[^a-zA-Z0-9_]/_}; local path_var="MODULE_PATH_${safe_name}"; local path=""
    eval "path=\"\${$path_var:-}\""
    if [ -n "$path" ]; then echo "$path"; elif [ -d "$CURRENT_DIR/$name/src" ]; then echo "$CURRENT_DIR/$name"; fi
}

# 核心逻辑：POM 发现与依赖检测
discover_modules_from_pom() {
    [ ${#MODULE_LIST[@]} -eq 0 ] && init_module_path_cache
    local all_modules=()

    # 1. 从根 pom.xml 中读取顶层模块
    if [ -f "$CURRENT_DIR/pom.xml" ]; then
        while IFS= read -r module; do
            module=$(echo "$module" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
            [ -n "$module" ] && all_modules+=("$module")
        done < <(sed -n 's/.*<module>\(.*\)<\/module>.*/\1/p' "$CURRENT_DIR/pom.xml")
    fi

    # 2. 遍历每个模块，读取其子模块
    for name in "${MODULE_LIST[@]}"; do
        local path=$(find_service_module_path "$name")
        if [ -n "$path" ] && [ -f "$path/pom.xml" ]; then
            while IFS= read -r submodule; do
                submodule=$(echo "$submodule" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
                [ -n "$submodule" ] && all_modules+=("$submodule")
            done < <(sed -n 's/.*<module>\(.*\)<\/module>.*/\1/p' "$path/pom.xml")
        fi
    done

    # 3. 去重并返回
    printf '%s\n' "${all_modules[@]}" | sort -u
}

find_dependent_services() {
    local mod_name=$1; local deps=()
    [ ${#MODULE_LIST[@]} -eq 0 ] && init_module_path_cache
    for name in "${MODULE_LIST[@]}"; do
        local dir=$(find_service_module_path "$name")
        if [ -n "$dir" ] && [ -f "$dir/pom.xml" ]; then
            if grep -q "<artifactId>${mod_name}</artifactId>" "$dir/pom.xml"; then
                local dir_name=$(basename "$dir")
                for svc in "${SERVICE_DEPENDENCY_ORDER[@]}"; do
                    if [[ "$svc" == *"$dir_name"* ]] || [[ "$dir_name" == *"$svc"* ]]; then deps+=("$svc"); fi
                done
            fi
        fi
    done
    printf '%s\n' "${deps[@]}" | sort -u
}

calculate_file_hash() {
    local file_path=$1; [ -f "$file_path" ] && $HASH_CMD "$file_path" 2>/dev/null | awk '{print $1}' || echo ""
}

calculate_module_hash() {
    local root=$1; [ ! -d "${root}/src" ] && return
    local src_h=$(find "${root}/src" -type f -print0 2>/dev/null | xargs -0 $HASH_CMD | sort | $HASH_CMD | cut -d' ' -f1)
    local pom_h=""; [ -f "${root}/pom.xml" ] && pom_h=$($HASH_CMD "${root}/pom.xml" | cut -d' ' -f1)
    if [ -z "$pom_h" ]; then echo "$src_h"; else echo "${src_h}${pom_h}" | $HASH_CMD | cut -d' ' -f1; fi
}

update_project_source_manifest() {
    log_info "正在更新源码哈希快照..."
    local start=$(date +%s); local temp="${SOURCE_HASHES_MANIFEST}.tmp"
    local mods=$(discover_modules_from_pom)
    while IFS= read -r m;
    do
        local p=$(find_service_module_path "$m")
        [ -n "$p" ] && local h=$(calculate_module_hash "$p") && [ -n "$h" ] && echo "$p $h" >> "$temp"
    done <<< "$mods"; mv "$temp" "$SOURCE_HASHES_MANIFEST"
    log_info "快照更新完成，耗时 $(($(date +%s) - start)) 秒"
}

detect_changes() {
    # 源码哈希清单不存在时，直接全量构建
    if [ ! -f "$SOURCE_HASHES_MANIFEST" ]; then
        log_info "源码哈希清单不存在，执行全量构建"
        HAS_DETECTED_CHANGES=true
        SERVICE_NAMES=""  # 清空服务名，触发全量构建
        return
    fi

    local detected=()
    log_info "使用源码哈希检测变更..."
    local mods=$(discover_modules_from_pom)
    while IFS= read -r m; do
        local p=$(find_service_module_path "$m"); [ -z "$p" ] && continue
        local cur_h=$(calculate_module_hash "$p")
        local rec_h=$(grep "^${p} " "$SOURCE_HASHES_MANIFEST" 2>/dev/null | awk '{print $2}' || echo "")
        if [ -n "$cur_h" ] && [ "$cur_h" != "$rec_h" ]; then
            local mn=$(basename "$p"); local found=false
            log_info "模块变更: $mn"
            for s in "${SERVICE_DEPENDENCY_ORDER[@]}"; do
                if [[ "$s" == *"$mn"* ]]; then detected+=("$s"); found=true; fi
            done
            if [ "$found" = false ]; then
                local sd=$(find_dependent_services "$mn")
                [ -n "$sd" ] && while read -r d; do detected+=("$d"); done <<< "$sd"
            fi
        fi
    done <<< "$mods"

    if [ ${#detected[@]} -gt 0 ]; then
        HAS_DETECTED_CHANGES=true
        local unique=($(printf "%s\n" "${detected[@]}" | sort -u))
        if [ -z "$SERVICE_NAMES" ]; then
            SERVICE_NAMES=$(join_array_comma "${unique[@]}")
            log_info "检测到变更服务: $SERVICE_NAMES"
        fi
    fi
}
find_package_path() {
    local name=$1; local dir=$2
    local p=$(find "$dir" -maxdepth 1 \( -name "${name}-*.tar.gz" -o -name "${name}.tar.gz" \) -print -quit 2>/dev/null)
    [ -z "$p" ] && p=$(find "$dir" -maxdepth 1 -name "*${name}*.tar.gz" -print -quit 2>/dev/null)
    echo "$p"
}

# 部署失败回退函数
rollback_to_backup() {
    local service_name=$1 deploy_base_dir=$2
    local backup_base_dir="${DEPLOY_BASE_DIR%/*}/backup"
    [ -n "$CUSTOM_BACKUP_DIR" ] && backup_base_dir="$CUSTOM_BACKUP_DIR"

    # 查找最新备份
    local latest_backup_dir=$(find "${backup_base_dir}/${service_name}" -maxdepth 1 -mindepth 1 -type d 2>/dev/null | sort -r | head -n1)

    if [ -z "$latest_backup_dir" ] || [ ! -d "$latest_backup_dir" ]; then
        log_error "未找到服务 $service_name 的备份版本，无法执行回退操作"
        return 1
    fi

    local deploy_dir="${deploy_base_dir}/${service_name}"

    log_info "开始回退服务 $service_name 到备份版本: $latest_backup_dir"

    # 停止可能还在运行的当前服务
    if [ -f "${deploy_dir}/bin/stop.sh" ]; then
        (cd "$deploy_dir" && ./bin/stop.sh --app-name "$service_name" --base-dir "$deploy_dir" &>/dev/null) || log_info "尝试停止当前服务"
    fi

    # 恢复备份到部署目录，使用 --delete 镜像同步，避免直接删除部署目录。
    mkdir -p "$deploy_dir"
    if ! rsync -a --delete "$latest_backup_dir"/ "$deploy_dir"/; then
        log_error "恢复备份失败"
        return 1
    fi

    log_info "服务 $service_name 已回退到备份版本，尝试重新启动..."

    # 尝试启动回退后的服务
    local t=$((STARTUP_TIMEOUT + 30))
    local start_cmd="./bin/start.sh -t $t"; [ "$NO_STARTUP_LOG" = "true" ] && start_cmd+=" -n"
    if (cd "$deploy_dir" && eval "$start_cmd"); then
        log_info "回退后的服务 $service_name 启动成功"
        return 0
    else
        log_error "回退后的服务 $service_name 启动失败"
        # 即使回退后的服务启动失败，也不返回错误
        # 这样可以让部署流程继续
        return 0
    fi
}

deploy_service() {
    local name=$1; local dir="${DEPLOY_BASE_DIR}/${name}"; local lock="/tmp/${name}.deploy.lock"
    local prod_backup_dir=""
    local staging_dir="${DEPLOY_BASE_DIR}/.deploy-staging/${name}.$$"
    if [ -f "$lock" ]; then
        local pid=$(cat "$lock" 2>/dev/null || echo "")
        if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then log_error "$name 正在部署中，跳过"; return 1; fi
    fi
    echo "$$ " > "$lock"; trap "rm -f '$lock'" RETURN
    local pkg=$(find_package_path "$name" "$GZ_FILE_DIR")
    [ -z "$pkg" ] && { log_error "未找到服务包: $name"; return 1; }
    log_info ">>> 正在部署: $name"
    local start=$(date +%s); local bk_root="${DEPLOY_BASE_DIR%/*}/backup/${name}"
    [ -n "$CUSTOM_BACKUP_DIR" ] && bk_root="$CUSTOM_BACKUP_DIR/${name}"

    if [ -f "${dir}/bin/stop.sh" ]; then
        log_debug "正在尝试优雅停止 $name..."
        if ! "${dir}/bin/stop.sh" --app-name "$name" --base-dir "$dir" >/dev/null 2>&1; then
            log_warn "停止超时，正在强制停止..."
            "${dir}/bin/stop.sh" --app-name "$name" --base-dir "$dir" -f >/dev/null 2>&1 || true
        fi
    fi
    if [ -d "$dir" ]; then
        local bk_d="${bk_root}/$(date +%Y%m%d%H%M%S)"
        if should_backup_service; then
            if [ "$DRY_RUN_CLEANUP" = "true" ]; then log_info "[试运行] 备份目录: $dir -> $bk_d"
            else mkdir -p "$(dirname "$bk_d")" && mv "$dir" "$bk_d"
                prod_backup_dir="$bk_d"
                while IFS= read -r old_backup_dir; do
                    safe_remove_tree "$old_backup_dir" || true
                done < <(ls -1dt "${bk_root}"/*/ 2>/dev/null | tail -n +$((KEEP_BACKUPS + 1)) || true)
            fi
        else
            log_info "开发模式不保留历史备份，使用 staging + rsync 更新部署目录: $dir"
        fi
    fi

    if ! mkdir -p "$staging_dir"; then
        log_error "创建部署暂存目录失败: $staging_dir"
        restore_backup_snapshot "$name" "$dir" "$prod_backup_dir" || true
        return 1
    fi
    if ! tar -xzf "$pkg" -C "$staging_dir" --strip-components=1; then
        log_error "解压服务包失败: $pkg"
        restore_backup_snapshot "$name" "$dir" "$prod_backup_dir" || true
        safe_remove_tree "$staging_dir" || true
        return 1
    fi
    if [ ! -d "${staging_dir}/bin" ] && ! mkdir -p "${staging_dir}/bin"; then
        log_error "创建脚本目录失败: ${staging_dir}/bin"
        restore_backup_snapshot "$name" "$dir" "$prod_backup_dir" || true
        safe_remove_tree "$staging_dir" || true
        return 1
    fi
    for file_item in "${SCRIPTS_AND_CONFIGS[@]}"; do
        local support_file
        support_file=$(find_support_file "$file_item")
        if [ -n "$support_file" ] && ! cp "$support_file" "${staging_dir}/bin/"; then
            log_error "复制部署脚本失败: $support_file"
            restore_backup_snapshot "$name" "$dir" "$prod_backup_dir" || true
            safe_remove_tree "$staging_dir" || true
            return 1
        fi
    done
    if ! chmod +x "${staging_dir}/bin/"*.sh; then
        log_error "设置部署脚本执行权限失败: ${staging_dir}/bin"
        restore_backup_snapshot "$name" "$dir" "$prod_backup_dir" || true
        safe_remove_tree "$staging_dir" || true
        return 1
    fi
    if [ -f "$DEPENDENCY_FILE" ] && ! cp "$DEPENDENCY_FILE" "${staging_dir}/"; then
        log_error "复制依赖顺序文件失败: $DEPENDENCY_FILE"
        restore_backup_snapshot "$name" "$dir" "$prod_backup_dir" || true
        safe_remove_tree "$staging_dir" || true
        return 1
    fi
    if ! mkdir -p "$dir"; then
        log_error "创建部署目录失败: $dir"
        restore_backup_snapshot "$name" "$dir" "$prod_backup_dir" || true
        safe_remove_tree "$staging_dir" || true
        return 1
    fi
    if ! rsync -a --delete "$staging_dir"/ "$dir"/; then
        log_error "同步部署目录失败: $staging_dir -> $dir"
        restore_backup_snapshot "$name" "$dir" "$prod_backup_dir" || true
        safe_remove_tree "$staging_dir" || true
        return 1
    fi
    export CUSTOM_JVM_OPTS; local t=$((STARTUP_TIMEOUT + 30))
    local start_cmd="./bin/start.sh -t $t"; [ "$NO_STARTUP_LOG" = "true" ] && start_cmd+=" -n"
    if ! (cd "$dir" && eval "$start_cmd"); then
        log_error "$name 启动失败"
        if [ "$IS_DEV_MODE" = true ]; then
            safe_remove_tree "$staging_dir" || true
            log_warn "开发模式部署启动失败，不执行回退"
            return 1
        elif [ "$PERFORM_ROLLBACK" = "true" ]; then
            log_warn "正在尝试回退 $name..."
            if ! rollback_to_backup "$name" "$DEPLOY_BASE_DIR"; then
                log_error "$name 回滚失败"
                # 即使回滚失败，也不返回错误，而是继续执行
                log_info "$name 部署完成但启动失败且回滚失败，请手动处理"
                return 0
            fi
            log_info "$name 回滚成功"
            return 0
        else
            log_warn "$name 部署完成但启动失败，请检查服务状态"
            safe_remove_tree "$staging_dir" || true
            return 1
        fi
    fi
    safe_remove_tree "$staging_dir" || true
    log_info "$name 部署成功，耗时 $(($(date +%s) - start)) 秒"
    return 0
}

execute_deployment_plan() {
    local list=$1; local start=$(date +%s); local succ=0; local fail=0; local skip=0
    [ -f "$REMOTE_MANIFEST_PATH" ] && while read -r h n; do local safe_n=${n//[^a-zA-Z0-9_]/_}; eval "DEPLOYED_HASH_${safe_n}='$h'"; done < "$REMOTE_MANIFEST_PATH"
    local to_dep=()
    local failed_services=()
    local skipped_services=()
    IFS=',' read -r -a input <<< "$list"; local new_m=$(mktemp)
    for s in "${input[@]}"; do
        local p=$(find_package_path "$s" "$GZ_FILE_DIR")
        if [ -n "$p" ]; then
            local pn=$(basename "$p"); local ph=$(calculate_file_hash "$p"); local safe_pn=${pn//[^a-zA-Z0-9_]/_}; local old_h=""
            eval "old_h=\"\${DEPLOYED_HASH_${safe_pn}:-}\""
            if [ "$FORCE_DEPLOYMENT" = true ] || [ -z "$old_h" ] || [ "$ph" != "$old_h" ]; then to_dep+=("$s")
            else ((skip+=1)); skipped_services+=("$s"); log_debug "跳过未变更: $s"; fi
            echo "$ph $pn" >> "$new_m"
        fi
    done
    if [ ${#to_dep[@]} -eq 0 ]; then log_info "所有服务 ($skip 个) 无需变更。"
    else log_info "开始部署 ${#to_dep[@]} 个服务: ${to_dep[*]}"
        for s in "${to_dep[@]}"; do
            if deploy_service "$s"; then
                ((succ+=1))
            else
                ((fail+=1))
                failed_services+=("$s")
                [ "$FAIL_FAST" = true ] && break
            fi
        done
    fi
    [ -s "$new_m" ] && mv "$new_m" "$REMOTE_MANIFEST_PATH" || rm -f "$new_m"
    log_info "任务结束: 成功 $succ, 失败 $fail, 跳过 $skip (总耗时 $(($(date +%s) - start)) 秒)"
    [ ${#failed_services[@]} -gt 0 ] && log_warn "失败服务: ${failed_services[*]}"
    [ ${#skipped_services[@]} -gt 0 ] && log_info "跳过服务: ${skipped_services[*]}"
    return 0
}

# 编排逻辑
prepare_packages() {
    init_module_path_cache
    if [ ! -f "$DEPENDENCY_FILE" ] && [ -f "$BUILD_FILE" ]; then log_info "生成依赖顺序..."; "$BUILD_FILE" --list-modules >/dev/null; fi
    [ -f "$DEPENDENCY_FILE" ] && source "$DEPENDENCY_FILE"
    if [[ -z ${SERVICE_DEPENDENCY_ORDER+x} ]] || [ ${#SERVICE_DEPENDENCY_ORDER[@]} -eq 0 ]; then
        local disc=$(discover_modules_from_pom); [ -n "$disc" ] && SERVICE_DEPENDENCY_ORDER=($disc)
    fi
    if [ "$IS_DEV_MODE" = true ]; then
        [ "$USE_HASH_DETECTION" = true ] && detect_changes
        if [ "$FORCE_BUILD" = true ] || [ "$HAS_DETECTED_CHANGES" = true ]; then
            if [ -n "$SERVICE_NAMES" ]; then
                IFS=',' read -r -a cur_t <<< "$SERVICE_NAMES"
                [ ${#cur_t[@]} -ge ${#SERVICE_DEPENDENCY_ORDER[@]} ] && log_info "切换全量构建..." && SERVICE_NAMES=""
            fi
            local target="${SERVICE_NAMES:-ALL}"
            local build_cmd="$BUILD_FILE"
            [ "$target" != "ALL" ] && build_cmd="$BUILD_FILE --module=$target"
            log_info "启动构建: $build_cmd"
            local b_start=$(date +%s)
            [ "$target" = "ALL" ] && "$BUILD_FILE" || "$BUILD_FILE" --module="$target"
            log_info "构建完成，耗时 $(($(date +%s) - b_start)) 秒"
            find "$CURRENT_DIR" -name "*.tar.gz" -path "*/target*" -exec rsync -a {} "$GZ_FILE_DIR/" \;
            update_project_source_manifest
        else
            local found=false
            if [ -n "$SERVICE_NAMES" ]; then
                IFS=',' read -r -a ts <<< "$SERVICE_NAMES"
                for t in "${ts[@]}"; do find "$CURRENT_DIR" -maxdepth 5 -name "*${t}*.tar.gz" -exec rsync -a {} "$GZ_FILE_DIR/" \; && found=true; done
            else find "$CURRENT_DIR" -maxdepth 5 -name "*.tar.gz" -exec rsync -a {} "$GZ_FILE_DIR/" \; && found=true; fi
            [ "$found" = false ] && { log_warn "未找到包，强制构建..."; FORCE_BUILD=true; prepare_packages; return; }
        fi
    fi
    local final=(); local ts; [ -n "$SERVICE_NAMES" ] && IFS=',' read -r -a ts <<< "$SERVICE_NAMES" || ts=("${SERVICE_DEPENDENCY_ORDER[@]}")
    for o in "${SERVICE_DEPENDENCY_ORDER[@]}"; do
        for t in "${ts[@]}"; do if [[ "$o" == *"$t"* ]] && [ -n "$(find_package_path "$o" "$GZ_FILE_DIR")" ]; then final+=("$o"); break; fi done
    done
    export ORDERED_SERVICE_LIST=$(join_array_comma "${final[@]}")
    if [ -z "$ORDERED_SERVICE_LIST" ]; then die "未识别到有效服务包"; fi
}

distribute_to_remote() {
    local remote="$1"; local host="${remote#*@}"; local user="${remote%@*}"; local r_tmp="/home/${user}/tmp"
    log_info ">>> 正在部署远程主机: $host"

    # 检查SSH连接是否可用
    if ! ssh $SSH_OPTS "$remote" "mkdir -p $r_tmp"; then
        log_error "无法连接到远程主机: $remote"
        return 1
    fi

    local files=("$0" "$DEPENDENCY_FILE")
    for item in "${SCRIPTS_AND_CONFIGS[@]}"; do
        local support_file
        support_file=$(find_support_file "$item")
        if [ -n "$support_file" ]; then files+=("$support_file"); fi
    done
    local svcs; IFS=',' read -r -a svcs <<< "$ORDERED_SERVICE_LIST"
    local total_b=0
    for v in "${svcs[@]}"; do
        local p=$(find_package_path "$v" "$GZ_FILE_DIR")
        if [ -n "$p" ]; then
            files+=("$p")
            local sz=$(stat -f%z "$p" 2>/dev/null || stat -c%s "$p" 2>/dev/null || echo 0)
            total_b=$(($total_b + $sz))
        fi
    done
    log_info "正在同步文件 (共 ${#files[@]} 个, 总大小 $(format_file_size $total_b))..."
    local s_start=$(date +%s); local r_args=(-rt --itemize-changes --progress -e "$RSYNC_SSH"); local r_log=$(mktemp)
    if ! rsync "${r_args[@]}" "${files[@]}" "${remote}:${r_tmp}/" 2>&1 | tee "$r_log"; then
        log_error "文件同步失败到远程主机: $remote"
        rm -f "$r_log"
        return 1
    fi
    local s_dur=$(($(date +%s) - s_start))
    local speed=0; [ $s_dur -gt 0 ] && speed=$(($total_b / $s_dur))
    log_info "同步完成: 耗时 ${s_dur} 秒, 平均速度 $(format_transfer_speed $speed)"
    local sync_files=($(grep '^<f' "$r_log" 2>/dev/null | awk '{print $2}' || true))
    if [ ${#sync_files[@]} -gt 0 ]; then
        [ ${#sync_files[@]} -gt 5 ] && log_info "实际同步文件: ${sync_files[*]:0:5} ...等 ${#sync_files[@]} 个" || log_info "实际同步文件: ${sync_files[*]}"
    fi
    rm -f "$r_log"
    # 远程主机总是使用 /home/$user/webapps 作为部署目录，不使用本机的DEPLOY_BASE_DIR
    local cmd="export GZ_FILE_DIR='$r_tmp' DEPLOY_BASE_DIR='/home/$user/webapps' "
    cmd+="ORDERED_SERVICE_LIST='$ORDERED_SERVICE_LIST' FORCE_DEPLOYMENT='$FORCE_DEPLOYMENT' "
    cmd+="CUSTOM_JVM_OPTS='$CUSTOM_JVM_OPTS' LOG_LEVEL='$LOG_LEVEL' STARTUP_TIMEOUT='$STARTUP_TIMEOUT' "
    cmd+="KEEP_BACKUPS='$KEEP_BACKUPS' PERFORM_ROLLBACK='$PERFORM_ROLLBACK' FAIL_FAST='$FAIL_FAST' "
    cmd+="NO_STARTUP_LOG='$NO_STARTUP_LOG' DRY_RUN_CLEANUP='$DRY_RUN_CLEANUP' "
    cmd+="&& chmod +x $r_tmp/deploy.sh && $r_tmp/deploy.sh --remote-execution"

    # 执行远程部署命令，增加调试信息
    log_debug "执行远程命令: $cmd"
    if ! ssh $SSH_OPTS "$remote" "$cmd"; then
        log_error "远程部署命令执行失败: $remote"
        return 1
    fi
}

parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            -s|--services) SERVICE_NAMES="$2"; shift 2 ;;
            -r|--remotes) REMOTES_STR="$2"; shift 2 ;;
            --src-dir) GZ_FILE_DIR="$2"; shift 2 ;;
            -d|--deploy-dir) DEPLOY_BASE_DIR="$2"; shift 2 ;;
            -j|--jvm) CUSTOM_JVM_OPTS="$2"; shift 2 ;;
            --fail-fast) FAIL_FAST=true; shift ;;
            --rollback) PERFORM_ROLLBACK=true; shift ;;
            --backup-dir) CUSTOM_BACKUP_DIR="$2"; shift 2 ;;
            --keep-backups) KEEP_BACKUPS="$2"; shift 2 ;;
            --dry-run-cleanup) DRY_RUN_CLEANUP=true; shift ;;
            --wait) STARTUP_TIMEOUT="$2"; shift 2 ;;
            -n|--no-startup-log) NO_STARTUP_LOG=true; shift ;;
            -f|--force) FORCE_DEPLOYMENT=true; USE_HASH_DETECTION=false; shift ;;
            --force-build) FORCE_BUILD=true; shift ;;
            --log-level) LOG_LEVEL=$(echo "$2" | tr '[:lower:]' '[:upper:]'); shift 2 ;;
            --remote-execution) IS_REMOTE_EXECUTION=true; shift ;;
            -h|--help) show_help; exit 0 ;;
            *) echo "未知参数: $1"; exit 1 ;;
        esac
    done
}

main() {
    parse_args "$@"
    check_command tar ssh rsync grep awk sed "$HASH_CMD"
    mkdir -p "$GZ_FILE_DIR"
    [ -f "$CURRENT_DIR/pom.xml" ] && IS_DEV_MODE=true
    if [ "$IS_REMOTE_EXECUTION" = true ]; then
        # 在远程执行模式下，即使服务启动失败也不退出
        execute_deployment_plan "$ORDERED_SERVICE_LIST"
    else prepare_packages
        if [ -n "$REMOTES_STR" ]; then
            IFS=',' read -r -a rs <<< "$REMOTES_STR"
            for r in "${rs[@]}"; do
                if ! distribute_to_remote "$r"; then
                    log_error "远程部署到 $r 失败"
                    if [ "$FAIL_FAST" = true ]; then
                        log_error "由于启用快速失败模式，停止部署"
                        exit 1
                    fi
                fi
            done
        else
            # 在本地执行模式下，即使服务启动失败也不退出
            execute_deployment_plan "$ORDERED_SERVICE_LIST"
        fi
        log_info "所有任务结束，总计耗时 $(($(date +%s) - SCRIPT_START_TIME)) 秒"
    fi
}
main "$@"
