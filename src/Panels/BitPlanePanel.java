package Panels;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class BitPlanePanel extends JPanel {

    // ── Fields ──
    private BufferedImage loadedImage;
    private BufferedImage currentPlaneImage;

    // Image display
    private JPanel        previewPanel;
    private JLabel        imageInfoLabel;

    // Controls
    private JComboBox<String> channelBox;
    private JToggleButton[]   planeButtons;
    private int               selectedPlane = 0;

    private JPanel[]      gridCells;
    private BufferedImage[] gridImages;
    private boolean       gridMode = false;

    // Card layout to switch between single and grid
    private JPanel      displayArea;
    private CardLayout  cardLayout;

    private JLabel statusLabel;

    private static final String CARD_SINGLE = "SINGLE";
    private static final String CARD_GRID   = "GRID";

    // ──────────────────────────────────────────────────────
    // Constructor
    // ──────────────────────────────────────────────────────
    public BitPlanePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(new Color(245, 245, 250));

        add(buildTopBar(),     BorderLayout.NORTH);
        add(buildMainArea(),   BorderLayout.CENTER);
        add(buildStatusBar(),  BorderLayout.SOUTH);
    }

    // ──────────────────────────────────────────────────────
    // TOP BAR — loader + channel + plane selector
    // ──────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel panel = new JPanel(new BorderLayout(10, 8));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("Bit Plane Viewer — Analyse each bit layer of the image"));

        // Row 1: load + info
        JPanel loadRow = new JPanel(new BorderLayout(10, 0));
        loadRow.setBackground(new Color(245, 245, 250));

        imageInfoLabel = new JLabel("Load a PNG or BMP image to begin bit plane analysis");
        imageInfoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        imageInfoLabel.setForeground(Color.GRAY);

        JButton loadBtn = makeBtn("📁 Load Image", new Color(70, 130, 180));
        loadBtn.addActionListener(e -> handleLoad());

        loadRow.add(imageInfoLabel, BorderLayout.CENTER);
        loadRow.add(loadBtn,        BorderLayout.EAST);

        // Row 2: channel + plane buttons + view toggle
        JPanel ctrlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        ctrlRow.setBackground(new Color(245, 245, 250));

        // Channel selector
        ctrlRow.add(makeBoldLabel("Channel:"));
        channelBox = new JComboBox<>(new String[]{"Red", "Green", "Blue"});
        channelBox.setFont(new Font("Arial", Font.PLAIN, 12));
        channelBox.setPreferredSize(new Dimension(90, 28));
        channelBox.addActionListener(e -> refreshCurrentView());
        ctrlRow.add(channelBox);

        ctrlRow.add(Box.createHorizontalStrut(12));
        ctrlRow.add(makeBoldLabel("Bit Plane:"));

        // Plane toggle buttons 0-7
        planeButtons = new JToggleButton[8];
        ButtonGroup planeGroup = new ButtonGroup();
        for (int i = 0; i < 8; i++) {
            JToggleButton btn = getJToggleButton(i);
            planeGroup.add(btn);
            planeButtons[i] = btn;
            ctrlRow.add(btn);
        }
        planeButtons[0].setSelected(true); // default to plane 0 (LSB)

        ctrlRow.add(Box.createHorizontalStrut(16));

        // View mode buttons
        JButton singleBtn = makeBtn("🔍 Single View", new Color(34, 139, 34));
        singleBtn.addActionListener(e -> exitGridMode());
        ctrlRow.add(singleBtn);

        JButton gridBtn = makeBtn("⊞ Grid View (all 8)", new Color(139, 90, 34));
        gridBtn.addActionListener(e -> enterGridMode());
        ctrlRow.add(gridBtn);

        JButton saveBtn = makeBtn("💾 Save", new Color(100, 100, 160));
        saveBtn.addActionListener(e -> saveCurrent());
        ctrlRow.add(saveBtn);

        panel.add(loadRow, BorderLayout.NORTH);
        panel.add(ctrlRow, BorderLayout.CENTER);
        return panel;
    }

    private JToggleButton getJToggleButton(int i) {
        String label = (i == 0) ? "0 (LSB)" : (i == 7) ? "7 (MSB)" : String.valueOf(i);
        JToggleButton btn = new JToggleButton(label);
        btn.setFont(new Font("Arial", Font.BOLD, 11));
        btn.setPreferredSize(new Dimension(65, 28));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBackground(new Color(200, 200, 220));
        final int plane = i;
        btn.addActionListener(e -> {
            selectedPlane = plane;
            if (gridMode) exitGridMode();
            refreshCurrentView();
        });
        return btn;
    }

    // ──────────────────────────────────────────────────────
    // MAIN DISPLAY AREA — CardLayout: single or grid
    // ──────────────────────────────────────────────────────
    private JPanel buildMainArea() {
        cardLayout  = new CardLayout();
        displayArea = new JPanel(cardLayout);
        displayArea.setBackground(new Color(245, 245, 250));

        // ── Single view ──
        previewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFit(g, currentPlaneImage, getWidth(), getHeight(),
                        "Load an image and select a bit plane");
            }
        };
        previewPanel.setBackground(Color.BLACK);
        previewPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));

        // ── Grid view ──
        // Grid view
        JPanel gridPanel = new JPanel(new GridLayout(2, 4, 6, 6));
        gridImages = new BufferedImage[8];
        gridCells  = new JPanel[8];
        gridPanel.setBackground(new Color(30, 30, 30));
        gridPanel.setBorder(new EmptyBorder(6, 6, 6, 6));

        for (int i = 0; i < 8; i++) {
            final int plane = i;
            JPanel cell = new JPanel(new BorderLayout(0, 2)) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                }
            };
            cell.setBackground(new Color(30, 30, 30));

            String lbl = (plane == 0) ? "Plane 0 (LSB)"
                    : (plane == 7) ? "Plane 7 (MSB)"
                    : "Plane " + plane;
            JLabel titleLbl = new JLabel(lbl, SwingConstants.CENTER);
            titleLbl.setFont(new Font("Arial", Font.BOLD, 10));
            titleLbl.setForeground(Color.LIGHT_GRAY);

            JPanel imageCell = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    drawFit(g, gridImages[plane], getWidth(), getHeight(), "");
                }
            };
            imageCell.setBackground(Color.BLACK);

            cell.add(titleLbl,  BorderLayout.NORTH);
            cell.add(imageCell, BorderLayout.CENTER);

            // Click grid cell to switch to single view of that plane
            imageCell.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    selectedPlane = plane;
                    planeButtons[plane].setSelected(true);
                    exitGridMode();
                }
            });
            imageCell.setCursor(new Cursor(Cursor.HAND_CURSOR));
            imageCell.setToolTipText("Click to view Plane " + plane + " in full size");

            gridCells[i] = cell;
            gridPanel.add(cell);
        }

        JScrollPane gridScroll = new JScrollPane(gridPanel);
        gridScroll.setBackground(new Color(30, 30, 30));
        gridScroll.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));

        displayArea.add(previewPanel, CARD_SINGLE);
        displayArea.add(gridScroll,   CARD_GRID);
        cardLayout.show(displayArea, CARD_SINGLE);

        return displayArea;
    }

    // ──────────────────────────────────────────────────────
    // STATUS BAR
    // ──────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 30, 60));
        panel.setBorder(new EmptyBorder(4, 10, 4, 10));

        statusLabel = new JLabel(
                "Ready — Load an image then select a channel and bit plane");
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(180, 220, 180));

        JLabel legendLabel = new JLabel(
                "⬛ = bit 0 (dark)   ⬜ = bit 1 (bright)   Pattern = hidden data!");
        legendLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        legendLabel.setForeground(new Color(180, 180, 220));

        panel.add(statusLabel,  BorderLayout.WEST);
        panel.add(legendLabel,  BorderLayout.EAST);
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

            // Convert to RGB
            loadedImage = new BufferedImage(
                    raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = loadedImage.createGraphics();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, raw.getWidth(), raw.getHeight());
            g2.drawImage(raw, 0, 0, null);
            g2.dispose();

            imageInfoLabel.setText(
                    file.getName() + "  |  "
                            + loadedImage.getWidth() + " × " + loadedImage.getHeight()
                            + " px  |  " + (file.length() / 1024) + " KB");

            // Reset to plane 0 single view
            selectedPlane = 0;
            planeButtons[0].setSelected(true);
            exitGridMode();
            refreshCurrentView();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error loading image: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Generate one bit plane image
    // ══════════════════════════════════════════════════════
    private BufferedImage generateBitPlane(int plane, int channel) {
        int w = loadedImage.getWidth();
        int h = loadedImage.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        int mask = 1 << plane; // e.g. plane 0 = 0b00000001, plane 7 = 0b10000000

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = loadedImage.getRGB(x, y);
                int r   = (rgb >> 16) & 0xFF;
                int g   = (rgb >>  8) & 0xFF;
                int b   =  rgb        & 0xFF;

                // Pick the selected channel value
                int channelVal = switch (channel) {
                    case 1  -> g;
                    case 2  -> b;
                    default -> r;
                };

                // If that bit is set → white (255), else → black (0)
                int brightness = ((channelVal & mask) != 0) ? 255 : 0;
                int pixel = (brightness << 16) | (brightness << 8) | brightness;
                result.setRGB(x, y, pixel);
            }
        }
        return result;
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Refresh single view with current plane + channel
    // ══════════════════════════════════════════════════════
    private void refreshCurrentView() {
        if (loadedImage == null) return;

        int channel = channelBox.getSelectedIndex(); // 0=R, 1=G, 2=B
        currentPlaneImage = generateBitPlane(selectedPlane, channel);

        String channelName = switch (channel) {
            case 1  -> "Green";
            case 2  -> "Blue";
            default -> "Red";
        };

        String planeName = (selectedPlane == 0) ? "0 (LSB)"
                : (selectedPlane == 7) ? "7 (MSB)"
                : String.valueOf(selectedPlane);

        // Colour the selected plane button green
        for (int i = 0; i < 8; i++) {
            planeButtons[i].setBackground(
                    i == selectedPlane ? new Color(34, 139, 34) : new Color(200, 200, 220));
            planeButtons[i].setForeground(
                    i == selectedPlane ? Color.WHITE : Color.BLACK);
        }

        previewPanel.repaint();

        // Analyse pattern
        String analysis = analysePattern(currentPlaneImage);
        statusLabel.setText(
                "Plane " + planeName + " | " + channelName + " channel | " + analysis);
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Enter grid view — generate all 8 planes
    // ══════════════════════════════════════════════════════
    private void enterGridMode() {
        if (loadedImage == null) {
            JOptionPane.showMessageDialog(this,
                    "Please load an image first!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        gridMode = true;
        int channel = channelBox.getSelectedIndex();

        // Generate all 8 planes
        for (int i = 0; i < 8; i++) {
            gridImages[i] = generateBitPlane(i, channel);
        }

        // Repaint all grid cells
        for (JPanel cell : gridCells) {
            cell.repaint();
        }

        String channelName = switch (channel) {
            case 1  -> "Green";
            case 2  -> "Blue";
            default -> "Red";
        };

        cardLayout.show(displayArea, CARD_GRID);
        statusLabel.setText(
                "Grid View — All 8 bit planes | " + channelName
                        + " channel | Click any plane to enlarge");
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Exit grid view → single view
    // ══════════════════════════════════════════════════════
    private void exitGridMode() {
        gridMode = false;
        cardLayout.show(displayArea, CARD_SINGLE);
        refreshCurrentView();
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Analyse how random/structured the plane looks
    // ══════════════════════════════════════════════════════
    private String analysePattern(BufferedImage plane) {
        int w          = plane.getWidth();
        int h          = plane.getHeight();
        long whitePixels = 0;
        long total       = (long) w * h;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((plane.getRGB(x, y) & 0xFF) == 255) whitePixels++;
            }
        }

        double ratio = (double) whitePixels / total;
        // Natural LSB plane ≈ 50% white. Hidden data also ≈ 50% but structured.
        // High planes (6,7) should be structured — mostly white or black.
        double deviation = Math.abs(ratio - 0.5);

        if (deviation > 0.35) {
            return String.format("%.1f%% white — Highly structured (normal for high planes)",
                    ratio * 100);
        } else if (deviation > 0.15) {
            return String.format("%.1f%% white — Semi-structured", ratio * 100);
        } else {
            return String.format(
                    "%.1f%% white — Near 50/50 (random noise — check for patterns visually!)",
                    ratio * 100);
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Save current plane image
    // ══════════════════════════════════════════════════════
    private void saveCurrent() {
        if (currentPlaneImage == null) {
            JOptionPane.showMessageDialog(this,
                    "Please generate a bit plane first!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String channelName = switch (channelBox.getSelectedIndex()) {
            case 1  -> "green";
            case 2  -> "blue";
            default -> "red";
        };
        String defaultName = "plane" + selectedPlane + "_" + channelName + ".png";

        File out = openSaveDialog(defaultName);
        if (out == null) return;

        if (!out.getName().toLowerCase().endsWith(".png"))
            out = new File(out.getAbsolutePath() + ".png");
        try {
            ImageIO.write(currentPlaneImage, "PNG", out);
            JOptionPane.showMessageDialog(this,
                    "Saved: " + out.getName(), "Saved",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error saving: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── File chooser helpers ──

    // Opens an image file chooser — returns chosen File or null if cancelled
    private File openImageFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load Image for Bit Plane Analysis");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image Files (PNG, BMP, JPG, JPEG, GIF, TIFF)",
                "png", "bmp", "jpg", "jpeg", "gif", "tiff"));
        return fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
                ? fc.getSelectedFile() : null;
    }

    // Opens a PNG save dialog — returns chosen File or null if cancelled
    private File openSaveDialog(String defaultFileName) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Bit Plane Image");
        fc.setSelectedFile(new File(defaultFileName));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "PNG Image", "png"));
        return fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION
                ? fc.getSelectedFile() : null;
    }

    // ══════════════════════════════════════════════════════
    // UI HELPER: Draw image scaled to fit panel
    // ══════════════════════════════════════════════════════
    private void drawFit(Graphics g, BufferedImage img,
                         int panelW, int panelH, String hint) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR); // sharp pixels for bit planes
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        if (img != null) {
            double scale = Math.min(
                    (double) panelW / img.getWidth(),
                    (double) panelH / img.getHeight());
            int dw = (int)(img.getWidth()  * scale);
            int dh = (int)(img.getHeight() * scale);
            g2.drawImage(img, (panelW - dw) / 2, (panelH - dh) / 2, dw, dh, null);
        } else if (!hint.isEmpty()) {
            g2.setColor(Color.GRAY);
            g2.setFont(new Font("Arial", Font.ITALIC, 12));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(hint,
                    Math.max(0, (panelW - fm.stringWidth(hint)) / 2),
                    (panelH + fm.getAscent()) / 2);
        }
    }

    // ══════════════════════════════════════════════════════
    // UI HELPERS: Buttons and labels
    // ══════════════════════════════════════════════════════
    private JButton makeBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Arial", Font.BOLD, 11));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JLabel makeBoldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", Font.BOLD, 12));
        return l;
    }
}