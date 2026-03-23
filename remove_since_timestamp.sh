#!/bin/bash

# 批量移除 demo 和 web/demo 目录下 Java 文件中的 @since 和 @date 时间戳行
# 使用方式: ./remove_since_timestamp.sh

# 函数: 移除指定目录下 Java 文件中的时间戳行
remove_timestamp_lines() {
    local dir=$1
    local count=0
    
    if [ ! -d "$dir" ]; then
        echo "  目录不存在: $dir"
        return 0
    fi
    
    while IFS= read -r file; do
        local modified=0
        
        # 移除包含 @since 的整行
        if grep -q "@since" "$file" 2>/dev/null; then
            sed -i '' '/ \* @since /d' "$file"
            modified=1
        fi
        
        # 移除包含 @date 的整行
        if grep -q "@date" "$file" 2>/dev/null; then
            sed -i '' '/ \* @date /d' "$file"
            modified=1
        fi
        
        if [ $modified -eq 1 ]; then
            echo "  已处理: $file"
            ((count++))
        fi
    done < <(find "$dir" -name "*.java" -type f 2>/dev/null)
    
    return $count
}

echo "开始处理 Java 文件中的时间戳..."
echo ""

# 处理 demo 目录
echo "处理 demo/ 目录..."
remove_timestamp_lines "demo"
demo_count=$?
echo ""

# 处理 web/demo 目录
echo "处理 web/demo/ 目录..."
remove_timestamp_lines "web/demo"
web_count=$?
echo ""

total=$((demo_count + web_count))
echo "处理完成! 共修改 $total 个文件"
