package chatroom.common.db;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 密码工具类。
 * <p>使用 SHA-256 对密码进行哈希，不存储明文密码。</p>
 */
public class PasswordUtils {

    private static final String ALGORITHM = "SHA-256";

    private PasswordUtils() {}

    /**
     * 对密码进行 SHA-256 哈希。
     * @param password 明文密码
     * @return 十六进制哈希字符串
     */
    public static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            byte[] digest = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }

    /** 验证密码 */
    public static boolean verify(String password, String hashed) {
        return hash(password).equals(hashed);
    }
}
