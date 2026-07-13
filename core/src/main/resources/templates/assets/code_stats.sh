#!/bin/bash

# 统计当前项目的代码文件总数、代码总行数，并分类统计
# 排除 .gitignore 中定义的目录或文件

# 设置项目目录
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 定义前端文件扩展名
declare -a FRONTEND_EXTENSIONS=("ts" "js" "vue" "jsx" "tsx" "css" "scss" "sass" "less" "html")

# 定义后端文件扩展名
declare -a BACKEND_EXTENSIONS=("java" "xml" "sql" "py" "sh")

# 定义其他文件扩展名
declare -a OTHER_EXTENSIONS=("yaml" "yml" "json" "md" "txt" "properties" "conf" "config" "gradle" "gitignore")

# 定义二进制文件扩展名（只统计文件数，不统计代码行数）
declare -a BINARY_EXTENSIONS=("png" "jpg" "jpeg" "gif" "webp" "svg" "ico" "pdf" "doc" "docx" "xls" "xlsx" "ppt" "pptx" "zip" "tar" "gz" "rar" "7z" "exe" "dll" "so" "dylib" "class" "jar" "war" "ear" "ttf" "woff" "woff2" "eot" "otf" "mp3" "mp4" "avi" "mov" "wmv" "flv" "mkv" "wav" "flac" "aac" "ogg" "webm")

# 默认模式为完整统计
MODE="full"

# 显示帮助信息
show_help() {
    echo "用法: $0 [选项]"
    echo "选项:"
    echo "  -h, --help     显示帮助信息"
    echo "  -b, --basic    基础统计模式（不包含二进制文件）"
    echo "  -f, --full     完整统计模式（包含二进制文件） [默认]"
    echo
    echo "示例:"
    echo "  $0          # 完整统计模式"
    echo "  $0 -b       # 基础统计模式"
    echo "  $0 --full   # 完整统计模式"
}

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -b|--basic)
            MODE="basic"
            shift
            ;;
        -f|--full)
            MODE="full"
            shift
            ;;
        *)
            echo "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
done

# 打印标题
print_title() {
    echo "╔════════════════════════════════════════════════════════════════╗"
    echo "║                    项目代码统计报告                            ║"
    echo "╠════════════════════════════════════════════════════════════════╣"
    printf "║ 项目路径: %-51s ║\n" "$(basename "$PROJECT_DIR")"
    if [ "$MODE" = "basic" ]; then
        printf "║ 统计模式: %-51s ║\n" "基础模式"
    else
        printf "║ 统计模式: %-51s ║\n" "完整模式（包含二进制文件）"
    fi
    echo "╚════════════════════════════════════════════════════════════════╝"
    echo
}

# 打印分隔线
print_separator() {
    echo "────────────────────────────────────────────────────────────────"
}

# 打印小标题
print_subtitle() {
    echo ">>> $1 <<<"
}

# 获取Git用户统计
get_git_user_stats() {
    local output_file="$1"
    local frontend_dir="$2"
    
    # 检查是否是Git仓库
    if [ ! -d ".git" ]; then
        return
    fi
    
    # 获取Git提交历史并统计每个用户的提交次数和更改行数
    # 使用临时文件存储中间结果
    local temp_file=$(mktemp)
    
    # 获取每个用户的提交次数
    git log --pretty=format:"%an" | sort | uniq -c > "$temp_file"
    
    # 处理每个用户
    while read -r line; do
        if [ -n "$line" ]; then
            # 提取提交次数和用户名
            commits=$(echo "$line" | awk '{print $1}')
            author=$(echo "$line" | cut -d' ' -f2-)
            
            # 获取该用户更改的行数（添加和删除）
            lines_changed=$(git log --author="$author" --pretty=tformat: --numstat | awk '{ added += $1; removed += $2 } END { print added+removed }' 2>/dev/null || echo 0)
            
            # 如果lines_changed为空则设为0
            if [ -z "$lines_changed" ] || [ "$lines_changed" = " " ]; then
                lines_changed=0
            fi
            
            # 统计该用户在前端代码中的更改行数
            frontend_lines=0
            if [ -n "$frontend_dir" ] && [ "$frontend_dir" != "." ]; then
                frontend_lines=$(git log --author="$author" --pretty=tformat: --numstat | grep "$frontend_dir/" 2>/dev/null | awk '{ added += $1; removed += $2 } END { print added+removed }' 2>/dev/null || echo 0)
                # 如果frontend_lines为空则设为0
                if [ -z "$frontend_lines" ] || [ "$frontend_lines" = " " ]; then
                    frontend_lines=0
                fi
            fi
            
            # 计算后端代码行数（总行数减去前端行数）
            backend_lines=$((lines_changed - frontend_lines))
            
            # 确保后端行数不为负数
            if [ "$backend_lines" -lt 0 ]; then
                backend_lines=0
            fi
            
            echo "$author $commits $lines_changed $frontend_lines $backend_lines" >> "$output_file"
        fi
    done < "$temp_file"
    
    # 清理临时文件
    rm -f "$temp_file"
}

