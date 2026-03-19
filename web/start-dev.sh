#!/bin/bash

# 定义颜色常量
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

CURRENT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 检查 Node.js 是否已安装
check_node() {
  # 手动加载 nvm 脚本以确保其可用
  export NVM_DIR="$HOME/.nvm"
  [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
  if ! command -v node &> /dev/null; then
    echo -e "${RED}错误：Node.js 未安装，请先安装 Node.js${NC}"
    echo -e "${YELLOW}建议使用 nvm 安装和管理 Node.js 版本："
    echo -e "1. 安装 nvm：curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.3/install.sh | bash"
    echo -e "2. 重新加载 shell：source ~/.bashrc 或 source ~/.zshrc"
    echo -e "3. 安装 Node.js 17：nvm install 17${NC}"
    exit 1
  fi
}

# 检查并安装 nvm
check_nvm() {
  if [ -n "$ZSH_VERSION" ]; then
    source ~/.zshrc
  elif [ -n "$BASH_VERSION" ]; then
    source ~/.bashrc
  fi

  # 重新加载配置文件后再次检查 nvm 是否可用
  if ! command -v nvm &> /dev/null; then
    echo -e "${YELLOW}未检测到 nvm，正在尝试安装...${NC}"
    curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.3/install.sh | bash
    if [ $? -ne 0 ]; then
      echo -e "${RED}nvm 安装失败，请手动安装 nvm${NC}"
      exit 1
    fi
    echo -e "${GREEN}nvm 安装成功，请重新运行脚本${NC}"
    exit 0
  fi
}

# 检查并切换 Node.js 版本
check_node_version() {
  # 从 package.json 中读取 Node.js 版本
  REQUIRED_NODE_VERSION=$(node -pe "require('./package.json').engines.node" | sed 's/[^0-9.]//g')
  NODE_VERSION=$(node -v | cut -d'v' -f2)
  IFS='.' read -r -a VERSION_ARRAY <<< "$NODE_VERSION"
  MAJOR_VERSION=${VERSION_ARRAY[0]}
  
  if [[ "$NODE_VERSION" != "$REQUIRED_NODE_VERSION" ]]; then
    check_nvm
    # 检查 nvm 中是否存在指定版本的 Node.js
    if ! nvm ls "$REQUIRED_NODE_VERSION" &> /dev/null; then
      echo -e "${YELLOW}未找到 Node.js $REQUIRED_NODE_VERSION 版本，正在安装...${NC}"
      nvm install "$REQUIRED_NODE_VERSION"
      if [ $? -ne 0 ]; then
        echo -e "${RED}Node.js $REQUIRED_NODE_VERSION 安装失败，请检查网络连接${NC}"
        exit 1
      fi
    fi
    nvm use "$REQUIRED_NODE_VERSION"
  fi
}

# 检查并安装 Yarn
check_yarn() {
  if ! command -v yarn &> /dev/null; then
    echo -e "${YELLOW}未检测到 Yarn，正在尝试安装...${NC}"
    npm install -g yarn
    if [ $? -ne 0 ]; then
      echo -e "${RED}Yarn 安装失败，请手动安装 Yarn${NC}"
      exit 1
    fi
  fi
}

# 安装项目依赖
install_dependencies() {
  echo -e "${GREEN}正在配置 Yarn 镜像源...${NC}"
  yarn config set registry https://registry.npmmirror.com
  
  #echo -e "${GREEN}正在清理并安装依赖...${NC}"
  #rm -rf ./yarn.lock node_modules && yarn cache clean
  yarn install --frozen-lockfile --verbose
  if [ $? -ne 0 ]; then
    echo -e "${RED}依赖安装失败，请检查网络或日志${NC}"
    exit 1
  fi
}

# 检测端口是否被占用
check_port() {
  local port=$(cat ${CURRENT_DIR}/vue.config.js |grep "port:"|awk -F': ' '{print $2}')
  if lsof -i :$port > /dev/null 2>&1; then
    echo -e "${RED}错误：端口 $port 已被占用，请检查并释放该端口${NC}"
    netstat -anp|grep $port
    exit 1
  fi
}

# 启动开发服务器
start_dev_server() {
  echo -e "${GREEN}正在启动开发服务器...${NC}"
  export NODE_OPTIONS=--openssl-legacy-provider
  yarn run serve
  if [ $? -ne 0 ]; then
    echo -e "${RED}开发服务器启动失败，请检查日志${NC}"
    exit 1
  fi
}

# 主函数
main() {
  check_node
  check_node_version
  check_yarn
  
  if [ ! -f "package.json" ]; then
    echo -e "${RED}错误：当前目录不是有效的项目根目录，请确保包含 package.json 文件${NC}"
    exit 1
  fi

  install_dependencies

  # 检查端口是否被占用
  check_port

  start_dev_server
  
  echo -e "${GREEN}开发环境已成功启动！${NC}"
}

main