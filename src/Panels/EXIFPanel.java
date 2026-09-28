package Panels;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class EXIFPanel extends JPanel {

    private File loadedFile;
    private BufferedImage previewImage;

    private JPanel imagePreviewPanel;
    private JLabel fileInfoLabel;
    private JTextPane summaryPane;
    private JTable metaTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JTextField searchField;

    private final List<String[]> allMetadata = new ArrayList<>();

    public EXIFPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(new Color(245, 245, 250));
        add(buildTopBar(), BorderLayout.NORTH);
        add(buildMainArea(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildTopBar() {
        JPanel panel = new JPanel(new BorderLayout(10, 8));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("EXIF Metadata Analyser — Full extraction, including PNG eXIf chunks"));

        imagePreviewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFit(g, previewImage, getWidth(), getHeight());
            }
        };
        imagePreviewPanel.setBackground(Color.WHITE);
        imagePreviewPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        imagePreviewPanel.setPreferredSize(new Dimension(160, 100));

        fileInfoLabel = new JLabel("Load a JPEG, PNG, TIFF or BMP image to extract metadata");
        fileInfoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        fileInfoLabel.setForeground(Color.GRAY);

        JLabel desc = new JLabel("""
            <html><b>💡 What this extracts:</b> Camera model, GPS location, date taken,
            software used, author, copyright, IPTC, XMP, PNG chunks (eXIf, tEXt, iTXt, zTXt), CTF flags.
            GPS coordinates can reveal WHERE a photo was taken!</html>""");
        desc.setFont(new Font("Arial", Font.PLAIN, 11));
        desc.setForeground(new Color(80, 80, 120));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        searchRow.setBackground(new Color(245, 245, 250));
        searchRow.add(makeBoldLabel("Search:"));
        searchField = new JTextField(20);
        searchField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applySearch(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applySearch(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applySearch(); }
        });
        searchRow.add(searchField);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRow.setBackground(new Color(245, 245, 250));

        JButton loadBtn    = makeBtn("📁 Load Image",          new Color(70, 130, 180));
        JButton analyseBtn = makeBtn("🔍 Extract All Metadata",new Color(34, 139, 34));
        JButton exportBtn  = makeBtn("💾 Export to TXT",       new Color(100, 60, 160));
        JButton clearBtn   = makeBtn("🗑️ Clear",               new Color(160, 60, 60));

        loadBtn.addActionListener(e    -> handleLoad());
        analyseBtn.addActionListener(e -> runExtraction());
        exportBtn.addActionListener(e  -> exportResults());
        clearBtn.addActionListener(e   -> clearAll());

        btnRow.add(loadBtn);
        btnRow.add(analyseBtn);
        btnRow.add(searchRow);
        btnRow.add(exportBtn);
        btnRow.add(clearBtn);

        JPanel infoPanel = new JPanel(new BorderLayout(5, 5));
        infoPanel.setBackground(new Color(245, 245, 250));
        infoPanel.add(fileInfoLabel, BorderLayout.NORTH);
        infoPanel.add(desc,          BorderLayout.CENTER);
        infoPanel.add(btnRow,        BorderLayout.SOUTH);

        panel.add(imagePreviewPanel, BorderLayout.WEST);
        panel.add(infoPanel,         BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildMainArea() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(new Color(245, 245, 250));
        panel.add(buildSummaryPanel(), BorderLayout.NORTH);
        panel.add(buildTablePanel(),   BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("Key Findings Summary"));
        panel.setPreferredSize(new Dimension(0, 160));

        summaryPane = new JTextPane();
        summaryPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        summaryPane.setEditable(false);
        summaryPane.setBackground(new Color(15, 15, 15));
        summaryPane.setForeground(new Color(0, 255, 100));
        summaryPane.setBorder(new EmptyBorder(8, 10, 8, 10));

        showWelcome();
        panel.add(new JScrollPane(summaryPane), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("All Metadata Fields (click column header to sort)"));

        String[] cols = {"Category", "Tag Name", "Tag ID", "Value"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        metaTable = new JTable(tableModel);
        metaTable.setFont(new Font("Monospaced", Font.PLAIN, 11));
        metaTable.setRowHeight(22);
        metaTable.setGridColor(new Color(220, 220, 235));
        metaTable.setSelectionBackground(new Color(70, 100, 180));
        metaTable.setSelectionForeground(Color.WHITE);
        metaTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        metaTable.getTableHeader().setBackground(new Color(40, 60, 120));
        metaTable.getTableHeader().setForeground(Color.WHITE);

        metaTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        metaTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        metaTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        metaTable.getColumnModel().getColumn(3).setPreferredWidth(400);

        metaTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    String cat = (String) table.getValueAt(row, 0);
                    setBackground(switch (cat != null ? cat : "") {
                        case "GPS" -> new Color(255, 250, 220);
                        case "Camera" -> new Color(220, 240, 255);
                        case "Date" -> new Color(220, 255, 220);
                        case "Software" -> new Color(255, 230, 220);
                        case "Author" -> new Color(240, 220, 255);
                        case "CTF" -> new Color(255, 220, 220);
                        case "IPTC" -> new Color(210, 240, 210);
                        case "XMP" -> new Color(220, 220, 255);
                        case "PNG" -> new Color(255, 245, 200);
                        default -> row % 2 == 0 ? Color.WHITE : new Color(245, 245, 252);
                    });
                    setForeground(Color.BLACK);
                }
                return this;
            }
        });

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        metaTable.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(metaTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 220)));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 30, 60));
        panel.setBorder(new EmptyBorder(4, 10, 4, 10));

        statusLabel = new JLabel("Ready — Load an image to extract metadata");
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(180, 220, 180));

        JLabel legend = new JLabel("🟡 GPS  🔵 Camera  🟢 Date  🟠 Software  🟣 Author  🔴 CTF  📰 IPTC  🗃️ XMP  🟤 PNG");
        legend.setFont(new Font("Arial", Font.ITALIC, 10));
        legend.setForeground(new Color(180, 180, 220));

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(legend,      BorderLayout.EAST);
        return panel;
    }

    private void handleLoad() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load Image for Metadata Extraction");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image Files (JPEG, PNG, TIFF, BMP, GIF)",
                "jpg", "jpeg", "png", "tiff", "tif", "bmp", "gif"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = fc.getSelectedFile();
        try {
            loadedFile = file;
            BufferedImage raw = ImageIO.read(file);
            if (raw != null) {
                previewImage = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = previewImage.createGraphics();
                g2.drawImage(raw, 0, 0, Color.WHITE, null);
                g2.dispose();
                imagePreviewPanel.repaint();
            }

            fileInfoLabel.setText(file.getName() + "  |  " + formatSize(file.length()) + "  |  " + file.getAbsolutePath());

            allMetadata.clear();
            tableModel.setRowCount(0);
            showWelcome();
            statusLabel.setText("Loaded: " + file.getName() + " — Click 'Extract All Metadata'");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runExtraction() {
        if (loadedFile == null) {
            JOptionPane.showMessageDialog(this, "Load an image first!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        allMetadata.clear();
        tableModel.setRowCount(0);

        extractBasicFileInfo();

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(loadedFile);
            for (Directory directory : metadata.getDirectories()) {
                String category = getCategoryName(directory);
                for (Tag tag : directory.getTags()) {
                    String tagName = tag.getTagName();
                    String value = tag.getDescription();
                    if (value == null || value.isEmpty()) value = "(no description)";
                    String tagId = String.format("0x%04X", tag.getTagType());
                    addMeta(category, tagName, tagId, value);
                }
                if (directory.hasErrors()) {
                    for (String error : directory.getErrors()) {
                        addMeta(category, "ERROR", "—", error);
                    }
                }
            }
        } catch (Exception e) {
            addMeta("Library", "Error", "—", "metadata-extractor failed: " + e.getMessage());
        }

        extractFlagPatterns();

        for (String[] row : allMetadata) tableModel.addRow(row);
        buildSummary();
        statusLabel.setText("Extracted " + allMetadata.size() + " metadata fields from " + loadedFile.getName());
    }

    private void extractBasicFileInfo() {
        addMeta("File", "Filename", "—", loadedFile.getName());
        addMeta("File", "Full Path", "—", loadedFile.getAbsolutePath());
        addMeta("File", "File Size", "—", formatSize(loadedFile.length()) + " (" + loadedFile.length() + " bytes)");
        addMeta("File", "Last Modified", "—", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(loadedFile.lastModified())));
        addMeta("File", "Extension", "—", loadedFile.getName().contains(".") ?
                loadedFile.getName().substring(loadedFile.getName().lastIndexOf('.') + 1).toUpperCase() : "Unknown");

        if (previewImage != null) {
            addMeta("Image", "Width", "—", previewImage.getWidth() + " pixels");
            addMeta("Image", "Height", "—", previewImage.getHeight() + " pixels");
            addMeta("Image", "Megapixels", "—", String.format("%.2f MP", (previewImage.getWidth() * previewImage.getHeight()) / 1_000_000.0));
        }
    }

    private void extractFlagPatterns() {
        try {
            byte[] fileBytes = Files.readAllBytes(loadedFile.toPath());
            String content = new String(fileBytes, StandardCharsets.ISO_8859_1);
            String[] patterns = {
                    "flag\\{[^}]{1,200}\\}", "FLAG\\{[^}]{1,200}\\}",
                    "CTF\\{[^}]{1,200}\\}", "ctf\\{[^}]{1,200}\\}",
                    "HTB\\{[^}]{1,200}\\}", "htb\\{[^}]{1,200}\\}",
                    "THM\\{[^}]{1,200}\\}", "thm\\{[^}]{1,200}\\}",
                    "picoCTF\\{[^}]{1,200}\\}", "KEY\\{[^}]{1,200}\\}"
            };
            for (String pat : patterns) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(pat, java.util.regex.Pattern.CASE_INSENSITIVE).matcher(content);
                while (m.find()) {
                    addMeta("CTF", "🚩 FLAG FOUND", "—", m.group());
                }
            }
        } catch (Exception ignored) {}
    }

    private String getCategoryName(Directory directory) {
        String name = directory.getName();
        if (name == null) return "Other";
        if (name.contains("GPS")) return "GPS";
        if (name.contains("Exif")) return "Camera";
        if (name.contains("PNG")) return "PNG";
        if (name.contains("IPTC")) return "IPTC";
        if (name.contains("XMP")) return "XMP";
        if (name.contains("JFIF")) return "JFIF";
        if (name.contains("ICC")) return "ICC Profile";
        return name;
    }

    private void buildSummary() {
        clearPane(summaryPane);
        Map<String, List<String[]>> byCategory = new LinkedHashMap<>();
        for (String[] row : allMetadata) {
            byCategory.computeIfAbsent(row[0], k -> new ArrayList<>()).add(row);
        }

        appendText(summaryPane, "📄 File: " + loadedFile.getName() + "  (" + allMetadata.size() + " fields)\n\n", Color.WHITE);

        List<String[]> ctf = byCategory.getOrDefault("CTF", List.of());
        if (!ctf.isEmpty()) {
            appendText(summaryPane, "🚩 CTF FLAGS:\n", Color.YELLOW);
            for (String[] r : ctf) appendText(summaryPane, "   " + r[3] + "\n", Color.YELLOW);
            appendText(summaryPane, "\n", Color.WHITE);
        }

        List<String[]> gps = byCategory.getOrDefault("GPS", List.of());
        if (!gps.isEmpty()) {
            appendText(summaryPane, "📍 GPS / Location:\n", new Color(200, 180, 0));
            for (String[] r : gps) appendText(summaryPane, "   " + r[1] + " : " + r[3] + "\n", new Color(255, 220, 100));
            String lat = findValue(gps, "Latitude");
            String lon = findValue(gps, "Longitude");
            if (lat != null && lon != null) {
                appendText(summaryPane, "   🌍 Google Maps: https://maps.google.com/?q=" + lat + "," + lon + "\n", new Color(100, 200, 255));
            }
            appendText(summaryPane, "\n", Color.WHITE);
        }

        showCategory(byCategory, "Camera", "📷 Camera Info:", new Color(100, 180, 255));
        showCategory(byCategory, "Date", "📅 Dates:", new Color(100, 220, 100));
        showCategory(byCategory, "Software", "💻 Software:", new Color(255, 180, 100));
        showCategory(byCategory, "Author", "👤 Author / Copyright:", new Color(200, 150, 255));
        showCategory(byCategory, "IPTC", "📰 IPTC Metadata:", new Color(150, 200, 150));
        showCategory(byCategory, "XMP", "🗃️ XMP Metadata:", new Color(180, 180, 255));
        showCategory(byCategory, "PNG", "🖼️ PNG Chunks:", new Color(220, 200, 120));

        if (ctf.isEmpty() && gps.isEmpty() && byCategory.size() == 1) {
            appendText(summaryPane, "No additional metadata found.\n", new Color(150, 150, 150));
        }
    }

    private void showCategory(Map<String, List<String[]>> map, String cat, String title, Color color) {
        List<String[]> rows = map.getOrDefault(cat, List.of());
        if (!rows.isEmpty()) {
            appendText(summaryPane, title + "\n", color);
            for (String[] r : rows) appendText(summaryPane, "   " + r[1] + " : " + r[3] + "\n", color.brighter());
            appendText(summaryPane, "\n", Color.WHITE);
        }
    }

    private String findValue(List<String[]> rows, String name) {
        return rows.stream().filter(r -> r[1].contains(name)).map(r -> r[3]).findFirst().orElse(null);
    }

    private void addMeta(String category, String name, String tagId, String value) {
        if (value == null || value.isEmpty() || value.equals("\0")) return;
        allMetadata.add(new String[]{category, name, tagId, value});
    }

    private void applySearch() {
        String query = searchField.getText().trim();
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) metaTable.getRowSorter();
        if (query.isEmpty()) sorter.setRowFilter(null);
        else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + query));
    }

    private void exportResults() {
        if (allMetadata.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No metadata to export. Run extraction first!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export Metadata");
        fc.setSelectedFile(new File(loadedFile != null ? loadedFile.getName() + "_metadata.txt" : "metadata.txt"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File out = fc.getSelectedFile();
        StringBuilder sb = new StringBuilder();
        sb.append("EXIF METADATA REPORT\nFile: ").append(loadedFile.getName()).append("\nDate: ").append(new Date()).append("\n").append("=".repeat(60)).append("\n\n");
        String lastCat = "";
        for (String[] row : allMetadata) {
            if (!row[0].equals(lastCat)) {
                sb.append("\n── ").append(row[0]).append(" ──\n");
                lastCat = row[0];
            }
            sb.append(String.format("  %-35s : %s\n", row[1], row[3]));
        }
        try {
            Files.writeString(out.toPath(), sb.toString());
            JOptionPane.showMessageDialog(this, "Exported " + allMetadata.size() + " fields to:\n" + out.getName(), "Exported", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearAll() {
        loadedFile = null;
        previewImage = null;
        imagePreviewPanel.repaint();
        fileInfoLabel.setText("Load a JPEG, PNG, TIFF or BMP image to extract metadata");
        allMetadata.clear();
        tableModel.setRowCount(0);
        searchField.setText("");
        clearPane(summaryPane);
        showWelcome();
        statusLabel.setText("Cleared.");
    }

    private String formatSize(long bytes) {
        if (bytes < 1024)        return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024));
    }

    private void showWelcome() {
        clearPane(summaryPane);
        appendText(summaryPane, """
                ╔══════════════════════════════════════════════════╗
                ║         EXIF METADATA ANALYSER v3.0             ║
                ╚══════════════════════════════════════════════════╝

                EXTRACTS (using metadata-extractor library):
                  📷 Camera (Make, Model, Lens, ISO, Aperture, Shutter)
                  📍 GPS (Latitude, Longitude, Altitude, Direction)
                  📅 Date (Date taken, Modified, Digitised)
                  💻 Software (Editor, OS, Processing tool)
                  👤 Author (Creator, Copyright, Artist, Owner)
                  📰 IPTC (Caption, Keywords, City, Country)
                  🗃️ XMP (Adobe/Photoshop extended metadata)
                  🖼️ PNG (tEXt, iTXt, zTXt chunks + eXIf for full EXIF)
                  🚩 CTF (automatic flag pattern detection)

                HOW TO USE:
                  1. Load any image (JPEG, PNG, TIFF, BMP, GIF)
                  2. Click 'Extract All Metadata'
                  3. Summary shows key findings
                  4. Full table shows every field (searchable)
                  5. Export saves everything to .txt
                """, new Color(0, 200, 80));
    }

    private void appendText(JTextPane pane, String text, Color colour) {
        StyledDocument doc = pane.getStyledDocument();
        Style style = pane.addStyle("s", null);
        StyleConstants.setForeground(style, colour);
        StyleConstants.setFontFamily(style, "Monospaced");
        StyleConstants.setFontSize(style, 12);
        try { doc.insertString(doc.getLength(), text, style); }
        catch (BadLocationException ignored) {}
    }

    private void clearPane(JTextPane pane) { pane.setText(""); }

    private void drawFit(Graphics g, BufferedImage img, int pw, int ph) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        if (img != null) {
            double scale = Math.min((double) pw / img.getWidth(), (double) ph / img.getHeight());
            int dw = (int)(img.getWidth()  * scale);
            int dh = (int)(img.getHeight() * scale);
            g2.drawImage(img, (pw - dw) / 2, (ph - dh) / 2, dw, dh, null);
        } else {
            g2.setColor(Color.GRAY);
            g2.setFont(new Font("Arial", Font.ITALIC, 11));
            String hint = "No Image";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(hint, Math.max(0, (pw - fm.stringWidth(hint)) / 2), (ph + fm.getAscent()) / 2);
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
}