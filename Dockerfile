# 使用官方OpenJDK 17作为基础镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制jar文件到容器中
COPY target/xx-rag-0.0.1-SNAPSHOT.jar app.jar

# 创建日志目录
RUN mkdir -p ./logs

# 暴露应用端口
EXPOSE 8081

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]