# 检查是否包含前端代码目录
find_frontend_dir() {
    # 检查是否包含web目录
    if [ -d "$PROJECT_DIR/web" ]; then
        echo "web"
        return
    fi
    
    # 检查常见前端目录
    for dir in "frontend" "client" "src" "app"; do
        if [ -d "$PROJECT_DIR/$dir" ] && (
            [ -f "$PROJECT_DIR/$dir/package.json" ] || 
            [ -f "$PROJECT_DIR/$dir/webpack.config.js" ] || 
            [ -f "$PROJECT_DIR/$dir/vite.config.js" ] || 
            [ -f "$PROJECT_DIR/$dir/angular.json" ] || 
            [ -f "$PROJECT_DIR/$dir/nuxt.config.js" ] || 
            [ -f "$PROJECT_DIR/$dir/next.config.js" ] || 
            [ -f "$PROJECT_DIR/$dir/vue.config.js" ]
        ); then
            echo "$dir"
            return
        fi
    done
    
    # 如果没有发现前端代码目录，返回空
    echo ""
}

# 解析.gitignore文件获取排除模式
parse_gitignore() {
    local gitignore_file="$1"
    local output_file="$2"
    
    if [ -f "$gitignore_file" ]; then
        # 读取.gitignore文件，过滤注释和空行，并处理相对路径
        grep -v '^#' "$gitignore_file" | grep -v '^$' | while read -r pattern; do
            # 移除可能的前导斜杠
            pattern=$(echo "$pattern" | sed 's/^\/\+//')
            # 只添加非空模式
            if [ -n "$pattern" ]; then
                echo "$pattern" >> "$output_file"
            fi
        done
    fi
}

# 获取文本文件列表
get_text_files() {
    local output_file="$1"
    local frontend_dir="$2"
    local exclude_patterns_file="$3"
    
    # 使用find一次性获取所有文件，提高效率
    ALL_FILES=$(mktemp)
    
    # 构建排除参数
    EXCLUDE_ARGS=()
    
    # 添加默认排除的目录
    EXCLUDE_ARGS+=(-not -path "*/node_modules/*" -not -path "*/target/*" -not -path "*/.git/*" -not -path "*/build/*" -not -path "*/dist/*" -not -path "*/.next/*" -not -path "*/out/*" -not -path "*/python/xzqh_data.html" -not -path "*/python/.venv/*")
    
    # 添加从.gitignore解析的排除模式
    if [ -s "$exclude_patterns_file" ]; then
        while IFS= read -r pattern; do
            # 跳过空行
            if [ -z "$pattern" ]; then
                continue
            fi
            # 添加到排除参数
            EXCLUDE_ARGS+=(-not -path "*/$pattern" -not -path "*/$pattern/*")
        done < "$exclude_patterns_file"
    fi
    
    # 查找文件
    find "$PROJECT_DIR" -type f "${EXCLUDE_ARGS[@]}" > "$ALL_FILES" 2>/dev/null
    
    # 如果有前端目录，则排除它
    if [ -n "$frontend_dir" ] && [ "$frontend_dir" != "." ]; then
        grep -v "/$frontend_dir/" "$ALL_FILES" > "${ALL_FILES}_filtered"
        mv "${ALL_FILES}_filtered" "$ALL_FILES"
    fi
    
    # 过滤出文本文件
    while IFS= read -r file; do
        # 跳过空行
        if [ -z "$file" ]; then
            continue
        fi
        
        # 检查文件是否存在
        if [ ! -f "$file" ]; then
            continue
        fi
        
        # 检查是否为文本文件，忽略错误输出
        if file -b --mime-type "$file" 2>/dev/null | grep -q "text/"; then
            # 转义特殊字符并添加到输出文件
            printf "%s\n" "$file" >> "$output_file"
        fi
    done < "$ALL_FILES"
    
    # 清理临时文件
    rm -f "$ALL_FILES"
}

