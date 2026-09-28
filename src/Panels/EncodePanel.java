package Panels;

import db.HistoryDAO;
import engine.ImageSteganography;
import engine.PixelScatter;
import model.User;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

public class EncodePanel extends JPanel {

    private final User currentUser;

    private BufferedImage selectedImage;
    private BufferedImage encodedImage;
    private File          selectedImageFile;
    private File          selectedSecretFile;

    private JLabel         imagePreviewLabel;
    private JLabel         imageInfoLabel;
    private JToggleButton  modeText;
    private JToggleButton  modeFile;
    private JPanel         inputCardsPanel;
    private CardLayout     inputCardLayout;
    private JTextArea      messageTextArea;
    private JLabel         charCountLabel;
    private JLabel         secretFileLabel;
    private JProgressBar   capacityBar;
    private JLabel         capacityLabel;
    private JPasswordField passwordField;
    private JButton        encodeButton;
    private JButton        saveButton;

    private static final String CARD_TEXT   = "TEXT";
    private static final String CARD_FILE   = "FILE";
    private static final String FILE_PREFIX = "FILE:";

    // ──────────────────────────────────────────────────────
    public EncodePanel(User user) {
        this.currentUser = user;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(new Color(245, 245, 250));

        capacityBar = new JProgressBar(0, 100);
        capacityBar.setStringPainted(true);
        capacityBar.setString("Upload an image first");
        capacityBar.setForeground(new Color(70, 130, 180));

        capacityLabel = new JLabel("Image capacity: N/A");
        capacityLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        capacityLabel.setForeground(Color.GRAY);

        add(buildTopSection(),    BorderLayout.NORTH);
        add(buildMiddleSection(), BorderLayout.CENTER);
        add(buildBottomSection(), BorderLayout.SOUTH);
    }

