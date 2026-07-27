package chatroom.common.protocol;

/**
 * 通信协议命令类型枚举。
 * <p>
 * 定义了客户端与服务端之间传输的所有消息类型。
 * 相当于 ESB API 中的接口定义层，统一了通信协议。
 * </p>
 */
public enum Command {

    /** 用户昵称注册（客户端 → 服务端，首条消息） */
    NICKNAME,
    /** 登录请求：LOGIN|username|password */
    LOGIN,
    /** 注册请求：REGISTER|username|password|nickname */
    REGISTER,
    /** 登录成功响应：LOGIN_OK|nickname */
    LOGIN_OK,
    /** 注册成功响应 */
    REGISTER_OK,
    /** 认证失败响应：AUTH_FAIL|reason */
    AUTH_FAIL,
    /** 聊天消息（双向） */
    MESSAGE,
    /** 心跳包（客户端 → 服务端） */
    HEARTBEAT,
    /** 系统通知（服务端 → 客户端） */
    SYSTEM_NOTICE,
    /** 错误消息（双向） */
    ERROR,
    /** 用户加入通知（服务端 → 客户端） */
    USER_JOIN,
    /** 用户离开通知（服务端 → 客户端） */
    USER_LEAVE,
    /** 私聊消息（双向） */
    WHISPER,
    /** 在线用户列表（服务端 → 客户端） */
    USER_LIST;

    /**
     * 判断是否为控制类命令（不应展示在聊天区域）。
     */
    public boolean isControl() {
        return this == HEARTBEAT || this == NICKNAME;
    }

    /**
     * 判断是否为内部协议消息（不应直接展示）。
     */
    public boolean isInternal() {
        return this == HEARTBEAT || this == NICKNAME || this == USER_LIST;
    }
}
