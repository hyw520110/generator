#!/bin/bash
# Core 模块安装脚本
# 功能：计算 src 目录和 pom.xml 的哈希值，与缓存比对，仅在变更时执行安装

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# 哈希缓存文件路径（存储在用户 .m2 目录下）
HASH_CACHE_DIR="$HOME/.m2/generator"
HASH_CACHE_FILE="$HASH_CACHE_DIR/core-hash.txt"

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

# 计算目录的哈希值（递归计算所有文件的哈希）
calculate_dir_hash() {
    local dir="$1"
    if [ ! -d "$dir" ]; then
        echo ""
        return
    fi
    
    # 使用 find 和 md5sum/shasum 计算所有文件的哈希
    if command -v md5sum >/dev/null 2>&1; then
        find "$dir" -type f -exec md5sum {} \; | sort | md5sum | cut -d' ' -f1
    elif command -v md5 >/dev/null 2>&1; then
        find "$dir" -type f -exec md5 {} \; | sort | md5
    else
        echo ""
    fi
}

# 计算文件的哈希值
calculate_file_hash() {
    local file="$1"
    if [ ! -f "$file" ]; then
        echo ""
        return
    fi
    
    if command -v md5sum >/dev/null 2>&1; then
        md5sum "$file" | cut -d' ' -f1
    elif command -v md5 >/dev/null 2>&1; then
        md5 -q "$file"
    else
        echo ""
    fi
}

# 创建缓存目录（如果不存在）
create_cache_dir() {
    if [ ! -d "$HASH_CACHE_DIR" ]; then
        mkdir -p "$HASH_CACHE_DIR"
    fi
}

# 读取缓存哈希值
read_cached_hash() {
    if [ -f "$HASH_CACHE_FILE" ]; then
        cat "$HASH_CACHE_FILE"
    else
        echo ""
    fi
}

# 保存缓存哈希值
save_hash() {
    local hash="$1"
    create_cache_dir
    echo "$hash" > "$HASH_CACHE_FILE"
}

# 计算当前哈希值
calculate_current_hash() {
    local src_hash=""
    local pom_hash=""
    
    # 计算 src 目录哈希
    if [ -d "src" ]; then
        src_hash=$(calculate_dir_hash "src")
    fi
    
    # 计算 pom.xml 哈希
    if [ -f "pom.xml" ]; then
        pom_hash=$(calculate_file_hash "pom.xml")
    fi
    
    # 组合哈希值
    echo "${src_hash}-${pom_hash}"
}

# 主逻辑
main() {
    echo "Core 模块安装检查"
    
    # 检查必要文件是否存在
    if [ ! -f "pom.xml" ]; then
        echo "❌ 错误: pom.xml 不存在"
        exit 1
    fi
    
    # 计算当前哈希值
    current_hash=$(calculate_current_hash)
    
    if [ -z "$current_hash" ]; then
        echo "⚠️  警告: 无法计算哈希值，强制执行安装"
        execute_install
        return
    fi
    
    # 读取缓存哈希值
    cached_hash=$(read_cached_hash)
    
    # 比对哈希值
    if [ "$current_hash" = "$cached_hash" ]; then
        echo "✓ Core 模块未变更，跳过安装"
        exit 0
    else
        echo "✓ 检测到 Core 模块变更，开始安装..."
        if [ -n "$cached_hash" ]; then
            echo "  原哈希: ${cached_hash:0:8}..."
            echo "  新哈希: ${current_hash:0:8}..."
        fi
        execute_install
    fi
}

# 执行安装
execute_install() {
    echo "开始安装 Core 模块"
    echo "使用 Maven: $MVN"
    echo "CPU 核心数: $CPU_CORES"
    echo "并行编译: -T ${CPU_CORES}C"
    
    $MVN clean compile install -Dmaven.test.skip=true -T ${CPU_CORES}C
    
    if [ $? -eq 0 ]; then
        echo "✅ Core 模块安装成功"
        
        # 保存新的哈希值
        current_hash=$(calculate_current_hash)
        save_hash "$current_hash"
        echo "✓ 已更新哈希缓存"
        exit 0
    else
        echo "❌ Core 模块安装失败"
        exit 1
    fi
}

# 执行主逻辑
main
