#!/bin/bash
# 优先使用 mvnd（Maven Daemon）
if command -v mvnd >/dev/null 2>&1; then
    MVN="mvnd"
else
    MVN="mvn"
fi

$MVN clean install -Dmaven.test.skip=true -T 2C