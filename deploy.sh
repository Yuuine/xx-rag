#!/bin/bash

# -------------------------------
# 潇潇 RAG 一键部署脚本
# -------------------------------

set -e  # 遇到错误退出

# -------------------------------
# 颜色输出函数
# -------------------------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

info()    { echo -e "${BLUE}[INFO]${NC} $1"; }
success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
error()   { echo -e "${RED}[ERROR]${NC} $1"; }

# -------------------------------
# 检查命令是否存在
# -------------------------------
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# -------------------------------
# 检查依赖
# -------------------------------
check_dependencies() {
    info "检查必要依赖..."
    if ! command_exists docker; then
        error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
    if ! command_exists docker-compose && ! command_exists "docker compose"; then
        error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi
    success "依赖检查通过"
}

# -------------------------------
# 检查端口占用
# -------------------------------
check_ports() {
    info "检查端口占用..."
    for port in 8081 3306 9200; do
        if ss -tuln | grep -q ":$port "; then
            warn "端口 $port 已被占用"
        else
            info "端口 $port 可用"
        fi
    done
}

# -------------------------------
# 选择 Docker Compose 命令
# -------------------------------
get_compose_cmd() {
    if command_exists docker-compose; then
        echo "docker-compose"
    else
        echo "docker compose"
    fi
}

# -------------------------------
# 构建 Spring Boot Docker 镜像
# -------------------------------
build_docker_image() {
    if [ -f "Dockerfile" ]; then
        info "构建 Spring Boot Docker 镜像..."
        docker build -t xx-rag-app:latest .
        success "镜像构建完成"
    else
        error "Dockerfile 不存在"
        exit 1
    fi
}

# -------------------------------
# 启动服务
# -------------------------------
start_services() {
    COMPOSE_CMD=$(get_compose_cmd)
    info "启动 Docker Compose 服务..."
    $COMPOSE_CMD up -d --build
    success "服务已启动"
}

# -------------------------------
# 等待服务健康
# -------------------------------
wait_for_services() {
    info "等待 MySQL 启动..."
    timeout 120 bash -c "until docker exec my-mysql mysqladmin ping -uroot -p123456 --silent; do sleep 5; done" || {
        error "MySQL 启动超时"
        exit 1
    }

    info "等待 Elasticsearch 启动..."
    timeout 120 bash -c "until curl -s http://localhost:9200 >/dev/null; do sleep 5; done" || {
        error "Elasticsearch 启动超时"
        exit 1
    }

    info "等待 Spring Boot 应用启动..."
    timeout 180 bash -c "until curl -s http://localhost:8081/actuator/health >/dev/null; do sleep 5; done" || {
        warn "应用可能启动较慢，请稍等"
    }
}

# -------------------------------
# 显示状态
# -------------------------------
show_status() {
    COMPOSE_CMD=$(get_compose_cmd)
    info "服务状态："
    $COMPOSE_CMD ps
}

# -------------------------------
# 停止服务
# -------------------------------
stop_services() {
    COMPOSE_CMD=$(get_compose_cmd)
    info "停止服务..."
    $COMPOSE_CMD down
    success "服务已停止"
}

# -------------------------------
# 清理部署（容器+镜像+卷）
# -------------------------------
clean_services() {
    COMPOSE_CMD=$(get_compose_cmd)
    info "清理部署..."
    $COMPOSE_CMD down -v
    docker rmi xx-rag-app:latest 2>/dev/null || true
    success "部署已清理"
}

# -------------------------------
# 显示访问信息
# -------------------------------
show_info() {
    success "======================================="
    success "潇潇 RAG 部署完成！"
    info "Spring Boot 应用: http://localhost:8081"
    info "MySQL: 3306 (root/123456)"
    info "Elasticsearch: 9200"
    info "查看日志: ${COMPOSE_CMD} logs -f app"
    info "停止服务: ${COMPOSE_CMD} down"
    info "重启服务: ${COMPOSE_CMD} restart"
    success "======================================="
}

# -------------------------------
# 主函数
# -------------------------------
case "${1:-deploy}" in
    deploy)
        check_dependencies
        check_ports
        build_docker_image
        start_services
        wait_for_services
        show_status
        show_info
        ;;
    stop)
        stop_services
        ;;
    status)
        show_status
        ;;
    logs)
        COMPOSE_CMD=$(get_compose_cmd)
        $COMPOSE_CMD logs -f app
        ;;
    clean)
        clean_services
        ;;
    *)
        error "用法: $0 {deploy|stop|status|logs|clean}"
        exit 1
        ;;
esac
