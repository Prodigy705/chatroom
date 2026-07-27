package chatroom.common;

import java.io.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 配置管理类（单例）
 * 统一从 chat.properties 加载配置。
 * 支持 classpath（JAR 包内）和文件系统（开发环境）双路径加载。
 */
public class Config {

    private static final Logger LOG = Logger.getLogger(Config.class.getName()); // 日志
    private static final Config INSTANCE = new Config(); // 单例实例
    private final Properties props; // 配置属性集

    private Config() {
        this.props = new Properties();
        try (InputStream is = resolveConfigStream()) {
            props.load(is); // 加载 chat.properties
            LOG.info("配置文件加载成功");
        } catch (IOException e) {
            throw new RuntimeException(ErrorCode.CONFIG_LOAD_FAILED.format(), e);
        }
    }

    public static Config getInstance() {
        return INSTANCE; // 获取全局单例
    }

    public String get(String key, String defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            LOG.warning(() -> ErrorCode.CONFIG_KEY_MISSING.format() + " '" + key + "', 使用默认值: " + defaultValue);
            return defaultValue; // 配置缺失，返回默认值
        }
        return value.trim();
    }

    public String get(String key) {
        return props.getProperty(key); // 无默认值版
    }

    public int getInt(String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            LOG.warning(() -> ErrorCode.CONFIG_KEY_MISSING.format() + " '" + key + "', 使用默认值: " + defaultValue);
            return defaultValue; // 配置缺失，返回默认值
        }
        try {
            return Integer.parseInt(value.trim()); // 字符串转整数
        } catch (NumberFormatException e) {
            LOG.log(Level.WARNING, ErrorCode.CONFIG_TYPE_ERROR.format() + " '" + key + "'=" + value, e);
            return defaultValue; // 格式错误，返回默认值
        }
    }

    public int getInt(String key) {
        return Integer.parseInt(props.getProperty(key)); // 无默认值版
    }

    private InputStream resolveConfigStream() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("chat.properties"); // 优先 classpath
        if (is != null) {
            LOG.fine("从 classpath 加载配置文件");
            return is;
        }
        for (String path : new String[]{"chat.properties", "../chat.properties", "../../chat.properties"}) { // 多路径兜底
            File f = new File(path);
            if (f.exists()) {
                LOG.fine("从文件系统加载配置文件: " + f.getAbsolutePath());
                return new FileInputStream(f);
            }
        }
        throw new FileNotFoundException("chat.properties 未找到（已搜索 classpath 和文件系统）");
    }
}
