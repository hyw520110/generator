#!/bin/sh

# 优先使用 mvnd（Maven Daemon）
if command -v mvnd >/dev/null 2>&1; then
    MVN="mvnd"
else
    MVN="mvn"
fi

# 动态检测CPU核心数
get_cpu_cores() {
    if command -v nproc >/dev/null 2>&1; then
        nproc
    elif [ -f /proc/cpuinfo ]; then
        grep -c ^processor /proc/cpuinfo
    elif command -v sysctl >/dev/null 2>&1; then
        sysctl -n hw.ncpu 2>/dev/null || echo 2
    else
        echo 2
    fi
}
CPU_CORES=$(get_cpu_cores)

# 计算路径哈希（用于生成唯一的配置文件名）
# 参数：$1 路径
# 返回：路径的 MD5 哈希值（前8位）
get_path_hash() {
    local path="$1"
    if command -v md5sum >/dev/null 2>&1; then
        echo -n "$path" | md5sum | cut -d' ' -f1 | cut -c1-8
    elif command -v md5 >/dev/null 2>&1; then
        echo -n "$path" | md5 | cut -c1-8
    else
        # 如果没有 md5 工具，使用简单字符串替换
        echo "$path" | tr '/' '_' | tr ' ' '_' | cut -c1-16
    fi
}

yarn install
yarn run build

rm -rf ./src/main/resources/static/*
mv ./dist/* ./src/main/resources/static/

$MVN clean package -T ${CPU_CORES}C

# 自动生成 <hash>.conf 配置文件（如果是多模块工程）
# 配置文件名基于项目路径的哈希值，确保唯一性
# 该文件在部署时由部署脚本负责拷贝到目标服务器
generate_start_config() {
    local pom_file="../pom.xml"
    
    # 检查父 pom.xml 是否存在
    if [ ! -f "$pom_file" ]; then
        return 0
    fi
    
    # 检查是否为多模块工程（包含 <modules> 标签）
    if ! grep -q "<modules>" "$pom_file" 2>/dev/null; then
        echo "[INFO] 单模块工程，跳过生成 .conf 配置文件"
        return 0
    fi
    
    echo "[INFO] 检测到 Maven 多模块工程，正在解析模块列表..."
    
    # 获取项目根目录路径并计算哈希
    local project_root="$(cd "$(dirname "$pom_file")" && pwd)"
    local path_hash=$(get_path_hash "$project_root")
    local config_file="../${path_hash}.conf"
    
    # 从 pom.xml 提取模块名称（按声明顺序）
    # 使用临时文件避免进程替换
    local tmp_modules=$(mktemp)
    grep -oE '<module>[^<]+</module>' "$pom_file" | \
        sed 's|<module>||g; s|</module>||g; s|/||g; s|\\||g' > "$tmp_modules"
    
    if [ ! -s "$tmp_modules" ]; then
        echo "[WARN] 未从 pom.xml 解析到模块"
        rm -f "$tmp_modules"
        return 0
    fi
    
    # 统计模块数量
    local module_count=$(wc -l < "$tmp_modules" | tr -d ' ')
    echo "[INFO] 解析到 ${module_count} 个模块"
    
    # 构建配置文件中数组格式的模块列表
    local module_array=""
    while read -r module; do
        [ -n "$module" ] && module_array="${module_array}\"$module\" "
    done < "$tmp_modules"
    rm -f "$tmp_modules"
    
    # 生成 <hash>.conf 配置文件
    cat > "$config_file" << EOF
# 自动生成的启动脚本配置文件
# 文件名格式: <hash>.conf (其中 <hash> 是项目路径的 MD5 哈希值前8位)
# 生成时间: $(date '+%Y-%m-%d %H:%M:%S')
# 来源: 从 ${pom_file} 解析
# 项目路径: ${project_root}
# 路径哈希: ${path_hash}
#
# 注意: 此文件由 package.sh 在构建阶段自动生成
# 部署时需由部署脚本负责拷贝到目标服务器的部署目录

# 服务启动顺序（按 pom.xml 中 modules 声明顺序）
SERVICE_DEPENDENCY_ORDER=(${module_array})

# 服务基础目录（部署时使用）
SERVICES_BASE_DIR=\$HOME/webapps

# 网络配置
NETWORK_IGNORED=uengine0,xdroid0,br*,ve*
NETWORK_PREFERRED=eth0
EOF

    echo "[INFO] 已生成配置文件: $config_file"
    echo "[INFO] 项目路径: $project_root"
    echo "[INFO] 路径哈希: $path_hash"
    echo "[INFO] 部署时请确保部署脚本将此文件拷贝到目标服务器"
}

# 执行配置生成
generate_start_config
