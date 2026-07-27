package chatroom.common;

/**
 * 统一错误码体系。
 * <p>
 * 参考 opsany-backend-framework 的 ErrorStatusCode 设计，
 * 为项目中所有异常场景定义标准化的错误码和消息。
 * 上层可根据 code 做国际化或特定处理。
 * </p>
 */
public enum ErrorCode {

    // ========== 通用（0-999） ==========
    SUCCESS(0, "成功"),
    UNKNOWN_ERROR(1, "未知错误"),
    INVALID_PARAM(2, "参数无效"),

    // ========== 网络层（1000-1999） ==========
    NOT_CONNECTED(1001, "未连接到服务器"),
    CONNECTION_REFUSED(1002, "连接被拒绝"),
    CONNECTION_TIMEOUT(1003, "连接超时"),
    CONNECTION_RESET(1004, "连接已断开"),
    SEND_FAILED(1005, "消息发送失败"),
    HEARTBEAT_TIMEOUT(1006, "心跳超时"),

    // ========== 配置层（2000-2999） ==========
    CONFIG_LOAD_FAILED(2001, "配置文件加载失败"),
    CONFIG_KEY_MISSING(2002, "配置项缺失"),
    CONFIG_TYPE_ERROR(2003, "配置项类型错误"),

    // ========== 业务层（3000-3999） ==========
    NICKNAME_EMPTY(3001, "昵称不能为空"),
    MESSAGE_TOO_LONG(3002, "消息过长"),
    SERVER_START_FAILED(3003, "服务端启动失败"),
    CLIENT_FULL(3004, "客户端连接数已达上限");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /** 获取数字错误码 */
    public int getCode() {
        return code;
    }

    /** 获取默认错误消息 */
    public String getMessage() {
        return message;
    }

    /** 格式化带参数的错误消息 */
    public String format(Object... args) {
        return String.format(message, args);
    }

    @Override
    public String toString() {
        return "[" + code + "] " + message;
    }
}
