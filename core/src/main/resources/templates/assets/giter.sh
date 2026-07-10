#!/bin/bash

# Git 自动化运维脚本
# 功能：自动发现并处理工作根目录、执行目录、脚本所属目录相关的 Git 仓库 Pull/Rebase/Commit/Push

set -euo pipefail

# --- 1. 配置区域 ---

resolve_script_path() {
    local source="$1"
    local dir target

    while [ -L "$source" ]; do
        dir="$(cd -P "$(dirname "$source")" && pwd)"
        target="$(readlink "$source")"
        if [[ "$target" == /* ]]; then
            source="$target"
        else
            source="$dir/$target"
        fi
    done

    dir="$(cd -P "$(dirname "$source")" && pwd)"
    printf '%s/%s\n' "$dir" "$(basename "$source")"
}

SCRIPT_PATH="$(resolve_script_path "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(cd "$(dirname "$SCRIPT_PATH")" && pwd)"
ORIGINAL_PWD="$(pwd)"
TARGET_DIR="$ORIGINAL_PWD"
DEFAULT_COMMIT_MSG="自动提交 $(date '+%Y-%m-%d %H:%M:%S')"
GITER_CONFIG_HOME="${GITER_CONFIG_HOME:-$HOME/.config/giter}"
GITER_REPO_CONFIG_DIR="$GITER_CONFIG_HOME/repos"
GITER_AUTO_UPDATE_GITIGNORE="${GITER_AUTO_UPDATE_GITIGNORE:-1}"
GITER_GIT_NETWORK_TIMEOUT="${GITER_GIT_NETWORK_TIMEOUT:-30}"
GITER_BRANCH_CACHE_TIMEOUT="${GITER_BRANCH_CACHE_TIMEOUT:-8}"
GITER_SSH_CONNECT_TIMEOUT="${GITER_SSH_CONNECT_TIMEOUT:-10}"
GITER_REFRESH_BRANCH_CACHE="${GITER_REFRESH_BRANCH_CACHE:-auto}"
GITER_INPUT_TIMEOUT="${GITER_INPUT_TIMEOUT:-5}"

# 未显式指定 -d/--dir 时默认处理当前执行目录所属仓库。
AUTO_DISCOVER=0
DISCOVER_DEPTH=2
DISCOVER_ALL_NESTED=0
DRY_RUN=0
SHOW_CONFIG=0

# 仓库差异化配置从 ~/.config/giter/repos/<repo_hash>.conf 加载。
GITER_PUSH_DIRS=()
GITER_EXCLUDE_DIRS=()
GITER_NESTED_POLICIES=()
GITER_EXTRA_PUSH_REMOTES=()
GITER_NESTED_POLICY_PATHS=()
GITER_NESTED_POLICY_STRATEGIES=()
GITER_NESTED_POLICY_REMOTES=()
GITER_NESTED_POLICY_BRANCHES=()
GITER_GITIGNORE_HASH=""
GITER_LOADED_REPO_CONFIGS=()
GITER_NESTED_REPO_DIRS=()
GITER_LOGGED_SKIP_PATHS=()
GITER_HIDDEN_GIT_DIRS=()

# 默认跳过超过该大小的文件，避免自动提交模型、压缩包、日志等大文件。
MAX_STAGE_FILE_MB="${GITER_MAX_STAGE_FILE_MB:-50}"
[[ "$MAX_STAGE_FILE_MB" =~ ^[0-9]+$ ]] || MAX_STAGE_FILE_MB=50
[[ "$GITER_GIT_NETWORK_TIMEOUT" =~ ^[0-9]+$ ]] || GITER_GIT_NETWORK_TIMEOUT=30
[[ "$GITER_BRANCH_CACHE_TIMEOUT" =~ ^[0-9]+$ ]] || GITER_BRANCH_CACHE_TIMEOUT=8
[[ "$GITER_SSH_CONNECT_TIMEOUT" =~ ^[0-9]+$ ]] || GITER_SSH_CONNECT_TIMEOUT=10
[[ "$GITER_INPUT_TIMEOUT" =~ ^[0-9]+$ ]] || GITER_INPUT_TIMEOUT=5

# 默认跳过的敏感路径/文件名模式。
SENSITIVE_PATH_PATTERNS=(
    ".env"
    ".env.*"
    "*.env"
    "*.key"
    "*.pem"
    "*.p12"
    "*.pfx"
    "*id_rsa*"
    "*id_ed25519*"
    "*credentials*"
    "*secret*"
    "*.gguf"
)

GITER_HIDDEN_GIT_DIR_NAME=".giter-hidden-git"

# --- 2. 颜色定义 ---

if [[ -t 2 ]]; then
    readonly RED='\033[0;31m'
    readonly GREEN='\033[0;32m'
    readonly YELLOW='\033[1;33m'
    readonly BLUE='\033[0;34m'
    readonly NC='\033[0m'
else
    readonly RED=''
    readonly GREEN=''
    readonly YELLOW=''
    readonly BLUE=''
    readonly NC=''
fi

# --- 3. 全局状态变量 ---

VERBOSE=0
LAST_COMMITS=1
SHOW_LAST_FILES=1
FORCE_MODE=1
FORCE_PULL=0
NO_PUSH=0
COMMIT_MSG=""
COMMIT_PATHS=()
GITER_WITH_NESTED=()
GITER_SKIP_NESTED=()
GITER_NESTED_SELECTION_EXPLICIT=0
GITER_CLI_EXTRA_PUSH_REMOTES=()
TARGET_BRANCH=""
TARGET_DIR_EXPLICIT=0
AUTO_REPOS=()
AUTO_REPO_LABELS=()
RUN_SUMMARY=()
GITER_SUMMARY_FILE=""
SELECTED_BRANCH=""
SELECTED_REMOTE_REF=""
GITER_SYNC_FAILURE_STATUS=""
GITER_SYNC_FAILURE_DETAIL=""
GITER_LAST_FETCH_ERROR=""

# 远程 fetch 状态跟踪（避免重复 fetch 同一个 remote）
GITER_FETCHED_REMOTES=()
GITER_FETCH_FAILED_REMOTES=()

# --- 4. 辅助函数 ---

log_info() { echo -e "${GREEN}[INFO] $*${NC}" >&2; }
log_warn() { echo -e "${YELLOW}[WARN] $*${NC}" >&2; }
log_error() { echo -e "${RED}[ERROR] $*${NC}" >&2; }
log_debug() {
    [ "$VERBOSE" -eq 1 ] || return 0
    echo -e "${BLUE}[DEBUG] $*${NC}" >&2
}

log_verbose() {
    [ "$VERBOSE" -eq 1 ] || return 0
    log_info "$@"
}

read_input() {
    local variable_name="$1"
    local prompt="${2:-}"
    local value=""
    local status=0

    if [[ "$GITER_INPUT_TIMEOUT" -gt 0 ]]; then
        read -r -t "$GITER_INPUT_TIMEOUT" -p "$prompt" value || status=$?
    else
        read -r -p "$prompt" value || status=$?
    fi
    if [[ -t 0 && "$status" -gt 128 ]]; then
        printf '\n' >&2
    fi
    printf -v "$variable_name" '%s' "$value"
}

install_giter_command() {
    local install_dir="${GITER_INSTALL_DIR:-$HOME/.local/bin}"
    local link_name link_path
    local envs_cmd=""
    local rc_file="${GITER_INSTALL_RC:-}"
    local link_names=("giter" "giter.sh")

    if [[ -n "${GITER_COMMAND_NAME:-}" ]]; then
        link_names+=("$GITER_COMMAND_NAME")
    fi

    mkdir -p "$install_dir"
    chmod +x "$SCRIPT_PATH" 2>/dev/null || true
    for link_name in "${link_names[@]}"; do
        link_path="$install_dir/$link_name"
        ln -sfn "$SCRIPT_PATH" "$link_path"
        log_info "已创建软链接: $link_path -> $SCRIPT_PATH"
    done

    if command -v envs >/dev/null 2>&1; then
        envs_cmd="$(command -v envs)"
    elif [[ -x "$SCRIPT_DIR/../os/envs" ]]; then
        envs_cmd="$SCRIPT_DIR/../os/envs"
    fi

    if [[ -n "$envs_cmd" ]]; then
        if [[ -n "$rc_file" ]]; then
            "$envs_cmd" add "PATH=$install_dir" "$rc_file" >/dev/null
        else
            "$envs_cmd" add "PATH=$install_dir" >/dev/null
        fi
        log_info "已通过 envs 添加 PATH: $install_dir"
    else
        log_warn "未检测到 envs，请手动添加 PATH: export PATH=\"$install_dir:\$PATH\""
    fi

    case ":$PATH:" in
        *":$install_dir:"*) ;;
        *) export PATH="$install_dir:$PATH" ;;
    esac

    if command -v giter >/dev/null 2>&1 && command -v giter.sh >/dev/null 2>&1; then
        log_info "命令已可用: giter, giter.sh"
    else
        log_warn "当前 shell 尚未加载新 PATH，请执行: source ${rc_file:-$HOME/.zshrc} 或重新打开终端"
    fi
}

print_status_summary() {
    local repo_name="${1:-}"
    local status_output file_count
    status_output="$(git status --short 2>/dev/null || true)"
    [[ -n "$status_output" ]] || return 0

    file_count="$(printf '%s\n' "$status_output" | sed '/^[[:space:]]*$/d' | wc -l | tr -d ' ')"
    if [[ "$VERBOSE" -eq 1 ]]; then
        if [[ -n "$repo_name" ]]; then
            log_info "${repo_name}: 检测到 ${file_count:-0} 个变更文件。"
        else
            log_info "检测到 ${file_count:-0} 个变更文件。"
        fi
        printf '%s\n' "$status_output" >&2
    else
        if [[ -n "$repo_name" ]]; then
            log_info "${repo_name}: 检测到 ${file_count:-0} 个变更文件。"
        else
            log_info "检测到 ${file_count:-0} 个变更文件。"
        fi
    fi
}

print_last_commit_details() {
    local repo_name="$1"
    local commit author committer subject files commit_time
    local config_name config_email config_name_origin config_email_origin
    local config_origin commit_count idx
    local -a recent_commits

    [[ "$VERBOSE" -eq 1 ]] || return 0
    commit_count="$LAST_COMMITS"
    [[ "$commit_count" =~ ^[0-9]+$ ]] || commit_count=1
    [[ "$commit_count" -gt 0 ]] || commit_count=1
    [[ "$commit_count" -le 20 ]] || commit_count=20
    while IFS= read -r commit; do
        [[ -n "$commit" ]] && recent_commits+=("$commit")
    done < <(git log -"${commit_count}" --format='%H' 2>/dev/null || true)
    [[ ${#recent_commits[@]} -gt 0 ]] || return 0

    config_name="$(git config --get user.name 2>/dev/null || true)"
    config_email="$(git config --get user.email 2>/dev/null || true)"
    config_name_origin="$(git config --show-origin --get user.name 2>/dev/null | awk '{print $1}' || true)"
    config_email_origin="$(git config --show-origin --get user.email 2>/dev/null | awk '{print $1}' || true)"
    if [[ -n "$config_name_origin" && "$config_name_origin" == "$config_email_origin" ]]; then
        config_origin="$config_name_origin"
    fi

    log_info "${repo_name}: 本次没有工作区变更。"
    if [[ -n "$config_name" || -n "$config_email" ]]; then
        printf '  当前仓库 Git 用户: %s <%s>\n' "${config_name:-未知}" "${config_email:-未知}" >&2
        if [[ -n "$config_origin" ]]; then
            printf '  Git 用户配置来源: %s\n' "$config_origin" >&2
        elif [[ -n "$config_name_origin" || -n "$config_email_origin" ]]; then
            printf '  Git 用户配置来源:\n' >&2
            printf '    name: %s\n' "${config_name_origin:-未知}" >&2
            printf '    email: %s\n' "${config_email_origin:-未知}" >&2
        fi
    fi

    if [[ "${#recent_commits[@]}" -eq 1 ]]; then
        log_info "${repo_name}: 最近一次提交（非本次提交）:"
    else
        log_info "${repo_name}: 最近 ${#recent_commits[@]} 次提交（非本次提交）:"
    fi

    idx=1
    for commit in "${recent_commits[@]}"; do
        author="$(git log -1 --format='%an <%ae>' "$commit" 2>/dev/null || true)"
        committer="$(git log -1 --format='%cn <%ce>' "$commit" 2>/dev/null || true)"
        commit_time="$(git log -1 --format='%ci' "$commit" 2>/dev/null || true)"
        subject="$(git log -1 --format='%s' "$commit" 2>/dev/null || true)"
        files=""
        if [[ "$SHOW_LAST_FILES" -eq 1 ]]; then
            files="$(git diff-tree --root --no-commit-id --name-status -r "$commit" 2>/dev/null || true)"
        fi

        printf '  %d. %s %s %s\n' "$idx" "$(git rev-parse --short "$commit" 2>/dev/null || echo "$commit")" "$commit_time" "${author:-未知}" >&2
        [[ -n "$committer" && "$committer" != "$author" ]] && printf '     提交者: %s\n' "$committer" >&2
        printf '     %s\n' "${subject:-无提交说明}" >&2
        if [[ "$SHOW_LAST_FILES" -eq 0 ]]; then
            :
        elif [[ -n "$files" ]]; then
            printf '     文件:\n' >&2
            printf '%s\n' "$files" | sed 's/^/       /' >&2
        else
            printf '     文件: 无文件列表\n' >&2
        fi
        idx=$((idx + 1))
        [[ "$idx" -le "${#recent_commits[@]}" ]] && printf '\n' >&2
    done
}

add_summary() {
    local repo="$1"
    local status="$2"
    local detail="${3:-}"
    local line="$repo|$status|$detail"

    RUN_SUMMARY+=("$line")
    if [[ -n "${GITER_SUMMARY_FILE:-}" ]]; then
        printf '%s\n' "$line" >> "$GITER_SUMMARY_FILE"
    fi
}

print_summary() {
    local item repo status detail
    local summary_items=()

    if [[ -n "${GITER_SUMMARY_FILE:-}" && -f "$GITER_SUMMARY_FILE" ]]; then
        while IFS= read -r item; do
            [[ -n "$item" ]] || continue
            summary_items+=("$item")
        done < "$GITER_SUMMARY_FILE"
    else
        summary_items=("${RUN_SUMMARY[@]+"${RUN_SUMMARY[@]}"}")
    fi

    [[ ${#summary_items[@]} -gt 0 ]] || return 0

    echo "" >&2
    log_info "执行摘要"
    for item in "${summary_items[@]+"${summary_items[@]}"}"; do
        IFS='|' read -r repo status detail <<< "$item"
        local repo_name
        repo_name="$(basename "$repo")"
        local status_icon
        case "$status" in
            完成) status_icon="✅" ;;
            失败) status_icon="❌" ;;
            暂停) status_icon="⏸️" ;;
            跳过) status_icon="⏭️" ;;
            预演) status_icon="🔍" ;;
            *)    status_icon="•" ;;
        esac
        if [[ -n "$detail" ]]; then
            printf '  %s %s: %s\n' "$status_icon" "$repo_name" "$detail" >&2
        else
            printf '  %s %s [%s]\n' "$status_icon" "$repo_name" "$status" >&2
        fi
    done
}

show_help() {
    cat <<EOF
Git 批量更新推送脚本 (增强版)

用法: $0 [选项] [-- path1 path2 ...]

选项:
  -m, --message MSG  指定提交注释
  -d, --dir PATH     指定根目录 (默认: 脚本所在目录)
  -b, --branch NAME  指定要处理的本地或远程分支；未指定时交互选择，默认当前分支
  -a, --auto         自动发现模式：扫描根目录、父目录、子目录中的所有 Git 仓库
  --all-nested       自动发现时包含未声明的嵌套 Git 仓库，但仍排除 GITER_EXCLUDE_DIRS
  --with-nested LIST 只处理指定的可推送嵌套仓库，逗号分隔或重复传参，如: --with-nested web
  --skip-nested LIST 跳过指定的可推送嵌套仓库，逗号分隔或重复传参
  --push-remotes LIST 提交后额外推送到指定远程，逗号分隔，如: --push-remotes origin,gitea
  --dry-run          预演模式：显示将执行的操作，不 fetch/rebase/commit/push
  --show-config      显示仓库配置、发现的嵌套仓库和候选仓库后退出
  --install          安装命令到 PATH：创建 ~/.local/bin/giter 和 giter.sh 软链接，并优先用 envs 配置 PATH
  -l, --depth NUM    自动发现扫描深度 (默认: 2)
                     0=仅根目录, 1=包括父目录, 2=包括子目录
  -f, --force        强制拉取到远端状态，需同时设置 GITER_ALLOW_RESET_HARD=1
  -i, --interactive  交互模式 (提交前需确认)，默认自动确认
  -n, --no-push      执行拉取和提交，但不推送
  -v, --verbose      显示详细日志
  --last, --lc NUM   配合 -v 显示最近 NUM 次提交详情（默认: 1，最大: 20）
  --no-files         配合 -v 隐藏最近提交详情中的文件列表
  -h, --help         显示帮助

环境变量:
  GITER_ADD_ALL=1              允许直接 git add -A（默认使用安全过滤暂存）
  GITER_ALLOW_RESET_HARD=1     允许 --force 执行 git reset --hard
  GITER_CONFIRM_DESTRUCTIVE=1  允许非交互执行 reset --hard
  GITER_DIVERGENCE_ACTION=     分叉处理策略: rebase(推荐), merge, reset, abort
  GITER_MAX_STAGE_FILE_MB=50   自动暂存文件大小上限
  GITER_CONFIG_HOME=~/.config/giter
  GITER_AUTO_UPDATE_GITIGNORE=0 禁止自动写入嵌套 Git 仓库和排除目录到 .gitignore（默认启用）
  GITER_GIT_NETWORK_TIMEOUT=30 Git fetch/push 网络操作超时秒数，0 表示不限制
  GITER_BRANCH_CACHE_TIMEOUT=8 分支列表远程缓存刷新超时秒数，0 表示不限制
  GITER_INPUT_TIMEOUT=5       交互输入等待秒数，超时采用当前提示的默认值，0 表示不限制
  GITER_REFRESH_BRANCH_CACHE=auto 分支列表远程缓存刷新策略: auto, 1, 0
  GITER_SSH_CONNECT_TIMEOUT=10 SSH 连接超时秒数
  GITER_NESTED_POLICIES=       嵌套仓库策略，格式: "path|strategy|remote|branch"
                               strategy: vendor-copy, vendor-copy-push, independent-push, update-only, reference, submodule
  GITER_EXTRA_PUSH_REMOTES=()   额外推送远程数组，例如: ("gitea")
  GITER_GITIGNORE_HASH=        主仓库 .gitignore 内容哈希，由脚本自动维护
  GITER_RETURN_ORIGINAL_BRANCH=1 非交互模式下，处理完指定分支后自动切回原分支
  GITER_DIRTY_WORKDIR_ACTION=  工作区有变更且需要同步远端时的处理策略:
                               commit(默认,先提交再同步), stash(储藏后同步), skip(跳过同步), discard(丢弃变更), abort(停止)

示例:
  $0
  $0 -m "修复 Bug"
  $0 -b develop -m "更新 develop"
  $0 -a -d /path/to/project
  $0 -l 1 -m "更新"
  $0 -i -d /path/to/project
  $0 -v --last 5
  $0 -v --last 5 --no-files
  $0 --install
  $0 --dry-run -d /path/to/project
  $0 --show-config -d /path/to/project
  $0 -m "只提交脚本" -- ai/ai-hub.sh ai/conf/tools/cli/claude.json

说明:
  无参数运行时默认以脚本所在目录为工作根目录。
  未通过 -b/--branch 指定分支时，交互终端会显示本地和远程分支列表，默认使用当前分支。
  如果选择的分支与原分支不同，操作完成后会询问是否切回原分支；非交互终端默认不切回，可用 GITER_RETURN_ORIGINAL_BRANCH=1 启用。
  交互终端使用 -d/--dir 指定目录时，指定目录所属 Git 仓库会优先作为第一个候选仓库。
  如果从另一个 Git 仓库目录执行本脚本，会同时识别“执行目录仓库”和“脚本所属仓库”，并让用户选择处理哪个仓库或多个仓库。
  自动发现会识别根目录下的嵌套 Git 仓库；未知嵌套仓库会在交互终端询问策略，非交互终端只提示和跳过。
  建议使用 GITER_NESTED_POLICIES 声明策略：vendor-copy 由父仓库提交目录内容但跳过子仓库 .git，vendor-copy-push 还会把子仓库纳入独立推送候选，reference 忽略不推送，independent-push 独立提交推送，update-only 只同步不提交推送，submodule 只提交指针。
  .gitignore 只用于推导 reference 和维护父仓库忽略项；无法仅凭 .gitignore 判断是否需要 independent-push。
  如 reference/independent-push/update-only 排除目录已被父仓库跟踪，会自动 git rm --cached 移出父仓库索引但保留工作区文件。
  默认只把 independent-push、vendor-copy-push、update-only、GITER_PUSH_DIRS 中声明的嵌套仓库作为处理候选；--all-nested 可临时包含未声明的嵌套仓库。
  --dry-run 只展示计划，不修改 .gitignore、不暂存、不同步、不提交、不推送。
  --show-config 可用于诊断仓库哈希配置、推送目录、排除目录和嵌套仓库发现结果。
  非交互终端会保持脚本化行为，只处理指定目录或自动发现结果，不弹出选择。
  安装后可在任意仓库目录直接运行: giter 或 giter.sh
EOF
}

hash_text() {
    local text="$1"
    if command -v shasum >/dev/null 2>&1; then
        printf '%s' "$text" | LC_ALL=C LANG=C shasum -a 256 | awk '{print substr($1,1,16)}'
    elif command -v sha256sum >/dev/null 2>&1; then
        printf '%s' "$text" | LC_ALL=C LANG=C sha256sum | awk '{print substr($1,1,16)}'
    else
        log_error "缺少哈希命令: shasum 或 sha256sum"
        return 1
    fi
}

append_csv_values_to_array() {
    local arr_name="$1"
    local csv="$2"
    local item

    [[ "$arr_name" =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ ]] || return 1
    csv="${csv//,/ }"
    for item in $csv; do
        [[ -n "$item" ]] || continue
        eval "${arr_name}+=(\"\$item\")"
    done
}

setup_git_network_env() {
    export GIT_TERMINAL_PROMPT="${GIT_TERMINAL_PROMPT:-0}"
    if [[ -z "${GIT_SSH_COMMAND:-}" ]]; then
        export GIT_SSH_COMMAND="ssh -o BatchMode=yes -o ConnectTimeout=${GITER_SSH_CONNECT_TIMEOUT} -o ServerAliveInterval=5 -o ServerAliveCountMax=1"
    fi
}

run_command_with_timeout() {
    local seconds="$1"
    shift
    local pid deadline now use_process_group=0

    if [[ "$seconds" -le 0 ]]; then
        "$@"
        return $?
    fi

    # 优先使用系统 timeout 命令（精度更高、无轮询开销）
    if command -v timeout >/dev/null 2>&1; then
        timeout "$seconds" "$@"
        return $?
    elif command -v gtimeout >/dev/null 2>&1; then
        gtimeout "$seconds" "$@"
        return $?
    fi

    deadline=$(($(date +%s) + seconds))
    if command -v setsid >/dev/null 2>&1; then
        setsid "$@" &
        use_process_group=1
    else
        "$@" &
    fi
    pid=$!

    while kill -0 "$pid" 2>/dev/null; do
        now=$(date +%s)
        if [[ "$now" -ge "$deadline" ]]; then
            if [[ "$use_process_group" -eq 1 ]]; then
                kill "-$pid" 2>/dev/null || kill "$pid" 2>/dev/null || true
                sleep 0.1 2>/dev/null || sleep 1
                kill -9 "-$pid" 2>/dev/null || kill -9 "$pid" 2>/dev/null || true
            else
                kill "$pid" 2>/dev/null || true
                sleep 0.1 2>/dev/null || sleep 1
                kill -9 "$pid" 2>/dev/null || true
            fi
            wait "$pid" 2>/dev/null || true
            return 124
        fi
        sleep 0.1 2>/dev/null || sleep 1
    done

    wait "$pid"
}

run_git_network_command_with_timeout() {
    local seconds="$1"
    shift
    setup_git_network_env
    run_command_with_timeout "$seconds" git "$@"
}

run_git_network_command() {
    run_git_network_command_with_timeout "$GITER_GIT_NETWORK_TIMEOUT" "$@"
}

run_git_push() {
    local status
    local output

    if [[ "$VERBOSE" -eq 1 ]]; then
        run_git_network_command push "$@"
        status=$?
    else
        output=$(run_git_network_command push "$@" 2>&1)
        status=$?
        if [[ "$status" -ne 0 && "$status" -ne 124 ]]; then
            log_error "推送失败详细原因:\n$output"
        fi
    fi

    if [[ "$status" -eq 124 ]]; then
        log_error "推送远程超时（${GITER_GIT_NETWORK_TIMEOUT}s），请检查网络或 SSH 认证。"
    fi
    return "$status"
}

is_git_repo() {
    local path="${1:-.}"
    git -C "$path" rev-parse --is-inside-work-tree >/dev/null 2>&1
}

git_root_for() {
    local path="$1"
    git -C "$path" rev-parse --show-toplevel 2>/dev/null || return 1
}

repo_config_path() {
    local repo_root="$1"
    local repo_hash
    repo_hash="$(hash_text "$repo_root")" || return 1
    printf '%s/%s.conf\n' "$GITER_REPO_CONFIG_DIR" "$repo_hash"
}

update_repo_config_index() {
    local repo_root="$1"
    local config_file="$2"
    local repo_hash index_file tmp_file

    repo_hash="$(hash_text "$repo_root")" || return 1
    mkdir -p "$GITER_REPO_CONFIG_DIR"
    index_file="$GITER_REPO_CONFIG_DIR/index.tsv"
    tmp_file="$(mktemp)"

    if [[ -f "$index_file" ]]; then
        awk -F '\t' -v key="$repo_hash" '$1 != key' "$index_file" > "$tmp_file"
    fi
    printf '%s\t%s\t%s\n' "$repo_hash" "$repo_root" "$config_file" >> "$tmp_file"
    mv "$tmp_file" "$index_file"
}

ensure_repo_config_file() {
    local repo_root="$1"
    local config_file

    config_file="$(repo_config_path "$repo_root")" || return 1
    if [[ "$DRY_RUN" -eq 1 ]]; then
        printf '%s\n' "$config_file"
        return 0
    fi

    mkdir -p "$(dirname "$config_file")"
    [[ -f "$config_file" ]] || {
        printf '# giter repository config for %s\n' "$repo_root" > "$config_file"
    }
    printf '%s\n' "$config_file"
}

set_repo_config_scalar() {
    local repo_root="$1"
    local key="$2"
    local value="$3"
    local config_file tmp_file

    [[ "$key" =~ ^[A-Z_][A-Z0-9_]*$ ]] || return 1
    config_file="$(ensure_repo_config_file "$repo_root")" || return 1

    if [[ "$DRY_RUN" -eq 1 ]]; then
        log_info "[dry-run] 将写入仓库配置: $key=$value"
        return 0
    fi

    tmp_file="$(mktemp)"
    if [[ -f "$config_file" ]]; then
        grep -v -E "^${key}=" "$config_file" > "$tmp_file" || true
    fi
    printf '%s="%s"\n' "$key" "$value" >> "$tmp_file"
    mv "$tmp_file" "$config_file"
}

gitignore_hash_for_repo() {
    local repo_root="$1"
    local gitignore_file="$repo_root/.gitignore"

    if [[ -f "$gitignore_file" ]]; then
        if command -v shasum >/dev/null 2>&1; then
            LC_ALL=C LANG=C shasum -a 256 "$gitignore_file" | awk '{print substr($1,1,16)}'
        elif command -v sha256sum >/dev/null 2>&1; then
            LC_ALL=C LANG=C sha256sum "$gitignore_file" | awk '{print substr($1,1,16)}'
        else
            log_error "缺少哈希命令: shasum 或 sha256sum"
            return 1
        fi
    else
        hash_text "__giter_missing_gitignore__"
    fi
}

update_repo_gitignore_hash() {
    local repo_root="$1"
    local current_hash

    [[ "$SHOW_CONFIG" -eq 0 ]] || return 0
    current_hash="$(gitignore_hash_for_repo "$repo_root")" || return 1
    [[ "$current_hash" != "${GITER_GITIGNORE_HASH:-}" ]] || return 0
    set_repo_config_scalar "$repo_root" "GITER_GITIGNORE_HASH" "$current_hash" || return 1
    GITER_GITIGNORE_HASH="$current_hash"
}

resolve_repo_config_path() {
    local repo_root="$1"
    local path="$2"

    if [[ "$path" == /* ]]; then
        printf '%s\n' "$path"
    else
        printf '%s/%s\n' "$repo_root" "${path#./}"
    fi
}

resolve_repo_config_paths() {
    local repo_root="$1"
    local arr_name="$2"
    local resolved=()
    local path

    # 安全校验：变量名只允许字母、数字、下划线
    [[ "$arr_name" =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ ]] || return 1

    local source_values=()
    eval "source_values=(\"\${${arr_name}[@]+\${${arr_name}[@]}}\")"

    for path in "${source_values[@]+"${source_values[@]}"}"; do
        [[ -n "$path" ]] || continue
        if [[ "$path" == /* ]]; then
            resolved+=("$path")
        else
            resolved+=("$repo_root/${path#./}")
        fi
    done

    eval "${arr_name}=(\"\${resolved[@]+\${resolved[@]}}\")"
}

nested_policy_strategy_is_valid() {
    case "$1" in
        vendor-copy|vendor-copy-push|subtree|independent-push|update-only|reference|submodule) return 0 ;;
        *) return 1 ;;
    esac
}

nested_policy_add() {
    local repo_root="$1"
    local path="$2"
    local strategy="$3"
    local remote="${4:-}"
    local branch="${5:-}"
    local resolved existing

    [[ -n "$path" && -n "$strategy" ]] || return 0
    nested_policy_strategy_is_valid "$strategy" || {
        log_warn "忽略未知嵌套仓库策略: $path|$strategy"
        return 0
    }

    resolved="$(resolve_repo_config_path "$repo_root" "$path")"
    resolved="$(trim_trailing_slash "$resolved")"

    for existing in "${GITER_NESTED_POLICY_PATHS[@]+"${GITER_NESTED_POLICY_PATHS[@]}"}"; do
        [[ "$existing" == "$resolved" ]] && return 0
    done

    GITER_NESTED_POLICY_PATHS+=("$resolved")
    GITER_NESTED_POLICY_STRATEGIES+=("$strategy")
    GITER_NESTED_POLICY_REMOTES+=("$remote")
    GITER_NESTED_POLICY_BRANCHES+=("$branch")
}

resolve_nested_policies() {
    local repo_root="$1"
    local entry path strategy remote branch

    GITER_NESTED_POLICY_PATHS=()
    GITER_NESTED_POLICY_STRATEGIES=()
    GITER_NESTED_POLICY_REMOTES=()
    GITER_NESTED_POLICY_BRANCHES=()

    for entry in "${GITER_NESTED_POLICIES[@]+"${GITER_NESTED_POLICIES[@]}"}"; do
        IFS='|' read -r path strategy remote branch <<< "$entry"
        nested_policy_add "$repo_root" "$path" "$strategy" "${remote:-}" "${branch:-}"
    done

    # 兼容旧配置：推送目录视为 independent-push，排除目录视为 reference。
    for path in "${GITER_PUSH_DIRS[@]+"${GITER_PUSH_DIRS[@]}"}"; do
        nested_policy_add "$repo_root" "$path" "independent-push"
    done
    for path in "${GITER_EXCLUDE_DIRS[@]+"${GITER_EXCLUDE_DIRS[@]}"}"; do
        nested_policy_add "$repo_root" "$path" "reference"
    done
}

nested_policy_index_for_path() {
    local abs_path="$1"
    local mode="${2:-under}"
    local policy_path
    local i

    abs_path="$(trim_trailing_slash "$abs_path")"
    for i in "${!GITER_NESTED_POLICY_PATHS[@]}"; do
        policy_path="$(trim_trailing_slash "${GITER_NESTED_POLICY_PATHS[$i]}")"
        if [[ "$mode" == "exact" ]]; then
            [[ "$abs_path" == "$policy_path" ]] && { echo "$i"; return 0; }
        else
            [[ "$abs_path" == "$policy_path" || "$abs_path" == "$policy_path"/* ]] && { echo "$i"; return 0; }
        fi
    done

    return 1
}

nested_policy_strategy_for_path() {
    local abs_path="$1"
    local mode="${2:-under}"
    local idx

    idx="$(nested_policy_index_for_path "$abs_path" "$mode" 2>/dev/null || true)"
    [[ -n "$idx" ]] || return 1
    echo "${GITER_NESTED_POLICY_STRATEGIES[$idx]}"
}

is_submodule_path() {
    local repo_root="$1"
    local rel_path="$2"

    [[ -n "$rel_path" && "$rel_path" != "." ]] || return 1
    if git -C "$repo_root" ls-files --stage -- "$rel_path" 2>/dev/null | awk '$1 == "160000" { found=1 } END { exit(found ? 0 : 1) }'; then
        return 0
    fi

    [[ -f "$repo_root/.gitmodules" ]] || return 1
    git -C "$repo_root" config --file .gitmodules --get-regexp 'submodule\..*\.path' 2>/dev/null \
        | awk -v p="$rel_path" '$2 == p { found=1 } END { exit(found ? 0 : 1) }'
}

is_vendor_copy_strategy() {
    case "$1" in
        vendor-copy|vendor-copy-push|subtree) return 0 ;;
        *) return 1 ;;
    esac
}

has_vendor_copy_policies_for_repo() {
    local repo_root="$1"
    local i path strategy

    for i in "${!GITER_NESTED_POLICY_PATHS[@]}"; do
        path="${GITER_NESTED_POLICY_PATHS[$i]}"
        strategy="${GITER_NESTED_POLICY_STRATEGIES[$i]}"
        is_vendor_copy_strategy "$strategy" || continue
        [[ -n "$path" ]] || continue
        case "$(trim_trailing_slash "$path")" in
            "$repo_root"/*) return 0 ;;
        esac
    done

    return 1
}

recover_stale_vendor_copy_git_dirs() {
    local repo_root="$1"
    local i path strategy hidden_meta git_meta matches rel_path

    [[ "$DRY_RUN" -eq 0 ]] || return 0
    [[ -d "$repo_root" ]] || return 0

    for i in "${!GITER_NESTED_POLICY_PATHS[@]}"; do
        path="${GITER_NESTED_POLICY_PATHS[$i]}"
        strategy="${GITER_NESTED_POLICY_STRATEGIES[$i]}"
        is_vendor_copy_strategy "$strategy" || continue
        [[ -n "$path" && -d "$path" ]] || continue
        path="$(trim_trailing_slash "$path")"
        case "$path" in
            "$repo_root"/*) ;;
            *) continue ;;
        esac

        for hidden_meta in "$path"/${GITER_HIDDEN_GIT_DIR_NAME}.*; do
            [[ -e "$hidden_meta" ]] || continue
            git_meta="$(dirname "$hidden_meta")/.git"
            rel_path="$(repo_relative_path "$repo_root" "$(dirname "$hidden_meta")" 2>/dev/null || dirname "$hidden_meta")"

            if [[ -e "$git_meta" ]]; then
                log_warn "发现残留的临时隐藏 Git 元数据，但 .git 已存在，请手动确认: $rel_path/$(basename "$hidden_meta")"
                continue
            fi

            matches="$(find "$(dirname "$hidden_meta")" -maxdepth 1 -name "${GITER_HIDDEN_GIT_DIR_NAME}.*" -print 2>/dev/null | wc -l | tr -d ' ')"
            if [[ "$matches" == "1" ]]; then
                mv "$hidden_meta" "$git_meta"
                log_warn "已恢复上次中断遗留的嵌套 Git 元数据: $rel_path/.git"
            else
                log_warn "发现多个临时隐藏 Git 元数据，请手动确认后恢复: $rel_path"
            fi
        done
    done
}

hide_vendor_copy_git_dirs() {
    local repo_root="$1"
    local i path strategy git_meta hidden_meta rel_path

    GITER_HIDDEN_GIT_DIRS=()

    for i in "${!GITER_NESTED_POLICY_PATHS[@]}"; do
        path="${GITER_NESTED_POLICY_PATHS[$i]}"
        strategy="${GITER_NESTED_POLICY_STRATEGIES[$i]}"
        is_vendor_copy_strategy "$strategy" || continue
        [[ -n "$path" && -d "$path" ]] || continue
        path="$(trim_trailing_slash "$path")"
        case "$path" in
            "$repo_root"/*) ;;
            *) continue ;;
        esac

        git_meta="$path/.git"
        [[ -e "$git_meta" ]] || continue
        hidden_meta="$path/${GITER_HIDDEN_GIT_DIR_NAME}.$$"
        if [[ -e "$hidden_meta" ]]; then
            log_error "临时隐藏目录已存在，拒绝继续: $hidden_meta"
            return 1
        fi

        if [[ "$DRY_RUN" -eq 1 ]]; then
            rel_path="$(repo_relative_path "$repo_root" "$path" 2>/dev/null || echo "$path")"
            log_info "[dry-run] vendor-copy 将临时隐藏嵌套仓库元数据: $rel_path/.git"
            continue
        fi

        mv "$git_meta" "$hidden_meta"
        GITER_HIDDEN_GIT_DIRS+=("$hidden_meta|$git_meta")
        rel_path="$(repo_relative_path "$repo_root" "$path" 2>/dev/null || echo "$path")"
        log_debug "vendor-copy 暂存期间隐藏嵌套仓库元数据: $rel_path/.git"
    done
}

restore_vendor_copy_git_dirs() {
    local i entry hidden_meta git_meta

    for ((i=${#GITER_HIDDEN_GIT_DIRS[@]}-1; i>=0; i--)); do
        entry="${GITER_HIDDEN_GIT_DIRS[$i]}"
        IFS='|' read -r hidden_meta git_meta <<< "$entry"
        [[ -n "$hidden_meta" && -n "$git_meta" ]] || continue
        if [[ -e "$hidden_meta" ]]; then
            mv "$hidden_meta" "$git_meta"
        fi
    done
    GITER_HIDDEN_GIT_DIRS=()
}

prepare_vendor_copy_index_paths() {
    local repo_root="$1"
    local i path strategy rel_path

    for i in "${!GITER_NESTED_POLICY_PATHS[@]}"; do
        path="${GITER_NESTED_POLICY_PATHS[$i]}"
        strategy="${GITER_NESTED_POLICY_STRATEGIES[$i]}"
        is_vendor_copy_strategy "$strategy" || continue
        [[ -n "$path" ]] || continue
        case "$path" in
            "$repo_root"/*) ;;
            *) continue ;;
        esac
        rel_path="$(repo_relative_path "$repo_root" "$path" 2>/dev/null || true)"
        [[ -n "$rel_path" && "$rel_path" != "." ]] || continue

        if is_submodule_path "$repo_root" "$rel_path"; then
            log_warn "vendor-copy 路径已在父仓库索引中记录为 Gitlink，移出索引后改按普通目录提交: $rel_path"
            if [[ "$DRY_RUN" -eq 1 ]]; then
                log_info "[dry-run] 将执行: git rm --cached -- $rel_path"
            else
                git -C "$repo_root" rm --cached -- "$rel_path" >/dev/null
            fi
        fi
    done
}

append_nested_policy_to_config() {
    local repo_root="$1"
    local rel_path="$2"
    local strategy="$3"
    local remote="${4:-}"
    local branch="${5:-}"
    local config_file

    config_file="$(ensure_repo_config_file "$repo_root")" || return 1
    if [[ "$DRY_RUN" -eq 1 ]]; then
        log_info "[dry-run] 将写入嵌套仓库策略: $rel_path|$strategy${remote:+|$remote}${branch:+|$branch}"
        return 0
    fi

    if ! grep -Fq "\"$rel_path|" "$config_file" 2>/dev/null; then
        printf 'GITER_NESTED_POLICIES+=("%s|%s|%s|%s")\n' "$rel_path" "$strategy" "$remote" "$branch" >> "$config_file"
        log_info "已写入嵌套仓库策略: $config_file"
    fi
}

prompt_unknown_nested_repo_policies() {
    local repo_root="$1"
    local nested rel_path choice strategy remote branch configured_any=0

    [[ -t 0 ]] || {
        for nested in "${GITER_NESTED_REPO_DIRS[@]+"${GITER_NESTED_REPO_DIRS[@]}"}"; do
            if ! nested_policy_strategy_for_path "$nested" "under" >/dev/null 2>&1; then
                rel_path="$(repo_relative_path "$repo_root" "$nested" || echo "$nested")"
                if parent_gitignore_ignores_path "$repo_root" "$rel_path"; then
                    log_warn "发现 .gitignore 已忽略但策略未声明的嵌套 Git 仓库，交互运行可确认写入 reference: $rel_path"
                else
                    log_warn "发现未声明策略的嵌套 Git 仓库，默认跳过父仓库提交/子仓库推送: $rel_path"
                fi
            fi
        done
        return 0
    }

    for nested in "${GITER_NESTED_REPO_DIRS[@]+"${GITER_NESTED_REPO_DIRS[@]}"}"; do
        nested_policy_strategy_for_path "$nested" "under" >/dev/null 2>&1 && continue

        rel_path="$(repo_relative_path "$repo_root" "$nested" || echo "$nested")"
        if is_submodule_path "$repo_root" "$rel_path"; then
            append_nested_policy_to_config "$repo_root" "$rel_path" "submodule" "" ""
            configured_any=1
            continue
        fi

        if parent_gitignore_ignores_path "$repo_root" "$rel_path"; then
            echo "" >&2
            log_warn "发现 .gitignore 已忽略的嵌套 Git 仓库: $rel_path"
            read_input choice "是否按 reference（不提交不推送）写入策略配置? [Y/n]: "
            choice="${choice:-Y}"
            case "$choice" in
                y|Y|yes|YES)
                    append_nested_policy_to_config "$repo_root" "$rel_path" "reference" "" ""
                    configured_any=1
                    continue
                    ;;
                n|N|no|NO) ;;
                *)
                    log_warn "无效选择，继续显示完整处理方式: $rel_path"
                    ;;
            esac
        fi

        echo "" >&2
        log_warn "发现未声明策略的嵌套 Git 仓库: $rel_path"
        echo "请选择处理方式:" >&2
        echo "  1) reference：不提交到父仓库，也不推送子仓库" >&2
        echo "  2) independent-push：子仓库单独提交/推送，父仓库不提交该目录" >&2
        echo "  3) update-only：子仓库只同步远端，不提交不推送，父仓库不提交该目录" >&2
        echo "  4) vendor-copy-push：子仓库单独提交/推送，父仓库也提交目录内容但不提交 .git" >&2
        echo "  5) vendor-copy：只由父仓库提交目录内容，子仓库不自动推送" >&2
        echo "  6) submodule：作为 Git submodule 管理，只提交 commit 指针" >&2
        echo "  7) 暂不处理（本次跳过，不写配置，下次仍会询问）" >&2
        read_input choice "请选择 [默认 7]: "
        choice="${choice:-7}"

        strategy=""
        remote=""
        branch=""
        case "$choice" in
            1|reference) strategy="reference" ;;
            2|independent-push) strategy="independent-push" ;;
            3|update-only) strategy="update-only" ;;
            4|vendor-copy-push)
                strategy="vendor-copy-push"
                ;;
            5|vendor-copy|subtree)
                strategy="vendor-copy"
                ;;
            6|submodule) strategy="submodule" ;;
            7|skip|"") continue ;;
            *)
                log_warn "无效选择，跳过: $rel_path"
                continue
                ;;
        esac

        append_nested_policy_to_config "$repo_root" "$rel_path" "$strategy" "$remote" "$branch"
        configured_any=1
    done

    if [[ "$configured_any" -eq 1 ]]; then
        load_repo_config "$repo_root" || return 1
    fi
}

warn_nested_policy_mismatches() {
    local repo_root="$1"
    local i path strategy rel_path

    for i in "${!GITER_NESTED_POLICY_PATHS[@]}"; do
        path="${GITER_NESTED_POLICY_PATHS[$i]}"
        strategy="${GITER_NESTED_POLICY_STRATEGIES[$i]}"
        rel_path="$(repo_relative_path "$repo_root" "$path" 2>/dev/null || echo "$path")"
        case "$strategy" in
            subtree)
                log_warn "策略 subtree 已兼容为 vendor-copy，建议将配置更新为: $rel_path|vendor-copy"
                ;;
            submodule)
                if ! is_submodule_path "$repo_root" "$rel_path"; then
                    log_warn "策略为 submodule，但父仓库未检测到 submodule 指针/.gitmodules: $rel_path"
                fi
                ;;
        esac
    done
}

repo_relative_path() {
    local repo_root="$1"
    local abs_path="$2"

    case "$abs_path" in
        "$repo_root"/*) printf '%s\n' "${abs_path#"$repo_root"/}" ;;
        "$repo_root") printf '.\n' ;;
        *) return 1 ;;
    esac
}

trim_trailing_slash() {
    local path="$1"
    while [[ "$path" != "/" && "$path" == */ ]]; do
        path="${path%/}"
    done
    printf '%s\n' "${path:-/}"
}

