#!/bin/bash

# 用途: 检查并输出后端接口的完整路径（组合类和方法上的路径）
CONTROLLER_DIR="."
FRONTEND_DIRS=()
EXTENSIONS=()
EXCLUDE_DIRS=()
USE_GITIGNORE=true
GITIGNORE_PATH=""

DOC_PATH=""
OUTPUT_FILE="$(dirname "$0")/api_docs.md"
STATS_JSON_FILE="$(dirname "$0")/api_stats.json"
SHOW_STATS=false
STATS_JSON=false
CHECK_FRONTEND=false
count=0
found_count=0
not_found_count=0
frontend_call_count=0
controller_count=0

# 使用临时文件替代关联数组以兼容 macOS 默认的 bash 3.2
TEMP_STATS_DIR=$(mktemp -d)
trap 'rm -rf "$TEMP_STATS_DIR"' EXIT
STATS_EVENTS_FILE="$TEMP_STATS_DIR/events.txt"

# 定义 usage 函数
usage() {
     echo "使用方法: $0 [选项]"
     echo "选项:"
     echo "  -src, --source-dir DIR       指定后端源码扫描目录 (默认: .)"
     echo "  -o, --output FILE            指定生成的 Markdown 文档输出路径 (默认: 脚本所在目录/api_docs.md)"
     echo "  -d, --doc-path PATH          指定已有文档路径用于对比漏记接口 (默认: 空)"
     echo "  -s, --stats                  显示统计信息"
     echo "     --stats-json [FILE]       输出结构化 JSON 统计文件 (默认: 脚本所在目录/api_stats.json)"
     echo "  -fd, --frontend-dir DIR      指定前端代码目录 (可多次使用)"
     echo "  -ext, --extension EXT        指定文件扩展名 (可多次使用，如: js,ts,vue)"
     echo "  -c, --check-frontend         检查前端代码中是否调用后端接口"
     echo "  --exclude DIR                手动指定排除目录 (可多次使用)"
     echo "  --no-gitignore               不使用 .gitignore 中的目录进行排除"
     echo "  --gitignore PATH             指定 .gitignore 路径 (默认: 源码目录下的 .gitignore)"
     echo "  -h, --help                   显示此帮助信息"
     exit 1
}

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -src|--source-dir) CONTROLLER_DIR="$2"; shift 2 ;;
        -o|--output) OUTPUT_FILE="$2"; shift 2 ;;
        -d|--doc-path) DOC_PATH="$2"; shift 2 ;;
        -s|--stats) SHOW_STATS=true; shift ;;
        --stats-json)
            STATS_JSON=true
            # 检查下一个参数是否是文件路径（不以 - 开头）
            if [[ $# -ge 2 && "$2" != -* ]]; then
                STATS_JSON_FILE="$2"
                shift 2
            else
                shift
            fi
            ;;
        -fd|--frontend-dir) FRONTEND_DIRS+=("$2"); shift 2 ;;
        -ext|--extension) EXTENSIONS+=("$2"); shift 2 ;;
        -c|--check-frontend) CHECK_FRONTEND=true; shift ;;
        --exclude) EXCLUDE_DIRS+=("$2"); shift 2 ;;
        --no-gitignore) USE_GITIGNORE=false; shift ;;
        --gitignore) GITIGNORE_PATH="$2"; shift 2 ;;
        -h|--help) usage ;;
        *) echo "未知参数: $1"; usage ;;
    esac
done