# 获取前端文本文件列表
get_frontend_text_files() {
    local output_file="$1"
    local frontend_dir="$2"
    local exclude_patterns_file="$3"
    
    if [ -z "$frontend_dir" ] || [ "$frontend_dir" = "." ]; then
        return
    fi
    
    local frontend_path="$PROJECT_DIR/$frontend_dir"
    if [ ! -d "$frontend_path" ]; then
        return
    fi
    
    # 使用find获取前端目录中的所有文件，提高效率
    ALL_FILES=$(mktemp)
    
    # 构建排除参数
    EXCLUDE_ARGS=()
    
    # 添加默认排除的目录（注意：不再排除node_modules，因为前端文件可能在其中）
    EXCLUDE_ARGS+=(-not -path "*/target/*" -not -path "*/.git/*" -not -path "*/build/*" -not -path "*/dist/*" -not -path "*/.next/*" -not -path "*/out/*" -not -path "*/unpackage/*")
    
    # 添加从.gitignore解析的排除模式，但要特殊处理node_modules
    if [ -s "$exclude_patterns_file" ]; then
        while IFS= read -r pattern; do
            # 跳过空行
            if [ -z "$pattern" ]; then
                continue
            fi
            # 特殊处理node_modules，只在非web目录下排除
            if [[ "$pattern" == "node_modules" ]] || [[ "$pattern" == "/node_modules" ]] || [[ "$pattern" == "node_modules/" ]]; then
                # 在web目录下不排除node_modules
                continue
            else
                # 添加到排除参数
                EXCLUDE_ARGS+=(-not -path "*/$pattern" -not -path "*/$pattern/*")
            fi
        done < "$exclude_patterns_file"
    fi
    
    # 查找文件（这里我们只查找src目录下的文件，避免node_modules干扰）
    find "$frontend_path/src" -type f "${EXCLUDE_ARGS[@]}" > "$ALL_FILES" 2>/dev/null
    
    # 过滤出文本文件
    while IFS= read -r file; do
        # 跳过空行
        if [ -z "$file" ]; then
            continue
        fi
        
        # 检查文件是否存在
        if [ ! -f "$file" ]; then
            continue
        fi
        
        # 检查是否为文本文件，忽略错误输出
        if file -b --mime-type "$file" 2>/dev/null | grep -q "text/"; then
            # 转义特殊字符并添加到输出文件
            printf "%s\n" "$file" >> "$output_file"
        fi
    done < "$ALL_FILES"
    
    # 清理临时文件
    rm -f "$ALL_FILES"
}

# 统计文件数量和行数
count_files_and_lines() {
    local file_list="$1"
    local extension="$2"
    
    # 检查文件列表是否存在且不为空
    if [ ! -s "$file_list" ]; then
        echo "0 0"
        return
    fi
    
    local files=0
    local lines=0
    
    while IFS= read -r file; do
        # 跳过空行
        if [ -z "$file" ]; then
            continue
        fi
        
        # 确保文件存在且以指定扩展名结尾
        if [ -f "$file" ] && [[ "$file" == *".$extension" ]]; then
            files=$((files + 1))
            # 对于二进制文件扩展名，不统计行数
            is_binary=false
            for bin_ext in "${BINARY_EXTENSIONS[@]}"; do
                if [ "$bin_ext" = "$extension" ]; then
                    is_binary=true
                    break
                fi
            done
            
            if [ "$is_binary" = false ] || [ "$MODE" = "basic" ]; then
                local file_lines=$(wc -l < "$file" 2>/dev/null || echo 0)
                # 确保file_lines是数字
                if ! [[ "$file_lines" =~ ^[0-9]+$ ]]; then
                    file_lines=0
                fi
                lines=$((lines + file_lines))
            fi
        fi
    done < "$file_list"
    
    echo "$files $lines"
}