stage_path_abs() {
    local path="$1"
    local abs_path

    if [[ "$path" == /* ]]; then
        abs_path="$path"
    else
        abs_path="$(pwd)/${path#./}"
    fi

    trim_trailing_slash "$abs_path"
}

path_is_same_or_under() {
    local path root
    path="$(trim_trailing_slash "$1")"
    root="$(trim_trailing_slash "$2")"
    [[ -n "$root" && ( "$path" == "$root" || "$path" == "$root"/* ) ]]
}

log_skip_once() {
    local key="$1"
    local message="$2"
    local level="${3:-warn}"
    local logged

    for logged in "${GITER_LOGGED_SKIP_PATHS[@]+"${GITER_LOGGED_SKIP_PATHS[@]}"}"; do
        [[ "$logged" == "$key" ]] && return 0
    done

    GITER_LOGGED_SKIP_PATHS+=("$key")
    case "$level" in
        info) log_info "$message" ;;
        *) log_warn "$message" ;;
    esac
}

load_repo_config() {
    local repo_root="$1"
    local config_file loaded already_indexed=0

    config_file="$(repo_config_path "$repo_root")" || return 1

    if declare -p GITER_LOADED_REPO_CONFIGS >/dev/null 2>&1; then
        for loaded in "${GITER_LOADED_REPO_CONFIGS[@]+"${GITER_LOADED_REPO_CONFIGS[@]}"}"; do
            [[ "$loaded" == "$repo_root" ]] && already_indexed=1
        done
    fi

    GITER_PUSH_DIRS=()
    GITER_EXCLUDE_DIRS=()
    GITER_NESTED_POLICIES=()
    GITER_EXTRA_PUSH_REMOTES=()
    GITER_GITIGNORE_HASH=""

    if [[ -f "$config_file" ]]; then
        # shellcheck source=/dev/null
        source "$config_file"
        resolve_repo_config_paths "$repo_root" GITER_PUSH_DIRS
        resolve_repo_config_paths "$repo_root" GITER_EXCLUDE_DIRS
        resolve_nested_policies "$repo_root"
        if [[ "$DRY_RUN" -eq 0 && "$SHOW_CONFIG" -eq 0 ]]; then
            update_repo_config_index "$repo_root" "$config_file"
        fi
        log_debug "已加载仓库配置: $config_file"
    else
        resolve_nested_policies "$repo_root"
    fi

    [[ "$already_indexed" -eq 1 ]] || GITER_LOADED_REPO_CONFIGS+=("$repo_root")
}

detect_nested_git_repos() {
    local repo_root="$1"
    local git_marker nested_root existing

    GITER_NESTED_REPO_DIRS=()
    GITER_LOGGED_SKIP_PATHS=()

    [[ -d "$repo_root" ]] || return 0

    while IFS= read -r -d '' git_marker; do
        nested_root="$(git_root_for "$(dirname "$git_marker")" 2>/dev/null || dirname "$git_marker")"
        nested_root="$(trim_trailing_slash "$nested_root")"
        [[ "$nested_root" != "$repo_root" ]] || continue

        for existing in "${GITER_NESTED_REPO_DIRS[@]+"${GITER_NESTED_REPO_DIRS[@]}"}"; do
            [[ "$existing" == "$nested_root" ]] && continue 2
        done

        GITER_NESTED_REPO_DIRS+=("$nested_root")
    done < <(find "$repo_root" -path "$repo_root/.git" -prune -o -name ".git" -print0 -prune 2>/dev/null)
}

parent_gitignore_ignores_path() {
    local repo_root="$1"
    local rel_path="$2"

    [[ -f "$repo_root/.gitignore" && -n "$rel_path" && "$rel_path" != "." ]] || return 1
    git -C "$repo_root" check-ignore --no-index -q -- "$rel_path/" "$rel_path/.git" 2>/dev/null
}

collect_parent_ignore_paths() {
    local repo_root="$1"
    local full_path clean_path existing
    local ignore_paths=()
    local i strategy

    for i in "${!GITER_NESTED_POLICY_PATHS[@]}"; do
        strategy="${GITER_NESTED_POLICY_STRATEGIES[$i]}"
        case "$strategy" in
            reference|independent-push|update-only) ;;
            *) continue ;;
        esac
        full_path="${GITER_NESTED_POLICY_PATHS[$i]}"
        [[ -n "$full_path" ]] || continue
        clean_path="$(repo_relative_path "$repo_root" "$full_path" || true)"
        [[ -n "$clean_path" && "$clean_path" != "." ]] || {
            log_warn "忽略非当前仓库路径，无法维护父仓库忽略项: $full_path"
            continue
        }

        clean_path="$(trim_trailing_slash "$clean_path")/"
        for existing in "${ignore_paths[@]+"${ignore_paths[@]}"}"; do
            [[ "$existing" == "$clean_path" ]] && continue 2
        done

        ignore_paths+=("$clean_path")
    done

    for full_path in "${GITER_EXCLUDE_DIRS[@]+"${GITER_EXCLUDE_DIRS[@]}"}"; do
        [[ -n "$full_path" ]] || continue
        clean_path="$(repo_relative_path "$repo_root" "$full_path" || true)"
        [[ -n "$clean_path" && "$clean_path" != "." ]] || continue
        clean_path="$(trim_trailing_slash "$clean_path")/"
        for existing in "${ignore_paths[@]+"${ignore_paths[@]}"}"; do
            [[ "$existing" == "$clean_path" ]] && continue 2
        done
        ignore_paths+=("$clean_path")
    done

    printf '%s\n' "${ignore_paths[@]+"${ignore_paths[@]}"}"
}

ensure_parent_gitignore_entries() {
    local repo_root="$1"
    local gitignore_file ignore_path

    [[ "$GITER_AUTO_UPDATE_GITIGNORE" == "1" ]] || return 0

    gitignore_file="$repo_root/.gitignore"
    while IFS= read -r ignore_path; do
        [[ -n "$ignore_path" ]] || continue
        if [ ! -f "$gitignore_file" ] || ! grep -qxF "$ignore_path" "$gitignore_file"; then
            if [[ "$DRY_RUN" -eq 1 ]]; then
                log_info "[dry-run] 将写入 .gitignore: $ignore_path"
                continue
            fi
            if [ -f "$gitignore_file" ] && [ -s "$gitignore_file" ] && [ -n "$(tail -c1 "$gitignore_file")" ]; then
                echo "" >> "$gitignore_file"
            fi
            echo "$ignore_path" >> "$gitignore_file"
            log_info "已写入 .gitignore: $ignore_path"
        fi
    done < <(collect_parent_ignore_paths "$repo_root")
}

untrack_parent_ignored_paths() {
    local repo_root="$1"
    local ignore_path tracked

    while IFS= read -r ignore_path; do
        [[ -n "$ignore_path" ]] || continue
        tracked="$(git -C "$repo_root" ls-files -- "$ignore_path" 2>/dev/null || true)"
        [[ -n "$tracked" ]] || continue

        log_warn "父仓库已跟踪嵌套/排除目录，移出索引但保留工作区: $ignore_path"
        if [[ "$DRY_RUN" -eq 1 ]]; then
            log_info "[dry-run] 将执行: git rm -r --cached -- $ignore_path"
            continue
        fi
        git -C "$repo_root" rm -r --cached -- "$ignore_path"
    done < <(collect_parent_ignore_paths "$repo_root")
}

prepare_parent_ignored_paths() {
    local repo_root="$1"
    local ignore_paths_cache

    ignore_paths_cache="$(collect_parent_ignore_paths "$repo_root")"
    [[ -n "$ignore_paths_cache" ]] || return 0

    # 复用缓存结果，避免 collect_parent_ignore_paths 被调用两次
    local gitignore_file ignore_path

    # ensure_parent_gitignore_entries 逻辑
    if [[ "$GITER_AUTO_UPDATE_GITIGNORE" == "1" ]]; then
        gitignore_file="$repo_root/.gitignore"
        while IFS= read -r ignore_path; do
            [[ -n "$ignore_path" ]] || continue
            if [ ! -f "$gitignore_file" ] || ! grep -qxF "$ignore_path" "$gitignore_file"; then
                if [[ "$DRY_RUN" -eq 1 ]]; then
                    log_info "[dry-run] 将写入 .gitignore: $ignore_path"
                    continue
                fi
                if [ -f "$gitignore_file" ] && [ -s "$gitignore_file" ] && [ -n "$(tail -c1 "$gitignore_file")" ]; then
                    echo "" >> "$gitignore_file"
                fi
                echo "$ignore_path" >> "$gitignore_file"
                log_info "已写入 .gitignore: $ignore_path"
            fi
        done <<< "$ignore_paths_cache"
    fi

    # untrack_parent_ignored_paths 逻辑
    local tracked
    while IFS= read -r ignore_path; do
        [[ -n "$ignore_path" ]] || continue
        tracked="$(git -C "$repo_root" ls-files -- "$ignore_path" 2>/dev/null || true)"
        [[ -n "$tracked" ]] || continue

        log_warn "父仓库已跟踪嵌套/排除目录，移出索引但保留工作区: $ignore_path"
        if [[ "$DRY_RUN" -eq 1 ]]; then
            log_info "[dry-run] 将执行: git rm -r --cached -- $ignore_path"
            continue
        fi
        git -C "$repo_root" rm -r --cached -- "$ignore_path"
    done <<< "$ignore_paths_cache"
}

file_size_bytes() {
    local file="$1"
    [[ -f "$file" ]] || { echo 0; return 0; }
    stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null || echo 0
}

path_matches_sensitive_pattern() {
    local path="$1"
    local base pattern
    base="$(basename "$path")"

    case "$base" in
        .env.example|.env.sample|.env.template|.env.local.example|example.env|sample.env|template.env)
            return 1
            ;;
    esac

    for pattern in "${SENSITIVE_PATH_PATTERNS[@]+"${SENSITIVE_PATH_PATTERNS[@]}"}"; do
        # shellcheck disable=SC2254
        case "$path" in
            $pattern|*/$pattern) return 0 ;;
        esac
        # shellcheck disable=SC2254
        case "$base" in
            $pattern) return 0 ;;
        esac
    done
    return 1
}

