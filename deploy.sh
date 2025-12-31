#!/bin/bash

# 一键部署xx-rag应用的脚本
set -e  # 遇到错误时终止脚本执行

echo "开始部署xx-rag应用..."

# 检查是否安装了Docker
if ! command -v docker &> /dev/null; then
    echo "错误: Docker未安装，请先安装Docker"
    exit 1
fi

# 检查是否安装了Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo "错误: Docker Compose未安装，请先安装Docker Compose"
    exit 1
fi

# 构建项目
echo "正在构建项目..."
./build-docker.sh

if [ $? -ne 0 ]; then
    echo "项目构建失败，部署终止"
    exit 1
fi

echo "项目构建完成"

# 启动服务
echo "正在启动服务..."
docker-compose up -d

if [ $? -ne 0 ]; then
    echo "服务启动失败"
    exit 1
fi

echo "服务启动完成"

# 等待一段时间让服务启动
echo "等待服务启动..."
sleep 10

# 显示运行中的容器
echo "当前运行的容器:"
docker-compose ps

echo ""
echo "部署完成！"
echo "应用访问地址: http://localhost:8081"
echo "MySQL服务端口: 3306"
echo "Elasticsearch服务端口: 9200"
echo ""
echo "查看应用日志: docker-compose logs -f app"
echo "停止服务: docker-compose down"