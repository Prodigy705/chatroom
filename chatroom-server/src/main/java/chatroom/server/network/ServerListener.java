package chatroom.server.network;

import java.util.List;

/**
 * 服务端事件监听接口。
 * <p>
 * 用于 {@link ServerNetworkManager} 与 UI/Service 层解耦。
 * </p>
 */
public interface ServerListener {

    /** 收到一条新消息 */
    void onMessage(String message);

    /** 在线人数变化 */
    void onClientCountChanged(int count);

    /** 客户端列表变化 */
    default void onClientListChanged(List<String> displayNames) {
        onClientCountChanged(displayNames.size());
    }

    /** 服务端状态变化 */
    void onStatusChanged(String status);

    /** 处理认证请求。返回 null=成功，非null=错误消息 */
    default String onAuthRequest(String action, String username, String password, String nickname) {
        return "认证服务未实现";
    }

    /** 根据用户名获取昵称 */
    default String onGetNickname(String username) {
        return username;
    }
}
