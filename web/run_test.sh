#!/bin/bash

# GeneratorConfigTest 编译和执行脚本
# 用法: ./run_test.sh [选项]
# 选项:
#   --no-clean    不清理 demo 和 logs 目录
#   --clean       强制清理 demo 和 logs 目录（默认）
#   -h, --help    显示帮助信息

set -e  # 遇到错误立即退出

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# 默认开启清理功能
CLEAN_DIRS=true

# 解析参数
while [[ $# -gt 0 ]]; do
    case "$1" in
        --no-clean)
            CLEAN_DIRS=false
            shift
            ;;
        --clean)
            CLEAN_DIRS=true
            shift
            ;;
        -h|--help)
            cat << EOF
GeneratorConfigTest 编译和执行脚本

用法: $0 [选项]

选项:
  --no-clean    不清理 demo 和 logs 目录
  --clean       强制清理 demo 和 logs 目录（默认行为）
  -h, --help    显示此帮助信息

示例:
  $0                # 默认清理 demo 和 logs 目录
  $0 --no-clean      # 不清理 demo 和 logs 目录
  $0 --clean         # 显式清理 demo 和 logs 目录
EOF
            exit 0
            ;;
        *)
            echo "未知参数: $1"
            echo "使用 -h 或 --help 查看帮助信息"
            exit 1
            ;;
    esac
done

# 检查并安装 core 模块
check_and_install_core() {
    # 检查上级目录的 core 目录
    PARENT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
    CORE_DIR="$PARENT_DIR/core"
    CORE_INSTALL_SCRIPT="$CORE_DIR/install.sh"
    
    echo "检查 Core 模块..."
    
    if [ -d "$CORE_DIR" ] && [ -f "$CORE_INSTALL_SCRIPT" ]; then
        echo "✓ 检测到 Core 模块目录: $CORE_DIR"
        echo "✓ 执行 Core 模块安装脚本..."
        
        # 调用 core/install.sh
        if bash "$CORE_INSTALL_SCRIPT"; then
            echo "✓ Core 模块检查完成"
        else
            echo "❌ Core 模块安装失败！"
            exit 1
        fi
    else
        echo "✓ Core 模块目录不存在或安装脚本缺失，跳过"
    fi
}

# 清理函数
clean_directories() {
    echo "检查并清理目录..."
    
    # 清理 demo 目录
    if [ -d "demo" ]; then
        echo "删除 demo 目录..."
        rm -rf demo
        echo "✓ demo 目录已删除"
    else
        echo "✓ demo 目录不存在，跳过"
    fi
    
    # 清理 logs 目录
    if [ -d "logs" ]; then
        echo "删除 logs 目录..."
        rm -rf logs
        echo "✓ logs 目录已删除"
    else
        echo "✓ logs 目录不存在，跳过"
    fi
}

# 检查并安装 core 模块
check_and_install_core

# 如果启用了清理功能，则执行清理
if [ "$CLEAN_DIRS" = true ]; then
    clean_directories
else
    echo "跳过目录清理（--no-clean 模式）"
fi

echo "开始编译 GeneratorConfigTest..."

# 使用 Maven 编译测试代码
echo "执行: mvn test-compile"
mvn test-compile

if [ $? -ne 0 ]; then
    echo "❌ 编译失败！"
    exit 1
fi

echo "编译成功！开始执行 GeneratorConfigTest..."

# 使用 Maven 运行指定的测试类
echo "执行: mvn test -Dtest=GeneratorConfigTest"
mvn test -Dtest=GeneratorConfigTest

if [ $? -eq 0 ]; then
    echo "✅ 测试执行成功！"
    exit 0
else
    echo "❌ 测试执行失败！"
    exit 1
fi