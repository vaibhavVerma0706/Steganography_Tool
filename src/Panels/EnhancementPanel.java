package Panels;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class EnhancementPanel extends JPanel {

    // ── Fields ──
    private BufferedImage originalImage;   // never modified — always the source
    private BufferedImage enhancedImage;   // current result shown in panel

    // Image display panels
    private JPanel originalPanel;
    private JPanel enhancedPanel;

    // UI labels
    private JLabel imageInfoLabel;
    private JLabel statusLabel;

    // ── Sliders ──
    private JSlider brightnessSlider;
    private JSlider contrastSlider;
    private JSlider gammaSlider;
    private JSlider thresholdSlider;

    // ── Slider value labels ──
    private JLabel brightnessValLabel;
    private JLabel contrastValLabel;
    private JLabel gammaValLabel;
    private JLabel thresholdValLabel;

    // ── Toggle buttons ──
    private JToggleButton edgeBtn;
    private JToggleButton invertBtn;
    private JToggleButton grayscaleBtn;
    private JToggleButton thresholdBtn;

    // ──────────────────────────────────────────────────────
    // Constructor
    // ──────────────────────────────────────────────────────
    public EnhancementPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(new Color(245, 245, 250));

        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildMainArea(),  BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ──────────────────────────────────────────────────────
    // TOP BAR — loader + buttons
    // ──────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel panel = new JPanel(new BorderLayout(10, 8));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder(
                "Image Enhancement — Reveal hidden content with filters"));

        imageInfoLabel = new JLabel(
                "Load a PNG or BMP image to begin enhancement analysis");
        imageInfoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        imageInfoLabel.setForeground(Color.GRAY);

        JLabel desc = new JLabel("""
            <html><b>💡 What this does:</b> Applies brightness, contrast, gamma and edge
            filters to reveal hidden content invisible to the naked eye.
            Original is always preserved on the left for comparison.</html>""");
        desc.setFont(new Font("Arial", Font.PLAIN, 11));
        desc.setForeground(new Color(80, 80, 120));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRow.setBackground(new Color(245, 245, 250));

        JButton loadBtn  = makeBtn("📁 Load Image",    new Color(70, 130, 180));
        JButton applyBtn = makeBtn("✨ Apply Filters",  new Color(34, 139, 34));
        JButton resetBtn = makeBtn("↺ Reset All",      new Color(139, 90, 34));
        JButton saveBtn  = makeBtn("💾 Save Result",   new Color(100, 60, 160));

        loadBtn.addActionListener(e  -> handleLoad());
        applyBtn.addActionListener(e -> applyFilters());
        resetBtn.addActionListener(e -> resetAll());
        saveBtn.addActionListener(e  -> saveResult());

        btnRow.add(loadBtn);
        btnRow.add(applyBtn);
        btnRow.add(resetBtn);
        btnRow.add(saveBtn);

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.setBackground(new Color(245, 245, 250));
        top.add(imageInfoLabel, BorderLayout.NORTH);
        top.add(desc,           BorderLayout.CENTER);
        top.add(btnRow,         BorderLayout.SOUTH);

        panel.add(top, BorderLayout.CENTER);
        return panel;
    }

    // ──────────────────────────────────────────────────────
    // MAIN AREA — controls left, images right
    // ──────────────────────────────────────────────────────
    private JPanel buildMainArea() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(new Color(245, 245, 250));
        panel.add(buildControlsPanel(), BorderLayout.WEST);
        panel.add(buildImagesPanel(),   BorderLayout.CENTER);
        return panel;
    }

    // ──────────────────────────────────────────────────────
    // CONTROLS PANEL — left side
    // ──────────────────────────────────────────────────────
    private JPanel buildControlsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("Filter Controls"));
        panel.setPreferredSize(new Dimension(220, 0));

        // ── Brightness ──
        brightnessValLabel = makeValLabel("0");
        brightnessSlider   = makeSlider(-100, 100, 0);
        brightnessSlider.addChangeListener(e ->
                brightnessValLabel.setText(String.valueOf(brightnessSlider.getValue())));

        panel.add(makeSectionLabel("☀️ Brightness"));
        panel.add(makeSliderRow(brightnessSlider, brightnessValLabel));
        panel.add(makeHint("Boost dark areas to reveal hidden content"));
        panel.add(Box.createVerticalStrut(10));

        // ── Contrast ──
        contrastValLabel = makeValLabel("100%");
        contrastSlider   = makeSlider(50, 500, 100);
        contrastSlider.addChangeListener(e ->
                contrastValLabel.setText(contrastSlider.getValue() + "%"));

        panel.add(makeSectionLabel("◑ Contrast"));
        panel.add(makeSliderRow(contrastSlider, contrastValLabel));
        panel.add(makeHint("Spread pixel values — reveal subtle patterns"));
        panel.add(Box.createVerticalStrut(10));

        // ── Gamma ──
        gammaValLabel = makeValLabel("1.0");
        gammaSlider   = makeSlider(1, 100, 10); // stored as x10: 10 = gamma 1.0
        gammaSlider.addChangeListener(e ->
                gammaValLabel.setText(String.format("%.1f", gammaSlider.getValue() / 10.0)));

        panel.add(makeSectionLabel("γ Gamma"));
        panel.add(makeSliderRow(gammaSlider, gammaValLabel));
        panel.add(makeHint("Gamma < 1.0 brightens shadows dramatically"));
        panel.add(Box.createVerticalStrut(10));

        // ── Threshold ──
        thresholdValLabel = makeValLabel("128");
        thresholdSlider   = makeSlider(0, 255, 128);
        thresholdSlider.addChangeListener(e ->
                thresholdValLabel.setText(String.valueOf(thresholdSlider.getValue())));

        panel.add(makeSectionLabel("⬛ Threshold"));
        panel.add(makeSliderRow(thresholdSlider, thresholdValLabel));
        panel.add(makeHint("Makes image pure black/white at cutoff"));
        panel.add(Box.createVerticalStrut(14));

        // ── Toggle filters ──
        panel.add(makeSectionLabel("Filter Toggles:"));
        panel.add(Box.createVerticalStrut(4));

        edgeBtn      = makeToggle("🔲 Edge Detection");
        invertBtn    = makeToggle("⬜ Invert Colours");
        grayscaleBtn = makeToggle("▦ Grayscale");
        thresholdBtn = makeToggle("⬛ Apply Threshold");

        edgeBtn.setToolTipText("Detects edges — reveals micro-changes from LSB encoding");
        invertBtn.setToolTipText("Inverts all colours — sometimes reveals hidden patterns");
        grayscaleBtn.setToolTipText("Convert to grayscale — easier to spot brightness anomalies");
        thresholdBtn.setToolTipText("Black/white threshold — use slider above to set cutoff");

        for (JToggleButton btn : new JToggleButton[]{
                edgeBtn, invertBtn, grayscaleBtn, thresholdBtn
        }) {
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(btn);
            panel.add(Box.createVerticalStrut(4));
        }

        panel.add(Box.createVerticalStrut(10));
        panel.add(buildQuickPresetsPanel());
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    // ── Quick presets panel ──
    private JPanel buildQuickPresetsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(230, 240, 255));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 150, 200)),
                new EmptyBorder(6, 6, 6, 6)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        JLabel title = new JLabel("⚡ Quick Presets");
        title.setFont(new Font("Arial", Font.BOLD, 11));
        title.setForeground(new Color(50, 50, 150));
        panel.add(title);
        panel.add(Box.createVerticalStrut(4));

        // Each preset: name, brightness, contrast, gamma*10, description
        Object[][] presets = {
                {"🌑 Shadow Reveal",   80,  300, 3,  "Reveals hidden dark-area content" },
                {"🔬 LSB Amplify",      0,  500, 10, "Max contrast for LSB patterns"    },
                {"🌅 Gamma Boost",      0,  100, 3,  "Low gamma — lifts shadows"        },
                {"📄 High Key",        50,  200, 20, "Brightens entire image"           },
        };

        for (Object[] preset : presets) {
            JButton btn = getJButton(preset);
            panel.add(btn);
            panel.add(Box.createVerticalStrut(3));
        }
        return panel;
    }

    private JButton getJButton(Object[] preset) {
        String name   = (String)  preset[0];
        int    bright = (Integer) preset[1];
        int    contr  = (Integer) preset[2];
        int    gamma  = (Integer) preset[3];

        JButton btn = new JButton(name);
        btn.setFont(new Font("Arial", Font.PLAIN, 10));
        btn.setBackground(new Color(200, 215, 240));
        btn.setFocusPainted(false);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setToolTipText((String) preset[4]);
        btn.addActionListener(e -> {
            brightnessSlider.setValue(bright);
            contrastSlider.setValue(contr);
            gammaSlider.setValue(gamma);
            edgeBtn.setSelected(false);
            invertBtn.setSelected(false);
            thresholdBtn.setSelected(false);
            applyFilters();
        });
        return btn;
    }

    // ──────────────────────────────────────────────────────
    // IMAGES PANEL — original left, enhanced right
    // ──────────────────────────────────────────────────────
    private JPanel buildImagesPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 0));
        panel.setBackground(new Color(245, 245, 250));

        // Original
        originalPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFit(g, originalImage, getWidth(), getHeight(),
                        "Original image appears here");
            }
        };
        originalPanel.setBackground(Color.BLACK);
        originalPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));

        // Enhanced
        enhancedPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFit(g, enhancedImage, getWidth(), getHeight(),
                        "Enhanced result appears here");
            }
        };
        enhancedPanel.setBackground(Color.BLACK);
        enhancedPanel.setBorder(BorderFactory.createLineBorder(
                new Color(34, 139, 34), 2));

        panel.add(wrapImage(originalPanel, "Original"));
        panel.add(wrapImage(enhancedPanel, "Enhanced Result"));
        return panel;
    }

    private JPanel wrapImage(JPanel imgPanel, String title) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(245, 245, 250));
        wrapper.setBorder(new TitledBorder(title));
        wrapper.add(imgPanel, BorderLayout.CENTER);
        return wrapper;
    }

    // ──────────────────────────────────────────────────────
    // STATUS BAR
    // ──────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 30, 60));
        panel.setBorder(new EmptyBorder(4, 10, 4, 10));

        statusLabel = new JLabel("Ready — Load an image and adjust filters");
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(180, 220, 180));

        JLabel hint = new JLabel(
                "Tip: Use 'Shadow Reveal' preset first — most hidden content lives in dark areas");
        hint.setFont(new Font("Arial", Font.ITALIC, 10));
        hint.setForeground(new Color(180, 180, 220));

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(hint,        BorderLayout.EAST);
        return panel;
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Load Image
    // ══════════════════════════════════════════════════════
    private void handleLoad() {
        File file = openImageFile();
        if (file == null) return;
        try {
            BufferedImage raw = ImageIO.read(file);
            if (raw == null) throw new Exception("Cannot read image.");

            // Convert to RGB and store as original — never modify this
            originalImage = new BufferedImage(
                    raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = originalImage.createGraphics();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, raw.getWidth(), raw.getHeight());
            g2.drawImage(raw, 0, 0, null);
            g2.dispose();

            enhancedImage = originalImage; // start with original
            originalPanel.repaint();
            enhancedPanel.repaint();

            imageInfoLabel.setText(
                    file.getName() + "  |  "
                            + originalImage.getWidth() + " × " + originalImage.getHeight()
                            + " px  |  " + (file.length() / 1024) + " KB");

            statusLabel.setText("Image loaded — adjust sliders and click Apply Filters");

        } catch (Exception ex) {
            showError("Error loading image: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Apply all active filters to originalImage
    // ══════════════════════════════════════════════════════
    private void applyFilters() {
        if (originalImage == null) {
            showError("Please load an image first!");
            return;
        }

        int w = originalImage.getWidth();
        int h = originalImage.getHeight();

        // Start from a fresh copy of the original every time
        BufferedImage result = copyImage(originalImage);

        int    brightness = brightnessSlider.getValue();
        double contrast   = contrastSlider.getValue() / 100.0;
        double gamma      = gammaSlider.getValue() / 10.0;
        int    threshold  = thresholdSlider.getValue();

        // Build gamma lookup table for speed
        int[] gammaLUT = buildGammaLUT(gamma);

        // Apply pixel-level filters: brightness, contrast, gamma, grayscale
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = result.getRGB(x, y);
                int r   = (rgb >> 16) & 0xFF;
                int g   = (rgb >>  8) & 0xFF;
                int b   =  rgb        & 0xFF;

                // Brightness
                r = clamp(r + brightness);
                g = clamp(g + brightness);
                b = clamp(b + brightness);

                // Contrast — scale around midpoint 128
                r = clamp((int)((r - 128) * contrast + 128));
                g = clamp((int)((g - 128) * contrast + 128));
                b = clamp((int)((b - 128) * contrast + 128));

                // Gamma via lookup table
                r = gammaLUT[r];
                g = gammaLUT[g];
                b = gammaLUT[b];

                // Grayscale
                if (grayscaleBtn.isSelected()) {
                    int grey = (int)(0.299 * r + 0.587 * g + 0.114 * b);
                    r = grey; g = grey; b = grey;
                }

                // Threshold — make pure black or white
                if (thresholdBtn.isSelected()) {
                    int avg = (r + g + b) / 3;
                    int val = avg >= threshold ? 255 : 0;
                    r = val; g = val; b = val;
                }

                // Invert
                if (invertBtn.isSelected()) {
                    r = 255 - r;
                    g = 255 - g;
                    b = 255 - b;
                }

                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        // Edge detection (convolution — applied after pixel filters)
        if (edgeBtn.isSelected()) {
            result = applyEdgeDetection(result);
        }

        enhancedImage = result;
        enhancedPanel.repaint();

        // Build status description
        StringBuilder active = new StringBuilder("Filters: ");
        if (brightness != 0)             active.append("Brightness(").append(brightness).append(") ");
        if (contrast   != 1.0)          active.append("Contrast(").append(contrastSlider.getValue()).append("%) ");
        if (gamma      != 1.0)          active.append("Gamma(").append(String.format("%.1f", gamma)).append(") ");
        if (grayscaleBtn.isSelected())   active.append("Grayscale ");
        if (thresholdBtn.isSelected())   active.append("Threshold(").append(threshold).append(") ");
        if (invertBtn.isSelected())      active.append("Invert ");
        if (edgeBtn.isSelected())        active.append("EdgeDetect ");
        statusLabel.setText(active.toString());
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Build gamma lookup table (0-255 → 0-255)
    // ══════════════════════════════════════════════════════
    private int[] buildGammaLUT(double gamma) {
        int[] lut = new int[256];
        for (int i = 0; i < 256; i++) {
            lut[i] = clamp((int)(Math.pow(i / 255.0, gamma) * 255.0));
        }
        return lut;
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Edge detection using Sobel kernel
    // ══════════════════════════════════════════════════════
    private BufferedImage applyEdgeDetection(BufferedImage src) {
        // Sobel X kernel
        float[] sobelX = {
                -1, 0, 1,
                -2, 0, 2,
                -1, 0, 1
        };
        // Sobel Y kernel
        float[] sobelY = {
                -1, -2, -1,
                0,  0,  0,
                1,  2,  1
        };

        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                double gx = 0, gy = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int pixel = src.getRGB(x + kx, y + ky);
                        int gray  = (pixel >> 16) & 0xFF; // use red channel as gray
                        int ki    = (ky + 1) * 3 + (kx + 1);
                        gx += gray * sobelX[ki];
                        gy += gray * sobelY[ki];
                    }
                }
                int magnitude = clamp((int) Math.sqrt(gx * gx + gy * gy));
                result.setRGB(x, y, (magnitude << 16) | (magnitude << 8) | magnitude);
            }
        }
        return result;
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Reset all filters to defaults
    // ══════════════════════════════════════════════════════
    private void resetAll() {
        brightnessSlider.setValue(0);
        contrastSlider.setValue(100);
        gammaSlider.setValue(10);
        thresholdSlider.setValue(128);
        edgeBtn.setSelected(false);
        invertBtn.setSelected(false);
        grayscaleBtn.setSelected(false);
        thresholdBtn.setSelected(false);

        if (originalImage != null) {
            enhancedImage = originalImage;
            enhancedPanel.repaint();
        }
        statusLabel.setText("All filters reset to defaults.");
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Save enhanced result
    // ══════════════════════════════════════════════════════
    private void saveResult() {
        if (enhancedImage == null) {
            showError("Please apply filters first!");
            return;
        }
        File out = openSaveDialog();
        if (out == null) return;

        if (!out.getName().toLowerCase().endsWith(".png"))
            out = new File(out.getAbsolutePath() + ".png");
        try {
            ImageIO.write(enhancedImage, "PNG", out);
            JOptionPane.showMessageDialog(this,
                    "Saved: " + out.getName(), "Saved",
                    JOptionPane.INFORMATION_MESSAGE);
            statusLabel.setText("Saved: " + out.getName());
        } catch (Exception ex) {
            showError("Error saving: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════

    // Deep copy a BufferedImage
    private BufferedImage copyImage(BufferedImage src) {
        BufferedImage copy = new BufferedImage(
                src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = copy.createGraphics();
        g2.drawImage(src, 0, 0, null);
        g2.dispose();
        return copy;
    }

    // Clamp value to 0-255
    private int clamp(int val) {
        return Math.max(0, Math.min(255, val));
    }

    // Draw image fitting inside panel
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
            g2.setFont(new Font("Arial", Font.ITALIC, 12));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(hint,
                    Math.max(0, (panelW - fm.stringWidth(hint)) / 2),
                    (panelH + fm.getAscent()) / 2);
        }
    }

    // ══════════════════════════════════════════════════════
    // FILE CHOOSER HELPERS
    // ══════════════════════════════════════════════════════
    private File openImageFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load Image for Enhancement");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image Files (PNG, BMP, JPG, JPEG, GIF, TIFF)",
                "png", "bmp", "jpg", "jpeg", "gif", "tiff"));
        return fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
                ? fc.getSelectedFile() : null;
    }

    private File openSaveDialog() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Enhanced Image");
        fc.setSelectedFile(new File("enhanced_result.png"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "PNG Image", "png"));
        return fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION
                ? fc.getSelectedFile() : null;
    }

    // ══════════════════════════════════════════════════════
    // UI HELPERS
    // ══════════════════════════════════════════════════════
    private JSlider makeSlider(int min, int max, int value) {
        JSlider s = new JSlider(min, max, value);
        s.setBackground(new Color(245, 245, 250));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        return s;
    }

    private JPanel makeSliderRow(JSlider slider, JLabel valLabel) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBackground(new Color(245, 245, 250));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(slider,   BorderLayout.CENTER);
        row.add(valLabel, BorderLayout.EAST);
        return row;
    }

    private JLabel makeValLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", Font.BOLD, 11));
        l.setPreferredSize(new Dimension(42, 20));
        l.setForeground(new Color(50, 50, 150));
        l.setHorizontalAlignment(SwingConstants.RIGHT);
        return l;
    }

    private JLabel makeSectionLabel(String text) {
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

    private JToggleButton makeToggle(String text) {
        JToggleButton b = new JToggleButton(text);
        b.setFont(new Font("Arial", Font.PLAIN, 11));
        b.setBackground(new Color(200, 200, 220));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        return b;
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