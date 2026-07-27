package chatroom.common.db;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite 数据库管理器（单例）。
 * <p>统一管理数据库连接、建表和全局访问。</p>
 */
public class DbManager {

    private static final Logger LOG = Logger.getLogger(DbManager.class.getName());
    private static DbManager INSTANCE;

    private final Connection connection;

    private DbManager(File dataDir) {
        if (!dataDir.exists()) dataDir.mkdirs();
        String url = "jdbc:sqlite:" + new File(dataDir, "chatroom.db").getAbsolutePath();
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(url);
            // 设置忙等待超时，避免 SQLITE_BUSY
            try (Statement s = connection.createStatement()) {
                s.execute("PRAGMA journal_mode=WAL");
                s.execute("PRAGMA busy_timeout=5000");
            }
            initTables();
            LOG.info("SQLite 数据库已就绪: " + url);
        } catch (Exception e) {
            throw new RuntimeException("SQLite 初始化失败", e);
        }
    }

    public static synchronized DbManager init(File dataDir) {
        if (INSTANCE == null) INSTANCE = new DbManager(dataDir);
        return INSTANCE;
    }

    public static DbManager getInstance() {
        if (INSTANCE == null) throw new IllegalStateException("DbManager 未初始化");
        return INSTANCE;
    }

    public Connection getConnection() { return connection; }

    public void close() {
        try { if (connection != null && !connection.isClosed()) connection.close(); }
        catch (SQLException e) { LOG.log(Level.WARNING, "关闭连接异常", e); }
    }

    // ==================== 建表 ====================

    private void initTables() throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  username VARCHAR(50) NOT NULL UNIQUE," +
                "  password VARCHAR(64) NOT NULL," +
                "  nickname VARCHAR(50) NOT NULL," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS messages (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  sender VARCHAR(50) NOT NULL," +
                "  content TEXT NOT NULL," +
                "  msg_type VARCHAR(20) DEFAULT 'MESSAGE'," +
                "  target VARCHAR(50) DEFAULT ''," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
        }
    }
}