# 从 .gitignore 提取目录级忽略规则，每行输出一个目录名
parse_gitignore_dirs() {
    local gitignore_file="$1"

    if [ ! -f "$gitignore_file" ]; then
        return
    fi

    local seen=""

    while IFS= read -r line; do
        [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
        line="$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
        [[ -z "$line" ]] && continue
        [[ "${line:0:1}" == "!" ]] && continue
        [[ "$line" == *"?"* ]] && continue

        local dir_name=""

        # 模式 1: **/xxx/ 或 **/xxx (递归子目录模式，取最后一段目录名)
        if [[ "$line" == "**/"* ]]; then
            local stripped="${line#\*\*/}"
            stripped="${stripped%/}"
            # 必须以 / 结尾（表示目录）或不包含 .（不是文件）
            if [[ "$line" == *"/" && "$stripped" != *.* && "$stripped" != */* && -n "$stripped" ]]; then
                dir_name="$stripped"
            elif [[ "$stripped" == */* ]]; then
                local first="${stripped%%/*}"
                if [[ "$first" != *.* && -n "$first" ]]; then
                    dir_name="$first"
                fi
            fi
        # 模式 2: xxx/ (明确以 / 结尾，表示目录)
        elif [[ "$line" == */ ]]; then
            dir_name="${line%/}"
            dir_name="${dir_name#/}"
            if [[ "$dir_name" == */* ]]; then
                dir_name="${dir_name%%/*}"
            fi
        # 模式 3: .xxx/ 或 .xxx (点开头的隐藏目录，如 .settings/)
        # 注意：必须以 / 结尾才是目录，单独的 .xxx 可能是文件
        # 已在模式 2 处理

        # 模式 4: 纯单词目录名（不含 . 和 /，如 logs、venv、contracts）
        elif [[ "$line" =~ ^[a-zA-Z][a-zA-Z0-9_-]*$ ]]; then
            dir_name="$line"
        fi

        if [[ -n "$dir_name" && "$dir_name" != "." && "$dir_name" != ".." && ${#dir_name} -gt 1 ]]; then
            case " $seen " in
                *" $dir_name "*) ;;
                *)
                    echo "$dir_name"
                    seen="$seen $dir_name"
                    ;;
            esac
        fi
    done < "$gitignore_file"
}

# 预构建前端文件的搜索参数 (提高 grep 效率)
include_args=()
if [ "$CHECK_FRONTEND" == "true" ] && [ ${#FRONTEND_DIRS[@]} -gt 0 ] && [ ${#EXTENSIONS[@]} -gt 0 ]; then
    for ext in "${EXTENSIONS[@]}"; do
        include_args+=("--include=*.$ext")
    done
fi

class_name=""
base_path=""
tag_name=""
doc_status=""

echo "开始扫描并生成 API 文档到: $OUTPUT_FILE ..."

# 构建 find 排除参数
FIND_EXCLUDE_ARGS=()
collect_exclude_dirs() {
    local all_excludes=()

    for dir in "${EXCLUDE_DIRS[@]}"; do
        all_excludes+=("$dir")
    done

    if [ "$USE_GITIGNORE" == true ]; then
        local gitignore_file
        if [ -n "$GITIGNORE_PATH" ]; then
            gitignore_file="$GITIGNORE_PATH"
        else
            gitignore_file="$CONTROLLER_DIR/.gitignore"
        fi
        if [ -f "$gitignore_file" ]; then
            while IFS= read -r dir; do
                [ -n "$dir" ] && all_excludes+=("$dir")
            done < <(parse_gitignore_dirs "$gitignore_file")
        fi
    fi

    local seen=""
    for dir in "${all_excludes[@]}"; do
        if [[ " $seen " != *" $dir "* ]]; then
            FIND_EXCLUDE_ARGS+=("$dir")
            seen="$seen $dir"
        fi
    done
}
collect_exclude_dirs

if [ ${#FIND_EXCLUDE_ARGS[@]} -gt 0 ]; then
    echo "已排除目录 (${#FIND_EXCLUDE_ARGS[@]} 个): ${FIND_EXCLUDE_ARGS[*]}"
fi

> "$OUTPUT_FILE"

# 遍历所有 Controller 文件并输出到 OUTPUT_FILE
{
while IFS= read -r file_part; do
    name=$(basename "$file_part" .java)
    class_name="$name"
    base_path=""
    
    # 提取类级别的 @RequestMapping (可能有空格缩进)
    class_mapping=$(grep -E '^[[:space:]]*@RequestMapping' "$file_part" | head -1)
    if [ -n "$class_mapping" ]; then
        base_path=$(echo "$class_mapping" | grep -o 'value[[:space:]]*=[[:space:]]*"[^"]*"' | cut -d'"' -f2)
        [ -z "$base_path" ] && base_path=$(echo "$class_mapping" | grep -o '"[^"]*"' | head -1 | tr -d '"')
    fi
    
    # 提取类级别的 @Tag 或类注释
    tag_name=$(grep -o '@Tag([[:space:]]*name[[:space:]]*=[[:space:]]*"[^"]*"' "$file_part" | head -1 | sed -E 's/.*name[[:space:]]*=[[:space:]]*"([^"]*)".*/\1/')
    if [ -z "$tag_name" ]; then
        # 查找类定义前最近的星号注释
        tag_name=$(grep -B 10 "class $class_name" "$file_part" | grep -E "^[[:space:]]*\*.*" | grep -v "@" | tail -1 | sed -E 's/^[[:space:]]*\*+[[:space:]]*//' | cut -d' ' -f1-5)
        [ -z "$tag_name" ] && tag_name="$class_name"
    fi
    
    # 判断 Controller 是否在文档中
    doc_status=""
    if [ -f "$DOC_PATH" ]; then
        if grep -q "($class_name)" "$DOC_PATH"; then
            doc_status=" [FOUND]"
        else
            doc_status=" [NOT FOUND]"
        fi
    fi
    
    has_printed_class=false
    
    # 提取方法级别的 Mappings
    # 使用 grep -Hn 提取包含 Mapping 的行，排除了类级别的 RequestMapping 
    while IFS= read -r line; do
        line_num=$(echo "$line" | cut -d':' -f1)
        code_line=$(echo "$line" | cut -d':' -f2-)
        
        # 剔除类级别的映射行 (如果存在)
        if [[ "$code_line" == "$class_mapping" ]]; then
            continue
        fi

        if [ "$has_printed_class" = false ]; then
            echo "- **${tag_name}(${class_name})**${doc_status}"
            has_printed_class=true
            controller_count=$((controller_count+1))
            # （以事件日志记录替代关联数组）
        fi

        method_path=$(echo "$code_line" | grep -o 'value[[:space:]]*=[[:space:]]*"[^"]*"' | cut -d'"' -f2)
        [ -z "$method_path" ] && method_path=$(echo "$code_line" | grep -o '"[^"]*"' | head -1 | tr -d '"')
        
        # 获取上方的方法注释或 @Operation
        summary_name=$(awk -v ln="$line_num" 'NR>=ln-3 && NR<ln' "$file_part" | grep -o '@Operation([[:space:]]*summary[[:space:]]*=[[:space:]]*"[^"]*"' | head -1 | sed -E 's/.*summary[[:space:]]*=[[:space:]]*"([^"]*)".*/\1/')
        if [ -z "$summary_name" ]; then
            summary_name=$(awk -v ln="$line_num" 'NR>=ln-6 && NR<ln' "$file_part" | grep -E "^[[:space:]]*\*[[:space:]]*" | grep -v "@" | tail -1 | sed -E 's/^[[:space:]]*\*+[[:space:]]*//')
        fi
        
        mapping_type=$(echo "$code_line" | awk -F'@' '{print $2}' | awk -F'Mapping' '{print $1}' | tr '[:lower:]' '[:upper:]')
        if [[ "$mapping_type" == *REQUEST* || "$mapping_type" == "REQUEST" ]]; then
            if [[ "$code_line" =~ RequestMethod\.([A-Z_]+) ]]; then
                mapping_type="${BASH_REMATCH[1]}"
            else
                mapping_type="ALL"
            fi
        fi
        
        full_path="${base_path}${method_path}"
        # 移除多余的斜杠
        full_path=$(echo "$full_path" | sed 's/\/\//\//g')

        item_doc_status=""
        if [ -n "$DOC_PATH" ] && [ -f "$DOC_PATH" ]; then
            if grep -q "\`${full_path}\`" "$DOC_PATH" || grep -Fq " ${full_path} " "$DOC_PATH" || grep -Fq "](${full_path})" "$DOC_PATH"; then
                item_doc_status=" [FOUND]"
                found_count=$((found_count+1))
                echo "DOC|$tag_name" >> "$STATS_EVENTS_FILE"
            else
                item_doc_status=" [NOT FOUND]"
                not_found_count=$((not_found_count+1))
            fi
        fi

        frontend_status=""
        if [ "$CHECK_FRONTEND" == "true" ] && [ ${#FRONTEND_DIRS[@]} -gt 0 ] && [ ${#include_args[@]} -gt 0 ]; then
            for frontend_dir in "${FRONTEND_DIRS[@]}"; do
                if [ -d "$frontend_dir" ]; then
                    # 优化：使用 grep -r -q -m 1 大幅提升扫描速度
                    if grep -r -q -m 1 "${include_args[@]}" "[\"'/\`]$full_path[\"'/?\`]" "$frontend_dir" 2>/dev/null || grep -r -q -m 1 "${include_args[@]}" "[\"'/\`]$full_path$" "$frontend_dir" 2>/dev/null; then
                        frontend_status=" [USED]"
                        frontend_call_count=$((frontend_call_count+1))
                        echo "USED|$tag_name" >> "$STATS_EVENTS_FILE"
                        break
                    fi
                fi
            done
            if [ -z "$frontend_status" ]; then
                frontend_status=" [UNUSED]"
            fi
        fi

        # 记录服务分组总数事件
        echo "TOTAL|$tag_name" >> "$STATS_EVENTS_FILE"

        echo "  - [x] $mapping_type ${full_path} ${summary_name}${item_doc_status}${frontend_status}"
        count=$((count+1))
        
    done < <(grep -Hn -E '^[[:space:]]*@(Get|Post|Put|Delete|Patch|Request)Mapping' "$file_part" | cut -d':' -f2-)
done < <(
    find_args=("$CONTROLLER_DIR")
    if [ ${#FIND_EXCLUDE_ARGS[@]} -gt 0 ]; then
        find_args+=("(")
        first=true
        for dir in "${FIND_EXCLUDE_ARGS[@]}"; do
            if [ "$first" = true ]; then
                first=false
            else
                find_args+=("-o")
            fi
            find_args+=("-name" "$dir")
        done
        find_args+=(")")
        find_args+=("-type" "d")
        find_args+=("-prune")
        find_args+=("-o")
    fi
    find_args+=("-name" "*Controller.java")
    find_args+=("-type" "f")
    find_args+=("-print")
    find "${find_args[@]}"
)
} > "$OUTPUT_FILE"

echo "生成完成！文档已保存至: $OUTPUT_FILE"

if [ "$SHOW_STATS" == "true" ]; then
    echo "=============================="
    echo "统计信息:"
    echo "接口总数: $count"
    echo "控制器数量: $controller_count"
    echo "控制器已实现且文档已记录: $found_count"
    echo "控制器已实现但文档未记录: $not_found_count"

    if [ "$CHECK_FRONTEND" == "true" ]; then
        echo "前端代码中使用的接口: $frontend_call_count"
        echo "前端代码中未使用的接口: $((count - frontend_call_count))"
    fi
fi

# 输出结构化 JSON 统计文件
if [ "$STATS_JSON" == "true" ]; then
    generated_at=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    frontend_unused=$((count - frontend_call_count))
    doc_rate="0"
    if [ "$count" -gt 0 ]; then
        doc_rate=$(echo "scale=1; $found_count * 100 / $count" | bc 2>/dev/null || echo "0")
    fi
    used_rate="0"
    if [ "$count" -gt 0 ]; then
        used_rate=$(echo "scale=1; $frontend_call_count * 100 / $count" | bc 2>/dev/null || echo "0")
    fi

    # 构建按服务分组的 JSON
    service_json_parts=""
    if [ -f "$STATS_EVENTS_FILE" ]; then
        service_json_parts=$(awk -F'|' '{
            if ($1 == "TOTAL") total[$2]++
            if ($1 == "DOC") doc[$2]++
            if ($1 == "USED") used[$2]++
        } END {
            first=1
            for (tag in total) {
                safe_tag = tag
                gsub(/"/, "\\\"", safe_tag)
                part = sprintf("\"%s\": {\"total\": %d, \"documented\": %d, \"used\": %d}", safe_tag, total[tag], doc[tag]+0, used[tag]+0)
                if (first) {
                    printf "%s", part
                    first=0
                } else {
                    printf ", %s", part
                }
            }
        }' "$STATS_EVENTS_FILE")
    fi

    cat > "$STATS_JSON_FILE" << EOFJSON
{
  "generatedAt": "${generated_at}",
  "controllers": ${controller_count},
  "totalEndpoints": ${count},
  "documentedCount": ${found_count},
  "undocumentedCount": ${not_found_count},
  "frontendUsedCount": ${frontend_call_count},
  "frontendUnusedCount": ${frontend_unused},
  "documentationRate": ${doc_rate},
  "frontendUsageRate": ${used_rate},
  "byService": { ${service_json_parts} }
}
EOFJSON

    echo "统计 JSON 已保存至: $STATS_JSON_FILE"
fi
