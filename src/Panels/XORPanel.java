package Panels;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class XORPanel extends JPanel {

    // ── Image data fields ──
    private BufferedImage image1;
    private BufferedImage image2;
    private BufferedImage xorResult;

    // ── Image display panels (custom painted — never reassigned) ──
    private JPanel image1Display;
    private JPanel image2Display;
    private JPanel xorDisplay;

    // ── Info labels ──
    private JLabel image1InfoLabel;
    private JLabel image2InfoLabel;
    private JLabel statusLabel;

    // ── Controls ──
    private JSlider   amplifySlider;
    private JLabel    amplifyValLabel;
    private JTextPane statsPane;

    // ──────────────────────────────────────────────────────
    // Constructor
    // ──────────────────────────────────────────────────────
    public XORPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(new Color(245, 245, 250));

        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildMainArea(),  BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ──────────────────────────────────────────────────────
    // TOP BAR — description + controls
    // ──────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel panel = new JPanel(new BorderLayout(10, 8));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder(
                "XOR Image Generator — Reveal exact pixel differences between two images"));

        JLabel desc = new JLabel("""
            <html><b>💡 What this does:</b> XOR compares two images pixel by pixel.
            Identical pixels → black.  Changed pixels → bright colour.
            Hidden LSB data becomes immediately visible as bright patches!</html>""");
        desc.setFont(new Font("Arial", Font.PLAIN, 11));
        desc.setForeground(new Color(80, 80, 120));

        JPanel ctrlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        ctrlRow.setBackground(new Color(245, 245, 250));

        amplifyValLabel = new JLabel("1×");
        amplifyValLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
        amplifyValLabel.setPreferredSize(new Dimension(35, 20));
        amplifyValLabel.setForeground(new Color(50, 50, 150));

        amplifySlider = new JSlider(1, 50, 1);
        amplifySlider.setBackground(new Color(245, 245, 250));
        amplifySlider.setPreferredSize(new Dimension(180, 24));
        amplifySlider.setToolTipText(
                "Multiply pixel differences to make 1-bit LSB changes visible");
        amplifySlider.addChangeListener(e ->
                amplifyValLabel.setText(amplifySlider.getValue() + "×"));

        JButton xorBtn     = makeBtn("⊕ Generate XOR",         new Color(34, 139, 34));
        JButton saveBtn    = makeBtn("💾 Save XOR Result",      new Color(70, 130, 180));
        JButton clearBtn   = makeBtn("🗑️ Clear All",            new Color(160, 60, 60));

        xorBtn.addActionListener(e  -> generateXOR());
        saveBtn.addActionListener(e -> saveResult());
        clearBtn.addActionListener(e -> clearAll());

        ctrlRow.add(makeBoldLabel());
        ctrlRow.add(amplifySlider);
        ctrlRow.add(amplifyValLabel);
        ctrlRow.add(Box.createHorizontalStrut(12));
        ctrlRow.add(xorBtn);
        ctrlRow.add(saveBtn);
        ctrlRow.add(clearBtn);

        panel.add(desc,    BorderLayout.NORTH);
        panel.add(ctrlRow, BorderLayout.CENTER);
        return panel;
    }

    // ──────────────────────────────────────────────────────
    // MAIN AREA — three image panels + stats
    // ──────────────────────────────────────────────────────
    private JPanel buildMainArea() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(new Color(245, 245, 250));
        panel.add(buildImagesRow(),  BorderLayout.CENTER);
        panel.add(buildStatsPanel(), BorderLayout.SOUTH);
        return panel;
    }

    // ── Three image panels side by side ──
    private JPanel buildImagesRow() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 8, 0));
        panel.setBackground(new Color(245, 245, 250));

        // Info labels
        image1InfoLabel = makeInfoLabel();
        image2InfoLabel = makeInfoLabel();

        // Image 1 display panel
        image1Display = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFit(g, image1, getWidth(), getHeight(), "Click 'Load Image 1'");
            }
        };
        image1Display.setBackground(Color.BLACK);

        // Image 2 display panel
        image2Display = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFit(g, image2, getWidth(), getHeight(), "Click 'Load Image 2'");
            }
        };
        image2Display.setBackground(Color.BLACK);

        // XOR result display panel
        xorDisplay = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFit(g, xorResult, getWidth(), getHeight(),
                        "XOR result appears here");
            }
        };
        xorDisplay.setBackground(Color.BLACK);

        // Load buttons for image 1 and 2
        JButton loadBtn1 = makeBtn("📁 Load Image 1 (Original)",  new Color(70, 130, 180));
        JButton loadBtn2 = makeBtn("📁 Load Image 2 (Encoded)",   new Color(70, 130, 180));
        loadBtn1.addActionListener(e -> loadImage1());
        loadBtn2.addActionListener(e -> loadImage2());

        panel.add(wrapDisplay(image1Display, image1InfoLabel,
                "Image 1 — Original",          new Color(70, 130, 180), loadBtn1));
        panel.add(wrapDisplay(image2Display, image2InfoLabel,
                "Image 2 — Encoded/Modified",  new Color(70, 130, 180), loadBtn2));
        panel.add(wrapDisplay(xorDisplay,   new JLabel("", SwingConstants.CENTER),
                "XOR Result",                   new Color(34, 139, 34),  null));

        return panel;
    }

    // ── Wrap a display panel with title, info label, border, optional button ──
    private JPanel wrapDisplay(JPanel displayPanel, JLabel infoLabel,
                               String title, Color borderColor, JButton loadBtn) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        wrapper.setBackground(new Color(245, 245, 250));
        wrapper.setBorder(new TitledBorder(title));

        displayPanel.setBorder(BorderFactory.createLineBorder(borderColor, 2));

        wrapper.add(infoLabel,    BorderLayout.NORTH);
        wrapper.add(displayPanel, BorderLayout.CENTER);
        if (loadBtn != null) {
            wrapper.add(loadBtn, BorderLayout.SOUTH);
        }
        return wrapper;
    }

    // ── Stats panel ──
    private JPanel buildStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("XOR Statistics & Analysis"));
        panel.setPreferredSize(new Dimension(0, 140));

        statsPane = new JTextPane();
        statsPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        statsPane.setEditable(false);
        statsPane.setBackground(new Color(15, 15, 15));
        statsPane.setForeground(new Color(0, 255, 100));
        statsPane.setBorder(new EmptyBorder(6, 10, 6, 10));

        showStatsWelcome();

        panel.add(new JScrollPane(statsPane), BorderLayout.CENTER);
        return panel;
    }

    // ──────────────────────────────────────────────────────
    // STATUS BAR
    // ──────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 30, 60));
        panel.setBorder(new EmptyBorder(4, 10, 4, 10));

        statusLabel = new JLabel("Ready — Load two images then click Generate XOR");
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(180, 220, 180));

        JLabel hint = new JLabel(
                "Tip: Black = identical  |  Bright = changed  |  "
                        + "Increase Amplify to see subtle 1-bit changes");
        hint.setFont(new Font("Arial", Font.ITALIC, 10));
        hint.setForeground(new Color(180, 180, 220));

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(hint,        BorderLayout.EAST);
        return panel;
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Load images via buttons
    // ══════════════════════════════════════════════════════
    private void loadImage1() {
        BufferedImage img = loadImageFromDisk("Load Original Image (Image 1)");
        if (img == null) return;
        image1 = img;
        image1InfoLabel.setText(img.getWidth() + " × " + img.getHeight() + " px");
        image1Display.repaint();
        statusLabel.setText("Image 1 loaded — "
                + img.getWidth() + "×" + img.getHeight() + " px");
    }

    private void loadImage2() {
        BufferedImage img = loadImageFromDisk("Load Encoded Image (Image 2)");
        if (img == null) return;
        image2 = img;
        image2InfoLabel.setText(img.getWidth() + " × " + img.getHeight() + " px");
        image2Display.repaint();
        statusLabel.setText("Image 2 loaded — "
                + img.getWidth() + "×" + img.getHeight() + " px");
    }

    // ── Shared image loader ──
    private BufferedImage loadImageFromDisk(String title) {
        File file = openImageFile(title);
        if (file == null) return null;
        try {
            BufferedImage raw = ImageIO.read(file);
            if (raw == null) throw new Exception("Cannot read image.");
            BufferedImage rgb = new BufferedImage(
                    raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = rgb.createGraphics();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, raw.getWidth(), raw.getHeight());
            g2.drawImage(raw, 0, 0, null);
            g2.dispose();
            return rgb;
        } catch (Exception ex) {
            showError("Error loading: " + ex.getMessage());
            return null;
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Generate XOR image
    // ══════════════════════════════════════════════════════
    private void generateXOR() {
        if (image1 == null) { showError("Please load Image 1 first!"); return; }
        if (image2 == null) { showError("Please load Image 2 first!"); return; }

        if (image1.getWidth()  != image2.getWidth()
                || image1.getHeight() != image2.getHeight()) {
            showError("Images must be the same size!\n"
                    + "Image 1: " + image1.getWidth() + "×" + image1.getHeight() + "\n"
                    + "Image 2: " + image2.getWidth() + "×" + image2.getHeight());
            return;
        }

        int    w       = image1.getWidth();
        int    h       = image1.getHeight();
        int    amplify = amplifySlider.getValue();
        long   total   = (long) w * h;

        BufferedImage result    = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        long          diffCount = 0;
        long          totalDiff = 0;
        long          maxDiff   = 0;
        long          rDiff     = 0;
        long          gDiff     = 0;
        long          bDiff     = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb1 = image1.getRGB(x, y);
                int rgb2 = image2.getRGB(x, y);

                int r1 = (rgb1 >> 16) & 0xFF;
                int g1 = (rgb1 >>  8) & 0xFF;
                int b1 =  rgb1        & 0xFF;
                int r2 = (rgb2 >> 16) & 0xFF;
                int g2 = (rgb2 >>  8) & 0xFF;
                int b2 =  rgb2        & 0xFF;

                int xorR = r1 ^ r2;
                int xorG = g1 ^ g2;
                int xorB = b1 ^ b2;

                if (xorR != 0 || xorG != 0 || xorB != 0) {
                    diffCount++;
                    long pixDiff = Math.abs(r1-r2) + Math.abs(g1-g2) + Math.abs(b1-b2);
                    totalDiff += pixDiff;
                    if (pixDiff > maxDiff) maxDiff = pixDiff;
                    rDiff += Math.abs(r1 - r2);
                    gDiff += Math.abs(g1 - g2);
                    bDiff += Math.abs(b1 - b2);
                }

                int outR = clamp(xorR * amplify);
                int outG = clamp(xorG * amplify);
                int outB = clamp(xorB * amplify);
                result.setRGB(x, y, (outR << 16) | (outG << 8) | outB);
            }
        }

        xorResult = result;
        xorDisplay.repaint();

        double pctDiff = (diffCount / (double) total) * 100.0;
        double avgDiff = diffCount > 0 ? (double) totalDiff / diffCount : 0;

        showStats(diffCount, total, pctDiff, avgDiff, maxDiff,
                rDiff, gDiff, bDiff, amplify);

        statusLabel.setText("XOR generated — "
                + diffCount + " pixels differ ("
                + String.format("%.4f", pctDiff) + "%)");
    }

    // ══════════════════════════════════════════════════════
    // DISPLAY: XOR statistics
    // ══════════════════════════════════════════════════════
    private void showStats(long diffCount, long total, double pctDiff,
                           double avgDiff, long maxDiff,
                           long rDiff, long gDiff, long bDiff, int amplify) {
        clearPane(statsPane);

        appendText(statsPane,
                "Pixels changed : " + diffCount + " / " + total
                        + "  (" + String.format("%.4f", pctDiff) + "%)\n",
                new Color(0, 255, 100));
        appendText(statsPane,
                "Avg difference : " + String.format("%.2f", avgDiff)
                        + "  |  Max: " + maxDiff + " / 765\n",
                new Color(0, 255, 100));
        appendText(statsPane,
                "Per channel    : R=" + rDiff + "  G=" + gDiff + "  B=" + bDiff + "\n",
                new Color(100, 200, 255));
        appendText(statsPane,
                "Amplify factor : " + amplify + "×\n\n",
                new Color(150, 150, 150));

        String verdict;
        Color  verdictCol;
        if (diffCount == 0) {
            verdict    = "✅ Images are IDENTICAL — no differences detected.";
            verdictCol = new Color(0, 200, 80);
        } else if (pctDiff < 0.01) {
            verdict    = "🚩 VERY FEW pixels changed — consistent with LSB steganography!";
            verdictCol = Color.YELLOW;
        } else if (pctDiff < 5.0) {
            verdict    = "⚠️  Small % changed — subtle encoding or minor edit.";
            verdictCol = new Color(255, 165, 0);
        } else {
            verdict    = "🔴 Large differences — images may be significantly different.";
            verdictCol = Color.RED;
        }
        appendText(statsPane, verdict + "\n\n", verdictCol);

        appendText(statsPane, "Most changed channel: ", new Color(100, 200, 255));
        String dom;
        if (rDiff >= gDiff && rDiff >= bDiff)      dom = "🔴 RED";
        else if (gDiff >= rDiff && gDiff >= bDiff) dom = "🟢 GREEN";
        else                                        dom = "🔵 BLUE";
        appendText(statsPane, dom + "\n", Color.WHITE);
        appendText(statsPane,
                "→ Try LSB Decoder with "
                        + dom.split(" ")[1] + " channel selected!\n", Color.YELLOW);
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Save XOR result
    // ══════════════════════════════════════════════════════
    private void saveResult() {
        if (xorResult == null) {
            showError("Please generate XOR result first!");
            return;
        }
        File out = openSaveDialog();
        if (out == null) return;
        if (!out.getName().toLowerCase().endsWith(".png"))
            out = new File(out.getAbsolutePath() + ".png");
        try {
            ImageIO.write(xorResult, "PNG", out);
            JOptionPane.showMessageDialog(this,
                    "Saved: " + out.getName(), "Saved",
                    JOptionPane.INFORMATION_MESSAGE);
            statusLabel.setText("Saved: " + out.getName());
        } catch (Exception ex) {
            showError("Error saving: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Clear everything
    // ══════════════════════════════════════════════════════
    private void clearAll() {
        image1    = null;
        image2    = null;
        xorResult = null;
        image1InfoLabel.setText("No image loaded");
        image2InfoLabel.setText("No image loaded");
        amplifySlider.setValue(1);
        amplifyValLabel.setText("1×");
        image1Display.repaint();
        image2Display.repaint();
        xorDisplay.repaint();
        clearPane(statsPane);
        showStatsWelcome();
        statusLabel.setText("Cleared — Load two images and click Generate XOR.");
    }

    // ══════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════
    private int clamp(int val) {
        return Math.max(0, Math.min(255, val));
    }

    private void drawFit(Graphics g, BufferedImage img,
                         int panelW, int panelH, String hint) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        if (img != null) {
            double scale = Math.min(
                    (double) panelW / img.getWidth(),
                    (double) panelH / img.getHeight());
            int dw = (int)(img.getWidth()  * scale);
            int dh = (int)(img.getHeight() * scale);
            g2.drawImage(img, (panelW - dw) / 2, (panelH - dh) / 2, dw, dh, null);
        } else {
            g2.setColor(new Color(80, 80, 80));
            g2.setFont(new Font("Arial", Font.ITALIC, 11));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(hint,
                    Math.max(0, (panelW - fm.stringWidth(hint)) / 2),
                    (panelH + fm.getAscent()) / 2);
        }
    }

    // ══════════════════════════════════════════════════════
    // FILE CHOOSER HELPERS
    // ══════════════════════════════════════════════════════
    private File openImageFile(String title) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(title);
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image Files (PNG, BMP, JPG, JPEG)",
                "png", "bmp", "jpg", "jpeg"));
        return fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
                ? fc.getSelectedFile() : null;
    }

    private File openSaveDialog() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save XOR Result");
        fc.setSelectedFile(new File("xor_result.png"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "PNG Image", "png"));
        return fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION
                ? fc.getSelectedFile() : null;
    }

    // ══════════════════════════════════════════════════════
    // DISPLAY HELPERS
    // ══════════════════════════════════════════════════════
    private void showStatsWelcome() {
        appendText(statsPane, """
                HOW TO USE:
                  1. Click 'Load Image 1' → load original/clean image
                  2. Click 'Load Image 2' → load encoded/modified image
                  3. Click '⊕ Generate XOR' to see differences
                  4. Increase Amplify slider to make 1-bit LSB changes visible

                WHAT YOU WILL SEE:
                  ⬛ Black  = identical pixels (no change)
                  🟡 Bright = changed pixels  (hidden data region!)
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

    private void clearPane(JTextPane pane) { pane.setText(""); }

    private JLabel makeInfoLabel() {
        JLabel l = new JLabel("No image loaded", SwingConstants.CENTER);
        l.setFont(new Font("Arial", Font.PLAIN, 10));
        l.setForeground(new Color(100, 100, 100));
        return l;
    }

    private JLabel makeBoldLabel() {
        JLabel l = new JLabel("Amplify:");
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