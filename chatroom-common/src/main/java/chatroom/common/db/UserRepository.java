package chatroom.common.db;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 用户数据访问对象。
 * <p>基于 SQLite JDBC 实现用户注册与认证。</p>
 */
public class UserRepository {

    private static final Logger LOG = Logger.getLogger(UserRepository.class.getName());

    private final Connection conn;

    public UserRepository() {
        this.conn = DbManager.getInstance().getConnection();
    }

    /** 注册新用户。返回 null 表示成功，否则返回错误消息 */
    public synchronized String register(String username, String password, String nickname) {
        if (username == null || username.trim().isEmpty()) return "用户名不能为空";
        if (password == null || password.length() < 3) return "密码至少3位";

        String hashed = PasswordUtils.hash(password);
        String sql = "INSERT INTO users (username, password, nickname) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            ps.setString(2, hashed);
            ps.setString(3, nickname != null ? nickname.trim() : username.trim());
            ps.executeUpdate();
            LOG.info(() -> "新用户注册: " + username + " / " + nickname);
            return null;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                return "用户名已存在";
            }
            LOG.log(Level.WARNING, "注册失败", e);
            return "注册失败: " + e.getMessage();
        }
    }

    /** 用户登录认证。返回 null 表示成功，否则返回错误消息 */
    public String authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty()) return "用户名不能为空";
        String sql = "SELECT password FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hashed = rs.getString("password");
                if (PasswordUtils.verify(password, hashed)) return null;
            }
            return "用户名或密码错误";
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "登录查询失败", e);
            return "登录失败: " + e.getMessage();
        }
    }

    /** 获取用户昵称 */
    public String getNickname(String username) {
        String sql = "SELECT nickname FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("nickname");
        } catch (SQLException e) {
            LOG.log(Level.FINE, "查询昵称失败", e);
        }
        return username;
    }
}
