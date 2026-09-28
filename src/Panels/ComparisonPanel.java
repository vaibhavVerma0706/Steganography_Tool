package Panels;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class ComparisonPanel extends JPanel {

    // -------------------------------------------------------
    // Class level fields
    // -------------------------------------------------------
    private JPanel        originalImagePanel;   // custom painted
    private JPanel        encodedImagePanel;    // custom painted
    private BufferedImage originalImage;
    private BufferedImage encodedImage;

    private JLabel originalInfoLabel;
    private JLabel encodedInfoLabel;
    private JLabel diffResultLabel;
    private JButton compareButton;

    // -------------------------------------------------------
    // Constructor
    // -------------------------------------------------------
    public ComparisonPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(new Color(245, 245, 250));

        add(buildTopSection(),    BorderLayout.NORTH);
        add(buildMiddleSection(), BorderLayout.CENTER);
        add(buildBottomSection(), BorderLayout.SOUTH);
    }

    // -------------------------------------------------------
    // CORE: Draw image into any panel — HD bicubic, aspect-safe
    // Same approach used in CTFPanel
    // -------------------------------------------------------
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
            // Scale so image fits fully inside panel — both W and H respected
            double scaleW = (double) panelW / img.getWidth();
            double scaleH = (double) panelH / img.getHeight();
            double scale  = Math.min(scaleW, scaleH);
            int drawW = (int)(img.getWidth()  * scale);
            int drawH = (int)(img.getHeight() * scale);
            // Centre inside the panel
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

    // -------------------------------------------------------
    // Buttons
    // -------------------------------------------------------
    private JButton makeStyledButton(String text, Color bgColor, Dimension size) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (size != null) button.setPreferredSize(size);
        return button;
    }

    // -------------------------------------------------------
    // Info label below each image
    // -------------------------------------------------------
    private JLabel buildInfoLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.PLAIN, 11));
        label.setForeground(Color.GRAY);
        return label;
    }

    // -------------------------------------------------------
    // Load image as RGB BufferedImage
    // -------------------------------------------------------
    private BufferedImage loadImageAsRGB(File file) throws Exception {
        BufferedImage loaded = ImageIO.read(file);
        if (loaded == null) throw new Exception("Could not read image. Please use a PNG file.");
        BufferedImage rgb = new BufferedImage(
                loaded.getWidth(), loaded.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.drawImage(loaded, 0, 0, Color.WHITE, null);
        g.dispose();
        return rgb;
    }

    // -------------------------------------------------------
    // buildTopSection() — Title and instructions
    // -------------------------------------------------------
    private JPanel buildTopSection() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("Image Comparison Tool"));

        JLabel titleLabel = new JLabel(
                "Upload both images to compare them side by side", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        titleLabel.setForeground(new Color(80, 80, 80));

        JLabel hintLabel = new JLabel(
                "Tip: Both images should look identical to the human eye", SwingConstants.CENTER);
        hintLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        hintLabel.setForeground(new Color(180, 100, 0));

        panel.add(titleLabel, BorderLayout.CENTER);
        panel.add(hintLabel,  BorderLayout.SOUTH);
        return panel;
    }

    // -------------------------------------------------------
    // buildMiddleSection() — Two image previews side by side
    // -------------------------------------------------------
    private JPanel buildMiddleSection() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 15, 0));
        panel.setBackground(new Color(245, 245, 250));
        panel.add(buildOriginalPanel());
        panel.add(buildEncodedPanel());
        return panel;
    }

    // -------------------------------------------------------
    // buildOriginalPanel() — Left: original image
    // -------------------------------------------------------
    private JPanel buildOriginalPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(8, 8));
        wrapper.setBackground(new Color(245, 245, 250));
        wrapper.setBorder(new TitledBorder("Original Image"));

        originalInfoLabel = buildInfoLabel("Upload the original image");

        // Custom painted panel — fills CENTER, HD quality
        originalImagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFitImage(g, originalImage, getWidth(), getHeight(), "No Image Selected");
            }
        };
        originalImagePanel.setBackground(Color.WHITE);
        originalImagePanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        originalImagePanel.setPreferredSize(new Dimension(0, 220));

        JButton uploadBtn = makeStyledButton("📁 Upload Original",
                new Color(70, 130, 180), null);
        uploadBtn.addActionListener(e -> handleUploadOriginal());

        wrapper.add(originalInfoLabel,  BorderLayout.NORTH);
        wrapper.add(originalImagePanel, BorderLayout.CENTER);
        wrapper.add(uploadBtn,          BorderLayout.SOUTH);
        return wrapper;
    }

    // -------------------------------------------------------
    // buildEncodedPanel() — Right: encoded image
    // -------------------------------------------------------
    private JPanel buildEncodedPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(8, 8));
        wrapper.setBackground(new Color(245, 245, 250));
        wrapper.setBorder(new TitledBorder("Encoded Image"));

        encodedInfoLabel = buildInfoLabel("Upload the encoded image");

        // Custom painted panel — fills CENTER, HD quality
        encodedImagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFitImage(g, encodedImage, getWidth(), getHeight(), "No Image Selected");
            }
        };
        encodedImagePanel.setBackground(Color.WHITE);
        encodedImagePanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        encodedImagePanel.setPreferredSize(new Dimension(0, 220));

        JButton uploadBtn = makeStyledButton("📁 Upload Encoded",
                new Color(70, 130, 180), null);
        uploadBtn.addActionListener(e -> handleUploadEncoded());

        wrapper.add(encodedInfoLabel,  BorderLayout.NORTH);
        wrapper.add(encodedImagePanel, BorderLayout.CENTER);
        wrapper.add(uploadBtn,         BorderLayout.SOUTH);
        return wrapper;
    }

    // -------------------------------------------------------
    // buildBottomSection() — Compare button and result
    // -------------------------------------------------------
    private JPanel buildBottomSection() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("Comparison Result"));

        diffResultLabel = new JLabel(
                "Upload both images then click Compare", SwingConstants.CENTER);
        diffResultLabel.setFont(new Font("Arial", Font.BOLD, 13));
        diffResultLabel.setForeground(Color.GRAY);
        diffResultLabel.setBorder(new EmptyBorder(10, 10, 10, 10));

        compareButton = makeStyledButton("🔍 Compare Images",
                new Color(34, 139, 34), new Dimension(200, 40));
        compareButton.addActionListener(e -> handleCompare());

        JButton resetButton = makeStyledButton("🔄 Reset",
                new Color(169, 169, 169), new Dimension(120, 40));
        resetButton.addActionListener(e -> handleReset());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        buttonRow.setBackground(new Color(245, 245, 250));
        buttonRow.add(compareButton);
        buttonRow.add(resetButton);

        panel.add(diffResultLabel, BorderLayout.CENTER);
        panel.add(buttonRow,       BorderLayout.SOUTH);
        return panel;
    }

    // -------------------------------------------------------
    // handleUploadOriginal()
    // -------------------------------------------------------
    private void handleUploadOriginal() {
        File file = openFileChooser("Select Original Image");
        if (file == null) return;
        try {
            originalImage = loadImageAsRGB(file);
            originalInfoLabel.setText(
                    file.getName() + " | "
                            + originalImage.getWidth() + " x "
                            + originalImage.getHeight() + " px");
            originalImagePanel.repaint();   // triggers paintComponent — HD draw
            resetResult();
        } catch (Exception ex) {
            showError("Error loading original image: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------
    // handleUploadEncoded()
    // -------------------------------------------------------
    private void handleUploadEncoded() {
        File file = openFileChooser("Select Encoded Image");
        if (file == null) return;
        try {
            encodedImage = loadImageAsRGB(file);
            encodedInfoLabel.setText(
                    file.getName() + " | "
                            + encodedImage.getWidth() + " x "
                            + encodedImage.getHeight() + " px");
            encodedImagePanel.repaint();    // triggers paintComponent — HD draw
            resetResult();
        } catch (Exception ex) {
            showError("Error loading encoded image: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------
    // handleCompare() — runs pixel comparison on background thread
    // -------------------------------------------------------
    private void handleCompare() {
        if (originalImage == null) { showError("Please upload the original image first!"); return; }
        if (encodedImage  == null) { showError("Please upload the encoded image first!");  return; }
        if (originalImage.getWidth()  != encodedImage.getWidth()
                || originalImage.getHeight() != encodedImage.getHeight()) {
            showError("Images must be the same size!\nOriginal: "
                    + originalImage.getWidth() + " x " + originalImage.getHeight()
                    + "\nEncoded:  "
                    + encodedImage.getWidth()  + " x " + encodedImage.getHeight());
            return;
        }

        compareButton.setEnabled(false);
        compareButton.setText("Comparing...");
        diffResultLabel.setText("Analyzing pixels...");
        diffResultLabel.setForeground(Color.GRAY);

        SwingWorker<long[], Void> worker = new SwingWorker<>() {
            @Override protected long[] doInBackground() { return calculateDifference(); }

            @Override
            protected void done() {
                try {
                    displayResult(get());
                } catch (Exception ex) {
                    showError("Comparison failed: " + ex.getMessage());
                } finally {
                    compareButton.setText("🔍 Compare Images");
                    compareButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    // -------------------------------------------------------
    // calculateDifference()
    // Returns [differentPixels, totalPixels, totalDiff, maxChannelDiff]
    // -------------------------------------------------------
    private long[] calculateDifference() {
        int  width      = originalImage.getWidth();
        int  height     = originalImage.getHeight();
        long diffPixels = 0;
        long totalDiff  = 0;
        long maxDiff    = 0;
        long total      = (long) width * height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb1 = originalImage.getRGB(x, y);
                int rgb2 = encodedImage.getRGB(x, y);
                if (rgb1 != rgb2) {
                    diffPixels++;
                    long d = Math.abs(((rgb1 >> 16) & 0xFF) - ((rgb2 >> 16) & 0xFF))
                            + Math.abs(((rgb1 >>  8) & 0xFF) - ((rgb2 >>  8) & 0xFF))
                            + Math.abs(( rgb1        & 0xFF) - ( rgb2        & 0xFF));
                    totalDiff += d;
                    if (d > maxDiff) maxDiff = d;
                }
            }
        }
        return new long[]{diffPixels, total, totalDiff, maxDiff};
    }

    // -------------------------------------------------------
    // displayResult()
    // -------------------------------------------------------
    private void displayResult(long[] result) {
        long   diffPixels   = result[0];
        long   total        = result[1];
        long   totalDiff    = result[2];
        long   maxDiff      = result[3];
        double pctChanged   = (diffPixels / (double) total) * 100;
        double avgDiff      = diffPixels > 0 ? (double) totalDiff / diffPixels : 0;

        if (diffPixels == 0) {
            diffResultLabel.setText("✅ Images are identical — no changes detected");
            diffResultLabel.setForeground(new Color(34, 139, 34));
            return;
        }

        String verdict = pctChanged < 5
                ? "<font color='green'><b>✅ Subtle LSB encoding detected — consistent with steganography</b></font>"
                : "<font color='red'><b>⚠️ Large differences detected — images may not be related</b></font>";

        diffResultLabel.setText(
                "<html><center>"
                        + "Changed pixels: <b>" + diffPixels + "</b> / " + total
                        + " (" + String.format("%.4f", pctChanged) + "%)"
                        + "<br>Average pixel difference: <b>"
                        + String.format("%.2f", avgDiff) + "</b>"
                        + " &nbsp;|&nbsp; Max single-pixel diff: <b>" + maxDiff + "</b> / 765"
                        + "<br><br>" + verdict
                        + "</center></html>"
        );
        diffResultLabel.setForeground(Color.BLACK);
    }

    // -------------------------------------------------------
    // openFileChooser()
    // -------------------------------------------------------
    private File openFileChooser(String title) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(title);
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter( "Image Files (PNG, BMP, JPG, JPEG, GIF, TIFF)",
                "png", "bmp", "jpg", "jpeg", "gif", "tiff"));
        return fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
                ? fc.getSelectedFile() : null;
    }

    // -------------------------------------------------------
    // resetResult()
    // -------------------------------------------------------
    private void resetResult() {
        diffResultLabel.setText("Upload both images then click Compare");
        diffResultLabel.setForeground(Color.GRAY);
    }

    // -------------------------------------------------------
    // handleReset() — clears everything
    // -------------------------------------------------------
    private void handleReset() {
        originalImage = null;
        encodedImage  = null;
        originalInfoLabel.setText("Upload the original image");
        encodedInfoLabel.setText("Upload the encoded image");
        originalImagePanel.repaint();
        encodedImagePanel.repaint();
        resetResult();
    }

    // -------------------------------------------------------
    // showError()
    // -------------------------------------------------------
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}