#!/bin/bash

# 构建Docker镜像的脚本
echo "开始构建xx-rag Docker镜像..."

# 首先使用Maven打包项目
echo "正在使用Maven打包项目..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Maven打包失败，请检查错误信息"
    exit 1
fi

echo "Maven打包完成"

# 构建Docker镜像
echo "正在构建Docker镜像..."
docker build -t xx-rag:latest .

if [ $? -ne 0 ]; then
    echo "Docker镜像构建失败"
    exit 1
fi

echo "Docker镜像构建完成"

# 显示构建的镜像
docker images xx-rag:latest

echo "构建完成！可以使用 'docker-compose up -d' 启动服务"