is_tracked_in_current_repo() {
    local path="$1"
    git ls-files --error-unmatch -- "$path" >/dev/null 2>&1
}

is_ignored_in_current_repo() {
    local path="$1"
    git check-ignore -q -- "$path" 2>/dev/null
}

ignored_path_key() {
    local path="$1"
    local ignore_info

    ignore_info="$(git check-ignore -v -- "$path" 2>/dev/null | head -1 || true)"
    if [[ -n "$ignore_info" ]]; then
        printf '%s\n' "$ignore_info" | awk -F '\t' '{print $1}'
    else
        printf '%s\n' "$path"
    fi
}

should_skip_stage_path() {
    local path="$1"
    local max_bytes size abs_path skip_path rel_path
    local strategy

    # 内联 stage_path_abs: 避免 subshell fork
    if [[ "$path" == /* ]]; then
        abs_path="$path"
    else
        abs_path="$(pwd)/${path#./}"
    fi
    # 内联 trim_trailing_slash
    while [[ "$abs_path" != "/" && "$abs_path" == */ ]]; do
        abs_path="${abs_path%/}"
    done

    case "$abs_path" in
        */.git|*/.git/*)
            local git_meta_path rel_git_meta_path
            git_meta_path="${abs_path%%/.git*}/.git"
            rel_git_meta_path="${git_meta_path#"$(pwd)"/}"
            [[ "$rel_git_meta_path" == "$git_meta_path" ]] && rel_git_meta_path="$path"
            log_skip_once "gitdir:$git_meta_path" "跳过嵌套 .git 元数据: $rel_git_meta_path"
            return 0
            ;;
        */${GITER_HIDDEN_GIT_DIR_NAME}.*|*/${GITER_HIDDEN_GIT_DIR_NAME}.*/*)
            local hidden_git_meta_path rel_hidden_git_meta_path
            hidden_git_meta_path="${abs_path%%/${GITER_HIDDEN_GIT_DIR_NAME}.*}/${GITER_HIDDEN_GIT_DIR_NAME}"
            rel_hidden_git_meta_path="${hidden_git_meta_path#"$(pwd)"/}"
            [[ "$rel_hidden_git_meta_path" == "$hidden_git_meta_path" ]] && rel_hidden_git_meta_path="$path"
            log_skip_once "hidden-gitdir:$hidden_git_meta_path" "跳过临时隐藏的嵌套 Git 元数据: $rel_hidden_git_meta_path"
            return 0
            ;;
    esac

    strategy="$(nested_policy_strategy_for_path "$abs_path" "under" 2>/dev/null || true)"
    case "$strategy" in
        reference|independent-push|update-only)
            rel_path="${abs_path#"$(pwd)"/}"
            [[ "$rel_path" == "$abs_path" ]] && rel_path="$abs_path"
            log_skip_once "policy:$strategy:$abs_path" "按 $strategy 策略跳过父仓库提交: $rel_path"
            return 0
            ;;
        submodule)
            local policy_idx policy_path
            policy_idx="$(nested_policy_index_for_path "$abs_path" "under" 2>/dev/null || true)"
            policy_path="${GITER_NESTED_POLICY_PATHS[$policy_idx]:-}"
            if [[ -n "$policy_path" && "$abs_path" != "$policy_path" ]]; then
                rel_path="${abs_path#"$(pwd)"/}"
                [[ "$rel_path" == "$abs_path" ]] && rel_path="$abs_path"
                log_skip_once "submodule-content:$policy_path" "按 submodule 策略跳过子模块内部文件: $rel_path"
                return 0
            fi
            ;;
        vendor-copy|vendor-copy-push|subtree) ;;
    esac

    for skip_path in "${GITER_EXCLUDE_DIRS[@]+"${GITER_EXCLUDE_DIRS[@]}"}"; do
        [[ -n "$skip_path" ]] || continue
        # 内联 path_is_same_or_under: 纯字符串比较，零 fork
        local _r="${skip_path%/}"
        [[ -n "$_r" && ( "$abs_path" == "$_r" || "$abs_path" == "$_r"/* ) ]] || continue
        rel_path="${skip_path#"$(pwd)"/}"
        [[ "$rel_path" == "$skip_path" ]] && rel_path="$skip_path"
        log_skip_once "exclude:$skip_path" "跳过配置排除路径: $rel_path"
        return 0
    done

    for skip_path in "${GITER_NESTED_REPO_DIRS[@]+"${GITER_NESTED_REPO_DIRS[@]}"}"; do
        [[ -n "$skip_path" ]] || continue
        local _r="${skip_path%/}"
        [[ -n "$_r" && ( "$abs_path" == "$_r" || "$abs_path" == "$_r"/* ) ]] || continue
        strategy="$(nested_policy_strategy_for_path "$skip_path" "exact" 2>/dev/null || true)"
        case "$strategy" in
            vendor-copy|vendor-copy-push|subtree) continue ;;
            submodule)
                if [[ "$abs_path" == "$_r" ]]; then
                    return 1
                fi
                ;;
        esac
        rel_path="${skip_path#"$(pwd)"/}"
        [[ "$rel_path" == "$skip_path" ]] && rel_path="$skip_path"
        log_skip_once "nested:$skip_path" "跳过嵌套 Git 仓库: $rel_path"
        return 0
    done

    if path_matches_sensitive_pattern "$path"; then
        log_warn "跳过敏感路径: $path"
        return 0
    fi

    if ! is_tracked_in_current_repo "$path" && is_ignored_in_current_repo "$path"; then
        local ignore_key
        ignore_key="$(ignored_path_key "$path")"
        log_skip_once "ignored:$ignore_key" "跳过父仓库忽略路径: ${ignore_key:-$path}" "info"
        return 0
    fi

    if [[ -f "$path" ]]; then
        max_bytes=$((MAX_STAGE_FILE_MB * 1024 * 1024))
        size="$(file_size_bytes "$path")"
        if [[ "$size" =~ ^[0-9]+$ && "$size" -gt "$max_bytes" ]]; then
            log_warn "跳过大文件: $path ($(awk -v b="$size" 'BEGIN {printf "%.1f MiB", b/1048576}'))"
            return 0
        fi
    fi

    return 1
}

stage_one_path() {
    local path="$1"
    local abs_path strategy
    [[ -n "$path" ]] || return 0

    if [[ "$path" == /* ]]; then
        abs_path="$path"
    else
        abs_path="$(pwd)/${path#./}"
    fi
    abs_path="$(trim_trailing_slash "$abs_path")"
    strategy="$(nested_policy_strategy_for_path "$abs_path" "exact" 2>/dev/null || true)"
    if [[ "$strategy" == "submodule" ]]; then
        if [[ "$DRY_RUN" -eq 1 ]]; then
            log_info "[dry-run] 将暂存 submodule 指针: $path"
        else
            git add -- "$path"
        fi
        return 0
    fi

    should_skip_stage_path "$path" && return 0

    if [[ -d "$path" ]]; then
        stage_directory_safe "$path"
        return 0
    fi

    if [[ "$DRY_RUN" -eq 1 ]]; then
        log_info "[dry-run] 将暂存: $path"
        return 0
    fi

    git add -- "$path"
}

normalize_commit_paths_for_repo() {
    local repo_root="$1"
    local normalized=()
    local path abs rel

    [[ ${#COMMIT_PATHS[@]} -gt 0 ]] || return 0

    for path in "${COMMIT_PATHS[@]+"${COMMIT_PATHS[@]}"}"; do
        if [[ "$path" == /* ]]; then
            abs="$path"
        else
            abs="$ORIGINAL_PWD/$path"
        fi

        if rel=$(git -C "$repo_root" ls-files --full-name -- "$abs" 2>/dev/null | head -1); [[ -n "$rel" ]]; then
            normalized+=("$rel")
            continue
        fi

        case "$abs" in
            "$repo_root"/*) normalized+=("${abs#"$repo_root"/}") ;;
            "$repo_root") normalized+=(".") ;;
            *)
                log_warn "指定路径不在仓库内，跳过: $path"
                ;;
        esac
    done

    COMMIT_PATHS=("${normalized[@]}")
}

stage_directory_safe() {
    local dir="$1"
    local file
    local batch=()

    dir="$(trim_trailing_slash "$dir")"

    while IFS= read -r -d '' file; do
        should_skip_stage_path "$file" && continue
        batch+=("$file")
        if [[ ${#batch[@]} -ge 200 ]]; then
            if [[ "$DRY_RUN" -eq 1 ]]; then
                log_info "[dry-run] 将暂存 ${#batch[@]} 个文件"
            else
                git add -- "${batch[@]}"
            fi
            batch=()
        fi
    done < <(git ls-files -z --modified --deleted --others --exclude-standard -- "$dir")

    if [[ ${#batch[@]} -gt 0 ]]; then
        if [[ "$DRY_RUN" -eq 1 ]]; then
            log_info "[dry-run] 将暂存 ${#batch[@]} 个文件"
        else
            git add -- "${batch[@]}"
        fi
    fi
}

stage_safe_changes() {
    local stage_rc=0

    if [[ "${GITER_ADD_ALL:-0}" == "1" ]]; then
        if has_vendor_copy_policies_for_repo "$(pwd)"; then
            log_warn "检测到 vendor-copy 策略，忽略 GITER_ADD_ALL=1，改用安全暂存。"
        else
            log_warn "GITER_ADD_ALL=1，使用 git add -A"
            if [[ "$DRY_RUN" -eq 1 ]]; then
                log_info "[dry-run] 将执行: git add -A"
                return 0
            fi
            git add -A
            return 0
        fi
    fi

    prepare_vendor_copy_index_paths "$(pwd)" || return 1
    hide_vendor_copy_git_dirs "$(pwd)" || return 1
    trap 'restore_vendor_copy_git_dirs || true; trap - RETURN' RETURN

    if [[ ${#COMMIT_PATHS[@]} -gt 0 ]]; then
        local path
        for path in "${COMMIT_PATHS[@]+"${COMMIT_PATHS[@]}"}"; do
            stage_one_path "$path"
        done
        restore_vendor_copy_git_dirs || true
        trap - RETURN
        return 0
    fi

    local entry status path old_path new_path
    local batch=()
    while IFS= read -r -d '' entry; do
        [[ -n "$entry" ]] || continue
        status="${entry:0:2}"
        path="${entry:3}"

        if [[ "$status" == R* || "$status" == C* ]]; then
            old_path="$path"
            if IFS= read -r -d '' new_path; then
                if should_skip_stage_path "$old_path" || should_skip_stage_path "$new_path"; then
                    continue
                fi
                if [[ "$DRY_RUN" -eq 1 ]]; then
                    log_info "[dry-run] 将暂存重命名/复制: $old_path -> $new_path"
                    continue
                fi
                git add -A -- "$old_path" "$new_path"
            fi
            continue
        fi

        [[ -n "$path" ]] || continue

        # 只存在于暂存区的变更不需要再次 git add。典型场景是
        # "D  file"：文件已经 staged delete，工作区路径不存在，
        # 再执行 git add -- file 会报 pathspec 未匹配。
        if [[ "${status:0:1}" != " " && "${status:1:1}" == " " ]]; then
            continue
        fi

        local abs_status_path status_strategy
        if [[ "$path" == /* ]]; then
            abs_status_path="$path"
        else
            abs_status_path="$(pwd)/${path#./}"
        fi
        abs_status_path="$(trim_trailing_slash "$abs_status_path")"
        status_strategy="$(nested_policy_strategy_for_path "$abs_status_path" "exact" 2>/dev/null || true)"
        if [[ "$status_strategy" == "submodule" ]]; then
            if [[ "$DRY_RUN" -eq 1 ]]; then
                log_info "[dry-run] 将暂存 submodule 指针: $path"
            else
                git add -- "$path"
            fi
            continue
        fi

        should_skip_stage_path "$path" && continue

        if [[ "${status:1:1}" == "D" ]]; then
            if [[ "$DRY_RUN" -eq 1 ]]; then
                log_info "[dry-run] 将暂存删除: $path"
            else
                git add -A -- "$path"
            fi
            continue
        fi

        if [[ -d "$path" ]]; then
            stage_directory_safe "$path"
            continue
        fi

        batch+=("$path")
        if [[ ${#batch[@]} -ge 200 ]]; then
            if [[ "$DRY_RUN" -eq 1 ]]; then
                log_info "[dry-run] 将暂存 ${#batch[@]} 个文件"
            else
                git add -- "${batch[@]}"
            fi
            batch=()
        fi
    done < <(git status --porcelain -z) || stage_rc=$?

    if [[ ${#batch[@]} -gt 0 ]]; then
        if [[ "$DRY_RUN" -eq 1 ]]; then
            log_info "[dry-run] 将暂存 ${#batch[@]} 个文件"
        else
            git add -- "${batch[@]}"
        fi
    fi

    restore_vendor_copy_git_dirs || true
    trap - RETURN
    return "$stage_rc"
}

validate_cached_changes() {
    local file failed=0

    while IFS= read -r file; do
        [[ -n "$file" ]] || continue
        if should_skip_stage_path "$file"; then
            log_error "已暂存的变更包含禁止提交路径: $file"
            failed=1
        fi
    done < <(git diff --cached --name-only --diff-filter=ACMR)

    if [[ "$failed" -eq 1 ]]; then
        log_error "请先取消暂存上述文件: git restore --staged <path>"
        return 1
    fi
}

run_precommit_checks() {
    log_verbose "执行提交前检查..."
    validate_cached_changes
    if [[ "$VERBOSE" -eq 1 ]]; then
        git diff --check --cached
    else
        git diff --check --cached >/dev/null 2>&1
    fi

    local file
    while IFS= read -r file; do
        [[ -f "$file" ]] || continue
        LC_ALL=C LANG=C bash -n "$file"
    done < <(git diff --cached --name-only -- '*.sh' '*.bash')
}

discover_git_repos() {
    local base_dir="$1"
    local depth="$2"
    local repos=()
    local temp_file repo_root search_dir current_dir parent_dir repo_dir already_exists subrepo

    [[ -d "$base_dir" ]] || return 0

    search_dir="$base_dir"
    if is_git_repo "$base_dir"; then
        repo_root="$(git_root_for "$base_dir")"
        repos+=("$repo_root")
        search_dir="$repo_root"
    fi

    if [ "$depth" -ge 2 ]; then
        temp_file="$(mktemp)"
        find "$search_dir" -maxdepth 3 -name ".git" ! -path "*/.git/*" -print0 > "$temp_file" 2>/dev/null
        while IFS= read -r -d '' subrepo; do
            repo_dir="$(git_root_for "$(dirname "$subrepo")" 2>/dev/null || dirname "$subrepo")"
            repos+=("$repo_dir")
        done < "$temp_file"
        rm -f "$temp_file"
    fi

    if [ "$depth" -ge 1 ]; then
        current_dir="$search_dir"
        parent_dir="$(dirname "$current_dir")"
        while [ "$parent_dir" != "$current_dir" ]; do
            if [ -e "$parent_dir/.git" ]; then
                already_exists=0
                for repo_dir in "${repos[@]}"; do
                    if [ "$repo_dir" = "$parent_dir" ]; then
                        already_exists=1
                        break
                    fi
                done
                [ "$already_exists" -eq 1 ] || repos+=("$parent_dir")
            fi
            current_dir="$parent_dir"
            parent_dir="$(dirname "$current_dir")"
        done
    fi

    if [ ${#repos[@]} -gt 0 ]; then
        printf '%s\n' "${repos[@]}" | sort -u
    fi
}

add_auto_repo_candidate() {
    local repo_root="$1"
    local label="$2"
    local existing

    [[ -n "$repo_root" ]] || return 0
    for existing in "${AUTO_REPOS[@]+"${AUTO_REPOS[@]}"}"; do
        [[ "$existing" == "$repo_root" ]] && return 0
    done

    AUTO_REPOS+=("$repo_root")
    AUTO_REPO_LABELS+=("$label")
}

repo_in_configured_paths() {
    local repo_root="$1"
    local arr_name="$2"
    local configured

    # 安全校验：变量名只允许字母、数字、下划线
    [[ "$arr_name" =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ ]] || return 1

    local configured_paths=()
    eval "configured_paths=(\"\${${arr_name}[@]+\${${arr_name}[@]}}\")"

    repo_root="${repo_root%/}"
    for configured in "${configured_paths[@]+"${configured_paths[@]}"}"; do
        [[ -n "$configured" ]] || continue
        configured="${configured%/}"
        if [[ "$repo_root" == "$configured" ]]; then
            return 0
        fi
    done

    return 1
}

should_include_discovered_repo() {
    local repo_root="$1"
    local base_root="$2"
    local strategy

    [[ -n "$repo_root" ]] || return 1
    [[ "$repo_root" == "$base_root" ]] && return 0

    if [[ -n "$base_root" ]] && ! nested_repo_selected_by_cli "$base_root" "$repo_root"; then
        log_debug "按本次嵌套仓库选择跳过发现仓库: $repo_root"
        return 1
    fi

    if repo_in_configured_paths "$repo_root" GITER_EXCLUDE_DIRS; then
        log_debug "跳过配置排除的嵌套仓库候选: $repo_root"
        return 1
    fi

    strategy="$(nested_policy_strategy_for_path "$repo_root" "exact" 2>/dev/null || true)"
    case "$strategy" in
        reference|submodule|vendor-copy|subtree)
            log_debug "按 $strategy 策略跳过嵌套仓库候选: $repo_root"
            return 1
            ;;
        independent-push|vendor-copy-push|update-only)
            return 0
            ;;
    esac

    if [[ "$DISCOVER_ALL_NESTED" -eq 1 ]]; then
        return 0
    fi

    if repo_in_configured_paths "$repo_root" GITER_PUSH_DIRS; then
        return 0
    fi

    log_debug "跳过未声明为推送目录的嵌套仓库候选: $repo_root"
    return 1
}

add_configured_push_repo_candidates() {
    local label="$1"
    local base_root="${2:-}"
    local push_dir push_repo i

    for push_dir in "${GITER_PUSH_DIRS[@]+"${GITER_PUSH_DIRS[@]}"}"; do
        [[ -n "$push_dir" ]] || continue
        if push_repo="$(git_root_for "$push_dir" 2>/dev/null)"; then
            [[ -n "$base_root" && "$push_repo" == "$base_root" ]] && {
                log_debug "配置的推送目录未形成独立仓库，跳过候选: $push_dir"
                continue
            }
            if [[ -n "$base_root" ]] && ! nested_repo_selected_by_cli "$base_root" "$push_repo"; then
                log_debug "按本次嵌套仓库选择跳过候选: $push_repo"
                continue
            fi
            add_auto_repo_candidate "$push_repo" "$label"
        else
            log_debug "配置的推送目录不是 Git 仓库，跳过候选: $push_dir"
        fi
    done

    for i in "${!GITER_NESTED_POLICY_PATHS[@]}"; do
        case "${GITER_NESTED_POLICY_STRATEGIES[$i]}" in
            independent-push|vendor-copy-push|update-only) ;;
            *) continue ;;
        esac
        push_dir="${GITER_NESTED_POLICY_PATHS[$i]}"
        if push_repo="$(git_root_for "$push_dir" 2>/dev/null)"; then
            [[ -n "$base_root" && "$push_repo" == "$base_root" ]] && {
                log_debug "${GITER_NESTED_POLICY_STRATEGIES[$i]} 策略目录未形成独立仓库，跳过候选: $push_dir"
                continue
            }
            if [[ -n "$base_root" ]] && ! nested_repo_selected_by_cli "$base_root" "$push_repo"; then
                log_debug "按本次嵌套仓库选择跳过候选: $push_repo"
                continue
            fi
            add_auto_repo_candidate "$push_repo" "$label"
        else
            log_debug "${GITER_NESTED_POLICY_STRATEGIES[$i]} 策略目录不是 Git 仓库，跳过候选: $push_dir"
        fi
    done
}

add_auto_repo_context() {
    local base_dir="$1"
    local root_label="$2"
    local discover_label="$3"
    local repo_root base_root

    [[ -d "$base_dir" ]] || {
        log_debug "候选目录不存在，跳过: $base_dir"
        return 0
    }

    if repo_root="$(git_root_for "$base_dir" 2>/dev/null)"; then
        base_root="$repo_root"
        load_repo_config "$base_root" || return 1
        add_configured_push_repo_candidates "${root_label/仓库/推送目录}" "$base_root"
        add_auto_repo_candidate "$repo_root" "$root_label"
    else
        base_root="$base_dir"
    fi

    while IFS= read -r repo_root; do
        [[ -n "$repo_root" ]] || continue
        should_include_discovered_repo "$repo_root" "$base_root" || continue
        add_auto_repo_candidate "$repo_root" "$discover_label"
    done < <(discover_git_repos "$base_dir" "$DISCOVER_DEPTH")
}

build_auto_repo_candidates() {
    local before_count
    local explicit_candidate_count=0

    AUTO_REPOS=()
    AUTO_REPO_LABELS=()

    if [[ "$TARGET_DIR_EXPLICIT" -eq 1 ]]; then
        before_count="${#AUTO_REPOS[@]}"
        add_auto_repo_context "$TARGET_DIR" "指定目录仓库" "指定目录发现"
        explicit_candidate_count="${#AUTO_REPOS[@]}"
        explicit_candidate_count=$((explicit_candidate_count - before_count))
    fi

    # 交互终端下额外纳入执行目录和脚本所属仓库；显式 -d 时指定目录始终排在前面。
    if [[ -t 0 && ( "$TARGET_DIR_EXPLICIT" -eq 0 || "$explicit_candidate_count" -gt 0 ) ]]; then
        add_auto_repo_context "$ORIGINAL_PWD" "执行目录仓库" "执行目录发现"
        add_auto_repo_context "$SCRIPT_DIR" "脚本所属仓库" "脚本目录发现"
    fi

    if [[ "$TARGET_DIR_EXPLICIT" -eq 0 && ! -t 0 ]]; then
        add_auto_repo_context "$ORIGINAL_PWD" "执行目录仓库" "执行目录发现"
        add_auto_repo_context "$SCRIPT_DIR" "脚本所属仓库" "脚本目录发现"
    fi
}

print_repo_config_report() {
    local repo_root config_file item strategy rel_path i current_gitignore_hash hash_status

    repo_root="$(git_root_for "$1" 2>/dev/null || true)"
    if [[ -z "$repo_root" ]]; then
        log_warn "不是 Git 仓库，无法显示配置: $1"
        return 0
    fi

    load_repo_config "$repo_root" || return 1
    detect_nested_git_repos "$repo_root"
    warn_nested_policy_mismatches "$repo_root"
    config_file="$(repo_config_path "$repo_root")" || return 1
    current_gitignore_hash="$(gitignore_hash_for_repo "$repo_root" 2>/dev/null || echo "")"
    hash_status="未缓存"
    if [[ -n "${GITER_GITIGNORE_HASH:-}" ]]; then
        if [[ "$current_gitignore_hash" == "$GITER_GITIGNORE_HASH" ]]; then
            hash_status="一致"
        else
            hash_status="已变化"
        fi
    fi

    echo "仓库: $repo_root"
    echo "配置文件: $config_file"
    echo "自动维护 .gitignore: $GITER_AUTO_UPDATE_GITIGNORE"
    echo ".gitignore 哈希: 当前=${current_gitignore_hash:-未知} 缓存=${GITER_GITIGNORE_HASH:-无} 状态=$hash_status"
    echo "推送目录 GITER_PUSH_DIRS:"
    for item in "${GITER_PUSH_DIRS[@]+"${GITER_PUSH_DIRS[@]}"}"; do
        echo "  - $item"
    done
    echo "排除目录 GITER_EXCLUDE_DIRS:"
    for item in "${GITER_EXCLUDE_DIRS[@]+"${GITER_EXCLUDE_DIRS[@]}"}"; do
        echo "  - $item"
    done
    echo "嵌套仓库策略 GITER_NESTED_POLICIES:"
    for i in "${!GITER_NESTED_POLICY_PATHS[@]}"; do
        rel_path="$(repo_relative_path "$repo_root" "${GITER_NESTED_POLICY_PATHS[$i]}" 2>/dev/null || echo "${GITER_NESTED_POLICY_PATHS[$i]}")"
        strategy="${GITER_NESTED_POLICY_STRATEGIES[$i]}"
        [[ "$strategy" == "subtree" ]] && strategy="subtree(legacy -> vendor-copy)"
        printf '  - %s | %s' "$rel_path" "$strategy"
        [[ -n "${GITER_NESTED_POLICY_REMOTES[$i]:-}" ]] && printf ' | remote=%s' "${GITER_NESTED_POLICY_REMOTES[$i]}"
        [[ -n "${GITER_NESTED_POLICY_BRANCHES[$i]:-}" ]] && printf ' | branch=%s' "${GITER_NESTED_POLICY_BRANCHES[$i]}"
        printf '\n'
    done
    echo "嵌套 Git 仓库:"
    for item in "${GITER_NESTED_REPO_DIRS[@]+"${GITER_NESTED_REPO_DIRS[@]}"}"; do
        rel_path="$(repo_relative_path "$repo_root" "$item" 2>/dev/null || echo "$item")"
        strategy="$(nested_policy_strategy_for_path "$item" "exact" 2>/dev/null || true)"
        if [[ -z "$strategy" ]]; then
            strategy="$(nested_policy_strategy_for_path "$item" "under" 2>/dev/null || true)"
            [[ -n "$strategy" ]] && strategy="${strategy}, inherited"
        fi
        if [[ -z "$strategy" ]] && is_submodule_path "$repo_root" "$rel_path"; then
            strategy="submodule(推断)"
        fi
        echo "  - $rel_path${strategy:+ [$strategy]}"
    done
    echo "父仓库 .gitignore 维护项:"
    while IFS= read -r item; do
        [[ -n "$item" ]] || continue
        echo "  - $item"
    done < <(collect_parent_ignore_paths "$repo_root")
}

show_config_and_candidates() {
    local idx repo

    build_auto_repo_candidates

    echo "候选仓库:"
    for idx in "${!AUTO_REPOS[@]}"; do
        printf '  %d) %s: %s\n' "$((idx + 1))" "${AUTO_REPO_LABELS[$idx]}" "${AUTO_REPOS[$idx]}"
    done

    if [[ ${#AUTO_REPOS[@]} -eq 0 ]]; then
        echo "  (无)"
    fi

    for repo in "${AUTO_REPOS[@]+"${AUTO_REPOS[@]}"}"; do
        echo
        print_repo_config_report "$repo"
    done
}

select_auto_repos() {
    local count="${#AUTO_REPOS[@]}"
    local choice selected idx repo existing add_unique_selected
    local selected_repos=()

    [[ "$count" -gt 0 ]] || return 0

    if [[ "$count" -eq 1 || ! -t 0 ]]; then
        printf '%s\n' "${AUTO_REPOS[@]}"
        return 0
    fi

    echo "发现多个 Git 仓库，请选择要处理的仓库:" >&2
    for idx in "${!AUTO_REPOS[@]}"; do
        printf '  %d) %s: %s\n' "$((idx + 1))" "${AUTO_REPO_LABELS[$idx]}" "${AUTO_REPOS[$idx]}" >&2
    done
    echo "  a) 全部（按上方顺序执行）" >&2
    read_input choice "请选择 [默认 1，多个用空格，如: 1 2]: "
    choice="${choice:-1}"

    case "$choice" in
        a|A|all|ALL|全部)
            printf '%s\n' "${AUTO_REPOS[@]}"
            return 0
            ;;
    esac

    for selected in $choice; do
        if [[ "$selected" =~ ^[0-9]+$ ]]; then
            idx=$((selected - 1))
            if [[ "$idx" -ge 0 && "$idx" -lt "$count" ]]; then
                repo="${AUTO_REPOS[$idx]}"
                add_unique_selected=1
                for existing in "${selected_repos[@]+"${selected_repos[@]}"}"; do
                    [[ "$existing" == "$repo" ]] && add_unique_selected=0
                done
                [[ "$add_unique_selected" -eq 1 ]] && selected_repos+=("$repo")
            else
                log_warn "无效仓库编号，已忽略: $selected"
            fi
        else
            log_warn "无效选择，已忽略: $selected"
        fi
    done

    if [[ ${#selected_repos[@]} -eq 0 ]]; then
        log_warn "未选择有效仓库，默认处理第 1 个仓库。"
        selected_repos+=("${AUTO_REPOS[0]}")
    fi

    printf '%s\n' "${selected_repos[@]}"
}

get_current_branch() {
    git symbolic-ref --short -q HEAD 2>/dev/null || true
}

current_upstream() {
    local branch="${1:-main}"
    local upstream
    upstream=$(git rev-parse --abbrev-ref --symbolic-full-name "${branch}@{u}" 2>/dev/null || true)
    if [[ -n "$upstream" ]]; then
        printf '%s\n' "$upstream"
        return 0
    fi
    if git rev-parse --verify --quiet "origin/${branch}" >/dev/null; then
        printf '%s\n' "origin/${branch}"
        return 0
    fi
    return 1
}

list_local_branches() {
    git for-each-ref --format='%(refname:short)' refs/heads 2>/dev/null
}

list_remote_branches() {
    local full_ref short_ref

    while read -r full_ref short_ref; do
        [[ -n "$full_ref" && -n "$short_ref" ]] || continue
        [[ "$full_ref" == */HEAD ]] && continue
        [[ "$short_ref" == */* ]] || continue
        printf '%s\n' "$short_ref"
    done < <(git for-each-ref --format='%(refname) %(refname:short)' refs/remotes 2>/dev/null)
}

reset_remote_fetch_state() {
    GITER_FETCHED_REMOTES=()
    GITER_FETCH_FAILED_REMOTES=()
}

ensure_remote_fetch_all() {
    local remote="$1"
    local fetchspec="+refs/heads/*:refs/remotes/${remote}/*"

    [[ -n "$remote" ]] || return 1
    if ! git config --get-all "remote.${remote}.fetch" | grep -Fxq -- "$fetchspec"; then
        git config --add "remote.${remote}.fetch" "$fetchspec"
    fi
}

refresh_remote_branch_cache() {
    local remote
    local pids=()
    local pid_remotes=()
    local idx pid status

    reset_remote_fetch_state
    [[ "$DRY_RUN" -eq 0 ]] || return 0

    while IFS= read -r remote; do
        [[ -n "$remote" ]] || continue
        ensure_remote_fetch_all "$remote" || true
        log_verbose "正在刷新远程分支缓存: $remote"
        run_git_network_command_with_timeout "$GITER_BRANCH_CACHE_TIMEOUT" fetch --prune "$remote" >/dev/null 2>&1 &
        pids+=($!)
        pid_remotes+=("$remote")
    done < <(git remote 2>/dev/null)

    for idx in "${!pids[@]}"; do
        pid="${pids[$idx]}"
        status=0
        wait "$pid" 2>/dev/null || status=$?
        if [[ "$status" -eq 124 ]]; then
            GITER_FETCH_FAILED_REMOTES+=("${pid_remotes[$idx]}")
            log_verbose "刷新远程分支缓存未在 ${GITER_BRANCH_CACHE_TIMEOUT}s 内完成，继续使用本地缓存: ${pid_remotes[$idx]}（同步阶段会重试）"
        elif [[ "$status" -ne 0 ]]; then
            GITER_FETCH_FAILED_REMOTES+=("${pid_remotes[$idx]}")
            log_verbose "刷新远程分支缓存失败，继续使用本地缓存: ${pid_remotes[$idx]}（同步阶段会重试）"
        else
            GITER_FETCHED_REMOTES+=("${pid_remotes[$idx]}")
        fi
    done
}

should_refresh_branch_cache_for_selection() {
    local current_branch="$1"
    local local_count remote_count

    case "$GITER_REFRESH_BRANCH_CACHE" in
        1|true|yes|always) return 0 ;;
        0|false|no|never) return 1 ;;
    esac

    [[ -n "$TARGET_BRANCH" ]] && return 0
    [[ -t 0 ]] || return 0

    local_count="$(list_local_branches | wc -l | tr -d ' ')"
    remote_count="$(list_remote_branches | wc -l | tr -d ' ')"

    if [[ "$local_count" -le 1 && "$remote_count" -le 1 && -n "$current_branch" ]]; then
        if current_upstream "$current_branch" >/dev/null 2>&1; then
            return 1
        fi
    fi

    return 0
}

branch_exists() {
    local branch="$1"
    [[ -n "$branch" ]] || return 1
    git show-ref --verify --quiet "refs/heads/$branch"
}

remote_branch_exists() {
    local remote_branch="$1"
    [[ -n "$remote_branch" ]] || return 1
    git show-ref --verify --quiet "refs/remotes/$remote_branch"
}

remote_branch_to_local() {
    local remote_branch="$1"
    printf '%s\n' "${remote_branch#*/}"
}

