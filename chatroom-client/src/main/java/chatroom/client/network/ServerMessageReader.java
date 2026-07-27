package chatroom.client.network;

import chatroom.common.MessageCallback;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * 服务端消息读取器。
 * <p>
 * 在独立后台线程中循环读取服务端发来的消息，
 * 通过 {@link MessageCallback} 安全地传递给上层。
 * </p>
 */
public class ServerMessageReader implements Runnable {

    private static final Logger LOG = Logger.getLogger(ServerMessageReader.class.getName()); // 日志

    private final Socket socket; // 连接的 Socket
    private final MessageCallback callback; // 消息回调
    private volatile boolean running = true; // 运行状态

    public ServerMessageReader(Socket socket, MessageCallback callback) {
        this.socket = socket;
        this.callback = callback;
    }

    public void stop() {
        this.running = false; // 通知线程停止
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) { // UTF-8 读取
            String line;
            while (running && (line = reader.readLine()) != null) { // 逐行读取
                callback.onMessage(line); // 回调上层处理
            }
        } catch (IOException e) {
            if (running) { // 非主动关闭才报错
                LOG.fine(() -> "读取线程断开: " + e.getMessage());
                callback.onError("与服务器的连接已断开 (" + e.getMessage() + ")");
            }
        }
    }
}
