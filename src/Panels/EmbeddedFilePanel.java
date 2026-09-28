package Panels;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class EmbeddedFilePanel extends JPanel {

    // ── Fields ──
    private File   loadedFile;
    private byte[] fileBytes;

    // UI
    private JLabel    fileInfoLabel;
    private JTextPane resultPane;
    private JLabel    statusLabel;

    // Stores scan results for extraction
    private final List<ScanResult> scanResults = new ArrayList<>();

    // ── Known file signatures ──
    // Each entry: { signature bytes, file type name, extension, description }
    private static final Object[][] SIGNATURES = {
            { new byte[]{(byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A},
                    "PNG Image",     "png",  "PNG image file"                          },
            { new byte[]{(byte)0xFF,(byte)0xD8,(byte)0xFF},
                    "JPEG Image",    "jpg",  "JPEG image — check for steghide!"        },
            { new byte[]{0x47,0x49,0x46,0x38},
                    "GIF Image",     "gif",  "GIF image — check for frame hiding"      },
            { new byte[]{0x42,0x4D},
                    "BMP Image",     "bmp",  "BMP image file"                          },
            { new byte[]{0x50,0x4B,0x03,0x04},
                    "ZIP Archive",   "zip",  "ZIP — extract and examine contents!"     },
            { new byte[]{0x50,0x4B,0x05,0x06},
                    "ZIP (empty)",   "zip",  "Empty ZIP archive"                       },
            { new byte[]{0x52,0x61,0x72,0x21,0x1A,0x07},
                    "RAR Archive",   "rar",  "RAR — extract and examine contents!"     },
            { new byte[]{0x25,0x50,0x44,0x46},
                    "PDF Document",  "pdf",  "PDF — open and check for hidden layers!" },
            { new byte[]{0x1F,(byte)0x8B},
                    "GZIP Archive",  "gz",   "GZIP compressed data"                    },
            { new byte[]{0x42,0x5A,0x68},
                    "BZIP2 Archive", "bz2",  "BZIP2 compressed data"                   },
            { new byte[]{0x37,0x7A,(byte)0xBC,(byte)0xAF,0x27,0x1C},
                    "7-Zip Archive", "7z",   "7-Zip archive"                           },
            { new byte[]{0x49,0x44,0x33},
                    "MP3 Audio",     "mp3",  "MP3 — check spectrogram for hidden data!"},
            { new byte[]{0x52,0x49,0x46,0x46},
                    "WAV/AVI",       "wav",  "WAV audio — check LSB and spectrogram!"  },
            { new byte[]{0x4F,0x67,0x67,0x53},
                    "OGG Audio",     "ogg",  "OGG audio file"                          },
            { new byte[]{0x7F,0x45,0x4C,0x46},
                    "ELF Executable","elf",  "Linux executable — run carefully!"       },
            { new byte[]{0x4D,0x5A},
                    "Windows EXE",   "exe",  "Windows executable — run carefully!"     },
            { new byte[]{0x49,0x49,0x2A,0x00},
                    "TIFF Image",    "tiff", "TIFF image (little-endian)"              },
            { new byte[]{0x4D,0x4D,0x00,0x2A},
                    "TIFF Image",    "tiff", "TIFF image (big-endian)"                 },
            { new byte[]{0x53,0x51,0x4C,0x69},
                    "SQLite DB",     "db",   "SQLite database — open with DB browser!" },
            { new byte[]{0x7B,0x5C,0x72,0x74,0x66},
                    "RTF Document",  "rtf",  "Rich Text Format document"               },
    };

    // PNG end marker — IEND chunk CRC
    private static final byte[] PNG_IEND =
            {0x49,0x45,0x4E,0x44,(byte)0xAE,0x42,0x60,(byte)0x82};

    // JPEG end marker
    private static final byte[] JPEG_END = {(byte)0xFF,(byte)0xD9};

    // GIF end marker
    private static final byte[] GIF_END = {0x3B};

    // ──────────────────────────────────────────────────────
    // Inner class to hold one scan result
    // ──────────────────────────────────────────────────────
    private static class ScanResult {
        final int    offset;
        final String typeName;
        final String extension;
        final String description;
        final boolean isAtStart;
        final boolean isAfterEnd;

        ScanResult(int offset, String typeName, String extension,
                   String description, boolean isAtStart, boolean isAfterEnd) {
            this.offset      = offset;
            this.typeName    = typeName;
            this.extension   = extension;
            this.description = description;
            this.isAtStart   = isAtStart;
            this.isAfterEnd  = isAfterEnd;
        }
    }

    // ──────────────────────────────────────────────────────
    // Constructor
    // ──────────────────────────────────────────────────────
    public EmbeddedFilePanel() {
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
                "Embedded File Scanner — Find files hidden inside your image"));

        // Info label
        fileInfoLabel = new JLabel(
                "Load any image file to scan for embedded/appended hidden files");
        fileInfoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        fileInfoLabel.setForeground(Color.GRAY);

        // Description
        JLabel desc = new JLabel("""
            <html><b>💡 What this does:</b> Scans every byte of the file looking for
            known file signatures (ZIP, JPEG, PDF, RAR etc.) hidden anywhere inside.
            Also detects data appended after the image end marker (IEND / FFD9).</html>""");
        desc.setFont(new Font("Arial", Font.PLAIN, 11));
        desc.setForeground(new Color(80, 80, 120));

        // Buttons row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRow.setBackground(new Color(245, 245, 250));

        JButton loadBtn = makeBtn("📁 Load File", new Color(70, 130, 180));
        loadBtn.addActionListener(e -> handleLoad());

        JButton scanBtn = makeBtn("🔍 Scan for Embedded Files", new Color(34, 139, 34));
        scanBtn.addActionListener(e -> runScan());

        JButton extractBtn = makeBtn("📤 Extract Selected", new Color(139, 90, 34));
        extractBtn.addActionListener(e -> extractSelected());

        JButton extractAllBtn = makeBtn("📦 Extract All Found", new Color(100, 60, 160));
        extractAllBtn.addActionListener(e -> extractAll());

        JButton clearBtn = makeBtn("🗑️ Clear", new Color(160, 60, 60));
        clearBtn.addActionListener(e -> clearResults());

        btnRow.add(loadBtn);
        btnRow.add(scanBtn);
        btnRow.add(extractBtn);
        btnRow.add(extractAllBtn);
        btnRow.add(clearBtn);

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.setBackground(new Color(245, 245, 250));
        top.add(fileInfoLabel, BorderLayout.NORTH);
        top.add(desc,          BorderLayout.CENTER);
        top.add(btnRow,        BorderLayout.SOUTH);

        panel.add(top, BorderLayout.CENTER);
        return panel;
    }

    // ──────────────────────────────────────────────────────
    // MAIN AREA — result pane
    // ──────────────────────────────────────────────────────
    private JPanel buildMainArea() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("Scan Results"));

        resultPane = new JTextPane();
        resultPane.setFont(new Font("Monospaced", Font.PLAIN, 13));
        resultPane.setEditable(false);
        resultPane.setBackground(new Color(15, 15, 15));
        resultPane.setForeground(new Color(0, 255, 100));
        resultPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        showWelcome();

        JScrollPane scroll = new JScrollPane(resultPane);
        scroll.setBackground(new Color(15, 15, 15));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ──────────────────────────────────────────────────────
    // STATUS BAR
    // ──────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 30, 60));
        panel.setBorder(new EmptyBorder(4, 10, 4, 10));

        statusLabel = new JLabel("Ready — Load a file and click Scan");
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(180, 220, 180));

        JLabel hintLabel = new JLabel(
                "Tip: Any file found AFTER the end marker is almost certainly hidden data!");
        hintLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        hintLabel.setForeground(new Color(180, 180, 220));

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(hintLabel,   BorderLayout.EAST);
        return panel;
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Load file
    // ══════════════════════════════════════════════════════
    private void handleLoad() {
        File file = openFileChooser();
        if (file == null) return;
        try {
            fileBytes  = Files.readAllBytes(file.toPath());
            loadedFile = file;
            fileInfoLabel.setText(
                    file.getName() + "  |  "
                            + fileBytes.length + " bytes  ("
                            + (fileBytes.length / 1024) + " KB)  |  "
                            + file.getAbsolutePath());
            scanResults.clear();
            clearPane(resultPane);
            appendText(resultPane,
                    "✅ File loaded: " + file.getName() + "\n" +
                            "   Size: " + fileBytes.length + " bytes\n\n" +
                            "Click 'Scan for Embedded Files' to begin analysis.\n",
                    new Color(0, 255, 100));
            statusLabel.setText("File loaded — " + fileBytes.length + " bytes. Ready to scan.");
        } catch (Exception ex) {
            showError("Error loading file: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Main scan
    // ══════════════════════════════════════════════════════
    private void runScan() {
        if (fileBytes == null) {
            showError("Please load a file first!");
            return;
        }

        scanResults.clear();
        clearPane(resultPane);

        appendHeader("EMBEDDED FILE SCANNER");
        appendText(resultPane,
                "File  : " + loadedFile.getName() + "\n" +
                        "Size  : " + fileBytes.length + " bytes\n" +
                        "Scanning " + SIGNATURES.length + " known file signatures...\n\n",
                new Color(100, 180, 255));

        // ── Step 1: Find image end marker ──
        int endMarkerOffset = findEndMarker();
        appendSubHeader("STEP 1 — Image End Marker");
        if (endMarkerOffset >= 0) {
            appendText(resultPane,
                    "✅ End marker found at offset: 0x"
                            + String.format("%X", endMarkerOffset)
                            + " (" + endMarkerOffset + ")\n",
                    new Color(0, 255, 100));

            int bytesAfter = fileBytes.length - endMarkerOffset;
            if (bytesAfter > 12) {
                appendText(resultPane,
                        "🚩 " + bytesAfter + " bytes exist AFTER end marker!\n" +
                                "   This strongly suggests hidden/appended data!\n",
                        Color.YELLOW);
            } else {
                appendText(resultPane,
                        "✅ No significant data after end marker.\n",
                        new Color(0, 200, 80));
            }
        } else {
            appendText(resultPane,
                    "⚠️  No known end marker found.\n" +
                            "   File may be corrupted or use unknown format.\n",
                    Color.ORANGE);
        }
        appendText(resultPane, "\n", Color.WHITE);

        // ── Step 2: Scan all offsets for signatures ──
        appendSubHeader("STEP 2 — Full File Signature Scan");
        appendText(resultPane,
                "Scanning every byte for known file signatures...\n\n",
                new Color(100, 180, 255));

        int found = 0;
        for (int offset = 0; offset < fileBytes.length; offset++) {
            for (Object[] sig : SIGNATURES) {
                byte[] pattern = (byte[]) sig[0];
                if (matchesAt(fileBytes, offset, pattern)) {
                    String typeName   = (String) sig[1];
                    String extension  = (String) sig[2];
                    String desc       = (String) sig[3];
                    boolean isAtStart = (offset == 0);
                    boolean isAfterEnd = (endMarkerOffset >= 0
                            && offset > endMarkerOffset);

                    ScanResult result = new ScanResult(
                            offset, typeName, extension, desc,
                            isAtStart, isAfterEnd);
                    scanResults.add(result);

                    reportResult(result, found + 1);
                    found++;
                    break; // one match per offset
                }
            }
        }

        if (found == 0) {
            appendText(resultPane,
                    "No embedded file signatures found.\n",
                    new Color(150, 150, 150));
        }

        // ── Step 3: Summary ──
        appendSubHeader("STEP 3 — Summary");
        appendSummary(found, endMarkerOffset);

        statusLabel.setText("Scan complete — " + found + " signature(s) found. "
                + scanResults.stream().filter(r -> r.isAfterEnd).count()
                + " after end marker.");
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Find image end marker offset
    // ══════════════════════════════════════════════════════
    private int findEndMarker() {
        // Try PNG IEND
        int pngEnd = findPattern(fileBytes, PNG_IEND);
        if (pngEnd >= 0) return pngEnd + PNG_IEND.length;

        // Try JPEG FFD9
        int jpegEnd = findLastPattern(fileBytes, JPEG_END);
        if (jpegEnd >= 0) return jpegEnd + JPEG_END.length;

        // Try GIF 3B
        int gifEnd = findLastPattern(fileBytes, GIF_END);
        if (gifEnd >= 0) return gifEnd + GIF_END.length;

        return -1;
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Report one scan result to output pane
    // ══════════════════════════════════════════════════════
    private void reportResult(ScanResult r, int index) {
        // Colour coding
        Color headerCol;
        String tag;
        if (r.isAfterEnd) {
            headerCol = Color.YELLOW;
            tag = "🚩 HIDDEN — after end marker";
        } else if (r.isAtStart) {
            headerCol = new Color(0, 200, 80);
            tag = "✅ PRIMARY — this is the main file";
        } else {
            headerCol = new Color(255, 165, 0);
            tag = "⚠️  EMBEDDED — inside file body";
        }

        appendText(resultPane,
                "─── #" + index + " " + tag + " ───\n", headerCol);
        appendText(resultPane,
                "  Type      : " + r.typeName + "\n", Color.WHITE);
        appendText(resultPane,
                "  Offset    : " + r.offset + " (0x"
                        + String.format("%X", r.offset) + ")\n",
                new Color(180, 180, 255));
        appendText(resultPane,
                "  Size      : ~" + (fileBytes.length - r.offset)
                        + " bytes from this offset\n",
                new Color(150, 150, 150));
        appendText(resultPane,
                "  Extension : ." + r.extension + "\n",
                new Color(150, 150, 150));
        appendText(resultPane,
                "  Note      : " + r.description + "\n\n",
                new Color(200, 200, 100));
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Print summary
    // ══════════════════════════════════════════════════════
    private void appendSummary(int totalFound, int endMarkerOffset) {
        long afterEnd = scanResults.stream().filter(r -> r.isAfterEnd).count();
        long embedded = scanResults.stream()
                .filter(r -> !r.isAtStart && !r.isAfterEnd).count();

        appendText(resultPane,
                "Total signatures found : " + totalFound + "\n" +
                        "Primary file           : " + (totalFound > 0 ? 1 : 0) + "\n" +
                        "Embedded (in body)     : " + embedded + "\n" +
                        "After end marker       : " + afterEnd + "\n\n",
                new Color(0, 255, 100));

        if (afterEnd > 0) {
            appendText(resultPane,
                    "🚩 ACTION REQUIRED:\n" +
                            "   Use 'Extract Selected' or 'Extract All Found'\n" +
                            "   to save hidden files for further analysis!\n\n",
                    Color.YELLOW);
        } else if (totalFound <= 1) {
            appendText(resultPane,
                    "✅ No embedded files detected.\n" +
                            "   Try LSB Decoder or Bit Plane Viewer next.\n",
                    new Color(0, 200, 80));
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Extract selected result (first after-end or embedded)
    // ══════════════════════════════════════════════════════
    private void extractSelected() {
        if (scanResults.isEmpty()) {
            showError("No scan results. Please run a scan first.");
            return;
        }

        // Find first non-primary result to extract
        ScanResult target = null;
        for (ScanResult r : scanResults) {
            if (!r.isAtStart) { target = r; break; }
        }
        if (target == null) {
            showError("No embedded or appended files found to extract.");
            return;
        }
        extractResult(target);
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Extract all non-primary results
    // ══════════════════════════════════════════════════════
    private void extractAll() {
        if (scanResults.isEmpty()) {
            showError("No scan results. Please run a scan first.");
            return;
        }

        List<ScanResult> targets = new ArrayList<>();
        for (ScanResult r : scanResults) {
            if (!r.isAtStart) targets.add(r);
        }

        if (targets.isEmpty()) {
            showError("No embedded or appended files found to extract.");
            return;
        }

        // Ask user for output directory
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Output Folder for Extracted Files");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File outDir = fc.getSelectedFile();
        int  saved  = 0;

        for (int i = 0; i < targets.size(); i++) {
            ScanResult r    = targets.get(i);
            String     name = "extracted_" + (i + 1) + "_offset"
                    + r.offset + "." + r.extension;
            File outFile = new File(outDir, name);
            try {
                saveBytes(fileBytes, r.offset, outFile);
                saved++;
                appendText(resultPane,
                        "✅ Saved: " + outFile.getName() + "\n",
                        new Color(0, 255, 100));
            } catch (Exception ex) {
                appendText(resultPane,
                        "❌ Failed: " + name + " — " + ex.getMessage() + "\n",
                        Color.RED);
            }
        }
        appendText(resultPane,
                "\n" + saved + " file(s) extracted to: "
                        + outDir.getAbsolutePath() + "\n",
                Color.CYAN);
        statusLabel.setText("Extracted " + saved + " file(s) to " + outDir.getName());
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Extract one result — prompts user for save location
    // ══════════════════════════════════════════════════════
    private void extractResult(ScanResult r) {
        String defaultName = "extracted_offset" + r.offset + "." + r.extension;
        File   outFile     = openSaveDialog(defaultName, r.extension);
        if (outFile == null) return;

        try {
            saveBytes(fileBytes, r.offset, outFile);
            appendText(resultPane,
                    "✅ Extracted: " + outFile.getName()
                            + " (" + (fileBytes.length - r.offset) + " bytes)\n",
                    new Color(0, 255, 100));
            statusLabel.setText("Extracted: " + outFile.getName());
            JOptionPane.showMessageDialog(this,
                    "Saved: " + outFile.getAbsolutePath(),
                    "Extraction Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Error extracting: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Save bytes from offset to end → file
    // ══════════════════════════════════════════════════════
    private void saveBytes(byte[] data, int fromOffset, File outFile) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(data, fromOffset, data.length - fromOffset);
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Pattern matching helpers
    // ══════════════════════════════════════════════════════

    // Check if bytes match pattern at a given offset
    private boolean matchesAt(byte[] data, int offset, byte[] pattern) {
        if (offset + pattern.length > data.length) return false;
        for (int i = 0; i < pattern.length; i++) {
            if (data[offset + i] != pattern[i]) return false;
        }
        return true;
    }

    // Find first occurrence of pattern — returns offset or -1
    private int findPattern(byte[] data, byte[] pattern) {
        for (int i = 0; i <= data.length - pattern.length; i++) {
            if (matchesAt(data, i, pattern)) return i;
        }
        return -1;
    }

    // Find last occurrence of pattern — returns offset or -1
    private int findLastPattern(byte[] data, byte[] pattern) {
        int last = -1;
        for (int i = 0; i <= data.length - pattern.length; i++) {
            if (matchesAt(data, i, pattern)) last = i;
        }
        return last;
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Clear results
    // ══════════════════════════════════════════════════════
    private void clearResults() {
        scanResults.clear();
        clearPane(resultPane);
        showWelcome();
        statusLabel.setText("Cleared — Load a file and click Scan");
    }

    // ══════════════════════════════════════════════════════
    // FILE CHOOSER HELPERS
    // ══════════════════════════════════════════════════════
    private File openFileChooser() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load File to Scan");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image Files (PNG, BMP, JPG, JPEG, GIF, TIFF)",
                "png", "bmp", "jpg", "jpeg", "gif", "tiff"));
        return fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
                ? fc.getSelectedFile() : null;
    }

    private File openSaveDialog(String defaultName, String extension) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Extracted File");
        fc.setSelectedFile(new File(defaultName));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                extension.toUpperCase() + " File", extension));
        return fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION
                ? fc.getSelectedFile() : null;
    }

    // ══════════════════════════════════════════════════════
    // DISPLAY HELPERS
    // ══════════════════════════════════════════════════════
    private void showWelcome() {
        appendText(resultPane, """
                ╔══════════════════════════════════════════════════╗
                ║         EMBEDDED FILE SCANNER                   ║
                ╚══════════════════════════════════════════════════╝

                HOW TO USE:
                  1. Load any image file (PNG, JPEG, BMP, GIF)
                  2. Click 'Scan for Embedded Files'
                  3. Review results — files found AFTER the end
                     marker are almost certainly hidden data!
                  4. Click 'Extract Selected' or 'Extract All Found'
                     to save hidden files for further analysis

                WHAT THIS DETECTS:
                  ✅ Files appended after PNG IEND / JPEG FFD9
                  ✅ ZIP, RAR, 7z archives hidden inside images
                  ✅ Another JPEG/PNG image embedded inside
                  ✅ PDF, ELF, EXE, WAV, MP3 hidden anywhere
                  ✅ SQLite databases inside images

                COMMON CTF TRICK:
                  image.png = normal photo
                  But after IEND: PK.. = ZIP file!
                  → Rename to .zip → extract → find flag!
                """, new Color(0, 200, 80));
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

    private void appendHeader(String title) {
        String line = "═".repeat(52) + "\n";
        appendText(resultPane, line,                new Color(60, 120, 60));
        appendText(resultPane, "  " + title + "\n", Color.WHITE);
        appendText(resultPane, line,                new Color(60, 120, 60));
    }

    private void appendSubHeader(String title) {
        appendText(resultPane,
                "\n── " + title + " ──\n", new Color(100, 200, 255));
    }

    private void clearPane(JTextPane pane) { pane.setText(""); }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ══════════════════════════════════════════════════════
    // BUTTON HELPER
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
}
