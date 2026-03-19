#!/bin/sh

# =============================================================================
# 后端服务停止脚本
# 支持优雅停止 (SIGTERM) -> 超时/强制模式下自动转为强制停止 (SIGKILL)
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo_info() { printf "${GREEN}[INFO]${NC} %s\n" "$1"; }
echo_warn() { printf "${YELLOW}[WARN]${NC} %s\n" "$1"; }

FORCE=false
[ "$1" = "--force" ] || [ "$1" = "-f" ] && FORCE=true

# 提取 ArtifactID (匹配 Maven 占位符或默认值)
ARTIFACT_ID="@project.artifactId@"
[ "$ARTIFACT_ID" = "@project.artifactId@" ] && ARTIFACT_ID="generator-web"

# 查找 PID (同时匹配 mvn 进程和 java -jar 进程)
PID=$(ps -ef | grep -E "java.*$ARTIFACT_ID|spring-boot:run" | grep -v grep | awk '{print $2}')

if [ -z "$PID" ]; then
    echo_info "未检测到正在运行的后端服务。"
    exit 0
fi

echo_info "检测到服务进程 (PID: $PID)，正在尝试优雅停止..."
kill -15 $PID

# 等待停止 (最多 10 秒)
count=0
while ps -p $PID > /dev/null 2>&1; do
    sleep 1
    count=$((count + 1))
    printf "\r正在等待进程结束... [%ds]" "$count"
    
    if [ $count -ge 10 ]; then
        printf "\n"
        if [ "$FORCE" = true ]; then
            echo_warn "优雅停止超时，执行强制杀掉 (kill -9)..."
            kill -9 $PID
        else
            echo_warn "优雅停止超时，进程仍在运行。如需强制结束请加 -f 参数。"
            exit 1
        fi
        break
    fi
done

if ! ps -p $PID > /dev/null 2>&1; then
    printf "\n"
    echo_info "服务已成功停止。"
fi
