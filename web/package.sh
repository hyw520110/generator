#!/bin/sh

# 优先使用 mvnd（Maven Daemon）
if command -v mvnd >/dev/null 2>&1; then
    MVN="mvnd"
else
    MVN="mvn"
fi

yarn install
yarn run build

rm -rf ./src/main/resources/static/*
mv ./dist/* ./src/main/resources/static/

$MVN clean package
