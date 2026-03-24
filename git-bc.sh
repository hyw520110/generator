#!/bin/bash

# Git 分支差异对比及分支合并工具
# 用于比较两个 Git 分支之间的文件差异，并提供交互式文件对比功能

# 初始化变量
current_branch=""
compare_branch=""
local_branch=""
git_dir=""
analyze_workload=false
# 存储用户自定义的排除模式
custom_exclude_patterns=()

# 获取需要对比的文件列表（排除构建目录、依赖目录和其他不需要对比的文件）
# 默认排除项目相关的目录和文件
exclude_patterns=(
    # 构建输出目录
    '^dist/'
    '^build/'
    '^target/'
    '^out/'
    
    # 依赖管理目录
    '^node_modules/'
    '^\.m2/'
    '^\.gradle/'
    
    # 锁文件
    'yarn\.lock$'
    'package-lock\.json$'
    '\.lock$'
    
    # IDE 和编辑器相关
    '^\.vscode/'
    '^\.idea/'
    '^\.hbuilderx/'
    '\.swp$'
    '\.swo$'
    
    # 版本控制相关
    '^\.git/'
    '\.patch$'
    
    # 日志文件
    '\.log$'
)

# 显示帮助信息
show_help() {
    echo "用法: $0 [选项]"
    echo "选项:"
    echo "  -h, --help                    显示此帮助信息"
    echo "  -r, --remote BRANCH           指定要对比的远程分支（默认为 origin/当前分支）"
    echo "  -c, --current BRANCH          指定当前分支（默认为当前检出的分支）"
    echo "  -d, --dir DIRECTORY           指定 Git 目录（默认为当前目录）"
    echo "  -l, --local-branch BRANCH     指定要对比的本地分支（替代远程分支）"
    echo "  -e, --exclude PATTERN         添加额外的排除模式（可多次使用,支持正则表达式，如 '^config/'）"
    echo "  -m, --merge-workload          对比评估分支合并的工作量"
    echo ""
    echo "示例:"
    echo "  $0                                    # 对比当前分支与远程分支"
    echo "  $0 -r develop                         # 对比当前分支与 origin/develop"
    echo "  $0 -c feature-a -r develop            # 对比 feature-a 分支与 origin/develop"
    echo "  $0 -c feature-a -l develop            # 对比 feature-a 分支与本地 develop 分支"
    echo "  $0 -d /path/to/repo                   # 指定 Git 目录"
    echo "  $0 -e \"^config/\" -e \"\.md$\"          # 添加自定义排除模式"
}

safe_escape() {
    printf "%q" "$1"
}
# 处理合并冲突的函数
handle_merge_conflict() {
    local target_branch=$1
    local source_branch=$2

    echo "❌ 合并失败，存在冲突。"
    echo "请按照以下步骤解决冲突："
    echo "1. 使用 'git status' 查看有冲突的文件。"

    local merge_tool=$(git config --get merge.tool)
    if [ -n "$merge_tool" ]; then
        echo "2. 您已配置合并工具 '$merge_tool'。"
    else
        echo "2. 手动编辑这些文件以解决冲突。"
        echo "   您可以运行 'git config --global merge.tool <tool>' 配置一个可视化合并工具。"
    fi

    echo "3. 解决所有冲突后，使用 'git add .' 将更改标记为已解决。"
    if [ "$source_branch" != "unknown" ]; then
        echo "4. 运行以下命令完成合并提交:"
        echo "   git commit -m '合并分支 '$source_branch' 到 '$target_branch''"
    else
        echo "4. 运行 'git commit' 来完成合并。"
    fi
    echo "如果想放弃本次合并，可以运行 'git merge --abort'。"
    echo ""
    if [ -n "$merge_tool" ]; then
      read -p "是否现在运行 'git mergetool' 来解决冲突? [Y/n]: " run_mergetool
      run_mergetool=${run_mergetool:-Y}
      if [[ "$run_mergetool" =~ ^[Yy]$ ]]; then
     	git mergetool
      fi
    fi  
}