configure_branch_upstream() {
    local branch="$1"
    local remote_ref="$2"
    local remote="${remote_ref%%/*}"
    local remote_branch="${remote_ref#*/}"

    [[ -n "$branch" && -n "$remote" && "$remote" != "$remote_ref" && -n "$remote_branch" ]] || return 1
    ensure_remote_fetch_all "$remote" || true
    git config "branch.${branch}.remote" "$remote"
    git config "branch.${branch}.merge" "refs/heads/${remote_branch}"
}

set_branch_selection() {
    SELECTED_BRANCH="$1"
    SELECTED_REMOTE_REF="${2:-}"
}

resolve_branch_candidate() {
    local requested="$1"
    local remote_branch

    [[ -n "$requested" ]] || return 1

    if branch_exists "$requested"; then
        set_branch_selection "$requested" ""
        return 0
    fi

    if remote_branch_exists "$requested"; then
        set_branch_selection "$(remote_branch_to_local "$requested")" "$requested"
        return 0
    fi

    remote_branch="origin/${requested}"
    if remote_branch_exists "$remote_branch"; then
        set_branch_selection "$requested" "$remote_branch"
        return 0
    fi

    return 1
}

print_branch_candidates() {
    local current_branch="$1"
    local idx="$2"
    local display="$3"
    local local_branch="$4"
    local type="$5"
    local marker=" "

    [[ -n "$current_branch" && "$local_branch" == "$current_branch" ]] && marker="*"
    if [[ "$type" == "remote" ]]; then
        printf '  %d) %s [远程] %s -> %s\n' "$idx" "$marker" "$display" "$local_branch" >&2
    else
        printf '  %d) %s [本地] %s\n' "$idx" "$marker" "$display" >&2
    fi
}

