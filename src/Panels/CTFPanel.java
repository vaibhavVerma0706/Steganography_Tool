package Panels;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;

public class CTFPanel extends JPanel {

    // ── Class-level fields ──
    private BufferedImage loadedImage;
    private JLabel        imageDisplayLabel;
    private JLabel        imageInfoLabel;

    // LSB Viewer
    private JPanel            lsbImagePanel;
    private BufferedImage     lsbImage;
    private JComboBox<String> lsbChannelBox;

    // Pixel Inspector
    private JPanel        pixelImagePanel;
    private BufferedImage pixelDisplayImage;
    private JTextArea     pixelInfoArea;

    // RGB Splitter
    private JPanel        redPanel;
    private JPanel        greenPanel;
    private JPanel        bluePanel;
    private BufferedImage redImage;
    private BufferedImage greenImage;
    private BufferedImage blueImage;

    // Hex Viewer
    private JTextArea hexArea;
    private JSpinner  hexRowsSpinner;

    // ──────────────────────────────────────────────────────
    // Constructor
    // ──────────────────────────────────────────────────────
    public CTFPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(new Color(245, 245, 250));
        add(buildTopSection(), BorderLayout.NORTH);
        add(buildToolsTabs(),  BorderLayout.CENTER);
    }

    // ──────────────────────────────────────────────────────
    // Helper: load + convert image to RGB
    // ──────────────────────────────────────────────────────
    private BufferedImage loadRGB(File file) throws Exception {
        BufferedImage raw = ImageIO.read(file);
        if (raw == null) throw new Exception("Cannot read file. Use PNG or BMP.");
        BufferedImage rgb = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = rgb.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, raw.getWidth(), raw.getHeight());
        g2.drawImage(raw, 0, 0, null);
        g2.dispose();
        return rgb;
    }

    // ──────────────────────────────────────────────────────
    // Helper: open PNG/BMP file chooser
    // ──────────────────────────────────────────────────────
    private File chooseFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load Image");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image Files (PNG, BMP, JPG, JPEG, GIF, TIFF)",
                "png", "bmp", "jpg", "jpeg", "gif", "tiff"));
        return fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
                ? fc.getSelectedFile() : null;
    }

    // ──────────────────────────────────────────────────────
    // CORE drawing helper — used by ALL image panels.
    // Scales image to fit panel exactly (width AND height),
    // maintains aspect ratio, centres in panel.
    // Uses BICUBIC — no quality loss.
    // ──────────────────────────────────────────────────────
    private void drawFitImage(Graphics g, BufferedImage img,
                              int panelW, int panelH, String emptyHint) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        if (img != null) {
            // Scale so image fits completely inside the panel (both W and H)
            double scaleW = (double) panelW / img.getWidth();
            double scaleH = (double) panelH / img.getHeight();
            double scale  = Math.min(scaleW, scaleH); // ensures BOTH sides fit
            int drawW = (int)(img.getWidth()  * scale);
            int drawH = (int)(img.getHeight() * scale);
            // Centre the image inside the panel
            int x = (panelW - drawW) / 2;
            int y = (panelH - drawH) / 2;
            g2.drawImage(img, x, y, drawW, drawH, null);
        } else {
            g2.setColor(Color.GRAY);
            g2.setFont(new Font("Arial", Font.ITALIC, 12));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(emptyHint,
                    Math.max(0, (panelW - fm.stringWidth(emptyHint)) / 2),
                    (panelH + fm.getAscent()) / 2);
        }
    }

    // ──────────────────────────────────────────────────────
    // Helper: dark terminal-style text area
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

    private JButton makeGreenBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setBackground(new Color(34, 139, 34));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton makeBlueBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setBackground(new Color(70, 130, 180));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel makePreviewLabel(String hint, int w, int h) {
        JLabel lbl = new JLabel(hint, SwingConstants.CENTER);
        lbl.setPreferredSize(new Dimension(w, h));
        lbl.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        lbl.setBackground(Color.WHITE);
        lbl.setOpaque(true);
        lbl.setFont(new Font("Arial", Font.ITALIC, 11));
        lbl.setForeground(Color.GRAY);
        return lbl;
    }

    // ──────────────────────────────────────────────────────
    // Top section: image upload bar
    // ──────────────────────────────────────────────────────
    private JPanel buildTopSection() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("Load Image for CTF Analysis"));

        imageDisplayLabel = makePreviewLabel("No Image Loaded", 200, 130);

        imageInfoLabel = new JLabel("Upload a suspicious image to begin CTF analysis");
        imageInfoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        imageInfoLabel.setForeground(Color.GRAY);

        JLabel hint = new JLabel("Supports PNG and BMP formats");
        hint.setFont(new Font("Arial", Font.ITALIC, 11));
        hint.setForeground(new Color(180, 100, 0));

        JButton uploadBtn = makeBlueBtn("📁 Load Image for Analysis");
        uploadBtn.addActionListener(e -> handleLoad());

        JPanel right = new JPanel(new BorderLayout(5, 5));
        right.setBackground(new Color(245, 245, 250));
        right.add(imageInfoLabel, BorderLayout.NORTH);
        right.add(hint,           BorderLayout.CENTER);
        right.add(uploadBtn,      BorderLayout.SOUTH);

        panel.add(imageDisplayLabel, BorderLayout.WEST);
        panel.add(right,             BorderLayout.CENTER);
        return panel;
    }

    // ──────────────────────────────────────────────────────
    // Tabs
    // ──────────────────────────────────────────────────────
    private JTabbedPane buildToolsTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Arial", Font.BOLD, 12));
        tabs.setBackground(new Color(245, 245, 250));
        tabs.addTab("🔬 LSB Viewer",      buildLSBTab());
        tabs.addTab("🔎 Pixel Inspector", buildPixelTab());
        tabs.addTab("🎨 RGB Splitter",    buildRGBTab());
        tabs.addTab("🔢 Hex Viewer",      buildHexTab());
        tabs.setToolTipTextAt(0, "View LSB bit plane to detect hidden data");
        tabs.setToolTipTextAt(1, "Click any pixel to see RGB, hex and binary values");
        tabs.setToolTipTextAt(2, "Split image into Red, Green, Blue channels");
        tabs.setToolTipTextAt(3, "View raw hex bytes of the image file");
        return tabs;
    }

    // ══════════════════════════════════════════════════════
    // TAB 1 — LSB PLANE VIEWER
    // ══════════════════════════════════════════════════════
    private JPanel buildLSBTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel desc = new JLabel(
                "Extracts the least significant bit of each pixel — hidden data appears as a visible pattern",
                SwingConstants.CENTER);
        desc.setFont(new Font("Arial", Font.ITALIC, 11));
        desc.setForeground(new Color(80, 80, 80));

        lsbChannelBox = new JComboBox<>(
                new String[]{"All Channels", "Red Only", "Green Only", "Blue Only"});
        lsbChannelBox.setFont(new Font("Arial", Font.PLAIN, 12));

        JButton genBtn  = makeGreenBtn("🔬 Generate LSB Plane");
        JButton saveBtn = makeBlueBtn("💾 Save Result");
        genBtn.addActionListener(e  -> generateLSBPlane());
        saveBtn.addActionListener(e -> saveLSBResult());

        JPanel ctrlRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        ctrlRow.setBackground(new Color(245, 245, 250));
        ctrlRow.add(new JLabel("Channel:"));
        ctrlRow.add(lsbChannelBox);
        ctrlRow.add(genBtn);
        ctrlRow.add(saveBtn);

        // Custom panel — stretches to fill CENTER completely
        lsbImagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFitImage(g, lsbImage, getWidth(), getHeight(),
                        "Load an image and click Generate LSB Plane");
            }
        };
        lsbImagePanel.setBackground(Color.WHITE);
        lsbImagePanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.setBackground(new Color(245, 245, 250));
        top.add(desc,    BorderLayout.NORTH);
        top.add(ctrlRow, BorderLayout.CENTER);

        panel.add(top,           BorderLayout.NORTH);
        panel.add(lsbImagePanel, BorderLayout.CENTER);
        return panel;
    }

    // ══════════════════════════════════════════════════════
    // TAB 2 — PIXEL INSPECTOR
    // ══════════════════════════════════════════════════════
    private JPanel buildPixelTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel desc = new JLabel(
                "Click any pixel on the image to see its RGB, HEX and binary values",
                SwingConstants.CENTER);
        desc.setFont(new Font("Arial", Font.ITALIC, 11));
        desc.setForeground(new Color(80, 80, 80));

        // Custom panel fills CENTER — image always fits fully
        pixelImagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFitImage(g, pixelDisplayImage, getWidth(), getHeight(),
                        "Load an image then click any pixel");
            }
        };
        pixelImagePanel.setBackground(Color.WHITE);
        pixelImagePanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        pixelImagePanel.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        pixelImagePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { inspectPixel(e.getX(), e.getY()); }
        });

        pixelInfoArea = makeDarkArea("""
                Click any pixel on the image
                to inspect its values here...

                You will see:
                  Position  (X, Y)
                  RGB       values
                  HEX       values
                  Binary    values
                  LSB       bits""");

        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBackground(new Color(245, 245, 250));
        rightPanel.setBorder(new TitledBorder("Pixel Analysis"));
        rightPanel.setPreferredSize(new Dimension(230, 0));
        rightPanel.add(new JScrollPane(pixelInfoArea), BorderLayout.CENTER);

        panel.add(desc,            BorderLayout.NORTH);
        panel.add(pixelImagePanel, BorderLayout.CENTER);
        panel.add(rightPanel,      BorderLayout.EAST);
        return panel;
    }

    // ══════════════════════════════════════════════════════
    // TAB 3 — RGB CHANNEL SPLITTER
    // ══════════════════════════════════════════════════════
    private JPanel buildRGBTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel desc = new JLabel(
                "Splits image into Red, Green and Blue channels — hidden data may appear in one channel",
                SwingConstants.CENTER);
        desc.setFont(new Font("Arial", Font.ITALIC, 11));
        desc.setForeground(new Color(80, 80, 80));

        JButton splitBtn = makeGreenBtn("🎨 Split RGB Channels");
        splitBtn.addActionListener(e -> generateRGBSplit());

        JPanel ctrlRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        ctrlRow.setBackground(new Color(245, 245, 250));
        ctrlRow.add(splitBtn);

        // Each channel panel: custom paintComponent — image always fits inside frame
        redPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFitImage(g, redImage, getWidth(), getHeight(), "Red Channel");
            }
        };
        greenPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFitImage(g, greenImage, getWidth(), getHeight(), "Green Channel");
            }
        };
        bluePanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFitImage(g, blueImage, getWidth(), getHeight(), "Blue Channel");
            }
        };

        for (JPanel p : new JPanel[]{redPanel, greenPanel, bluePanel}) {
            p.setBackground(Color.WHITE);
            p.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        }

        JPanel channels = new JPanel(new GridLayout(1, 3, 10, 0));
        channels.setBackground(new Color(245, 245, 250));
        channels.add(wrapInTitle(redPanel,   "Red Channel"));
        channels.add(wrapInTitle(greenPanel, "Green Channel"));
        channels.add(wrapInTitle(bluePanel,  "Blue Channel"));

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.setBackground(new Color(245, 245, 250));
        top.add(desc,    BorderLayout.NORTH);
        top.add(ctrlRow, BorderLayout.CENTER);

        panel.add(top,      BorderLayout.NORTH);
        panel.add(channels, BorderLayout.CENTER);
        return panel;
    }

    // ══════════════════════════════════════════════════════
    // TAB 4 — HEX VIEWER
    // ══════════════════════════════════════════════════════
    private JPanel buildHexTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel desc = new JLabel(
                "View raw hex bytes of the image — find hidden text or file signatures",
                SwingConstants.CENTER);
        desc.setFont(new Font("Arial", Font.ITALIC, 11));
        desc.setForeground(new Color(80, 80, 80));

        hexRowsSpinner = new JSpinner(new SpinnerNumberModel(32, 8, 512, 8));
        hexRowsSpinner.setPreferredSize(new Dimension(70, 25));

        JButton genBtn    = makeGreenBtn("🔢 Generate Hex Dump");
        JButton searchBtn = makeBlueBtn("🔍 Search ASCII");
        genBtn.addActionListener(e    -> generateHexDump());
        searchBtn.addActionListener(e -> searchASCII());

        JPanel ctrlRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        ctrlRow.setBackground(new Color(245, 245, 250));
        ctrlRow.add(new JLabel("Rows:"));
        ctrlRow.add(hexRowsSpinner);
        ctrlRow.add(genBtn);
        ctrlRow.add(searchBtn);

        hexArea = makeDarkArea("""
                Load an image and click Generate Hex Dump

                Format:
                Offset    Hex Values              ASCII
                ────────────────────────────────────────
                00000000  89 50 4E 47 0D 0A ...   .PNG....""");

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.setBackground(new Color(245, 245, 250));
        top.add(desc,    BorderLayout.NORTH);
        top.add(ctrlRow, BorderLayout.CENTER);

        panel.add(top,                      BorderLayout.NORTH);
        panel.add(new JScrollPane(hexArea), BorderLayout.CENTER);
        return panel;
    }

    // ──────────────────────────────────────────────────────
    // wrapInTitle(): wraps a JPanel inside a titled border panel
    // ──────────────────────────────────────────────────────
    private JPanel wrapInTitle(JPanel inner, String title) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(245, 245, 250));
        wrapper.setBorder(new TitledBorder(title));
        wrapper.add(inner, BorderLayout.CENTER);
        return wrapper;
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Image Load
    // ══════════════════════════════════════════════════════
    private void handleLoad() {
        File file = chooseFile();
        if (file == null) return;
        try {
            loadedImage = loadRGB(file);

            // Top thumbnail — bicubic quality
            int tw = 200, th = 130;
            double scale = Math.min((double) tw / loadedImage.getWidth(),
                    (double) th / loadedImage.getHeight());
            int nw = (int)(loadedImage.getWidth()  * scale);
            int nh = (int)(loadedImage.getHeight() * scale);
            BufferedImage thumb = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = thumb.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(loadedImage, 0, 0, nw, nh, null);
            g2.dispose();
            imageDisplayLabel.setIcon(new ImageIcon(thumb));
            imageDisplayLabel.setText("");

            imageInfoLabel.setText(
                    file.getName()
                            + " | " + loadedImage.getWidth()
                            + " x " + loadedImage.getHeight()
                            + " px | " + (file.length() / 1024) + " KB");
            resetTools();
        } catch (Exception ex) {
            showErr("Error loading image: " + ex.getMessage());
        }
    }

    private void resetTools() {
        lsbImage = null;
        lsbImagePanel.repaint();

        pixelDisplayImage = loadedImage;
        pixelImagePanel.repaint();
        pixelInfoArea.setText("Click any pixel on the image to inspect it...");

        redImage = null; greenImage = null; blueImage = null;
        redPanel.repaint(); greenPanel.repaint(); bluePanel.repaint();

        hexArea.setText("Click Generate Hex Dump to analyze");
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: LSB Plane
    // ══════════════════════════════════════════════════════
    private void generateLSBPlane() {
        if (loadedImage == null) { showErr("Please load an image first!"); return; }

        String channel = (String) lsbChannelBox.getSelectedItem();
        if (channel == null) channel = "All Channels";

        int w = loadedImage.getWidth(), h = loadedImage.getHeight();
        BufferedImage lsbImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb  = loadedImage.getRGB(x, y);
                int r    = (rgb >> 16) & 0xFF;
                int g    = (rgb >>  8) & 0xFF;
                int b    =  rgb        & 0xFF;
                int lsbR = (r & 1) * 255;
                int lsbG = (g & 1) * 255;
                int lsbB = (b & 1) * 255;

                int pixel = switch (channel) {
                    case "Red Only"   -> (lsbR << 16);
                    case "Green Only" -> (lsbG <<  8);
                    case "Blue Only"  ->  lsbB;
                    default -> {
                        int avg = (lsbR + lsbG + lsbB) / 3;
                        yield (avg << 16) | (avg << 8) | avg;
                    }
                };
                lsbImg.setRGB(x, y, pixel);
            }
        }
        lsbImage = lsbImg;
        lsbImagePanel.repaint();
    }

    private void saveLSBResult() {
        if (lsbImage == null) { showErr("Please generate the LSB plane first!"); return; }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save LSB Plane Image");
        fc.setSelectedFile(new File("lsb_plane.png"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG Image", "png"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File out = fc.getSelectedFile();
            if (!out.getName().toLowerCase().endsWith(".png"))
                out = new File(out.getAbsolutePath() + ".png");
            try {
                ImageIO.write(lsbImage, "PNG", out);
                showOk("LSB plane saved: " + out.getName());
            } catch (Exception ex) {
                showErr("Error saving: " + ex.getMessage());
            }
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Pixel Inspector
    // ══════════════════════════════════════════════════════
    private void inspectPixel(int cx, int cy) {
        if (loadedImage == null) { showErr("Please load an image first!"); return; }

        // Map click → actual image pixel using exact same scale as paintComponent
        int panelW = pixelImagePanel.getWidth();
        int panelH = pixelImagePanel.getHeight();
        int iw = loadedImage.getWidth();
        int ih = loadedImage.getHeight();

        double scale = Math.min((double) panelW / iw, (double) panelH / ih);
        int drawW = (int)(iw * scale);
        int drawH = (int)(ih * scale);
        int offX  = (panelW - drawW) / 2;
        int offY  = (panelH - drawH) / 2;

        int ax = (int)((cx - offX) / scale);
        int ay = (int)((cy - offY) / scale);
        ax = Math.max(0, Math.min(ax, iw - 1));
        ay = Math.max(0, Math.min(ay, ih - 1));

        int rgb = loadedImage.getRGB(ax, ay);
        int r   = (rgb >> 16) & 0xFF;
        int g   = (rgb >>  8) & 0xFF;
        int b   =  rgb        & 0xFF;

        pixelInfoArea.setText(
                "═══════════════════════════\n"
                        + " PIXEL INSPECTOR RESULTS\n"
                        + "═══════════════════════════\n\n"
                        + " Position:\n"
                        + "   X = " + ax + "   Y = " + ay + "\n\n"
                        + " RGB Values:\n"
                        + "   R = " + r + "\n"
                        + "   G = " + g + "\n"
                        + "   B = " + b + "\n\n"
                        + " HEX Values:\n"
                        + "   R = 0x" + String.format("%02X", r) + "\n"
                        + "   G = 0x" + String.format("%02X", g) + "\n"
                        + "   B = 0x" + String.format("%02X", b) + "\n"
                        + "   Full = " + String.format("#%02X%02X%02X", r, g, b) + "\n\n"
                        + " Binary Values:\n"
                        + "   R = " + toBin(r) + "\n"
                        + "   G = " + toBin(g) + "\n"
                        + "   B = " + toBin(b) + "\n\n"
                        + " LSB (Hidden Bits):\n"
                        + "   R LSB = " + (r & 1) + "\n"
                        + "   G LSB = " + (g & 1) + "\n"
                        + "   B LSB = " + (b & 1) + "\n"
                        + "   Bits  = " + (r & 1) + (g & 1) + (b & 1) + "\n"
        );
    }

    private String toBin(int val) {
        return String.format("%8s", Integer.toBinaryString(val)).replace(' ', '0');
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: RGB Splitter
    // ══════════════════════════════════════════════════════
    private void generateRGBSplit() {
        if (loadedImage == null) { showErr("Please load an image first!"); return; }

        int w = loadedImage.getWidth(), h = loadedImage.getHeight();
        BufferedImage rImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        BufferedImage gImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        BufferedImage bImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = loadedImage.getRGB(x, y);
                int r   = (rgb >> 16) & 0xFF;
                int g   = (rgb >>  8) & 0xFF;
                int bv  =  rgb        & 0xFF;
                rImg.setRGB(x, y, (r  << 16));
                gImg.setRGB(x, y, (g  <<  8));
                bImg.setRGB(x, y,  bv);
            }
        }
        redImage   = rImg;
        greenImage = gImg;
        blueImage  = bImg;
        redPanel.repaint();
        greenPanel.repaint();
        bluePanel.repaint();
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Hex Dump
    // ══════════════════════════════════════════════════════
    private void generateHexDump() {
        if (loadedImage == null) { showErr("Please load an image first!"); return; }

        int rows        = (int) hexRowsSpinner.getValue();
        int w           = loadedImage.getWidth();
        int h           = loadedImage.getHeight();
        int bytesPerRow = 8;
        int offset      = 0;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-10s %-27s %s%n", "Offset", "Hex Values", "ASCII"));
        sb.append("─".repeat(55)).append("\n");

        outer:
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x += bytesPerRow) {
                if (offset / bytesPerRow >= rows) break outer;
                StringBuilder hex   = new StringBuilder();
                StringBuilder ascii = new StringBuilder();
                for (int i = 0; i < bytesPerRow && (x + i) < w; i++) {
                    int rgb = loadedImage.getRGB(x + i, y);
                    int r   = (rgb >> 16) & 0xFF;
                    int g   = (rgb >>  8) & 0xFF;
                    int bv  =  rgb        & 0xFF;
                    hex.append(String.format("%02X %02X %02X ", r, g, bv));
                    ascii.append(printable(r));
                    ascii.append(printable(g));
                    ascii.append(printable(bv));
                }
                sb.append(String.format("%08X  %-27s %s%n", offset, hex, ascii));
                offset += bytesPerRow;
            }
        }
        sb.append("\n── End of dump (").append(rows).append(" rows) ──");
        hexArea.setText(sb.toString());
        hexArea.setCaretPosition(0);
    }

    private void searchASCII() {
        if (loadedImage == null) { showErr("Please load an image first!"); return; }

        int w = loadedImage.getWidth(), h = loadedImage.getHeight();
        StringBuilder found   = new StringBuilder();
        StringBuilder current = new StringBuilder();
        int count = 0;

        found.append("═══════════════════════════\n");
        found.append(" ASCII STRING SEARCH\n");
        found.append("═══════════════════════════\n\n");

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = loadedImage.getRGB(x, y);
                int r   = (rgb >> 16) & 0xFF;
                int g   = (rgb >>  8) & 0xFF;
                int b   =  rgb        & 0xFF;

                if (isPrintable(r) && isPrintable(g) && isPrintable(b)) {
                    current.append((char) r).append((char) g).append((char) b);
                } else {
                    if (current.length() >= 4) {
                        found.append("Found: \"").append(current)
                                .append("\" at pixel (").append(x).append(",").append(y).append(")\n");
                        count++;
                    }
                    current.setLength(0);
                }
            }
        }

        found.append(count == 0
                ? "\nNo readable ASCII strings found.\n"
                : "\nTotal strings found: " + count + "\n");
        hexArea.setText(found.toString());
    }

    private char    printable(int v)   { return (v >= 32 && v <= 126) ? (char) v : '.'; }
    private boolean isPrintable(int v) { return v >= 32 && v <= 126; }

    private void showErr(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error",   JOptionPane.ERROR_MESSAGE);
    }

    private void showOk(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
}