# 执行合并操作的函数
execute_merge() {
    local target_branch=$1
    local source_branch=$2
    local original_branch
    original_branch=$(git rev-parse --abbrev-ref HEAD)
	if [ "$target_branch" = "$source_branch" ]; then
	  echo "❌ 错误: 目标分支和源分支相同 ($target_branch)"
	  return 1
	fi
	if [[ "$target_branch" == origin/* ]]; then
	    local local_name="${target_branch#origin/}"
	    if ! git branch -q | grep -q "^$local_name$"; then
	        git checkout -b "$local_name" --track "$target_branch"
	    fi
	    target_branch="$local_name"
	fi
	
    # 如果目标分支不是当前分支，则需要最终切换回来
    local needs_checkout_back=false
    if [ "$original_branch" != "$target_branch" ]; then
        needs_checkout_back=true
    fi

    # 切换到目标分支
    if [ "$needs_checkout_back" = true ]; then
        echo "正在切换到分支 '$target_branch'..."
        echo "即将执行: git checkout "$target_branch""
        if ! git checkout "$target_branch"; then
            echo "❌ 切换到分支 '$target_branch' 失败。"
            # 切换失败，我们仍在 original_branch，所以无需做任何事
            return 1
        fi
    fi

    # 执行合并
    echo "正在将分支 '$source_branch' 合并到 '$target_branch'..."
    echo "即将执行: git merge "$source_branch""
    if ! git merge "$source_branch"; then
        # 合并失败
        handle_merge_conflict "$target_branch" "$source_branch"
        echo "合并出现冲突。正在中止合并操作..."
        echo "即将执行: git merge --abort"
        if ! git merge --abort; then
            echo "❌ 警告: 'git merge --abort' 失败。"
            echo "仓库可能处于不稳定状态。请手动解决。"
            # 发生严重错误，不要尝试自动切换分支
            needs_checkout_back=false
        else
            echo "已自动中止合并。"
        fi
    else
        # 合并成功
        echo "✅ 自动合并成功！"
    fi

    # 如果需要，切换回原始分支
    if [ "$needs_checkout_back" = true ]; then
        echo "操作完成，正在切换回原始分支 '$original_branch'..."
        echo "即将执行: git checkout "$original_branch""
        if ! git checkout "$original_branch"; then
            echo "❌ 紧急警告: 无法自动切换回原始分支 '$original_branch'。" >&2
            echo "仓库可能处于中间状态。请手动执行 'git checkout $original_branch' 来恢复。" >&2
        fi
    fi
}


# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;; 
        -c|--current)
            if [[ -z "$2" || "$2" == -* ]]; then
                echo "❌ 错误: -c/--current 选项需要指定分支名称"
                show_help
                exit 1
            fi
            current_branch="$2"
            shift 2
            ;; 
        -r|--remote)
            if [[ -z "$2" || "$2" == -* ]]; then
                echo "❌ 错误: -r/--remote 选项需要指定分支名称"
                show_help
                exit 1
            fi
            compare_branch="$2"
            shift 2
            ;; 
        -l|--local-branch)
            if [[ -z "$2" || "$2" == -* ]]; then
                echo "❌ 错误: -l/--local-branch 选项需要指定分支名称"
                show_help
                exit 1
            fi
            local_branch="$2"
            shift 2
            ;; 
        -d|--dir)
            if [[ -z "$2" || "$2" == -* ]]; then
                echo "❌ 错误: -d/--dir 选项需要指定目录路径"
                show_help
                exit 1
            fi
            git_dir="$2"
            shift 2
            ;; 
        -e|--exclude)
            if [[ -z "$2" || "$2" == -* ]]; then
                echo "❌ 错误: -e/--exclude 选项需要指定排除模式"
                show_help
                exit 1
            fi
            custom_exclude_patterns+=("$2")
            shift 2
            ;; 
        -m|--merge-workload)
            analyze_workload=true
            shift
            ;; 
        *)
            echo "未知选项: $1"
            show_help
            exit 1
            ;; 
    esac
done

# 验证 Git 目录路径安全性
validate_git_dir() {
    local dir="$1"
    
    # 检查路径是否为空
    if [ -z "$dir" ]; then
        echo "❌ 错误: 目录路径不能为空"
        return 1
    fi
    
    # 规范化路径
    if command -v realpath >/dev/null 2>&1; then
        normalized_dir=$(realpath "$dir")
    else
        # macOS 兼容方案
        normalized_dir=$(cd "$dir" && pwd -P)
    fi
    if [ -z "$normalized_dir" ]; then
        echo "❌ 错误: 无法解析目录路径 '$dir'"
        return 1
    fi
    
    # 检查路径是否指向目录
    if [ ! -d "$normalized_dir" ]; then
        echo "❌ 错误: $normalized_dir 不是一个有效的目录"
        return 1
    fi
    
    # 检查目录是否为 Git 仓库
    if [ ! -d "$normalized_dir/.git" ]; then
        echo "❌ 错误: $normalized_dir 不是一个 Git 目录"
        return 1
    fi
    
    echo "$normalized_dir"
    return 0
}

# 如果未指定 Git 目录，则使用当前目录
if [ -z "$git_dir" ]; then
    git_dir="$(pwd)"
else
    # 验证指定的 Git 目录
    git_dir=$(validate_git_dir "$git_dir") || exit 1
fi

# 进入 Git 目录
cd "$git_dir" || {
    echo "❌ 错误: 无法进入目录 $git_dir"
    exit 1
}

echo "Git 目录: $(pwd)"

# 验证当前目录确实是 Git 仓库
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo "❌ 错误: 当前目录不是 Git 仓库"
    exit 1
fi

# 如果未指定当前分支，则获取当前分支名称
if [ -z "$current_branch" ]; then
    current_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)
    # 检查命令是否成功执行
    if [ $? -ne 0 ]; then
        echo "❌ 错误: 无法获取当前分支信息"
        exit 1
    fi
fi

# 验证分支名称安全性
validate_branch_name() {
    local branch="$1"
    
    # 检查分支名称是否为空
    if [ -z "$branch" ]; then
        echo "❌ 错误: 分支名称不能为空"
        return 1
    fi
    
    # 检查分支名称长度（Git 限制为 256 字符）
    if [ ${#branch} -gt 256 ]; then
        echo "❌ 错误: 分支名称过长（超过 256 字符）"
        return 1
    fi
    
    # 检查是否包含非法字符（Git 分支名称不能包含以下字符）
    # 允许的字符：字母、数字、以及 . _ - /（但不能以这些字符开头或结尾，也不能连续出现）
    if [[ "$branch" =~ [^a-zA-Z0-9._/-] ]]; then
        echo "❌ 错误: 分支名称包含非法字符"
        return 1
    fi
    
    # 检查是否以斜杠、点或连字符开头或结尾
    if [[ "$branch" =~ ^[./-]|[-/]$ ]]; then
        echo "❌ 错误: 分支名称不能以斜杠、点或连字符开头或结尾"
        return 1
    fi
    
    # 检查是否包含连续的斜杠、点或连字符
    if [[ "$branch" =~ \.\.|//|-- ]]; then
        echo "❌ 错误: 分支名称不能包含连续的点或斜杠"
        return 1
    fi
    
    # 检查是否包含特殊文件名
    if [[ "$branch" == " @" || "$branch" =~ @{ ]]; then
        echo "❌ 错误: 分支名称不能包含 ' @{' 或单独的 ' @'"
        return 1
    fi
    
    # 检查是否为 Git 保留名称
    if [[ "$branch" == "HEAD" || "$branch" == "MERGE_HEAD" || "$branch" == "FETCH_HEAD" ]]; then
        echo "❌ 错误: '$branch' 是 Git 保留名称"
        return 1
    fi
    
    return 0
}

# 验证当前分支名称安全性
if ! validate_branch_name "$current_branch"; then
    exit 1
fi

# 验证当前分支是否存在
if ! git show-ref --verify --quiet "refs/heads/$current_branch"; then
    echo "❌ 错误: 当前分支 '$current_branch' 不存在"
    exit 1
fi

# 处理分支对比逻辑
if [ -n "$local_branch" ]; then
    # 验证本地分支名称安全性
    if ! validate_branch_name "$local_branch"; then
        exit 1
    fi
    
    # 使用指定的本地分支进行对比
    compare_branch="$local_branch"
    echo "当前分支 (左边): $current_branch"
    echo "对比分支 (右边): $compare_branch (本地分支)"
    
    # 验证本地分支是否存在
    if ! git show-ref --verify --quiet "refs/heads/$local_branch"; then
        echo "❌ 错误: 本地分支 '$local_branch' 不存在"
        exit 1
    fi
elif [ -n "$compare_branch" ]; then
    # 验证远程分支名称安全性
    if ! validate_branch_name "$compare_branch"; then
        exit 1
    fi
    
    # 使用指定的远程分支进行对比
    remote_branch="$compare_branch"
    compare_branch="origin/$compare_branch"
    echo "当前分支 (左边): $current_branch"
    echo "对比分支 (右边): $compare_branch (远程分支)"
    
    # 验证远程分支是否存在
    if ! git show-ref --verify --quiet "refs/remotes/origin/$remote_branch"; then
        echo "⚠️  警告: 远程分支 'origin/$remote_branch' 不存在，将继续执行但可能无结果"
    fi
else
    # 默认使用当前分支的远程分支进行对比
    compare_branch="origin/$current_branch"
    echo "当前分支 (左边): $current_branch"
    echo "对比分支 (右边): $compare_branch (默认远程分支)"
    
    # 验证远程分支是否存在
    if ! git show-ref --verify --quiet "refs/remotes/origin/$current_branch"; then
        echo "⚠️  警告: 远程分支 '$compare_branch' 不存在，将继续执行但可能无结果"
    fi
fi

echo "正在获取远程分支状态..."
# 只有在需要时才更新远程分支状态
if [[ "$compare_branch" == origin/* ]]; then
    git fetch origin
    # 检查命令是否成功执行
    if [ $? -ne 0 ]; then
        echo "❌ 错误: 无法获取远程分支状态"
        exit 1
    fi
fi
# 新增函数：安全格式化时间戳（兼容Linux/macOS）
format_timestamp() {
    local ts=$1
    if [ "$ts" = "0" ]; then
        echo "未知"
        return
    fi

    local formatted_date
    # 尝试Linux格式化
    formatted_date=$(date -d "@$ts" "+%Y-%m-%d %H:%M:%S" 2>/dev/null)
    
    if [ -n "$formatted_date" ]; then
        echo "$formatted_date"
    else
        # macOS fallback
        date -j -f "%s" "$ts" "+%Y-%m-%d %H:%M:%S" 2>/dev/null || echo "未知"
    fi
}
# 分析合并工作量的函数
analyze_merge_workload() {
    local branch1="$1"
    local branch2="$2"
    
    echo ""
    echo "📊 分析合并工作量..."
    
    # 获取分支信息
    echo "分支信息:"
    echo "  当前分支: $branch1"
    echo "  目标分支: $branch2"
    
    # 获取分支的最新提交时间
    local branch1_time=$(git log -1 --format="%ct" "$branch1" 2>/dev/null || echo "0")
    local branch2_time=$(git log -1 --format="%ct" "$branch2" 2>/dev/null || echo "0")
    
    if [ "$branch1_time" != "0" ]; then
        local branch1_date=$(format_timestamp "$branch1_time")
        echo "  $branch1 最新提交: $branch1_date"
    fi
    
    if [ "$branch2_time" != "0" ]; then
        local branch2_date=$(format_timestamp "$branch2_time")
        echo "  $branch2 最新提交: $branch2_date"
    fi
    
    # 获取共同祖先
    local merge_base=$(git merge-base "$branch1" "$branch2" 2>/dev/null)
    if [ -n "$merge_base" ]; then
        local base_time=$(git log -1 --format="%ct" "$merge_base" 2>/dev/null || echo "0")
        if [ "$base_time" != "0" ]; then
            local base_date=$(date -d "@$base_time" "+%Y-%m-%d %H:%M:%S")
            echo "  共同祖先: $base_date ($merge_base)"
        fi
    fi
    
    echo ""
    echo "提交统计:"
    
    # 统计各分支独有的提交数量
    local branch1_commits=$(git rev-list --count "$branch1" ^"$branch2" 2>/dev/null || echo "0")
    local branch2_commits=$(git rev-list --count "$branch2" ^"$branch1" 2>/dev/null || echo "0")
    
    echo "  $branch1 独有提交: $branch1_commits 个"
    echo "  $branch2 独有提交: $branch2_commits 个"
    
    # 获取差异文件统计
    echo ""
    echo "文件差异统计:"
    
    # 统计 branch1 -> branch2 的差异
    local files_1_to_2=$(git diff --name-only "$branch1" "$branch2" 2>/dev/null | wc -l)
    echo "  差异文件总数: $files_1_to_2 个"
    
    # 统计增加和删除的行数
    local diff_stats=$(git diff --stat "$branch1" "$branch2" 2>/dev/null | tail -1)
    if [ -n "$diff_stats" ]; then
        echo "  $diff_stats"
    fi
    
    # 过滤后的文件列表
    local all_files=$(git diff --name-only "$branch1" "$branch2" 2>/dev/null)
    local filtered_files=()
    
    if [ -n "$all_files" ]; then
        while IFS= read -r file; do
            [ -z "$file" ] && continue
            
            local should_exclude=false
            for pattern in "${exclude_patterns[@]}"; do
                if [[ "$file" =~ $pattern ]]; then
                    should_exclude=true
                    break
                fi
            done
            
            if [ "$should_exclude" = false ]; then
                filtered_files+=("$file")
            fi
        done <<< "$all_files"
    fi
    
    echo "  需要处理的文件: ${#filtered_files[@]} 个（排除构建文件等）"
    
    # 分析合并复杂度
    echo ""
    echo "合并复杂度分析:"
    
    # 检查是否有潜在冲突
    local conflict_files=0
    local modified_same_files=()
    
    if [ ${#filtered_files[@]} -gt 0 ]; then
        #for file in "${filtered_files[@]}"; do
        #    local file_in_1=$(git log --oneline "$merge_base".."$branch1" -- "$file" 2>/dev/null | wc -l)
        #    local file_in_2=$(git log --oneline "$merge_base".."$branch2" -- "$file" 2>/dev/null | wc -l)
        #if [ "$file_in_1" -gt 0 ] && [ "$file_in_2" -gt 0 ]; then
        #        modified_same_files+=("$file")
        #        conflict_files=$((conflict_files + 1))
        #    fi
        #done
        # 一次性获取所有文件的修改状态
		local diff_status=$(git diff --name-status "$merge_base" "$branch1" "$branch2" 2>/dev/null)
		while IFS= read -r line; do
		  local status=$(echo "$line" | awk '{print $1}')
		  local file=$(echo "$line" | awk '{print $2}')
		  if [ "$status" = "M" ] || [ "$status" = "A" ] || [ "$status" = "D" ]; then
		    # 检查是否在两个分支中都有修改
		    if [[ "$diff_status" =~ "[$branch1]" && "$diff_status" =~ "[$branch2]" ]]; then
		      modified_same_files+=("$file")
		       conflict_files=$((conflict_files + 1))
		     fi
		   fi
		 done <<< "$diff_status"    
    fi
	# 该命令较新(Git 2.39+)，兼容性需考虑    
    if git merge-tree --write-tree "$branch1" "$branch2" >/dev/null 2>&1; then
    	echo "无冲突"
	else
	    echo "  可能冲突的文件: $conflict_files 个"
	fi
    # 计算合并工作量评分
    echo ""
    echo "合并工作量评估:"
    
    # 工作量评分算法：
    # - 基础分：差异文件数量 * 1
    # - 提交数差异：abs(branch1_commits - branch2_commits) * 2  
    # - 潜在冲突：conflict_files * 5
    # - 时间差异：如果分支分离时间过长，增加复杂度
    
    #local base_score=${#filtered_files[@]}
    # local commit_diff_score=$(( (branch1_commits > branch2_commits ? branch1_commits - branch2_commits : branch2_commits - branch1_commits) * 2 ))
    # local conflict_score=$((conflict_files * 5))
	local base_score=$(( ${#filtered_files[@]} * 1 ))
	local commit_diff_score=$(( (branch1_commits > branch2_commits ? branch1_commits - branch2_commits : branch2_commits - branch1_commits) * 2 ))
	local conflict_score=$(( conflict_files * 2 ))  # 降低冲突权重

    # 时间差异评分
    local time_diff_score=0
    if [ "$branch1_time" != "0" ] && [ "$branch2_time" != "0" ] && [ -n "$merge_base" ]; then
        local base_time_val=$(git log -1 --format="%ct" "$merge_base" 2>/dev/null || echo "0")
        if [ "$base_time_val" != "0" ]; then
            local days_since_split=$(( (branch1_time > branch2_time ? branch1_time : branch2_time) - base_time_val ))
            days_since_split=$((days_since_split / 86400))  # 转换为天数
            if [ $days_since_split -gt 30 ]; then
                time_diff_score=$((days_since_split / 10))
            fi
        fi
    fi
    
    local total_score=$((base_score + commit_diff_score + conflict_score + time_diff_score))
    
    echo ""
    echo "工作量评分详情:"
    echo "  基础文件差异: $base_score 分"
    echo "  提交数差异: $commit_diff_score 分"
    echo "  潜在冲突: $conflict_score 分"
    echo "  时间差异: $time_diff_score 分"
    echo "  总评分: $total_score 分"
    
    # 给出建议
    echo ""
    echo "合并建议:"
    if [ $total_score -lt 10 ]; then
        echo "  🟢 合并复杂度较低，可以直接合并"
    elif [ $total_score -lt 30 ]; then
        echo "  🟡 合并复杂度中等，建议仔细检查差异后合并"
    elif [ $total_score -lt 60 ]; then
        echo "  🟠 合并复杂度较高，建议分步骤合并或寻求协助"
    else
        echo "  🔴 合并复杂度很高，强烈建议详细规划合并策略"
    fi
    
    # 双向合并工作量对比
    echo ""
    echo "双向合并工作量对比:"
    
    # 方案1：将 branch2 合并到 branch1 
    local scenario1_conflicts=$conflict_files
    local scenario1_new_commits=$branch2_commits
    echo "方案1: 将 $branch2 合并到 $branch1"
    echo "  需要处理的新提交: $scenario1_new_commits 个"
    echo "  可能的冲突文件: $scenario1_conflicts 个"
    echo "  工作量评估: $((scenario1_new_commits + scenario1_conflicts * 3)) 分"
    
    # 方案2：将 branch1 合并到 branch2   
    local scenario2_conflicts=$conflict_files
    local scenario2_new_commits=$branch1_commits
    echo ""
    echo "方案2: 将 $branch1 合并到 $branch2"
    echo "  需要处理的新提交: $scenario2_new_commits 个"
    echo "  可能的冲突文件: $scenario2_conflicts 个"
    echo "  工作量评估: $((scenario2_new_commits + scenario2_conflicts * 3)) 分"
    
    # 给出推荐方案
    echo ""
    local scenario1_total=$((scenario1_new_commits + scenario1_conflicts * 3))
    local scenario2_total=$((scenario2_new_commits + scenario2_conflicts * 3))

    if [ $scenario1_total -lt $scenario2_total ]; then
        echo "🎯 推荐方案: 方案1 ($branch2 合并到 $branch1)"
        echo "   原因: 工作量较小（${scenario1_total}分 vs ${scenario2_total}分）"
    elif [ $scenario2_total -lt $scenario1_total ]; then
        echo "🎯 推荐方案: 方案2 ($branch1 合并到 $branch2)"
        echo "   原因: 工作量较小（${scenario2_total}分 vs ${scenario1_total}分）"
    else
        echo "🎯 两种方案工作量相当（均为${scenario1_total}分），可根据项目流程选择"
    fi
    
    # 显示潜在冲突文件
    if [ ${#modified_same_files[@]} -gt 0 ]; then
        echo ""
        echo "需要特别注意的文件（可能冲突）:"
        for file in "${modified_same_files[@]}"; do
            echo "  - $file"
        done
    fi

    echo ""
    read -p "请选择合并方案 [1|2] 或按 Enter 退出: " merge_choice

    case $merge_choice in
        1)
            execute_merge "$branch1" "$branch2"
            ;;
        2)
            local local_branch2=$branch2
            if [[ "$branch2" == origin/* ]]; then
                local_branch2=${branch2#origin/}
            fi
            execute_merge "$local_branch2" "$branch1"
            ;;
        *)
            echo "已退出合并操作"
            ;;
    esac
}

# 添加用户自定义的排除模式
for pattern in "${custom_exclude_patterns[@]}"; do
    exclude_patterns+=("$pattern")
done

# 获取需要对比的文件列表
echo "正在获取差异文件列表..."
all_files=$(git diff --name-only "$current_branch" "$compare_branch" 2>/dev/null)

# 过滤文件列表，排除不需要的文件
filtered_files=()
if [ -n "$all_files" ]; then
    while IFS= read -r file; do
        # 跳过空行
        [ -z "$file" ] && continue
        
        should_exclude=false
        
        # 检查是否匹配任何排除模式
        for pattern in "${exclude_patterns[@]}"; do
            if [[ "$file" =~ $pattern ]]; then
                should_exclude=true
                echo "排除文件: $file (匹配模式: $pattern)"
                break
            fi
        done
        
        # 如果不需要排除，则添加到结果中
        if [ "$should_exclude" = false ]; then
            filtered_files+=("$file")
        fi
    done <<< "$all_files"
fi

# 如果是工作量分析模式
if [ "$analyze_workload" = true ]; then
    analyze_merge_workload "$current_branch" "$compare_branch"
    exit 0
fi

# 如果没有需要对比的文件，则退出
if [ ${#filtered_files[@]} -eq 0 ]; then
    echo "🎉 没有需要对比的文件，$current_branch 分支与 $compare_branch 分支无差异"
    exit 0
fi

total_files=${#filtered_files[@]}

# 显示需要对比的文件列表
echo "🔍 发现以下差异文件:"
for i in "${!filtered_files[@]}"; do
    printf "%4d. %s\n" $((i+1)) "${filtered_files[i]}"
done
echo ""
echo "共计: $total_files 个文件"

# 安全转义函数，防止命令注入
safe_escape() {
    local input="$1"
    # 使用 printf %q 进行安全转义
    printf "%q" "$input"
}

# 同步文件到指定分支并提交
sync_and_commit() {
    local source_branch="$1"
    local target_branch="$2"
    local file="$3"
    local commit_message="$4"
    
    # 保存当前分支
    local original_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)
    
    # 切换到目标分支
    if ! git checkout "$target_branch" >/dev/null 2>&1; then
        echo "❌ 错误: 无法切换到 $target_branch 分支"
        # 尝试切换回原分支
        git checkout "$original_branch" >/dev/null 2>&1
        return 1
    fi
    
    # 检出源分支的文件到目标分支
    if ! git checkout "$source_branch" -- "$file" >/dev/null 2>&1; then
        echo "❌ 错误: 无法同步文件 $file"
        # 切换回原分支
        git checkout "$original_branch" >/dev/null 2>&1
        return 1
    fi
    
    # 添加并提交更改
    if git add "$file" && git commit -m "$commit_message" >/dev/null 2>&1; then
        echo "✅ 已提交更改到 $target_branch 分支"
    else
        echo "⚠️  警告: 无法自动提交更改，请手动提交"
    fi
    
    # 切换回原分支
    if ! git checkout "$original_branch" >/dev/null 2>&1; then
        echo "⚠️  警告: 无法切换回原分支 $original_branch"
        return 1
    fi
    
    return 0
}

# 处理单个文件对比的函数
process_file() {
    local file=$1
    local index=$2
    local total=$3
    
    # 检查文件是否存在于 Git 中
    if git ls-files --error-unmatch "$file" &>/dev/null; then
        echo "[$index/$total] 正在对比: $file"
        echo "  左边: $current_branch 分支"
        echo "  右边: $compare_branch 分支"
        # 在 difftool 之前记录文件的 Git hash
		original_hash=$(git hash-object "$file" 2>/dev/null || echo "")
		
		# 执行 difftool
		# git difftool --no-prompt "$current_branch" "$compare_branch" -- "$file"
		# 文件路径未转义，可能导致命令注入 全局使用safe_escape处理文件路径
		git difftool --no-prompt "$current_branch" "$compare_branch" -- "$(safe_escape "$file")"
		
		# 对比当前工作区文件 hash
		new_hash=$(git hash-object "$file" 2>/dev/null || echo "")
		
        # 检查文件是否在对比工具中被修改过
        if [ "$original_hash" != "$new_hash" ]; then
            echo ""
            echo "✅ 检测到文件内容已修改"
            
            # 默认将当前分支的修改同步到对比分支
            # 这是因为 difftool 修改的总是工作目录中的文件，即当前分支的文件
            echo "正在将 '$current_branch' 分支的更新自动同步到 '$compare_branch' 分支..."

            # 步骤 A: 将工作目录中的更改提交到当前分支
            echo "  - 步骤 1/2: 提交更改到当前分支 ($current_branch)..."
            local commit_msg_current="[自动同步] 更新文件 $file"
            
            if git add "$file" && git commit -m "$commit_msg_current"; then
                echo "    ✅ 已提交更改到 '$current_branch'"
            else
                echo "    ⚠️  警告: 无法自动提交更改到 '$current_branch'。同步中止。"
                # 如果提交失败（例如，因为 pre-commit hook），则停止后续操作
                return
            fi

            # 步骤 B: 将当前分支的这个文件同步到对比分支
            echo "  - 步骤 2/2: 将文件从 '$current_branch' 同步到 '$compare_branch'..."
            sync_and_commit "$current_branch" "$compare_branch" "$file" "同步 $file 文件从 $current_branch 分支"
        else
            echo ""
            echo "ℹ️  文件在对比工具中未检测到修改，无需同步。"
        fi
    else
        echo "[$index/$total] 文件已删除或不存在: $file"
    fi
}

# 提供更清晰的输入提示
echo ""
read -p "请选择操作 [a]查看所有差异 [m]评估合并工作量 [n]退出程序 [数字]查看指定文件 [文件路径]查看指定文件: " choice

# 去除空格并转换为小写
choice=$(echo "$choice" | tr -d ' ' | tr '[:upper:]' '[:lower:]')

# 输入验证
if [[ -n "$choice" && "$choice" != "a" && "$choice" != "n" && "$choice" != "m" ]]; then
    # 如果输入的是数字，检查是否在有效范围内
    if [[ "$choice" =~ ^[0-9]+$ ]]; then
        if [ "$choice" -lt 1 ] || [ "$choice" -gt "$total_files" ]; then
            echo "❌ 错误: 文件编号应在 1 到 $total_files 之间"
            exit 1
        fi
    else
        # 如果输入的不是数字，检查是否是文件路径
        found=false
        for i in "${!filtered_files[@]}"; do
            if [[ "${filtered_files[i]}" == "$choice" ]]; then
                found=true
                break
            fi
        done
        if [ "$found" = false ]; then
            echo "❌ 错误: 无效的文件路径或选项"
            exit 1
        fi
    fi
fi

# 处理退出情况
if [[ "$choice" == "n" || -z "$choice" ]]; then
    echo "已取消操作"
    exit 0
fi

# 处理工作量分析
if [[ "$choice" == "m" ]]; then
    analyze_merge_workload "$current_branch" "$compare_branch"
    exit 0
fi

# 处理查看所有文件的情况
if [[ "$choice" == "a" ]]; then
    echo "开始对比所有文件差异..."
    echo "左边: $current_branch 分支"
    echo "右边: $compare_branch 分支"
    
    # 遍历所有文件
    for i in "${!filtered_files[@]}"; do
        file_index=$((i+1))
        process_file "${filtered_files[i]}" "$file_index" "$total_files"
    done
    
    echo "✅ 差异对比完成！已处理 $total_files 个文件。"
else
    # 处理单个文件或按编号选择文件的情况
    found=false
    for i in "${!filtered_files[@]}"; do
        file_index=$((i+1))
        # if [[ "$choice" == "${filtered_files[i]}" || "$choice" == "$file_index" ]]; then
        # 支持模糊匹配（前缀匹配）
        if [ "$choice" == "$file_index" ] || [[ "${filtered_files[i]}" == "$choice"* ]]; then
            process_file "${filtered_files[i]}" "$file_index" "$total_files"
            found=true
            break
        fi
done
    
    # 如果没有找到匹配的文件
    if [ "$found" = false ]; then
        echo "❌ 错误: 无效的文件路径或编号"
        exit 1
    fi
fi