select_branch_for_repo() {
    local repo_root="$1"
    local current_branch="$2"
    local branch remote_ref choice idx selected default_branch default_display
    local display local_branch type
    local candidate_displays=()
    local candidate_locals=()
    local candidate_remotes=()
    local candidate_types=()
    local unique_locals=()
    local seen_local unique_branch local_candidate_count remote_candidate_count first_remote_candidate

    SELECTED_BRANCH=""
    SELECTED_REMOTE_REF=""
    if should_refresh_branch_cache_for_selection "$current_branch"; then
        refresh_remote_branch_cache
    else
        reset_remote_fetch_state
        log_debug "跳过远程分支缓存刷新。"
    fi

    while IFS= read -r branch; do
        [[ -n "$branch" ]] || continue
        candidate_displays+=("$branch")
        candidate_locals+=("$branch")
        candidate_remotes+=("")
        candidate_types+=("local")
    done < <(list_local_branches)

    while IFS= read -r remote_ref; do
        [[ -n "$remote_ref" ]] || continue
        candidate_displays+=("$remote_ref")
        candidate_locals+=("$(remote_branch_to_local "$remote_ref")")
        candidate_remotes+=("$remote_ref")
        candidate_types+=("remote")
    done < <(list_remote_branches)

    if [[ ${#candidate_displays[@]} -eq 0 ]]; then
        if [[ -n "$current_branch" ]]; then
            candidate_displays+=("$current_branch")
            candidate_locals+=("$current_branch")
            candidate_remotes+=("")
            candidate_types+=("local")
        else
            log_warn "[${repo_root}] 未发现本地或远程分支，跳过。"
            return 1
        fi
    fi

    for local_branch in "${candidate_locals[@]}"; do
        seen_local=0
        for branch in "${unique_locals[@]+"${unique_locals[@]}"}"; do
            [[ "$branch" == "$local_branch" ]] && seen_local=1
        done
        [[ "$seen_local" -eq 0 ]] && unique_locals+=("$local_branch")
    done

    if [[ -n "$TARGET_BRANCH" ]]; then
        resolve_branch_candidate "$TARGET_BRANCH" && return 0
        log_error "[${repo_root}] 不存在本地或远程分支: ${TARGET_BRANCH}"
        return 1
    fi

    if [[ -n "$current_branch" ]]; then
        default_branch="$current_branch"
        default_display="$current_branch"
    else
        default_branch="${candidate_locals[0]}"
        default_display="${candidate_displays[0]}"
    fi

    if [[ ! -t 0 ]]; then
        set_branch_selection "$default_branch" ""
        return 0
    fi

    if [[ ${#unique_locals[@]} -eq 1 ]]; then
        unique_branch="${unique_locals[0]}"
        local_candidate_count=0
        remote_candidate_count=0
        first_remote_candidate=""

        for idx in "${!candidate_locals[@]}"; do
            [[ "${candidate_locals[$idx]}" == "$unique_branch" ]] || continue
            if [[ "${candidate_types[$idx]}" == "local" ]]; then
                local_candidate_count=$((local_candidate_count + 1))
            elif [[ -n "${candidate_remotes[$idx]}" ]]; then
                remote_candidate_count=$((remote_candidate_count + 1))
                [[ -n "$first_remote_candidate" ]] || first_remote_candidate="${candidate_remotes[$idx]}"
            fi
        done

        if [[ "$local_candidate_count" -gt 0 || "$remote_candidate_count" -eq 1 ]]; then
            if [[ "$local_candidate_count" -gt 0 ]]; then
                set_branch_selection "$unique_branch" ""
            else
                set_branch_selection "$unique_branch" "$first_remote_candidate"
            fi
            log_verbose "仅发现一个分支 ${unique_branch}，自动选择。"
            return 0
        fi
    fi

    echo "分支列表（本地 + 远程）: $repo_root" >&2
    for idx in "${!candidate_displays[@]}"; do
        print_branch_candidates \
            "$current_branch" \
            "$((idx + 1))" \
            "${candidate_displays[$idx]}" \
            "${candidate_locals[$idx]}" \
            "${candidate_types[$idx]}"
    done

    read_input choice "请选择要操作的分支 [默认 ${default_display}]: "
    choice="${choice:-$default_display}"

    if [[ "$choice" =~ ^[0-9]+$ ]]; then
        idx=$((choice - 1))
        if [[ "$idx" -lt 0 || "$idx" -ge "${#candidate_displays[@]}" ]]; then
            log_error "无效分支编号: $choice"
            return 1
        fi
        set_branch_selection "${candidate_locals[$idx]}" "${candidate_remotes[$idx]}"
        return 0
    else
        selected="$choice"
    fi

    for idx in "${!candidate_displays[@]}"; do
        display="${candidate_displays[$idx]}"
        local_branch="${candidate_locals[$idx]}"
        remote_ref="${candidate_remotes[$idx]}"
        if [[ "$selected" == "$display" || "$selected" == "$local_branch" || "$selected" == "$remote_ref" ]]; then
            set_branch_selection "$local_branch" "$remote_ref"
            return 0
        fi
    done

    log_error "[${repo_root}] 不存在本地或远程分支: ${selected}"
    return 1
}

switch_git_branch() {
    local branch="$1"
    local remote_ref="${2:-}"
    local remote_branch

    if branch_exists "$branch"; then
        git switch "$branch"
        return $?
    fi

    if [[ -n "$remote_ref" && ! "$remote_ref" =~ /HEAD$ ]] && remote_branch_exists "$remote_ref"; then
        log_info "创建本地跟踪分支: ${branch} -> ${remote_ref}"
        git switch -c "$branch" "$remote_ref" && configure_branch_upstream "$branch" "$remote_ref"
        return $?
    fi

    remote_branch="origin/${branch}"
    if remote_branch_exists "$remote_branch"; then
        log_info "创建本地跟踪分支: ${branch} -> ${remote_branch}"
        git switch -c "$branch" "$remote_branch" && configure_branch_upstream "$branch" "$remote_branch"
        return $?
    fi

    log_warn "git switch 失败，尝试使用 git checkout。"
    git checkout "$branch"
}

checkout_branch_if_needed() {
    local repo_root="$1"
    local original_branch="$2"
    local target_branch="$3"
    local target_remote_ref="${4:-}"

    [[ -n "$target_branch" ]] || return 1
    if [[ "$original_branch" == "$target_branch" ]]; then
        return 0
    fi

    if [[ "$DRY_RUN" -eq 1 ]]; then
        log_info "[dry-run] 将切换分支: ${original_branch:-detached HEAD} -> ${target_branch}"
        return 0
    fi

    log_info "切换分支: ${original_branch:-detached HEAD} -> ${target_branch}"
    (
        cd "$repo_root" || exit 1
        switch_git_branch "$target_branch" "$target_remote_ref"
    )
}

maybe_return_to_original_branch() {
    local repo_root="$1"
    local original_branch="$2"
    local selected_branch="$3"
    local current_branch choice

    [[ -n "$original_branch" && -n "$selected_branch" ]] || return 0
    [[ "$original_branch" != "$selected_branch" ]] || return 0
    [[ "$DRY_RUN" -eq 0 ]] || return 0
    [[ -d "$repo_root" ]] || return 0

    (
        cd "$repo_root" || exit 0
        current_branch="$(get_current_branch)"
        [[ "$current_branch" == "$selected_branch" ]] || exit 0

        if [[ -t 0 ]]; then
            read_input choice "是否切回原分支 ${original_branch}? [Y/n]: "
            if [[ ! "${choice:-Y}" =~ ^[Yy]$ ]]; then
                log_info "保留在分支: ${selected_branch}"
                exit 0
            fi
        elif [[ "${GITER_RETURN_ORIGINAL_BRANCH:-0}" != "1" ]]; then
            log_info "保留在分支: ${selected_branch}（设置 GITER_RETURN_ORIGINAL_BRANCH=1 可在非交互模式自动切回 ${original_branch}）"
            exit 0
        fi

        log_info "切回原分支: ${original_branch}"
        switch_git_branch "$original_branch"
    )
}

set_fetch_error() {
    local status="$1"
    local remote="$2"
    local ref="$3"
    local output="${4:-}"
    local detail=""

    if [[ -n "$output" ]]; then
        detail="$(printf '%s\n' "$output" | sed '/^[[:space:]]*$/d' | tail -n 12)"
    fi

    if [[ -n "$detail" ]]; then
        GITER_LAST_FETCH_ERROR="git fetch ${remote} ${ref} 失败（退出码 ${status}）：\n${detail}"
    elif [[ "$status" -eq 124 ]]; then
        GITER_LAST_FETCH_ERROR="git fetch ${remote} ${ref} 超时（${GITER_GIT_NETWORK_TIMEOUT}s）"
    else
        GITER_LAST_FETCH_ERROR="git fetch ${remote} ${ref} 失败（退出码 ${status}），未返回错误输出"
    fi
}

fetch_upstream() {
    local upstream="$1"
    local remote ref r fr
    local already_fetched=0

    GITER_LAST_FETCH_ERROR=""
    remote="${upstream%%/*}"
    ref="${upstream#*/}"
    [[ -n "$remote" && -n "$ref" && "$remote" != "$ref" ]] || return 1

    # 如果该 remote 已在缓存刷新阶段成功 fetch，跳过重复请求
    for r in "${GITER_FETCHED_REMOTES[@]+"${GITER_FETCHED_REMOTES[@]}"}"; do
        if [[ "$r" == "$remote" ]]; then
            already_fetched=1
            break
        fi
    done
    if [[ "$already_fetched" -eq 1 ]]; then
        log_debug "远程 $remote 已在缓存刷新阶段 fetch，跳过重复请求。"
        return 0
    fi

    # 如果该 remote 在缓存刷新阶段超时/失败，用完整超时重试
    for r in "${GITER_FETCH_FAILED_REMOTES[@]+"${GITER_FETCH_FAILED_REMOTES[@]}"}"; do
        if [[ "$r" != "$remote" ]]; then
            continue
        fi

        log_verbose "重试获取远程 ${remote}（使用 ${GITER_GIT_NETWORK_TIMEOUT}s 超时）..."
        local status=0 output=""
        if output="$(run_git_network_command fetch "$remote" "$ref" 2>&1)"; then
            status=0
        else
            status=$?
        fi
        if [[ "$status" -eq 0 ]]; then
            GITER_FETCHED_REMOTES+=("$remote")
            local new_failed=()
            for fr in "${GITER_FETCH_FAILED_REMOTES[@]+"${GITER_FETCH_FAILED_REMOTES[@]}"}"; do
                [[ "$fr" != "$remote" ]] && new_failed+=("$fr")
            done
            GITER_FETCH_FAILED_REMOTES=("${new_failed[@]+"${new_failed[@]}"}")
        else
            set_fetch_error "$status" "$remote" "$ref" "$output"
        fi
        return "$status"
    done

    # 首次 fetch（缓存刷新阶段未涉及的 remote）
    local status=0 output=""
    if output="$(run_git_network_command fetch "$remote" "$ref" 2>&1)"; then
        return 0
    else
        status=$?
        set_fetch_error "$status" "$remote" "$ref" "$output"
        return "$status"
    fi
}

rebase_in_progress() {
    local git_dir
    git_dir="$(git rev-parse --git-dir 2>/dev/null || true)"
    [[ -n "$git_dir" ]] || return 1
    [[ -d "$git_dir/rebase-merge" || -d "$git_dir/rebase-apply" ]]
}

set_sync_failure() {
    GITER_SYNC_FAILURE_STATUS="${1:-失败}"
    GITER_SYNC_FAILURE_DETAIL="${2:-同步远端失败}"
}

clear_sync_failure() {
    GITER_SYNC_FAILURE_STATUS=""
    GITER_SYNC_FAILURE_DETAIL=""
}

print_rebase_resolution_commands() {
    local upstream="$1"
    local branch="${2:-$(get_current_branch)}"

    {
        echo
        echo "rebase 处理命令:"
        echo "  git status"
        echo "  git diff --name-only --diff-filter=U"
        echo "  git add <已解决文件>"
        echo "  git rebase --continue"
        echo
        echo "放弃本次 rebase:"
        echo "  git rebase --abort"
        echo
        echo "重新执行同步:"
        echo "  git fetch ${upstream%%/*} ${upstream#*/}"
        echo "  git rebase ${upstream}"
        [[ -n "$branch" ]] && echo "  git push ${upstream%%/*} ${branch}"
        echo
    } >&2
}

predict_rebase_conflict_risk() {
    local upstream="$1"
    local base local_files remote_files overlap

    base="$(git merge-base HEAD "$upstream" 2>/dev/null || true)"
    [[ -n "$base" ]] || return 0

    local_files="$(git diff --name-only "$base"..HEAD 2>/dev/null || true)"
    remote_files="$(git diff --name-only "$base".."$upstream" 2>/dev/null || true)"
    [[ -n "$local_files" && -n "$remote_files" ]] || return 0

    overlap="$(comm -12 \
        <(printf '%s\n' "$local_files" | sed '/^[[:space:]]*$/d' | LC_ALL=C sort -u) \
        <(printf '%s\n' "$remote_files" | sed '/^[[:space:]]*$/d' | LC_ALL=C sort -u) \
        2>/dev/null || true)"

    [[ -n "$overlap" ]] || return 0

    log_warn "rebase 冲突风险预判：本地提交和远端提交改动了相同文件。"
    printf '%s\n' "$overlap" | head -20 | sed 's/^/  - /' >&2
    local overlap_count
    overlap_count="$(printf '%s\n' "$overlap" | sed '/^[[:space:]]*$/d' | wc -l | tr -d ' ')"
    if [[ "${overlap_count:-0}" -gt 20 ]]; then
        printf '  ... 还有 %s 个文件\n' "$((overlap_count - 20))" >&2
    fi
}

run_rebase_with_guidance() {
    local branch="$1"
    local upstream="$2"
    local status=0

    predict_rebase_conflict_risk "$upstream"
    log_info "执行命令: git rebase ${upstream}"
    set +e
    git rebase "$upstream"
    status=$?
    set -e
    if [[ "$status" -eq 0 ]]; then
        clear_sync_failure
        return 0
    fi

    if rebase_in_progress; then
        set_sync_failure "暂停" "rebase 冲突，等待人工解决"
        log_error "rebase 已暂停，存在需要人工解决的冲突。"
        git status --short >&2 || true
        print_rebase_resolution_commands "$upstream" "$branch"
    else
        set_sync_failure "失败" "rebase 失败，命令: git rebase ${upstream}"
        log_error "rebase 失败（退出码 ${status}）。"
        print_rebase_resolution_commands "$upstream" "$branch"
    fi

    return "$status"
}

acquire_repo_lock() {
    local repo_root="$1"
    local git_dir lock_dir stale_pid

    [[ "$DRY_RUN" -eq 0 && "$SHOW_CONFIG" -eq 0 ]] || return 0

    git_dir="$(git -C "$repo_root" rev-parse --git-dir 2>/dev/null)" || return 1
    lock_dir="$git_dir/giter.lock"

    if mkdir "$lock_dir" 2>/dev/null; then
        printf '%s\n' "$$" > "$lock_dir/pid"
        return 0
    fi

    # 检测持锁进程是否还活着，自动清理残留锁
    if [[ -f "$lock_dir/pid" ]]; then
        stale_pid="$(cat "$lock_dir/pid" 2>/dev/null)"
        if [[ -n "$stale_pid" ]] && ! kill -0 "$stale_pid" 2>/dev/null; then
            log_warn "清理残留锁（PID $stale_pid 已不存在）: $lock_dir"
            rm -rf "$lock_dir"
            if mkdir "$lock_dir" 2>/dev/null; then
                printf '%s\n' "$$" > "$lock_dir/pid"
                return 0
            fi
        fi
    fi

    log_error "仓库正在被另一个 giter 进程处理: $repo_root"
    log_error "如确认没有进程运行，可手动删除锁: $lock_dir"
    return 1
}

release_repo_lock() {
    local repo_root="$1"
    local git_dir lock_dir

    [[ "$DRY_RUN" -eq 0 && "$SHOW_CONFIG" -eq 0 ]] || return 0

    git_dir="$(git -C "$repo_root" rev-parse --git-dir 2>/dev/null || true)"
    [[ -n "$git_dir" ]] || return 0
    lock_dir="$git_dir/giter.lock"

    if [[ -d "$lock_dir" ]]; then
        rm -rf "$lock_dir"
    fi
}

abort_if_rebase_in_progress() {
    if rebase_in_progress; then
        set_sync_failure "暂停" "检测到已有 rebase 进行中，等待人工解决"
        log_error "检测到 rebase 正在进行，无法继续自动操作。"
        git status --short >&2 || true
        log_error "请解决冲突后执行: git rebase --continue"
        log_error "或放弃本次 rebase: git rebase --abort"
        return 1
    fi
}

reset_hard_to_upstream() {
    local upstream="$1"
    if [[ "${GITER_ALLOW_RESET_HARD:-0}" != "1" ]]; then
        log_error "--force 需要显式设置 GITER_ALLOW_RESET_HARD=1，已拒绝 reset --hard"
        return 1
    fi

    if [[ "${GITER_CONFIRM_DESTRUCTIVE:-0}" != "1" ]]; then
        print_status_summary
        read_input confirm "确认执行 git reset --hard ${upstream} ? (y/N): "
        [[ "$confirm" =~ ^[Yy]$ ]] || return 1
    fi

    git reset --hard "$upstream"
}

handle_dirty_workdir_before_sync() {
    local branch="$1"
    local upstream="$2"
    local action="${GITER_DIRTY_WORKDIR_ACTION:-}"
    local choice
    local new_commits

    log_warn "本地落后于 ${upstream}，但工作区有未提交变更："
    git status --short >&2

    new_commits="$(git log --oneline HEAD.."$upstream" 2>/dev/null | head -10)"
    if [[ -n "$new_commits" ]]; then
        local commit_count
        commit_count="$(git rev-list --count HEAD.."$upstream" 2>/dev/null || echo "?")"
        echo "" >&2
        log_info "远端有 ${commit_count} 个新提交需要同步："
        echo "$new_commits" | sed 's/^/  /' >&2
        [[ "$commit_count" -gt 10 ]] && echo "  ... (仅显示前 10 个)" >&2
    fi

    # 非交互终端：根据环境变量决定
    if [[ ! -t 0 ]]; then
        action="${action:-commit}"
    fi

    # 交互终端且未指定策略：显示选项
    if [[ -z "$action" ]]; then
        echo "" >&2
        echo "请选择处理方式:" >&2
        echo "  1) 先提交本地变更，再同步远端（推荐）" >&2
        echo "  2) 储藏本地变更，同步后恢复（git stash + rebase + stash pop）" >&2
        echo "  3) 跳过同步，仅提交推送本地变更（可能推送失败）" >&2
        echo "  4) 丢弃本地变更，直接同步远端（危险）" >&2
        echo "  5) 停止，手动处理" >&2
        read_input choice "请选择 [默认 1]: "
        case "${choice:-1}" in
            1|c|C|commit)  action="commit" ;;
            2|s|S|stash)   action="stash" ;;
            3|skip)        action="skip" ;;
            4|d|D|discard) action="discard" ;;
            5|q|Q|abort)   action="abort" ;;
            *)
                log_error "无效选择: $choice"
                return 1
                ;;
        esac
    fi

    case "$action" in
        commit)
            log_info "先提交本地变更，再同步远端。"
            commit_local_changes || {
                set_sync_failure "失败" "同步前提交本地变更失败"
                log_error "提交本地变更失败"
                return 1
            }
            if [ -n "$(git status --porcelain)" ]; then
                set_sync_failure "暂停" "同步前提交后仍有未提交变更，等待人工确认"
                log_error "提交本地变更后工作区仍不干净，已停止 rebase。"
                git status --short >&2
                log_error "请确认是否需要提交、恢复或删除这些残留变更后再重试。"
                return 1
            fi
            log_info "本地变更已提交，执行 rebase ${upstream}。"
            run_rebase_with_guidance "$branch" "$upstream" || return $?
            ;;
        stash)
            log_info "储藏本地变更..."
            git stash push -m "giter: 同步前自动储藏 $(date '+%Y-%m-%d %H:%M:%S')"
            log_info "执行 rebase ${upstream}..."
            if run_rebase_with_guidance "$branch" "$upstream"; then
                log_info "rebase 成功，恢复储藏的变更..."
                if ! git stash pop; then
                    set_sync_failure "暂停" "stash pop 出现冲突，等待人工解决"
                    log_error "恢复储藏变更时出现冲突，请手动解决："
                    log_error "  git stash show -p  # 查看储藏内容"
                    log_error "  git stash pop      # 重新尝试恢复"
                    log_error "  git stash drop     # 解决后丢弃储藏"
                    return 1
                fi
                log_info "储藏变更已恢复。"
            else
                log_error "rebase 失败，储藏的变更保留在 stash 中。"
                log_error "请先解决 rebase 冲突，再执行 git stash pop 恢复变更。"
                return 1
            fi
            ;;
        skip)
            log_info "跳过远端同步，后续推送可能因非 fast-forward 而失败。"
            return 0
            ;;
        discard)
            if [[ -t 0 ]]; then
                local confirm
                log_warn "即将丢弃以下本地变更："
                git diff --stat >&2
                read_input confirm "确认丢弃所有未提交变更? (y/N): "
                [[ "$confirm" =~ ^[Yy]$ ]] || { log_info "已取消"; return 1; }
            fi
            log_warn "丢弃本地变更，恢复到 HEAD 状态。"
            git checkout -- .
            git clean -fd
            log_info "工作区已清理，执行 rebase ${upstream}。"
            run_rebase_with_guidance "$branch" "$upstream" || return $?
            ;;
        abort)
            set_sync_failure "暂停" "等待人工处理工作区变更"
            log_warn "已停止。请手动处理工作区变更后重试。"
            return 1
            ;;
        *)
            set_sync_failure "失败" "无效的 GITER_DIRTY_WORKDIR_ACTION: $action"
            log_error "无效的 GITER_DIRTY_WORKDIR_ACTION: $action"
            return 1
            ;;
    esac
}

