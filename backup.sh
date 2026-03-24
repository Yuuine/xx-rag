#!/bin/bash

# -------------------------------
# 潇潇 RAG 数据备份与恢复脚本
# -------------------------------

set -e

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
# 配置
# -------------------------------
BACKUP_DIR="${BACKUP_DIR:-./backups}"
COMPOSE_CMD=""

# -------------------------------
# 检测 Docker Compose 命令
# -------------------------------
detect_compose() {
    if command -v docker-compose >/dev/null 2>&1; then
        COMPOSE_CMD="docker-compose"
    elif docker compose version >/dev/null 2>&1; then
        COMPOSE_CMD="docker compose"
    else
        error "Docker Compose 未安装"
        exit 1
    fi
}

# -------------------------------
# 创建备份目录
# -------------------------------
init_backup_dir() {
    local date_str=$(date +%Y%m%d_%H%M%S)
    BACKUP_PATH="$BACKUP_DIR/backup_$date_str"
    mkdir -p "$BACKUP_PATH"
    info "备份目录: $BACKUP_PATH"
}

# -------------------------------
# 备份 MySQL 数据
# -------------------------------
backup_mysql() {
    info "备份 MySQL 数据..."

    local mysql_container="my-mysql"
    local mysql_user="root"
    local mysql_password="${MYSQL_ROOT_PASSWORD:-123456}"
    local mysql_database="rag"

    # 检查容器是否运行
    if ! docker ps | grep -q "$mysql_container"; then
        error "MySQL 容器未运行"
        return 1
    fi

    # 执行备份
    docker exec "$mysql_container" mysqldump -u"$mysql_user" -p"$mysql_password" "$mysql_database" > "$BACKUP_PATH/mysql_$mysql_database.sql"

    if [ $? -eq 0 ]; then
        success "MySQL 备份完成: mysql_$mysql_database.sql"
    else
        error "MySQL 备份失败"
        return 1
    fi
}

# -------------------------------
# 备份 Elasticsearch 数据
# -------------------------------
backup_elasticsearch() {
    info "备份 Elasticsearch 数据..."

    local es_container="my-es"
    local es_index="rag_chunks"

    # 检查容器是否运行
    if ! docker ps | grep -q "$es_container"; then
        error "Elasticsearch 容器未运行"
        return 1
    fi

    # 使用 elasticdump 或手动导出
    # 这里使用简单的快照方式
    docker exec "$es_container" curl -s "localhost:9200/$es_index/_search?size=10000" > "$BACKUP_PATH/elasticsearch_$es_index.json"

    if [ $? -eq 0 ]; then
        success "Elasticsearch 备份完成: elasticsearch_$es_index.json"
    else
        warn "Elasticsearch 备份可能不完整，请检查"
    fi
}

# -------------------------------
# 备份 Docker 卷
# -------------------------------
backup_volumes() {
    info "备份 Docker 卷..."

    local volumes=("mysql_data" "es_data" "rabbitmq_data")

    for volume in "${volumes[@]}"; do
        local volume_backup="$BACKUP_PATH/volume_$volume.tar"
        docker run --rm -v "${volume}:/data" -v "$BACKUP_PATH:/backup" busybox tar cvf "/backup/volume_$volume.tar" /data
        if [ $? -eq 0 ]; then
            success "卷备份完成: volume_$volume.tar"
        else
            warn "卷备份失败: $volume"
        fi
    done
}

# -------------------------------
# 创建备份信息文件
# -------------------------------
create_backup_info() {
    local info_file="$BACKUP_PATH/backup_info.txt"
    cat > "$info_file" << EOF
潇潇 RAG 备份信息
==================
备份时间: $(date '+%Y-%m-%d %H:%M:%S')
备份路径: $BACKUP_PATH

包含内容:
- MySQL 数据库导出
- Elasticsearch 索引数据
- Docker 卷备份

恢复说明:
1. MySQL: docker exec -i my-mysql mysql -uroot -p123456 rag < mysql_rag.sql
2. ES: 使用 init-es-index.sh 重建索引后导入数据
3. 卷: docker run --rm -v volume_name:/data -v \$(pwd):/backup busybox tar xvf /backup/volume_xxx.tar -C /
EOF
    success "备份信息已保存: backup_info.txt"
}

