# ☁️ 聊于云端 Chatroom

> **一个基于 Java Swing 的局域网即时聊天室** — 支持多客户端广播通信、用户认证、私聊、SQLite 持久化存储。

![Java](https://img.shields.io/badge/Java-8%2B-orange?logo=openjdk)
![Maven](https://img.shields.io/badge/Maven-3.6%2B-blue?logo=apachemaven)
![SQLite](https://img.shields.io/badge/Database-SQLite-green?logo=sqlite)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## 📋 目录

- [功能特性](#-功能特性)
- [架构概览](#-架构概览)
- [快速开始](#-快速开始)
- [使用指南](#-使用指南)
- [数据库](#-数据库)
- [项目结构](#-项目结构)
- [技术栈](#-技术栈)
- [常见问题](#-常见问题)
- [贡献指南](#-贡献指南)
- [License](#-license)

---

## ✨ 功能特性

### 核心功能
| 功能 | 说明 |
|------|------|
| 🖥️ **多客户端同时在线** | 基于 TCP Socket + 多线程架构，支持数十客户端并发连接 |
| 📢 **实时消息广播** | 消息自动推送到所有在线客户端，延迟 < 100ms |
| 👤 **用户认证系统** | 注册/登录功能，密码经 SHA-256 加密存储 |
| 💬 **私聊** | 输入 `@用户名 消息` 即可发送私密消息，仅目标用户可见 |
| 👥 **在线用户列表** | 客户端与服务端均实时显示在线用户，连接/断开自动更新 |

### 高级特性
| 功能 | 说明 |
|------|------|
| 💓 **心跳保活** | 客户端每 30s 发送心跳，服务端 90s 超时自动清理死连接 |
| 📝 **SQLite 持久化** | 用户数据、聊天记录全部存入 SQLite 数据库，重启不丢失 |
| 🌐 **UTF-8 全链路** | 从 Socket 到数据库全链路 UTF-8 编码，中文无乱码 |
| 🔌 **服务端无头模式** | `--headless` 参数支持无 GUI 运行，可部署于 Linux 服务器 |
| 🐳 **Docker 支持** | 提供 Dockerfile，一键构建容器化部署 |
| 📋 **消息日志** | 所有聊天记录自动保存至 SQLite `messages` 表，可追溯历史 |

---

## 🏗️ 架构概览

```
┌────────────────────────────────────────────────────────────┐
│                      客户端 (Swing)                         │
│  ┌──────────┐   ┌──────────────┐   ┌──────────────────┐   │
│  │ Client   │ → │  Client      │ → │  ClientNetwork   │   │
│  │ ChatMain │   │  Service     │   │  Manager         │   │
│  │  (UI)    │   │  (业务逻辑)   │   │  (Socket/心跳)   │   │
│  └──────────┘   └──────────────┘   └────────┬─────────┘   │
│                                              │              │
└──────────────────────────────────────────────┼──────────────┘
                                               │ TCP
┌──────────────────────────────────────────────┼──────────────┐
│                      服务端                    │              │
│  ┌────────────────────────────────────────────┴──────────┐  │
│  │              ServerNetworkManager                     │  │
│  │  ┌──────────────┐   ┌──────────────┐  ┌────────────┐ │  │
│  │  │ accept 循环  │   │ 广播路由      │  │ 心跳扫描    │ │  │
│  │  └──────┬───────┘   └──────┬───────┘  └─────┬──────┘ │  │
│  └─────────┼──────────────────┼────────────────┼─────────┘  │
│            ▼                  ▼                ▼            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              ServerService                           │  │
│  │  ┌──────────────┐  ┌──────────────────────────────┐  │  │
│  │  │  认证/授权    │  │  消息入库 · 广播 · 日志      │  │  │
│  │  └──────┬───────┘  └───────────┬──────────────────┘  │  │
│  └─────────┼──────────────────────┼──────────────────────┘  │
│            ▼                      ▼                         │
│  ┌──────────────────┐  ┌──────────────────────┐            │
│  │  SQLite users    │  │  SQLite messages     │            │
│  └──────────────────┘  └──────────────────────┘            │
└─────────────────────────────────────────────────────────────┘
```

---

## 🚀 快速开始

### 环境要求

| 依赖 | 版本 | 备注 |
|------|------|------|
| JDK | 8+ | 必需 |
| Maven | 3.6+ | 可选，可直接用 `javac` |
| 操作系统 | Windows / Linux / macOS | 服务端 `--headless` 可运行于无 GUI 环境 |

### 下载依赖

项目使用 **SQLite** 作为数据库，需先下载 JDBC 驱动：

```bash
cd lib/
# SQLite JDBC
curl -L -o sqlite-jdbc.jar https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar
# SLF4J（SQLite 依赖）
curl -L -o slf4j-api.jar https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar
curl -L -o slf4j-nop.jar https://repo1.maven.org/maven2/org/slf4j/slf4j-nop/2.0.9/slf4j-nop-2.0.9.jar
```

### 使用 Maven 构建

```bash
# 1. 编译打包
mvn clean package -DskipTests

# 2. 运行服务端（GUI 模式）
java -jar chatroom-server/target/chatroom-server-1.0.0.jar

# 3. 运行服务端（无头模式，适用于服务器）
java -jar chatroom-server/target/chatroom-server-1.0.0.jar --headless --port=8888

# 4. 运行客户端
java -jar chatroom-client/target/chatroom-client-1.0.0.jar
```

### 直接使用 javac（免 Maven）

```powershell
# Windows PowerShell 一键编译
$lib = "lib\sqlite-jdbc.jar;lib\slf4j-api.jar"
$res = "chatroom-common\src\main\resources"
javac -encoding UTF-8 -cp "$lib;$res" -d out `
  chatroom-common/src/main/java/chatroom/common/*.java `
  chatroom-common/src/main/java/chatroom/common/protocol/*.java `
  chatroom-common/src/main/java/chatroom/common/db/*.java `
  chatroom-client/src/main/java/chatroom/client/ui/*.java `
  chatroom-client/src/main/java/chatroom/client/service/*.java `
  chatroom-client/src/main/java/chatroom/client/network/*.java `
  chatroom-server/src/main/java/chatroom/server/ui/*.java `
  chatroom-server/src/main/java/chatroom/server/service/*.java `
  chatroom-server/src/main/java/chatroom/server/network/*.java

# 运行服务端
java -cp "lib\sqlite-jdbc.jar;lib\slf4j-api.jar;lib\slf4j-nop.jar;out;$res" chatroom.server.ui.ServerChatMain --headless

# 运行客户端（新开终端）
java -cp "lib\sqlite-jdbc.jar;lib\slf4j-api.jar;lib\slf4j-nop.jar;out;$res" chatroom.client.ui.ClientChatMain
```

### Docker 部署

```bash
# 构建镜像
docker build -t chatroom-server:latest .

# 运行容器
docker run -d -p 8888:8888 --name chatroom chatroom-server:latest

# 查看日志
docker logs -f chatroom
```

---

## 📖 使用指南

### 第一步：启动服务端

```bash
# Windows 有窗口模式
java -jar chatroom-server-1.0.0.jar

# 无窗口模式（推荐服务器）
java -jar chatroom-server-1.0.0.jar --headless
```

服务端窗口包含：
- **左侧**：消息日志区，显示所有聊天记录和系统通知
- **右侧**：在线客户端列表，实时显示已连接用户
- **底部**：输入框，服务端可广播消息

### 第二步：启动客户端

```bash
java -jar chatroom-client-1.0.0.jar
```

会弹出登录对话框：
- **新用户**：填写用户名 + 密码 + 昵称，点击「注册」
- **老用户**：填写用户名 + 密码，点击「登录」

### 第三步：开始聊天

| 操作 | 说明 |
|------|------|
| 输入文字 → Enter | 发送公共消息，所有在线用户可见 |
| `@小明 你好` → Enter | 发送私聊消息，仅「小明」可见 |
| 窗口右侧列表 | 显示所有在线用户 |

### 配置文件

`chat.properties`：

```properties
serverPort=8888      # 服务端监听端口
clientIp=127.0.0.1   # 客户端连接的服务端 IP（局域网用实际 IP）
clientPort=8888      # 客户端连接端口（必须与 serverPort 一致）
```

---

## 🗄️ 数据库

项目使用 **SQLite** 嵌入式数据库，零配置，自动管理。

### 表结构

```sql
-- 用户表
CREATE TABLE users (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  username    VARCHAR(50) NOT NULL UNIQUE,  -- 登录名
  password    VARCHAR(64) NOT NULL,          -- SHA-256 哈希
  nickname    VARCHAR(50) NOT NULL,          -- 显示昵称
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 消息表
CREATE TABLE messages (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  sender      VARCHAR(50) NOT NULL,          -- 发送者
  content     TEXT NOT NULL,                 -- 消息内容
  msg_type    VARCHAR(20) DEFAULT 'MESSAGE', -- MESSAGE / WHISPER
  target      VARCHAR(50) DEFAULT '',        -- 私聊目标
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 数据文件

数据库文件自动生成在 `data/chatroom.db`，服务端启动时自动建表，无需手动干预。

---

## 📁 项目结构

```
Chatroom/                          # 父 POM
├── pom.xml                        # 多模块 Maven 配置
├── chat.properties                # 全局配置文件
├── Dockerfile                     # 容器化构建
├── lib/                           # 第三方依赖 (SQLite JDBC, SLF4J)
├── .gitignore
├── README.md
│
├── chatroom-common/               # 🔧 共享层
│   └── src/main/java/chatroom/common/
│       ├── Config.java            # 配置管理（单例）
│       ├── ErrorCode.java         # 统一错误码
│       ├── MessageCallback.java   # 回调接口
│       ├── db/                    # 数据库层
│       │   ├── DbManager.java     # SQLite 连接管理
│       │   ├── UserRepository.java# 用户 DAO
│       │   ├── MessageRepository.java # 消息 DAO
│       │   └── PasswordUtils.java # SHA-256 加密
│       └── protocol/              # 协议层
│           ├── Command.java       # 命令枚举
│           └── ChatMessage.java   # 消息模型
│
├── chatroom-client/               # 💻 客户端
│   └── src/main/java/chatroom/client/
│       ├── ui/                    # 展示层
│       │   ├── ClientChatMain.java# 主窗口
│       │   └── LoginDialog.java   # 登录对话框
│       ├── service/               # 业务层
│       │   └── ClientService.java # 客户端业务逻辑
│       └── network/               # 网络层
│           ├── ClientNetworkManager.java # 连接/收发
│           └── ServerMessageReader.java  # 消息读取
│
└── chatroom-server/               # 🖥️ 服务端
    └── src/main/java/chatroom/server/
        ├── ui/                    # 展示层
        │   └── ServerChatMain.java# 主窗口
        ├── service/               # 业务层
        │   └── ServerService.java # 服务端业务逻辑
        └── network/               # 网络层
            ├── ServerNetworkManager.java  # accept/广播
            ├── ServerListener.java        # 事件监听接口
            ├── ClientHandlerListener.java # 客户端事件接口
            └── ClientHandler.java         # 单客户端处理器
```

---

## 🛠️ 技术栈

| 层次 | 技术 | 用途 |
|------|------|------|
| **语言** | Java 8+ | 跨平台运行 |
| **UI** | Swing (JFrame) | 客户端/服务端图形界面 |
| **网络** | TCP Socket (`java.net`) | 客户端-服务端通信 |
| **并发** | `Thread` + `CopyOnWriteArrayList` | 多客户端并发处理 |
| **构建** | Maven 多模块 | 模块化管理 |
| **数据库** | SQLite via JDBC | 用户认证 + 消息持久化 |
| **容器化** | Docker 多阶段构建 | 服务端一键部署 |
| **日志** | `java.util.logging` | 运行时日志 |

---

## ❓ 常见问题

**Q: 客户端无法连接服务端？**
A: 检查 `chat.properties` 中的 `clientIp` 是否指向服务端 IP，防火墙是否放行 8888 端口。

**Q: 数据库文件在哪？**
A: 服务端启动后自动创建 `data/chatroom.db`，可随时备份。

**Q: 如何查看聊天历史？**
A: 用 SQLite 客户端（如 DB Browser）打开 `data/chatroom.db`，查看 `messages` 表。

**Q: 服务端报 `Address already in use`？**
A: 端口被占用，用 `netstat -ano | findstr 8888` 查找进程并关闭，或修改 `chat.properties` 端口。

**Q: 客户端乱码？**
A: 编译时需指定 `-encoding UTF-8`，两端字符集保持一致。

---

## 🤝 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: add amazing feature'`)
4. 推送分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

请遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范。

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