check_stale_stashes() {
    local repo_root="$1"
    local stash_list stash_count stash_entry stash_ref stash_msg
    local giter_stashes=()

    stash_list="$(git -C "$repo_root" stash list 2>/dev/null || true)"
    [[ -n "$stash_list" ]] || return 0

    # 筛选 giter 相关的 stash（autostash 或 giter: 前缀）
    while IFS= read -r stash_entry; do
        [[ -n "$stash_entry" ]] || continue
        if [[ "$stash_entry" == *"autostash"* || "$stash_entry" == *"giter:"* ]]; then
            giter_stashes+=("$stash_entry")
        fi
    done <<< "$stash_list"

    [[ ${#giter_stashes[@]} -gt 0 ]] || return 0

    stash_count="${#giter_stashes[@]}"
    log_warn "检测到 ${stash_count} 个可能被遗忘的储藏记录："
    for stash_entry in "${giter_stashes[@]}"; do
        echo "  $stash_entry" >&2
    done

    [[ -t 0 ]] || {
        log_warn "非交互模式，跳过储藏处理。可手动执行 git stash list 查看。"
        return 0
    }

    echo "" >&2
    echo "请选择处理方式:" >&2
    echo "  1) 逐个查看并决定（推荐）" >&2
    echo "  2) 全部恢复到工作区（git stash pop）" >&2
    echo "  3) 全部丢弃" >&2
    echo "  4) 暂时忽略，继续执行" >&2

    local choice
    read_input choice "请选择 [默认 4]: "

    case "${choice:-4}" in
        1)
            local idx=0
            for stash_entry in "${giter_stashes[@]}"; do
                stash_ref="${stash_entry%%:*}"
                stash_msg="${stash_entry#*: }"
                echo "" >&2
                log_info "[$((idx+1))/${stash_count}] $stash_msg"
                echo "  内容摘要:" >&2
                git -C "$repo_root" stash show "$stash_ref" 2>/dev/null | sed 's/^/    /' >&2

                local action
                read_input action "  操作: [p]恢复(pop) [d]丢弃(drop) [s]跳过 [默认 s]: "
                case "${action:-s}" in
                    p|P|pop)
                        if git -C "$repo_root" stash pop "$stash_ref" 2>/dev/null; then
                            log_info "已恢复: $stash_ref"
                        else
                            log_error "恢复 $stash_ref 时出现冲突，请手动解决。"
                            return 1
                        fi
                        ;;
                    d|D|drop)
                        git -C "$repo_root" stash drop "$stash_ref" 2>/dev/null
                        log_info "已丢弃: $stash_ref"
                        ;;
                    *)
                        log_info "跳过: $stash_ref"
                        ;;
                esac
                idx=$((idx + 1))
            done
            ;;
        2)
            # 从最新的开始 pop（避免索引偏移问题）
            local i
            for ((i=${#giter_stashes[@]}-1; i>=0; i--)); do
                stash_ref="${giter_stashes[$i]%%:*}"
                if ! git -C "$repo_root" stash pop "$stash_ref" 2>/dev/null; then
                    log_error "恢复 $stash_ref 时出现冲突，剩余储藏保留。"
                    return 1
                fi
                log_info "已恢复: $stash_ref"
            done
            ;;
        3)
            local confirm
            read_input confirm "确认丢弃所有 ${stash_count} 个储藏? (y/N): "
            if [[ "$confirm" =~ ^[Yy]$ ]]; then
                for stash_entry in "${giter_stashes[@]}"; do
                    stash_ref="${stash_entry%%:*}"
                    git -C "$repo_root" stash drop "$stash_ref" 2>/dev/null || true
                done
                log_info "已丢弃所有 giter 相关储藏。"
            else
                log_info "已取消。"
            fi
            ;;
        4|*)
            log_info "暂时忽略储藏记录，继续执行。"
            ;;
    esac
}