# 统计二进制文件（仅在完整模式下使用）
count_binary_files() {
    local project_dir="$1"
    local frontend_dir="$2"
    local extension="$3"
    local is_frontend="$4"
    
    # 在基础模式下不统计二进制文件
    if [ "$MODE" = "basic" ]; then
        echo "0"
        return
    fi
    
    local count=0
    
    if [ "$is_frontend" = "true" ] && [ -n "$frontend_dir" ] && [ "$frontend_dir" != "." ]; then
        # 统计前端二进制文件
        count=$(find "$project_dir/$frontend_dir" -type f -name "*.$extension" -not -path "*/node_modules/*" -not -path "*/target/*" -not -path "*/.git/*" -not -path "*/build/*" -not -path "*/dist/*" -not -path "*/.next/*" -not -path "*/out/*" -not -path "*/unpackage/*" 2>/dev/null | wc -l)
    elif [ "$is_frontend" = "false" ]; then
        # 统计后端二进制文件
        if [ -n "$frontend_dir" ] && [ "$frontend_dir" != "." ]; then
            count=$(find "$project_dir" -type f -name "*.$extension" -not -path "*/$frontend_dir/*" -not -path "*/node_modules/*" -not -path "*/target/*" -not -path "*/.git/*" -not -path "*/build/*" -not -path "*/dist/*" -not -path "*/.next/*" -not -path "*/out/*" -not -path "*/python/xzqh_data.html" -not -path "*/python/.venv/*" 2>/dev/null | wc -l)
        else
            count=$(find "$project_dir" -type f -name "*.$extension" -not -path "*/node_modules/*" -not -path "*/target/*" -not -path "*/.git/*" -not -path "*/build/*" -not -path "*/dist/*" -not -path "*/.next/*" -not -path "*/out/*" -not -path "*/python/xzqh_data.html" -not -path "*/python/.venv/*" 2>/dev/null | wc -l)
        fi
    fi
    
    echo "$count"
}

