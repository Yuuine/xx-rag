#!/bin/bash

# --------------------------------
# 潇潇 RAG Docker 热更新脚本
# --------------------------------

set -e  # 遇到错误退出

# -------------------------------
# 可配置变量
# -------------------------------
APP_IMAGE="xx-rag-app"          # 镜像名称
APP_SERVICE="app"               # Compose 中的服务名称
BRANCH="main"                   # 要跟踪的分支
APP_PORT="8081"                 # 应用监听端口
HEALTH_PATH=""                  # 健康检查路径（当前为空，仅检查端口连通性）
BACKUP_TAG="backup"             # 备份标签名

# -------------------------------
# 颜色输出函数
# -------------------------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

info()    { echo -e "${BLUE}[INFO]${NC} $1"; }
success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
error()   { echo -e "${RED}[ERROR]${NC} $1"; }

# -------------------------------
# Docker Compose 命令检测
# -------------------------------
COMPOSE_CMD=""

command -v docker-compose >/dev/null 2>&1 && COMPOSE_CMD="docker-compose"
docker compose version >/dev/null 2>&1 && COMPOSE_CMD="docker compose"

if [ -z "$COMPOSE_CMD" ]; then
    error "Docker Compose 未安装"
    exit 1
fi

info "使用 Compose 命令: $COMPOSE_CMD"

# -------------------------------
# Git 检查更新
# -------------------------------
info "检查 $BRANCH 分支更新..."

git fetch origin $BRANCH

LOCAL=$(git rev-parse HEAD)
REMOTE=$(git rev-parse origin/$BRANCH)

if [ "$LOCAL" == "$REMOTE" ]; then
    success "代码无更新，无需热更新"
    exit 0
fi

info "检测到代码更新，开始热更新"

# -------------------------------
# 检查工作区是否干净
# -------------------------------
if ! git diff --quiet || ! git diff --cached --quiet; then
    warn "检测到本地有未提交的修改（工作区或暂存区）"
    echo -n "是否继续强制重置并丢失这些修改？(y/N): "
    read -r confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        error "用户取消操作，退出"
        exit 1
    fi
fi

# -------------------------------
# 备份旧镜像
# -------------------------------
info "备份当前镜像..."
docker tag ${APP_IMAGE}:latest ${APP_IMAGE}:${BACKUP_TAG} || true

# -------------------------------
# 拉取最新代码
# -------------------------------
info "拉取最新代码并重置..."
git reset --hard origin/$BRANCH

# -------------------------------
# 构建新镜像
# -------------------------------
info "构建新的 ${APP_IMAGE} 镜像..."
docker build -t ${APP_IMAGE}:latest .

success "镜像构建完成"

# -------------------------------
# 重启 app 服务
# -------------------------------
info "重启 ${APP_SERVICE} 服务..."
$COMPOSE_CMD up -d --no-deps --build ${APP_SERVICE}

# -------------------------------
# 端口连通性检查（后续可加 Actuator，目前为了轻量先不添加）
# -------------------------------
info "等待应用端口就绪（最多 180 秒）..."

timeout 180 bash -c "
until curl -s -f http://localhost:${APP_PORT}${HEALTH_PATH} >/dev/null; do
    sleep 5
done
" || {
    error "启动超时或端口未就绪，进行回滚"
    docker tag ${APP_IMAGE}:${BACKUP_TAG} ${APP_IMAGE}:latest
    docker rmi ${APP_IMAGE}:latest || true
    $COMPOSE_CMD up -d --no-deps ${APP_SERVICE}
    error "回滚完成，服务已恢复到上一版本"
    exit 1
}

success "热更新完成，应用端口已就绪"

# -------------------------------
# 成功后清理操作
# -------------------------------
info "清理备份标签和历史镜像..."
docker rmi ${APP_IMAGE}:${BACKUP_TAG} || true                    # 删除备份标签
docker image prune -f --filter "dangling=true" >/dev/null 2>&1   # 清理悬挂镜像

# -------------------------------
# 显示状态
# -------------------------------
$COMPOSE_CMD ps