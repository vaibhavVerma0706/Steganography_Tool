package Panels;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class CTFAnalysisPanel extends JPanel {

    // ── Class-level fields ──
    private BufferedImage loadedImage;
    private File          loadedFile;
    private JLabel        imagePreviewLabel;
    private JLabel        imageInfoLabel;
    private JTextArea     metadataArea;
    private JTextArea     signatureArea;
    private JTextArea     statisticsArea;
    private JTextArea     patternArea;
    private JCheckBox     chkBase64;
    private JCheckBox     chkFlags;
    private JCheckBox     chkBinary;
    private JCheckBox     chkMorse;

    // ── Known file magic bytes ──
    private static final Object[][] SIGNATURES = {
            {"PNG",   new int[]{0x89, 0x50, 0x4E, 0x47}},
            {"JPEG",  new int[]{0xFF, 0xD8, 0xFF}},
            {"ZIP",   new int[]{0x50, 0x4B, 0x03, 0x04}},
            {"PDF",   new int[]{0x25, 0x50, 0x44, 0x46}},
            {"RAR",   new int[]{0x52, 0x61, 0x72, 0x21}},
            {"GIF",   new int[]{0x47, 0x49, 0x46, 0x38}},
            {"EXE",   new int[]{0x4D, 0x5A}},
            {"MP3",   new int[]{0x49, 0x44, 0x33}},
            {"BMP",   new int[]{0x42, 0x4D}},
            {"7ZIP",  new int[]{0x37, 0x7A, 0xBC, 0xAF}},
            {"CLASS", new int[]{0xCA, 0xFE, 0xBA, 0xBE}},
    };

    // ── Morse code table ──
    private static final String[][] MORSE = {
            {".-","A"},    {"-...","B"},  {"-.-.","C"}, {"-..","D"},
            {".","E"},     {"..-.","F"},  {"--.","G"},  {"....","H"},
            {"..","I"},    {".---","J"},  {"-.-","K"},  {".-..","L"},
            {"--","M"},    {"-.","N"},    {"---","O"},  {".--.","P"},
            {"--.-","Q"},  {".-.","R"},   {"...","S"},  {"-","T"},
            {"..-","U"},   {"...-","V"},  {".--","W"},  {"-..-","X"},
            {"-.--","Y"},  {"--..","Z"},
            {".----","1"}, {"..---","2"}, {"...--","3"}, {"....-","4"},
            {".....","5"}, {"-....","6"}, {"--...","7"}, {"---..","8"},
            {"----.","9"}, {"-----","0"}
    };

    // ──────────────────────────────────────────────────────
    // Constructor
    // ──────────────────────────────────────────────────────
    public CTFAnalysisPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(new Color(245, 245, 250));
        add(buildTopSection(),  BorderLayout.NORTH);
        add(buildTabsSection(), BorderLayout.CENTER);
    }

    // ──────────────────────────────────────────────────────
    // Helper: load + convert image to RGB
    // ──────────────────────────────────────────────────────
    private BufferedImage loadRGB(File file) throws Exception {
        BufferedImage raw = ImageIO.read(file);
        if (raw == null) throw new Exception("Cannot read file. Use PNG or BMP.");
        BufferedImage result = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = result.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, raw.getWidth(), raw.getHeight());
        g2.drawImage(raw, 0, 0, null);
        g2.dispose();
        return result;
    }

    // ──────────────────────────────────────────────────────
    // Helper: open PNG/BMP file chooser
    // ──────────────────────────────────────────────────────
    private File chooseFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load Image for CTF Analysis");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image Files (PNG, BMP)", "png", "bmp"
        ));
        return fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
                ? fc.getSelectedFile() : null;
    }

    // ──────────────────────────────────────────────────────
    // Helper: dark terminal text area
    // ──────────────────────────────────────────────────────
    private JTextArea makeDarkArea(String hint) {
        JTextArea area = new JTextArea();
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setEditable(false);
        area.setBackground(new Color(20, 20, 20));
        area.setForeground(new Color(0, 255, 100));
        area.setCaretColor(Color.WHITE);
        area.setBorder(new EmptyBorder(8, 8, 8, 8));
        area.setText(hint);
        return area;
    }

    // ──────────────────────────────────────────────────────
    // Helper: green action button
    // ──────────────────────────────────────────────────────
    private JButton makeGreenBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setBackground(new Color(34, 139, 34));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ──────────────────────────────────────────────────────
    // Helper: blue secondary button
    // ──────────────────────────────────────────────────────
    private JButton makeBlueBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setBackground(new Color(70, 130, 180));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ──────────────────────────────────────────────────────
    // Helper: two-button control row
    // ──────────────────────────────────────────────────────
    private JPanel makeCtrlRow(JButton left, JButton right) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        row.setBackground(new Color(245, 245, 250));
        row.add(left);
        row.add(right);
        return row;
    }

    // ──────────────────────────────────────────────────────
    // Helper: italic description label
    // ──────────────────────────────────────────────────────
    private JLabel makeDescLabel(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("Arial", Font.ITALIC, 11));
        lbl.setForeground(new Color(80, 80, 80));
        return lbl;
    }

    // ──────────────────────────────────────────────────────
    // Helper: standard tab panel layout
    // ──────────────────────────────────────────────────────
    private JPanel makeTabPanel(JLabel desc, JPanel controls, JTextArea output) {
        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.setBackground(new Color(245, 245, 250));
        top.add(desc,     BorderLayout.NORTH);
        top.add(controls, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.add(top,                     BorderLayout.NORTH);
        panel.add(new JScrollPane(output), BorderLayout.CENTER);
        return panel;
    }

    // ──────────────────────────────────────────────────────
    // Helper: styled checkbox
    // ──────────────────────────────────────────────────────
    private JCheckBox makeCheckBox(String text) {
        JCheckBox cb = new JCheckBox(text, true);
        cb.setBackground(new Color(245, 245, 250));
        cb.setFont(new Font("Arial", Font.PLAIN, 12));
        return cb;
    }

    // ──────────────────────────────────────────────────────
    // Top section: shared image loader
    // ──────────────────────────────────────────────────────
    private JPanel buildTopSection() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("Load Image for Advanced CTF Analysis"));

        imagePreviewLabel = new JLabel("No Image Loaded", SwingConstants.CENTER);
        imagePreviewLabel.setPreferredSize(new Dimension(180, 120));
        imagePreviewLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        imagePreviewLabel.setBackground(Color.WHITE);
        imagePreviewLabel.setOpaque(true);
        imagePreviewLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        imagePreviewLabel.setForeground(Color.GRAY);

        imageInfoLabel = new JLabel("Upload a suspicious image to begin analysis");
        imageInfoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        imageInfoLabel.setForeground(Color.GRAY);

        JLabel tip = new JLabel("Tip: Try all tools on every CTF image!");
        tip.setFont(new Font("Arial", Font.ITALIC, 11));
        tip.setForeground(new Color(180, 100, 0));

        JButton uploadBtn = makeBlueBtn("📁 Load Image");
        uploadBtn.addActionListener(e -> handleLoad());

        JPanel right = new JPanel(new BorderLayout(5, 5));
        right.setBackground(new Color(245, 245, 250));
        right.add(imageInfoLabel, BorderLayout.NORTH);
        right.add(tip,            BorderLayout.CENTER);
        right.add(uploadBtn,      BorderLayout.SOUTH);

        panel.add(imagePreviewLabel, BorderLayout.WEST);
        panel.add(right,             BorderLayout.CENTER);
        return panel;
    }

    // ──────────────────────────────────────────────────────
    // Inner tabs: 4 analysis tools
    // ──────────────────────────────────────────────────────
    private JTabbedPane buildTabsSection() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Arial", Font.BOLD, 12));
        tabs.setBackground(new Color(245, 245, 250));
        tabs.addTab("📋 Metadata",   buildMetadataTab());
        tabs.addTab("🔏 Signatures", buildSignatureTab());
        tabs.addTab("📊 Statistics", buildStatisticsTab());
        tabs.addTab("🔎 Patterns",   buildPatternTab());
        tabs.setToolTipTextAt(0, "Read hidden metadata from image file");
        tabs.setToolTipTextAt(1, "Detect hidden files inside the image");
        tabs.setToolTipTextAt(2, "Chi-Square and entropy analysis");
        tabs.setToolTipTextAt(3, "Search for Base64, flags and Morse");
        return tabs;
    }

    // ══════════════════════════════════════════════════════
    // TAB 1 — METADATA
    // ══════════════════════════════════════════════════════
    private JPanel buildMetadataTab() {
        metadataArea = makeDarkArea("""
                Load an image and click Analyze Metadata

                Shows: file info, dimensions, PNG chunks,
                software used, hidden text, capacity""");
        JButton go   = makeGreenBtn("📋 Analyze Metadata");
        JButton copy = makeBlueBtn("📋 Copy Output");
        go.addActionListener(e -> runMetadata());
        copy.addActionListener(e -> doCopy(metadataArea));
        return makeTabPanel(
                makeDescLabel("Reads all metadata stored inside the image file"),
                makeCtrlRow(go, copy),
                metadataArea
        );
    }

    // ══════════════════════════════════════════════════════
    // TAB 2 — FILE SIGNATURES
    // ══════════════════════════════════════════════════════
    private JPanel buildSignatureTab() {
        signatureArea = makeDarkArea("""
                Load an image and click Scan File Signatures

                Scans for: PNG, JPEG, ZIP, PDF, RAR,
                GIF, EXE, MP3, BMP, 7ZIP, CLASS""");
        JButton go   = makeGreenBtn("🔏 Scan File Signatures");
        JButton copy = makeBlueBtn("📋 Copy Output");
        go.addActionListener(e -> runSignatures());
        copy.addActionListener(e -> doCopy(signatureArea));
        return makeTabPanel(
                makeDescLabel("Scans raw bytes for known file signatures — finds hidden files"),
                makeCtrlRow(go, copy),
                signatureArea
        );
    }

    // ══════════════════════════════════════════════════════
    // TAB 3 — STATISTICS
    // ══════════════════════════════════════════════════════
    private JPanel buildStatisticsTab() {
        statisticsArea = makeDarkArea("""
                Load an image and click Run Statistical Analysis

                Calculates: Chi-Square, Shannon Entropy,
                LSB randomness score and overall verdict""");
        JButton go   = makeGreenBtn("📊 Run Statistical Analysis");
        JButton copy = makeBlueBtn("📋 Copy Output");
        go.addActionListener(e -> runStatistics());
        copy.addActionListener(e -> doCopy(statisticsArea));
        return makeTabPanel(
                makeDescLabel("Chi-Square and entropy — detects steganography without a password"),
                makeCtrlRow(go, copy),
                statisticsArea
        );
    }

    // ══════════════════════════════════════════════════════
    // TAB 4 — PATTERN SEARCH
    // ══════════════════════════════════════════════════════
    private JPanel buildPatternTab() {
        patternArea = makeDarkArea("""
                Load an image and click Run Pattern Search

                Searches LSB bits for: Base64, CTF flags,
                Binary ASCII and Morse code""");

        chkBase64 = makeCheckBox("Base64 Decoder");
        chkFlags  = makeCheckBox("CTF Flag Patterns");
        chkBinary = makeCheckBox("Binary to ASCII");
        chkMorse  = makeCheckBox("Morse Code");

        JPanel checkRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        checkRow.setBackground(new Color(245, 245, 250));
        checkRow.setBorder(new TitledBorder("Search Options"));
        checkRow.add(chkBase64);
        checkRow.add(chkFlags);
        checkRow.add(chkBinary);
        checkRow.add(chkMorse);

        JButton go   = makeGreenBtn("🔎 Run Pattern Search");
        JButton copy = makeBlueBtn("📋 Copy Output");
        go.addActionListener(e -> runPatterns());
        copy.addActionListener(e -> doCopy(patternArea));

        JPanel controls = new JPanel(new BorderLayout(5, 5));
        controls.setBackground(new Color(245, 245, 250));
        controls.add(checkRow,              BorderLayout.CENTER);
        controls.add(makeCtrlRow(go, copy), BorderLayout.SOUTH);

        return makeTabPanel(
                makeDescLabel("Search LSB bits for Base64, CTF flags, Morse and binary ASCII"),
                controls,
                patternArea
        );
    }

    // ══════════════════════════════════════════════════════
    // IMAGE LOAD
    // ══════════════════════════════════════════════════════
    private void handleLoad() {
        File file = chooseFile();
        if (file == null) return;
        try {
            loadedImage = loadRGB(file);
            loadedFile  = file;
            imagePreviewLabel.setIcon(new ImageIcon(
                    loadedImage.getScaledInstance(180, 120, Image.SCALE_SMOOTH)
            ));
            imagePreviewLabel.setText("");
            imageInfoLabel.setText(
                    file.getName()
                            + " | " + loadedImage.getWidth()
                            + " x " + loadedImage.getHeight()
                            + " px | " + String.format("%.1f", file.length() / 1024.0) + " KB"
            );
            metadataArea.setText("Click Analyze Metadata to begin...");
            signatureArea.setText("Click Scan File Signatures to begin...");
            statisticsArea.setText("Click Run Statistical Analysis to begin...");
            patternArea.setText("Click Run Pattern Search to begin...");
        } catch (Exception ex) {
            showErr("Error loading image: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // METADATA LOGIC
    // ══════════════════════════════════════════════════════
    private void runMetadata() {
        if (noImage()) return;
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() { return buildMetadataText(); }
            @Override protected void done() {
                try { metadataArea.setText(get()); metadataArea.setCaretPosition(0); }
                catch (Exception ex) { showErr("Metadata failed: " + ex.getMessage()); }
            }
        }.execute();
    }

    private String buildMetadataText() {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════\n");
        sb.append(" METADATA ANALYSIS RESULTS\n");
        sb.append("═══════════════════════════════════════\n\n");

        sb.append("── FILE INFORMATION ──────────────────\n");
        sb.append("Name:     ").append(loadedFile.getName()).append("\n");
        sb.append("Size:     ")
                .append(String.format("%.2f", loadedFile.length() / 1024.0))
                .append(" KB (").append(loadedFile.length()).append(" bytes)\n");
        sb.append("Path:     ").append(loadedFile.getAbsolutePath()).append("\n");
        sb.append("Readable: ").append(loadedFile.canRead()).append("\n");
        sb.append("Hidden:   ").append(loadedFile.isHidden()).append("\n\n");

        sb.append("── IMAGE PROPERTIES ──────────────────\n");
        long totalPx = (long) loadedImage.getWidth() * loadedImage.getHeight();
        sb.append("Width:        ").append(loadedImage.getWidth()).append(" px\n");
        sb.append("Height:       ").append(loadedImage.getHeight()).append(" px\n");
        sb.append("Total Pixels: ").append(totalPx).append("\n");
        sb.append("Color Type:   ").append(colorTypeName()).append("\n");
        sb.append("Bit Depth:    8 bits per channel\n");
        sb.append("Channels:     3 (RGB)\n\n");

        sb.append("── STEGANOGRAPHY CAPACITY ────────────\n");
        long maxBits  = totalPx * 3;
        long maxBytes = maxBits / 8;
        sb.append("Max hidden bits:  ").append(maxBits).append("\n");
        sb.append("Max hidden bytes: ").append(maxBytes).append("\n");
        sb.append("Max hidden chars: ").append(maxBytes - 4).append("\n\n");

        sb.append("── PNG CHUNK ANALYSIS ────────────────\n");
        scanPNGChunks(sb);

        sb.append("\n── CTF HINTS ─────────────────────────\n");
        sb.append("Extension matches: ");
        sb.append(loadedFile.getName().toLowerCase().endsWith(".png")
                ? "YES (PNG)\n" : "CHECK MANUALLY\n");
        sb.append("Suspicious size:   ");
        sb.append(loadedFile.length() > (totalPx * 3 + 1000)
                ? "YES - larger than expected!\n" : "No - looks normal\n");
        sb.append("\nLarge file size may mean extra data\n");
        sb.append("is appended after the image ends!\n");
        return sb.toString();
    }

    private void scanPNGChunks(StringBuilder sb) {
        try (FileInputStream fis = new FileInputStream(loadedFile)) {
            byte[] all     = fis.readAllBytes();
            String content = new String(all, StandardCharsets.ISO_8859_1);

            List<String> pngChunks = extractTextChunks(all);
            sb.append("text chunks: ");
            if (pngChunks.isEmpty()) {
                sb.append("None found\n");
            } else {
                sb.append(pngChunks.size()).append(" found\n");
                for (String chunk : pngChunks) sb.append("  → ").append(chunk).append("\n");
            }

            sb.append("\nSoftware signatures:\n");
            String[] tools = {
                    "Steganography_hide","OpenStego","SilentEye",
                    "GIMP","Photoshop","Paint.NET",
                    "ImageMagick","Steganography","OutGuess"
            };
            boolean found = false;
            for (String tool : tools) {
                if (content.contains(tool)) {
                    sb.append("  !! FOUND: ").append(tool).append(" !!\n");
                    found = true;
                }
            }
            if (!found) sb.append("  None detected\n");

            sb.append("\nReadable strings in header:\n");
            extractHeaderStrings(all, sb);

        } catch (Exception ex) {
            sb.append("Could not read file: ").append(ex.getMessage()).append("\n");
        }
    }

    private List<String> extractTextChunks(byte[] bytes) {
        List<String> results  = new ArrayList<>();
        byte[] pngChunkMarker = "text".getBytes(StandardCharsets.US_ASCII);

        for (int i = 0; i < bytes.length - pngChunkMarker.length - 4; i++) {
            boolean hit = true;
            for (int j = 0; j < pngChunkMarker.length; j++) {
                if (bytes[i + j] != pngChunkMarker[j]) { hit = false; break; }
            }
            if (hit) {
                StringBuilder txt = new StringBuilder();
                int end = Math.min(i + pngChunkMarker.length + 100, bytes.length);
                for (int k = i + pngChunkMarker.length; k < end; k++) {
                    char c = (char)(bytes[k] & 0xFF);
                    if (c >= 32 && c < 127) txt.append(c);
                    else if (!txt.isEmpty()) break;
                }
                if (txt.length() > 2) results.add(txt.toString());
            }
        }
        return results;
    }

    private void extractHeaderStrings(byte[] bytes, StringBuilder sb) {
        StringBuilder cur = new StringBuilder();
        int limit = Math.min(512, bytes.length);
        int found = 0;
        for (int i = 0; i < limit; i++) {
            char c = (char)(bytes[i] & 0xFF);
            if (c >= 32 && c < 127) {
                cur.append(c);
            } else {
                if (!cur.isEmpty() && cur.length() >= 4) {
                    sb.append("  \"").append(cur).append("\"\n");
                    found++;
                }
                cur.setLength(0);
            }
        }
        if (found == 0) sb.append("  No readable strings found\n");
    }

    private String colorTypeName() {
        return switch (loadedImage.getType()) {
            case BufferedImage.TYPE_INT_RGB   -> "RGB";
            case BufferedImage.TYPE_INT_ARGB  -> "ARGB";
            case BufferedImage.TYPE_INT_BGR   -> "BGR";
            case BufferedImage.TYPE_BYTE_GRAY -> "Grayscale";
            default -> "Type " + loadedImage.getType();
        };
    }

    // ══════════════════════════════════════════════════════
    // SIGNATURE SCAN LOGIC
    // ══════════════════════════════════════════════════════
    private void runSignatures() {
        if (noImage()) return;
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() { return buildSignatureText(); }
            @Override protected void done() {
                try { signatureArea.setText(get()); signatureArea.setCaretPosition(0); }
                catch (Exception ex) { showErr("Scan failed: " + ex.getMessage()); }
            }
        }.execute();
    }

    private String buildSignatureText() {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════\n");
        sb.append(" FILE SIGNATURE SCAN RESULTS\n");
        sb.append("═══════════════════════════════════════\n\n");
        try (FileInputStream fis = new FileInputStream(loadedFile)) {
            byte[] all     = fis.readAllBytes();
            int totalFound = 0;
            sb.append("File size: ").append(all.length).append(" bytes\n");
            sb.append("Scanning ").append(SIGNATURES.length).append(" signatures...\n\n");
            sb.append("── RESULTS ───────────────────────────\n");
            for (Object[] sig : SIGNATURES) {
                String name = (String) sig[0];
                int[] magic = (int[]) sig[1];
                for (int pos : findSignature(all, magic)) {
                    String tag = pos == 0 ? " (image header)" : " !! HIDDEN FILE !!";
                    sb.append(String.format("  [%s] offset 0x%08X (%d)%s\n", name, pos, pos, tag));
                    sb.append("  Context: ");
                    int s = Math.max(0, pos - 2), e = Math.min(all.length, pos + 12);
                    for (int i = s; i < e; i++) sb.append(String.format("%02X ", all[i] & 0xFF));
                    sb.append("\n\n");
                    totalFound++;
                }
            }
            sb.append("── CONCLUSION ────────────────────────\n");
            if (totalFound <= 1) {
                sb.append("Only image header found.\n");
                sb.append("No hidden files detected.\n");
            } else {
                sb.append("!! SUSPICIOUS: Multiple signatures found !!\n");
                sb.append("Possible hidden files inside image!\n\n");
                sb.append("Try: bin_walk image.png\n");
                sb.append("     foremost image.png\n");
            }
        } catch (Exception ex) {
            sb.append("Error: ").append(ex.getMessage());
        }
        return sb.toString();
    }

    private List<Integer> findSignature(byte[] data, int[] magic) {
        List<Integer> hits = new ArrayList<>();
        for (int i = 0; i <= data.length - magic.length; i++) {
            boolean ok = true;
            for (int j = 0; j < magic.length; j++) {
                if ((data[i + j] & 0xFF) != magic[j]) { ok = false; break; }
            }
            if (ok) hits.add(i);
        }
        return hits;
    }

    // ══════════════════════════════════════════════════════
    // STATISTICS LOGIC
    // ══════════════════════════════════════════════════════
    private void runStatistics() {
        if (noImage()) return;
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() { return buildStatisticsText(); }
            @Override protected void done() {
                try { statisticsArea.setText(get()); statisticsArea.setCaretPosition(0); }
                catch (Exception ex) { showErr("Analysis failed: " + ex.getMessage()); }
            }
        }.execute();
    }

    private String buildStatisticsText() {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════\n");
        sb.append(" STATISTICAL ANALYSIS RESULTS\n");
        sb.append("═══════════════════════════════════════\n\n");

        int iw = loadedImage.getWidth(), ih = loadedImage.getHeight();
        long total = (long) iw * ih;
        int[] fR = new int[256], fG = new int[256], fB = new int[256];
        long oR = 0, oG = 0, oB = 0;

        for (int y = 0; y < ih; y++) {
            for (int x = 0; x < iw; x++) {
                int rgb = loadedImage.getRGB(x, y);
                int r   = (rgb >> 16) & 0xFF;
                int g   = (rgb >>  8) & 0xFF;
                int b   =  rgb        & 0xFF;
                fR[r]++; fG[g]++; fB[b]++;
                oR += (r & 1); oG += (g & 1); oB += (b & 1);
            }
        }

        sb.append("── LSB RANDOMNESS ────────────────────\n");
        double pR = (oR / (double) total) * 100;
        double pG = (oG / (double) total) * 100;
        double pB = (oB / (double) total) * 100;
        double pA = (pR + pG + pB) / 3;
        sb.append(String.format("Red   LSB ones: %.2f%%%n", pR));
        sb.append(String.format("Green LSB ones: %.2f%%%n", pG));
        sb.append(String.format("Blue  LSB ones: %.2f%%%n", pB));
        sb.append(String.format("Average:        %.2f%%%n", pA));
        sb.append(pA > 48 && pA < 52
                ? "Verdict: !! SUSPICIOUS - close to 50%!\n\n"
                : "Verdict: Probably clean\n\n");

        sb.append("── CHI-SQUARE TEST ───────────────────\n");
        double cR = calcChiSquare(fR, total);
        double cG = calcChiSquare(fG, total);
        double cB = calcChiSquare(fB, total);
        double cA = (cR + cG + cB) / 3;
        sb.append(String.format("Red   Chi-Square: %.4f%n", cR));
        sb.append(String.format("Green Chi-Square: %.4f%n", cG));
        sb.append(String.format("Blue  Chi-Square: %.4f%n", cB));
        sb.append(String.format("Average:          %.4f%n", cA));
        sb.append(cA < 0.1
                ? "Verdict: !! SUSPICIOUS - uniform distribution!\n\n"
                : "Verdict: Probably clean\n\n");

        sb.append("── SHANNON ENTROPY ───────────────────\n");
        double eR = calcEntropy(fR, total);
        double eG = calcEntropy(fG, total);
        double eB = calcEntropy(fB, total);
        double eA = (eR + eG + eB) / 3;
        sb.append(String.format("Red   Entropy: %.4f bits%n", eR));
        sb.append(String.format("Green Entropy: %.4f bits%n", eG));
        sb.append(String.format("Blue  Entropy: %.4f bits%n", eB));
        sb.append(String.format("Average:       %.4f bits%n", eA));
        sb.append(eA > 7.9 ? "Verdict: !! HIGH ENTROPY - encrypted data!\n\n"
                : eA > 7.0    ? "Verdict: Normal for natural images\n\n"
                : "Verdict: Low entropy - simple image\n\n");

        sb.append("── OVERALL VERDICT ───────────────────\n");
        boolean sus = (pA > 48 && pA < 52) || cA < 0.1 || eA > 7.9;
        if (sus) {
            sb.append("!! THIS IMAGE IS SUSPICIOUS !!\n");
            sb.append("Statistical tests suggest hidden data.\n");
            sb.append("Next: run Pattern Search and Signatures.\n");
        } else {
            sb.append("Image appears statistically clean.\n");
            sb.append("No obvious steganography detected.\n");
        }
        return sb.toString();
    }

    private double calcChiSquare(int[] freq, long total) {
        double exp = total / 256.0, chi = 0;
        for (int c : freq) { double d = c - exp; chi += d * d / exp; }
        return chi / 256.0;
    }

    private double calcEntropy(int[] freq, long total) {
        double ent = 0;
        for (int c : freq) {
            if (c > 0) { double p = c / (double) total; ent -= p * (Math.log(p) / Math.log(2)); }
        }
        return ent;
    }

    // ══════════════════════════════════════════════════════
    // PATTERN SEARCH LOGIC
    // ══════════════════════════════════════════════════════
    private void runPatterns() {
        if (noImage()) return;
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() { return buildPatternText(); }
            @Override protected void done() {
                try { patternArea.setText(get()); patternArea.setCaretPosition(0); }
                catch (Exception ex) { showErr("Search failed: " + ex.getMessage()); }
            }
        }.execute();
    }

    private String buildPatternText() {
        StringBuilder sb   = new StringBuilder();
        String         bits = gatherLSBBits();

        sb.append("═══════════════════════════════════════\n");
        sb.append(" PATTERN SEARCH RESULTS\n");
        sb.append("═══════════════════════════════════════\n\n");

        if (chkBinary.isSelected()) {
            sb.append("── BINARY TO ASCII ───────────────────\n");
            doBinaryASCII(bits, sb);
            sb.append("\n");
        }
        if (chkFlags.isSelected()) {
            sb.append("── CTF FLAG PATTERNS ─────────────────\n");
            doFlagSearch(bits, sb);
            sb.append("\n");
        }
        if (chkBase64.isSelected()) {
            sb.append("── BASE64 DECODER ────────────────────\n");
            doBase64(bits, sb);
            sb.append("\n");
        }
        if (chkMorse.isSelected()) {
            sb.append("── MORSE CODE DECODER ────────────────\n");
            doMorse(bits, sb);
            sb.append("\n");
        }
        return sb.toString();
    }

    private String gatherLSBBits() {
        StringBuilder bits = new StringBuilder();
        int iw = loadedImage.getWidth(), ih = loadedImage.getHeight();
        for (int y = 0; y < ih; y++) {
            for (int x = 0; x < iw; x++) {
                int rgb = loadedImage.getRGB(x, y);
                bits.append((rgb >> 16) & 1);
                bits.append((rgb >>  8) & 1);
                bits.append( rgb        & 1);
            }
        }
        return bits.toString();
    }

    private void doBinaryASCII(String bits, StringBuilder sb) {
        StringBuilder ascii = new StringBuilder();
        int lim = Math.min(bits.length() - 8, 2000);
        for (int i = 0; i < lim; i += 8) {
            int v = Integer.parseInt(bits.substring(i, i + 8), 2);
            if (v >= 32 && v <= 126)            ascii.append((char) v);
            else if (v == 0 && !ascii.isEmpty()) break;
            else                                 ascii.append('.');
        }
        String res = ascii.toString().trim();
        if (!res.isEmpty() && !res.replace(".", "").isEmpty()) {
            sb.append("First 250 chars from LSB bits:\n");
            sb.append(res, 0, Math.min(res.length(), 250)).append("\n");
            long readable = res.chars().filter(c -> c != '.').count();
            double ratio  = readable / (double) res.length();
            sb.append(String.format("Readable ratio: %.1f%%%n", ratio * 100));
            sb.append(ratio > 0.7
                    ? "!! HIGH READABLE RATIO - Likely hidden text!\n"
                    : "Low readable ratio - may be encrypted\n");
        } else {
            sb.append("No readable ASCII found in LSB bits\n");
        }
    }

    private void doFlagSearch(String bits, StringBuilder sb) {
        String[] prefixes = {
                "CTF{","ctf{","flag{","FLAG{",
                "picoCTF{","HTB{","THM{","DUCTF{"
        };
        StringBuilder ascii = new StringBuilder();
        int lim = Math.min(bits.length() - 8, 50000);
        for (int i = 0; i < lim; i += 8) {
            int v = Integer.parseInt(bits.substring(i, i + 8), 2);
            ascii.append(v >= 32 && v <= 126 ? (char) v : ' ');
        }
        String text   = ascii.toString();
        boolean found = false;
        for (String pre : prefixes) {
            int idx = text.indexOf(pre);
            if (idx >= 0) {
                int end = text.indexOf('}', idx);
                String flag = end > idx
                        ? text.substring(idx, end + 1)
                        : text.substring(idx, Math.min(idx + 50, text.length()));
                sb.append("!! FLAG FOUND !!\n  ").append(flag).append("\n\n");
                found = true;
            }
        }
        if (!found) sb.append("No CTF flag patterns found in LSB data\n");
    }

    private void doBase64(String bits, StringBuilder sb) {
        int bc        = bits.length() / 8;
        byte[] raw    = new byte[Math.min(bc, 1024)];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) Integer.parseInt(bits.substring(i * 8, i * 8 + 8), 2);
        }
        String src         = new String(raw, StandardCharsets.ISO_8859_1);
        StringBuilder run  = new StringBuilder();
        StringBuilder best = new StringBuilder();
        for (char c : src.toCharArray()) {
            if (isB64(c)) {
                run.append(c);
                if (run.length() > best.length()) best = new StringBuilder(run);
            } else {
                run.setLength(0);
            }
        }
        if (best.length() >= 8) {
            while (best.length() % 4 != 0) best.append('=');
            try {
                byte[] dec = Base64.getDecoder().decode(best.toString());
                String out = new String(dec, StandardCharsets.UTF_8);
                sb.append("Base64 found (length: ").append(best.length()).append(")\n");
                sb.append("Raw:     ").append(best, 0, Math.min(best.length(), 60)).append("\n");
                sb.append("Decoded: ").append(out,  0, Math.min(out.length(),  200)).append("\n");
            } catch (Exception ex) {
                sb.append("Base64 found but decode failed\n");
                sb.append("Raw: ").append(best, 0, Math.min(best.length(), 60)).append("\n");
            }
        } else {
            sb.append("No valid Base64 sequence found\n");
        }
    }

    private void doMorse(String bits, StringBuilder sb) {
        String sample  = bits.substring(0, Math.min(200, bits.length()));
        String morseStr = buildMorseString(sample);
        String decoded  = decodeMorseString(morseStr);
        sb.append("LSB bits as Morse:\n");
        sb.append(morseStr, 0, Math.min(morseStr.length(), 100)).append("\n\n");
        sb.append("Decoded:\n  ").append(decoded).append("\n");
        sb.append("\nNote: Try different bit groupings\n");
        sb.append("if the result looks incorrect.\n");
    }

    private String buildMorseString(String bits) {
        StringBuilder morse = new StringBuilder();
        for (int i = 0; i + 1 < bits.length(); i += 2) {
            switch (bits.substring(i, i + 2)) {
                case "10" -> morse.append(". ");
                case "00" -> morse.append("- ");
                case "11" -> morse.append("/ ");
                default   -> morse.append("  ");
            }
        }
        return morse.toString().trim();
    }

    private String decodeMorseString(String morse) {
        StringBuilder out = new StringBuilder();
        for (String word : morse.split(" / ")) {
            for (String letter : word.trim().split("\\s+")) {
                String cl  = letter.trim();
                boolean ok = false;
                for (String[] entry : MORSE) {
                    if (entry[0].equals(cl)) { out.append(entry[1]); ok = true; break; }
                }
                if (!ok && !cl.isEmpty()) out.append('?');
            }
            out.append(' ');
        }
        return out.toString().trim();
    }

    private boolean isB64(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                || (c >= '0' && c <= '9') || c == '+' || c == '/' || c == '=';
    }

    // ══════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════
    private void doCopy(JTextArea area) {
        area.selectAll();
        area.copy();
        area.select(0, 0);
        JOptionPane.showMessageDialog(
                this, "Output copied to clipboard!", "Success", JOptionPane.INFORMATION_MESSAGE
        );
    }

    private boolean noImage() {
        if (loadedImage == null || loadedFile == null) {
            showErr("Please load an image first!");
            return true;
        }
        return false;
    }

    private void showErr(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}