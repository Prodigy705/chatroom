package chatroom.server.network;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 单个客户端连接处理器。
 * <p>
 * 每个客户端连接对应一个 {@link ClientHandler} 实例，
 * 在独立线程中循环读取客户端消息，通过回调通知上层。
 * </p>
 */
public class ClientHandler implements Runnable {

    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName()); // 日志
    static final long HEARTBEAT_TIMEOUT_MS = 90_000; // 3次心跳无响应=超时

    private final Socket socket; // 客户端 Socket
    private final BufferedReader reader; // UTF-8 读取流
    private final BufferedWriter writer; // UTF-8 写入流
    private final String hostAddress; // 客户端标识 "ip:port"
    private final ClientHandlerListener listener; // 事件回调
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss"); // 时间格式化

    private String nickname; // 用户昵称（认证后不为 null）
    private boolean authenticated;
    private volatile long lastActivityTime = System.currentTimeMillis();

    public ClientHandler(Socket socket, ClientHandlerListener listener) throws IOException {
        this.socket = socket;
        this.listener = listener;
        this.hostAddress = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        try {
            socket.setSoTimeout((int) HEARTBEAT_TIMEOUT_MS);
        } catch (SocketException e) {
            LOG.log(Level.FINE, "设置 Socket 超时失败: {0}", e.getMessage());
        }
    }

    public String getClientInfo() { return hostAddress; }
    public boolean isAuthenticated() { return authenticated; }

    public String getDisplayName() {
        return (nickname != null) ? nickname : hostAddress;
    }

    public long getLastActivityTime() { return lastActivityTime; }

    public boolean isHeartbeatTimeout() {
        return System.currentTimeMillis() - lastActivityTime > HEARTBEAT_TIMEOUT_MS;
    }

    public void sendMessage(String message) {
        try {
            writer.write(message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            LOG.log(Level.FINE, "发送消息给 {0} 失败: {1}", new Object[]{getDisplayName(), e.getMessage()});
        }
    }

    @Override
    public void run() {
        try {
            String line;
            // 首条消息必须是 LOGIN 或 REGISTER
            if ((line = reader.readLine()) != null) {
                lastActivityTime = System.currentTimeMillis();
                if (line.startsWith("LOGIN|")) {
                    handleLogin(line);
                } else if (line.startsWith("REGISTER|")) {
                    handleRegister(line);
                } else {
                    sendMessage("AUTH_FAIL|请先登录 (LOGIN|username|password)");
                    close();
                    listener.onClientDisconnected(this);
                    return;
                }
            }
            if (!authenticated) return;

            // 认证成功后进入主循环
            while ((line = reader.readLine()) != null) {
                lastActivityTime = System.currentTimeMillis();
                if ("HEARTBEAT".equals(line)) {
                    LOG.finest(() -> "收到心跳 - " + getDisplayName());
                    continue;
                }
                if (line.startsWith("@!")) {
                    listener.onClientMessage(this, line);
                    continue;
                }
                String timeStr = new SimpleDateFormat("HH:mm:ss").format(new Date());
                String formatted = "[" + timeStr + "] " + nickname + ": " + line;
                listener.onClientMessage(this, formatted);
            }
        } catch (SocketTimeoutException e) {
            LOG.warning(() -> "客户端 " + getDisplayName() + " 心跳超时，强制断开");
        } catch (IOException e) {
            LOG.log(Level.FINE, "客户端 {0} 连接断开: {1}", new Object[]{getDisplayName(), e.getMessage()});
        } finally {
            close();
            listener.onClientDisconnected(this);
            LOG.info(() -> "客户端已释放: " + getDisplayName());
        }
    }

    /** 处理登录请求: LOGIN|username|password */
    private void handleLogin(String line) throws IOException {
        String[] parts = line.split("\\|", 3);
        if (parts.length < 3) {
            sendMessage("AUTH_FAIL|格式错误");
            return;
        }
        String username = parts[1];
        String password = parts[2];
        // 通过网络层处理认证
        listener.onClientAuthRequest(this, "LOGIN", username, password);
    }

    /** 处理注册请求: REGISTER|username|password|nickname */
    private void handleRegister(String line) throws IOException {
        String[] parts = line.split("\\|", 4);
        if (parts.length < 4) {
            sendMessage("AUTH_FAIL|格式错误");
            return;
        }
        String username = parts[1];
        String password = parts[2];
        String nick = parts[3].isEmpty() ? username : parts[3];
        listener.onClientAuthRequest(this, "REGISTER", username, password, nick);
    }

    /** 认证成功后调用，设置昵称并通知 UI */
    public void onAuthSuccess(String nick) {
        this.nickname = nick;
        this.authenticated = true;
        LOG.info(() -> "客户端 " + hostAddress + " 认证成功，昵称: " + nickname);
        listener.onClientMessage(this, ">>> 用户 [" + nickname + "] 加入了聊天室 (" + hostAddress + ")");
    }

    /** 认证失败 */
    public void onAuthFailed(String reason) {
        sendMessage("AUTH_FAIL|" + reason);
    }

    public void close() {
        try { reader.close(); } catch (IOException e) { LOG.log(Level.FINE, "关闭 reader 异常", e); }
        try { writer.close(); } catch (IOException e) { LOG.log(Level.FINE, "关闭 writer 异常", e); }
        try { if (!socket.isClosed()) socket.close(); } catch (IOException e) { LOG.log(Level.FINE, "关闭 socket 异常", e); }
    }
}
