package Panels;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SteghidePanel extends JPanel {

    // ── Fields ──
    private File          loadedFile;
    private BufferedImage loadedImage;

    // UI
    private JPanel    imageDisplayPanel;
    private BufferedImage imageDisplayImage;
    private JLabel    imageInfoLabel;
    private JTextPane resultPane;
    private JLabel    statusLabel;
    private JTextField passwordField;

    // Decoded data for save
    private byte[] decodedBytes;
    private String decodedFileName;

    // ── Known CTF flag patterns ──
    private static final String[] FLAG_PATTERNS = {
            "flag\\{[^}]+\\}", "FLAG\\{[^}]+\\}",
            "CTF\\{[^}]+\\}",  "ctf\\{[^}]+\\}",
            "HTB\\{[^}]+\\}",  "htb\\{[^}]+\\}",
            "THM\\{[^}]+\\}",  "thm\\{[^}]+\\}",
            "picoCTF\\{[^}]+\\}", "PICO\\{[^}]+\\}",
            "KEY\\{[^}]+\\}",
    };

    // ── Common CTF passwords to try ──
    private static final String[] COMMON_PASSWORDS = {
            "",             // empty — most common in CTF!
            "password",
            "secret",
            "admin",
            "12345",
            "steganography",
            "stego",
            "hidden",
            "flag",
            "ctf",
            "hack",
            "letmein",
            "abc123",
            "qwerty",
            "root",
            "user",
            "guest",
            "test",
            "pass",
            "1234",
    };

    // ──────────────────────────────────────────────────────
    // Constructor
    // ──────────────────────────────────────────────────────
    public SteghidePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(new Color(245, 245, 250));

        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildMainArea(),  BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ──────────────────────────────────────────────────────
    // TOP BAR
    // ──────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel panel = new JPanel(new BorderLayout(10, 8));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder(
                "Steghide Decoder — Extract data hidden with Steghide tool"));

        // Image thumbnail
        imageDisplayPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFit(g, imageDisplayImage, getWidth(), getHeight());
            }
        };
        imageDisplayPanel.setBackground(Color.WHITE);
        imageDisplayPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        imageDisplayPanel.setPreferredSize(new Dimension(160, 100));

        imageInfoLabel = new JLabel(
                "Load a JPEG or BMP image encoded with Steghide");
        imageInfoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        imageInfoLabel.setForeground(Color.GRAY);

        JLabel desc = new JLabel("""
            <html>
            <b>💡 What is Steghide?</b> A popular CTF tool that hides data in JPEG/BMP
            images using graph theory. It requires a password to extract.<br><br>
            <b>This tab tries:</b> Empty password, common CTF passwords, or your custom
            password — and extracts the hidden file/message if found.
            </html>""");
        desc.setFont(new Font("Arial", Font.PLAIN, 11));
        desc.setForeground(new Color(60, 60, 100));

        // Button row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRow.setBackground(new Color(245, 245, 250));

        JButton loadBtn      = makeBtn("📁 Load Image",            new Color(70, 130, 180));
        JButton emptyPassBtn = makeBtn("🔓 Try Empty Password",    new Color(34, 139, 34));
        JButton commonBtn    = makeBtn("🔑 Try Common Passwords",  new Color(210, 105, 30));
        JButton saveBtn      = makeBtn("💾 Save Extracted File",   new Color(100, 60, 160));
        JButton clearBtn     = makeBtn("🗑️ Clear",                 new Color(160, 60, 60));

        loadBtn.addActionListener(e      -> handleLoad());
        emptyPassBtn.addActionListener(e -> tryEmptyPassword());
        commonBtn.addActionListener(e    -> tryCommonPasswords());
        saveBtn.addActionListener(e      -> saveExtracted());
        clearBtn.addActionListener(e     -> clearAll());

        btnRow.add(loadBtn);
        btnRow.add(emptyPassBtn);
        btnRow.add(commonBtn);
        btnRow.add(saveBtn);
        btnRow.add(clearBtn);

        // Custom password row
        JPanel passRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        passRow.setBackground(new Color(245, 245, 250));
        passRow.add(makeBoldLabel("Custom password:"));
        passwordField = new JTextField(18);
        passwordField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        passwordField.setToolTipText("Enter a specific password to try");
        passRow.add(passwordField);

        JButton customBtn = makeBtn("🔐 Try This Password", new Color(34, 100, 180));
        customBtn.addActionListener(e -> tryCustomPassword());
        passRow.add(customBtn);

        JPanel infoPanel = new JPanel(new BorderLayout(5, 5));
        infoPanel.setBackground(new Color(245, 245, 250));
        infoPanel.add(imageInfoLabel, BorderLayout.NORTH);
        infoPanel.add(desc,           BorderLayout.CENTER);
        infoPanel.add(btnRow,         BorderLayout.SOUTH);

        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        topPanel.setBackground(new Color(245, 245, 250));
        topPanel.add(imageDisplayPanel, BorderLayout.WEST);
        topPanel.add(infoPanel,         BorderLayout.CENTER);
        topPanel.add(passRow,           BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.CENTER);
        return panel;
    }

    // ──────────────────────────────────────────────────────
    // MAIN AREA — result pane
    // ──────────────────────────────────────────────────────
    private JPanel buildMainArea() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("Results"));

        resultPane = new JTextPane();
        resultPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        resultPane.setEditable(false);
        resultPane.setBackground(new Color(15, 15, 15));
        resultPane.setForeground(new Color(0, 255, 100));
        resultPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        showWelcome();

        panel.add(new JScrollPane(resultPane), BorderLayout.CENTER);
        return panel;
    }

    // ──────────────────────────────────────────────────────
    // STATUS BAR
    // ──────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 30, 60));
        panel.setBorder(new EmptyBorder(4, 10, 4, 10));

        statusLabel = new JLabel("Ready — Load a JPEG or BMP image");
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(180, 220, 180));

        JLabel hint = new JLabel(
                "Tip: In CTF challenges the steghide password is often empty or the challenge name");
        hint.setFont(new Font("Arial", Font.ITALIC, 10));
        hint.setForeground(new Color(180, 180, 220));

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(hint,        BorderLayout.EAST);
        return panel;
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Load image
    // ══════════════════════════════════════════════════════
    private void handleLoad() {
        File file = openFileChooser();
        if (file == null) return;
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            loadedFile = file;

            BufferedImage raw = ImageIO.read(file);
            if (raw != null) {
                loadedImage = new BufferedImage(
                        raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = loadedImage.createGraphics();
                g2.drawImage(raw, 0, 0, Color.WHITE, null);
                g2.dispose();
                imageDisplayImage = loadedImage;
                imageDisplayPanel.repaint();
            }

            imageInfoLabel.setText(
                    file.getName() + "  |  " + bytes.length + " bytes  |  "
                            + detectFormat(bytes));

            clearPane(resultPane);
            appendText(resultPane,
                    "✅ Loaded: " + file.getName() + "\n" +
                            "   Size  : " + bytes.length + " bytes\n" +
                            "   Format: " + detectFormat(bytes) + "\n\n" +
                            "Click a decode button above to attempt extraction.\n",
                    new Color(0, 255, 100));

            statusLabel.setText("Loaded: " + file.getName());
            decodedBytes    = null;
            decodedFileName = null;

        } catch (Exception ex) {
            showError("Error loading: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Try empty password (most common CTF case)
    // ══════════════════════════════════════════════════════
    private void tryEmptyPassword() {
        if (loadedFile == null) { showError("Load an image first!"); return; }

        clearPane(resultPane);
        appendHeader("TRYING EMPTY PASSWORD");
        appendText(resultPane,
                "Empty password is the most common in CTF steganography!\n\n",
                new Color(100, 180, 255));

        SteghideResult result = attemptSteghideDecode(loadedFile, "");
        displayResult(result, "");
        statusLabel.setText("Empty password attempt complete.");
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Try common CTF passwords
    // ══════════════════════════════════════════════════════
    private void tryCommonPasswords() {
        if (loadedFile == null) { showError("Load an image first!"); return; }

        clearPane(resultPane);
        appendHeader("TRYING " + COMMON_PASSWORDS.length + " COMMON PASSWORDS");
        appendText(resultPane,
                "Testing common CTF passwords...\n\n",
                new Color(100, 180, 255));

        boolean found = false;
        for (String pwd : COMMON_PASSWORDS) {
            String display = pwd.isEmpty() ? "(empty)" : "'" + pwd + "'";
            SteghideResult result = attemptSteghideDecode(loadedFile, pwd);

            if (result.success) {
                found = true;
                appendText(resultPane,
                        "🚩 SUCCESS with password: " + display + "\n\n",
                        Color.YELLOW);
                displayResult(result, pwd);
                break;
            } else {
                appendText(resultPane,
                        "❌ " + display + " — " + result.errorMessage + "\n",
                        new Color(120, 120, 120));
            }
        }

        if (!found) {
            appendText(resultPane,
                    """
                            
                            ── All common passwords failed ──
                            
                            Suggestions:
                              • Try the challenge name as password
                              • Try the image filename (without extension)
                              • Check EXIF data for password hints
                              • The image may not use steghide
                            """,
                    new Color(255, 165, 0));
        }
        statusLabel.setText("Common password test complete.");
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Try custom password entered by user
    // ══════════════════════════════════════════════════════
    private void tryCustomPassword() {
        if (loadedFile == null) { showError("Load an image first!"); return; }

        String pwd = passwordField.getText();
        clearPane(resultPane);
        appendHeader("TRYING CUSTOM PASSWORD");
        appendText(resultPane,
                "Password: '" + (pwd.isEmpty() ? "(empty)" : pwd) + "'\n\n",
                new Color(100, 180, 255));

        SteghideResult result = attemptSteghideDecode(loadedFile, pwd);
        displayResult(result, pwd);
        statusLabel.setText("Custom password attempt complete.");
    }

    // ══════════════════════════════════════════════════════
    // CORE: Steghide decode simulation
    //
    // ⚠️  IMPORTANT NOTE:
    // Pure Java cannot run the actual steghide binary directly.
    // This implementation:
    //   1. Checks if 'steghide' is installed on the system
    //   2. If yes  → runs it as a process and captures output
    //   3. If no   → performs statistical LSB analysis to
    //               detect steghide-encoded images and guide user
    // ══════════════════════════════════════════════════════
    private SteghideResult attemptSteghideDecode(File imageFile, String password) {
        // First check if steghide binary is available
        if (isSteghideAvailable()) {
            return runSteghideProcess(imageFile, password);
        } else {
            return simulateSteghideAnalysis(imageFile, password);
        }
    }

    // ── Check if steghide is installed ──
    private boolean isSteghideAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("steghide", "--version");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            proc.waitFor();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    // ── Run actual steghide process (when installed) ──
    private SteghideResult runSteghideProcess(File imageFile, String password) {
        try {
            // Create temp output file
            File tempOut = File.createTempFile("steghide_out_", ".bin");
            tempOut.deleteOnExit();

            // Build steghide command
            ProcessBuilder pb = new ProcessBuilder(
                    "steghide", "extract",
                    "-sf", imageFile.getAbsolutePath(),
                    "-p",  password,
                    "-f",
                    "-xf", tempOut.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            String output = new String(proc.getInputStream().readAllBytes());
            int    exit   = proc.waitFor();

            if (exit == 0 && tempOut.exists() && tempOut.length() > 0) {
                byte[] extracted = Files.readAllBytes(tempOut.toPath());
                return new SteghideResult(true, extracted,
                        "steghide_extracted.bin", output, "");
            } else {
                return new SteghideResult(false, null, null,
                        output, "Wrong password or no data");
            }
        } catch (Exception ex) {
            return new SteghideResult(false, null, null, "",
                    "Process error: " + ex.getMessage());
        }
    }

    // ── Simulation when steghide not installed ──
    // Performs LSB analysis to detect steghide signature
    private SteghideResult simulateSteghideAnalysis(File imageFile, String password) {
        try {
            byte[] fileBytes = Files.readAllBytes(imageFile.toPath());

            // Steghide detection: analyse pixel pair distribution (chi-square)
            BufferedImage img = loadedImage;
            if (img == null) {
                return new SteghideResult(false, null, null, "",
                        "Could not read image pixels");
            }

            // Count pixel value pair equalisation (steghide signature)
            int[] hist = new int[256];
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int rgb = img.getRGB(x, y);
                    hist[(rgb >> 16) & 0xFF]++;  // red channel
                }
            }

            double pairScore = calculatePairScore(hist);
            boolean likelySteghide = pairScore > 0.80;

            if (likelySteghide) {
                return new SteghideResult(false, null, null,
                        "DETECTED: Statistical analysis suggests steghide encoding!\n" +
                                "Pair equalisation score: " + String.format("%.2f", pairScore * 100) + "%\n",
                        "steghide_not_installed");
            } else {
                return new SteghideResult(false, null, null,
                        "No steghide signature detected (pair score: "
                                + String.format("%.2f", pairScore * 100) + "%)",
                        "no_steghide");
            }
        } catch (Exception ex) {
            return new SteghideResult(false, null, null, "",
                    ex.getMessage());
        }
    }

    private double calculatePairScore(int[] hist) {
        double score = 0;
        int count = 0;
        for (int i = 0; i < 255; i += 2) {
            int sum = hist[i] + hist[i + 1];
            if (sum == 0) continue;
            double diff = Math.abs(hist[i] - hist[i + 1]) / (double) sum;
            score += (1.0 - diff);
            count++;
        }
        return count > 0 ? score / count : 0;
    }

    // ══════════════════════════════════════════════════════
    // DISPLAY: Show decode result
    // ══════════════════════════════════════════════════════
    private void displayResult(SteghideResult result, String password) {
        if (result.success) {
            // ── Successfully extracted ──
            appendText(resultPane,
                    "✅ EXTRACTION SUCCESSFUL!\n" +
                            "Password used: '" + (password.isEmpty() ? "(empty)" : password) + "'\n" +
                            "Extracted: " + result.dataBytes.length + " bytes\n\n",
                    new Color(0, 255, 100));

            decodedBytes    = result.dataBytes;
            decodedFileName = result.fileName;

            String text = toAscii(result.dataBytes);
            if (countPrintable(result.dataBytes) > result.dataBytes.length * 0.7) {
                appendText(resultPane, "── Content ──\n", new Color(100, 200, 255));
                if (containsFlag(text)) {
                    appendText(resultPane, "🚩 FLAG FOUND!\n\n", Color.YELLOW);
                    highlightFlags(text);
                } else {
                    appendText(resultPane, text + "\n", new Color(0, 255, 100));
                }
            } else {
                appendText(resultPane,
                        "Binary data extracted.\n" +
                                "First bytes: " + toHexPreview(result.dataBytes, 32) + "\n" +
                                "Click 'Save Extracted File' to save it.\n",
                        new Color(0, 200, 150));
            }

        } else if (result.errorMessage.equals("steghide_not_installed")) {
            // ── Steghide not installed — show detection result ──
            appendText(resultPane,
                    "⚠️  Steghide is NOT installed on this system.\n\n",
                    Color.YELLOW);
            appendText(resultPane, result.processOutput, new Color(100, 200, 255));
            appendText(resultPane, """

                    ── How to use Steghide ──
                    Steghide must be installed separately:

                    Windows:
                      Download from: https://steghide.sourceforge.net
                      Run: steghide extract -sf image.jpg -p ""

                    Linux:
                      sudo apt install steghide
                      steghide extract -sf image.jpg -p ""

                    Mac:
                      brew install steghide
                      steghide extract -sf image.jpg -p ""

                    Common commands:
                      Empty password : steghide extract -sf image.jpg -p ""
                      With password  : steghide extract -sf image.jpg -p "password"
                      Check info     : steghide info image.jpg

                    """, new Color(180, 220, 180));

        } else if (result.errorMessage.equals("no_steghide")) {
            appendText(resultPane,
                    "✅ Statistical analysis complete.\n\n" +
                            result.processOutput + "\n\n" +
                            "This image does NOT appear to be steghide-encoded.\n" +
                            "Try the LSB Decoder tab for plain LSB steganography.\n",
                    new Color(150, 150, 150));

        } else {
            appendText(resultPane,
                    "❌ Extraction failed.\n" +
                            "Reason: " + result.errorMessage + "\n",
                    new Color(200, 80, 80));
            if (!result.processOutput.isEmpty()) {
                appendText(resultPane,
                        "Output: " + result.processOutput + "\n",
                        new Color(150, 150, 150));
            }
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Save extracted bytes to file
    // ══════════════════════════════════════════════════════
    private void saveExtracted() {
        if (decodedBytes == null) {
            showError("No extracted data to save. Run a decode first!");
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Extracted File");
        fc.setSelectedFile(new File(
                decodedFileName != null ? decodedFileName : "steghide_extracted.bin"));

        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try (FileOutputStream fos = new FileOutputStream(fc.getSelectedFile())) {
            fos.write(decodedBytes);
            JOptionPane.showMessageDialog(this,
                    "Saved: " + fc.getSelectedFile().getName(),
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
            statusLabel.setText("Saved: " + fc.getSelectedFile().getName());
        } catch (Exception ex) {
            showError("Error saving: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Clear all
    // ══════════════════════════════════════════════════════
    private void clearAll() {
        loadedFile         = null;
        loadedImage        = null;
        imageDisplayImage  = null;
        decodedBytes       = null;
        decodedFileName    = null;
        imageDisplayPanel.repaint();
        imageInfoLabel.setText("Load a JPEG or BMP image encoded with Steghide");
        passwordField.setText("");
        clearPane(resultPane);
        showWelcome();
        statusLabel.setText("Cleared.");
    }

    // ══════════════════════════════════════════════════════
    // INNER CLASS: Steghide result
    // ══════════════════════════════════════════════════════
    private static class SteghideResult {
        final boolean success;
        final byte[]  dataBytes;
        final String  fileName;
        final String  processOutput;
        final String  errorMessage;

        SteghideResult(boolean success, byte[] dataBytes, String fileName,
                       String processOutput, String errorMessage) {
            this.success       = success;
            this.dataBytes     = dataBytes;
            this.fileName      = fileName;
            this.processOutput = processOutput;
            this.errorMessage  = errorMessage;
        }
    }

    // ══════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════
    private String detectFormat(byte[] data) {
        if (data.length < 4) return "Unknown";
        if (data[0] == (byte)0xFF && data[1] == (byte)0xD8) return "JPEG";
        if (data[0] == (byte)0x89 && data[1] == 0x50)       return "PNG";
        if (data[0] == 0x42 && data[1] == 0x4D)             return "BMP";
        if (data[0] == 0x47 && data[1] == 0x49)             return "GIF";
        return "Unknown";
    }

    private String toAscii(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            char c = (char)(b & 0xFF);
            sb.append((c >= 32 && c < 127) ? c : '.');
        }
        return sb.toString();
    }

    private int countPrintable(byte[] bytes) {
        int count = 0;
        for (byte b : bytes) {
            char c = (char)(b & 0xFF);
            if (c >= 32 && c < 127) count++;
        }
        return count;
    }

    private boolean containsFlag(String text) {
        for (String pat : FLAG_PATTERNS) {
            if (Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(text).find())
                return true;
        }
        return false;
    }

    private void highlightFlags(String text) {
        List<int[]> ranges = new ArrayList<>();
        for (String pat : FLAG_PATTERNS) {
            Matcher m = Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(text);
            while (m.find()) ranges.add(new int[]{m.start(), m.end()});
        }
        ranges.sort((a, b) -> a[0] - b[0]);
        int pos = 0;
        for (int[] range : ranges) {
            if (range[0] > pos)
                appendText(resultPane, text.substring(pos, range[0]),
                        new Color(0, 255, 100));
            appendText(resultPane, text.substring(range[0], range[1]), Color.YELLOW);
            pos = range[1];
        }
        if (pos < text.length())
            appendText(resultPane, text.substring(pos), new Color(0, 255, 100));
        appendText(resultPane, "\n", Color.WHITE);
    }

    private String toHexPreview(byte[] data, int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(data.length, max); i++)
            sb.append(String.format("%02X ", data[i]));
        return sb.toString().trim();
    }

    // ══════════════════════════════════════════════════════
    // FILE CHOOSER
    // ══════════════════════════════════════════════════════
    private File openFileChooser() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load Image for Steghide Analysis");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image Files (JPEG, BMP, PNG)",
                "jpg", "jpeg", "bmp", "png"));
        return fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
                ? fc.getSelectedFile() : null;
    }

    // ══════════════════════════════════════════════════════
    // DISPLAY HELPERS
    // ══════════════════════════════════════════════════════
    private void showWelcome() {
        appendText(resultPane, """
                ╔══════════════════════════════════════════════════╗
                ║           STEGHIDE DECODER                      ║
                ╚══════════════════════════════════════════════════╝

                WHAT IS STEGHIDE?
                  A popular tool that hides files inside JPEG/BMP images.
                  Uses graph theory — much harder to detect than plain LSB.
                  Always requires a password (can be empty "").

                HOW TO USE:
                  1. Load a JPEG or BMP image
                  2. Click 'Try Empty Password' first
                     (most CTF challenges use empty password!)
                  3. If that fails → click 'Try Common Passwords'
                     (tests 20 common CTF passwords automatically)
                  4. If you know the password → enter it and click
                     'Try This Password'

                IF STEGHIDE IS INSTALLED:
                  This tool runs steghide directly and extracts the file.
                  Results shown here, save with 'Save Extracted File'.

                IF STEGHIDE IS NOT INSTALLED:
                  Statistical analysis is performed to detect IF steghide
                  was used, plus full instructions to install and use it.
                """, new Color(0, 200, 80));
    }

    private void appendText(JTextPane pane, String text, Color colour) {
        StyledDocument doc   = pane.getStyledDocument();
        Style          style = pane.addStyle("s", null);
        StyleConstants.setForeground(style, colour);
        StyleConstants.setFontFamily(style, "Monospaced");
        StyleConstants.setFontSize(style, 12);
        try { doc.insertString(doc.getLength(), text, style); }
        catch (BadLocationException ignored) {}
    }

    private void appendHeader(String title) {
        String line = "═".repeat(50) + "\n";
        appendText(resultPane, line,                 new Color(60, 120, 60));
        appendText(resultPane, "  " + title + "\n",  Color.WHITE);
        appendText(resultPane, line + "\n",          new Color(60, 120, 60));
    }

    private void clearPane(JTextPane pane) { pane.setText(""); }

    private void drawFit(Graphics g, BufferedImage img, int pw, int ph) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        if (img != null) {
            double scale = Math.min((double) pw / img.getWidth(),
                    (double) ph / img.getHeight());
            int dw = (int)(img.getWidth()  * scale);
            int dh = (int)(img.getHeight() * scale);
            g2.drawImage(img, (pw - dw) / 2, (ph - dh) / 2, dw, dh, null);
        } else {
            g2.setColor(Color.GRAY);
            g2.setFont(new Font("Arial", Font.ITALIC, 11));
            String hint = "No Image Loaded";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(hint,
                    Math.max(0, (pw - fm.stringWidth(hint)) / 2),
                    (ph + fm.getAscent()) / 2);
        }
    }

    private JLabel makeBoldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", Font.BOLD, 12));
        return l;
    }

    private JButton makeBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Arial", Font.BOLD, 11));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}