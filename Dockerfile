# ==========================================================
# 聊于云端 Chatroom - 服务端 Docker 镜像
# 多阶段构建：编译 → 运行
# 默认以 --headless 模式启动，适用于无 GUI 的服务器部署
# ==========================================================

# ---- 阶段 1：编译 ----
FROM maven:3.8.4-openjdk-8-slim AS builder

WORKDIR /build

# 1. 先复制 POM 文件，利用 Docker 缓存加速依赖下载
COPY pom.xml .
COPY chatroom-common/pom.xml chatroom-common/
COPY chatroom-client/pom.xml chatroom-client/
COPY chatroom-server/pom.xml chatroom-server/

# 2. 下载依赖（此层可缓存，除非 POM 变化）
RUN mvn dependency:go-offline -q || true

# 3. 复制全部源码
COPY . .

# 4. 编译打包（跳过测试）
RUN mvn clean package -DskipTests -q

# ---- 阶段 2：运行 ----
FROM openjdk:8-jre-slim

WORKDIR /app

# 从构建阶段复制产物
COPY --from=builder /build/chatroom-server/target/chatroom-server-*.jar server.jar
COPY --from=builder /build/chatroom-common/target/chatroom-common-*.jar common.jar
COPY --from=builder /build/chat.properties .

# 服务端端口
EXPOSE 8888

# 默认以无头模式启动（可在 docker run 时覆盖 CMD）
ENTRYPOINT ["java", "-cp", "server.jar:common.jar", \
            "chatroom.server.ui.ServerChatMain", "--headless"]
