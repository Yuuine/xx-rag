#!/bin/bash

# -------------------------------
# 潇潇 RAG 一键部署脚本 (Linux)
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
# 全局变量，保存 Compose 命令
# -------------------------------
COMPOSE_CMD=""

# -------------------------------
# 检查命令是否存在
# -------------------------------
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
# 加载 .env 文件
# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
if [ -f ".env" ]; then
    while IFS='=' read -r key value; do
        if [[ ! $key =~ ^# ]] && [[ -n $key ]]; then
            export "$key=$value"
        fi
    done < .env
    info "已加载 .env 配置"
fi

# 检查必需的环境变量
check_required_env_vars() {
    info "检查必需的环境变量..."

    # 检查 EMBEDDING_API_KEY
    if [ -z "$EMBEDDING_API_KEY" ]; then
        warn "警告: EMBEDDING_API_KEY 环境变量未设置!"
        echo -n "请输入 DashScope Embedding API Key (或按回车跳过): "
        read -r input_key
        if [ -n "$input_key" ]; then
            export EMBEDDING_API_KEY="$input_key"
        else
            error "错误: 必须提供 EMBEDDING_API_KEY"
            exit 1
        fi
    fi

    # 检查 DEEPSEEK_API_KEY
    if [ -z "$DEEPSEEK_API_KEY" ]; then
        warn "警告: DEEPSEEK_API_KEY 环境变量未设置!"
        echo -n "请输入 DeepSeek API Key (或按回车跳过): "
        read -r input_key
        if [ -n "$input_key" ]; then
            export DEEPSEEK_API_KEY="$input_key"
        else
            warn "警告: DEEPSEEK_API_KEY 未设置，推理功能可能无法正常工作"
        fi
    fi

    success "环境变量检查完成"
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

    # 检测 docker-compose 或 docker compose
    if command_exists docker-compose; then
        COMPOSE_CMD="docker-compose"
    elif docker compose version >/dev/null 2>&1; then
        COMPOSE_CMD="docker compose"
    else
        error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi

    success "依赖检查通过，使用 Compose 命令: $COMPOSE_CMD"
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
    info "服务状态："
    $COMPOSE_CMD ps
}

# -------------------------------
# 停止服务
# -------------------------------
stop_services() {
    info "停止服务..."
    $COMPOSE_CMD down
    success "服务已停止"
}

# -------------------------------
# 清理部署（容器+镜像+卷）
# -------------------------------
clean_services() {
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
    info "查看日志: $COMPOSE_CMD logs -f app"
    info "停止服务: $COMPOSE_CMD down"
    info "重启服务: $COMPOSE_CMD restart"
    success "======================================="
}

# -------------------------------
# 主函数
# -------------------------------
case "${1:-deploy}" in
    deploy)
        check_required_env_vars
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
