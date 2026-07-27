package chatroom.client.ui;

import chatroom.client.service.ClientService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 客户端主窗口。
 * <p>
 * 职责单一：仅负责 UI 展示和用户交互。
 * 所有业务逻辑委托给 {@link ClientService}。
 * 对应于 opsany-frontend-framework 中的 views/ 展示层。
 * </p>
 */
public class ClientChatMain extends JFrame {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        LoginDialog.AuthResult auth = LoginDialog.show();
        if (auth == null) System.exit(0);
        new ClientChatMain(auth);
    }

    private JTextArea messageArea;
    private JTextField inputField;
    private JButton sendButton;
    private JLabel statusLabel;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;

    private final String nickname;
    private final String authAction;
    private final String authUsername;
    private final String authPassword;
    private ClientService clientService;
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public ClientChatMain(LoginDialog.AuthResult auth) {
        this.authAction = auth.action;
        this.authUsername = auth.username;
        this.authPassword = auth.password;
        this.nickname = auth.nickname;
        initUI();
        initListeners();
        setVisible(true);
        startService();
    }

    private void initUI() {
        // 消息区
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        JScrollPane messageScroll = new JScrollPane(messageArea);
        messageScroll.setBorder(BorderFactory.createTitledBorder("💬 聊天消息"));

        // 在线用户列表（右侧）
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        userList.setFixedCellHeight(26);
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setBorder(BorderFactory.createTitledBorder("👤 在线用户"));
        userScroll.setPreferredSize(new Dimension(150, 0));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, messageScroll, userScroll);
        splitPane.setResizeWeight(0.82);
        splitPane.setDividerSize(4);

        // 底部输入区（自适应宽度）
        JPanel bottomPanel = new JPanel(new BorderLayout(6, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        inputField = new JTextField();
        sendButton = new JButton("发送");
        sendButton.setEnabled(false);
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        getRootPane().setDefaultButton(sendButton);

        statusLabel = new JLabel("正在连接...");
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(230, 230, 250));
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        add(statusLabel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        setTitle("聊于云端 - " + nickname);
        setSize(800, 500);
        setMinimumSize(new Dimension(640, 360));
        setLocation(600, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void initListeners() {
        sendButton.addActionListener(e -> sendMessage()); // 点击发送
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (clientService != null) clientService.disconnect(); // 关闭时断开
            }
        });
    }

    private void startService() {
        clientService = new ClientService();
        clientService.connect(authAction, authUsername, authPassword, nickname,
                this::onMessageReceived, this::onError,
                this::onConnectionChanged, this::onUserListChanged);
    }

    private void onMessageReceived(String message) {
        SwingUtilities.invokeLater(() -> {
            if (!sendButton.isEnabled()) {
                sendButton.setEnabled(true);
                inputField.requestFocus();
                printBanner("已连接到服务器");
            }
            appendMessage(message);
        });
    }

    private void onError(String error) {
        SwingUtilities.invokeLater(() -> {
            appendMessage(error);
            setStatusLabel("连接已断开");
        });
    }

    private void onConnectionChanged(boolean connected) {
        SwingUtilities.invokeLater(() -> {
            if (connected) setStatusLabel("正在连接 " + "...");
            else {
                sendButton.setEnabled(false);
                setStatusLabel("连接已断开");
            }
        });
    }

    /** 在线用户列表更新 */
    private void onUserListChanged(List<String> users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String name : users) {
                if (!name.equals(nickname)) userListModel.addElement(name);
            }
            // 自己放最前面
            userListModel.add(0, nickname + " (我)");
        });
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        boolean sent = clientService.sendMessage(text); // 委托业务层发送
        if (!sent) {
            appendMessage("发送失败，连接可能已断开");
            setStatusLabel("连接已断开");
        }
        inputField.setText("");
    }

    private void appendMessage(String msg) {
        messageArea.append(msg + System.lineSeparator());
        messageArea.setCaretPosition(messageArea.getDocument().getLength()); // 自动滚动
    }

    private void setStatusLabel(String text) {
        statusLabel.setText(text);
        if (text.contains("失败") || text.contains("断开")) {
            statusLabel.setBackground(new Color(255, 200, 200)); // 红色=异常
        } else if (text.contains("已连接")) {
            statusLabel.setBackground(new Color(200, 255, 200)); // 绿色=正常
        } else {
            statusLabel.setBackground(new Color(230, 230, 250)); // 紫色=默认
        }
    }

    private void printBanner(String info) {
        appendMessage("============================================");
        appendMessage("  " + info);
        appendMessage("  昵称: " + nickname);
        appendMessage("  可以开始聊天了");
        appendMessage("============================================");
    }
}
