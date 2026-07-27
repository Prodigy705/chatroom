# 🚀 项目优化记录

> 优化日期：2026-07-27
> 优化范围：架构、编码规范、功能完整性、可维护性、可部署性

---

## 目录

1. [新增构建系统 — Maven](#1-新增构建系统--maven)
2. [版本管理 — .gitignore](#2-版本管理--gitignore)
3. [配置管理增强 — Config.java](#3-配置管理增强--configjava)
4. [用户昵称系统](#4-用户昵称系统)
5. [UTF-8 编码全链路支持](#5-utf-8-编码全链路支持)
6. [日志系统引入](#6-日志系统引入)
7. [心跳保活机制](#7-心跳保活机制)
8. [输入验证与安全](#8-输入验证与安全)
9. [服务端无头模式](#9-服务端无头模式)
10. [事件处理优化](#10-事件处理优化)
11. [线程模型修复](#11-线程模型修复)
12. [错误处理增强](#12-错误处理增强)
13. [项目文档](#13-项目文档)
14. [项目结构重构 — 职责分离](#14-项目结构重构--职责分离)
15. [包路径扁平化 + 内嵌接口独立](#15-包路径扁平化--内嵌接口独立)
16. [多模块架构重构 — 参考 OpsAny](#16-多模块架构重构--参考-opsany)

---

## 1. 新增构建系统 — Maven

### 变更文件

| 文件 | 操作 |
|------|------|
| `pom.xml` | ✨ 新建 |

### 优化内容

- 使用 Maven 作为构建工具，实现**可重复构建**
- 通过 `maven-jar-plugin` 同时生成两个 JAR：
  - `Chatroom-1.0.0-client.jar` — 客户端入口
  - `Chatroom-1.0.0-server.jar` — 服务端入口
- 将 `chat.properties` 配置为资源文件，JAR 包内自动包含
- 编译级别：Java 8（向下兼容）

### 构建命令

```bash
mvn clean package -DskipTests
java -jar target/Chatroom-1.0.0-client.jar
java -jar target/Chatroom-1.0.0-server.jar
java -jar target/Chatroom-1.0.0-server.jar --headless
```

---

## 2. 版本管理 — .gitignore

### 变更文件

| 文件 | 操作 |
|------|------|
| `.gitignore` | 🔄 重写 |

### 优化内容

- 忽略 `*.class`、`target/`、`out/`、`build/` 等编译产物
- 忽略 IDE 配置（`.idea/`、`*.iml`、`.vscode/`）
- 忽略操作系统文件（`.DS_Store`、`Thumbs.db`）
- 忽略日志文件（`*.log`）和临时文件（`*.tmp`、`*.bak`）

---

## 3. 配置管理增强 — Config.java

### 变更文件

| 文件 | 操作 |
|------|------|
| `src/com/chatroom/common/Config.java` | 🔄 重写 |

### 优化内容

| 问题 | 优化前 | 优化后 |
|------|--------|--------|
| **无默认值** | `get(key)` 返回 `null`，`getInt(key)` 直接 `parseInt` 可能抛异常 | 新增 `get(key, defaultVal)` 和 `getInt(key, defaultVal)` 重载，配置缺失时回退默认值并记录警告 |
| **无日志** | 无任何输出 | 加载成功/失败、配置缺失、格式错误均有日志 |
| **错误信息模糊** | "无法加载配置文件 chat.properties" | 日志区分 classpath 加载和文件系统加载路径 |

### 代码示例

```java
// 优化前 — 配置缺失直接崩溃
private final int serverPort = Config.getInstance().getInt("serverPort");

// 优化后 — 带默认值安全回退
private final int serverPort = Config.getInstance().getInt("serverPort", 8888);
```

---

## 4. 用户昵称系统

### 变更文件

| 文件 | 操作 |
|------|------|
| `src/com/chatroom/client/ClientChatMain.java` | 🔄 重写 |
| `src/com/chatroom/server/ClientHandler.java` | 🔄 重写 |

### 优化内容

#### 客户端 — 登录对话框

- 启动时弹出美观的昵称输入框
- 输入为空时默认 "匿名用户"
- 取消则退出程序
- 窗口标题动态显示昵称：`聊于云端 - 昵称`

#### 服务端 — 昵称注册

- `ClientHandler` 新增 `nickname` 字段
- **首条消息**作为用户昵称（而非 IP:Port）
- 后续消息格式：`[HH:mm:ss] 昵称: 消息内容`
- 新增 `getDisplayName()` 方法，优先返回昵称
- 客户端加入/离开时显示昵称

### 消息流

```
客户端连接 → 发送昵称("小明") → 服务端注册
客户端发送("大家好")  →  服务端显示 "[14:30:00] 小明: 大家好"  →  广播
```

---

## 5. UTF-8 编码全链路支持

### 变更文件

| 文件 | 操作 |
|------|------|
| `src/com/chatroom/client/ClientChatMain.java` | 🔄 重写 |
| `src/com/chatroom/client/ServerMessageReader.java` | 🔄 编辑 |
| `src/com/chatroom/server/ClientHandler.java` | 🔄 重写 |

### 优化内容

| 位置 | 优化前 | 优化后 |
|------|--------|--------|
| `ClientChatMain` 写 | `OutputStreamWriter(socket.getOutputStream())` | `OutputStreamWriter(..., StandardCharsets.UTF_8)` |
| `ServerMessageReader` 读 | `InputStreamReader(socket.getInputStream())` | `InputStreamReader(..., StandardCharsets.UTF_8)` |
| `ClientHandler` 读写 | 同上 | 同上 |

**解决跨平台中文乱码问题**，确保 Linux / macOS / Windows 之间传输中文字符正常。

---

## 6. 日志系统引入

### 变更文件

| 文件 | 操作 |
|------|------|
| `src/com/chatroom/common/Config.java` | 🔄 重写 |
| `src/com/chatroom/client/ClientChatMain.java` | 🔄 重写 |
| `src/com/chatroom/client/ServerMessageReader.java` | 🔄 编辑 |
| `src/com/chatroom/server/ClientHandler.java` | 🔄 重写 |
| `src/com/chatroom/server/ServerChatMain.java` | 🔄 重写 |

### 优化内容

- 使用 **`java.util.logging.Logger`**，零外部依赖
- 每个类独立 Logger 实例
- 日志级别合理使用：
  - `SEVERE` — 启动失败、致命错误
  - `WARNING` — 配置缺失、连接失败
  - `INFO` — 连接/断开、在线人数
  - `FINE` — 正常关闭、心跳调试
  - `FINEST` — 心跳包收发

### 对比

```java
// 优化前 — 异常被完全吞没
catch (IOException ignored) {}

// 优化后 — 异常可追溯
catch (IOException e) {
    LOG.log(Level.FINE, "关闭 socket 异常", e);
}
```

---

## 7. 心跳保活机制

### 变更文件

| 文件 | 操作 |
|------|------|
| `src/com/chatroom/client/ClientChatMain.java` | 🔄 重写 |
| `src/com/chatroom/server/ClientHandler.java` | 🔄 重写 |
| `src/com/chatroom/server/ServerChatMain.java` | 🔄 重写 |

### 优化内容

#### 客户端（发送心跳）

- 独立守护线程 `Client-Heartbeat`
- 每 **30 秒**发送 `HEARTBEAT` 文本行
- 连接断开时自动停止

#### 服务端（检测超时）

- `ClientHandler` 记录 `lastActivityTime`，每次收到消息更新
- 设置 Socket 读超时 `HEARTBEAT_TIMEOUT_MS = 90秒`（3 次心跳容忍）
- `ServerChatMain` 独立线程每 **15 秒**扫描所有客户端
- 超时客户端自动断开并清理

### 效果

- 客户端强行关闭（`kill -9`、拔网线）后，服务端 **≤90 秒** 内检测到断开
- 避免死连接占用服务端资源

---

## 8. 输入验证与安全

### 变更文件

| 文件 | 操作 |
|------|------|
| `src/com/chatroom/client/ClientChatMain.java` | 🔄 重写 |
| `src/com/chatroom/server/ServerChatMain.java` | 🔄 重写 |

### 优化内容

| 防护 | 说明 |
|------|------|
| **消息长度上限** | 客户端和服务端均限制 `MAX_MESSAGE_LENGTH = 1024` 字符 |
| **空白消息过滤** | 发送前 `trim()` 判空 |
| **昵称防空** | 空昵称自动回退为 "匿名用户" |

---

## 9. 服务端无头模式

### 变更文件

| 文件 | 操作 |
|------|------|
| `src/com/chatroom/server/ServerChatMain.java` | 🔄 重写 |

### 优化内容

- 支持命令行参数 `--headless` 以纯控制台模式运行
- 支持 `--port=<number>` 动态指定端口（覆盖配置文件）
- 无头模式下所有输出写入 `System.out`
- 可以部署在无 GUI 的 Linux 服务器上

### 使用方式

```bash
# 有头模式（默认）
java -jar Chatroom-1.0.0-server.jar

# 无头模式
java -jar Chatroom-1.0.0-server.jar --headless

# 无头 + 指定端口
java -jar Chatroom-1.0.0-server.jar --headless --port=9999
```

---

## 10. 事件处理优化

### 变更文件

| 文件 | 操作 |
|------|------|
| `src/com/chatroom/client/ClientChatMain.java` | 🔄 重写 |
| `src/com/chatroom/server/ServerChatMain.java` | 🔄 重写 |

### 优化内容

| 问题 | 优化前 | 优化后 |
|------|--------|--------|
| **重复绑定** | 同时实现 `ActionListener` + `KeyListener` 处理发送 | 使用 `JRootPane.setDefaultButton()` + 单一 `ActionListener` |
| **接口臃肿** | 实现 2 个接口共 5 个方法，3 个空方法 | 仅 1 个 `ActionListener` 方法，简洁清晰 |

### 原理

`JRootPane.setDefaultButton()` 会在输入框按 Enter 时自动触发按钮的 `ActionListener`，无需手动监听键盘事件。

---

## 11. 线程模型修复

### 变更文件

| 文件 | 操作 |
|------|------|
| `src/com/chatroom/client/ClientChatMain.java` | 🔄 重写 |

### 优化内容

| 问题 | 优化前 | 优化后 |
|------|--------|--------|
| **`readerThread.join()` 位置** | 在 `connect()` 方法中 join，但已无副作用 | 保留 join 等待断开后执行 `finally` 清理 |
| **`readerThread` 字段冗余** | 类字段存储 `readerThread` | 改为局部变量 |
| **`InterruptedException` 被吞没** | `catch (InterruptedException ignored)` | `catch (InterruptedException e) { Thread.currentThread().interrupt(); }` |

---

## 12. 错误处理增强

### 变更文件

所有源文件

### 优化内容

| 问题 | 示例 | 优化后 |
|------|------|--------|
| **异常吞没** | `catch (IOException ignored) {}` | 至少 `LOG.log(Level.FINE, ...)` 记录 |
| **无错误反馈** | 连接失败无日志 | `LOG.log(Level.WARNING, "连接失败", e)` |
| **关闭窗口 NPE** | `messageReader.stop()` 时 `messageReader` 可能为 null | 加 `if (messageReader != null)` 判空 |

---

## 13. 项目文档

### 新增文件

| 文件 | 说明 |
|------|------|
| `README.md` | 项目说明、快速开始、构建/运行指南 |
| `OPTIMIZATION_RECORD.md` | 本文，优化全过程记录 |

### README 包含

- 项目简介与功能特性清单
- 环境要求
- Maven 构建命令
- javac 直接编译命令
- 配置文件说明
- 项目结构图
- 技术栈列表

---

## 14. 项目结构重构 — 职责分离

### 变更文件

| 文件 | 操作 | 职责 |
|------|------|------|
| `src/chatroom/client/LoginDialog.java` | ✨ 新建 | **登录对话框**：仅负责昵称输入 UI |
| `src/chatroom/client/ClientNetworkManager.java` | ✨ 新建 | **客户端网络层**：连接、发送、心跳、资源管理 |
| `src/chatroom/client/ClientChatMain.java` | 🔄 重写 | **仅保留 UI**：窗口布局、事件、文本展示 |
| `src/chatroom/server/ServerNetworkManager.java` | ✨ 新建 | **服务端网络层**：accept循环、客户端列表、广播、心跳扫描 |
| `src/chatroom/server/ServerChatMain.java` | 🔄 重写 | **仅保留 UI**：窗口布局、事件、文本展示 |

### 重构动机

**优化前** — 两个主类承担了过多职责：

```
ClientChatMain.java (~270行)
  ├── UI 初始化 + 事件监听
  ├── Socket 连接管理
  ├── 首条消息(昵称)发送
  ├── 消息读取线程管理
  ├── 心跳发送线程
  ├── 消息发送 + 输入校验
  └── 资源清理

ServerChatMain.java (~280行)
  ├── UI 初始化 + 事件监听
  ├── ServerSocket accept 循环
  ├── 客户端列表管理 (CopyOnWriteArrayList)
  ├── 消息广播逻辑
  ├── 心跳扫描线程
  ├── 无头模式逻辑
  └── 资源清理
```

**优化后** — 每个类职责单一：

```
ClientChatMain.java        → 仅 UI：窗口、输入、展示
LoginDialog.java           → 仅登录对话框
ClientNetworkManager.java  → 仅网络：连接、收发、心跳
ServerMessageReader.java   → 仅读取

ServerChatMain.java        → 仅 UI：窗口、输入、展示
ServerNetworkManager.java  → 仅网络：accept、广播、心跳扫描
ServerListener.java        → 服务端事件监听接口 ✨
ClientHandlerListener.java → 客户端事件监听接口 ✨
ClientHandler.java         → 仅单客户端处理
```

### 架构对比

| 维度 | 重构前 | 重构后 |
|------|--------|--------|
| **客户端类数** | 2 个（`ClientChatMain` + `ServerMessageReader`） | 4 个（增加 `LoginDialog` + `ClientNetworkManager`） |
| **服务端类数** | 2 个（`ServerChatMain` + `ClientHandler`） | 3 个（增加 `ServerNetworkManager`） |
| **UI 依赖网络** | 直接持有 Socket/Writer | 通过 `MessageCallback` 回调解耦 |
| **网络依赖 UI** | 间接持有，仍耦合 | 零依赖，纯接口回调 |
| **可测试性** | 低（网络逻辑嵌在 Swing 类中） | 高（`ClientNetworkManager` / `ServerNetworkManager` 可单独测试） |
| **可扩展性** | 修改 UI 可能影响网络逻辑 | 修改 UI 不影响网络层，反之亦然 |

### 关键设计决策

1. **`ServerNetworkManager.ServerListener` 接口** — 网络层通过此接口将事件（消息/人数/状态）传递给 UI 或控制台，网络层完全不依赖 Swing
2. **`ClientNetworkManager` 无 Swing 依赖** — 通过 `MessageCallback` 回调上报数据，UI 层在回调中自行 `SwingUtilities.invokeLater()`
3. **`LoginDialog` 纯静态工具类** — 与主窗口完全解耦，方便将来替换为更复杂的登录界面

---

## 15. 包路径扁平化 + 内嵌接口独立

### 变更文件

| 文件 | 操作 | 说明 |
|------|------|------|
| 所有 `.java` 文件 | 📦 包名变更 | `com.chatroom.xxx` → `chatroom.xxx` |
| `src/chatroom/server/ServerListener.java` | ✨ 新建 | 从 `ServerNetworkManager` 中提取的独立接口 |
| `src/chatroom/server/ClientHandlerListener.java` | ✨ 新建 | 从 `ClientHandler` 中提取的独立接口 |
| `src/com/chatroom/` 目录 | 🗑️ 删除 | 旧包结构已清理 |

### 优化内容

#### 1. 压平包路径

```
旧结构:  src/com/chatroom/{client,server,common}/
新结构:  src/chatroom/{client,server,common}/
包名前缀: com.chatroom.xxx  →  chatroom.xxx
```

对于一个课程作业/小型项目，`com` 前缀除了增加嵌套深度外没有实际意义。压平后：
- 目录层级更浅，文件浏览更直观
- 包名更简洁，输入更少
- 不影响 Java 编译和运行

#### 2. 内嵌接口提取为独立文件

**提取前** — 接口作为内部类型定义，导致导入语法怪异：
```java
// ServerChatMain.java
import com.chatroom.server.ServerNetworkManager.ServerListener;
//       ^^^^^^^^^^^^^^^^       ^^^^^^^^^^^^^^^^^^^^^^
//       两层嵌套              内部接口
```

**提取后** — 每个接口独立成文件，导入清晰：
```java
// ServerChatMain.java
import chatroom.server.ServerListener;
//       ^^^^^^^^       ^^^^^^^^^^^^^^
//       一层           独立接口
```

| 接口 | 原位置 | 现位置 |
|------|--------|--------|
| `ServerListener` | `ServerNetworkManager` 内部 | `chatroom/server/ServerListener.java` |
| `ClientHandlerListener` | `ClientHandler` 内部 (`OnMessageListener`) | `chatroom/server/ClientHandlerListener.java` |

### 最终项目结构

```
src/
├── META-INF/
│   └── MANIFEST.MF
└── chatroom/
    ├── client/
    │   ├── ClientChatMain.java        # [UI]     客户端主窗口
    │   ├── ClientNetworkManager.java  # [网络]   连接/收发/心跳
    │   ├── LoginDialog.java           # [UI]     登录对话框
    │   └── ServerMessageReader.java   # [网络]   消息读取器
    ├── common/
    │   ├── Config.java                # [配置]   单例配置
    │   └── MessageCallback.java       # [接口]   消息回调
    └── server/
        ├── ServerChatMain.java        # [UI]     服务端主窗口
        ├── ServerNetworkManager.java  # [网络]   accept/广播/心跳扫描
        ├── ServerListener.java        # [接口]   服务端事件监听器 ✨
        ├── ClientHandlerListener.java # [接口]   客户端事件监听器 ✨
        └── ClientHandler.java         # [网络]   单客户端处理器
```

---

## 优化前后对比总表

| 维度 | 优化前 | 优化后 |
|------|--------|--------|
| **构建工具** | ❌ 无（依赖 IDE） | ✅ Maven |
| **版本管理** | ⚠️ 基础 .gitignore | ✅ 完整 .gitignore |
| **项目文档** | ❌ 无 | ✅ README.md + OPTIMIZATION_RECORD.md |
| **用户身份** | ❌ 仅 IP:Port | ✅ 昵称系统 |
| **消息编码** | ❌ 平台默认编码 | ✅ UTF-8 全链路 |
| **日志系统** | ❌ 无 | ✅ java.util.logging |
| **心跳保活** | ❌ 无 | ✅ 30s 心跳 + 90s 超时检测 |
| **输入验证** | ❌ 无限制 | ✅ 1024 字符上限 |
| **事件处理** | ⚠️ 重复绑定 | ✅ setDefaultButton |
| **服务端模式** | ❌ 仅 GUI | ✅ GUI + --headless 无头 |
| **异常处理** | ⚠️ 多处 ignored | ✅ 日志记录所有异常 |
| **配置健壮性** | ⚠️ 无默认值 | ✅ 带默认值的 get/getInt |
| **线程安全** | ⚠️ writer 未同步 | ✅ synchronized(writer) 保护 |
| **窗口关闭 NPE** | ❌ 可能 NPE | ✅ 判空保护 |
| **项目架构** | ⚠️ 2 个主类承担过多职责 | ✅ 11 个文件各司其职，UI 与网络完全解耦 |
| **包结构** | ⚠️ `com.chatroom.xxx` 三层嵌套 | ✅ `chatroom.xxx` 二层扁平，整洁直观 |
| **内嵌接口** | ⚠️ 接口嵌在类内部，导入语法怪异 | ✅ 接口独立成文件，`import` 一行直达 |
| **可测试性** | ❌ 网络逻辑嵌在 Swing 类中 | ✅ `ClientNetworkManager` / `ServerNetworkManager` 可脱离 Swing 测试 |
| **可扩展性** | ⚠️ 修改 UI 可能影响网络 | ✅ 接口解耦，UI/网络独立演化 |
| **模块架构** | ❌ 单模块 | ✅ Maven 多模块（common / client / server） |
| **协议层** | ❌ 裸字符串传输 | ✅ `ChatMessage` 数据模型 + `Command` 协议枚举 |
| **错误码** | ❌ 无统一错误码 | ✅ `ErrorCode` 枚举 + 标准化错误消息 |
| **业务层** | ❌ UI 直接调网络 | ✅ `ClientService` / `ServerService` 中介层 |

---

## 16. 多模块架构重构 — 参考 OpsAny

### 参考项目

| 项目 | 核心模式 | 参考点 |
|------|----------|--------|
| **opsany-paas** | 多服务 + 公共模块 | Maven 多模块拆分 |
| **opsany-backend-framework** | 分层架构 + 错误码 + 配置分离 | Service 层 + ErrorCode |
| **opsany-frontend-framework** | API 层 + 数据模型 | Protocol 协议层 + ChatMessage 模型 |

### 变更说明

#### 1. 多模块 Maven 结构

借鉴 opsany-paas 的多服务架构，将原单模块拆分为三个独立 Maven 模块：

```
旧: 单模块 pom.xml + src/chatroom/{common,client,server}/
新: 父 POM + chatroom-common + chatroom-client + chatroom-server  (独立 jar)
```

| 模块 | 职责 | 对应 OpsAny 概念 |
|------|------|-----------------|
| `chatroom-common` | 协议定义、数据模型、错误码、基础工具 | `component/` + `common/` |
| `chatroom-client` | 客户端 UI + 业务逻辑 + 网络通信 | `paas-ce/paas/paas/`（独立服务） |
| `chatroom-server` | 服务端 UI + 业务逻辑 + 连接管理 | `paas-ce/paas/appengine/`（独立服务） |

#### 2. 新增 Protocol 协议层

借鉴 opsany-frontend-framework 的 API 层和 opsany-backend-framework 的 ESB 组件概念：

```
chatroom.common.protocol/
├── Command.java          # 通信命令枚举 (NICKNAME, MESSAGE, HEARTBEAT...)
└── ChatMessage.java      # 结构化消息模型 (sender, content, timestamp, command)
```

**之前**：所有消息以裸字符串 `"HEARTBEAT"` 或 `"[HH:mm:ss] 昵称: 内容"` 传输  
**之后**：使用 `ChatMessage` 对象结构化封装，`Command` 枚举标识消息类型

#### 3. 新增 ErrorCode 错误码体系

借鉴 opsany-backend-framework 的 `ErrorStatusCode` 设计：

```java
ErrorCode.NOT_CONNECTED      (1001, "未连接到服务器")
ErrorCode.HEARTBEAT_TIMEOUT  (1006, "心跳超时")
ErrorCode.MESSAGE_TOO_LONG   (3002, "消息过长")
```

- 按范围分类：通用(0-999)、网络层(1000-1999)、配置层(2000-2999)、业务层(3000-3999)
- 每个错误码携带默认消息，支持 `format()` 参数化
- 取代散落在各处的硬编码错误字符串

#### 4. 新增 Service 业务逻辑层

借鉴 opsany-backend-framework 的 Controller/Service 分层：

```
旧: UI (ClientChatMain) → 直接调用 → Network (ClientNetworkManager)
新: UI (ClientChatMain) → Service (ClientService) → Network (ClientNetworkManager)
                                        ↑
                                 业务逻辑居中协调
```

`ClientService` 职责：
- 协调 UI 回调与网络操作
- 使用 `ChatMessage` 模型格式化消息
- 提供 `connect(nickname, onMessage, onError, onConnected)` 简洁 API

`ServerService` 职责：
- 管理服务端生命周期
- 将网络事件转化为领域事件
- 封装广播逻辑

### 最终架构对比

| 维度 | 优化前 | 优化后 |
|------|--------|--------|
| **模块数** | 1 个 | 3 个（common + client + server） |
| **架构层数** | 2 层（UI + Network） | 3 层（UI + Service + Network） |
| **协议** | 裸字符串 | `ChatMessage` + `Command` 枚举 |
| **错误处理** | 散乱字符串 | `ErrorCode` 枚举 + 参数化消息 |
| **构建** | Maven 单模块 | Maven 多模块（增量编译） |
| **代码总行数** | ~900 行 | ~1100 行（含大量注释和 Javadoc） |

---

## 遗留问题 / 未来规划

- [ ] **安装 Maven** — 当前系统未安装 Maven，安装后可执行 `mvn clean package` 一键构建
- [ ] **单元测试** — 使用 JUnit 为 `Config`、`ClientService`、`ServerService` 编写测试
- [ ] **消息历史持久化** — 服务端保存聊天记录到文件
- [ ] **私聊功能** — 支持 `@用户名` 私聊
- [ ] **SSL/TLS 加密** — 使用 `SSLSocket` 加密传输
- [ ] **GUI 美化** — 使用 FlatLaf 等现代 LookAndFeel
- [ ] **文件传输** — 支持图片/文件发送
- [ ] **ChatMessage 序列化** — 将 `ChatMessage` 对象序列化为 JSON 传输，替代当前行协议
