package Panels;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.*;
import javax.swing.tree.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.Inflater;

public class PNGChunkPanel extends JPanel {

    // ── Fields ──
    private File   loadedFile;
    private byte[] fileBytes;

    // UI
    private JLabel    fileInfoLabel;
    private JTree     chunkTree;
    private JTextPane detailPane;
    private JLabel    statusLabel;

    // Stores all parsed chunks
    private final List<PNGChunk> chunks = new ArrayList<>();

    // Standard PNG chunk types (names are defined by PNG specification)
    private static final Set<String> STANDARD_CHUNKS = Set.of(
            "IHDR","PLTE","IDAT","IEND","tEXt","zTXt","iTXt",
            "cHRM","gAMA","sRGB","bKGD","hIST","tRNS","pHYs",
            "sBIT","sPLT","tIME","iCCP","eXIf"
    );

    // PNG file signature
    private static final byte[] PNG_SIG =
            {(byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A};

    // ──────────────────────────────────────────────────────
    // Inner class — one PNG chunk
    // ──────────────────────────────────────────────────────
    private static class PNGChunk {
        final int    offset;       // byte offset in file
        final int    dataLength;   // length field value
        final String type;         // 4-char type name
        final byte[] data;         // raw data bytes
        final long   storedCRC;    // CRC read from file
        final long   computedCRC;  // CRC we computed
        final boolean crcOk;       // do they match?
        final boolean isStandard;  // known chunk type?

        PNGChunk(int offset, int dataLength, String type,
                 byte[] data, long storedCRC, long computedCRC) {
            this.offset      = offset;
            this.dataLength  = dataLength;
            this.type        = type;
            this.data        = data;
            this.storedCRC   = storedCRC;
            this.computedCRC = computedCRC;
            this.crcOk       = (storedCRC == computedCRC);
            this.isStandard  = STANDARD_CHUNKS.contains(type);
        }
    }

    // ──────────────────────────────────────────────────────
    // Constructor
    // ──────────────────────────────────────────────────────
    public PNGChunkPanel() {
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
                "PNG Chunk Tree Viewer — Inspect every chunk inside a PNG file"));

        fileInfoLabel = new JLabel("Load a PNG file to analyse its internal chunk structure");
        fileInfoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        fileInfoLabel.setForeground(Color.GRAY);

        JLabel desc = new JLabel("""
            <html><b>💡 What this does:</b> Parses every PNG chunk, shows its type,
            size and content. Highlights non-standard chunks and CRC errors — both are
            common CTF hiding techniques!</html>""");
        desc.setFont(new Font("Arial", Font.PLAIN, 11));
        desc.setForeground(new Color(80, 80, 120));

        // Button row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRow.setBackground(new Color(245, 245, 250));

        JButton loadBtn    = makeBtn("📁 Load PNG",              new Color(70, 130, 180));
        JButton parseBtn   = makeBtn("🧩 Parse Chunks",          new Color(34, 139, 34));
        JButton expandBtn  = makeBtn("⊞ Expand All",            new Color(100, 100, 160));
        JButton collapseBtn= makeBtn("⊟ Collapse All",          new Color(100, 100, 160));
        JButton extractBtn = makeBtn("📤 Export Chunk Data",     new Color(139, 90, 34));
        JButton fixBtn     = makeBtn("🔧 Check CRC Errors",      new Color(160, 80, 0));

        loadBtn.addActionListener(e -> handleLoad());
        parseBtn.addActionListener(e -> parseChunks());
        expandBtn.addActionListener(e -> expandAll());
        collapseBtn.addActionListener(e -> collapseAll());
        extractBtn.addActionListener(e -> exportSelectedChunk());
        fixBtn.addActionListener(e -> checkCRCErrors());

