package chatroom.common;

import chatroom.common.protocol.ChatMessage;

/**
 * 网络层 → 业务/UI 层回调接口。
 * <p>
 * 网络线程通过此接口将数据交给上层安全地处理，
 * 使网络层无需依赖任何上层框架。
 * 参考 opsany 中 MessageCallback 的设计理念。
 * </p>
 */
@FunctionalInterface
public interface MessageCallback {

    /** 收到一条格式化后的消息文本（用于 UI 展示） */
    void onMessage(String message);

    /** 收到结构化消息对象（业务层使用） */
    default void onChatMessage(ChatMessage msg) {
        onMessage(msg.format());
    }

    /** 发生错误 */
    default void onError(String error) {
        onMessage("错误: " + error);
    }

    /** 认证成功回调 */
    default void onAuthSuccess(String displayName, String serverIp, int serverPort) {
        onMessage("已连接到服务器 " + serverIp + ":" + serverPort);
    }
}
