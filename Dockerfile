FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# 复制 pom 并下载依赖
COPY pom.xml .
RUN /usr/share/maven/bin/mvn dependency:go-offline -B

# 复制源码并打包
COPY src ./src
RUN /usr/share/maven/bin/mvn clean package -DskipTests -B

# 运行阶段
FROM openjdk:17-jdk-slim

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apt-get update && apt-get install -y --no-install-recommends \
    libglib2.0-0 libsm6 libxext6 libxrender-dev libgomp1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
RUN mkdir -p /app/logs

COPY --from=build /app/target/xx-rag-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-Xms256m", "-Xmx512g", "-jar", "app.jar"]