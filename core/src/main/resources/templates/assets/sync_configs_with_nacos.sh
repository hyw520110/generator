#!/bin/bash

# 脚本功能：先对比本地配置文件和 Nacos 上的配置文件，然后根据用户选择决定是否推送
# 用法: ./sync_configs_with_nacos.sh [profile]

set -e  # 遇到错误时退出

# --- 颜色定义 ---
C_RED=$(tput setaf 1)
C_GREEN=$(tput setaf 2)
C_YELLOW=$(tput setaf 3)
C_BLUE=$(tput setaf 4)
C_NC=$(tput sgr0)

# --- 配置 ---
CONFIG_DIR=${CONFIG_DIR:-"config"}
NACOS_CLI_SCRIPT="./nacos-cli.sh"

# 检查 nacos-cli.sh 是否存在
if [[ ! -f "$NACOS_CLI_SCRIPT" ]]; then
    echo "${C_RED}错误: $NACOS_CLI_SCRIPT 不存在${C_NC}" >&2
    exit 1
fi

# 检查配置目录是否存在
if [[ ! -d "$CONFIG_DIR" ]]; then
    echo "${C_RED}错误: 配置目录 $CONFIG_DIR 不存在${C_NC}" >&2
    exit 1
fi

# 获取要同步的 profile，默认为 dev
PROFILE=${1:-"dev"}
echo "${C_BLUE}正在同步 $PROFILE 环境的本地配置与 Nacos 配置...${C_NC}"

# 获取要同步的命名空间ID，根据profile自动识别
NAMESPACE_ID=$(./nacos-cli.sh get-namespace "$PROFILE")
if [[ -z "$NAMESPACE_ID" ]]; then
    echo "${C_YELLOW}警告: 未能自动获取 $PROFILE 环境的命名空间ID${C_NC}"
    echo "请输入 $PROFILE 环境的命名空间ID (Namespace ID): "
    read -r NAMESPACE_ID
    if [[ -z "$NAMESPACE_ID" ]]; then
        echo "${C_RED}错误: 命名空间ID不能为空${C_NC}" >&2
        exit 1
    fi
fi

echo "${C_BLUE}使用命名空间ID: $NAMESPACE_ID${C_NC}"

# 用于统计对比结果
total_configs=0
matched_configs=0
mismatched_configs=()
missing_on_nacos=()
missing_on_local=()

# 临时文件目录
TEMP_DIR=$(mktemp -d)
trap 'rm -rf "$TEMP_DIR"' EXIT

# 函数：对比单个配置文件
compare_config_file() {
    local file_path="$1"
    local file_name=$(basename "$file_path")
    local dir_name=$(basename "$(dirname "$file_path")")
    
    # 确定 dataId 和 group
    local data_id
    local group
    
    if [[ "$dir_name" != "config" ]]; then
        # 服务特定配置: config/service-name/service-name-dev.yaml
        if [[ "$file_name" == "${dir_name}"-* ]]; then
            # 文件名以服务名开头，如 service-name-dev.yaml
            data_id="$file_name"
            group="${dir_name^^}"  # 服务名作为group，转为大写
        else
            # 文件名不以服务名开头
            data_id="${dir_name}-${file_name}"
            group="${dir_name^^}"  # 服务名作为group，转为大写
        fi
    else
        # 通用配置: config/application-dev.yaml
        data_id="$file_name"
        group="DEFAULT_GROUP"
    fi
    
    echo "${C_BLUE}正在对比配置: $file_path [dataId: $data_id, group: $group]${C_NC}"
    
    # 获取 Nacos 上的配置内容
    local nacos_config_file="$TEMP_DIR/${data_id}_${group}.nacos"
    if "$NACOS_CLI_SCRIPT" get "$data_id" "$group" "$NAMESPACE_ID" > "$nacos_config_file" 2>/dev/null; then
        # 比较本地文件与 Nacos 文件
        if cmp -s "$file_path" "$nacos_config_file"; then
            echo "${C_GREEN}✓ 配置 $data_id 本地与 Nacos 一致${C_NC}"
            ((matched_configs++))
        else
            echo "${C_YELLOW}⚠ 配置 $data_id 本地与 Nacos 不一致${C_NC}"
            mismatched_configs+=("$file_path")
        fi
    else
        echo "${C_RED}⚠ 配置 $data_id 在 Nacos 上不存在${C_NC}"
        missing_on_nacos+=("$file_path")
    fi
    
    ((total_configs++))
}

# 函数：推送单个配置文件
push_config_file() {
    local file_path="$1"
    local file_name=$(basename "$file_path")
    local dir_name=$(basename "$(dirname "$file_path")")
    
    # 确定 dataId 和 group
    local data_id
    local group
    
    if [[ "$dir_name" != "config" ]]; then
        # 服务特定配置: config/service-name/service-name-dev.yaml
        if [[ "$file_name" == "${dir_name}"-* ]]; then
            # 文件名以服务名开头，如 service-name-dev.yaml
            data_id="$file_name"
            group="${dir_name^^}"  # 服务名作为group，转为大写
        else
            # 文件名不以服务名开头
            data_id="${dir_name}-${file_name}"
            group="${dir_name^^}"  # 服务名作为group，转为大写
        fi
    else
        # 通用配置: config/application-dev.yaml
        data_id="$file_name"
        group="DEFAULT_GROUP"
    fi
    
    echo "${C_BLUE}正在推送配置: $file_path [dataId: $data_id, group: $group]${C_NC}"
    
    if "$NACOS_CLI_SCRIPT" publish "$data_id" "$group" "$NAMESPACE_ID" "$file_path"; then
        echo "${C_GREEN}✓ 配置 $data_id 推送成功${C_NC}"
    else
        echo "${C_RED}✗ 配置 $data_id 推送失败${C_NC}"
        return 1
    fi
}

