# 多阶段构建 - 构建阶段
FROM maven:3.8.6-openjdk-17 AS builder

# 设置工作目录
WORKDIR /app

# 复制pom.xml和源码
COPY pom.xml .
COPY core/pom.xml ./core/
COPY web/pom.xml ./web/

# 下载依赖
RUN mvn dependency:go-offline

# 复制源码
COPY . .

# 构建应用
RUN mvn clean package -DskipTests

# 多阶段构建 - 运行阶段
FROM openjdk:17-jre-slim

# 设置工作目录
WORKDIR /app

# 安装必要的工具
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 复制构建好的jar文件
COPY --from=builder /app/core/target/generator-core.jar /app/app.jar

# 创建非root用户
RUN groupadd -r generator && useradd -r -g generator generator
RUN chown -R generator:generator /app
USER generator

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "/app/app.jar"]