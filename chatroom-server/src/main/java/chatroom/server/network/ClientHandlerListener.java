package chatroom.server.network;

/**
 * 客户端连接处理器的事件监听接口。
 * <p>
 * 用于 {@link ClientHandler} 与 {@link ServerNetworkManager} 解耦。
 * </p>
 */
public interface ClientHandlerListener {

    /** 收到客户端消息 */
    void onClientMessage(ClientHandler source, String formattedMessage);

    /** 客户端断开连接 */
    void onClientDisconnected(ClientHandler handler);

    /** 客户端认证请求 */
    void onClientAuthRequest(ClientHandler handler, String action, String... args);
}
