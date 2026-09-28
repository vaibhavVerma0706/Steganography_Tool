package Panels;

import db.DatabaseManager;
import db.UserDAO;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class LoginPanel extends JPanel {

    // ── Callback: called when login succeeds, passes User object ──
    public interface LoginListener {
        void onLoginSuccess(User user);
    }

    // ── Fields ──
    private final LoginListener loginListener;

    // ── Login form fields ──
    private JTextField     loginUsernameField;
    private JPasswordField loginPasswordField;
    private JLabel         loginStatusLabel;

    // ── Register form fields ──
    private JTextField     regUsernameField;
    private JPasswordField regPasswordField;
    private JPasswordField regConfirmField;
    private JLabel         regStatusLabel;

    // ── Card layout to switch between login and register ──
    private JPanel     cardsPanel;
    private CardLayout cardLayout;

    private static final String CARD_LOGIN    = "LOGIN";
    private static final String CARD_REGISTER = "REGISTER";

    // ──────────────────────────────────────────────────────
    // Constructor
    // ──────────────────────────────────────────────────────
    public LoginPanel(LoginListener listener) {
        this.loginListener = listener;

        setLayout(new GridBagLayout()); // centres content
        setBackground(new Color(30, 30, 60));

        add(buildCentreCard());
    }

    // ──────────────────────────────────────────────────────
    // Centre card — contains header + card switcher
    // ──────────────────────────────────────────────────────
    private JPanel buildCentreCard() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(new Color(245, 245, 255));
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));
        panel.setPreferredSize(new Dimension(420, 500));

        // Round look via overlay border
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 200), 2),
                new EmptyBorder(30, 40, 30, 40)));

        panel.add(buildHeader(),  BorderLayout.NORTH);
        panel.add(buildCards(),   BorderLayout.CENTER);
        panel.add(buildDbStatus(),BorderLayout.SOUTH);
        return panel;
    }

    // ── Header ──
    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(new Color(245, 245, 255));

        JLabel title = new JLabel("🔒 Steganography Tool", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setForeground(new Color(30, 30, 100));

        JLabel sub = new JLabel(
                "CTF Analysis & Image Steganography", SwingConstants.CENTER);
        sub.setFont(new Font("Arial", Font.ITALIC, 12));
        sub.setForeground(Color.GRAY);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(180, 180, 220));

        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setBackground(new Color(245, 245, 255));
        top.add(title, BorderLayout.NORTH);
        top.add(sub,   BorderLayout.CENTER);
        top.add(sep,   BorderLayout.SOUTH);
        top.setBorder(new EmptyBorder(0, 0, 20, 0));

        panel.add(top, BorderLayout.CENTER);
        return panel;
    }

    // ── Card switcher ──
    private JPanel buildCards() {
        cardLayout  = new CardLayout();
        cardsPanel  = new JPanel(cardLayout);
        cardsPanel.setBackground(new Color(245, 245, 255));

        cardsPanel.add(buildLoginForm(),    CARD_LOGIN);
        cardsPanel.add(buildRegisterForm(), CARD_REGISTER);

        cardLayout.show(cardsPanel, CARD_LOGIN);
        return cardsPanel;
    }

    // ──────────────────────────────────────────────────────
    // LOGIN FORM
    // ──────────────────────────────────────────────────────
    private JPanel buildLoginForm() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(245, 245, 255));

        JLabel heading = makeHeading("Login to Your Account");
        panel.add(heading);
        panel.add(Box.createVerticalStrut(20));

        // Username
        panel.add(makeFieldLabel("Username"));
        panel.add(Box.createVerticalStrut(4));
        loginUsernameField = makeTextField("Enter your username");
        panel.add(loginUsernameField);
        panel.add(Box.createVerticalStrut(14));

        // Password
        panel.add(makeFieldLabel("Password"));
        panel.add(Box.createVerticalStrut(4));
        loginPasswordField = makePasswordField("Enter your password");
        panel.add(loginPasswordField);
        panel.add(Box.createVerticalStrut(20));

        // Status label
        loginStatusLabel = makeStatusLabel();
        panel.add(loginStatusLabel);
        panel.add(Box.createVerticalStrut(8));

        // Login button
        JButton loginBtn = makeMainButton("🔓 Login", new Color(34, 100, 200));
        loginBtn.addActionListener(e -> handleLogin());
        panel.add(loginBtn);
        panel.add(Box.createVerticalStrut(14));

        // Switch to register
        JLabel switchLabel = makeSwitchLabel(
                "Don't have an account?  ");
        JButton switchBtn = makeLinkButton("Register here");
        switchBtn.addActionListener(e ->
                cardLayout.show(cardsPanel, CARD_REGISTER));

        JPanel switchRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        switchRow.setBackground(new Color(245, 245, 255));
        switchRow.add(switchLabel);
        switchRow.add(switchBtn);
        panel.add(switchRow);

        // Allow pressing Enter to login
        loginPasswordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) handleLogin();
            }
        });

        return panel;
    }

    // ──────────────────────────────────────────────────────
    // REGISTER FORM
    // ──────────────────────────────────────────────────────
    private JPanel buildRegisterForm() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(245, 245, 255));

        JLabel heading = makeHeading("Create New Account");
        panel.add(heading);
        panel.add(Box.createVerticalStrut(16));

        // Username
        panel.add(makeFieldLabel("Username"));
        panel.add(Box.createVerticalStrut(4));
        regUsernameField = makeTextField("Choose a username");
        panel.add(regUsernameField);
        panel.add(Box.createVerticalStrut(12));

        // Password
        panel.add(makeFieldLabel("Password"));
        panel.add(Box.createVerticalStrut(4));
        regPasswordField = makePasswordField("Choose a password");
        panel.add(regPasswordField);
        panel.add(Box.createVerticalStrut(12));

        // Confirm password
        panel.add(makeFieldLabel("Confirm Password"));
        panel.add(Box.createVerticalStrut(4));
        regConfirmField = makePasswordField("Confirm your password");
        panel.add(regConfirmField);
        panel.add(Box.createVerticalStrut(16));

        // Status label
        regStatusLabel = makeStatusLabel();
        panel.add(regStatusLabel);
        panel.add(Box.createVerticalStrut(8));

        // Register button
        JButton regBtn = makeMainButton("✅ Create Account", new Color(34, 139, 34));
        regBtn.addActionListener(e -> handleRegister());
        panel.add(regBtn);
        panel.add(Box.createVerticalStrut(14));

        // Switch to login
        JLabel switchLabel = makeSwitchLabel("Already have an account?  ");
        JButton switchBtn  = makeLinkButton("Login here");
        switchBtn.addActionListener(e -> {
            cardLayout.show(cardsPanel, CARD_LOGIN);
            clearRegisterForm();
        });

        JPanel switchRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        switchRow.setBackground(new Color(245, 245, 255));
        switchRow.add(switchLabel);
        switchRow.add(switchBtn);
        panel.add(switchRow);

        return panel;
    }

    // ── DB status indicator at bottom ──
    private JPanel buildDbStatus() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(new Color(245, 245, 255));
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));

        boolean connected = DatabaseManager.testConnection();
        String  icon      = connected ? "🟢" : "🔴";
        String  text      = connected ? "Database Connected" : "Database Not Connected";
        Color   colour    = connected
                ? new Color(0, 120, 0) : new Color(180, 0, 0);

        JLabel status = new JLabel(icon + "  " + text);
        status.setFont(new Font("Arial", Font.PLAIN, 11));
        status.setForeground(colour);
        panel.add(status);
        return panel;
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Handle login
    // ══════════════════════════════════════════════════════
    private void handleLogin() {
        String username = loginUsernameField.getText().trim();
        String password = new String(loginPasswordField.getPassword());

        // Validation
        if (username.isEmpty()) {
            setStatus(loginStatusLabel, "Please enter your username.", Color.RED);
            return;
        }
        if (password.isEmpty()) {
            setStatus(loginStatusLabel, "Please enter your password.", Color.RED);
            return;
        }

        // Disable button while logging in
        setStatus(loginStatusLabel, "Logging in...", Color.GRAY);

        // Attempt login
        User user = UserDAO.login(username, password);

        if (user != null) {
            setStatus(loginStatusLabel, "✅ Login successful!", new Color(0, 120, 0));
            // Notify MainFrame that login succeeded
            loginListener.onLoginSuccess(user);
        } else {
            setStatus(loginStatusLabel,
                    "❌ Wrong username or password.", Color.RED);
            loginPasswordField.setText("");
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Handle register
    // ══════════════════════════════════════════════════════
    private void handleRegister() {
        String username = regUsernameField.getText().trim();
        String password = new String(regPasswordField.getPassword());
        String confirm  = new String(regConfirmField.getPassword());

        // Validation
        if (username.isEmpty()) {
            setStatus(regStatusLabel, "Please enter a username.", Color.RED);
            return;
        }
        if (username.length() < 3) {
            setStatus(regStatusLabel,
                    "Username must be at least 3 characters.", Color.RED);
            return;
        }
        if (password.isEmpty()) {
            setStatus(regStatusLabel, "Please enter a password.", Color.RED);
            return;
        }
        if (password.length() < 4) {
            setStatus(regStatusLabel,
                    "Password must be at least 4 characters.", Color.RED);
            return;
        }
        if (!password.equals(confirm)) {
            setStatus(regStatusLabel,
                    "Passwords do not match.", Color.RED);
            regConfirmField.setText("");
            return;
        }

        setStatus(regStatusLabel, "Creating account...", Color.GRAY);

        String result = UserDAO.register(username, password);

        switch (result) {
            case "ok" -> {
                setStatus(regStatusLabel,
                        "✅ Account created! Please login.", new Color(0, 120, 0));
                clearRegisterForm();
                // Switch to login after short delay
                Timer timer = new Timer(1200, e ->
                        cardLayout.show(cardsPanel, CARD_LOGIN));
                timer.setRepeats(false);
                timer.start();
            }
            case "taken" -> setStatus(regStatusLabel,
                    "❌ Username already taken.", Color.RED);
            default -> setStatus(regStatusLabel,
                    "❌ Registration failed. Check DB connection.", Color.RED);
        }
    }

    // ══════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════
    private void clearRegisterForm() {
        regUsernameField.setText("");
        regPasswordField.setText("");
        regConfirmField.setText("");
        regStatusLabel.setText(" ");
    }

    private void setStatus(JLabel label, String text, Color colour) {
        label.setText(text);
        label.setForeground(colour);
    }

    // ── UI component factories ──
    private JLabel makeHeading(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Arial", Font.BOLD, 16));
        l.setForeground(new Color(30, 30, 100));
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private JLabel makeFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", Font.BOLD, 12));
        l.setForeground(new Color(60, 60, 100));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JTextField makeTextField(String placeholder) {
        JTextField f = new JTextField();
        f.setFont(new Font("Arial", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 220)),
                new EmptyBorder(6, 8, 6, 8)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        return f;
    }

    private JPasswordField makePasswordField(String placeholder) {
        JPasswordField f = new JPasswordField();
        f.setFont(new Font("Arial", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 220)),
                new EmptyBorder(6, 8, 6, 8)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        return f;
    }

    private JButton makeMainButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Arial", Font.BOLD, 13));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        return b;
    }

    private JLabel makeSwitchLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", Font.PLAIN, 11));
        l.setForeground(Color.GRAY);
        return l;
    }

    private JButton makeLinkButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Arial", Font.BOLD, 11));
        b.setForeground(new Color(34, 100, 200));
        b.setBackground(new Color(245, 245, 255));
        b.setBorder(BorderFactory.createEmptyBorder());
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setFocusPainted(false);
        return b;
    }

    private JLabel makeStatusLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font("Arial", Font.PLAIN, 11));
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }
}