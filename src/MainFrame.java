import Panels.*;
import model.User;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    // ── Logged-in user passed from login screen ──
    private final User currentUser;

    // ── Keep references to DB panels for refresh on tab switch ──
    private HistoryPanel    historyPanel;
    private CTFTrackerPanel ctfTrackerPanel;

    // -------------------------------------------------------
    // Constructor — now accepts logged-in User object
    // -------------------------------------------------------
    public MainFrame(User user) {
        this.currentUser = user;
        configureWindow();
        buildUI();
        finalizeWindow();
    }

    // -------------------------------------------------------
    // configureWindow()
    // -------------------------------------------------------
    private void configureWindow() {
        setTitle("Steganography Tool — " + currentUser.getUsername());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(750, 650));
        setPreferredSize(new Dimension(850, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(245, 245, 250));
    }

    // -------------------------------------------------------
    // buildUI()
    // -------------------------------------------------------
    private void buildUI() {
        setLayout(new BorderLayout());
        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(buildTabbedPane(),  BorderLayout.CENTER);
        add(buildFooterPanel(), BorderLayout.SOUTH);
    }

    // -------------------------------------------------------
    // buildHeaderPanel()
    // Now shows logged-in username + logout button
    // -------------------------------------------------------
    private JPanel buildHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(new Color(30, 30, 60));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("Steganography Tool", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel(
                "Logged in as: " + currentUser.getUsername()
                        + "   |   Hide and extract secret messages inside images",
                SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        subtitleLabel.setForeground(new Color(180, 180, 220));

        // Logout button
        JButton logoutBtn = new JButton("🚪 Logout");
        logoutBtn.setFont(new Font("Arial", Font.BOLD, 11));
        logoutBtn.setBackground(new Color(160, 60, 60));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> handleLogout());

        panel.add(titleLabel,    BorderLayout.CENTER);
        panel.add(subtitleLabel, BorderLayout.SOUTH);
        panel.add(logoutBtn,     BorderLayout.EAST);
        return panel;
    }

    // -------------------------------------------------------
    // buildTabbedPane()
    // Same tabs as your original + 2 new DB tabs added
    // EncodePanel and DecodePanel now receive currentUser
    // -------------------------------------------------------
    private JTabbedPane buildTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 13));
        tabbedPane.setBackground(new Color(245, 245, 250));

        // ── Create DB-aware panels — pass user to them ──
        historyPanel    = new HistoryPanel(currentUser);
        ctfTrackerPanel = new CTFTrackerPanel(currentUser);

        // ── Your original tabs — EncodePanel/DecodePanel now get user ──
        tabbedPane.addTab("🔒  Encode",        new EncodePanel(currentUser));
        tabbedPane.addTab("🔓  Decode",        new DecodePanel(currentUser));
        tabbedPane.addTab("🔍  Compare",       new ComparisonPanel());
        tabbedPane.addTab("🏴  CTF Tools",     new CTFPanel());
        tabbedPane.addTab("🧪  CTF Analysis",  new CTFAnalysisPanel());
        tabbedPane.addTab("🔓 LSB Decoder",    new LSBDecoderPanel());
        tabbedPane.addTab("🎛️ Bit Planes",     new BitPlanePanel());
        tabbedPane.addTab("📦 Embedded Files", new EmbeddedFilePanel());
        tabbedPane.addTab("🧩 PNG Chunks",     new PNGChunkPanel());
        tabbedPane.addTab("✨ Enhancement",    new EnhancementPanel());
        tabbedPane.addTab("⊕ XOR Analysis",   new XORPanel());
        tabbedPane.addTab("🕵️ Steghide",       new SteghidePanel());

        // ── NEW: Database tabs ──
        tabbedPane.addTab("📋 History",       historyPanel);
        tabbedPane.addTab("🏴 CTF Tracker",   ctfTrackerPanel);
        tabbedPane.addTab("🔎 EXIF Metadata",  new EXIFPanel());

        // ── Your original tooltips (unchanged) ──
        tabbedPane.setToolTipTextAt(0,  "Hide a secret message inside an image");
        tabbedPane.setToolTipTextAt(1,  "Extract a hidden message from an image");
        tabbedPane.setToolTipTextAt(2,  "Compare original and encoded images");
        tabbedPane.setToolTipTextAt(3,  "CTF tools - LSB, Pixel Inspector, RGB, Hex");
        tabbedPane.setToolTipTextAt(4,  "Advanced CTF - Metadata, Signatures, Statistics, Patterns");
        tabbedPane.setToolTipTextAt(5,  "Extract hidden messages from LSB of pixel channels");
        tabbedPane.setToolTipTextAt(6,  "View all 8 bit planes per channel — detect hidden data visually");
        tabbedPane.setToolTipTextAt(7,  "Scan for files hidden or appended inside images");
        tabbedPane.setToolTipTextAt(8,  "Parse and inspect every PNG chunk — find hidden data in chunks");
        tabbedPane.setToolTipTextAt(9,  "Apply brightness, contrast, gamma and edge filters");
        tabbedPane.setToolTipTextAt(10, "XOR two images to reveal exact pixel differences");
        tabbedPane.setToolTipTextAt(11, "Steghide password attack on JPEG/BMP images");
        tabbedPane.setToolTipTextAt(12, "View your encode/decode history from database");
        tabbedPane.setToolTipTextAt(13, "Track CTF challenges and record found flags");
        tabbedPane.setToolTipTextAt(14, "Extract EXIF, IPTC, XMP, GPS metadata from images");
        // ── Refresh DB panels automatically when their tab is clicked ──
        tabbedPane.addChangeListener(e -> {
            Component selected = tabbedPane.getSelectedComponent();
            if (selected == historyPanel) {
                historyPanel.loadHistory();
            } else if (selected == ctfTrackerPanel) {
                ctfTrackerPanel.loadChallenges();
            }
        });

        return tabbedPane;
    }

    // -------------------------------------------------------
    // buildFooterPanel()
    // Now shows username in footer
    // -------------------------------------------------------
    private JPanel buildFooterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 30, 60));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 15, 6, 15));

        JLabel statusLabel = new JLabel(
                "Ready  —  AES-256 Encrypted  |  LSB Steganography  |  PNG Format",
                SwingConstants.LEFT);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(180, 180, 220));

        JLabel userLabel = new JLabel(
                "User: " + currentUser.getUsername() + "  |  v1.0",
                SwingConstants.RIGHT);
        userLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        userLabel.setForeground(new Color(180, 180, 220));

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(userLabel,   BorderLayout.EAST);
        return panel;
    }

    // -------------------------------------------------------
    // handleLogout()
    // Closes MainFrame and reopens login screen
    // -------------------------------------------------------
    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?",
                "Logout", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            dispose();          // close this window
            Main.showLogin();   // reopen login screen
        }
    }

    // -------------------------------------------------------
    // finalizeWindow()
    // -------------------------------------------------------
    private void finalizeWindow() {
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}