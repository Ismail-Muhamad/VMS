
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class Login extends JFrame {

    private JTextField txtUser;
    private JPasswordField txtPass;
    private final JLabel lblMsg;

    public Login() {
        setTitle("تسجيل الدخول");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 340);
        setMinimumSize(new Dimension(480, 320));
        setLocationRelativeTo(null);

        Color primaryBlue = new Color(18, 72, 132);
        Color bg = new Color(243, 244, 246);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(bg);
        root.setBorder(new EmptyBorder(18, 18, 18, 18));
        root.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("تسجيل الدخول");
        title.setFont(new Font("Tahoma", Font.BOLD, 20));
        title.setHorizontalAlignment(SwingConstants.RIGHT);

        JLabel sub = new JLabel("ADMIN / SUB_ADMIN / USER");
        sub.setFont(new Font("Tahoma", Font.PLAIN, 12));
        sub.setForeground(new Color(100, 116, 139));
        sub.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(sub);

        header.add(titles, BorderLayout.CENTER);
        root.add(header, BorderLayout.NORTH);

        // Card
        CardPanel card = new CardPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        txtUser = new JTextField();
        txtUser.setFont(new Font("Tahoma", Font.PLAIN, 14));
        txtUser.setHorizontalAlignment(SwingConstants.RIGHT);
        txtUser.setBorder(BorderFactory.createTitledBorder("اسم المستخدم"));

        txtPass = new JPasswordField();
        txtPass.setFont(new Font("Tahoma", Font.PLAIN, 14));
        txtPass.setHorizontalAlignment(SwingConstants.RIGHT);
        txtPass.setBorder(BorderFactory.createTitledBorder("كلمة المرور"));

        lblMsg = new JLabel(" ");
        lblMsg.setForeground(new Color(220, 38, 38));
        lblMsg.setHorizontalAlignment(SwingConstants.RIGHT);
        lblMsg.setFont(new Font("Tahoma", Font.PLAIN, 12));

        JButton btnLogin = new JButton("تسجيل الدخول");
        btnLogin.setFont(new Font("Tahoma", Font.PLAIN, 14));
        btnLogin.setBackground(primaryBlue);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.setMargin(new Insets(10, 10, 10, 10));

        // Enter يعمل Login
        ActionListener doLogin = e -> tryLogin();
        btnLogin.addActionListener(doLogin);
        txtUser.addActionListener(doLogin);
        txtPass.addActionListener(doLogin);

        gbc.gridy = 0;
        form.add(txtUser, gbc);
        gbc.gridy = 1;
        form.add(txtPass, gbc);
        gbc.gridy = 2;
        form.add(lblMsg, gbc);
        gbc.gridy = 3;
        form.add(btnLogin, gbc);

        card.add(form, BorderLayout.CENTER);
        root.add(card, BorderLayout.CENTER);

        setContentPane(root);
    }

    private static class CardPanel extends JPanel {

        private final int arc = 18;
        private final int shadowSize = 8;

        public CardPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = shadowSize;
            int y = shadowSize;
            int w = getWidth() - shadowSize * 2;
            int h = getHeight() - shadowSize * 2;

            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillRoundRect(x + 2, y + 3, w, h, arc, arc);

            g2.setColor(getBackground());
            g2.fillRoundRect(x, y, w, h, arc, arc);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    private void tryLogin() {
        lblMsg.setText(" ");
        String UserName = txtUser.getText().trim();
        String Password = new String(txtPass.getPassword());

        if (UserName.isEmpty() || Password.isEmpty()) {
            lblMsg.setText("من فضلك اكتب اسم المستخدم وكلمة المرور.");
            return;
        }

        try {
            AuthUser user = AuthService.login(UserName, Password);
            if (user == null) {
                lblMsg.setText("بيانات الدخول غير صحيحة.");
                return;
            }

            // افتح الداشبورد
            TransportDashboard dash = new TransportDashboard(user);
            dash.setVisible(true);
            dispose();

        } catch (Exception ex) {
            lblMsg.setText("مشكلة اتصال/استعلام DB: " + ex.getMessage());
            System.out.println(ex.getMessage());

        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new Login().setVisible(true));
    }
}