    private JPanel buildTopSection() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("Step 1 — Upload Cover Image"));

        imagePreviewLabel = new JLabel("No Image Selected", SwingConstants.CENTER);
        imagePreviewLabel.setPreferredSize(new Dimension(200, 150));
        imagePreviewLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        imagePreviewLabel.setBackground(Color.WHITE);
        imagePreviewLabel.setOpaque(true);
        imagePreviewLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        imagePreviewLabel.setForeground(Color.GRAY);

        imageInfoLabel = new JLabel("Upload a PNG, BMP or JPG image to begin");
        imageInfoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        imageInfoLabel.setForeground(Color.GRAY);

        JLabel hint = new JLabel("Tip: Larger images = more space to hide data");
        hint.setFont(new Font("Arial", Font.ITALIC, 10));
        hint.setForeground(new Color(180, 100, 0));

        JButton uploadBtn = makeBtn("📁 Upload Cover Image", new Color(70, 130, 180));
        uploadBtn.addActionListener(e -> handleImageUpload());

        JPanel right = new JPanel(new BorderLayout(5, 5));
        right.setBackground(new Color(245, 245, 250));
        right.add(imageInfoLabel, BorderLayout.NORTH);
        right.add(hint,           BorderLayout.CENTER);
        right.add(uploadBtn,      BorderLayout.SOUTH);

        panel.add(imagePreviewLabel, BorderLayout.WEST);
        panel.add(right,             BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildMiddleSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(new Color(245, 245, 250));
        panel.add(buildModeToggleRow(), BorderLayout.NORTH);
        panel.add(buildInputCards(),    BorderLayout.CENTER);
        panel.add(buildPasswordPanel(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildModeToggleRow() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("Step 2 — Choose What to Hide"));

        modeText = new JToggleButton("💬 Hide Text Message", true);
        modeFile = new JToggleButton("📦 Hide Any File (ZIP, PDF, TXT...)");
        styleToggle(modeText, true);
        styleToggle(modeFile, false);

        ButtonGroup group = new ButtonGroup();
        group.add(modeText);
        group.add(modeFile);

        modeText.addActionListener(e -> {
            styleToggle(modeText, true);
            styleToggle(modeFile, false);
            inputCardLayout.show(inputCardsPanel, CARD_TEXT);
            selectedSecretFile = null;
            updateCapacity();
        });
        modeFile.addActionListener(e -> {
            styleToggle(modeText, false);
            styleToggle(modeFile, true);
            inputCardLayout.show(inputCardsPanel, CARD_FILE);
            updateCapacity();
        });

        JLabel hint = new JLabel("  ← Switch between hiding text or a file");
        hint.setFont(new Font("Arial", Font.ITALIC, 10));
        hint.setForeground(new Color(100, 100, 140));
        panel.add(modeText);
        panel.add(modeFile);
        panel.add(hint);
        return panel;
    }

    private JPanel buildInputCards() {
        inputCardLayout = new CardLayout();
        inputCardsPanel = new JPanel(inputCardLayout);
        inputCardsPanel.setBackground(new Color(245, 245, 250));
        inputCardsPanel.add(buildTextCard(), CARD_TEXT);
        inputCardsPanel.add(buildFileCard(), CARD_FILE);
        inputCardLayout.show(inputCardsPanel, CARD_TEXT);
        return inputCardsPanel;
    }

    private JPanel buildTextCard() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("Type Your Secret Message"));

        messageTextArea = new JTextArea(4, 40);
        messageTextArea.setFont(new Font("Arial", Font.PLAIN, 13));
        messageTextArea.setLineWrap(true);
        messageTextArea.setWrapStyleWord(true);
        messageTextArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        messageTextArea.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {
                    public void insertUpdate(javax.swing.event.DocumentEvent e)  { updateCapacity(); }
                    public void removeUpdate(javax.swing.event.DocumentEvent e)  { updateCapacity(); }
                    public void changedUpdate(javax.swing.event.DocumentEvent e) { updateCapacity(); }
                });

        charCountLabel = new JLabel("0 characters");
        charCountLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        charCountLabel.setForeground(Color.GRAY);

        panel.add(new JScrollPane(messageTextArea), BorderLayout.CENTER);
        panel.add(charCountLabel,                   BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildFileCard() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("Select File to Hide Inside Image"));

        JLabel desc = new JLabel("""
            <html><b>Hide any file inside the image:</b><br>
            ZIP · RAR · PDF · TXT · DOCX · PNG · MP3 · EXE...<br><br>
            <font color='#B46400'>⚠️ Larger files need larger cover images!</font></html>""");
        desc.setFont(new Font("Arial", Font.PLAIN, 11));

        secretFileLabel = new JLabel("No file selected — click Browse");
        secretFileLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        secretFileLabel.setForeground(Color.GRAY);
        secretFileLabel.setBackground(Color.WHITE);
        secretFileLabel.setOpaque(true);
        secretFileLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                new EmptyBorder(5, 8, 5, 8)));

        JButton browseBtn = makeBtn("📂 Browse File", new Color(139, 90, 34));
        browseBtn.addActionListener(e -> handleSelectSecretFile());

        JPanel fileRow = new JPanel(new BorderLayout(8, 0));
        fileRow.setBackground(new Color(245, 245, 250));
        fileRow.add(secretFileLabel, BorderLayout.CENTER);
        fileRow.add(browseBtn,       BorderLayout.EAST);

        panel.add(desc,    BorderLayout.NORTH);
        panel.add(fileRow, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildPasswordPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("Step 3 — Enter Password"));

        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordField.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel hint = new JLabel("Remember this password — you need it to decode!");
        hint.setFont(new Font("Arial", Font.ITALIC, 11));
        hint.setForeground(new Color(180, 100, 0));

        JPanel capRow = new JPanel(new BorderLayout(5, 0));
        capRow.setBackground(new Color(245, 245, 250));
        capRow.add(capacityBar,   BorderLayout.CENTER);
        capRow.add(capacityLabel, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.setBackground(new Color(245, 245, 250));
        top.add(passwordField, BorderLayout.CENTER);
        top.add(hint,          BorderLayout.SOUTH);

        panel.add(top,    BorderLayout.CENTER);
        panel.add(capRow, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildBottomSection() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panel.setBackground(new Color(245, 245, 250));

        encodeButton = makeBtn("🔒 Encode & Hide", new Color(34, 139, 34));
        encodeButton.setFont(new Font("Arial", Font.BOLD, 14));
        encodeButton.setPreferredSize(new Dimension(180, 40));
        encodeButton.addActionListener(e -> handleEncode());

        saveButton = makeBtn("💾 Save Image", new Color(70, 130, 180));
        saveButton.setFont(new Font("Arial", Font.BOLD, 14));
        saveButton.setPreferredSize(new Dimension(150, 40));
        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> handleSave());

        panel.add(encodeButton);
        panel.add(saveButton);
        return panel;
    }

    private void handleImageUpload() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Cover Image");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image Files (PNG, BMP, JPG, JPEG)", "png", "bmp", "jpg", "jpeg"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        try {
            BufferedImage loaded = ImageIO.read(file);
            if (loaded == null) { showError("Cannot read image."); return; }

            BufferedImage rgb = new BufferedImage(
                    loaded.getWidth(), loaded.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = rgb.createGraphics();
            g2.drawImage(loaded, 0, 0, null);
            g2.dispose();

            selectedImage     = rgb;
            selectedImageFile = file;

            imagePreviewLabel.setIcon(new ImageIcon(
                    selectedImage.getScaledInstance(200, 150, Image.SCALE_SMOOTH)));
            imagePreviewLabel.setText("");
            imageInfoLabel.setText(file.getName() + "  |  "
                    + selectedImage.getWidth() + " × "
                    + selectedImage.getHeight() + " px");

            encodedImage = null;
            saveButton.setEnabled(false);
            updateCapacity();
        } catch (Exception ex) {
            showError("Error loading image:\n" + ex.getMessage());
        }
    }

    private void handleSelectSecretFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select File to Hide Inside Image");
        fc.setAcceptAllFileFilterUsed(true);
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Common Files", "zip","rar","7z","pdf","txt","docx",
                "xlsx","png","jpg","jpeg","gif","mp3","wav","exe","bin"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        selectedSecretFile = fc.getSelectedFile();
        secretFileLabel.setText("✅  " + selectedSecretFile.getName()
                + "  (" + formatSize(selectedSecretFile.length()) + ")");
        secretFileLabel.setForeground(new Color(34, 100, 34));
        updateCapacity();
    }

    private void updateCapacity() {
        if (selectedImage == null) {
            capacityBar.setValue(0);
            capacityBar.setString("Upload a cover image first");
            capacityLabel.setText("N/A");
            return;
        }
        int totalPixels = selectedImage.getWidth() * selectedImage.getHeight();

        if (isFileMode()) {
            if (selectedSecretFile == null) {
                capacityBar.setValue(0);
                capacityBar.setString("Browse a file to check capacity");
                capacityLabel.setText("Total pixels: " + totalPixels);
                return;
            }
            long fileSize     = selectedSecretFile.length();
            long base64Size   = (fileSize * 4 / 3) + 100;
            int  pixelsNeeded = (int)(base64Size * 8 / 3);
            int  pct = Math.min(100,
                    (int)((pixelsNeeded / (double) totalPixels) * 100));
            capacityBar.setValue(pct);
            capacityBar.setString(pct + "% of image capacity used");
            capacityLabel.setText(formatSize(fileSize)
                    + " → needs ~" + pixelsNeeded + " / " + totalPixels + " px");
            updateBarColour(pct);
        } else {
            String msg  = messageTextArea.getText();
            charCountLabel.setText(msg.length() + " characters");
            if (msg.isEmpty()) {
                capacityBar.setValue(0);
                capacityBar.setString("Type a message to see capacity");
                capacityLabel.setText("Total pixels: " + totalPixels);
                return;
            }
            int pixelsNeeded = PixelScatter.calculateBitsNeeded(msg);
            int pct = Math.min(100,
                    (int)((pixelsNeeded / (double) totalPixels) * 100));
            capacityBar.setValue(pct);
            capacityBar.setString(pct + "% of image capacity used");
            capacityLabel.setText("Pixels: " + pixelsNeeded + " / " + totalPixels);
            updateBarColour(pct);
        }
    }

    private void updateBarColour(int pct) {
        if (pct < 50)      capacityBar.setForeground(new Color(34, 139, 34));
        else if (pct < 80) capacityBar.setForeground(new Color(255, 165, 0));
        else               capacityBar.setForeground(new Color(220, 20, 60));
    }

    private void handleEncode() {
        if (selectedImage == null) { showError("Please upload a cover image!"); return; }
        String password = new String(passwordField.getPassword()).trim();
        if (password.isEmpty())        { showError("Please enter a password!"); return; }
        if (password.length() < 4)     { showError("Password must be at least 4 characters!"); return; }
        if (isFileMode()) encodeFile(password);
        else              encodeText(password);
    }

    private void encodeText(String password) {
        String message = messageTextArea.getText().trim();
        if (message.isEmpty()) { showError("Please enter a message!"); return; }

        encodeButton.setEnabled(false);
        encodeButton.setText("Encoding...");

        SwingWorker<BufferedImage, Void> worker = new SwingWorker<>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                return ImageSteganography.encode(selectedImage, message, password);
            }
            @Override
            protected void done() {
                encodeButton.setText("🔒 Encode & Hide");
                encodeButton.setEnabled(true);
                try {
                    encodedImage = get();
                    saveButton.setEnabled(true);
                    HistoryDAO.saveRecord(currentUser.getId(), "ENCODE",
                            selectedImageFile != null ? selectedImageFile.getName() : "",
                            selectedImageFile != null ? selectedImageFile.length() : 0,
                            "TEXT", "");
                    showSuccess("✅ Text encoded! Click Save Image.");
                } catch (Exception ex) {
                    showError("Encoding failed:\n" + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void encodeFile(String password) {
        if (selectedSecretFile == null) {
            showError("Please select a file to hide!");
            return;
        }
        encodeButton.setEnabled(false);
        encodeButton.setText("Encoding file...");

        SwingWorker<BufferedImage, Void> worker = new SwingWorker<>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                byte[] bytes   = Files.readAllBytes(selectedSecretFile.toPath());
                String b64     = Base64.getEncoder().encodeToString(bytes);
                String payload = FILE_PREFIX + selectedSecretFile.getName() + ":" + b64;
                return ImageSteganography.encode(selectedImage, payload, password);
            }
            @Override
            protected void done() {
                encodeButton.setText("🔒 Encode & Hide");
                encodeButton.setEnabled(true);
                try {
                    encodedImage = get();
                    saveButton.setEnabled(true);
                    HistoryDAO.saveRecord(currentUser.getId(), "ENCODE",
                            selectedImageFile != null ? selectedImageFile.getName() : "",
                            selectedImageFile != null ? selectedImageFile.length() : 0,
                            "FILE", selectedSecretFile.getName());
                    showSuccess("✅ File hidden!\n"
                            + "File: " + selectedSecretFile.getName() + "\n"
                            + "Click Save Image.");
                } catch (Exception ex) {
                    showError("Encoding failed:\n" + ex.getMessage()
                            + "\n\nTip: Use a larger cover image!");
                }
            }
        };
        worker.execute();
    }

    private void handleSave() {
        if (encodedImage == null) { showError("Please encode first!"); return; }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Encoded Image");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG Image", "png"));
        fc.setSelectedFile(new File("encoded_image.png"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File out = fc.getSelectedFile();
        if (!out.getName().toLowerCase().endsWith(".png"))
            out = new File(out.getAbsolutePath() + ".png");
        try {
            ImageIO.write(encodedImage, "PNG", out);
            showSuccess("✅ Saved!\n" + out.getAbsolutePath()
                    + "\n\nAlways keep as PNG!");
        } catch (Exception ex) {
            showError("Error saving:\n" + ex.getMessage());
        }
    }

    private boolean isFileMode() { return modeFile != null && modeFile.isSelected(); }

    private String formatSize(long bytes) {
        if (bytes < 1024)        return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private void styleToggle(JToggleButton btn, boolean active) {
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBackground(active ? new Color(34, 100, 34) : new Color(200, 200, 215));
        btn.setForeground(active ? Color.WHITE : Color.DARK_GRAY);
    }

    private JButton makeBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Arial", Font.BOLD, 13));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccess(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
}