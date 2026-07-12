#!/bin/bash

# 功能:
#   通过参数区分，支持对 Nacos 配置进行单点或批量操作。
#
# API 参考: https://nacos.io/docs/latest/manual/admin/admin-api/
#

# --- 配置 (优先级: 环境变量 > 脚本内定义) ---
NACOS_SERVER=${NACOS_SERVER:-"nacos-server:8848"} # Nacos 服务器地址:端口
NACOS_CONTEXT_PATH=${NACOS_CONTEXT_PATH:-"/nacos"}       # Nacos 的 Context Path
NACOS_USERNAME=${NACOS_USERNAME:-"nacos"}
NACOS_PASSWORD=${NACOS_PASSWORD:-"nacos"}
CONFIG_DIR=${CONFIG_DIR:-"config"}      

# --- API Endpoints ---
LOGIN_ENDPOINT_V3="/v3/auth/user/login"             # V3 登录接口
CONFIGS_ENDPOINT_V1="/v1/cs/configs"              # V1 配置读写接口
CONFIGS_ENDPOINT_V3="/v3/admin/cs/config"          # V3 配置读写接口

# --- 脚本变量 ---
ACCESS_TOKEN_FILE=".nacos_token"

# --- Nacos Namespace ID 映射 (从 pom.xml 解析或用户输入) ---
# 请勿手动修改此区域，脚本会自动更新
# 例如: NACOS_NAMESPACE_dev="8e09643e-7ad5-4613-bc69-4cc2f36ba8aa"

# --- 颜色定义 ---
C_RED=$(tput setaf 1)
C_GREEN=$(tput setaf 2)
C_YELLOW=$(tput setaf 3)
C_BLUE=$(tput setaf 4)
C_NC=$(tput sgr0)

# --- 函数定义 ---