# 查找并对比所有配置文件
echo "${C_BLUE}开始查找配置文件并进行对比...${C_NC}"

while IFS= read -r -d '' file; do
    # 检查是否匹配指定的 profile
    if [[ "$file" == *"-${PROFILE}.yaml" || "$file" == *"-${PROFILE}.yml" ]]; then
        compare_config_file "$file"
    fi
done < <(find "$CONFIG_DIR" -name "*-${PROFILE}.yaml" -o -name "*-${PROFILE}.yml" -print0)

# 还需要处理一些不以 profile 结尾但包含 profile 的文件
while IFS= read -r -d '' file; do
    # 避免重复处理已经在上面处理过的文件
    if [[ ! "$file" == *"-${PROFILE}.yaml" && ! "$file" == *"-${PROFILE}.yml" ]]; then
        # 检查文件名是否包含 profile
        basename_file=$(basename "$file")
        if [[ "$basename_file" == *"$PROFILE"* ]]; then
            compare_config_file "$file"
        fi
    fi
done < <(find "$CONFIG_DIR" -name "*.yaml" -o -name "*.yml" -print0)

# 检查Nacos上有哪些配置在本地没有
echo "${C_BLUE}正在检查 Nacos 上存在但本地缺少的配置...${C_NC}"

# 获取 Nacos 上的所有配置列表
nacos_configs_json="$TEMP_DIR/nacos_configs.json"
"$NACOS_CLI_SCRIPT" list "DEFAULT_GROUP" "$NAMESPACE_ID" > "$nacos_configs_json" 2>/dev/null || echo "[]" > "$nacos_configs_json"

# 通过jq提取数据，如果没有jq则跳过此步骤
if command -v jq >/dev/null 2>&1; then
    # 从 Nacos 配置列表中提取 dataId 并与本地文件比较
    while IFS= read -r data_id; do
        # 检查本地是否有对应的文件
        found=0
        for local_file in $(find "$CONFIG_DIR" -name "*.yaml" -o -name "*.yml"); do
            local file_name=$(basename "$local_file")
            local dir_name=$(basename "$(dirname "$local_file")")
            
            # 确定本地文件对应的 dataId
            local local_data_id
            if [[ "$dir_name" != "config" ]]; then
                if [[ "$file_name" == "${dir_name}"-* ]]; then
                    local local_data_id="$file_name"
                else
                    local local_data_id="${dir_name}-${file_name}"
                fi
            else
                local local_data_id="$file_name"
            fi
            
            if [[ "$local_data_id" == "$data_id" ]]; then
                found=1
                break
            fi
        done
        
        if [[ $found -eq 0 ]]; then
            missing_on_local+=("$data_id")
            echo "${C_YELLOW}⚠ 配置 $data_id 在本地不存在${C_NC}"
        fi
    done < <(jq -r '.data?.pageItems[]?.dataId // empty' "$nacos_configs_json" 2>/dev/null)
else
    echo "${C_YELLOW}警告: 未安装 jq，跳过检查 Nacos 上存在但本地缺少的配置...${C_NC}"
fi

echo
echo "${C_BLUE}对比完成总结:${C_NC}"
echo "总配置文件数: $total_configs"
echo "匹配的配置数: $matched_configs"
echo "不匹配的配置数: ${#mismatched_configs[@]}"
echo "本地有但 Nacos 没有的配置数: ${#missing_on_nacos[@]}"
echo "Nacos 有但本地没有的配置数: ${#missing_on_local[@]}"

if [ ${#mismatched_configs[@]} -gt 0 ]; then
    echo
    echo "${C_YELLOW}不匹配的配置文件:${C_NC}"
    for mismatch in "${mismatched_configs[@]}"; do
        echo "  - $mismatch"
    done
fi

if [ ${#missing_on_nacos[@]} -gt 0 ]; then
    echo
    echo "${C_RED}本地有但 Nacos 没有的配置文件:${C_NC}"
    for missing in "${missing_on_nacos[@]}"; do
        echo "  - $missing"
    done
fi

if [ ${#missing_on_local[@]} -gt 0 ]; then
    echo
    echo "${C_YELLOW}Nacos 有但本地没有的配置:${C_NC}"
    for missing in "${missing_on_local[@]}"; do
        echo "  - $missing"
    done
fi

# 询问用户是否要推送配置
if [ ${#mismatched_configs[@]} -gt 0 ] || [ ${#missing_on_nacos[@]} -gt 0 ]; then
    echo
    read -p "${C_YELLOW}发现配置不一致或缺失，是否要推送本地配置到 Nacos? (y/N): ${C_NC}" -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "${C_BLUE}开始推送配置到 Nacos...${C_NC}"
        
        # 推送所有本地有但 Nacos 没有或不一致的配置文件
        all_files_to_push=("${mismatched_configs[@]}" "${missing_on_nacos[@]}")
        push_success=0
        push_failures=0
        
        for file in "${all_files_to_push[@]}"; do
            if push_config_file "$file"; then
                ((push_success++))
            else
                ((push_failures++))
            fi
        done
        
        echo
        echo "${C_BLUE}推送完成总结:${C_NC}"
        echo "成功推送数: $push_success"
        echo "推送失败数: $push_failures"
        
        if [ $push_failures -eq 0 ]; then
            echo "${C_GREEN}所有配置文件推送成功!${C_NC}"
        else
            echo "${C_RED}部分配置文件推送失败，请检查错误信息${C_NC}"
            exit 1
        fi
    else
        echo "${C_YELLOW}跳过推送，配置同步完成${C_NC}"
    fi
else
    echo "${C_GREEN}所有本地配置文件与 Nacos 上的配置一致！无需推送${C_NC}"
fi