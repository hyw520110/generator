#!/bin/bash
# 打开浏览器
# WINDOWS
# rundll32 url.dll,FileProtocolHandler

CURRENT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PORT=$(cat ${CURRENT_DIR}/vue.config.js |grep "port:"|awk -F': ' '{print $2}')
URL="http://localhost:$PORT"

if [ "$(uname)" == "Darwin" ];then
    # MAC
    open /Applications/Safari.app $URL
elif [ "$(uname)" == "Linux" ];then
    # LINUX
    chrome=/opt/apps/cn.google.chrome/files/google/chrome/google-chrome
    firefox=/usr/share/applications/com.mozilla.firefox-zh.desktop
    if [ -f "$chrome" ];then
        $chrome $URL
    elif [ -f "$firefox" ]; then
        $firefox $URL
    fi
fi

