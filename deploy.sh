#!/bin/bash

# Generator部署脚本
set -e

echo "=========================================="
echo "Generator Docker部署脚本"
echo "=========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $*"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $*"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $*"
}

# 检查Docker是否安装
check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker未安装，请先安装Docker"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose未安装，请先安装Docker Compose"
        exit 1
    fi
    
    log_info "Docker和Docker Compose已安装"
}

# 构建镜像
build_image() {
    log_info "开始构建Docker镜像..."
    docker-compose build --no-cache
    log_info "Docker镜像构建完成"
}

# 停止现有容器
stop_containers() {
    log_info "停止现有容器..."
    docker-compose down || true
}

# 启动容器
start_containers() {
    log_info "启动容器..."
    docker-compose up -d
    
    # 等待服务启动
    log_info "等待服务启动..."
    sleep 30
    
    # 检查健康状态
    if docker-compose ps | grep -q "Up"; then
        log_info "服务启动成功！"
        log_info "访问地址: http://localhost:8080"
    else
        log_error "服务启动失败，请检查日志"
        docker-compose logs
        exit 1
    fi
}

# 显示服务状态
show_status() {
    log_info "服务状态:"
    docker-compose ps
}

# 显示日志
show_logs() {
    log_info "服务日志:"
    docker-compose logs -f generator
}

# 清理资源
cleanup() {
    log_info "清理Docker资源..."
    docker-compose down -v
    docker image prune -f
    log_warn "资源清理完成"
}

# 主函数
main() {
    case "${1:-help}" in
        build)
            check_docker
            build_image
            ;;
        up)
            check_docker
            stop_containers
            start_containers
            show_status
            ;;
        down)
            stop_containers
            ;;
        restart)
            check_docker
            stop_containers
            start_containers
            show_status
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs
            ;;
        cleanup)
            cleanup
            ;;
        help|*)
            echo "用法: $0 {build|up|down|restart|status|logs|cleanup|help}"
            echo ""
            echo "命令说明:"
            echo "  build    - 构建Docker镜像"
            echo "  up       - 启动服务"
            echo "  down     - 停止服务"
            echo "  restart  - 重启服务"
            echo "  status   - 查看服务状态"
            echo "  logs     - 查看服务日志"
            echo "  cleanup  - 清理Docker资源"
            echo "  help     - 显示帮助信息"
            ;;
    esac
}

# 执行主函数
main "$@"