package chatroom.client.network;

import chatroom.common.Config;
import chatroom.common.MessageCallback;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 客户端网络管理层。
 * <p>
 * 职责单一：管理与服务端的 Socket 连接、消息收发、心跳保活。
 * 不依赖上层框架，通过 {@link MessageCallback} 回传数据。
 * 对应于 opsany-backend-framework 中 component/esb_api.py 的通信层。
 * </p>
 */
public class ClientNetworkManager {

    private static final Logger LOG = Logger.getLogger(ClientNetworkManager.class.getName()); // 日志
    private static final long HEARTBEAT_INTERVAL_MS = 30_000; // 30秒发一次心跳

    private final String serverIp;
    private final int serverPort;
    private final String username;
    private final String password;
    private final String nickname;
    private final String authAction; // "LOGIN" or "REGISTER"
    private final MessageCallback callback;

    private Socket socket;
    private BufferedWriter writer;
    private ServerMessageReader messageReader;
    private volatile boolean connected;

    public ClientNetworkManager(String authAction, String username, String password, String nickname, MessageCallback callback) {
        this.authAction = authAction;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.callback = callback;
        this.serverIp = Config.getInstance().get("clientIp", "127.0.0.1");
        this.serverPort = Config.getInstance().getInt("clientPort", 8888);
    }

    public String getServerIp() { return serverIp; }
    public int getServerPort() { return serverPort; }
    public boolean isConnected() { return connected; }

    /** 连接到服务端并认证（阻塞直到断开） */
    public void connect() {
        try {
            socket = new Socket(serverIp, serverPort);
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            // 发送认证请求
            if ("REGISTER".equals(authAction)) {
                writer.write("REGISTER|" + username + "|" + password + "|" + nickname);
            } else {
                writer.write("LOGIN|" + username + "|" + password);
            }
            writer.newLine();
            writer.flush();
            connected = true;

            // 读取认证响应（首行）
            BufferedReader tempReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String authResponse = tempReader.readLine();

            if (authResponse == null) {
                callback.onError("服务器无响应");
                closeResources();
                return;
            }

            if (authResponse.startsWith("LOGIN_OK|") || authResponse.startsWith("REGISTER_OK|")) {
                String displayName = authResponse.contains("|") ? authResponse.split("\\|", 2)[1] : nickname;
                callback.onAuthSuccess(displayName, serverIp, serverPort);
            } else if (authResponse.startsWith("AUTH_FAIL|")) {
                String reason = authResponse.contains("|") ? authResponse.split("\\|", 2)[1] : "认证失败";
                callback.onError(reason);
                closeResources();
                return;
            } else {
                callback.onError("认证失败: " + authResponse);
                closeResources();
                return;
            }

            // 认证通过，启动消息读取
            messageReader = new ServerMessageReader(socket, createReaderCallback());
            Thread readerThread = new Thread(messageReader, "Server-Reader");
            readerThread.start();

            startHeartbeat();

            try {
                readerThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "连接失败", e);
            callback.onError("连接失败: " + e.getMessage());
        } finally {
            closeResources();
        }
    }

    public void disconnect() {
        connected = false;
        if (messageReader != null) messageReader.stop(); // 停止读取线程
        closeResources();
    }

    public boolean sendMessage(String text) {
        if (!connected || writer == null) return false;
        try {
            synchronized (this) { // 同步写避免线程冲突
                writer.write(text);
                writer.newLine();
                writer.flush();
            }
            return true;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "发送消息失败", e);
            callback.onError("发送失败，连接可能已断开");
            return false;
        }
    }

    private MessageCallback createReaderCallback() {
        return new MessageCallback() {
            @Override
            public void onMessage(String message) {
                callback.onMessage(message); // 透传消息
            }

            @Override
            public void onError(String error) {
                connected = false; // 标记断开
                callback.onError(error);
            }
        };
    }

    private void startHeartbeat() {
        Thread heartbeatThread = new Thread(() -> {
            while (connected && writer != null) {
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL_MS);
                    if (connected) {
                        synchronized (this) {
                            if (writer != null) {
                                writer.write("HEARTBEAT");
                                writer.newLine();
                                writer.flush();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    LOG.log(Level.FINE, "发送心跳失败", e);
                    break;
                }
            }
        }, "Client-Heartbeat");
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    private void closeResources() {
        connected = false;
        try { if (writer != null) writer.close(); } catch (IOException e) {
            LOG.log(Level.FINE, "关闭 writer 异常", e);
        }
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException e) {
            LOG.log(Level.FINE, "关闭 socket 异常", e);
        }
    }
}
