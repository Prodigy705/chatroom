package chatroom.server.service;

import chatroom.common.db.DbManager;
import chatroom.common.db.MessageRepository;
import chatroom.common.db.UserRepository;
import chatroom.common.protocol.ChatMessage;
import chatroom.server.network.ServerListener;
import chatroom.server.network.ServerNetworkManager;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 服务端业务逻辑层。
 * <p>
 * 位于 UI 层和网络层之间，职责包括：
 * <ul>
 *   <li>管理服务端生命周期（启动/停止）</li>
 *   <li>将网络层的原始事件转化为领域事件</li>
 *   <li>处理消息广播逻辑</li>
 *   <li>提供简洁的 API 供 UI 层调用</li>
 * </ul>
 * 参考 opsany-backend-framework 中 Service 层的设计理念。
 * </p>
 */
public class ServerService {

    private static final Logger LOG = Logger.getLogger(ServerService.class.getName());

    private ServerNetworkManager networkManager;
    private boolean running;
    private UserRepository userRepo;
    private MessageRepository messageRepo;

    private Consumer<String> messageConsumer;
    private Consumer<String> statusConsumer;
    private Consumer<List<String>> clientListConsumer;

    public void start(int port,
                      Consumer<String> onMessage,
                      Consumer<String> onStatus,
                      Consumer<List<String>> onClientListChanged) {
        this.messageConsumer = onMessage;
        this.statusConsumer = onStatus;
        this.clientListConsumer = onClientListChanged;
        this.running = true;

        // 初始化 SQLite 数据库
        try {
            DbManager.init(new File("data"));
            userRepo = new UserRepository();
            messageRepo = new MessageRepository();
            status("数据库已就绪");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "数据库初始化失败", e);
            status("数据库初始化失败: " + e.getMessage());
        }

        networkManager = new ServerNetworkManager(port, createServerListener());
        new Thread(networkManager::start, "Server-Accept").start();
        status("服务端启动中，端口: " + port);
    }

    public void broadcast(String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (!running || networkManager == null) return;

        ChatMessage serverMsg = ChatMessage.createChat("服务器", text.trim());
        String formatted = serverMsg.format();
        message(formatted);
        networkManager.broadcastAll(formatted);
    }

    public void stop() {
        running = false;
        if (networkManager != null) networkManager.stop();
        if (DbManager.getInstance() != null) {
            try { DbManager.getInstance().close(); } catch (Exception ignored) {}
        }
        status("服务端已停止");
    }

    public boolean isRunning() { return running; }

    private ServerListener createServerListener() {
        return new ServerListener() {
            @Override
            public void onMessage(String message) {
                message(message);
            }

            @Override
            public void onClientCountChanged(int count) {
                status("在线人数: " + count);
            }

            @Override
            public void onClientListChanged(List<String> displayNames) {
                if (clientListConsumer != null) clientListConsumer.accept(displayNames);
            }

            @Override
            public void onStatusChanged(String text) {
                status(text);
            }

            @Override
            public String onAuthRequest(String action, String username, String password, String nickname) {
                if (userRepo == null) return "数据库未就绪";
                if ("LOGIN".equals(action)) return userRepo.authenticate(username, password);
                if ("REGISTER".equals(action)) return userRepo.register(username, password, nickname);
                return "未知操作";
            }

            @Override
            public String onGetNickname(String username) {
                if (userRepo == null) return username;
                String nick = userRepo.getNickname(username);
                return nick != null ? nick : username;
            }
        };
    }

    private void message(String msg) {
        if (messageConsumer != null) messageConsumer.accept(msg);
        // 存储到 SQLite
        if (messageRepo != null && msg != null) {
            try {
                // 解析消息格式 [HH:mm:ss] sender: content
                String sender = "系统";
                String content = msg;
                String type = "MESSAGE";
                String target = "";

                if (msg.contains("] ")) {
                    int start = msg.indexOf("] ") + 2;
                    String after = msg.substring(start);
                    if (after.startsWith("[私聊")) {
                        type = "WHISPER";
                        content = after;
                    } else if (after.contains(": ")) {
                        int sep = after.indexOf(": ");
                        sender = after.substring(0, sep);
                        content = after.substring(sep + 2);
                    } else {
                        content = after;
                    }
                }
                messageRepo.addMessage(sender, content, type, target);
            } catch (Exception e) {
                LOG.log(Level.FINE, "存储消息异常", e);
            }
        }
    }

    private void status(String text) {
        if (statusConsumer != null) statusConsumer.accept(text);
    }
}
