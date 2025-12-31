#!/bin/bash

# 停止xx-rag应用的脚本
set -e  # 遇到错误时终止脚本执行

echo "正在停止xx-rag应用..."

# 检查Docker Compose是否安装
if ! command -v docker-compose &> /dev/null; then
    echo "错误: Docker Compose未安装"
    exit 1
fi

# 停止并删除容器
echo "正在停止服务..."
docker-compose down

if [ $? -ne 0 ]; then
    echo "服务停止失败"
    exit 1
fi

echo "服务已停止"

# 可选：删除卷（这将删除数据库数据）
read -p "是否删除数据卷? 这将删除MySQL和Elasticsearch的数据 (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "正在删除数据卷..."
    docker-compose down -v
    echo "数据卷已删除"
fi

echo "应用已停止"