# 主函数
main() {
    print_title
    
    # 创建临时文件存储.gitignore模式
    EXCLUDE_PATTERNS_FILE=$(mktemp)
    
    # 解析根目录.gitignore
    parse_gitignore "$PROJECT_DIR/.gitignore" "$EXCLUDE_PATTERNS_FILE"
    
    # 解析web目录.gitignore（如果存在）
    if [ -f "$PROJECT_DIR/web/.gitignore" ]; then
        parse_gitignore "$PROJECT_DIR/web/.gitignore" "$EXCLUDE_PATTERNS_FILE"
    fi
    
    # 检查是否存在前端代码目录
    FRONTEND_DIR=$(find_frontend_dir)
    
    # 获取所有文本文件（排除前端目录）
    ALL_TEXT_FILES=$(mktemp)
    get_text_files "$ALL_TEXT_FILES" "$FRONTEND_DIR" "$EXCLUDE_PATTERNS_FILE"
    
    # 获取前端文本文件
    FRONTEND_TEXT_FILES=$(mktemp)
    get_frontend_text_files "$FRONTEND_TEXT_FILES" "$FRONTEND_DIR" "$EXCLUDE_PATTERNS_FILE"
    
    # 统计总数
    backend_files_count=$(wc -l < "$ALL_TEXT_FILES" 2>/dev/null || echo 0)
    frontend_files_count=$(wc -l < "$FRONTEND_TEXT_FILES" 2>/dev/null || echo 0)
    total_files=$((backend_files_count + frontend_files_count))
    
    backend_lines_count=0
    while IFS= read -r file; do
        # 确保文件存在
        if [ -f "$file" ]; then
            # 检查是否为二进制文件扩展名
            is_binary=false
            for bin_ext in "${BINARY_EXTENSIONS[@]}"; do
                if [[ "$file" == *".$bin_ext" ]]; then
                    is_binary=true
                    break
                fi
            done
            
            if [ "$is_binary" = false ] || [ "$MODE" = "basic" ]; then
                file_lines=$(wc -l < "$file" 2>/dev/null || echo 0)
                backend_lines_count=$((backend_lines_count + file_lines))
            fi
        fi
    done < "$ALL_TEXT_FILES"
    
    frontend_lines_count=0
    while IFS= read -r file; do
        # 确保文件存在
        if [ -f "$file" ]; then
            # 检查是否为二进制文件扩展名
            is_binary=false
            for bin_ext in "${BINARY_EXTENSIONS[@]}"; do
                if [[ "$file" == *".$bin_ext" ]]; then
                    is_binary=true
                    break
                fi
            done
            
            if [ "$is_binary" = false ] || [ "$MODE" = "basic" ]; then
                file_lines=$(wc -l < "$file" 2>/dev/null || echo 0)
                frontend_lines_count=$((frontend_lines_count + file_lines))
            fi
        fi
    done < "$FRONTEND_TEXT_FILES"
    
    total_lines=$((backend_lines_count + frontend_lines_count))
    
    # 处理除零错误
    if [ "$total_lines" -eq 0 ]; then
        total_lines=1
    fi
    
    # 获取Git用户统计
    GIT_USER_STATS_FILE=$(mktemp)
    get_git_user_stats "$GIT_USER_STATS_FILE" "$FRONTEND_DIR"
    
    # 显示总体统计（移到前面）
    print_subtitle "总体统计"
    printf "%-8s 文件数: %6s, 代码行数: %8s\n" "总计" "$total_files" "$total_lines"
    printf "%-8s 文件数: %6s, 代码行数: %8s\n" "后端" "$backend_files_count" "$backend_lines_count"
    printf "%-8s 文件数: %6s, 代码行数: %8s\n" "前端" "$frontend_files_count" "$frontend_lines_count"
    print_separator
    echo
    
    # 创建一个临时文件来存储语言统计数据
    LANG_STATS_FILE=$(mktemp)
    
    # 统计后端文件
    for ext in "${BACKEND_EXTENSIONS[@]}"; do
        case $ext in
            "java") name="Java" ;;
            "xml") name="XML" ;;
            "sql") name="SQL" ;;
            "py") name="Python" ;;
            "sh") name="Bash" ;;
        esac
        
        # 统计后端文件
        result=$(count_files_and_lines "$ALL_TEXT_FILES" "$ext")
        files=$(echo "$result" | cut -d' ' -f1)
        lines=$(echo "$result" | cut -d' ' -f2)
        
        echo "$ext $name $files $lines" >> "$LANG_STATS_FILE"
    done
    
    # 统计Markdown文件（通常属于后端）
    result=$(count_files_and_lines "$ALL_TEXT_FILES" "md")
    md_files=$(echo "$result" | cut -d' ' -f1)
    md_lines=$(echo "$result" | cut -d' ' -f2)
    echo "md Markdown $md_files $md_lines" >> "$LANG_STATS_FILE"
    
    # 统计前端文件
    if [ -n "$FRONTEND_DIR" ] && [ "$FRONTEND_DIR" != "." ]; then
        for ext in "${FRONTEND_EXTENSIONS[@]}"; do
            # 排除HTML文件，因为它可能属于后端模板
            if [ "$ext" = "html" ]; then
                continue
            fi
            
            case $ext in
                "ts") name="TypeScript" ;;
                "js") name="JavaScript" ;;
                "vue") name="Vue" ;;
                "jsx") name="JSX" ;;
                "tsx") name="TSX" ;;
                "css") name="CSS" ;;
                "scss") name="SCSS" ;;
                "sass") name="SASS" ;;
                "less") name="LESS" ;;
                "html") name="HTML" ;;
            esac
            
            # 统计前端文件
            result=$(count_files_and_lines "$FRONTEND_TEXT_FILES" "$ext")
            files=$(echo "$result" | cut -d' ' -f1)
            lines=$(echo "$result" | cut -d' ' -f2)
            
            echo "$ext $name $files $lines" >> "$LANG_STATS_FILE"
        done
    fi
    
    # 统计其他文件
    for ext in "${OTHER_EXTENSIONS[@]}"; do
        case $ext in
            "yaml"|"yml") name="YAML" ;;
            "json") name="JSON" ;;
            "md") name="Markdown" ;;
            "txt") name="Text" ;;
            "properties") name="Properties" ;;
            "conf"|"config") name="Config" ;;
            "gradle") name="Gradle" ;;
            "gitignore") name="Git Ignore" ;;
            *) name="$ext" ;;
        esac
        
        # 统计其他文件（在后端文件中查找，因为前端文件已经被单独处理）
        result=$(count_files_and_lines "$ALL_TEXT_FILES" "$ext")
        # 确保result格式正确
        if [[ "$result" =~ ^[0-9]+\ [0-9]+$ ]]; then
            files=$(echo "$result" | cut -d' ' -f1)
            lines=$(echo "$result" | cut -d' ' -f2)
            
            # 确保files和lines是数字
            if ! [[ "$files" =~ ^[0-9]+$ ]]; then
                files=0
            fi
            if ! [[ "$lines" =~ ^[0-9]+$ ]]; then
                lines=0
            fi
            
            # 只有当文件数大于0时才添加到统计中
            if [ "$files" -gt 0 ] || [ "$lines" -gt 0 ]; then
                echo "$ext $name $files $lines" >> "$LANG_STATS_FILE"
            fi
        fi
        
        # 统计前端中的其他文件
        if [ -n "$FRONTEND_DIR" ] && [ "$FRONTEND_DIR" != "." ]; then
            result=$(count_files_and_lines "$FRONTEND_TEXT_FILES" "$ext")
            # 确保result格式正确
            if [[ "$result" =~ ^[0-9]+\ [0-9]+$ ]]; then
                files=$(echo "$result" | cut -d' ' -f1)
                lines=$(echo "$result" | cut -d' ' -f2)
                
                # 确保files和lines是数字
                if ! [[ "$files" =~ ^[0-9]+$ ]]; then
                    files=0
                fi
                if ! [[ "$lines" =~ ^[0-9]+$ ]]; then
                    lines=0
                fi
                
                # 只有当文件数大于0时才添加到统计中
                if [ "$files" -gt 0 ] || [ "$lines" -gt 0 ]; then
                    echo "$ext $name $files $lines" >> "$LANG_STATS_FILE"
                fi
            fi
        fi
    done
    
    # 在完整模式下统计二进制文件（只统计文件数，行数为0）
    if [ "$MODE" = "full" ]; then
        for ext in "${BINARY_EXTENSIONS[@]}"; do
            case $ext in
                "png") name="PNG Image" ;;
                "jpg"|"jpeg") name="JPEG Image" ;;
                "gif") name="GIF Image" ;;
                "webp") name="WebP Image" ;;
                "svg") name="SVG Image" ;;
                "ico") name="Icon" ;;
                "pdf") name="PDF Document" ;;
                "doc"|"docx") name="Word Document" ;;
                "xls"|"xlsx") name="Excel Document" ;;
                "ppt"|"pptx") name="PowerPoint Document" ;;
                "zip"|"tar"|"gz"|"rar"|"7z") name="Archive" ;;
                "exe"|"dll"|"so"|"dylib") name="Executable/Binary" ;;
                "class"|"jar"|"war"|"ear") name="Java Archive" ;;
                "ttf"|"woff"|"woff2"|"eot"|"otf") name="Font" ;;
                "mp3"|"mp4"|"avi"|"mov"|"wmv"|"flv"|"mkv"|"wav"|"flac"|"aac"|"ogg"|"webm") name="Media" ;;
                *) name="$ext (Binary)" ;;
            esac
            
            # 统计后端二进制文件
            backend_binary_count=$(count_binary_files "$PROJECT_DIR" "$FRONTEND_DIR" "$ext" "false")
            if [ "$backend_binary_count" -gt 0 ]; then
                echo "$ext $name $backend_binary_count 0" >> "$LANG_STATS_FILE"
            fi
            
            # 统计前端二进制文件
            if [ -n "$FRONTEND_DIR" ] && [ "$FRONTEND_DIR" != "." ]; then
                frontend_binary_count=$(count_binary_files "$PROJECT_DIR" "$FRONTEND_DIR" "$ext" "true")
                if [ "$frontend_binary_count" -gt 0 ]; then
                    echo "$ext $name $frontend_binary_count 0" >> "$LANG_STATS_FILE"
                fi
            fi
        done
    fi
    
    # 按代码行数排序并去重
    SORTED_LANG_STATS_FILE=$(mktemp)
    # 使用awk去重，保留相同name的条目中文件数和行数最大的
    awk '!seen[$2]++ || ($4 > max[$2]) { max[$2] = $4; line[$2] = $0 } END { for (name in line) print line[name] }' "$LANG_STATS_FILE" | sort -k4,4nr > "$SORTED_LANG_STATS_FILE"
    
    # 显示后端统计
    print_subtitle "后端代码统计 (按文件类型)"
    has_backend=false
    backend_files_total=0
    backend_lines_total=0
    backend_binary_files=0
    # 使用关联数组跟踪已处理的文件类型
    declare -A processed_types
    while read -r ext name files lines; do
        # 跳过已处理的文件类型
        if [[ -n "${processed_types[$name]}" ]]; then
            continue
        fi
        
        # 验证files和lines是有效的数字
        if ! [[ "$files" =~ ^[0-9]+$ ]]; then
            files=0
        fi
        if ! [[ "$lines" =~ ^[0-9]+$ ]]; then
            lines=0
        fi
        
        if [ "$lines" -gt 0 ] || [ "$files" -gt 0 ]; then
            # 判断是否为后端文件（不在前端扩展名中）
            is_backend=true
            for fext in "${FRONTEND_EXTENSIONS[@]}"; do
                if [ "$ext" = "$fext" ]; then
                    is_backend=false
                    break
                fi
            done
            
            # Markdown文件也归类为后端
            if [ "$ext" = "md" ]; then
                is_backend=true
            fi
            
            # 检查是否为二进制文件类型
            is_binary=false
            for bin_ext in "${BINARY_EXTENSIONS[@]}"; do
                if [ "$bin_ext" = "$ext" ]; then
                    is_binary=true
                    break
                fi
            done
            
            if [ "$is_backend" = true ]; then
                has_backend=true
                backend_files_total=$((backend_files_total + files))
                backend_lines_total=$((backend_lines_total + lines))
                if [ "$is_binary" = true ]; then
                    backend_binary_files=$((backend_binary_files + files))
                fi
                processed_types["$name"]=1
                percentage=$(awk "BEGIN {printf \"%.2f\", $lines * 100 / $total_lines}" 2>/dev/null || echo "0.00")
                # 确保percentage是数字
                if ! [[ "$percentage" =~ ^[0-9]+\.?[0-9]*$ ]]; then
                    percentage="0.00"
                fi
                printf "  %-12s : %6s 个文件, %8s 行代码 (%6.2f%%)\n" "$name" "$files" "$lines" "$percentage"
            fi
        fi
    done < "$SORTED_LANG_STATS_FILE"
    
    # 显示后端二进制文件统计（仅在完整模式下显示）
    if [ "$MODE" = "full" ] && [ "$backend_binary_files" -gt 0 ]; then
        echo "  ----------------------------------------------------------------"
        printf "  二进制文件总计 : %6s 个文件, %8s 行代码 (只统计文件数)\n" "$backend_binary_files" "0"
    fi
    
    # 显示后端总计
    if [ "$has_backend" = true ]; then
        echo "  ----------------------------------------------------------------"
        printf "  %-12s : %6s 个文件, %8s 行代码 (100.00%%)\n" "后端总计" "$backend_files_total" "$backend_lines_total"
    else
        echo "  暂无后端代码文件"
    fi
    
    # 重置已处理的文件类型数组
    unset processed_types
    declare -A processed_types
    
    # 显示前端统计
    if [ -n "$FRONTEND_DIR" ] && [ "$FRONTEND_DIR" != "." ]; then
        print_subtitle "前端代码统计 (目录: $FRONTEND_DIR, 按文件类型)"
        has_frontend=false
        frontend_files_total=0
        frontend_lines_total=0
        frontend_binary_files=0
        while read -r ext name files lines; do
            # 跳过已处理的文件类型
            if [[ -n "${processed_types[$name]}" ]]; then
                continue
            fi
            
            # 验证files和lines是有效的数字
            if ! [[ "$files" =~ ^[0-9]+$ ]]; then
                files=0
            fi
            if ! [[ "$lines" =~ ^[0-9]+$ ]]; then
                lines=0
            fi
            
            if [ "$lines" -gt 0 ] || [ "$files" -gt 0 ]; then
                # 判断是否为前端文件（在前端扩展名中）
                is_frontend=false
                for fext in "${FRONTEND_EXTENSIONS[@]}"; do
                    if [ "$ext" = "$fext" ]; then
                        is_frontend=true
                        break
                    fi
                done
                
                # 检查是否为二进制文件类型
                is_binary=false
                for bin_ext in "${BINARY_EXTENSIONS[@]}"; do
                    if [ "$bin_ext" = "$ext" ]; then
                        is_binary=true
                        break
                    fi
                done
                
                if [ "$is_frontend" = true ]; then
                    has_frontend=true
                    frontend_files_total=$((frontend_files_total + files))
                    frontend_lines_total=$((frontend_lines_total + lines))
                    if [ "$is_binary" = true ] && [ "$MODE" = "full" ]; then
                        frontend_binary_files=$((frontend_binary_files + files))
                    fi
                    processed_types["$name"]=1
                    percentage=$(awk "BEGIN {printf \"%.2f\", $lines * 100 / $total_lines}" 2>/dev/null || echo "0.00")
                    # 确保percentage是数字
                    if ! [[ "$percentage" =~ ^[0-9]+\.?[0-9]*$ ]]; then
                        percentage="0.00"
                    fi
                    printf "  %-12s : %6s 个文件, %8s 行代码 (%6.2f%%)\n" "$name" "$files" "$lines" "$percentage"
                fi
            fi
        done < "$SORTED_LANG_STATS_FILE"
        
        # 显示前端二进制文件统计（仅在完整模式下显示）
        if [ "$MODE" = "full" ] && [ "$frontend_binary_files" -gt 0 ]; then
            echo "  ----------------------------------------------------------------"
            printf "  二进制文件总计 : %6s 个文件, %8s 行代码 (只统计文件数)\n" "$frontend_binary_files" "0"
        fi
        
        # 显示前端总计
        if [ "$has_frontend" = true ]; then
            echo "  ----------------------------------------------------------------"
            printf "  %-12s : %6s 个文件, %8s 行代码 (100.00%%)\n" "前端总计" "$frontend_files_total" "$frontend_lines_total"
        else
            echo "  暂无前端代码文件"
        fi
    fi
    
    # 清理关联数组
    unset processed_types
    
    print_separator
    
    # 显示Git用户统计
    if [ -s "$GIT_USER_STATS_FILE" ]; then
        print_subtitle "Git用户统计"
        printf "  %-20s %10s %14s %10s %10s\n" "用户名" "提交次数" "总更改行数" "前端行数" "后端行数"
        echo "  ----------------------------------------------------------------"
        while read -r author commits lines_changed frontend_lines backend_lines; do
            if [ -n "$author" ]; then
                printf "  %-20s %10s %14s %10s %10s\n" "$author" "$commits" "$lines_changed" "$frontend_lines" "$backend_lines"
            fi
        done < <(sort -k2,2nr "$GIT_USER_STATS_FILE")
        print_separator
    fi
    
    # 清理临时文件
    rm -f "$ALL_TEXT_FILES" "$FRONTEND_TEXT_FILES" "$LANG_STATS_FILE" "$SORTED_LANG_STATS_FILE" "$GIT_USER_STATS_FILE" "$EXCLUDE_PATTERNS_FILE"
}

# 执行主函数
main