print_divergence_guidance() {
    local branch="$1"
    local upstream="$2"

    log_error "本地与远端已分叉: ${branch} <-> ${upstream}"
    {
        echo
        echo "推荐解决方案:"
        echo "  1) rebase（推荐）：保留本地提交，把它们接到远端最新提交之后，历史更线性。"
        echo "     git fetch origin ${branch}"
        echo "     git rebase --autostash ${upstream}"
        echo "     git push origin ${branch}"
        echo
        echo "  2) merge：生成一次合并提交，适合不想改写本地提交顺序的场景。"
        echo "     git fetch origin ${branch}"
        echo "     git merge --no-ff ${upstream}"
        echo "     git push origin ${branch}"
        echo
        echo "  3) reset（危险）：丢弃本地提交并强制对齐远端。"
        echo "     GITER_ALLOW_RESET_HARD=1 $0 --force"
        echo
        echo "如 rebase/merge 出现冲突，解决冲突后执行:"
        echo "  git add <冲突文件>"
        echo "  git rebase --continue   # rebase 场景"
        echo "  git commit              # merge 场景"
        echo
    } >&2
}

choose_divergence_action() {
    local branch="$1"
    local upstream="$2"
    local action="${GITER_DIVERGENCE_ACTION:-}"
    local choice

    case "$action" in
        rebase|merge|reset|abort) ;;
        "")
            if [[ ! -t 0 ]]; then
                return 1
            fi
            echo "请选择处理方式: 1) rebase 推荐  2) merge  3) reset 危险  4) 停止 [默认 1]: " >&2
            read_input choice
            case "${choice:-1}" in
                1|r|R|rebase) action="rebase" ;;
                2|m|M|merge) action="merge" ;;
                3|reset) action="reset" ;;
                4|q|Q|n|N|abort) action="abort" ;;
                *)
                    log_error "无效选择: $choice"
                    return 1
                    ;;
            esac
            ;;
        *)
            log_error "无效的 GITER_DIVERGENCE_ACTION: $action"
            return 1
            ;;
    esac

    case "$action" in
        rebase)
            if [ -n "$(git status --porcelain)" ]; then
                handle_dirty_workdir_before_sync "$branch" "$upstream" || return 1
            else
                log_info "执行推荐方案: git rebase ${upstream}"
                run_rebase_with_guidance "$branch" "$upstream" || return $?
            fi
            ;;
        merge)
            log_info "执行方案: git merge --no-ff ${upstream}"
            git merge --no-ff "$upstream" || {
                set_sync_failure "暂停" "merge 冲突，等待人工解决"
                log_error "merge 失败或出现冲突。"
                log_error "请解决冲突后执行: git add <已解决文件> && git commit"
                log_error "或放弃本次 merge: git merge --abort"
                return 1
            }
            ;;
        reset)
            reset_hard_to_upstream "$upstream" || return $?
            ;;
        abort)
            set_sync_failure "暂停" "等待人工 rebase/merge 后重试"
            log_warn "已停止。请手动 rebase/merge 后重试。"
            return 1
            ;;
    esac
}

get_remote_url() {
    local remote_name="${1:-origin}"
    git remote get-url "$remote_name" 2>/dev/null || true
}

list_all_remotes() {
    local remote
    while IFS= read -r remote; do
        [[ -n "$remote" ]] || continue
        printf '%s\t%s\n' "$remote" "$(git remote get-url "$remote" 2>/dev/null || true)"
    done <<< "$(git remote 2>/dev/null)"
}

display_all_remotes() {
    local remote url
    log_verbose "远程仓库配置:"
    while IFS=$'\t' read -r remote url; do
        [[ -n "$remote" ]] || continue
        log_verbose "  - $remote: $url"
    done <<< "$(list_all_remotes)"
}

