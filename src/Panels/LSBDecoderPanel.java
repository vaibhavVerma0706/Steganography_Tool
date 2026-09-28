package Panels;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LSBDecoderPanel extends JPanel {

    // ── Fields ──
    private BufferedImage loadedImage;

    // Image display
    private JPanel        imageDisplayPanel;
    private BufferedImage imageDisplayImage;
    private JLabel        imageInfoLabel;

    // Controls
    private JComboBox<String> channelBox;
    private JComboBox<String> bitOrderBox;
    private JComboBox<String> readOrderBox;
    private JComboBox<String> outputFormatBox;
    private JSpinner          bitsSpinner;

    // Output
    private JTextPane resultPane;
    private JLabel    statsLabel;

    // ── Known CTF flag patterns ──
    private static final String[] FLAG_PATTERNS = {
            "flag\\{[^}]+\\}", "FLAG\\{[^}]+\\}",
            "CTF\\{[^}]+\\}",  "ctf\\{[^}]+\\}",
            "HTB\\{[^}]+\\}",  "htb\\{[^}]+\\}",
            "THM\\{[^}]+\\}",  "thm\\{[^}]+\\}",
            "picoCTF\\{[^}]+\\}", "PICO\\{[^}]+\\}",
            "KEY\\{[^}]+\\}",  "SECRET\\{[^}]+\\}",
    };

    // ── OpenStego magic marker (first 2 bytes of OpenStego header) ──
    private static final int OPENSTEGO_SIG_1 = 0x4F;  // 'O'
    private static final int OPENSTEGO_SIG_2 = 0x53;  // 'S'

    // ──────────────────────────────────────────────────────
    // Constructor
    // ──────────────────────────────────────────────────────
    public LSBDecoderPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(new Color(245, 245, 250));
        add(buildTopBar(),   BorderLayout.NORTH);
        add(buildMainArea(), BorderLayout.CENTER);
    }

    // ──────────────────────────────────────────────────────
    // TOP BAR — image loader
    // ──────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("Load Image for LSB Decoding"));

        imageDisplayPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFit(g, imageDisplayImage, getWidth(), getHeight());
            }
        };
        imageDisplayPanel.setBackground(Color.WHITE);
        imageDisplayPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        imageDisplayPanel.setPreferredSize(new Dimension(180, 110));

        imageInfoLabel = new JLabel(
                "Load an image (PNG, BMP, JPG, GIF, TIFF) to begin LSB decoding");
        imageInfoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        imageInfoLabel.setForeground(Color.GRAY);

        JLabel hint = new JLabel("""
                <html><b>💡 What this does:</b> Reads the Least Significant Bit of each pixel channel<br>
                and assembles them into bytes to recover hidden messages.</html>""");
        hint.setFont(new Font("Arial", Font.PLAIN, 11));
        hint.setForeground(new Color(80, 80, 120));

        JButton loadBtn = makeBlueBtn("📁 Load Image");
        loadBtn.addActionListener(e -> handleLoad());

        JPanel right = new JPanel(new BorderLayout(5, 5));
        right.setBackground(new Color(245, 245, 250));
        right.add(imageInfoLabel, BorderLayout.NORTH);
        right.add(hint,           BorderLayout.CENTER);
        right.add(loadBtn,        BorderLayout.SOUTH);

        panel.add(imageDisplayPanel, BorderLayout.WEST);
        panel.add(right,             BorderLayout.CENTER);
        return panel;
    }

    // ──────────────────────────────────────────────────────
    // MAIN AREA — controls left, output right
    // ──────────────────────────────────────────────────────
    private JPanel buildMainArea() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(new Color(245, 245, 250));
        panel.add(buildControlsPanel(), BorderLayout.WEST);
        panel.add(buildOutputPanel(),   BorderLayout.CENTER);
        return panel;
    }

    // ──────────────────────────────────────────────────────
    // CONTROLS PANEL — left side
    // ──────────────────────────────────────────────────────
    private JPanel buildControlsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("Decoder Settings"));
        panel.setPreferredSize(new Dimension(230, 0));

        // Channel
        panel.add(makeLabel("📡 Channel to Read:"));
        channelBox = new JComboBox<>(new String[]{
                "All RGB (R→G→B)", "Red Only", "Green Only", "Blue Only"
        });
        styleCombo(channelBox);
        panel.add(channelBox);
        panel.add(makeHint("Which colour channel holds the hidden data"));
        panel.add(Box.createVerticalStrut(10));

        // Bit order
        panel.add(makeLabel("🔢 Bit Order:"));
        bitOrderBox = new JComboBox<>(new String[]{
                "LSB First (bit 0→7)", "MSB First (bit 7→0)"
        });
        styleCombo(bitOrderBox);
        panel.add(bitOrderBox);
        panel.add(makeHint("Order bits are assembled into bytes"));
        panel.add(Box.createVerticalStrut(10));

        // Reading order
        panel.add(makeLabel("📖 Reading Order:"));
        readOrderBox = new JComboBox<>(new String[]{
                "Row by Row (L→R, T→B)", "Column by Column (T→B, L→R)"
        });
        styleCombo(readOrderBox);
        panel.add(readOrderBox);
        panel.add(makeHint("Direction pixels are read across image"));
        panel.add(Box.createVerticalStrut(10));

        // Output format
        panel.add(makeLabel("📄 Output Format:"));
        outputFormatBox = new JComboBox<>(new String[]{
                "Text (ASCII)", "Hexadecimal", "Binary", "All Formats"
        });
        styleCombo(outputFormatBox);
        panel.add(outputFormatBox);
        panel.add(makeHint("How to display the decoded bits"));
        panel.add(Box.createVerticalStrut(10));

        // Bits to read
        panel.add(makeLabel("🔢 Bits to Read:"));
        bitsSpinner = new JSpinner(new SpinnerNumberModel(256, 8, 100000, 8));
        bitsSpinner.setFont(new Font("Monospaced", Font.PLAIN, 12));
        bitsSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        panel.add(bitsSpinner);
        panel.add(makeHint("Total bits to extract (8 bits = 1 char)"));
        panel.add(Box.createVerticalStrut(14));

        // ── Standard decode buttons ──
        panel.add(makeSectionDivider("── Standard Decode ──"));
        panel.add(Box.createVerticalStrut(4));

        JButton decodeBtn = makeGreenBtn("🔓 Decode LSB");
        decodeBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        decodeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        decodeBtn.addActionListener(e -> runDecode());
        panel.add(decodeBtn);
        panel.add(Box.createVerticalStrut(6));

        JButton tryAllBtn = makeOrangeBtn("🔄 Try All Combinations");
        tryAllBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        tryAllBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        tryAllBtn.setToolTipText("Tests all channel + bit order combos");
        tryAllBtn.addActionListener(e -> runTryAll());
        panel.add(tryAllBtn);
        panel.add(Box.createVerticalStrut(10));

        // ── Basic Universal Decoder ──
        panel.add(makeSectionDivider("── Basic Decode (No Password) ──"));
        panel.add(Box.createVerticalStrut(4));

        JButton basicBtn = makeColorBtn("🌐 Basic Decode (CTF / No Password)",
                new Color(0, 110, 180));
        basicBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        basicBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        basicBtn.setToolTipText(
                "Decodes plain LSB with NO password or encryption. "
                        + "Works on CTF images made with zsteg, stegsolve, online tools etc.");
        basicBtn.addActionListener(e -> runBasicDecode());
        panel.add(basicBtn);
        panel.add(makeHint("No password needed — for CTF plain LSB images"));
        panel.add(Box.createVerticalStrut(10));

        // ── NEW: XOR brute-force ──
        panel.add(makeSectionDivider("── XOR Brute-Force ──"));
        panel.add(Box.createVerticalStrut(4));

        JButton xorBtn = makePurpleBtn("🔑 XOR Brute-Force (0-255)");
        xorBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        xorBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        xorBtn.setToolTipText(
                "Extracts LSB then XORs with every key 0-255. Finds XOR-obfuscated flags.");
        xorBtn.addActionListener(e -> runXORBruteForce());
        panel.add(xorBtn);
        panel.add(makeHint("For XOR-obfuscated hidden data"));
        panel.add(Box.createVerticalStrut(10));

        // ── NEW: OpenStego parser ──
        panel.add(makeSectionDivider("── OpenStego ──"));
        panel.add(Box.createVerticalStrut(4));

        JButton openStegoBtn = makeCyanBtn("📂 Parse OpenStego Header");
        openStegoBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        openStegoBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        openStegoBtn.setToolTipText(
                "Detects and strips OpenStego header to extract hidden file/message");
        openStegoBtn.addActionListener(e -> runOpenStegoParser());
        panel.add(openStegoBtn);
        panel.add(makeHint("For images encoded with OpenStego tool"));
        panel.add(Box.createVerticalStrut(10));

        // ── Clear ──
        JButton clearBtn = makeRedBtn("🗑️ Clear Output");
        clearBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        clearBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        clearBtn.addActionListener(e -> clearPane(resultPane));
        panel.add(clearBtn);
        panel.add(Box.createVerticalGlue());

        panel.add(buildQuickRef());
        return panel;
    }

    // ── Quick reference box ──
    private JPanel buildQuickRef() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(230, 240, 255));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 150, 200)),
                new EmptyBorder(6, 6, 6, 6)));

        JLabel title = new JLabel("💡 Quick Guide:");
        title.setFont(new Font("Arial", Font.BOLD, 10));
        title.setForeground(new Color(50, 50, 150));
        p.add(title);

        for (String line : new String[]{
                "• CTF image (no pwd) → Basic Decode",
                "• Plain LSB → Try All Combos",
                "• XOR encoded → XOR Brute-Force",
                "• OpenStego image → Parse OpenStego",
                "• Steghide → use Steghide tab",
                "• 🚩 = flag pattern found!"
        }) {
            JLabel l = new JLabel(line);
            l.setFont(new Font("Arial", Font.PLAIN, 10));
            l.setForeground(new Color(50, 50, 120));
            p.add(l);
        }
        return p;
    }

    // ──────────────────────────────────────────────────────
    // OUTPUT PANEL — right side
    // ──────────────────────────────────────────────────────
    private JPanel buildOutputPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("Decoded Output"));

        statsLabel = new JLabel("  Ready — load an image and click Decode");
        statsLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statsLabel.setForeground(new Color(80, 80, 80));
        statsLabel.setBorder(new EmptyBorder(4, 4, 4, 4));

        resultPane = new JTextPane();
        resultPane.setFont(new Font("Monospaced", Font.PLAIN, 13));
        resultPane.setEditable(false);
        resultPane.setBackground(new Color(15, 15, 15));
        resultPane.setForeground(new Color(0, 255, 100));
        resultPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        appendText(resultPane, """
                ╔══════════════════════════════════════════════════╗
                ║           LSB BIT DECODER                       ║
                ╚══════════════════════════════════════════════════╝

                FOUR DECODE MODES:

                1. 🌐 Basic Decode (CTF / No Password)   ← START HERE
                   → For ANY plain LSB CTF image
                   → No password or encryption needed
                   → Tries all 16 combinations automatically
                   → Detects flags, file types, readable text
                   → Works on: zsteg, stegsolve, online tools

                2. 🔓 Decode LSB / Try All Combinations
                   → Manual channel/order control
                   → More detailed output per combination

                3. 🔑 XOR Brute-Force (0-255)
                   → For XOR-obfuscated steganography

                4. 📂 Parse OpenStego Header
                   → For images encoded with OpenStego tool

                🚩 Flag patterns always highlighted in YELLOW
                """, new Color(0, 200, 80));

        JScrollPane scroll = new JScrollPane(resultPane);
        scroll.setBackground(new Color(15, 15, 15));

        JButton saveBtn = makeBlueBtn("💾 Save Output");
        saveBtn.addActionListener(e -> saveOutput());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(new Color(245, 245, 250));
        bottom.add(saveBtn);

        panel.add(statsLabel, BorderLayout.NORTH);
        panel.add(scroll,     BorderLayout.CENTER);
        panel.add(bottom,     BorderLayout.SOUTH);
        return panel;
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Load Image
    // ══════════════════════════════════════════════════════
    private void handleLoad() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load Image for LSB Decoding");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image Files (PNG, BMP, JPG, JPEG, GIF, TIFF)",
                "png", "bmp", "jpg", "jpeg", "gif", "tiff"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        try {
            BufferedImage raw = ImageIO.read(file);
            if (raw == null) throw new Exception("Cannot read image.");

            loadedImage = new BufferedImage(
                    raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = loadedImage.createGraphics();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, raw.getWidth(), raw.getHeight());
            g2.drawImage(raw, 0, 0, null);
            g2.dispose();

            imageDisplayImage = loadedImage;
            imageDisplayPanel.repaint();

            long maxBits = (long) loadedImage.getWidth() * loadedImage.getHeight() * 3;
            imageInfoLabel.setText(
                    file.getName() + "  |  "
                            + loadedImage.getWidth() + " × " + loadedImage.getHeight()
                            + "  |  Max: " + (maxBits / 8) + " bytes");

            bitsSpinner.setValue((int) Math.min(8000, maxBits));
            statsLabel.setText("  ✅ Loaded — capacity: " + (maxBits / 8) + " bytes max");

            clearPane(resultPane);
            appendText(resultPane,
                    "✅ Loaded: " + file.getName() + "\n" +
                            "   Size  : " + loadedImage.getWidth() + " × "
                            + loadedImage.getHeight() + "\n" +
                            "   Max   : " + (maxBits / 8) + " bytes hidden capacity\n\n" +
                            "Choose a decode method on the left and click the button.\n",
                    new Color(0, 255, 100));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Standard LSB Decode
    // ══════════════════════════════════════════════════════
    private void runDecode() {
        if (loadedImage == null) { showErr("Load an image first!"); return; }

        String channel   = (String) channelBox.getSelectedItem();
        String bitOrder  = (String) bitOrderBox.getSelectedItem();
        String readOrder = (String) readOrderBox.getSelectedItem();
        String format    = (String) outputFormatBox.getSelectedItem();
        int    bits      = (int) bitsSpinner.getValue();

        if (channel   == null) channel   = "All RGB (R→G→B)";
        if (bitOrder  == null) bitOrder  = "LSB First (bit 0→7)";
        if (readOrder == null) readOrder = "Row by Row (L→R, T→B)";
        if (format    == null) format    = "Text (ASCII)";

        clearPane(resultPane);
        appendSectionHeader(resultPane, "LSB DECODE");
        appendText(resultPane,
                "Channel: " + channel + "  |  Bit Order: " + bitOrder + "\n" +
                        "Read: " + readOrder + "  |  Format: " + format + "\n\n",
                new Color(100, 180, 255));

        boolean[] bitsArr = extractBits(channel, readOrder, bits);
        byte[]    bytes   = bitsToBytes(bitsArr, bitOrder.startsWith("MSB"));

        switch (format) {
            case "All Formats"  -> { showAsText(bytes); showAsHex(bytes); showAsBinary(bitsArr); }
            case "Hexadecimal"  -> showAsHex(bytes);
            case "Binary"       -> showAsBinary(bitsArr);
            default             -> showAsText(bytes);
        }

        statsLabel.setText("  ✅ Decoded " + bitsArr.length + " bits → "
                + bytes.length + " bytes  |  " + countPrintable(bytes) + " printable");
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Try All Combinations
    // ══════════════════════════════════════════════════════
    private void runTryAll() {
        if (loadedImage == null) { showErr("Load an image first!"); return; }

        int bits = (int) bitsSpinner.getValue();
        clearPane(resultPane);
        appendSectionHeader(resultPane, "TRY ALL COMBINATIONS");
        appendText(resultPane, """
                Testing every channel + bit order + reading direction...
                Sorted by most printable characters first.

                """, new Color(100, 180, 255));

        String[] channels   = {"All RGB (R→G→B)", "Red Only", "Green Only", "Blue Only"};
        String[] bitOrders  = {"LSB First (bit 0→7)", "MSB First (bit 7→0)"};
        String[] readOrders = {"Row by Row (L→R, T→B)", "Column by Column (T→B, L→R)"};

        List<String[]> results = new ArrayList<>();
        for (String ch : channels) {
            for (String bo : bitOrders) {
                for (String ro : readOrders) {
                    boolean[] bArr  = extractBits(ch, ro, bits);
                    byte[]    bytes = bitsToBytes(bArr, bo.startsWith("MSB"));
                    String    text  = toAscii(bytes);
                    int       score = countPrintable(bytes);
                    boolean   flag  = containsFlag(text);
                    results.add(new String[]{ch, bo, ro, text,
                            String.valueOf(score), String.valueOf(flag)});
                }
            }
        }

        results.sort(Comparator.comparingInt(
                (String[] a) -> Integer.parseInt(a[4])).reversed());

        int rank = 1;
        for (String[] r : results) {
            boolean hasFlag = Boolean.parseBoolean(r[5]);
            int     score   = Integer.parseInt(r[4]);
            String  combo   = r[0] + " | " + r[1] + " | " + r[2];
            String  preview = r[3].length() > 80 ? r[3].substring(0, 80) + "..." : r[3];

            Color headerCol = hasFlag    ? Color.YELLOW
                    : score > 50 ? new Color(0, 255, 100)
                    : new Color(120, 120, 120);

            appendText(resultPane,
                    "─── #" + rank + (hasFlag ? " 🚩 FLAG FOUND!" : "") + " ───\n",
                    headerCol);
            appendText(resultPane,
                    "Settings : " + combo + "\n", new Color(180, 180, 255));
            appendText(resultPane,
                    "Readable : " + score + " / " + (bits / 8) + " chars\n",
                    new Color(150, 150, 150));

            if (hasFlag) highlightFlags(r[3]);
            else appendText(resultPane, "Preview  : " + preview + "\n",
                    new Color(200, 200, 200));
            appendText(resultPane, "\n", Color.WHITE);
            rank++;
        }

        long flagCount = results.stream()
                .filter(r -> Boolean.parseBoolean(r[5])).count();
        statsLabel.setText("  ✅ Tested " + results.size()
                + " combinations — " + flagCount + " flags found");
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Basic Universal Decode — NO password, NO encryption
    // ══════════════════════════════════════════════════════
    // How it works:
    //   1. Reads LSB of each R, G, B channel pixel by pixel
    //   2. Groups bits into bytes
    //   3. Converts bytes directly to ASCII text (no decryption)
    //   4. Detects stop condition: null byte OR all non-printable
    //   5. Detects file signatures (ZIP, PDF, PNG etc.)
    //   6. Checks for CTF flag patterns
    //   7. Tries all 16 channel/order combinations automatically
    //   8. Shows the best result + all others ranked by readability
    // ══════════════════════════════════════════════════════
    private void runBasicDecode() {
        if (loadedImage == null) { showErr("Load an image first!"); return; }

        clearPane(resultPane);
        appendSectionHeader(resultPane, "BASIC DECODE — NO PASSWORD / NO ENCRYPTION");
        appendText(resultPane,
                "This decoder reads raw LSB pixel bits directly as text.\n" +
                        "No password or decryption needed.\n" +
                        "Works on: zsteg, stegsolve, online CTF tools, plain LSB tools.\n\n",
                new Color(100, 180, 255));

        // ── Step 1: Try the full image capacity ──
        int maxBits = loadedImage.getWidth() * loadedImage.getHeight() * 3;

        // ── Step 2: Try all 16 combinations ──
        String[] channels   = {"All RGB (R→G→B)", "Red Only", "Green Only", "Blue Only"};
        String[] bitOrders  = {"LSB First (bit 0→7)", "MSB First (bit 7→0)"};
        String[] readOrders = {"Row by Row (L→R, T→B)", "Column by Column (T→B, L→R)"};

        // Store results: [channel, bitOrder, readOrder, text, score, hasFlag, fileType]
        List<String[]> results = new ArrayList<>();

        for (String ch : channels) {
            for (String bo : bitOrders) {
                for (String ro : readOrders) {

                    // Extract raw bits
                    boolean[] bitsArr = extractBits(ch, ro, maxBits);

                    // Convert to bytes — NO decryption
                    byte[] bytes = bitsToBytes(bitsArr, bo.startsWith("MSB"));

                    // ── Smart stop: find where meaningful data ends ──
                    byte[] trimmed = trimToStop(bytes);

                    // ── Detect what type of data this is ──
                    String fileType   = detectFileType(trimmed);
                    String text       = toAscii(trimmed);
                    int    score      = countPrintable(trimmed);
                    boolean hasFlag   = containsFlag(text);

                    results.add(new String[]{
                            ch, bo, ro, text,
                            String.valueOf(score),
                            String.valueOf(hasFlag),
                            fileType,
                            // Store raw bytes length for display
                            String.valueOf(trimmed.length)
                    });
                }
            }
        }

        // ── Step 3: Sort — flags first, then by printable score ──
        results.sort((a, b) -> {
            boolean flagA = Boolean.parseBoolean(a[5]);
            boolean flagB = Boolean.parseBoolean(b[5]);
            if (flagA != flagB) return flagB ? 1 : -1;
            return Integer.parseInt(b[4]) - Integer.parseInt(a[4]);
        });

        // ── Step 4: Display results ──
        boolean anyFlagFound = false;
        int shown = 0;

        for (String[] r : results) {
            boolean hasFlag  = Boolean.parseBoolean(r[5]);
            int     score    = Integer.parseInt(r[4]);
            String  fileType = r[6];
            int     dataLen  = Integer.parseInt(r[7]);
            String  combo    = r[0] + " | " + r[1] + " | " + r[2];

            // Skip completely unreadable results (less than 15% printable)
            // Unless it has a known file signature
            if (score < dataLen * 0.15 && fileType.equals("Unknown") && !hasFlag) {
                continue;
            }

            if (hasFlag) anyFlagFound = true;

            Color headerCol = hasFlag           ? Color.YELLOW
                    : !fileType.equals("Unknown")   ? new Color(255, 165, 0)
                    : score > dataLen * 0.6         ? new Color(0, 255, 100)
                    : new Color(120, 120, 120);

            // Header line
            appendText(resultPane,
                    "─── #" + (shown + 1)
                            + (hasFlag ? " 🚩 FLAG FOUND!" : "")
                            + (!fileType.equals("Unknown") ? " 📦 " + fileType + " DETECTED!" : "")
                            + " ───\n",
                    headerCol);

            // Settings
            appendText(resultPane,
                    "Settings  : " + combo + "\n",
                    new Color(150, 150, 200));

            // Readability score
            appendText(resultPane,
                    "Readable  : " + score + " / " + dataLen + " bytes"
                            + "  (" + String.format("%.0f", score * 100.0 / Math.max(dataLen, 1)) + "%)\n",
                    new Color(150, 150, 150));

            // File type if detected
            if (!fileType.equals("Unknown")) {
                appendText(resultPane,
                        "File type : " + fileType + "\n",
                        new Color(255, 165, 0));
            }

            // Content
            appendText(resultPane, "Content   : ", new Color(100, 200, 255));
            if (hasFlag) {
                appendText(resultPane, "\n", Color.WHITE);
                highlightFlags(r[3]);
            } else if (!fileType.equals("Unknown")) {
                appendText(resultPane,
                        "[Binary " + fileType + " data — use Embedded File Scanner to extract]\n",
                        new Color(255, 165, 0));
            } else {
                String preview = r[3].length() > 120
                        ? r[3].substring(0, 120) + "..." : r[3];
                appendText(resultPane, preview + "\n", new Color(0, 255, 100));
            }
            appendText(resultPane, "\n", Color.WHITE);
            shown++;

            // Stop after top 5 results — avoid flooding
            if (shown >= 5) break;
        }

        // ── Step 5: Summary ──
        if (shown == 0) {
            appendText(resultPane,
                    "❌ No readable data found in any combination.\n\n" +
                            "This means the image either:\n" +
                            "  • Has no hidden data\n" +
                            "  • Uses encryption (try your decoder with password)\n" +
                            "  • Uses steghide (try Steghide tab)\n" +
                            "  • Uses a non-LSB method\n",
                    new Color(255, 165, 0));
        } else if (!anyFlagFound) {
            appendText(resultPane,
                    "── No flag pattern found automatically ──\n" +
                            "💡 Tips:\n" +
                            "  • Look at the readable text above manually\n" +
                            "  • The flag might use a custom format\n" +
                            "  • Try 'Try All Combinations' for more detail\n" +
                            "  • Try XOR Brute-Force if text looks scrambled\n",
                    new Color(180, 180, 100));
        }

        long flagCount = results.stream()
                .filter(r -> Boolean.parseBoolean(r[5])).count();
        statsLabel.setText("  Basic Decode complete — showed "
                + shown + " readable results — "
                + flagCount + " flag pattern(s) found");
    }

    // ══════════════════════════════════════════════════════
    // HELPER: Trim byte array to where meaningful data stops
    // Stops at: null byte sequence OR all non-printable run
    // ══════════════════════════════════════════════════════
    private byte[] trimToStop(byte[] data) {
        // Limit to first 4000 bytes for performance
        int limit = Math.min(data.length, 4000);

        // Find first long run of null bytes (5+ consecutive nulls = end of data)
        int stopAt = limit;
        int nullRun = 0;
        for (int i = 0; i < limit; i++) {
            if (data[i] == 0) {
                nullRun++;
                if (nullRun >= 5) {
                    stopAt = i - nullRun + 1;
                    break;
                }
            } else {
                nullRun = 0;
            }
        }

        // Also stop at first long run of non-printable non-null bytes
        // (suggests we hit encrypted/binary data boundary)
        int nonPrintRun = 0;
        int altStop     = stopAt;
        for (int i = 0; i < stopAt; i++) {
            char c = (char)(data[i] & 0xFF);
            if ((c < 32 || c >= 127) && data[i] != 0 && data[i] != '\n'
                    && data[i] != '\r' && data[i] != '\t') {
                nonPrintRun++;
                if (nonPrintRun >= 20) {
                    altStop = Math.max(0, i - 20);
                    break;
                }
            } else {
                nonPrintRun = 0;
            }
        }

        // Use the earlier stop point only if it gives us some data
        int finalStop = (altStop > 8) ? Math.min(stopAt, altStop) : stopAt;
        finalStop = Math.max(finalStop, Math.min(32, limit)); // always return at least 32 bytes

        byte[] result = new byte[finalStop];
        System.arraycopy(data, 0, result, 0, finalStop);
        return result;
    }

    // ══════════════════════════════════════════════════════
    // HELPER: Detect file type from first few bytes (magic bytes)
    // ══════════════════════════════════════════════════════
    private String detectFileType(byte[] data) {
        if (data.length < 4) return "Unknown";

        int b0 = data[0] & 0xFF;
        int b1 = data[1] & 0xFF;
        int b2 = data[2] & 0xFF;
        int b3 = data[3] & 0xFF;

        // ZIP
        if (b0 == 0x50 && b1 == 0x4B) return "ZIP Archive";
        // PDF
        if (b0 == 0x25 && b1 == 0x50 && b2 == 0x44 && b3 == 0x46) return "PDF Document";
        // PNG
        if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) return "PNG Image";
        // JPEG
        if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) return "JPEG Image";
        // GIF
        if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46) return "GIF Image";
        // RAR
        if (b0 == 0x52 && b1 == 0x61 && b2 == 0x72 && b3 == 0x21) return "RAR Archive";
        // 7-Zip
        if (b0 == 0x37 && b1 == 0x7A && b2 == 0xBC && b3 == 0xAF) return "7-Zip Archive";
        // ELF (Linux binary)
        if (b0 == 0x7F && b1 == 0x45 && b2 == 0x4C && b3 == 0x46) return "ELF Executable";
        // MP3
        if (b0 == 0x49 && b1 == 0x44 && b2 == 0x33) return "MP3 Audio";
        // BMP
        if (b0 == 0x42 && b1 == 0x4D) return "BMP Image";

        return "Unknown";
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: XOR Brute-Force
    // What it does:
    //   1. Extract LSB bits using current settings
    //   2. Convert to bytes
    //   3. XOR every byte with key 0, then key 1... up to key 255
    //   4. For each key, check if result contains readable text or flag
    //   5. Show all keys that produce readable output
    // ══════════════════════════════════════════════════════
    private void runXORBruteForce() {
        if (loadedImage == null) { showErr("Load an image first!"); return; }

        String channel   = (String) channelBox.getSelectedItem();
        String bitOrder  = (String) bitOrderBox.getSelectedItem();
        String readOrder = (String) readOrderBox.getSelectedItem();
        int    bits      = (int) bitsSpinner.getValue();

        if (channel   == null) channel   = "All RGB (R→G→B)";
        if (bitOrder  == null) bitOrder  = "LSB First (bit 0→7)";
        if (readOrder == null) readOrder = "Row by Row (L→R, T→B)";

        clearPane(resultPane);
        appendSectionHeader(resultPane, "XOR BRUTE-FORCE (Keys 0-255)");
        appendText(resultPane,
                """
                HOW THIS WORKS:
                  Extracts LSB data, then XORs with every possible single-byte key (0-255).
                  If data was XOR-obfuscated before hiding, one key will produce readable text.
                  Shows keys that produce the most printable characters.
    
                """, new Color(100, 180, 255));

        // Step 1: Extract raw LSB bytes
        boolean[] bitsArr  = extractBits(channel, readOrder, bits);
        byte[]    rawBytes = bitsToBytes(bitsArr, bitOrder.startsWith("MSB"));

        appendText(resultPane,
                "Extracted " + rawBytes.length + " bytes from image.\n" +
                        "Testing XOR keys 0-255...\n\n",
                new Color(150, 150, 150));

        // Step 2: Try each XOR key
        List<int[]> scores = new ArrayList<>(); // [key, printableCount, hasFlag]
        for (int key = 0; key <= 255; key++) {
            byte[] xored       = xorBytes(rawBytes, (byte) key);
            String text        = toAscii(xored);
            int    printable   = countPrintable(xored);
            boolean hasFlag    = containsFlag(text);
            scores.add(new int[]{key, printable, hasFlag ? 1 : 0});
        }

        // Step 3: Sort by printable count + flag hits first
        scores.sort((a, b) -> {
            if (b[2] != a[2]) return b[2] - a[2]; // flags first
            return b[1] - a[1];                    // then by printable count
        });

        // Step 4: Show top 20 results
        int shown = 0;
        for (int[] entry : scores) {
            int     key      = entry[0];
            int     readable = entry[1];
            boolean hasFlag  = entry[2] == 1;

            // Skip completely unreadable results (less than 20% printable)
            if (readable < rawBytes.length * 0.2 && !hasFlag) continue;

            byte[] xored = xorBytes(rawBytes, (byte) key);
            String text  = toAscii(xored);

            Color  col     = hasFlag ? Color.YELLOW : new Color(0, 200, 80);
            String preview = text.length() > 100 ? text.substring(0, 100) + "..." : text;

            appendText(resultPane,
                    "─── Key: 0x" + String.format("%02X", key)
                            + " (" + key + ")"
                            + (hasFlag ? " 🚩 FLAG FOUND!" : "") + " ───\n", col);
            appendText(resultPane,
                    "Readable: " + readable + "/" + rawBytes.length + " chars\n",
                    new Color(150, 150, 150));

            if (hasFlag) {
                highlightFlags(text);
            } else {
                appendText(resultPane, "Result: " + preview + "\n",
                        new Color(0, 200, 80));
            }
            appendText(resultPane, "\n", Color.WHITE);

            shown++;
            if (shown >= 20) {
                appendText(resultPane,
                        "... (showing top 20 readable results)\n",
                        new Color(150, 150, 150));
                break;
            }
        }

        if (shown == 0) {
            appendText(resultPane,
                    "No readable output found with any XOR key.\n\n" +
                            "Suggestions:\n" +
                            "  • Try a different channel (Red/Green/Blue Only)\n" +
                            "  • Try MSB First bit order\n" +
                            "  • The data may use multi-byte XOR key\n" +
                            "  • The data may not be XOR-encoded\n",
                    new Color(255, 165, 0));
        }

        long flagCount = scores.stream().filter(e -> e[2] == 1).count();
        statsLabel.setText("  XOR brute-force done — "
                + shown + " readable keys found, " + flagCount + " flag(s)");
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: OpenStego Header Parser
    // OpenStego format (version 2):
    //   Byte 0-1 : Magic "OS" (0x4F 0x53)
    //   Byte 2   : Version (usually 2)
    //   Byte 3   : Channel bits used (1, 2, or 3)
    //   Byte 4   : Flags (bit0=encrypted, bit1=compressed)
    //   Byte 5   : Filename length (0 = no filename)
    //   Byte 6.. : Filename bytes (if length > 0)
    //   Remaining: Actual hidden data
    // ══════════════════════════════════════════════════════
    private void runOpenStegoParser() {
        if (loadedImage == null) { showErr("Load an image first!"); return; }

        clearPane(resultPane);
        appendSectionHeader(resultPane, "OPENSTEGO HEADER PARSER");
        appendText(resultPane, """
                OpenStego is a popular steganography tool.
                This parser detects its header format and extracts hidden data.
                Works when no password was used during encoding.

                """, new Color(100, 180, 255));

        // Try different bit counts (OpenStego uses 1, 2, or 3 bits per channel)
        int[] bitsPerChannel = {1, 2, 3};
        boolean detected = false;

        for (int bpc : bitsPerChannel) {
            int    totalBits  = loadedImage.getWidth() * loadedImage.getHeight() * 3 * bpc;
            byte[] extracted  = extractBitsAsBytes("All RGB (R→G→B)",
                    "Row by Row (L→R, T→B)", totalBits, bpc);

            if (extracted.length < 6) continue;

            // Check for OpenStego magic bytes "OS"
            if ((extracted[0] & 0xFF) == OPENSTEGO_SIG_1
                    && (extracted[1] & 0xFF) == OPENSTEGO_SIG_2) {

                detected = true;
                appendText(resultPane,
                        "✅ OpenStego header DETECTED! (using " + bpc + " bit(s) per channel)\n\n",
                        new Color(0, 255, 100));

                parseOpenStegoHeader(extracted, bpc);
                break;
            }
        }

        if (!detected) {
            // Try standard LSB extraction and check for OS marker
            boolean[] bits   = extractBits("All RGB (R→G→B)",
                    "Row by Row (L→R, T→B)", 200 * 8);
            byte[]    bytes  = bitsToBytes(bits, false);

            if (bytes.length >= 2
                    && (bytes[0] & 0xFF) == OPENSTEGO_SIG_1
                    && (bytes[1] & 0xFF) == OPENSTEGO_SIG_2) {

                detected = true;
                appendText(resultPane,
                        "✅ OpenStego header detected in standard LSB!\n\n",
                        new Color(0, 255, 100));
                parseOpenStegoHeader(bytes, 1);
            }
        }

        if (!detected) {
            appendText(resultPane,
                    "❌ No OpenStego header found.\n\n" +
                            "This image was NOT encoded with OpenStego, or:\n" +
                            "  • A password was used (data is encrypted — cannot decode)\n" +
                            "  • The image uses a different steganography tool\n\n" +
                            "Try these instead:\n" +
                            "  → 'Try All Combinations' for plain LSB\n" +
                            "  → 'XOR Brute-Force' for XOR-obfuscated data\n" +
                            "  → 'Steghide' tab for steghide-encoded images\n",
                    new Color(255, 165, 0));
        }
    }

    // ── Parse and display OpenStego header fields ──
    private void parseOpenStegoHeader(byte[] data, int bitsPerChannel) {
        int version     =  data[2] & 0xFF;
        int channelBits =  data[3] & 0xFF;
        int flags       =  data[4] & 0xFF;
        int filenameLen =  data[5] & 0xFF;

        boolean isEncrypted  = (flags & 0x01) != 0;
        boolean isCompressed = (flags & 0x02) != 0;

        appendText(resultPane, "── Header Fields ──\n", new Color(100, 200, 255));
        appendText(resultPane,
                "Magic      : OS (0x4F 0x53) ✅\n" +
                        "Version    : " + version + "\n" +
                        "Bits/chan  : " + channelBits + "\n" +
                        "Encrypted  : " + (isEncrypted  ? "YES ⚠️ Password required!" : "NO ✅") + "\n" +
                        "Compressed : " + (isCompressed ? "YES (zlib)" : "NO") + "\n" +
                        "Filename L : " + filenameLen + " bytes\n",
                new Color(0, 255, 100));

        if (isEncrypted) {
            appendText(resultPane,
                    "\n⚠️ Data is AES encrypted.\n" +
                            "Cannot decode without the password.\n" +
                            "Try common passwords: '', 'password', 'secret', 'admin'\n" +
                            "Use the actual OpenStego application with the password.\n",
                    Color.YELLOW);
            return;
        }

        // Extract filename
        int dataStart = 6 + filenameLen;
        if (filenameLen > 0 && data.length >= dataStart) {
            String filename = new String(data, 6, filenameLen);
            appendText(resultPane, "Filename   : " + filename + "\n",
                    new Color(0, 255, 100));

            // Try to save the extracted data
            if (data.length > dataStart) {
                byte[] hiddenData = new byte[data.length - dataStart];
                System.arraycopy(data, dataStart, hiddenData, 0, hiddenData.length);

                appendText(resultPane,
                        "\n── Extracted Data ──\n", new Color(100, 200, 255));
                appendText(resultPane,
                        "Size: " + hiddenData.length + " bytes\n",
                        new Color(0, 255, 100));

                // Show as text if printable
                String text = toAscii(hiddenData);
                if (countPrintable(hiddenData) > hiddenData.length * 0.7) {
                    appendText(resultPane, "Content (text):\n", new Color(100, 200, 255));
                    if (containsFlag(text)) {
                        highlightFlags(text);
                    } else {
                        String preview = text.length() > 500
                                ? text.substring(0, 500) + "..." : text;
                        appendText(resultPane, preview + "\n", new Color(0, 255, 100));
                    }
                } else {
                    // Binary data — offer save
                    appendText(resultPane,
                            "Binary data detected.\n" +
                                    "First 32 bytes (hex): " + toHexPreview(hiddenData, 32) + "\n\n",
                            new Color(0, 200, 150));
                    offerSave(hiddenData, filename);
                }
            }
        } else {
            // No filename — show raw data after header
            if (data.length > 6) {
                byte[] hiddenData = new byte[data.length - 6];
                System.arraycopy(data, 6, hiddenData, 0, hiddenData.length);

                String text = toAscii(hiddenData);
                appendText(resultPane,
                        "\n── Extracted Data (no filename) ──\n",
                        new Color(100, 200, 255));
                if (containsFlag(text)) {
                    highlightFlags(text);
                } else {
                    String preview = text.length() > 300
                            ? text.substring(0, 300) + "..." : text;
                    appendText(resultPane, preview + "\n", new Color(0, 255, 100));
                }
            }
        }
        statsLabel.setText("  OpenStego parsed — version " + version
                + (isEncrypted ? " (encrypted)" : " (no password)"));
    }

    // ── Offer to save extracted binary data ──
    private void offerSave(byte[] data, String suggestedName) {
        int choice = JOptionPane.showConfirmDialog(this,
                "Extracted " + data.length + " bytes of binary data.\n" +
                        "Save as '" + suggestedName + "'?",
                "Save Extracted File",
                JOptionPane.YES_NO_OPTION);

        if (choice != JOptionPane.YES_OPTION) return;

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(suggestedName));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            java.nio.file.Files.write(fc.getSelectedFile().toPath(), data);
            appendText(resultPane,
                    "✅ Saved to: " + fc.getSelectedFile().getName() + "\n",
                    new Color(0, 255, 100));
        } catch (Exception ex) {
            appendText(resultPane,
                    "❌ Save failed: " + ex.getMessage() + "\n", Color.RED);
        }
    }

    // ══════════════════════════════════════════════════════
    // CORE: Extract LSB bits (1 bit per channel)
    // ══════════════════════════════════════════════════════
    private boolean[] extractBits(String channel, String readOrder, int maxBits) {
        int w = loadedImage.getWidth();
        int h = loadedImage.getHeight();
        List<Boolean> bits   = new ArrayList<>();
        boolean       rowFirst = readOrder.startsWith("Row");
        int outerMax = rowFirst ? h : w;
        int innerMax = rowFirst ? w : h;

        outer:
        for (int outer = 0; outer < outerMax; outer++) {
            for (int inner = 0; inner < innerMax; inner++) {
                int x = rowFirst ? inner : outer;
                int y = rowFirst ? outer : inner;
                int rgb = loadedImage.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >>  8) & 0xFF;
                int b =  rgb        & 0xFF;

                switch (channel) {
                    case "Red Only"   -> bits.add((r & 1) == 1);
                    case "Green Only" -> bits.add((g & 1) == 1);
                    case "Blue Only"  -> bits.add((b & 1) == 1);
                    default -> {
                        bits.add((r & 1) == 1);
                        bits.add((g & 1) == 1);
                        bits.add((b & 1) == 1);
                    }
                }
                if (bits.size() >= maxBits) break outer;
            }
        }

        boolean[] arr = new boolean[bits.size()];
        for (int i = 0; i < bits.size(); i++) arr[i] = bits.get(i);
        return arr;
    }

    // ══════════════════════════════════════════════════════
    // CORE: Extract N bits per channel (for OpenStego multi-bit)
    // ══════════════════════════════════════════════════════
    private byte[] extractBitsAsBytes(String channel, String readOrder,
                                      int maxBits, int bitsPerChannel) {
        int w = loadedImage.getWidth();
        int h = loadedImage.getHeight();
        List<Integer> bits    = new ArrayList<>();
        boolean       rowFirst = readOrder.startsWith("Row");

        outer:
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = loadedImage.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >>  8) & 0xFF;
                int b =  rgb        & 0xFF;

                int mask = (1 << bitsPerChannel) - 1;
                // Extract bitsPerChannel bits from each channel
                for (int bit = 0; bit < bitsPerChannel; bit++) {
                    bits.add((r >> bit) & 1);
                }
                for (int bit = 0; bit < bitsPerChannel; bit++) {
                    bits.add((g >> bit) & 1);
                }
                for (int bit = 0; bit < bitsPerChannel; bit++) {
                    bits.add((b >> bit) & 1);
                }
                if (bits.size() >= maxBits) break outer;
            }
        }

        // Assemble bits into bytes
        int    numBytes = bits.size() / 8;
        byte[] bytes    = new byte[numBytes];
        for (int i = 0; i < numBytes; i++) {
            int val = 0;
            for (int bit = 0; bit < 8; bit++) {
                int idx = i * 8 + bit;
                if (idx < bits.size() && bits.get(idx) == 1) {
                    val |= (1 << bit);
                }
            }
            bytes[i] = (byte) val;
        }
        return bytes;
    }

    // ══════════════════════════════════════════════════════
    // CORE: Assemble bits into bytes
    // ══════════════════════════════════════════════════════
    private byte[] bitsToBytes(boolean[] bits, boolean msbFirst) {
        int    numBytes = bits.length / 8;
        byte[] bytes    = new byte[numBytes];
        for (int i = 0; i < numBytes; i++) {
            int val = 0;
            for (int b = 0; b < 8; b++) {
                int idx = i * 8 + b;
                if (idx < bits.length && bits[idx]) {
                    val |= msbFirst ? (1 << (7 - b)) : (1 << b);
                }
            }
            bytes[i] = (byte) val;
        }
        return bytes;
    }

    // ══════════════════════════════════════════════════════
    // CORE: XOR byte array with a single-byte key
    // ══════════════════════════════════════════════════════
    private byte[] xorBytes(byte[] data, byte key) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte)(data[i] ^ key);
        }
        return result;
    }

    // ══════════════════════════════════════════════════════
    // DISPLAY helpers
    // ══════════════════════════════════════════════════════
    private void showAsText(byte[] bytes) {
        appendText(resultPane, "── TEXT OUTPUT ──\n", new Color(100, 200, 255));
        String text = toAscii(bytes);
        if (containsFlag(text)) {
            appendText(resultPane, "🚩 FLAG DETECTED!\n\n", Color.YELLOW);
            highlightFlags(text);
        } else {
            appendText(resultPane, text + "\n", new Color(0, 255, 100));
        }
        int p = countPrintable(bytes);
        appendText(resultPane,
                "\n[" + p + "/" + bytes.length + " printable — "
                        + String.format("%.1f", p * 100.0 / Math.max(bytes.length, 1)) + "%]\n\n",
                new Color(150, 150, 150));
    }

    private void showAsHex(byte[] bytes) {
        appendText(resultPane, "── HEX OUTPUT ──\n", new Color(100, 200, 255));
        StringBuilder hex = new StringBuilder();
        StringBuilder asc = new StringBuilder();
        int cols = 16;
        for (int i = 0; i < bytes.length; i++) {
            if (i % cols == 0) hex.append(String.format("%04X  ", i));
            hex.append(String.format("%02X ", bytes[i]));
            char c = (char)(bytes[i] & 0xFF);
            asc.append((c >= 32 && c < 127) ? c : '.');
            if ((i + 1) % cols == 0 || i == bytes.length - 1) {
                int pad = cols - ((i % cols) + 1);
                hex.append("   ".repeat(pad));
                hex.append("  ").append(asc).append("\n");
                asc.setLength(0);
            }
        }
        appendText(resultPane, hex.toString(), new Color(0, 255, 180));
        appendText(resultPane, "\n", Color.WHITE);
    }

    private void showAsBinary(boolean[] bits) {
        appendText(resultPane, "── BINARY OUTPUT ──\n", new Color(100, 200, 255));
        StringBuilder sb    = new StringBuilder();
        int           limit = Math.min(bits.length, 512);
        for (int i = 0; i < limit; i++) {
            sb.append(bits[i] ? '1' : '0');
            if ((i + 1) % 8 == 0) {
                int val = 0;
                for (int b = 0; b < 8; b++) {
                    int idx = (i / 8) * 8 + b;
                    if (idx < bits.length && bits[idx]) val |= (1 << (7 - b));
                }
                char c = (char) val;
                sb.append("  = ").append(String.format("%3d", val))
                        .append(" '").append((c >= 32 && c < 127) ? c : '.').append("'\n");
            }
        }
        if (bits.length > 512)
            sb.append("... (first 64 bytes shown)\n");
        appendText(resultPane, sb.toString(), new Color(180, 255, 180));
        appendText(resultPane, "\n", Color.WHITE);
    }

    private void highlightFlags(String text) {
        List<int[]> ranges = new ArrayList<>();
        for (String pat : FLAG_PATTERNS) {
            Matcher m = Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(text);
            while (m.find()) ranges.add(new int[]{m.start(), m.end()});
        }
        ranges.sort(Comparator.comparingInt(a -> a[0]));

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

    // ══════════════════════════════════════════════════════
    // UTILITY helpers
    // ══════════════════════════════════════════════════════
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

    private String toHexPreview(byte[] data, int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(data.length, max); i++)
            sb.append(String.format("%02X ", data[i]));
        return sb.toString().trim();
    }

    // ══════════════════════════════════════════════════════
    // SAVE output
    // ══════════════════════════════════════════════════════
    private void saveOutput() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("lsb_decoded.txt"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Text File", "txt"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            java.nio.file.Files.writeString(fc.getSelectedFile().toPath(),
                    resultPane.getText());
            JOptionPane.showMessageDialog(this,
                    "Saved: " + fc.getSelectedFile().getName(),
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showErr("Error: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // UI HELPERS
    // ══════════════════════════════════════════════════════
    private void drawFit(Graphics g, BufferedImage img, int pw, int ph) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        if (img != null) {
            double scale = Math.min((double) pw / img.getWidth(),
                    (double) ph / img.getHeight());
            int dw = (int)(img.getWidth() * scale);
            int dh = (int)(img.getHeight() * scale);
            g2.drawImage(img, (pw - dw) / 2, (ph - dh) / 2, dw, dh, null);
        } else {
            g2.setColor(Color.GRAY);
            g2.setFont(new Font("Arial", Font.ITALIC, 11));
            String h = "No Image Loaded";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(h, Math.max(0, (pw - fm.stringWidth(h)) / 2),
                    (ph + fm.getAscent()) / 2);
        }
    }

    private void appendText(JTextPane pane, String text, Color colour) {
        StyledDocument doc   = pane.getStyledDocument();
        Style          style = pane.addStyle("s", null);
        StyleConstants.setForeground(style, colour);
        StyleConstants.setFontFamily(style, "Monospaced");
        StyleConstants.setFontSize(style, 13);
        try { doc.insertString(doc.getLength(), text, style); }
        catch (BadLocationException ignored) {}
    }

    private void appendSectionHeader(JTextPane pane, String title) {
        String line = "═".repeat(52) + "\n";
        appendText(pane, line,                  new Color(60, 120, 60));
        appendText(pane, "  " + title + "\n",   Color.WHITE);
        appendText(pane, line,                  new Color(60, 120, 60));
    }

    private void clearPane(JTextPane pane) { pane.setText(""); }

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", Font.BOLD, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, 0, 2, 0));
        return l;
    }

    private JLabel makeHint(String text) {
        JLabel l = new JLabel("<html><i>" + text + "</i></html>");
        l.setFont(new Font("Arial", Font.ITALIC, 10));
        l.setForeground(new Color(120, 120, 140));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel makeSectionDivider(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", Font.BOLD, 10));
        l.setForeground(new Color(80, 80, 120));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void styleCombo(JComboBox<String> box) {
        box.setFont(new Font("Arial", Font.PLAIN, 12));
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private JButton makeGreenBtn(String text) {
        return makeColorBtn(text, new Color(34, 139, 34));
    }

    private JButton makeBlueBtn(String text) {
        return makeColorBtn(text, new Color(70, 130, 180));
    }

    private JButton makeOrangeBtn(String text) {
        return makeColorBtn(text, new Color(210, 105, 30));
    }

    private JButton makeRedBtn(String text) {
        return makeColorBtn(text, new Color(180, 50, 50));
    }

    private JButton makePurpleBtn(String text) {
        return makeColorBtn(text, new Color(130, 50, 180));
    }

    private JButton makeCyanBtn(String text) {
        return makeColorBtn(text, new Color(0, 140, 160));
    }

    private JButton makeColorBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Arial", Font.BOLD, 11));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void showErr(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}