# Makefile for xx-rag Docker deployment

.PHONY: build docker-build deploy stop clean logs

# 默认目标
help:
	@echo "可用命令:"
	@echo "  make build        - 构建项目JAR包"
	@echo "  make docker-build - 构建Docker镜像"
	@echo "  make deploy       - 部署应用到Docker"
	@echo "  make stop         - 停止应用"
	@echo "  make clean        - 清理构建文件和停止服务"
	@echo "  make logs         - 查看应用日志"
	@echo "  make status       - 查看服务状态"

# 构建项目
build:
	mvn clean package -DskipTests

# 构建Docker镜像
docker-build: build
	docker build -t xx-rag:latest .

# 部署应用
deploy: docker-build
	docker-compose up -d

# 停止应用
stop:
	docker-compose down

# 清理
clean:
	mvn clean
	docker-compose down -v
	docker rmi xx-rag:latest 2>/dev/null || true

# 查看日志
logs:
	docker-compose logs -f app

# 查看状态
status:
	docker-compose ps