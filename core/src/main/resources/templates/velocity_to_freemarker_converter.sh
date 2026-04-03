#!/bin/bash
set -e

BASE_DIR="/Users/mac/workspace/generator"
VELOCITY_DIR="$BASE_DIR/core/src/main/resources/templates/velocity"
FREEMARKER_DIR="$BASE_DIR/core/src/main/resources/templates/freemarker"
CONVERTER_PL="$BASE_DIR/velocity_to_freemarker_converter.pl"

echo "=========================================="
echo "开始批量转换 (V9 修复版): Velocity -> FreeMarker"
echo "=========================================="

rm -rf "$FREEMARKER_DIR"
mkdir -p "$FREEMARKER_DIR"

echo "1. 过滤并复制文本模板文件..."
cd "$VELOCITY_DIR"

# 定义允许转换的文本后缀
EXTS="vm|java|xml|html|vue|sh|yml|yaml|properties|txt|json|sql|md|js"

find . -type f | while read -r file; do
    rel_path="${file#./}"
    
    # 统一将模板后缀改为 .ftl
    # 如果原文件名带 .vm，去掉 .vm 加上 .ftl
    # 如果是其他文本后缀，直接加 .ftl (例如 %s.js -> %s.js.ftl)
    if [[ "$rel_path" =~ \.($EXTS)$ ]]; then
        if [[ "$rel_path" == *.vm ]]; then
            target_path="$FREEMARKER_DIR/${rel_path%.vm}.ftl"
        else
            target_path="$FREEMARKER_DIR/$rel_path.ftl"
        fi
        mkdir -p "$(dirname "$target_path")"
        cp "$rel_path" "$target_path"
    else
        # 二进制文件直接原样复制
        target_path="$FREEMARKER_DIR/$rel_path"
        mkdir -p "$(dirname "$target_path")"
        cp "$rel_path" "$target_path"
        echo "  (跳过重命名二进制文件: $rel_path)"
    fi
done

echo "✓ 过滤复制完成。"

echo "2. 运行 Perl 转换脚本..."
perl "$CONVERTER_PL"

echo "=========================================="
echo "任务成功完成！"