select_upstream_remote() {
    local branch="$1"
    local choice idx
    local remotes=()
    local remote_names=()
    local remotes_output

    remotes_output="$(list_all_remotes)"
    while IFS=$'\t' read -r remote url; do
        [[ -n "$remote" ]] || continue
        remotes+=("$remote")
        remote_names+=("$remote")
    done <<< "$remotes_output"

    if [[ ${#remotes[@]} -eq 0 ]]; then
        log_error "未找到任何远程仓库"
        return 1
    fi

    echo "请选择要推送到的远程仓库:" >&2
    for idx in "${!remotes[@]}"; do
        echo "  $((idx + 1))) ${remotes[$idx]}" >&2
    done

    read_input choice "请选择 [默认 1]: "
    choice="${choice:-1}"

    if [[ "$choice" =~ ^[0-9]+$ ]] && [[ "$choice" -ge 1 && "$choice" -le ${#remotes[@]} ]]; then
        local selected="${remotes[$((choice - 1))]}"
        log_info "已选择远程: $selected"
        SELECTED_REMOTE="$selected"

        if git rev-parse --verify --quiet "${selected}/${branch}" >/dev/null; then
            SELECTED_UPSTREAM="${selected}/${branch}"
        else
            select_remote_branch "$selected" "$branch"
        fi
        return 0
    fi

    log_error "无效选择"
    return 1
}

select_remote_branch() {
    local remote="$1"
    local branch="$2"
    local choice idx
    local branches=()
    local branch_output

    branch_output="$(git branch -r --list "${remote}/*" 2>/dev/null | sed "s/^[[:space:]]*//" | grep -v "HEAD")"

    if [[ -z "$branch_output" ]]; then
        echo "远程 ${remote} 没有可用的分支，将直接创建 ${remote}/${branch}" >&2
        SELECTED_UPSTREAM=""
        log_info "将直接推送创建 ${remote}/${branch}"
        return 0
    fi

    echo "" >&2
    echo "远程 ${remote} 的分支列表:" >&2
    while IFS= read -r b; do
        [[ -n "$b" ]] || continue
        local short="${b#${remote}/}"
        branches+=("$short")
        echo "  ${#branches[@]}) ${short}" >&2
    done <<< "$branch_output"

    echo "  $(( ${#branches[@]} + 1 ))) 直接创建 ${branch} (同名分支)" >&2

    read_input choice "请选择目标分支 [默认 ${#branches[@]}]: "
    choice="${choice:-${#branches[@]}}"

    if [[ "$choice" =~ ^[0-9]+$ ]] && [[ "$choice" -ge 1 && "$choice" -le $(( ${#branches[@]} + 1 )) ]]; then
        if [[ "$choice" -eq $(( ${#branches[@]} + 1 )) ]]; then
            SELECTED_UPSTREAM=""
            log_info "将直接推送创建 ${remote}/${branch}"
        else
            SELECTED_UPSTREAM="${remote}/${branches[$((choice - 1))]}"
            log_info "将推送到 ${remote}/${branches[$((choice - 1))]}"
        fi
    else
        log_error "无效选择"
        return 1
    fi
}

sync_remote_before_commit() {
    local branch="${1:-$(get_current_branch)}"
    local upstream
    local local_commit remote_commit base

    clear_sync_failure
    abort_if_rebase_in_progress || return 1

    display_all_remotes

    upstream="$(current_upstream "${branch:-main}")" || {
        log_info "未找到上游分支 ${branch}，请选择远程仓库"
        if [[ -t 0 ]]; then
            select_upstream_remote "$branch" || return 1
            upstream="$SELECTED_UPSTREAM"
        else
            set_sync_failure "失败" "未设置上游分支；请执行: git branch --set-upstream-to=<remote>/<branch> ${branch}"
            log_error "非交互模式需要设置分支上游或使用 -b 指定分支"
            return 1
        fi
    }

    if [[ -z "$upstream" ]]; then
        log_info "无上游分支需要同步，将直接推送"
        return 0
    fi

    if [[ "$DRY_RUN" -eq 1 ]]; then
        log_info "[dry-run] 将获取远程状态并同步: ${upstream}"
        return 0
    fi

    log_verbose "正在获取远程状态: ${upstream}"
    local fetch_status=0
    local remote_name="${upstream%%/*}"

    # 判断是否需要 fetch（fetch_upstream 内部会判断跳过/重试/首次）
    local already_fetched=0
    for r in "${GITER_FETCHED_REMOTES[@]+"${GITER_FETCHED_REMOTES[@]}"}"; do
    [[ "$r" == "$remote_name" ]] && { already_fetched=1; break; }
    done
    if [[ "$already_fetched" -eq 1 ]]; then
    log_debug "远程 $remote_name 已是最新，跳过 fetch。"
    else
    fetch_upstream "$upstream"
    fetch_status=$?
    if [[ "$fetch_status" -ne 0 ]]; then
    if [[ "$fetch_status" -eq 124 ]]; then
    log_warn "获取远程状态超时（${GITER_GIT_NETWORK_TIMEOUT}s），请检查网络或 SSH 认证。"
    fi
    if git rev-parse --verify --quiet "$upstream" >/dev/null; then
    log_warn "无法更新远程分支 ${upstream}，使用本地缓存状态继续。"
    [[ -n "$GITER_LAST_FETCH_ERROR" ]] && log_warn "详细原因:\n${GITER_LAST_FETCH_ERROR}"
    else
    log_warn "未找到上游分支 ${upstream}，可能是新分支。"
    [[ -n "$GITER_LAST_FETCH_ERROR" ]] && log_warn "详细原因:\n${GITER_LAST_FETCH_ERROR}"
    fi
    fi
    fi

    if ! git rev-parse --verify --quiet "$upstream" >/dev/null; then
        return 0
    fi

    remote_commit="$(git rev-parse "$upstream" 2>/dev/null || true)"
    [[ -n "$remote_commit" ]] || return 0

    local_commit="$(git rev-parse HEAD)"
    [[ "$local_commit" == "$remote_commit" ]] && return 0

    base="$(git merge-base HEAD "$upstream" 2>/dev/null || true)"
    if [[ "$base" == "$local_commit" ]]; then
        if [ -n "$(git status --porcelain)" ]; then
            handle_dirty_workdir_before_sync "$branch" "$upstream" || return 1
        else
            log_info "本地落后于 ${upstream}，工作区干净，执行 rebase。"
            run_rebase_with_guidance "$branch" "$upstream" || return $?
        fi
    elif [[ "$base" == "$remote_commit" ]]; then
        log_info "本地分支领先远端，跳过拉取。"
    else
        if [ "$FORCE_PULL" -eq 1 ]; then
            reset_hard_to_upstream "$upstream"
        else
            print_divergence_guidance "$branch" "$upstream"
            choose_divergence_action "$branch" "$upstream" || return 1
        fi
    fi
}

commit_local_changes() {
    local commit_before commit_after repo_name

    if [ -z "$(git status --porcelain)" ]; then
        return 0
    fi

    if [[ "$DRY_RUN" -eq 1 ]]; then
        log_info "[dry-run] 当前工作区存在变更，将按安全规则暂存并提交。"
        repo_name="$(basename "$(git rev-parse --show-toplevel 2>/dev/null || pwd)")"
        print_status_summary "$repo_name"
        stage_safe_changes
        return 0
    fi

    stage_safe_changes

    if git diff --cached --quiet; then
        log_warn "没有可提交的安全变更。"
        return 0
    fi

    repo_name="$(basename "$(git rev-parse --show-toplevel 2>/dev/null || pwd)")"
    if [ "$FORCE_MODE" -eq 0 ]; then
        print_status_summary "$repo_name"
        read_input confirm "确认提交? (y/N): "
        [[ "$confirm" =~ ^[Yy]$ ]] || return 0
    else
        print_status_summary "$repo_name"
    fi

    run_precommit_checks

    local msg="${COMMIT_MSG:-$DEFAULT_COMMIT_MSG}"
    commit_before="$(git rev-parse HEAD 2>/dev/null || true)"
    if [[ "$VERBOSE" -eq 1 ]]; then
        git commit -m "$msg"
    else
        git commit -q -m "$msg"
    fi
    commit_after="$(git rev-parse HEAD 2>/dev/null || true)"
    if [[ -n "$commit_before" && -n "$commit_after" && "$commit_before" != "$commit_after" ]]; then
        GITER_DID_COMMIT=1
        GITER_COMMIT_BEFORE="${GITER_COMMIT_BEFORE:-$commit_before}"
        GITER_COMMIT_AFTER="$commit_after"
        log_info "${repo_name}: 已提交 $(git rev-parse --short "$commit_after")"
    fi
}

push_if_needed() {
    local branch="$1"
    local upstream remote
    local current_local current_remote
    local extra_remote pushed_remote
    local pushed_remotes=()

    [ "$NO_PUSH" -eq 0 ] || {
        log_info "已按 --no-push 跳过推送。"
        return 0
    }

    if [[ -n "${SELECTED_REMOTE:-}" ]]; then
        remote="$SELECTED_REMOTE"
        upstream="$SELECTED_UPSTREAM"
    else
        upstream="$(current_upstream "$branch")" || upstream=""
        remote="${upstream%%/*}"
        [[ -n "$remote" && "$remote" != "$upstream" ]] || remote="origin"
    fi

    if [[ "$DRY_RUN" -eq 1 ]]; then
        log_info "[dry-run] 将按需推送: $remote $branch"
        pushed_remotes+=("$remote")
        for extra_remote in "${GITER_EXTRA_PUSH_REMOTES[@]+"${GITER_EXTRA_PUSH_REMOTES[@]}"}" "${GITER_CLI_EXTRA_PUSH_REMOTES[@]+"${GITER_CLI_EXTRA_PUSH_REMOTES[@]}"}"; do
            [[ -n "$extra_remote" ]] || continue
            for pushed_remote in "${pushed_remotes[@]+"${pushed_remotes[@]}"}"; do
                [[ "$pushed_remote" == "$extra_remote" ]] && continue 2
            done
            pushed_remotes+=("$extra_remote")
            log_info "[dry-run] 将额外推送: $extra_remote $branch"
        done
        return 0
    fi

    current_local="$(git rev-parse HEAD)"
    local push_quiet=""
    [[ "$VERBOSE" -eq 1 ]] || push_quiet="-q"

    if [[ -z "$upstream" ]]; then
        if ! run_git_push -u $push_quiet "$remote" "$branch"; then
            return 1
        fi
        GITER_DID_PUSH=1
    else
        current_remote="$(git rev-parse "$upstream" 2>/dev/null || true)"
        if [ -z "$current_remote" ]; then
            if ! run_git_push -u $push_quiet "$remote" "$branch"; then
                return 1
            fi
            GITER_DID_PUSH=1
        elif [ "$current_local" != "$current_remote" ]; then
            if git merge-base --is-ancestor "$current_remote" "$current_local"; then
                if ! run_git_push $push_quiet "$remote" "$branch"; then
                    return 1
                fi
                GITER_DID_PUSH=1
            else
                log_error "远端不是本地祖先，拒绝推送。请先手动同步。"
                return 1
            fi
        else
            log_verbose "本地与远端一致，无需推送。"
        fi
    fi

    pushed_remotes+=("$remote")
    for extra_remote in "${GITER_EXTRA_PUSH_REMOTES[@]+"${GITER_EXTRA_PUSH_REMOTES[@]}"}" "${GITER_CLI_EXTRA_PUSH_REMOTES[@]+"${GITER_CLI_EXTRA_PUSH_REMOTES[@]}"}"; do
        [[ -n "$extra_remote" ]] || continue
        for pushed_remote in "${pushed_remotes[@]+"${pushed_remotes[@]}"}"; do
            [[ "$pushed_remote" == "$extra_remote" ]] && continue 2
        done
        if ! git remote get-url "$extra_remote" >/dev/null 2>&1; then
            log_warn "额外推送远程不存在，跳过: $extra_remote"
            continue
        fi
        if ! run_git_push $push_quiet "$extra_remote" "$branch"; then
            return 1
        fi
        pushed_remotes+=("$extra_remote")
        GITER_DID_PUSH=1
    done
}

# --- 5. 核心 Git 逻辑 ---

perform_git_ops() {
    local work_dir="$1"
    local repo_action="${2:-normal}"
    local dir_name
    dir_name="$(basename "$work_dir")"

    (
        # shellcheck disable=SC2329
        cleanup_repo() {
            local rc=$?
            trap - EXIT
            restore_vendor_copy_git_dirs || true
            maybe_return_to_original_branch "${repo_root:-}" "${original_branch:-}" "${branch:-}" || true
            release_repo_lock "${repo_root:-}" || true
            exit "$rc"
        }

        if [ ! -d "$work_dir" ]; then
            log_debug "目录不存在，跳过: $work_dir"
            exit 0
        fi

        cd "$work_dir" || exit 1

        if ! is_git_repo "."; then
            log_warn "[${dir_name}] 不是 Git 仓库，跳过。"
            exit 0
        fi

        local repo_root original_branch branch branch_remote_ref
        repo_root="$(git_root_for ".")"
        original_branch="$(get_current_branch)"
        branch="$original_branch"

        load_repo_config "$repo_root" || exit 1
        cd "$repo_root" || exit 1
        recover_stale_vendor_copy_git_dirs "$repo_root"
        acquire_repo_lock "$repo_root" || exit 1
        trap cleanup_repo EXIT

        # 检测是否有遗忘的 stash 记录
        check_stale_stashes "$repo_root"

        if [[ -z "$original_branch" && -z "$TARGET_BRANCH" && ! -t 0 ]]; then
            log_warn "[${dir_name}] 当前处于 detached HEAD，跳过。"
            add_summary "$repo_root" "跳过" "detached HEAD"
            exit 0
        fi

        if ! select_branch_for_repo "$repo_root" "$original_branch"; then
            add_summary "$repo_root" "失败" "分支选择失败"
            exit 1
        fi
        branch="$SELECTED_BRANCH"
        branch_remote_ref="$SELECTED_REMOTE_REF"

        if ! checkout_branch_if_needed "$repo_root" "$original_branch" "$branch" "$branch_remote_ref"; then
            add_summary "$repo_root" "失败" "切换分支失败: $branch"
            exit 1
        fi

        if [[ "$DRY_RUN" -eq 0 ]]; then
            branch="$(get_current_branch)"
        fi

        detect_nested_git_repos "$repo_root"
        prompt_unknown_nested_repo_policies "$repo_root"
        warn_nested_policy_mismatches "$repo_root"
        prepare_parent_ignored_paths "$repo_root"
        update_repo_gitignore_hash "$repo_root"
        normalize_commit_paths_for_repo "$repo_root"

        if [ -z "$branch" ]; then
            log_warn "[${dir_name}] 当前处于 detached HEAD，跳过。"
            add_summary "$repo_root" "跳过" "detached HEAD"
            exit 0
        fi

        local head_before_sync head_after_sync dirty_count=0
        local did_sync=0 did_commit=0 did_push=0
        GITER_DID_COMMIT=0
        GITER_COMMIT_BEFORE=""
        GITER_COMMIT_AFTER=""
        GITER_SYNC_FAILURE_STATUS=""
        GITER_SYNC_FAILURE_DETAIL=""

        head_before_sync="$(git rev-parse HEAD 2>/dev/null || true)"
        if [[ "$repo_action" == "update-only" && -z "${GITER_DIRTY_WORKDIR_ACTION:-}" ]]; then
            GITER_DIRTY_WORKDIR_ACTION="stash"
        fi

        if ! sync_remote_before_commit "$branch"; then
            add_summary "$repo_root" "${GITER_SYNC_FAILURE_STATUS:-失败}" "${GITER_SYNC_FAILURE_DETAIL:-同步远端失败，请查看上方 Git 输出}"
            exit 1
        fi
        head_after_sync="$(git rev-parse HEAD 2>/dev/null || true)"
        if [[ -n "$head_before_sync" && -n "$head_after_sync" && "$head_before_sync" != "$head_after_sync" ]]; then
            did_sync=1
        fi

        if [[ "$repo_action" == "update-only" ]]; then
            if [ -n "$(git status --porcelain)" ]; then
                dirty_count="$(git status --porcelain | sed '/^[[:space:]]*$/d' | wc -l | tr -d ' ')"
            fi
        else
            if ! commit_local_changes; then
                add_summary "$repo_root" "失败" "提交失败"
                exit 1
            fi
            did_commit="${GITER_DID_COMMIT:-0}"

            GITER_DID_PUSH=0
            if ! push_if_needed "$branch"; then
                add_summary "$repo_root" "失败" "推送失败"
                exit 1
            fi
            did_push="${GITER_DID_PUSH:-0}"
        fi

        if [[ "$DRY_RUN" -eq 1 ]]; then
            add_summary "$repo_root" "预演" "分支 ${branch}，未执行写入/提交/推送"
        elif [[ "$did_sync" -eq 1 || "$did_commit" -eq 1 || "$did_push" -eq 1 ]]; then
            # 收集有价值的摘要信息
            local summary_detail="" action_text=""
            local head_short remote_url_short
            head_short="$(git rev-parse --short HEAD 2>/dev/null || echo "?")"
            remote_url_short="$(git remote get-url origin 2>/dev/null | sed 's|.*[:/]||;s|\.git$||' || echo "")"

            [[ "$did_commit" -eq 1 ]] && action_text="${action_text:+$action_text,}提交"
            [[ "$did_sync" -eq 1 ]] && action_text="${action_text:+$action_text,}同步"
            [[ "$did_push" -eq 1 ]] && action_text="${action_text:+$action_text,}推送"
            if [[ "$did_push" -eq 1 && "$did_commit" -eq 0 && "$did_sync" -eq 0 ]]; then
                action_text="无新提交，仅推送"
            fi

            # 统计本轮新提交的文件数和增删行；无本轮提交时不沿用 HEAD~1。
            local commit_stat=""
            if [[ "$did_commit" -eq 1 && -n "${GITER_COMMIT_BEFORE:-}" && -n "${GITER_COMMIT_AFTER:-}" ]]; then
                commit_stat="$(git diff --shortstat "$GITER_COMMIT_BEFORE" "$GITER_COMMIT_AFTER" 2>/dev/null || true)"
            fi

            summary_detail="${remote_url_short:+${remote_url_short} }${branch} ${head_short}${action_text:+ | ${action_text}}"
            if [[ -n "$commit_stat" ]]; then
                # 格式如: 5 files changed, 227 insertions(+), 4 deletions(-)
                local files_changed insertions deletions
                files_changed="$(echo "$commit_stat" | grep -o '[0-9]* file' | grep -o '[0-9]*')"
                insertions="$(echo "$commit_stat" | grep -o '[0-9]* insertion' | grep -o '[0-9]*')"
                deletions="$(echo "$commit_stat" | grep -o '[0-9]* deletion' | grep -o '[0-9]*')"
                summary_detail="${summary_detail} | ${files_changed:-0}文件"
                [[ -n "$insertions" && "$insertions" != "0" ]] && summary_detail="${summary_detail} +${insertions}"
                [[ -n "$deletions" && "$deletions" != "0" ]] && summary_detail="${summary_detail} -${deletions}"
            fi

            add_summary "$repo_root" "完成" "$summary_detail"
        elif [[ "$repo_action" == "update-only" && "${dirty_count:-0}" -gt 0 ]]; then
            add_summary "$repo_root" "跳过" "${branch} | update-only 已检查同步，保留 ${dirty_count} 个本地未提交变更"
        else
            print_last_commit_details "$dir_name"
            add_summary "$repo_root" "跳过" "${branch} | 本次无变更"
        fi
    )
}

repo_list_contains() {
    local needle="$1"
    shift
    local item

    for item in "$@"; do
        [[ "$item" == "$needle" ]] && return 0
    done
    return 1
}

configured_child_push_repos_for_parent() {
    local repo_root="$1"
    local push_dir push_repo policy_idx

    load_repo_config "$repo_root" || return 1

    for push_dir in "${GITER_PUSH_DIRS[@]+"${GITER_PUSH_DIRS[@]}"}"; do
        [[ -n "$push_dir" ]] || continue
        if push_repo="$(git_root_for "$push_dir" 2>/dev/null)"; then
            [[ "$push_repo" != "$repo_root" ]] && printf '%s\n' "$push_repo"
        fi
    done

    for policy_idx in "${!GITER_NESTED_POLICY_PATHS[@]}"; do
        case "${GITER_NESTED_POLICY_STRATEGIES[$policy_idx]}" in
            independent-push|vendor-copy-push|update-only) ;;
            *) continue ;;
        esac
        push_dir="${GITER_NESTED_POLICY_PATHS[$policy_idx]}"
        if push_repo="$(git_root_for "$push_dir" 2>/dev/null)"; then
            [[ "$push_repo" != "$repo_root" ]] && printf '%s\n' "$push_repo"
        fi
    done | sort -u
}

nested_selection_matches() {
    local repo_root="$1"
    local child_repo="$2"
    local selector="$3"
    local rel_path base_name

    [[ -n "$selector" ]] || return 1
    rel_path="$(repo_relative_path "$repo_root" "$child_repo" 2>/dev/null || echo "$child_repo")"
    base_name="$(basename "$child_repo")"
    selector="$(trim_trailing_slash "$selector")"

    [[ "$selector" == "$child_repo" || "$selector" == "$rel_path" || "$selector" == "$base_name" ]]
}

nested_repo_selected_by_cli() {
    local repo_root="$1"
    local child_repo="$2"
    local selector

    for selector in "${GITER_SKIP_NESTED[@]+"${GITER_SKIP_NESTED[@]}"}"; do
        if nested_selection_matches "$repo_root" "$child_repo" "$selector"; then
            return 1
        fi
    done

    if [[ ${#GITER_WITH_NESTED[@]} -eq 0 ]]; then
        return 0
    fi

    for selector in "${GITER_WITH_NESTED[@]+"${GITER_WITH_NESTED[@]}"}"; do
        if nested_selection_matches "$repo_root" "$child_repo" "$selector"; then
            return 0
        fi
    done

    return 1
}

select_child_push_repos_for_parent() {
    local repo_root="$1"
    local child_repo rel_path choice selected idx
    local child_repos=()
    local selected_repos=()

    while IFS= read -r child_repo; do
        [[ -n "$child_repo" ]] || continue
        nested_repo_selected_by_cli "$repo_root" "$child_repo" || continue
        child_repos+=("$child_repo")
    done < <(configured_child_push_repos_for_parent "$repo_root")

    [[ ${#child_repos[@]} -gt 0 ]] || return 0

    if [[ "$GITER_NESTED_SELECTION_EXPLICIT" -eq 1 || ! -t 0 ]]; then
        printf '%s\n' "${child_repos[@]}"
        return 0
    fi

    echo "" >&2
    echo "发现配置的嵌套仓库处理候选:" >&2
    for idx in "${!child_repos[@]}"; do
        rel_path="$(repo_relative_path "$repo_root" "${child_repos[$idx]}" 2>/dev/null || echo "${child_repos[$idx]}")"
        printf '  %d) %s\n' "$((idx + 1))" "$rel_path" >&2
    done
    echo "  a) 全部（默认）" >&2
    echo "  n) 不处理嵌套仓库" >&2
    read_input choice "请选择本次要处理的嵌套仓库 [默认 a，多个用空格，如: 1 2]: "
    choice="${choice:-a}"

    case "$choice" in
        a|A|all|ALL|全部)
            printf '%s\n' "${child_repos[@]}"
            return 0
            ;;
        n|N|none|NONE|不处理)
            return 0
            ;;
    esac

    for selected in $choice; do
        if [[ "$selected" =~ ^[0-9]+$ ]]; then
            idx=$((selected - 1))
            if [[ "$idx" -ge 0 && "$idx" -lt "${#child_repos[@]}" ]]; then
                selected_repos+=("${child_repos[$idx]}")
            else
                log_warn "无效嵌套仓库编号，已忽略: $selected"
            fi
        else
            for child_repo in "${child_repos[@]+"${child_repos[@]}"}"; do
                if nested_selection_matches "$repo_root" "$child_repo" "$selected"; then
                    selected_repos+=("$child_repo")
                fi
            done
        fi
    done

    printf '%s\n' "${selected_repos[@]+"${selected_repos[@]}"}" | sort -u
}

maintain_gitignore() {
    is_git_repo "$TARGET_DIR" || return 0

    local repo_root
    repo_root="$(git_root_for "$TARGET_DIR")" || return 0
    load_repo_config "$repo_root" || return 1
    detect_nested_git_repos "$repo_root"
    ensure_parent_gitignore_entries "$repo_root"
}

# --- 6. 主程序 ---

main() {
    local original_argc=$#
    local fail_count=0
    GITER_SUMMARY_FILE="$(mktemp)"
    trap 'rm -f "$GITER_SUMMARY_FILE"' EXIT

    while [[ $# -gt 0 ]]; do
        case "$1" in
            -d|--dir)
                [[ $# -ge 2 ]] || { log_error "$1 需要 PATH 参数"; exit 2; }
                TARGET_DIR="$2"
                TARGET_DIR_EXPLICIT=1
                shift 2
                ;;
            -b|--branch)
                [[ $# -ge 2 && -n "$2" ]] || { log_error "$1 需要 BRANCH 参数"; exit 2; }
                TARGET_BRANCH="$2"
                shift 2
                ;;
            -a|--auto)
                AUTO_DISCOVER=1
                shift
                ;;
            --all-nested)
                DISCOVER_ALL_NESTED=1
                shift
                ;;
            --with-nested)
                [[ $# -ge 2 && -n "$2" ]] || { log_error "$1 需要 LIST 参数"; exit 2; }
                append_csv_values_to_array GITER_WITH_NESTED "$2"
                GITER_NESTED_SELECTION_EXPLICIT=1
                shift 2
                ;;
            --skip-nested)
                [[ $# -ge 2 && -n "$2" ]] || { log_error "$1 需要 LIST 参数"; exit 2; }
                append_csv_values_to_array GITER_SKIP_NESTED "$2"
                GITER_NESTED_SELECTION_EXPLICIT=1
                shift 2
                ;;
            --push-remotes)
                [[ $# -ge 2 && -n "$2" ]] || { log_error "$1 需要 LIST 参数"; exit 2; }
                append_csv_values_to_array GITER_CLI_EXTRA_PUSH_REMOTES "$2"
                shift 2
                ;;
            --dry-run)
                DRY_RUN=1
                shift
                ;;
            --show-config)
                SHOW_CONFIG=1
                AUTO_DISCOVER=1
                shift
                ;;
            --install)
                install_giter_command
                exit 0
                ;;
            -l|--depth)
                [[ $# -ge 2 && "$2" =~ ^[0-9]+$ ]] || { log_error "$1 需要数字参数"; exit 2; }
                DISCOVER_DEPTH="$2"
                shift 2
                ;;
            -f|--force)
                FORCE_PULL=1
                shift
                ;;
            -i|--interactive)
                FORCE_MODE=0
                shift
                ;;
            -v|--verbose)
                VERBOSE=1
                shift
                ;;
            --last|--lc|-lc)
                [[ $# -ge 2 && "$2" =~ ^[0-9]+$ ]] || { log_error "$1 需要数字参数"; exit 2; }
                LAST_COMMITS="$2"
                if [[ "$LAST_COMMITS" -lt 1 ]]; then
                    LAST_COMMITS=1
                elif [[ "$LAST_COMMITS" -gt 20 ]]; then
                    LAST_COMMITS=20
                fi
                shift 2
                ;;
            --no-files)
                SHOW_LAST_FILES=0
                shift
                ;;
            -n|--no-push)
                NO_PUSH=1
                shift
                ;;
            -m|--message)
                [[ $# -ge 2 && -n "$2" ]] || { log_error "$1 需要 MSG 参数"; exit 2; }
                COMMIT_MSG="$2"
                shift 2
                ;;
            --)
                shift
                while [[ $# -gt 0 ]]; do
                    COMMIT_PATHS+=("$1")
                    shift
                done
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            -*)
                log_error "未知选项: $1"
                exit 2
                ;;
            *)
                log_error "未知选项或不支持位置参数: $1 (请使用 -m 指定提交注释)"
                exit 2
                ;;
        esac
    done

    if [ "$original_argc" -eq 0 ] && [ "$AUTO_DISCOVER" -eq 0 ]; then
        AUTO_DISCOVER=1
        log_verbose "启用自动发现"
    fi

    if [[ "$TARGET_DIR" != /* ]]; then
        TARGET_DIR="$(pwd)/$TARGET_DIR"
    fi

    if [ "$TARGET_DIR_EXPLICIT" -eq 1 ] && [ "$AUTO_DISCOVER" -eq 0 ] && [ -t 0 ]; then
        AUTO_DISCOVER=1
        log_verbose "启用指定目录仓库候选选择"
    fi

    log_verbose "脚本目录: $SCRIPT_DIR"
    log_verbose "git仓库目录: $ORIGINAL_PWD"

    if [[ "$SHOW_CONFIG" -eq 1 ]]; then
        show_config_and_candidates
        exit 0
    fi

    if [ "$AUTO_DISCOVER" -eq 1 ]; then
        local repos_file repo_count repo_dir
        local selected_repo_dirs=()
        repos_file="$(mktemp)"
        build_auto_repo_candidates
        select_auto_repos > "$repos_file"

        repo_count="$(wc -l < "$repos_file" | tr -d ' ')"
        if [ -z "$repo_count" ] || [ "$repo_count" -eq 0 ]; then
            log_warn "未发现任何 Git 仓库"
            rm -f "$repos_file"
            exit 0
        fi

        while IFS= read -r repo_dir; do
            [ -z "$repo_dir" ] && continue
            selected_repo_dirs+=("$repo_dir")
        done < "$repos_file"
        rm -f "$repos_file"

        local processed_repo_dirs=()
        local child_repo_dir child_repo_action
        for repo_dir in "${selected_repo_dirs[@]+"${selected_repo_dirs[@]}"}"; do
            load_repo_config "$repo_dir" || {
                fail_count=$((fail_count + 1))
                continue
            }
            while IFS= read -r child_repo_dir; do
                [[ -n "$child_repo_dir" ]] || continue
                repo_list_contains "$child_repo_dir" "${processed_repo_dirs[@]+"${processed_repo_dirs[@]}"}" && continue
                child_repo_action="$(nested_policy_strategy_for_path "$child_repo_dir" "exact" 2>/dev/null || echo "independent-push")"
                log_verbose "先处理父仓库配置的子仓库: $child_repo_dir"
                if ! perform_git_ops "$child_repo_dir" "$child_repo_action"; then
                    fail_count=$((fail_count + 1))
                fi
                processed_repo_dirs+=("$child_repo_dir")
            done < <(select_child_push_repos_for_parent "$repo_dir")

            repo_list_contains "$repo_dir" "${processed_repo_dirs[@]+"${processed_repo_dirs[@]}"}" && continue
            if ! perform_git_ops "$repo_dir"; then
                fail_count=$((fail_count + 1))
            fi
            processed_repo_dirs+=("$repo_dir")
        done
    else
        local sub_dir policy_idx
        local root_for_config=""
        if is_git_repo "$TARGET_DIR"; then
            root_for_config="$(git_root_for "$TARGET_DIR")"
            load_repo_config "$root_for_config"
        fi

        for sub_dir in "${GITER_PUSH_DIRS[@]+"${GITER_PUSH_DIRS[@]}"}"; do
            if [[ "$root_for_config" != "" ]]; then
                local sub_repo_for_selection
                sub_repo_for_selection="$(git_root_for "$sub_dir" 2>/dev/null || echo "$sub_dir")"
                nested_repo_selected_by_cli "$root_for_config" "$sub_repo_for_selection" || continue
            fi
            if ! perform_git_ops "$sub_dir"; then
                fail_count=$((fail_count + 1))
            fi
        done

        for policy_idx in "${!GITER_NESTED_POLICY_PATHS[@]}"; do
            case "${GITER_NESTED_POLICY_STRATEGIES[$policy_idx]}" in
                independent-push|vendor-copy-push|update-only) ;;
                *) continue ;;
            esac
            sub_dir="${GITER_NESTED_POLICY_PATHS[$policy_idx]}"
            if ! repo_in_configured_paths "$sub_dir" GITER_PUSH_DIRS; then
                if [[ "$root_for_config" != "" ]]; then
                    local sub_repo_for_selection
                    sub_repo_for_selection="$(git_root_for "$sub_dir" 2>/dev/null || echo "$sub_dir")"
                    nested_repo_selected_by_cli "$root_for_config" "$sub_repo_for_selection" || continue
                fi
                if ! perform_git_ops "$sub_dir" "${GITER_NESTED_POLICY_STRATEGIES[$policy_idx]}"; then
                    fail_count=$((fail_count + 1))
                fi
            fi
        done

        if ! perform_git_ops "$TARGET_DIR"; then
            fail_count=$((fail_count + 1))
        fi
    fi

    if [ "$fail_count" -gt 0 ]; then
        print_summary
        log_error "共有 $fail_count 个仓库处理失败。"
        exit 1
    fi

    print_summary
}

main "$@"
