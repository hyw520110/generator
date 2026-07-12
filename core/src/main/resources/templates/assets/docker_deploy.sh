#!/bin/bash
set -Eeuo pipefail
trap 'code=$?; echo "[ERROR] $(date "+%Y-%m-%d %H:%M:%S") last cmd: ${BASH_COMMAND} at ${BASH_SOURCE[0]}:${LINENO}, exit ${code}" >&2' ERR

# 通用 Docker 部署脚本
# 用法: ./docker_deploy.sh <SERVICE_NAME> <IMAGE_TAG> [CONTAINER_NAME] [HOST_PORT] [CONTAINER_PORT] [CUSTOM_JVM_OPTS] [SPRING_PROFILES] [SKYWALKING_AGENT_NAME] [SKYWALKING_COLLECTOR_ADDRESS]
# 示例: ./docker_deploy.sh gateway-service 1.0.0-SNAPSHOT gateway-app 8080 8080 "-Xms512m -Xmx1024m" "prod" "gateway-docker" "127.0.0.1:11800"

# --- 参数检查 ---
if [ "$#" -lt 2 ]; then
    echo "用法: $0 <SERVICE_NAME> <IMAGE_TAG> [CONTAINER_NAME] [HOST_PORT] [CONTAINER_PORT] [CUSTOM_JVM_OPTS] [SPRING_PROFILES] [SKYWALKING_AGENT_NAME] [SKYWALKING_COLLECTOR_ADDRESS]"
    echo "示例: $0 gateway-service 1.0.0-SNAPSHOT gateway-app 8080 8080 \"-Xms512m -Xmx1024m\" \"prod\" \"gateway-docker\" \"127.0.0.1:11800\""
    exit 1
fi

SERVICE_NAME=$1
IMAGE_TAG=$2
CONTAINER_NAME=${3:-$SERVICE_NAME}
HOST_PORT=${4:-8080}
CONTAINER_PORT=${5:-8080}
CUSTOM_JVM_OPTS=${6:-}
SPRING_PROFILES=${7:-}
SKYWALKING_AGENT_NAME=${8:-$SERVICE_NAME}
SKYWALKING_COLLECTOR_ADDRESS=${9:-}

# --- 变量定义 ---
IMAGE_NAME="${SERVICE_NAME}:${IMAGE_TAG}"

# 容器内部的 SkyWalking Agent 路径 (假设已通过 Dockerfile 或卷挂载到容器内部)
CONTAINER_SKYWALKING_AGENT_PATH="/opt/skywalking-agent"

# --- 函数定义 ---

# 停止并移除旧容器
_stop_and_remove_old_container() {
    local name=$1
    echo "正在检查并停止/移除旧容器 ${name}..."
    if docker ps -a --format '{{.Names}}' | grep -q "^${name}$"; then
        if docker ps --format '{{.Names}}' | grep -q "^${name}$"; then
            echo "停止容器 ${name} ..."
            docker stop ${name}
        fi
        echo "移除容器 ${name} ..."
        docker rm ${name}
    else
        echo "容器 ${name} 不存在，跳过停止/移除。"
    fi
}

# --- 主逻辑 ---

echo "--------------------------------------------------"
echo "开始部署 Docker 服务: ${SERVICE_NAME}"
echo "镜像: ${IMAGE_NAME}"
echo "容器名称: ${CONTAINER_NAME}"
echo "端口映射: ${HOST_PORT}:${CONTAINER_PORT}"
echo "JVM 参数: ${CUSTOM_JVM_OPTS:-默认}"
echo "Spring Profile: ${SPRING_PROFILES:-默认}"
echo "SkyWalking Agent 名称: ${SKYWALKING_AGENT_NAME:-默认}"
echo "SkyWalking Collector 地址: ${SKYWALKING_COLLECTOR_ADDRESS:-默认}"
echo "--------------------------------------------------"

# 1. 拉取最新镜像
echo "正在拉取 Docker 镜像 ${IMAGE_NAME} ..."
docker pull ${IMAGE_NAME}
if [ $? -ne 0 ]; then
    echo "错误: 拉取镜像 ${IMAGE_NAME} 失败。"
    exit 1
fi

# 2. 停止并移除旧容器
_stop_and_remove_old_container "${CONTAINER_NAME}"

# 3. 构建 Docker 运行命令
RUN_COMMAND=(docker run -d --restart=always --name "${CONTAINER_NAME}" -p "${HOST_PORT}:${CONTAINER_PORT}")

# 构建 JAVA_TOOL_OPTIONS
JAVA_TOOL_OPTIONS_VAL=""
if [ -n "${CUSTOM_JVM_OPTS}" ]; then
    JAVA_TOOL_OPTIONS_VAL="${CUSTOM_JVM_OPTS}"
fi

# 添加 SkyWalking Agent 配置
if [ -n "${SKYWALKING_AGENT_NAME}" ] && [ -n "${SKYWALKING_COLLECTOR_ADDRESS}" ]; then
    SKYWALKING_AGENT_OPTS="-javaagent:${CONTAINER_SKYWALKING_AGENT_PATH}/skywalking-agent.jar"
    SKYWALKING_AGENT_OPTS="${SKYWALKING_AGENT_OPTS} -Dskywalking.agent.service_name=${SKYWALKING_AGENT_NAME}"
    SKYWALKING_AGENT_OPTS="${SKYWALKING_AGENT_OPTS} -Dskywalking.collector.backend_service=${SKYWALKING_COLLECTOR_ADDRESS}"

    if [ -n "${JAVA_TOOL_OPTIONS_VAL}" ]; then
        JAVA_TOOL_OPTIONS_VAL="${JAVA_TOOL_OPTIONS_VAL} ${SKYWALKING_AGENT_OPTS}"
    else
        JAVA_TOOL_OPTIONS_VAL="${SKYWALKING_AGENT_OPTS}"
    fi
fi

# 添加 JAVA_TOOL_OPTIONS 环境变量
if [ -n "${JAVA_TOOL_OPTIONS_VAL}" ]; then
    RUN_COMMAND+=( -e "JAVA_TOOL_OPTIONS=${JAVA_TOOL_OPTIONS_VAL}" );
fi

# 添加 Spring Profile 环境变量
if [ -n "${SPRING_PROFILES}" ]; then
    RUN_COMMAND+=( -e "SPRING_PROFILES_ACTIVE=${SPRING_PROFILES}" )
fi

RUN_COMMAND+=("${IMAGE_NAME}")

# 4. 运行新容器
echo "正在运行新容器: ${CONTAINER_NAME}"
echo "执行命令: ${RUN_COMMAND[*]}"

"${RUN_COMMAND[@]}"

if [ $? -ne 0 ]; then
    echo "错误: 启动容器 ${CONTAINER_NAME} 失败。"
    exit 1
fi

echo "容器 ${CONTAINER_NAME} 已启动。"

# 5. 检查容器状态
sleep 5 # 等待容器启动
if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "Docker 服务 ${SERVICE_NAME} 部署成功，容器 ${CONTAINER_NAME} 正在运行。"
else
    echo "警告: Docker 服务 ${SERVICE_NAME} 部署失败或容器 ${CONTAINER_NAME} 未正常运行。请检查 Docker 日志。"
    exit 1
fi

echo "--------------------------------------------------"
echo "Docker 服务 ${SERVICE_NAME} 部署完成。"
echo "--------------------------------------------------"