# -------------------------------
# 压缩备份
# -------------------------------
compress_backup() {
    info "压缩备份文件..."
    local tar_file="${BACKUP_PATH}.tar.gz"
    tar -czf "$tar_file" -C "$BACKUP_DIR" "$(basename $BACKUP_PATH)"
    rm -rf "$BACKUP_PATH"
    success "备份已压缩: $(basename $tar_file)"
    info "备份文件大小: $(du -h "$tar_file" | cut -f1)"
}

# -------------------------------
# 列出备份
# -------------------------------
list_backups() {
    info "可用的备份文件:"
    if [ -d "$BACKUP_DIR" ]; then
        ls -lh "$BACKUP_DIR"/*.tar.gz 2>/dev/null || info "暂无备份文件"
    else
        info "备份目录不存在"
    fi
}

# -------------------------------
# 恢复 MySQL
# -------------------------------
restore_mysql() {
    local backup_file="$1"
    info "恢复 MySQL 数据..."

    if [ ! -f "$backup_file" ]; then
        error "备份文件不存在: $backup_file"
        return 1
    fi

    local mysql_container="my-mysql"
    local mysql_user="root"
    local mysql_password="${MYSQL_ROOT_PASSWORD:-123456}"
    local mysql_database="rag"

    # 检查容器是否运行
    if ! docker ps | grep -q "$mysql_container"; then
        error "MySQL 容器未运行"
        return 1
    fi

    warn "即将恢复 MySQL 数据，当前数据将被覆盖！"
    read -p "确认继续? (y/N): " confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        info "已取消恢复"
        return 0
    fi

    # 执行恢复
    docker exec -i "$mysql_container" mysql -u"$mysql_user" -p"$mysql_password" "$mysql_database" < "$backup_file"

    if [ $? -eq 0 ]; then
        success "MySQL 恢复完成"
    else
        error "MySQL 恢复失败"
        return 1
    fi
}

# -------------------------------
# 清理旧备份
# -------------------------------
cleanup_backups() {
    local keep_days="${1:-7}"
    info "清理 ${keep_days} 天前的备份..."

    if [ -d "$BACKUP_DIR" ]; then
        find "$BACKUP_DIR" -name "backup_*.tar.gz" -mtime +$keep_days -delete
        success "旧备份清理完成"
    fi
}

# -------------------------------
# 显示帮助
# -------------------------------
show_help() {
    cat << EOF
潇潇 RAG 数据备份与恢复脚本

用法: $0 {backup|restore|list|cleanup|help}

命令:
    backup              创建完整备份
    restore <文件>      从备份文件恢复 MySQL
    list                列出所有备份
    cleanup [天数]      清理指定天数前的备份 (默认7天)
    help                显示帮助信息

环境变量:
    BACKUP_DIR          备份目录 (默认: ./backups)
    MYSQL_ROOT_PASSWORD MySQL root 密码 (默认: 123456)

示例:
    $0 backup                           # 创建备份
    $0 list                             # 列出备份
    $0 restore ./backups/backup_xxx.tar.gz  # 恢复数据
    $0 cleanup 30                       # 清理30天前的备份
EOF
}

# -------------------------------
# 主函数
# -------------------------------
case "${1:-backup}" in
    backup)
        detect_compose
        init_backup_dir
        backup_mysql
        backup_elasticsearch
        backup_volumes
        create_backup_info
        compress_backup
        success "备份完成！"
        ;;
    restore)
        if [ -z "$2" ]; then
            error "请指定备份文件路径"
            show_help
            exit 1
        fi
        restore_mysql "$2"
        ;;
    list)
        list_backups
        ;;
    cleanup)
        cleanup_backups "${2:-7}"
        ;;
    help)
        show_help
        ;;
    *)
        error "未知命令: $1"
        show_help
        exit 1
        ;;
esac
