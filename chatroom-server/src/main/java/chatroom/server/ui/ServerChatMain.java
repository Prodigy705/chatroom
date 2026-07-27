package chatroom.server.ui;

import chatroom.common.Config;
import chatroom.server.service.ServerService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * 服务端主窗口 / 无头服务端入口。
 * <p>
 * 职责单一：仅负责 UI 展示和用户交互。
 * 所有业务逻辑委托给 {@link ServerService}。
 * </p>
 *
 * <h3>启动方式</h3>
 * <ul>
 *   <li>无参数 — 启动 GUI 模式</li>
 *   <li>{@code --headless} — 纯命令行模式</li>
 *   <li>{@code --port=<number>} — 指定端口</li>
 * </ul>
 */
public class ServerChatMain extends JFrame {

    private static final Logger LOG = Logger.getLogger(ServerChatMain.class.getName()); // 日志
    private static final int MAX_MESSAGE_LENGTH = 1024; // 消息长度上限

    public static void main(String[] args) {
        boolean headless = false;
        int port = Config.getInstance().getInt("serverPort", 8888); // 读取配置端口

        for (String arg : args) { // 解析命令行参数
            if ("--headless".equalsIgnoreCase(arg)) headless = true; // 无头模式
            else if (arg.startsWith("--port=")) { // 指定端口
                try { port = Integer.parseInt(arg.substring("--port=".length())); }
                catch (NumberFormatException e) {
                    System.err.println("无效端口号，使用默认值 " + port);
                }
            }
        }

        ServerChatMain app = new ServerChatMain(port, headless);
        app.start();
    }

    private JTextArea messageArea; // 消息展示区
    private JTextField inputField; // 输入框
    private JButton sendButton; // 发送按钮
    private JLabel statusLabel; // 状态栏
    private DefaultListModel<String> clientListModel; // 客户端列表模型
    private JList<String> clientList; // 客户端列表面板
    private final boolean headless; // 无头模式标识

    private final int serverPort; // 监听端口
    private ServerService serverService; // 业务逻辑层
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss"); // 时间格式化

    public ServerChatMain(int port, boolean headless) {
        this.serverPort = port;
        this.headless = headless;
        if (!headless) {
            initUI();
            initListeners();
        }
    }

    public void start() {
        if (!headless) SwingUtilities.invokeLater(() -> setVisible(true));
        serverService = new ServerService();
        serverService.start(serverPort, this::outputMessage, this::outputStatus, this::onClientListChanged);
    }

    private void initUI() {
        // === 消息区 ===
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        JScrollPane messageScroll = new JScrollPane(messageArea);
        messageScroll.setBorder(BorderFactory.createTitledBorder("📋 消息日志"));

        // === 客户端列表（右侧可视化） ===
        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        clientList.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        clientList.setFixedCellHeight(28);
        clientList.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JScrollPane clientScroll = new JScrollPane(clientList);
        clientScroll.setBorder(BorderFactory.createTitledBorder("👤 在线客户端"));
        clientScroll.setPreferredSize(new Dimension(180, 0));

        // === 左右分割 ===
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, messageScroll, clientScroll);
        splitPane.setResizeWeight(0.78); // 消息区占 78%，客户端列表占 22%
        splitPane.setDividerSize(4);

        // === 底部输入区（自适应宽度） ===
        JPanel bottomPanel = new JPanel(new BorderLayout(6, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        inputField = new JTextField(); // 不设固定列数，自适应填充
        sendButton = new JButton("发送");
        bottomPanel.add(inputField, BorderLayout.CENTER); // 输入框填满剩余空间
        bottomPanel.add(sendButton, BorderLayout.EAST);   // 按钮固定在右侧
        getRootPane().setDefaultButton(sendButton);

        // === 状态栏 ===
        statusLabel = new JLabel("服务端启动中...");
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(230, 230, 250));
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        add(statusLabel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        setTitle("聊于云端 服务端");
        setSize(800, 500);
        setMinimumSize(new Dimension(640, 360));
        setLocation(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void initListeners() {
        sendButton.addActionListener(e -> sendMessage());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (serverService != null) serverService.stop();
            }
        });
    }

    private void sendMessage() {
        if (inputField == null) return;
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        if (text.length() > MAX_MESSAGE_LENGTH) text = text.substring(0, MAX_MESSAGE_LENGTH);
        serverService.broadcast(text);
        inputField.setText("");
    }

    /** 输出消息到消息区 */
    private void outputMessage(String msg) {
        if (headless) {
            System.out.println(msg);
        } else {
            SwingUtilities.invokeLater(() -> {
                messageArea.append(msg + System.lineSeparator());
                messageArea.setCaretPosition(messageArea.getDocument().getLength());
            });
        }
    }

    /** 更新状态栏 + 客户端列表 */
    private void outputStatus(String text) {
        if (!headless) SwingUtilities.invokeLater(() -> statusLabel.setText(text));
        LOG.info(() -> text);
    }

    /** {@link ServerListener#onClientListChanged} 回调 — 刷新右侧客户端列表 */
    private void onClientListChanged(List<String> displayNames) {
        if (headless) return;
        SwingUtilities.invokeLater(() -> {
            clientListModel.clear();
            for (String name : displayNames) clientListModel.addElement(name);
        });
    }
}
