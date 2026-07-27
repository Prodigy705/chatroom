package chatroom.client.service;

import chatroom.client.network.ClientNetworkManager;
import chatroom.client.network.ServerMessageReader;
import chatroom.common.MessageCallback;
import chatroom.common.protocol.ChatMessage;
import chatroom.common.protocol.Command;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 客户端业务逻辑层。
 * <p>
 * 位于 UI 层和网络层之间，职责包括：
 * <ul>
 *   <li>管理连接生命周期</li>
 *   <li>将 UI 层的请求转化为网络操作</li>
 *   <li>将网络层的原始数据转化为 {@link ChatMessage} 领域对象</li>
 *   <li>提供简洁的 API 供 UI 层调用</li>
 * </ul>
 * 参考 opsany-backend-framework 中 Service 层的设计理念。
 * </p>
 */
public class ClientService {

    private static final int MAX_MESSAGE_LENGTH = 1024;

    private ClientNetworkManager networkManager;
    private String nickname;
    private boolean connected;
    private String authAction;
    private String authUsername;
    private String authPassword;

    private final List<String> onlineUsers = new ArrayList<>();

    private Consumer<String> messageConsumer;
    private Consumer<String> errorConsumer;
    private Consumer<Boolean> connectionConsumer;
    private Consumer<List<String>> userListConsumer;

    public void connect(String authAction, String username, String password, String nickname,
                        Consumer<String> onMessage,
                        Consumer<String> onError,
                        Consumer<Boolean> onConnected,
                        Consumer<List<String>> onUserList) {
        this.authAction = authAction;
        this.authUsername = username;
        this.authPassword = password;
        this.nickname = nickname;
        this.messageConsumer = onMessage;
        this.errorConsumer = onError;
        this.connectionConsumer = onConnected;
        this.userListConsumer = onUserList;

        networkManager = new ClientNetworkManager(authAction, username, password, nickname, createNetworkCallback());
        new Thread(() -> {
            connected = true;
            if (connectionConsumer != null) connectionConsumer.accept(true);
            networkManager.connect();
            connected = false;
            if (connectionConsumer != null) connectionConsumer.accept(false);
        }, "Client-Connect").start();
    }

    /**
     * 发送消息。自动识别私聊格式 {@code @用户 消息}。
     */
    public boolean sendMessage(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        if (text.length() > MAX_MESSAGE_LENGTH) text = text.substring(0, MAX_MESSAGE_LENGTH);

        // 检测 @用户 私聊格式
        if (text.startsWith("@")) {
            int spaceIdx = text.indexOf(' ');
            if (spaceIdx > 1) {
                String target = text.substring(1, spaceIdx);
                String content = text.substring(spaceIdx + 1).trim();
                if (!content.isEmpty() && onlineUsers.contains(target)) {
                    String whisper = "@!whisper|" + target + "|" + content;
                    if (messageConsumer != null)
                        messageConsumer.accept("[私聊 to " + target + "] " + content);
                    return networkManager != null && networkManager.sendMessage(whisper);
                } else {
                    if (messageConsumer != null)
                        messageConsumer.accept("[错误] 用户 " + target + " 不在线或格式错误");
                    return false;
                }
            }
        }

        // 普通聊天消息
        ChatMessage localMsg = ChatMessage.createChat(nickname, text);
        if (messageConsumer != null) messageConsumer.accept(localMsg.format());
        return networkManager != null && networkManager.sendMessage(text);
    }

    public void disconnect() {
        if (networkManager != null) networkManager.disconnect();
        connected = false;
    }

    public boolean isConnected() { return connected; }
    public String getNickname() { return nickname; }
    public List<String> getOnlineUsers() { return new ArrayList<>(onlineUsers); }

    private MessageCallback createNetworkCallback() {
        return new MessageCallback() {
            @Override
            public void onMessage(String message) {
                if (message == null) return;

                // 解析 @!userlist|name1,name2
                if (message.startsWith("@!userlist|")) {
                    String listStr = message.substring("@!userlist|".length());
                    onlineUsers.clear();
                    if (!listStr.isEmpty()) {
                        String[] names = listStr.split(",");
                        for (String n : names) if (!n.isEmpty()) onlineUsers.add(n);
                    }
                    if (userListConsumer != null) userListConsumer.accept(getOnlineUsers());
                    return;
                }

                // 解析 @!whisper|sender|content → 显示为 [私聊 from sender] content
                if (message.startsWith("@!whisper|")) {
                    String[] parts = message.split("\\|", 3);
                    if (parts.length == 3) {
                        String from = parts[1];
                        String content = parts[2];
                        if (messageConsumer != null)
                            messageConsumer.accept("[私聊 from " + from + "] " + content);
                        return;
                    }
                }

                if (messageConsumer != null) messageConsumer.accept(message);
            }

            @Override
            public void onError(String error) {
                connected = false;
                if (errorConsumer != null) errorConsumer.accept(error);
            }
        };
    }
}
