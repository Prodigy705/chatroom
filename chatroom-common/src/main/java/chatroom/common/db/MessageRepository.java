package chatroom.common.db;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 消息数据访问对象。
 * <p>基于 SQLite JDBC 实现消息持久化存储。</p>
 */
public class MessageRepository {

    private static final Logger LOG = Logger.getLogger(MessageRepository.class.getName());

    private final Connection conn;

    public MessageRepository() {
        this.conn = DbManager.getInstance().getConnection();
    }

    /** 存储一条消息 */
    public synchronized long addMessage(String sender, String content, String type, String target) {
        String sql = "INSERT INTO messages (sender, content, msg_type, target) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sender);
            ps.setString(2, content != null ? content : "");
            ps.setString(3, type != null ? type : "MESSAGE");
            ps.setString(4, target != null ? target : "");
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "存储消息失败", e);
        }
        return -1;
    }

    /** 获取格式化后的消息历史 */
    public synchronized List<String> getFormattedHistory(int limit) {
        List<String> result = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String sql = "SELECT sender, content, msg_type, target, created_at FROM messages ORDER BY id DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String time = sdf.format(new Date(rs.getTimestamp("created_at").getTime()));
                String sender = rs.getString("sender");
                String content = rs.getString("content");
                String type = rs.getString("msg_type");
                String target = rs.getString("target");
                if ("WHISPER".equals(type)) {
                    result.add(0, "[" + time + "] [私聊 from " + sender + " to " + target + "] " + content);
                } else {
                    result.add(0, "[" + time + "] " + sender + ": " + content);
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "查询消息历史失败", e);
        }
        return result;
    }
}
