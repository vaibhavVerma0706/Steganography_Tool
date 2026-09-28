package Panels;

import db.HistoryDAO;
import engine.ImageSteganography;
import model.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;

public class DecodePanel extends JPanel {

    private final User currentUser;

    private JLabel         imagePreviewLabel;
    private JLabel         imageInfoLabel;
    private JPasswordField passwordField;
    private JTextArea      outputTextArea;
    private JButton        decodeButton;
    private JButton        saveFileButton;
    private JLabel         resultTypeLabel;
    private BufferedImage  selectedImage;
    private File           selectedImageFile;

    private byte[] decodedFileBytes;
    private String decodedFileName;

    private static final String FILE_PREFIX = "FILE:";

    // ──────────────────────────────────────────────────────
    public DecodePanel(User user) {
        this.currentUser = user;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(new Color(245, 245, 250));
        add(buildTopSection(),    BorderLayout.NORTH);
        add(buildMiddleSection(), BorderLayout.CENTER);
        add(buildBottomSection(), BorderLayout.SOUTH);
    }

    private JPanel buildTopSection() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("Step 1 — Upload Encoded Image"));

        imagePreviewLabel = new JLabel("No Image Selected", SwingConstants.CENTER);
        imagePreviewLabel.setPreferredSize(new Dimension(200, 150));
        imagePreviewLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        imagePreviewLabel.setOpaque(true);
        imagePreviewLabel.setBackground(Color.WHITE);
        imagePreviewLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        imagePreviewLabel.setForeground(Color.GRAY);

        imageInfoLabel = new JLabel("Upload the encoded PNG image to begin");
        imageInfoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        imageInfoLabel.setForeground(Color.GRAY);

        JLabel hint = new JLabel("Supports PNG images encoded with this tool");
        hint.setFont(new Font("Arial", Font.ITALIC, 11));
        hint.setForeground(new Color(180, 100, 0));

        JButton uploadBtn = makeButton("📁 Upload Encoded Image", new Color(70, 130, 180));
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
        JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));
        panel.setBackground(new Color(245, 245, 250));

        // Password panel
        JPanel passwordPanel = new JPanel(new BorderLayout(5, 5));
        passwordPanel.setBackground(new Color(245, 245, 250));
        passwordPanel.setBorder(new TitledBorder("Step 2 — Enter Password"));

        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordField.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel passwordHint = new JLabel("Enter the same password used when encoding");
        passwordHint.setFont(new Font("Arial", Font.ITALIC, 11));
        passwordHint.setForeground(new Color(180, 100, 0));

        passwordPanel.add(passwordField, BorderLayout.CENTER);
        passwordPanel.add(passwordHint,  BorderLayout.SOUTH);

        // Output panel
        JPanel outputPanel = new JPanel(new BorderLayout(5, 5));
        outputPanel.setBackground(new Color(245, 245, 250));
        outputPanel.setBorder(new TitledBorder("Step 3 — Decoded Result"));

        resultTypeLabel = new JLabel("Awaiting decode...");
        resultTypeLabel.setFont(new Font("Arial", Font.BOLD, 11));
        resultTypeLabel.setForeground(Color.GRAY);
        resultTypeLabel.setBorder(new EmptyBorder(2, 4, 2, 4));

        outputTextArea = new JTextArea(4, 40);
        outputTextArea.setFont(new Font("Arial", Font.PLAIN, 13));
        outputTextArea.setLineWrap(true);
        outputTextArea.setWrapStyleWord(true);
        outputTextArea.setEditable(false);
        outputTextArea.setBackground(new Color(240, 255, 240));
        outputTextArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        outputTextArea.setText("Decoded result will appear here...");
        outputTextArea.setForeground(Color.GRAY);

        saveFileButton = makeButton("💾 Save Extracted File", new Color(34, 139, 34));
        saveFileButton.setEnabled(false);
        saveFileButton.addActionListener(e -> handleSaveFile());

        JButton copyBtn  = makeButton("📋 Copy",   new Color(100, 149, 237));
        JButton clearBtn = makeButton("🗑️ Clear",  new Color(169, 169, 169));
        copyBtn.addActionListener(e  -> handleCopy());
        clearBtn.addActionListener(e -> handleClear());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        btnRow.setBackground(new Color(245, 245, 250));
        btnRow.add(clearBtn);
        btnRow.add(copyBtn);
        btnRow.add(saveFileButton);

        JPanel outputTop = new JPanel(new BorderLayout());
        outputTop.setBackground(new Color(245, 245, 250));
        outputTop.add(resultTypeLabel, BorderLayout.CENTER);

        outputPanel.add(outputTop,                       BorderLayout.NORTH);
        outputPanel.add(new JScrollPane(outputTextArea), BorderLayout.CENTER);
        outputPanel.add(btnRow,                          BorderLayout.SOUTH);

        panel.add(passwordPanel);
        panel.add(outputPanel);
        return panel;
    }

    private JPanel buildBottomSection() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panel.setBackground(new Color(245, 245, 250));

        decodeButton = makeButton("🔓 Decode", new Color(34, 139, 34));
        decodeButton.setFont(new Font("Arial", Font.BOLD, 14));
        decodeButton.setPreferredSize(new Dimension(200, 40));
        decodeButton.addActionListener(e -> handleDecode());

        panel.add(decodeButton);
        return panel;
    }

    // ══════════════════════════════════════════════════════
    private void handleImageUpload() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Encoded Image");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image Files (PNG, BMP, JPG)", "png", "bmp", "jpg", "jpeg"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        try {
            BufferedImage loaded = ImageIO.read(file);
            if (loaded == null) { showError("Cannot read image."); return; }

            selectedImage     = toRGB(loaded);
            selectedImageFile = file;

            imagePreviewLabel.setIcon(new ImageIcon(
                    selectedImage.getScaledInstance(200, 150, Image.SCALE_SMOOTH)));
            imagePreviewLabel.setText("");
            imageInfoLabel.setText(file.getName() + "  |  "
                    + selectedImage.getWidth() + " × "
                    + selectedImage.getHeight() + " px");
            resetOutput();
        } catch (Exception ex) {
            showError("Error loading: " + ex.getMessage());
        }
    }

    private void handleDecode() {
        if (selectedImage == null) { showError("Please upload an image first!"); return; }
        String password = new String(passwordField.getPassword()).trim();
        if (password.isEmpty())    { showError("Please enter the password!"); return; }
        if (password.length() < 4) { showError("Password must be at least 4 characters!"); return; }

        decodeButton.setEnabled(false);
        decodeButton.setText("Decoding...");
        resetOutput();

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return ImageSteganography.decode(selectedImage, password);
            }
            @Override
            protected void done() {
                decodeButton.setText("🔓 Decode");
                decodeButton.setEnabled(true);
                try {
                    processDecodedResult(get());
                } catch (Exception ex) {
                    String msg = ex.getMessage();
                    if (msg != null && msg.contains("No hidden message")) {
                        showError("No hidden message found!\n\n"
                                + "Possible reasons:\n"
                                + "  1. Wrong password\n"
                                + "  2. Image has no hidden data\n"
                                + "  3. Image was converted to JPEG");
                    } else {
                        showError("Decoding failed:\n" + msg);
                    }
                    outputTextArea.setText("Decoding failed. Please try again.");
                    outputTextArea.setForeground(Color.RED);
                    resultTypeLabel.setText("❌ Decode failed");
                    resultTypeLabel.setForeground(Color.RED);
                }
            }
        };
        worker.execute();
    }

    private void processDecodedResult(String raw) {
        if (raw == null || raw.isEmpty()) {
            outputTextArea.setText("(empty result)");
            resultTypeLabel.setText("⚠️ Empty result");
            return;
        }

        if (raw.startsWith(FILE_PREFIX)) {
            processHiddenFile(raw);
        } else {
            processHiddenText(raw);
        }
    }

    private void processHiddenText(String text) {
        decodedFileBytes = null;
        decodedFileName  = null;
        saveFileButton.setEnabled(false);

        outputTextArea.setText(text);
        outputTextArea.setForeground(new Color(0, 100, 0));
        resultTypeLabel.setText("💬 Text message decoded successfully!");
        resultTypeLabel.setForeground(new Color(34, 139, 34));

        // Save to history
        HistoryDAO.saveRecord(currentUser.getId(), "DECODE",
                selectedImageFile != null ? selectedImageFile.getName() : "",
                selectedImageFile != null ? selectedImageFile.length() : 0,
                "TEXT", "");

        showSuccess("✅ Text message decoded successfully!");
    }

    private void processHiddenFile(String raw) {
        String withoutPrefix = raw.substring(FILE_PREFIX.length());
        int    sep           = withoutPrefix.indexOf(':');
        if (sep < 0) {
            outputTextArea.setText(raw);
            resultTypeLabel.setText("⚠️ Could not parse file data");
            return;
        }

        decodedFileName   = withoutPrefix.substring(0, sep);
        String base64Data = withoutPrefix.substring(sep + 1);

        try {
            decodedFileBytes = Base64.getDecoder().decode(base64Data);

            outputTextArea.setText(
                    "📦 HIDDEN FILE EXTRACTED!\n\n"
                            + "Filename : " + decodedFileName + "\n"
                            + "Size     : " + formatSize(decodedFileBytes.length) + "\n\n"
                            + "Click '💾 Save Extracted File' to save it.");
            outputTextArea.setForeground(new Color(0, 100, 0));

            resultTypeLabel.setText(
                    "📦 Hidden file: " + decodedFileName
                            + " (" + formatSize(decodedFileBytes.length) + ")");
            resultTypeLabel.setForeground(new Color(34, 100, 200));

            saveFileButton.setEnabled(true);
            saveFileButton.setText("💾 Save: " + decodedFileName);

            // Save to history
            HistoryDAO.saveRecord(currentUser.getId(), "DECODE",
                    selectedImageFile != null ? selectedImageFile.getName() : "",
                    selectedImageFile != null ? selectedImageFile.length() : 0,
                    "FILE", decodedFileName);

            showSuccess("✅ Hidden file extracted!\n\n"
                    + "File: " + decodedFileName + "\n"
                    + "Size: " + formatSize(decodedFileBytes.length) + "\n\n"
                    + "Click 'Save Extracted File' to save it!");

        } catch (IllegalArgumentException ex) {
            outputTextArea.setText("❌ Failed to decode file data.\n"
                    + "Wrong password or corrupted image.");
            outputTextArea.setForeground(Color.RED);
            resultTypeLabel.setText("❌ File decode failed");
        }
    }

    private void handleSaveFile() {
        if (decodedFileBytes == null || decodedFileName == null) {
            showError("No extracted file to save!");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Extracted File");
        fc.setSelectedFile(new File(decodedFileName));
        fc.setAcceptAllFileFilterUsed(true);
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try (FileOutputStream fos = new FileOutputStream(fc.getSelectedFile())) {
            fos.write(decodedFileBytes);
            showSuccess("✅ Saved: " + fc.getSelectedFile().getAbsolutePath());
        } catch (Exception ex) {
            showError("Error saving: " + ex.getMessage());
        }
    }

    private void handleCopy() {
        String text = outputTextArea.getText();
        if (text.isEmpty() || text.equals("Decoded result will appear here...")) {
            showError("No decoded message to copy!");
            return;
        }
        outputTextArea.selectAll();
        outputTextArea.copy();
        outputTextArea.select(0, 0);
        showSuccess("Copied to clipboard!");
    }

    private void handleClear() {
        selectedImage     = null;
        selectedImageFile = null;
        imagePreviewLabel.setIcon(null);
        imagePreviewLabel.setText("No Image Selected");
        imageInfoLabel.setText("Upload the encoded PNG image to begin");
        passwordField.setText("");
        resetOutput();
    }

    private void resetOutput() {
        decodedFileBytes = null;
        decodedFileName  = null;
        saveFileButton.setEnabled(false);
        saveFileButton.setText("💾 Save Extracted File");
        outputTextArea.setText("Decoded result will appear here...");
        outputTextArea.setForeground(Color.GRAY);
        resultTypeLabel.setText("Awaiting decode...");
        resultTypeLabel.setForeground(Color.GRAY);
    }

    private BufferedImage toRGB(BufferedImage image) {
        BufferedImage rgb = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.drawImage(image, 0, 0, Color.WHITE, null);
        g.dispose();
        return rgb;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024)        return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private JButton makeButton(String text, Color bg) {
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