# 函数：输出 API 命令
# 用法：log_api_command <method> <url> [params...]
log_api_command() {
    local method=$1
    local url=$2
    shift 2
    
    echo -e "\n${C_BLUE}--- API 命令 ---${C_NC}"
    echo -e "${C_YELLOW}方法:${C_NC} $method"
    echo -e "${C_YELLOW}URL:${C_NC} $url"
    
    if [ $# -gt 0 ]; then
        echo -e "${C_YELLOW}参数:${C_NC}"
        for param in "$@"; do
            # 隐藏敏感信息（如 accessToken）
            if [[ "$param" =~ accessToken= ]]; then
                echo "  ${param%%=*}=***hidden***"
            else
                echo "  $param"
            fi
        done
    fi
    echo -e "${C_BLUE}-------------${C_NC}\n"
}

# 函数：输出 API 响应
# 用法：log_api_response <http_code> <body>
log_api_response() {
    local http_code=$1
    local body=$2
    
    echo -e "\n${C_BLUE}--- API 响应 ---${C_NC}"
    if [ "$http_code" == "200" ] || [ "$http_code" == "true" ]; then
        echo -e "${C_GREEN}HTTP 状态码: $http_code${C_NC}"
    else
        echo -e "${C_RED}HTTP 状态码: $http_code${C_NC}"
    fi
    
    if [ -n "$body" ]; then
        echo -e "${C_YELLOW}响应内容:${C_NC}"
        echo "$body" | head -20
        if [ $(echo "$body" | wc -l) -gt 20 ]; then
            echo "... (内容已截断)"
        fi
    fi
    echo -e "${C_BLUE}-------------${C_NC}\n"
}

# 函数：从 pom.xml 中解析给定配置文件的 nacos.namespace
# 用法：get_namespace_id_from_pom <profile_id>
get_namespace_id_from_pom() {
    local profile_id="$1"
    awk -v p_id="$profile_id" '
        /<profile>/ { in_profile=1; }
        in_profile && $0 ~ "[[:space:]]*<profile>" p_id "</profile>" {
            # 找到匹配的 profile 标签
            # 现在查找下一个 <nacos.namespace>
            while (getline && $0 !~ /<\/profile>/) {
                if ($0 ~ "[[:space:]]*<nacos.namespace>") {
                    sub(/<nacos.namespace>/, "", $0);
                    sub(/<\/nacos.namespace>/, "", $0);
                    print trim($0);
                    exit; # 找到并打印，然后退出
                }
            }
        }
        /<\/profile>/ { in_profile=0; }
        function trim(s) {
            sub(/^[ \t\r\n]+/, "", s);
            sub(/[ \t\r\n]+$/, "", s);
            return s;
        }
    ' pom.xml
}

# 函数：将变量保存到脚本头
# 用法：save_variable_to_script <variable_name> <variable_value>
save_variable_to_script() {
    local var_name="$1"
    local var_value="$2"
    local script_file="$0" # Current script file

    # 检查变量是否已存在于头中
    if grep -q "^${var_name}=" "$script_file"; then
        # 更新现有变量
        sed -i "/^${var_name}=/c\${var_name}=\"${var_value}\"" "$script_file"
        echo -e "${C_YELLOW}更新脚本变量: ${var_name}=\"${var_value}\"${C_NC}"
    else
        # 添加新变量到头中 (在最后一个现有头变量定义之后)
        # 查找最后一个看起来像头变量定义的行
        local last_header_var_line=$(grep -n "^[A-Z_]*=.*" "$script_file" | tail -n 1 | cut -d: -f1)
        if [ -n "$last_header_var_line" ]; then
            sed -i "${last_header_var_line}a\\n${var_name}=\"${var_value}\"" "$script_file"
            echo -e "${C_YELLOW}添加脚本变量: ${var_name}=\"${var_value}\"${C_NC}"
        else
            # Fallback: add to the very top after #!/bin/bash
            sed -i "2a\\n${var_name}=\"${var_value}\"" "$script_file"
            echo -e "${C_YELLOW}添加脚本变量: ${var_name}=\"${var_value}\"${C_NC}"
        fi
    fi
}

function print_help() {
  echo -e "${C_BLUE}Nacos 配置管理脚本 v5.1${C_NC}"
  echo "通过参数自动区分单点/批量模式。"
  echo ""
  echo "用法: $0 <command> [options]"
  echo ""
  echo "命令:"
  echo -e "  ${C_GREEN}compare${C_NC}: 对比本地与远程的配置"
  echo -e "  ${C_GREEN}pull${C_NC}:    从远程拉取配置到本地"
  echo -e "  ${C_GREEN}push${C_NC}:    将本地配置推送到远程"
  echo -e "  ${C_GREEN}clear-cache${C_NC}: 清除Nacos客户端缓存"
  echo -e "  ${C_GREEN}version${C_NC}:  获取Nacos服务器版本信息"
  echo ""
  echo "选项:"
  echo -e "  ${C_YELLOW}--api-version <v1|v3>${C_NC}  指定 Nacos API 版本 (默认: v1)"
  echo -e "  ${C_YELLOW}--dataId <id>${C_NC}       指定此项进入 [单点模式]。如果id不含后缀，默认为.yaml"
  echo -e "  --namespace <ns>   指定 Namespace ID (默认: public)"
  echo -e "  --group <group>      指定 Group (默认: DEFAULT_GROUP)"
  echo -e "  --delete           (仅用于批量 push) 删除远程多余的配置"
  echo -e "  --no-diff          (仅用于 compare) 不启动可视化差异工具"
  echo -e "  --safe-push        (仅用于 compare) 自动推送远程不存在的配置文件"
  echo -e "  --force-push       强制推送配置，覆盖已存在的远程配置"
  echo -e "  --delete-before-push  推送前先删除远程配置（解决重复内容问题）"
  echo -e "  --clear-cache      清除Nacos客户端缓存（针对应用启动问题）"
  echo -e "  -d, --config-dir <dir>  指定配置文件目录 (默认: config)"
  echo -e "  -h, --help         显示帮助信息"
  echo ""
  echo "示例:"
  echo -e "  ${C_BLUE}# [单点模式] 使用 v1 API 对比 gateway-service.yaml${C_NC}"
  echo "  $0 compare --dataId gateway-service --namespace dev --api-version v1"
  echo ""
  echo -e "  ${C_BLUE}# [单点模式] 使用 v3 API 对比 gateway-service.yaml${C_NC}"
  echo "  $0 compare --dataId gateway-service --namespace dev --api-version v3"
  echo ""
  echo -e "  ${C_BLUE}# [单点模式] 对比且不启动可视化差异工具${C_NC}"
  echo "  $0 compare --dataId gateway-service --namespace dev --no-diff"
  echo ""
  echo -e "  ${C_BLUE}# [单点模式] 对比并自动推送远程不存在的配置文件${C_NC}"
  echo "  $0 compare --dataId gateway-service --namespace dev --safe-push"
  echo ""
  echo -e "  ${C_BLUE}# [单点模式] 强制推送配置（覆盖已存在的配置）${C_NC}"
  echo "  $0 push --dataId gateway-service --namespace dev --force-push"
  echo ""
  echo -e "  ${C_BLUE}# [批量模式] 拉取 dev 环境下所有配置${C_NC}"
  echo "  $0 pull dev DEFAULT_GROUP"
  echo ""
  echo -e "  ${C_BLUE}# [批量模式] 强制推送所有配置（覆盖已存在的配置）${C_NC}"
  echo "  $0 push dev DEFAULT_GROUP --force-push"
  echo ""
  echo -e "  ${C_BLUE}# [单点模式] 删除后推送配置（解决重复内容问题）${C_NC}"
  echo "  $0 push --dataId user-service-dev.yaml --delete-before-push"
  echo ""
  echo -e "  ${C_BLUE}# 清除Nacos客户端缓存${C_NC}"
  echo "  $0 clear-cache"
  echo ""
  echo -e "  ${C_BLUE}# 获取Nacos服务器版本${C_NC}"
  echo "  $0 version"
  echo ""
  echo -e "  ${C_BLUE}# 使用自定义配置目录${C_NC}"
  echo "  $0 -d /path/to/config pull dev DEFAULT_GROUP"
}

function check_deps() {
  if ! command -v curl &> /dev/null; then echo -e "${C_RED}'curl' 未安装，请先安装.${C_NC}"; exit 1; fi
  if ! command -v jq &> /dev/null; then echo -e "${C_RED}'jq' 未安装，请先安装.${C_NC}"; exit 1; fi
  if ! command -v unzip &> /dev/null; then echo -e "${C_RED}'unzip' 未安装，请先安装.${C_NC}"; exit 1; fi
}

function check_config_dir() {
  if [ ! -d "$CONFIG_DIR" ]; then
    echo -e "${C_YELLOW}本地配置目录 '${CONFIG_DIR}' 不存在, 将自动创建。${C_NC}"
    mkdir -p "$CONFIG_DIR" || { echo -e "${C_RED}无法创建目录 '${CONFIG_DIR}'，请检查权限。${C_NC}"; exit 1; }
  fi
}

function check_connection() {
    while true; do
        local host; host=$(echo "$NACOS_SERVER" | cut -d: -f1)
        local port; port=$(echo "$NACOS_SERVER" | cut -d: -f2)

        echo "正在检查与 Nacos 服务器的连接: $host:$port"

        if timeout 2 bash -c "</dev/tcp/$host/$port" &>/dev/null; then
            echo -e "${C_GREEN}端口 $host:$port 可达，继续执行...${C_NC}"
            break
        else
            echo -e "${C_RED}错误: 无法连接到 Nacos 服务器端口 $host:$port。${C_NC}"
            read -p "请输入新的 Nacos 地址 (例如: 127.0.0.1:8848) 或按 Ctrl+C 退出: " new_server
            if [ -n "$new_server" ]; then
                if [[ "$new_server" =~ ^[^:]+:[0-9]+$ ]]; then
                    NACOS_SERVER=$new_server
                else
                    echo -e "${C_RED}格式无效，请输入 '主机:端口' 格式。将重试原地址...${C_NC}"
                fi
            else
                echo "输入为空，将重试原地址..."
            fi
            echo ""
        fi
    done
}

function clear_nacos_cache() {
    echo -e "\n${C_BLUE}--- 清除Nacos客户端缓存 ---${C_NC}"
    
    # 查找并删除Nacos客户端缓存目录
    local cache_dirs=(
        "$HOME/nacos"
        "$HOME/.nacos"
        "/tmp/nacos"
        "./nacos"
        "./.nacos"
    )
    
    local cleared=false
    for dir in "${cache_dirs[@]}"; do
        if [ -d "$dir" ]; then
            echo -e "${C_YELLOW}删除缓存目录: $dir${C_NC}"
            rm -rf "$dir"
            cleared=true
        fi
    done
    
    # 查找并删除可能的配置缓存文件
    local cache_patterns=(
        ".nacos-cache*"
        "nacos-config-cache*"
        "config-cache-*"
    )
    
    for pattern in "${cache_patterns[@]}"; do
        find . -name "$pattern" -type f 2>/dev/null | while read -r file; do
            echo -e "${C_YELLOW}删除缓存文件: $file${C_NC}"
            rm -f "$file"
            cleared=true
        done
    done
    
    if [ "$cleared" = true ]; then
        echo -e "${C_GREEN}Nacos客户端缓存已清除${C_NC}"
        echo -e "${C_YELLOW}请重启应用程序以使更改生效${C_NC}"
    else
        echo -e "${C_YELLOW}未找到Nacos客户端缓存${C_NC}"
    fi
    
    # 提示其他可能的解决方案
    echo -e "\n${C_BLUE}其他解决方案:${C_NC}"
    echo "1. 重启UserApplication"
    echo "2. 检查是否有多个配置文件加载（如bootstrap.yml/application.yml）"
    echo "3. 检查Spring Profile配置"
    echo "4. 检查是否有配置继承或覆盖问题"
}

# 功能：获取 Nacos 服务器版本
# 用法：get_nacos_version
function get_nacos_version() {
    echo -e "\n${C_BLUE}--- 获取 Nacos 服务器版本 ---${C_NC}"

    # 尝试使用 /v1/console/server/state 接口获取服务器状态和版本信息
    local server_state_url="http://$NACOS_SERVER$NACOS_CONTEXT_PATH/v1/console/server/state"
    echo -e "${C_YELLOW}尝试访问服务器状态接口...${C_NC}"
    local response; response=$(curl --noproxy "*" -s -w "\n%{http_code}" -X GET "$server_state_url" 2>/dev/null)
    local http_code; http_code=$(echo "$response" | tail -n1)
    local body; body=$(echo "$response" | head -n -1)

    if [ "$http_code" == "200" ]; then
        # 尝试解析响应以获取版本信息
        # 在Nacos中，版本信息可能在字段"Nacos-Server-Version"或其他类似字段中
        local version=$(echo "$body" | jq -r '.["Nacos-Server-Version"] // .version // ."nacos.version" // empty' 2>/dev/null)

        if [ -n "$version" ] && [ "$version" != "null" ] && [ "$version" != "" ]; then
            echo -e "${C_GREEN}Nacos 服务器版本信息:${C_NC}"
            echo -e "  版本号: ${C_YELLOW}$version${C_NC}"
            echo -e "  服务器地址: ${C_YELLOW}$NACOS_SERVER${C_NC}"
        else
            echo -e "${C_GREEN}Nacos 服务器状态:${C_NC}"
            echo -e "  服务器地址: ${C_YELLOW}$NACOS_SERVER${C_NC}"
            echo -e "  响应内容: $body"
        fi
    else
        echo -e "${C_YELLOW}标准状态接口不可用 (HTTP $http_code)，尝试其他方法...${C_NC}"

        # 尝试从响应头获取版本信息
        echo -e "${C_YELLOW}尝试从响应头获取服务器信息...${C_NC}"
        local root_url="http://$NACOS_SERVER$NACOS_CONTEXT_PATH/"
        local root_response=$(curl --noproxy "*" -s -w "\n%{http_code}" -X GET "$root_url")
        local response_http_code=$(echo "$root_response" | tail -n1)
        local response_body=$(echo "$root_response" | head -n -1)

        # 获取响应头
        local headers_response=$(curl --noproxy "*" -I -s -X GET "$root_url" 2>/dev/null)
        local server_header=$(echo "$headers_response" | grep -i "Server:" | head -n1 | awk -F': ' '{print $2}' | tr -d '\r\n')
        local powered_by=$(echo "$headers_response" | grep -i "X-Powered-By:" | head -n1 | awk -F': ' '{print $2}' | tr -d '\r\n')
        local nacos_version_header=$(echo "$headers_response" | grep -i "Nacos-Version:" | head -n1 | awk -F': ' '{print $2}' | tr -d '\r\n')

        echo -e "${C_GREEN}Nacos 服务器信息:${C_NC}"
        echo -e "  服务器地址: ${C_YELLOW}$NACOS_SERVER${C_NC}"
        if [ -n "$nacos_version_header" ]; then
            echo -e "  版本信息 (来自响应头): ${C_YELLOW}$nacos_version_header${C_NC}"
        fi
        if [ -n "$server_header" ]; then
            echo -e "  服务器标识: ${C_YELLOW}$server_header${C_NC}"
        fi
        if [ -n "$powered_by" ]; then
            echo -e "  技术栈信息: ${C_YELLOW}$powered_by${C_NC}"
        fi
        echo -e "  HTTP响应状态: ${C_YELLOW}$response_http_code${C_NC}"

        # 尝试健康检查接口
        local health_url="http://$NACOS_SERVER$NACOS_CONTEXT_PATH/v1/console/health"
        local health_response=$(curl --noproxy "*" -s -w "\n%{http_code}" -X GET "$health_url" 2>/dev/null)
        local health_http_code=$(echo "$health_response" | tail -n1)
        local health_body=$(echo "$health_response" | head -n -1)

        if [ "$health_http_code" == "200" ]; then
            echo -e "  服务健康状态: ${C_GREEN}正常${C_NC}"
        else
            echo -e "  服务健康状态: ${C_YELLOW}可能存在异常${C_NC}"
            echo -e "  HTTP状态码: ${C_YELLOW}$health_http_code${C_NC}"
        fi

        # 如果还是没找到版本信息，输出响应内容供诊断
        if [ -z "$nacos_version_header" ]; then
            echo -e "\n${C_YELLOW}响应内容示例 (检查是否存在版本信息):${C_NC}"
            echo "$response_body" | head -n 10
        fi
    fi
}

function login() {
  if [ -f "$ACCESS_TOKEN_FILE" ]; then
    local token_data; token_data=$(cat "$ACCESS_TOKEN_FILE")
    local token_ttl; token_ttl=$(echo "$token_data" | head -n 1)
    local token_stored_time; token_stored_time=$(echo "$token_data" | tail -n 1)
    local current_time; current_time=$(date +%s)
    local integer_regex='^[0-9]+$'

    if [[ $token_ttl =~ $integer_regex ]] && [[ $token_stored_time =~ $integer_regex ]]; then
        if [ $((current_time - token_stored_time)) -lt "$token_ttl" ]; then
            ACCESS_TOKEN=$(echo "$token_data" | sed -n '2p')
            if [ -n "$ACCESS_TOKEN" ]; then return; fi
        fi
    else
        echo -e "${C_YELLOW}Token 文件格式无效，将强制重新登录...${C_NC}"
    fi
  fi
  echo "正在登录 Nacos..."
  local login_url="http://$NACOS_SERVER$NACOS_CONTEXT_PATH$LOGIN_ENDPOINT_V3"
  echo "  登录URL: $login_url"
  local response; response=$(curl --noproxy "*" -s -X POST "$login_url" -d "username=$NACOS_USERNAME" -d "password=$NACOS_PASSWORD")
  ACCESS_TOKEN=$(echo "$response" | jq -r '.accessToken // empty')
  TOKEN_TTL=$(echo "$response" | jq -r '.tokenTtl // 18000')
  if [ -z "$ACCESS_TOKEN" ]; then 
    echo -e "${C_YELLOW}警告: 正在打印凭据用于调试...${C_NC}"
    echo "  服务器: $NACOS_SERVER"
    echo "  用户名: $NACOS_USERNAME"
    echo "  密码: $NACOS_PASSWORD"
    echo -e "${C_RED}Nacos 登录失败! 请检查用户名和密码.${C_NC}"
    echo "响应: $response"
    exit 1
  fi
  echo -e "$TOKEN_TTL\n$ACCESS_TOKEN\n$(date +%s)" > "$ACCESS_TOKEN_FILE"
  echo -e "${C_GREEN}登录成功 (Token 有效期: $TOKEN_TTL 秒)${C_NC}"
}

# 函数：构建 API URL 和参数
# 用法：build_api_params <api_version> <action> <dataId> <group> <namespaceId> <content>
# 返回：API URL 和参数（通过全局变量）
function build_api_params() {
  local api_version=$1
  local action=$2
  local dataId=$3
  local group=$4
  local namespaceId=$5
  local content=$6
  
  # 设置全局变量
  API_URL=""
  API_PARAMS=()
  
  if [ "$api_version" == "v3" ]; then
    API_URL="http://$NACOS_SERVER$NACOS_CONTEXT_PATH$CONFIGS_ENDPOINT_V3"
    API_PARAMS=("-d" "namespaceId=$namespaceId" "-d" "dataId=$dataId" "-d" "groupName=$group")
    if [ -n "$content" ]; then
      API_PARAMS+=("--data-urlencode" "content=$content")
    fi
  else
    # 默认使用 v1
    API_URL="http://$NACOS_SERVER$NACOS_CONTEXT_PATH$CONFIGS_ENDPOINT_V1"
    API_PARAMS=("-d" "tenant=$namespaceId" "-d" "dataId=$dataId" "-d" "group=$group")
    if [ -n "$content" ]; then
      API_PARAMS+=("--data-urlencode" "content=$content")
    fi
  fi
}

# --- 单点操作函数 ---
function single_op() {
    local action=$1; local namespace=$2; local group=$3; local dataId=$4; local api_version=${5:-v1}
    
    # 灵活查找本地文件
    local found_files
    mapfile -t found_files < <(find "$CONFIG_DIR" -type f -name "$dataId")
    local local_file_path=""
    if [ ${#found_files[@]} -gt 1 ]; then
        echo -e "${C_YELLOW}警告: 在 '$CONFIG_DIR' 中找到多个名为 '$dataId' 的文件。将使用第一个:${C_NC}"
        echo "  ${found_files[0]}"
        local_file_path="${found_files[0]}"
    elif [ ${#found_files[@]} -eq 1 ]; then
        local_file_path="${found_files[0]}"
    fi

    local profile_name=""
    # 检查 dataId 是否包含 '-' 且不以 '-' 开头或结尾
    if [[ "$dataId" == *'-'* ]]; then
        # 1. 截取 . 左边的字符串
        local base_name=$(echo "$dataId" | cut -d'.' -f1)
        # 2. 截取最后一个 '-' 右边的字符串作为 profile 名称
        profile_name=$(echo "$base_name" | awk -F'-' '{print $NF}')
    fi

    local target_namespace_id=""
    if [ -n "$profile_name" ]; then
        # 优先从 pom.xml 解析获取 Namespace ID
        target_namespace_id=$(get_namespace_id_from_pom "$profile_name")

        if [ -z "$target_namespace_id" ]; then
            # 如果 pom.xml 也未解析到，则默认使用 public
            echo -e "${C_YELLOW}警告: 未能从 pom.xml 解析到 profile '$profile_name' 的 Nacos Namespace ID，将默认使用 'public'。${C_NC}"
            target_namespace_id="public"
        fi
    else
        # 如果 dataId 不包含 profile，则使用默认 Namespace (PARAM_NAMESPACE)
        target_namespace_id="$namespace"
    fi

    if [ -z "$target_namespace_id" ]; then
        echo -e "${C_RED}错误: 无法确定 Nacos Namespace ID。${C_NC}"
        exit 1
    fi

    # 构建 API URL 和参数
    build_api_params "$api_version" "$action" "$dataId" "$group" "$target_namespace_id" ""
    
    echo -e "\n${C_BLUE}--- [单点模式] $action (API: $api_version) ---${C_NC}"
    
    local display_namespace_name="${profile_name:-public}"
    if [ "$target_namespace_id" == "public" ]; then
        display_namespace_name="public"
    fi
    echo -e "Namespace: ${C_YELLOW}$display_namespace_name${C_NC} (ID: ${C_YELLOW}$target_namespace_id${C_NC}), Group: ${C_YELLOW}$group${C_NC}, DataID: ${C_YELLOW}$dataId${C_NC}"


    # 获取远程内容
    # 添加短暂延迟，确保能获取到最新配置
    if [ "$action" == "compare" ] && [ "$PARAM_FORCE_PUSH" = true ]; then
        echo -e "${C_YELLOW}等待配置更新...${C_NC}"
        sleep 1
    fi
    
    # 构建 API URL 和参数
    local get_url
    local get_params_str
    if [ "$api_version" == "v3" ]; then
        get_url="http://$NACOS_SERVER$NACOS_CONTEXT_PATH$CONFIGS_ENDPOINT_V3"
        get_params_str="dataId=$dataId&groupName=$group&namespaceId=$target_namespace_id&accessToken=$ACCESS_TOKEN"
    else
        get_url="http://$NACOS_SERVER$NACOS_CONTEXT_PATH$CONFIGS_ENDPOINT_V1"
        get_params_str="dataId=$dataId&group=$group&tenant=$target_namespace_id&accessToken=$ACCESS_TOKEN"
    fi
    
    # 输出 API 命令
    log_api_command "GET" "$get_url?$get_params_str"
    
    local remote_response; remote_response=$(curl --noproxy "*" -s -w "\n%{http_code}" -X GET "$get_url?$get_params_str")
    local remote_http_code; remote_http_code=$(echo "$remote_response" | tail -n1)
    local remote_content; remote_content=$(echo "$remote_response" | head -n -1)
    
    # 输出 API 响应
    log_api_response "$remote_http_code" "$remote_content"

    if [ "$remote_http_code" == "404" ]; then
        remote_content=""
        echo "(远程配置不存在)"
    elif [ "$remote_http_code" != "200" ]; then
        echo -e "${C_RED}获取远程配置失败 (HTTP $remote_http_code): $remote_content${C_NC}"; exit 1
    fi

    case "$action" in
        "pull")
            local pull_destination=${local_file_path:-${CONFIG_DIR}/${namespace}/${group}/${dataId}}
            mkdir -p "$(dirname "$pull_destination")"
            echo -n "$remote_content" > "$pull_destination"
            echo -e "${C_GREEN}配置已成功拉取到: $pull_destination${C_NC}"
            ;;
        "push")
            if [ -z "$local_file_path" ]; then echo -e "${C_RED}错误: 在 '$CONFIG_DIR' 目录中找不到名为 '$dataId' 的本地文件!${C_NC}"; exit 1; fi
            local local_content; local_content=$(cat "$local_file_path")

            # 如果启用删除后推送，先删除远程配置
            if [ "$PARAM_DELETE_BEFORE_PUSH" = true ]; then
                echo -e "${C_YELLOW}删除远程配置...${C_NC}"
                
                # 构建删除参数
                local delete_params=()
                if [ "$api_version" == "v3" ]; then
                    delete_params=("accessToken=$ACCESS_TOKEN" "namespaceId=$target_namespace_id" "groupName=$group" "dataId=$dataId")
                else
                    delete_params=("accessToken=$ACCESS_TOKEN" "tenant=$target_namespace_id" "group=$group" "dataId=$dataId")
                fi
                
                # 输出删除 API 命令
                log_api_command "DELETE" "$API_URL?${delete_params[*]}"
                
                local delete_response; delete_response=$(curl --noproxy "*" -s -w "\n%{http_code}" -X DELETE "$API_URL?${delete_params[*]}")
                local delete_http_code; delete_http_code=$(echo "$delete_response" | tail -n1)
                local delete_body; delete_body=$(echo "$delete_response" | head -n -1)
                
                # 输出删除 API 响应
                log_api_response "$delete_http_code" "$delete_body"
                
                if [ "$delete_http_code" == "200" ]; then
                    echo -e "${C_GREEN}远程配置删除成功${C_NC}"
                else
                    echo -e "${C_YELLOW}远程配置可能不存在或删除失败 (HTTP $delete_http_code)${C_NC}"
                fi
                # 短暂延迟确保删除操作完成
                sleep 0.5
            fi

            # 构建推送参数
            build_api_params "$api_version" "push" "$dataId" "$group" "$target_namespace_id" "$local_content"
            
            # 如果启用强制推送，添加updateForExist参数
            if [ "$PARAM_FORCE_PUSH" = true ]; then
                API_PARAMS+=("-d" "updateForExist=true")
                echo -e "${C_YELLOW}强制推送模式已启用，将覆盖已存在的配置${C_NC}"
            fi
            
            # 输出推送 API 命令
            echo -e "${C_YELLOW}推送配置内容大小: $(echo -n "$local_content" | wc -c) 字节${C_NC}"
            log_api_command "POST" "$API_URL?accessToken=$ACCESS_TOKEN" "${API_PARAMS[@]}"

            local push_response; push_response=$(curl --noproxy "*" -s -w "\n%{http_code}" -X POST "$API_URL?accessToken=$ACCESS_TOKEN" "${API_PARAMS[@]}")
            local push_http_code; push_http_code=$(echo "$push_response" | tail -n1)
            local push_body; push_body=$(echo "$push_response" | head -n -1)
            
            # 输出推送 API 响应
            log_api_response "$push_http_code" "$push_body"
            
            # v3 API 返回格式: {"code":0,"message":"success","data":true}
            # v1 API 返回格式: true
            if [ "$push_http_code" == "200" ]; then
                # 检查 v3 API 响应格式
                local success=$(echo "$push_body" | jq -r '.data // . // empty' 2>/dev/null)
                if [ "$success" == "true" ] || [ "$push_body" == "true" ]; then 
                # 获取配置文件的绝对路径
                local  absolute_path=$(realpath "$local_file_path" 2>/dev/null || echo "$local_file_path")
                
                # 验证推送是否真正成功
                echo -e "${C_YELLOW}验证推送结果...${C_NC}"
                sleep 1  # 等待配置生效
                
                # 构建验证参数字符串
                local verify_params_str=""
                if [ "$api_version" == "v3" ]; then
                    verify_params_str="dataId=$dataId&groupName=$group&namespaceId=$target_namespace_id&accessToken=$ACCESS_TOKEN"
                else
                    verify_params_str="dataId=$dataId&group=$group&tenant=$target_namespace_id&accessToken=$ACCESS_TOKEN"
                fi
                
                # 输出验证 API 命令
                log_api_command "GET" "$API_URL?$verify_params_str"
                
                local verify_response; verify_response=$(curl --noproxy "*" -s -w "\n%{http_code}" -X GET "$API_URL?$verify_params_str")
                local verify_http_code; verify_http_code=$(echo "$verify_response" | tail -n1)
                local verify_content; verify_content=$(echo "$verify_response" | head -n -1)
                
                # 输出验证 API 响应（仅显示状态码，不显示完整内容）
                echo -e "${C_BLUE}--- 验证响应 ---${C_NC}"
                if [ "$verify_http_code" == "200" ]; then
                    echo -e "${C_GREEN}验证成功 (HTTP $verify_http_code)${C_NC}"
                    echo -e "${C_YELLOW}远程配置内容大小: $(echo -n "$verify_content" | wc -c) 字节${C_NC}"
                else
                    echo -e "${C_RED}验证失败 (HTTP $verify_http_code)${C_NC}"
                fi
                echo -e "${C_BLUE}-------------${C_NC}\n"
                
                if [ "$verify_http_code" == "200" ]; then
                    # v3 API 返回格式: {"code":0,"message":"success","data":{"content":"...","dataId":"...",...}}
                    # v1 API 返回格式: 直接是配置内容
                    
                    local remote_config_content=""
                    if [ "$api_version" == "v3" ]; then
                        # 提取 content 字段
                        remote_config_content=$(echo "$verify_content" | jq -r '.data.content // empty' 2>/dev/null)
                    else
                        # v1 API 直接返回内容
                        remote_config_content="$verify_content"
                    fi
                    
                    if [ -n "$remote_config_content" ]; then
                        # 比较本地和远程内容是否一致
                        local temp_local; temp_local=$(mktemp)
                        echo -n "$local_content" > "$temp_local"
                        local temp_remote; temp_remote=$(mktemp)
                        echo -n "$remote_config_content" > "$temp_remote"
                        
                        if diff -q "$temp_local" "$temp_remote" > /dev/null 2>&1; then
                            if [ "$PARAM_FORCE_PUSH" = true ]; then
                                echo -e "${C_GREEN}配置文件强制推送成功: $absolute_path${C_NC}"
                            else
                                echo -e "${C_GREEN}配置文件推送nacos成功: $absolute_path${C_NC}"
                            fi
                        else
                            echo -e "${C_YELLOW}警告: 推送成功但内容不一致，可能是Nacos服务端问题${C_NC}"
                            echo -e "${C_YELLOW}建议使用 --delete-before-push 选项${C_NC}"
                        fi
                        rm -f "$temp_local" "$temp_remote"
                    else
                        echo -e "${C_YELLOW}警告: 无法解析远程配置内容${C_NC}"
                    fi
                else
                    echo -e "${C_YELLOW}警告: 无法验证推送结果 (HTTP $verify_http_code)${C_NC}"
                fi
            else 
                echo -e "${C_RED}推送失败 (HTTP $push_http_code): $push_body${C_NC}"; exit 1; 
            fi
            fi
            ;;
        "compare")
            local remote_temp_file; remote_temp_file=$(mktemp)
            trap 'rm -f -- "$remote_temp_file"' EXIT
            echo -n "$remote_content" > "$remote_temp_file"
            if [ -z "$local_file_path" ]; then
                echo -e "${C_YELLOW}警告: 在 '$CONFIG_DIR' 目录中找不到名为 '$dataId' 的本地文件。将只显示远程内容。${C_NC}"
                echo -e "${C_BLUE}--- 远程配置 ---${C_NC}\n$remote_content"
            else
                echo -e "${C_BLUE}--- 对比差异 (本地 vs 远程) ---${C_NC}"
                diff -u "$local_file_path" "$remote_temp_file"
                local diff_result=$?
                if [ $diff_result -eq 0 ]; then
                    echo -e "${C_GREEN}本地与远程配置一致。${C_NC}"
                else
                    # 检查是否存在远程配置
                    local remote_exists=true
                    if [ "$remote_http_code" == "404" ]; then
                        remote_exists=false
                        echo -e "${C_YELLOW}远程配置不存在，为安全推送的候选文件${C_NC}"
                    fi

                    echo -e "\n${C_YELLOW}差异说明:${C_NC}"
                    echo -e "${C_YELLOW}  左边 (---): 本地配置文件${C_NC}"
                    echo -e "${C_YELLOW}  右边 (+++): Nacos远程配置${C_NC}"
                    echo -e "${C_YELLOW}提示: 您可以运行以下命令使用可视化工具查看差异:${C_NC}"
                    echo -e "${C_BLUE}  git difftool --no-prompt \"$local_file_path\" \"$remote_temp_file\"${C_NC}"
                    # 检查是否设置了 --safe-push 参数
                    if [ "$PARAM_SAFE_PUSH" = true ]; then
                        if [ "$remote_exists" = false ]; then
                            echo -e "${C_YELLOW}--safe-push 参数已设置，正在推送远程不存在的配置文件...${C_NC}"
                            # 调用推送操作
                            single_op "push" "$namespace" "$group" "$dataId"
                        else
                            echo -e "${C_YELLOW}--safe-push 参数已设置，但远程配置已存在，跳过推送...${C_NC}"
                        fi
                    elif [ "$PARAM_NO_DIFF" = true ]; then
                        echo -e "${C_YELLOW}--no-diff 参数已设置，跳过可视化差异工具...${C_NC}"
                    else
                        # 询问用户是否要自动打开difftool，默认为y
                        read -p "是否要自动打开可视化差异工具? (Y/n): " -n 1 -r
                        echo
                        if [[ ! $REPLY =~ ^[Nn]$ ]]; then
                            echo -e "${C_YELLOW}正在尝试打开差异工具...${C_NC}"
                            # 尝试使用git difftool，如果失败则提供备选方案
                            if ! git difftool --no-prompt "$local_file_path" "$remote_temp_file"; then
                                echo -e "${C_RED}默认的diff工具无法使用${C_NC}"
                                echo -e "${C_YELLOW}提供以下备选方案:${C_NC}"

                                # 检查可用的工具
                                local options=()
                                if command -v vimdiff >/dev/null 2>&1; then
                                    options+=("vimdiff")
                                    echo -e "${C_BLUE}1. 使用vimdiff: vimdiff \"$local_file_path\" \"$remote_temp_file\"${C_NC}"
                                fi

                                if command -v meld >/dev/null 2>&1; then
                                    options+=("meld")
                                    echo -e "${C_BLUE}2. 使用meld: meld \"$local_file_path\" \"$remote_temp_file\"${C_NC}"
                                fi

                                if command -v vim >/dev/null 2>&1; then
                                    options+=("vim")
                                    echo -e "${C_BLUE}3. 使用vim: vim -d \"$local_file_path\" \"$remote_temp_file\"${C_NC}"
                                fi

                                echo -e "${C_BLUE}$(( ${#options[@]} + 1 )). 直接显示差异: diff -u \"$local_file_path\" \"$remote_temp_file\"${C_NC}"
                                echo -e "${C_BLUE}$(( ${#options[@]} + 3 )). 退出${C_NC}"

                                read -p "请选择操作 (1-$(( ${#options[@]} + 3 )), 默认为直接显示差异): " choice

                                # 默认选项是直接显示差异
                                if [[ -z "$choice" ]]; then
                                    choice=$(( ${#options[@]} + 1 ))
                                fi

                                # 执行选择的操作
                                case $choice in
                                    1)
                                        if [ ${#options[@]} -gt 0 ]; then
                                            case ${options[0]} in
                                                "vimdiff") vimdiff "$local_file_path" "$remote_temp_file" ;;
                                                "meld") meld "$local_file_path" "$remote_temp_file" ;;
                                                "vim") vim -d "$local_file_path" "$remote_temp_file" ;;
                                            esac
                                        else
                                            diff -u "$local_file_path" "$remote_temp_file"
                                        fi
                                        ;;
                                    2)
                                        if [ ${#options[@]} -gt 1 ]; then
                                            case ${options[1]} in
                                                "vimdiff") vimdiff "$local_file_path" "$remote_temp_file" ;;
                                                "meld") meld "$local_file_path" "$remote_temp_file" ;;
                                                "vim") vim -d "$local_file_path" "$remote_temp_file" ;;
                                            esac
                                        elif [ ${#options[@]} -eq 1 ]; then
                                            diff -u "$local_file_path" "$remote_temp_file"
                                        else
                                            echo -e "${C_YELLOW}请运行以下命令安装 meld:${C_NC}"; echo "sudo apt-get install meld"
                                        fi
                                        ;;
                                    3)
                                        if [ ${#options[@]} -gt 2 ]; then
                                            case ${options[2]} in
                                                "vimdiff") vimdiff "$local_file_path" "$remote_temp_file" ;;
                                                "meld") meld "$local_file_path" "$remote_temp_file" ;;
                                                "vim") vim -d "$local_file_path" "$remote_temp_file" ;;
                                            esac
                                        elif [ ${#options[@]} -eq 2 ]; then
                                            diff -u "$local_file_path" "$remote_temp_file"
                                        elif [ ${#options[@]} -eq 1 ]; then
                                            echo -e "${C_YELLOW}请运行以下命令安装 meld:${C_NC}"; echo "sudo apt-get install meld"
                                        else
                                            echo "已退出"
                                        fi
                                        ;;
                                    4)
                                        if [ ${#options[@]} -gt 2 ]; then
                                            diff -u "$local_file_path" "$remote_temp_file"
                                        elif [ ${#options[@]} -eq 2 ]; then
                                            echo -e "${C_YELLOW}请运行以下命令安装 meld:${C_NC}"; echo "sudo apt-get install meld"
                                        elif [ ${#options[@]} -eq 1 ]; then
                                            echo -e "${C_YELLOW}请运行以下命令安装 meld:${C_NC}"; echo "sudo apt-get install meld"
                                        else
                                            echo "已退出"
                                        fi
                                        ;;
                                    5)
                                        if [ ${#options[@]} -gt 2 ]; then
                                            echo -e "${C_YELLOW}请运行以下命令安装 meld:${C_NC}"; echo "sudo apt-get install meld"
                                        elif [ ${#options[@]} -eq 2 ]; then
                                            echo -e "${C_YELLOW}请运行以下命令安装 meld:${C_NC}"; echo "sudo apt-get install meld"
                                        else
                                            echo "已退出"
                                        fi
                                        ;;
                                    *)
                                        diff -u "$local_file_path" "$remote_temp_file"
                                        ;;
                                esac
                            fi
                        fi
                    fi
                fi
            fi
            ;;
    esac
}

# --- 批量操作函数 ---
function bulk_op() {
    local action=$1; local namespace=$2; local group=$3; local delete_flag=$4; local api_version=${5:-v1}
    local temp_dir; temp_dir=$(mktemp -d); trap 'rm -rf -- "$temp_dir"' EXIT
    local remote_dir="$temp_dir/$namespace/$group"; mkdir -p "$remote_dir"
    local local_dir="$CONFIG_DIR/$namespace/$group"; mkdir -p "$local_dir"

    echo -e "\n${C_BLUE}--- [批量模式] $action (API: $api_version) ---${C_NC}"
    echo "本地目录: $local_dir"
    echo "远程参数: Namespace=${C_YELLOW}$namespace${C_NC}, Group=${C_YELLOW}$group${C_NC}\n"

    # 构建导出 URL
    local export_url
    if [ "$api_version" == "v3" ]; then
        export_url="http://$NACOS_SERVER$NACOS_CONTEXT_PATH$CONFIGS_ENDPOINT_V3?export=true&groupName=$group&namespaceId=$namespace&accessToken=$ACCESS_TOKEN"
    else
        export_url="http://$NACOS_SERVER$NACOS_CONTEXT_PATH$CONFIGS_ENDPOINT_V1?export=true&group=$group&tenant=$namespace&accessToken=$ACCESS_TOKEN"
    fi
    
    local zip_file="$temp_dir/nacos_export.zip"
    local http_code; http_code=$(curl --noproxy "*" -s -w '%{http_code}' -X GET "$export_url" -o "$zip_file")
    if [ "$http_code" != "200" ]; then echo -e "${C_RED}批量导出失败 (HTTP $http_code)!${C_NC}"; exit 1; fi
    if [ -s "$zip_file" ] && unzip -l "$zip_file" &>/dev/null; then unzip -q -o "$zip_file" -d "$remote_dir"; fi
    rm "$zip_file"

    case "$action" in
    	"-h")
    		print_help; exit 0 ;;
        "compare")
            # 检查是否启用安全推送模式
            if [ "$PARAM_SAFE_PUSH" = true ]; then
                echo -e "${C_YELLOW}批量安全推送模式：仅推送远程不存在的配置文件${C_NC}"
                # 遍历本地文件，检查哪些在远程不存在，然后推送
                find "$local_dir" -type f | while read -r local_file; do
                    local dataId=$(basename "$local_file")
                    local relative_path=$(realpath --relative-to="$local_dir" "$local_file")
                    local remote_file_path="$remote_dir/$relative_path"

                    if [ ! -f "$remote_file_path" ]; then
                        echo -e "${C_YELLOW}检测到远程不存在的配置文件: $dataId${C_NC}"
                        # 推送本地文件到远程
                        single_op "push" "$namespace" "$group" "$dataId"
                    else
                        echo -e "${C_GREEN}配置文件已存在: $dataId${C_NC}"
                    fi
                done
            else
                # 正常比较模式
                diff -r --brief "$local_dir" "$remote_dir" || true
            fi
            ;;
        "pull")
            read -p "确认要将远程配置覆盖到本地吗? (y/n): " confirm
            if [ "$confirm" == "y" ]; then rsync -a --delete "$remote_dir/" "$local_dir/"; echo -e "${C_GREEN}配置已从 Nacos 更新到本地。${C_NC}"; else echo "操作已取消。"; fi
            ;;
        "push")
            if [ "$PARAM_FORCE_PUSH" = true ]; then
                echo -e "${C_YELLOW}强制推送模式已启用，将覆盖所有已存在的配置${C_NC}"
                read -p "确认要强制推送本地配置到 Nacos 吗? (y/n): " confirm
            else
                read -p "确认要将本地配置推送到 Nacos 吗? (y/n): " confirm
            fi
            if [ "$confirm" == "y" ]; then
                find "$local_dir" -type f | while read -r file; do
                    local dataId; dataId=$(basename "$file"); local push_group; push_group=$(basename "$(dirname "$file")")
                    single_op "push" "$namespace" "$push_group" "$dataId"
                done
                if [ "$delete_flag" = true ]; then
                    echo "检查需要删除的配置..."
                    comm -13 <(find "$local_dir" -type f -printf '%P\n' | sort) <(find "$remote_dir" -type f -printf '%P\n' | sort) | while read -r file; do
                        local dataId; dataId=$(basename "$file"); local push_group; push_group=$(basename "$(dirname "$file")")
                        
                        # 构建删除 URL
                        local delete_url
                        if [ "$api_version" == "v3" ]; then
                            delete_url="http://$NACOS_SERVER$NACOS_CONTEXT_PATH$CONFIGS_ENDPOINT_V3?accessToken=$ACCESS_TOKEN&namespaceId=$namespace&groupName=$push_group&dataId=$dataId"
                        else
                            delete_url="http://$NACOS_SERVER$NACOS_CONTEXT_PATH$CONFIGS_ENDPOINT_V1?accessToken=$ACCESS_TOKEN&tenant=$namespace&group=$push_group&dataId=$dataId"
                        fi
                        
                        curl --noproxy "*" -s -X DELETE "$delete_url" > /dev/null
                        echo -e "${C_YELLOW}已删除远程配置: $file${C_NC}"
                    done
                fi
                if [ "$PARAM_FORCE_PUSH" = true ]; then
                    echo -e "${C_GREEN}批量强制推送完成。${C_NC}"
                else
                    echo -e "${C_GREEN}批量推送完成。${C_NC}"
                fi
            else
                echo "操作已取消。"
            fi
            ;;
    esac
}

# --- 主逻辑 ---

# 检查是否是帮助命令
if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    print_help
    exit 0
fi

ACTION=${1?"错误: 未提供操作命令 (pull, push, compare, version)。"}
shift

PARAM_DATA_ID=""
PARAM_NAMESPACE="public"
PARAM_GROUP="DEFAULT_GROUP"
PARAM_API_VERSION="v1"
PARAM_DELETE=false
PARAM_NO_DIFF=false
PARAM_SAFE_PUSH=false
PARAM_FORCE_PUSH=false
PARAM_DELETE_BEFORE_PUSH=false
PARAM_CLEAR_CACHE=false
POSITIONAL_ARGS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dataId) PARAM_DATA_ID="$2"; shift 2 ;;
    --namespace) PARAM_NAMESPACE="$2"; shift 2 ;;
    --group) PARAM_GROUP="$2"; shift 2 ;;
    --api-version) PARAM_API_VERSION="$2"; shift 2 ;;
    --delete) PARAM_DELETE=true; shift ;;
    --no-diff) PARAM_NO_DIFF=true; shift ;;
    --safe-push) PARAM_SAFE_PUSH=true; shift ;;
    --force-push) PARAM_FORCE_PUSH=true; shift ;;
    --delete-before-push) PARAM_DELETE_BEFORE_PUSH=true; shift ;;
    --clear-cache) PARAM_CLEAR_CACHE=true; shift ;;
    -h|--help) print_help; exit 0 ;;
    -d|--config-dir) CONFIG_DIR="$2"; shift 2 ;;
    -*) echo "错误: 未知选项 $1"; print_help; exit 1 ;;
    *) POSITIONAL_ARGS+=("$1"); shift ;;
  esac