        btnRow.add(loadBtn);
        btnRow.add(parseBtn);
        btnRow.add(expandBtn);
        btnRow.add(collapseBtn);
        btnRow.add(extractBtn);
        btnRow.add(fixBtn);

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.setBackground(new Color(245, 245, 250));
        top.add(fileInfoLabel, BorderLayout.NORTH);
        top.add(desc,          BorderLayout.CENTER);
        top.add(btnRow,        BorderLayout.SOUTH);

        panel.add(top, BorderLayout.CENTER);
        return panel;
    }

    // ──────────────────────────────────────────────────────
    // MAIN AREA — tree left, detail right
    // ──────────────────────────────────────────────────────
    private JPanel buildMainArea() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBackground(new Color(245, 245, 250));

        // ── LEFT: chunk tree ──
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("No file loaded");
        chunkTree = new JTree(root);
        chunkTree.setFont(new Font("Monospaced", Font.PLAIN, 12));
        chunkTree.setBackground(new Color(25, 25, 25));
        chunkTree.setForeground(new Color(0, 255, 100));
        chunkTree.setRowHeight(22);
        chunkTree.setBorder(new EmptyBorder(6, 6, 6, 6));

        // Custom renderer — colour-code chunk rows
        chunkTree.setCellRenderer(new ChunkTreeRenderer());

        // On selection → show detail
        chunkTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) chunkTree.getLastSelectedPathComponent();
            if (node != null && node.getUserObject() instanceof PNGChunk chunk) {
                showChunkDetail(chunk);
            }
        });

        JScrollPane treeScroll = new JScrollPane(chunkTree);
        treeScroll.setBackground(new Color(25, 25, 25));
        treeScroll.setBorder(new TitledBorder("Chunk Tree"));
        treeScroll.setPreferredSize(new Dimension(280, 0));

        // ── RIGHT: detail pane ──
        detailPane = new JTextPane();
        detailPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        detailPane.setEditable(false);
        detailPane.setBackground(new Color(15, 15, 15));
        detailPane.setForeground(new Color(0, 255, 100));
        detailPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        showDetailWelcome();

        JScrollPane detailScroll = new JScrollPane(detailPane);
        detailScroll.setBorder(new TitledBorder("Chunk Details"));

        panel.add(treeScroll,  BorderLayout.WEST);
        panel.add(detailScroll, BorderLayout.CENTER);
        return panel;
    }

    // ──────────────────────────────────────────────────────
    // STATUS BAR
    // ──────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 30, 60));
        panel.setBorder(new EmptyBorder(4, 10, 4, 10));

        statusLabel = new JLabel("Ready — Load a PNG file and click Parse Chunks");
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(180, 220, 180));

        JLabel legend = new JLabel(
                "🟢 Standard  🟡 Non-standard  🔴 CRC Error  🚩 Contains flag pattern");
        legend.setFont(new Font("Arial", Font.ITALIC, 10));
        legend.setForeground(new Color(180, 180, 220));

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(legend,      BorderLayout.EAST);
        return panel;
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Load PNG file
    // ══════════════════════════════════════════════════════
    private void handleLoad() {
        File file = openFileChooser();
        if (file == null) return;
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());

            // Verify PNG signature
            if (!isPNG(bytes)) {
                JOptionPane.showMessageDialog(this,
                        "This does not appear to be a valid PNG file.\n" +
                                "First bytes: " + toHexString(bytes),
                        "Not a PNG", JOptionPane.WARNING_MESSAGE);
                return;
            }

            fileBytes  = bytes;
            loadedFile = file;
            chunks.clear();

            fileInfoLabel.setText(
                    file.getName() + "  |  "
                            + fileBytes.length + " bytes  |  "
                            + file.getAbsolutePath());

            // Reset tree
            DefaultMutableTreeNode root =
                    new DefaultMutableTreeNode("📄 " + file.getName() + " (click Parse Chunks)");
            chunkTree.setModel(new DefaultTreeModel(root));
            showDetailWelcome();

            statusLabel.setText("PNG loaded — " + fileBytes.length
                    + " bytes. Click Parse Chunks.");

        } catch (Exception ex) {
            showError("Error loading file: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Parse all PNG chunks
    // ══════════════════════════════════════════════════════
    private void parseChunks() {
        if (fileBytes == null) {
            showError("Please load a PNG file first!");
            return;
        }

        chunks.clear();
        int offset = 8; // skip 8-byte PNG signature

        while (offset + 12 <= fileBytes.length) {
            // Read length (4 bytes big-endian)
            int len = readInt(fileBytes, offset);

            // Sanity check
            if (len < 0 || offset + 12 + len > fileBytes.length) break;

            // Read type (4 ASCII bytes)
            String type = new String(fileBytes, offset + 4, 4);

            // Read data
            byte[] data = new byte[len];
            if (len > 0) System.arraycopy(fileBytes, offset + 8, data, 0, len);

            // Read stored CRC
            long storedCRC = readUnsignedInt(fileBytes, offset + 8 + len);

            // Compute CRC over type + data
            long computedCRC = computeCRC(fileBytes, offset + 4, 4 + len);

            chunks.add(new PNGChunk(offset, len, type, data, storedCRC, computedCRC));

            if (type.equals("IEND")) {
                // Check for data after IEND
                int afterIEND = offset + 12 + len;
                if (afterIEND < fileBytes.length) {
                    // Add a fake "virtual" entry to show appended data
                    byte[] appended = new byte[fileBytes.length - afterIEND];
                    System.arraycopy(fileBytes, afterIEND, appended, 0, appended.length);
                    chunks.add(new PNGChunk(afterIEND, appended.length,
                            "????", appended, 0, 0));
                }
                break;
            }

            offset += 12 + len;
        }

        buildTree();
        statusLabel.setText("Parsed " + chunks.size() + " chunks — "
                + chunks.stream().filter(c -> !c.isStandard).count()
                + " non-standard, "
                + chunks.stream().filter(c -> !c.crcOk).count()
                + " CRC errors");
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Build JTree from parsed chunks
    // ══════════════════════════════════════════════════════
    private void buildTree() {
        DefaultMutableTreeNode root =
                new DefaultMutableTreeNode("📄 " + loadedFile.getName()
                        + "  (" + chunks.size() + " chunks)");

        for (PNGChunk chunk : chunks) {
            // Build node label
            DefaultMutableTreeNode displayNode = getDefaultMutableTreeNode(chunk);

            // Add child nodes for IHDR fields
            if (chunk.type.equals("IHDR") && chunk.data.length >= 13) {
                addIHDRChildren(displayNode, chunk);
            }

            // Add child for text content
            if ((chunk.type.equals("tEXt") || chunk.type.equals("iTXt"))
                    && chunk.data.length > 0) {
                String text = extractPrintable(chunk.data);
                if (!text.isEmpty()) {
                    displayNode.add(new DefaultMutableTreeNode("📝 " + text));
                }
            }

            root.add(displayNode);
        }

        chunkTree.setModel(new DefaultTreeModel(root));
        expandAll();
    }

    private static DefaultMutableTreeNode getDefaultMutableTreeNode(PNGChunk chunk) {
        String icon  = chunk.type.equals("????") ? "🚩"
                : !chunk.isStandard         ? "⚠️"
                : !chunk.crcOk              ? "🔴"
                : "✅";
        String label = icon + " [" + chunk.type + "]"
                + "  " + chunk.dataLength + " bytes"
                + "  @ 0x" + String.format("%X", chunk.offset);

        // Node stores chunk as user object for detail view on click
        DefaultMutableTreeNode displayNode = new DefaultMutableTreeNode(label) {
            @Override
            public String toString() { return label; }
        };
        displayNode.setUserObject(chunk);
        return displayNode;
    }

    // ── Add IHDR field children to tree node ──
    private void addIHDRChildren(DefaultMutableTreeNode parent, PNGChunk chunk) {
        int width     = readInt(chunk.data, 0);
        int height    = readInt(chunk.data, 4);
        int bitDepth  = chunk.data[8]  & 0xFF;
        int colorType = chunk.data[9]  & 0xFF;
        int interlace = chunk.data[12] & 0xFF;

        String colorTypeName = switch (colorType) {
            case 0 -> "Grayscale";
            case 2 -> "RGB";
            case 3 -> "Indexed (palette)";
            case 4 -> "Grayscale + Alpha";
            case 6 -> "RGBA";
            default -> "Unknown (" + colorType + ")";
        };

        parent.add(new DefaultMutableTreeNode("Width     : " + width + " px"));
        parent.add(new DefaultMutableTreeNode("Height    : " + height + " px"));
        parent.add(new DefaultMutableTreeNode("Bit Depth : " + bitDepth));
        parent.add(new DefaultMutableTreeNode("Color Type: " + colorTypeName));
        parent.add(new DefaultMutableTreeNode("Interlace : " + (interlace == 1 ? "Adam7" : "None")));

        // Validate dimensions match file size
        long expectedMax = (long) width * height * 4;
        if (fileBytes.length > expectedMax) {
            parent.add(new DefaultMutableTreeNode(
                    "⚠️ File larger than expected max (" + expectedMax + " bytes)!"));
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Show chunk detail in right pane
    // ══════════════════════════════════════════════════════
    private void showChunkDetail(PNGChunk chunk) {
        clearPane(detailPane);

        // Header
        appendText(detailPane,
                "═".repeat(48) + "\n", new Color(60, 120, 60));
        appendText(detailPane,
                "  CHUNK: [" + chunk.type + "]\n", Color.WHITE);
        appendText(detailPane,
                "═".repeat(48) + "\n\n", new Color(60, 120, 60));

        // Basic info
        appendText(detailPane, "── Basic Info ──\n", new Color(100, 200, 255));
        appendText(detailPane,
                "Type       : " + chunk.type + "\n" +
                        "Offset     : " + chunk.offset + " (0x"
                        + String.format("%X", chunk.offset) + ")\n" +
                        "Data Length: " + chunk.dataLength + " bytes\n" +
                        "Standard   : " + (chunk.isStandard ? "✅ Yes" : "⚠️ NO — custom chunk!") + "\n\n",
                new Color(0, 255, 100));

        // CRC check
        appendText(detailPane, "── CRC Integrity ──\n", new Color(100, 200, 255));
        appendText(detailPane,
                "Stored CRC  : 0x" + String.format("%08X", chunk.storedCRC) + "\n" +
                        "Computed CRC: 0x" + String.format("%08X", chunk.computedCRC) + "\n",
                new Color(0, 255, 100));
        if (chunk.crcOk) {
            appendText(detailPane, "Result      : ✅ CRC OK\n\n", new Color(0, 200, 80));
        } else {
            appendText(detailPane,
                    "Result      : 🔴 CRC MISMATCH — data may be corrupted or modified!\n\n",
                    Color.RED);
        }

        // Type-specific details
        showTypeSpecificDetail(chunk);

        // Raw hex dump (first 256 bytes)
        appendText(detailPane, "── Raw Data (hex) ──\n", new Color(100, 200, 255));
        if (chunk.data.length == 0) {
            appendText(detailPane, "(empty)\n", new Color(150, 150, 150));
        } else {
            appendText(detailPane, hexDump(chunk.data, 256), new Color(0, 200, 150));
            if (chunk.data.length > 256) {
                appendText(detailPane,
                        "... (" + (chunk.data.length - 256) + " more bytes)\n",
                        new Color(150, 150, 150));
            }
        }

        // Flag pattern check
        String printable = extractPrintable(chunk.data);
        if (containsFlag(printable)) {
            appendText(detailPane, "\n🚩 FLAG PATTERN DETECTED IN THIS CHUNK!\n",
                    Color.YELLOW);
            appendText(detailPane, printable + "\n", Color.YELLOW);
        }
    }

    // ── Type-specific detail sections ──
    private void showTypeSpecificDetail(PNGChunk chunk) {
        switch (chunk.type) {
            case "IHDR"         -> showIHDRDetail(chunk);
            case "tEXt","iTXt" -> showTextDetail(chunk);   // merged — same logic
            case "zTXt"         -> showZTXtDetail(chunk);
            case "pHYs"         -> showPHYsDetail(chunk);
            case "tIME"         -> showTIMEDetail(chunk);
            case "????"         -> showAppendedDetail(chunk);
            default             -> { /* no special handling needed */ }
        }
    }

    private void showIHDRDetail(PNGChunk chunk) {
        if (chunk.data.length < 13) return;
        appendText(detailPane, "── IHDR Fields ──\n", new Color(100, 200, 255));
        int width     = readInt(chunk.data, 0);
        int height    = readInt(chunk.data, 4);
        int bitDepth  = chunk.data[8]  & 0xFF;
        int colorType = chunk.data[9]  & 0xFF;
        int compress  = chunk.data[10] & 0xFF;
        int filter    = chunk.data[11] & 0xFF;
        int interlace = chunk.data[12] & 0xFF;

        appendText(detailPane,
                "Width        : " + width + " px\n" +
                        "Height       : " + height + " px\n" +
                        "Bit Depth    : " + bitDepth + "\n" +
                        "Color Type   : " + colorType + "\n" +
                        "Compression  : " + compress + "\n" +
                        "Filter Method: " + filter + "\n" +
                        "Interlace    : " + interlace + "\n\n",
                new Color(0, 255, 100));

        // Size check
        long maxExpected = (long) width * height * 4;
        appendText(detailPane,
                "Expected max size: " + maxExpected + " bytes\n" +
                        "Actual file size : " + fileBytes.length + " bytes\n",
                new Color(0, 255, 100));
        if (fileBytes.length > maxExpected) {
            appendText(detailPane,
                    "⚠️ File is LARGER than expected — possible hidden data!\n\n",
                    Color.YELLOW);
        } else {
            appendText(detailPane, "✅ File size looks normal.\n\n",
                    new Color(0, 200, 80));
        }
    }

    private void showTextDetail(PNGChunk chunk) {
        appendText(detailPane, "── Text Content ──\n", new Color(100, 200, 255));
        String content = extractPrintable(chunk.data);
        appendText(detailPane, content + "\n\n", Color.WHITE);
    }

    private void showZTXtDetail(PNGChunk chunk) {
        appendText(detailPane, "── zTXt Compressed Text ──\n", new Color(100, 200, 255));
        // Find null separator between keyword and compressed data
        int sep = -1;
        for (int i = 0; i < chunk.data.length; i++) {
            if (chunk.data[i] == 0) { sep = i; break; }
        }
        if (sep < 0) {
            appendText(detailPane, "(malformed zTXt chunk)\n\n", Color.RED);
            return;
        }
        String keyword = new String(chunk.data, 0, sep);
        appendText(detailPane, "Keyword: " + keyword + "\n", new Color(0,255,100));

        // Decompress the text (skip keyword + null + compression method byte)
        int dataStart = sep + 2;
        if (dataStart >= chunk.data.length) {
            appendText(detailPane, "(no compressed data)\n\n", Color.ORANGE);
            return;
        }
        try {
            byte[] compressed = new byte[chunk.data.length - dataStart];
            System.arraycopy(chunk.data, dataStart, compressed, 0, compressed.length);
            byte[]   decompressed = decompress(compressed);
            String   text         = new String(decompressed);
            appendText(detailPane, "Decompressed: " + text + "\n\n", Color.WHITE);
            if (containsFlag(text)) {
                appendText(detailPane, "🚩 FLAG FOUND IN COMPRESSED TEXT!\n\n", Color.YELLOW);
            }
        } catch (Exception ex) {
            appendText(detailPane, "Decompression failed: " + ex.getMessage() + "\n\n",
                    Color.RED);
        }
    }

    private void showPHYsDetail(PNGChunk chunk) {
        if (chunk.data.length < 9) return;
        appendText(detailPane, "── pHYs Physical Dimensions ──\n", new Color(100, 200, 255));
        int xDPI  = readInt(chunk.data, 0);
        int yDPI  = readInt(chunk.data, 4);
        int unit  = chunk.data[8] & 0xFF;
        appendText(detailPane,
                "X pixels/unit: " + xDPI + "\n" +
                        "Y pixels/unit: " + yDPI + "\n" +
                        "Unit         : " + (unit == 1 ? "metre" : "unknown") + "\n\n",
                new Color(0, 255, 100));
    }

    private void showTIMEDetail(PNGChunk chunk) {
        if (chunk.data.length < 7) return;
        appendText(detailPane, "── tIME Timestamp ──\n", new Color(100, 200, 255));
        int year   = ((chunk.data[0] & 0xFF) << 8) | (chunk.data[1] & 0xFF);
        int month  = chunk.data[2] & 0xFF;
        int day    = chunk.data[3] & 0xFF;
        int hour   = chunk.data[4] & 0xFF;
        int minute = chunk.data[5] & 0xFF;
        int second = chunk.data[6] & 0xFF;
        appendText(detailPane,
                String.format("Date: %04d-%02d-%02d\nTime: %02d:%02d:%02d\n\n",
                        year, month, day, hour, minute, second),
                new Color(0, 255, 100));
    }

    private void showAppendedDetail(PNGChunk chunk) {
        appendText(detailPane, "── ⚠️ APPENDED DATA AFTER IEND ──\n",
                Color.YELLOW);
        appendText(detailPane,
                "This data exists AFTER the PNG IEND marker.\n" +
                        "It is NOT part of the PNG image.\n" +
                        "This is a STRONG indicator of hidden data!\n\n" +
                        "Size: " + chunk.dataLength + " bytes\n\n" +
                        "First bytes (hex):\n",
                Color.YELLOW);
        appendText(detailPane, hexDump(chunk.data, 64), new Color(0, 200, 150));
        appendText(detailPane,
                "\nUse 'Embedded File Scanner' tab to extract this data!\n\n",
                Color.ORANGE);
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Check all CRC errors and report
    // ══════════════════════════════════════════════════════
    private void checkCRCErrors() {
        if (chunks.isEmpty()) {
            showError("Please parse chunks first!");
            return;
        }
        clearPane(detailPane);
        appendText(detailPane,
                "═".repeat(48) + "\n  CRC INTEGRITY REPORT\n" +
                        "═".repeat(48) + "\n\n",
                new Color(60, 120, 60));

        long errors = chunks.stream().filter(c -> !c.crcOk && !c.type.equals("????")).count();

        if (errors == 0) {
            appendText(detailPane,
                    "✅ All " + chunks.size() + " chunks have valid CRC values.\n" +
                            "No data corruption or CRC manipulation detected.\n",
                    new Color(0, 200, 80));
        } else {
            appendText(detailPane,
                    "🔴 Found " + errors + " CRC error(s)!\n\n", Color.RED);
            for (PNGChunk chunk : chunks) {
                if (!chunk.crcOk && !chunk.type.equals("????")) {
                    appendText(detailPane,
                            "Chunk [" + chunk.type + "] at offset "
                                    + chunk.offset + ":\n",
                            Color.ORANGE);
                    appendText(detailPane,
                            "  Stored  : 0x" + String.format("%08X", chunk.storedCRC) + "\n" +
                                    "  Computed: 0x" + String.format("%08X", chunk.computedCRC) + "\n" +
                                    "  → Data may be modified or corrupted!\n\n",
                            Color.RED);
                }
            }
        }
        statusLabel.setText("CRC check complete — " + errors + " error(s) found");
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Export selected chunk data to file
    // ══════════════════════════════════════════════════════
    private void exportSelectedChunk() {
        DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) chunkTree.getLastSelectedPathComponent();

        if (node == null || !(node.getUserObject() instanceof PNGChunk chunk)) {
            showError("Please select a chunk in the tree first!");
            return;
        }
        if (chunk.data.length == 0) {
            showError("Selected chunk has no data to export.");
            return;
        }

        String defaultName = "chunk_" + chunk.type + "_offset" + chunk.offset + ".bin";
        File   outFile     = openSaveDialog(defaultName);
        if (outFile == null) return;

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(chunk.data);
            JOptionPane.showMessageDialog(this,
                    "Saved " + chunk.data.length + " bytes to: " + outFile.getName(),
                    "Exported", JOptionPane.INFORMATION_MESSAGE);
            statusLabel.setText("Exported chunk [" + chunk.type + "] → " + outFile.getName());
        } catch (Exception ex) {
            showError("Error saving: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // TREE HELPERS
    // ══════════════════════════════════════════════════════
    private void expandAll() {
        for (int i = 0; i < chunkTree.getRowCount(); i++) chunkTree.expandRow(i);
    }

    private void collapseAll() {
        for (int i = chunkTree.getRowCount() - 1; i > 0; i--) chunkTree.collapseRow(i);
    }

    // ══════════════════════════════════════════════════════
    // UTILITY: PNG / byte helpers
    // ══════════════════════════════════════════════════════
    private boolean isPNG(byte[] data) {
        if (data.length < 8) return false;
        for (int i = 0; i < PNG_SIG.length; i++) {
            if (data[i] != PNG_SIG[i]) return false;
        }
        return true;
    }

    private int readInt(byte[] data, int offset) {
        return ((data[offset]   & 0xFF) << 24)
                | ((data[offset+1] & 0xFF) << 16)
                | ((data[offset+2] & 0xFF) <<  8)
                |  (data[offset+3] & 0xFF);
    }

    private long readUnsignedInt(byte[] data, int offset) {
        return readInt(data, offset) & 0xFFFFFFFFL;
    }

    // CRC-32 computation (standard PNG CRC)
    private static final long[] CRC_TABLE = buildCRCTable();

    private static long[] buildCRCTable() {
        long[] table = new long[256];
        for (int n = 0; n < 256; n++) {
            long c = n;
            for (int k = 0; k < 8; k++) {
                c = ((c & 1) != 0) ? (0xEDB88320L ^ (c >>> 1)) : (c >>> 1);
            }
            table[n] = c;
        }
        return table;
    }

    private long computeCRC(byte[] data, int offset, int length) {
        long crc = 0xFFFFFFFFL;
        for (int i = offset; i < offset + length; i++) {
            crc = CRC_TABLE[(int)((crc ^ data[i]) & 0xFF)] ^ (crc >>> 8);
        }
        return crc ^ 0xFFFFFFFFL;
    }

    // Decompress zlib/deflate data
    private byte[] decompress(byte[] data) throws Exception {
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        byte[] buffer = new byte[4096];
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            baos.write(buffer, 0, count);
        }
        inflater.end();
        return baos.toByteArray();
    }

    // Extract printable ASCII from byte array
    private String extractPrintable(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            char c = (char)(b & 0xFF);
            if (c >= 32 && c < 127) sb.append(c);
            else if (!sb.isEmpty()) sb.append(' ');
        }
        return sb.toString().trim();
    }

    // Check for CTF flag patterns
    private boolean containsFlag(String text) {
        return text.toLowerCase().contains("flag{")
                || text.toLowerCase().contains("ctf{")
                || text.toLowerCase().contains("htb{")
                || text.toLowerCase().contains("thm{")
                || text.toLowerCase().contains("pico{");
    }

    // Hex dump of bytes (up to maxBytes)
    private String hexDump(byte[] data, int maxBytes) {
        StringBuilder sb   = new StringBuilder();
        StringBuilder asc  = new StringBuilder();
        int           limit = Math.min(data.length, maxBytes);
        int           cols  = 16;

        for (int i = 0; i < limit; i++) {
            if (i % cols == 0) sb.append(String.format("%04X  ", i));
            sb.append(String.format("%02X ", data[i]));
            char c = (char)(data[i] & 0xFF);
            asc.append((c >= 32 && c < 127) ? c : '.');
            if ((i + 1) % cols == 0 || i == limit - 1) {
                int pad = cols - ((i % cols) + 1);
                sb.append("   ".repeat(pad));
                sb.append("  ").append(asc).append("\n");
                asc.setLength(0);
            }
        }
        return sb.toString();
    }

    // Returns hex string of first 8 bytes of data
    private String toHexString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(data.length, 8); i++) {
            sb.append(String.format("%02X ", data[i]));
        }
        return sb.toString().trim();
    }

    // ══════════════════════════════════════════════════════
    // CUSTOM TREE CELL RENDERER
    // ══════════════════════════════════════════════════════
    private static class ChunkTreeRenderer extends DefaultTreeCellRenderer {

        ChunkTreeRenderer() {
            setBackgroundNonSelectionColor(new Color(25, 25, 25));
            setBackgroundSelectionColor(new Color(50, 80, 50));
            setBorderSelectionColor(new Color(0, 200, 80));
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(
                    tree, value, selected, expanded, leaf, row, hasFocus);

            setFont(new Font("Monospaced", Font.PLAIN, 12));
            setBackground(selected ? new Color(50, 80, 50) : new Color(25, 25, 25));

            if (value instanceof DefaultMutableTreeNode node
                    && node.getUserObject() instanceof PNGChunk chunk) {
                if (chunk.type.equals("????")) {
                    setForeground(Color.YELLOW);
                } else if (!chunk.isStandard) {
                    setForeground(Color.ORANGE);
                } else if (!chunk.crcOk) {
                    setForeground(Color.RED);
                } else {
                    setForeground(new Color(0, 220, 80));
                }
            } else {
                setForeground(new Color(180, 180, 220));
            }
            return this;
        }
    }

    // ══════════════════════════════════════════════════════
    // FILE CHOOSER HELPERS
    // ══════════════════════════════════════════════════════
    private File openFileChooser() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load PNG File");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "PNG Image", "png"));
        return fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
                ? fc.getSelectedFile() : null;
    }

    private File openSaveDialog(String defaultName) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export Chunk Data");
        fc.setSelectedFile(new File(defaultName));
        return fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION
                ? fc.getSelectedFile() : null;
    }

    // ══════════════════════════════════════════════════════
    // DISPLAY HELPERS
    // ══════════════════════════════════════════════════════
    private void showDetailWelcome() {
        clearPane(detailPane);
        appendText(detailPane, """
                ╔══════════════════════════════════════════════════╗
                ║         PNG CHUNK TREE VIEWER                   ║
                ╚══════════════════════════════════════════════════╝

                HOW TO USE:
                  1. Load a PNG file
                  2. Click 'Parse Chunks'
                  3. Click any chunk in the tree to see details
                  4. Look for:
                     ⚠️  Non-standard chunks (custom/hidden data)
                     🔴  CRC errors (data may be modified)
                     🚩  Data after IEND (appended hidden file!)

                WHAT TO LOOK FOR:
                  tEXt / iTXt  → flag hidden in text fields
                  zTXt         → flag in compressed text
                  Custom chunk → entirely custom hidden data
                  After IEND   → another file glued to end
                  Wrong IHDR   → dimensions hide content

                COLOUR LEGEND:
                  🟢 Green  = standard chunk, CRC OK
                  🟡 Orange = non-standard chunk type
                  🔴 Red    = CRC mismatch detected
                  🟡 Yellow = data after IEND marker
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

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
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