#!/bin/sh

if ! command -v bcompare &> /dev/null; then
    echo "错误: 'bcompare' 命令未找到。请确保Beyond Compare已安装并将其命令行工具加入到PATH中。" >&2
    exit 127
fi

"bcompare" "$2" "$5"