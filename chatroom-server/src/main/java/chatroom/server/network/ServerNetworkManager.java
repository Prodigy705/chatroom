package chatroom.server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 服务端网络管理层。
 * <p>
 * 职责单一：管理 ServerSocket、客户端连接/断开、消息广播、心跳扫描。
 * 不依赖上层框架，通过 {@link ServerListener} 回传事件。
 * </p>
 */
public class ServerNetworkManager {

    private static final Logger LOG = Logger.getLogger(ServerNetworkManager.class.getName()); // 日志
    private static final int HEARTBEAT_SCAN_INTERVAL_MS = 15_000; // 心跳扫描间隔

    private final int port; // 监听端口
    private final ServerListener listener; // 事件回调
    private ServerSocket serverSocket; // 服务端 Socket
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>(); // 线程安全客户端列表
    private volatile boolean running; // 运行状态

    public ServerNetworkManager(int port, ServerListener listener) {
        this.port = port;
        this.listener = listener;
    }

    public void start() {
        running = true;
        try {
            serverSocket = new ServerSocket(port); // 绑定端口
            serverSocket.setReuseAddress(true); // 允许端口复用
            listener.onStatusChanged("服务端已启动，端口: " + port);
            listener.onMessage("============================================");
            listener.onMessage("  服务端已启动，监听端口: " + port);
            listener.onMessage("  等待客户端连接...");
            listener.onMessage("============================================");

            Thread heartbeatThread = new Thread(this::heartbeatScanLoop, "Server-Heartbeat"); // 启动心跳扫描
            heartbeatThread.setDaemon(true);
            heartbeatThread.start();
            acceptLoop(); // 进入接受连接循环
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "服务端启动失败", e);
            listener.onStatusChanged("服务端启动失败: " + e.getMessage());
        } finally {
            closeAll(); // 确保资源清理
        }
    }

    public void stop() {
        running = false;
        closeAll();
        LOG.info("服务端已停止");
    }

    public int getClientCount() { return clients.size(); }

    public void broadcast(String message, ClientHandler source) {
        for (ClientHandler client : clients) {
            if (client != source) client.sendMessage(message); // 不回显给发送者
        }
    }

    public void broadcastAll(String message) {
        broadcast(message, null); // 广播给所有客户端
    }

    /** 构建当前在线客户端昵称列表 */
    private List<String> buildClientNameList() {
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        for (ClientHandler c : clients) names.add(c.getDisplayName());
        return names;
    }

    /** 通知 UI 更新客户端列表，并广播用户列表给所有客户端 */
    private void notifyClientListChanged() {
        listener.onClientCountChanged(clients.size());
        listener.onClientListChanged(buildClientNameList());
        // 广播 @!userlist 到所有在线客户端
        String userListMsg = "@!userlist|" + String.join(",", buildClientNameList());
        broadcastAll(userListMsg);
    }

    /** 处理私聊：将消息路由给目标客户端 */
    private void routeWhisper(ClientHandler source, String rawLine) {
        // rawLine 格式: @!whisper|targetName|content
        String[] parts = rawLine.split("\\|", 3);
        if (parts.length < 3) return;
        String targetName = parts[1];
        String content = parts[2];
        String senderName = source.getDisplayName();

        // 查找目标客户端
        for (ClientHandler target : clients) {
            if (target.getDisplayName().equals(targetName)) {
                // 发送给目标
                target.sendMessage("@!whisper|" + senderName + "|" + content);
                // 发送回执给发送者
                String echo = "[私聊 to " + targetName + "] " + content;
                listener.onMessage(source.getClientInfo() + " 私聊 " + targetName + ": " + content);
                return;
            }
        }
        // 目标不在线
        source.sendMessage("[错误] 用户 " + targetName + " 不在线");
    }

    private void acceptLoop() {
        while (running && !serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, createClientListener());
                // 暂不加入列表，等认证成功后加入
                new Thread(handler, "Client-" + socket.getPort()).start();
                listener.onMessage(">>> 新连接: " + handler.getClientInfo());
                LOG.info(() -> "新连接: " + handler.getClientInfo());
            } catch (IOException e) {
                if (running) {
                    LOG.log(Level.WARNING, "接受客户端连接异常", e);
                    listener.onStatusChanged("接受连接异常: " + e.getMessage());
                }
            }
        }
    }

    private ClientHandlerListener createClientListener() {
        return new ClientHandlerListener() {
            @Override
            public void onClientMessage(ClientHandler source, String formattedMessage) {
                if (formattedMessage.startsWith("@!whisper|")) {
                    routeWhisper(source, formattedMessage);
                    return;
                }
                listener.onMessage(formattedMessage);
                broadcast(formattedMessage, source);
            }

            @Override
            public void onClientDisconnected(ClientHandler handler) {
                clients.remove(handler);
                notifyClientListChanged();
                if (handler.isAuthenticated())
                    listener.onMessage("<<< 客户端已断开: " + handler.getDisplayName());
                LOG.info(() -> "客户端已断开: " + handler.getDisplayName());
            }

            @Override
            public void onClientAuthRequest(ClientHandler handler, String action, String... args) {
                if ("LOGIN".equals(action) && args.length >= 2) {
                    String error = listener.onAuthRequest(action, args[0], args[1], null);
                    if (error == null) {
                        String nick = listener.onGetNickname(args[0]);
                        handler.onAuthSuccess(nick != null ? nick : args[0]);
                        clients.add(handler);
                        notifyClientListChanged();
                        handler.sendMessage("LOGIN_OK|" + (nick != null ? nick : args[0]));
                    } else {
                        handler.onAuthFailed(error);
                    }
                } else if ("REGISTER".equals(action) && args.length >= 3) {
                    String error = listener.onAuthRequest(action, args[0], args[1], args[2]);
                    if (error == null) {
                        handler.onAuthSuccess(args[2]);
                        clients.add(handler);
                        notifyClientListChanged();
                        handler.sendMessage("REGISTER_OK|" + args[2]);
                    } else {
                        handler.onAuthFailed(error);
                    }
                }
            }
        };
    }

    private void heartbeatScanLoop() {
        while (running) {
            try {
                Thread.sleep(HEARTBEAT_SCAN_INTERVAL_MS); // 定期扫描
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            long now = System.currentTimeMillis();
            Iterator<ClientHandler> it = clients.iterator();
            while (it.hasNext()) {
                ClientHandler h = it.next();
                if (h.isHeartbeatTimeout()) { // 判定超时
                    String name = h.getDisplayName();
                    LOG.warning(() -> "心跳超时断开: " + name);
                    listener.onMessage("<<< 心跳超时断开: " + name);
                    h.close(); // 强制关闭
                    it.remove(); // 从列表移除
                    listener.onClientCountChanged(clients.size());
                }
            }
        }
    }

    private void closeAll() {
        for (ClientHandler client : clients) client.close(); // 关闭所有客户端
        clients.clear();
        try { if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close(); } catch (IOException e) {
            LOG.log(Level.FINE, "关闭 ServerSocket 异常", e);
        }
        listener.onStatusChanged("服务端已停止");
    }
}
