package chatroom.common.protocol;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 聊天消息数据模型。
 * <p>
 * 封装消息的所有元数据，取代原始的字符串传输。
 * 相当于 Django Model / Vuex Store 中的数据结构定义。
 * </p>
 */
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");

    /** 消息类型 */
    private Command command;
    /** 发送者昵称 */
    private String sender;
    /** 消息内容 */
    private String content;
    /** 发送时间戳（毫秒） */
    private long timestamp;
    /** 发送者主机地址（ip:port，由服务端填充） */
    private String hostAddress;

    /** 默认构造（JSON 反序列化用） */
    public ChatMessage() {
    }

    /**
     * 快速创建一条聊天消息。
     *
     * @param sender  发送者昵称
     * @param content 消息正文
     */
    public static ChatMessage createChat(String sender, String content) {
        ChatMessage msg = new ChatMessage();
        msg.command = Command.MESSAGE;
        msg.sender = sender;
        msg.content = content;
        msg.timestamp = System.currentTimeMillis();
        return msg;
    }

    /**
     * 创建一条系统通知。
     *
     * @param content 通知内容
     */
    public static ChatMessage createSystemNotice(String content) {
        ChatMessage msg = new ChatMessage();
        msg.command = Command.SYSTEM_NOTICE;
        msg.sender = "系统";
        msg.content = content;
        msg.timestamp = System.currentTimeMillis();
        return msg;
    }

    /**
     * 创建一条用户加入/离开通知。
     *
     * @param command USER_JOIN 或 USER_LEAVE
     * @param nickname 用户昵称
     */
    public static ChatMessage createUserEvent(Command command, String nickname) {
        ChatMessage msg = new ChatMessage();
        msg.command = command;
        msg.sender = nickname;
        msg.timestamp = System.currentTimeMillis();
        return msg;
    }

    /**
     * 创建一条私聊消息。
     *
     * @param sender   发送者昵称
     * @param target   目标用户昵称
     * @param content  消息正文
     */
    public static ChatMessage createWhisper(String sender, String target, String content) {
        ChatMessage msg = new ChatMessage();
        msg.command = Command.WHISPER;
        msg.sender = sender;
        msg.content = target + "|" + content; // target|content 编码在 content 中
        msg.timestamp = System.currentTimeMillis();
        return msg;
    }

    /**
     * 格式化消息为可读字符串。
     * <pre>
     * 聊天消息:   "[HH:mm:ss] 昵称: 内容"
     * 私聊消息:   "[私聊 from 昵称] 内容"
     * 系统通知:   "*** 内容"
     * 用户事件:   ">>> 用户 [昵称] 加入了聊天室"
     * </pre>
     */
    public String format() {
        String time = SDF.format(new Date(timestamp));
        switch (command) {
            case MESSAGE:      return "[" + time + "] " + sender + ": " + content;
            case WHISPER: {
                int sep = content.indexOf('|');
                if (sep > 0) {
                    String target = content.substring(0, sep);
                    String msg = content.substring(sep + 1);
                    return "[" + time + "] [私聊 to " + target + "] " + msg;
                }
                return "[" + time + "] [私聊] " + content;
            }
            case SYSTEM_NOTICE: return "[" + time + "] *** " + content;
            case USER_JOIN:    return "[" + time + "] >>> 用户 [" + sender + "] 加入了聊天室"
                        + (hostAddress != null ? " (" + hostAddress + ")" : "");
            case USER_LEAVE:   return "[" + time + "] <<< 用户 [" + sender + "] 离开了聊天室";
            case ERROR:        return "[" + time + "] 错误: " + content;
            default:           return content != null ? content : "";
        }
    }

    /**
     * 序列化为行协议字符串（用于 TCP 传输）。
     * <pre>
     * 普通消息:   "[HH:mm:ss] 昵称: 内容"
     * 私聊:      "@!whisper|target|sender|content"
     * 用户列表:  "@!userlist|name1,name2"
     * </pre>
     */
    public String wireFormat() {
        switch (command) {
            case WHISPER: {
                int sep = content.indexOf('|');
                String target = content.substring(0, sep);
                String msg = content.substring(sep + 1);
                return "@!whisper|" + target + "|" + sender + "|" + msg;
            }
            case USER_LIST: return "@!userlist|" + content;
            default:        return format();
        }
    }

    /**
     * 判断一行协议文本是否为控制指令。
     */
    public static boolean isWireControl(String line) {
        return line.startsWith("@!") || "HEARTBEAT".equals(line);
    }

    /**
     * 从 {@link #wireFormat()} 反序列化私聊消息。
     */
    public static ChatMessage parseWhisper(String line) {
        // @!whisper|target|sender|content
        String[] parts = line.split("\\|", 4);
        if (parts.length == 4) {
            ChatMessage msg = new ChatMessage();
            msg.command = Command.WHISPER;
            msg.sender = parts[2];
            msg.content = parts[1] + "|" + parts[3]; // target|content
            msg.timestamp = System.currentTimeMillis();
            return msg;
        }
        return null;
    }

    public Command getCommand() { return command; }
    public void setCommand(Command command) { this.command = command; } // 设置消息类型
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; } // 设置发送者
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; } // 设置消息正文
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; } // 设置时间戳
    public String getHostAddress() { return hostAddress; }
    public void setHostAddress(String hostAddress) { this.hostAddress = hostAddress; } // 设置主机地址

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatMessage)) return false;
        ChatMessage that = (ChatMessage) o;
        return timestamp == that.timestamp
                && command == that.command
                && Objects.equals(sender, that.sender)
                && Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command, sender, content, timestamp);
    }

    @Override
    public String toString() {
        return format();
    }
}
