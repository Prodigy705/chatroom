package chatroom.client.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 登录/注册对话框。
 * <p>支持新用户注册和已有用户登录，带用户名+密码+昵称字段。</p>
 */
public class LoginDialog {

    private LoginDialog() {}

    /** 登录对话框返回结果 */
    public static class AuthResult {
        public final String action; // "LOGIN" or "REGISTER"
        public final String username;
        public final String password;
        public final String nickname;

        public AuthResult(String action, String username, String password, String nickname) {
            this.action = action;
            this.username = username;
            this.password = password;
            this.nickname = nickname;
        }
    }

    /**
     * 显示登录/注册对话框。
     * @return AuthResult 或 null（用户取消）
     */
    public static AuthResult show() {
        JTextField usernameField = new JTextField(16);
        JPasswordField passwordField = new JPasswordField(16);
        JTextField nicknameField = new JTextField(16);
        Font font = new Font("微软雅黑", Font.PLAIN, 14);
        usernameField.setFont(font);
        passwordField.setFont(font);
        nicknameField.setFont(font);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("用户名:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1;
        panel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JLabel("密码:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1;
        panel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panel.add(new JLabel("昵称:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1;
        panel.add(nicknameField, gbc);

        // 按钮
        JButton loginBtn = new JButton("登录");
        JButton registerBtn = new JButton("注册");
        JButton cancelBtn = new JButton("取消");
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        btnPanel.add(loginBtn);
        btnPanel.add(registerBtn);
        btnPanel.add(cancelBtn);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(btnPanel, gbc);

        JDialog dialog = new JDialog();
        dialog.setTitle("聊于云端 - 登录/注册");
        dialog.setModal(true);
        dialog.getContentPane().add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        final AuthResult[] result = {null};

        loginBtn.addActionListener(e -> {
            String u = usernameField.getText().trim();
            String p = new String(passwordField.getPassword());
            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "用户名和密码不能为空");
                return;
            }
            result[0] = new AuthResult("LOGIN", u, p, u);
            dialog.dispose();
        });

        registerBtn.addActionListener(e -> {
            String u = usernameField.getText().trim();
            String p = new String(passwordField.getPassword());
            String n = nicknameField.getText().trim();
            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "用户名和密码不能为空");
                return;
            }
            if (p.length() < 3) {
                JOptionPane.showMessageDialog(dialog, "密码至少3位");
                return;
            }
            result[0] = new AuthResult("REGISTER", u, p, n.isEmpty() ? u : n);
            dialog.dispose();
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
        return result[0];
    }
}