done

# 验证 API 版本
if ! [[ "$PARAM_API_VERSION" =~ ^(v1|v3)$ ]]; then
  echo -e "${C_RED}错误: 无效的 API 版本 '$PARAM_API_VERSION'。请使用 v1 或 v3。${C_NC}"
  print_help
  exit 1
fi

if ! [[ "$ACTION" =~ ^(pull|push|compare|clear-cache|version)$ ]]; then
    echo -e "${C_RED}错误: 无效的操作命令 '$ACTION'。请使用 pull, push, compare, clear-cache 或 version。${C_NC}"
    print_help
    exit 1
fi

check_deps
check_config_dir

if [ "$ACTION" == "version" ]; then
    check_connection
    get_nacos_version
elif [ "$ACTION" == "clear-cache" ]; then
    clear_nacos_cache
else
    check_connection
    login

    if [ -n "$PARAM_DATA_ID" ] && [[ "$PARAM_DATA_ID" != *.* ]]; then
        PARAM_DATA_ID="$PARAM_DATA_ID.yaml"
        echo -e "${C_YELLOW}提示: dataId 未指定后缀，已自动添加 .yaml 后缀。处理ID: $PARAM_DATA_ID${C_NC}"
    fi

    if [ -n "$PARAM_DATA_ID" ]; then
        if [ ${#POSITIONAL_ARGS[@]} -ne 0 ]; then
            echo -e "${C_RED}错误: 在单点模式下 (--dataId 已指定) 不应使用位置参数。${C_NC}"
            print_help
            exit 1
        fi
        single_op "$ACTION" "$PARAM_NAMESPACE" "$PARAM_GROUP" "$PARAM_DATA_ID" "$PARAM_API_VERSION"
    else
        if [ ${#POSITIONAL_ARGS[@]} -gt 0 ]; then PARAM_NAMESPACE=${POSITIONAL_ARGS[0]}; fi
        if [ ${#POSITIONAL_ARGS[@]} -gt 1 ]; then PARAM_GROUP=${POSITIONAL_ARGS[1]}; fi
        bulk_op "$ACTION" "$PARAM_NAMESPACE" "$PARAM_GROUP" "$PARAM_DELETE" "$PARAM_API_VERSION"
    fi
fi