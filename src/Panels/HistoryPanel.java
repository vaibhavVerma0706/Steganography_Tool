package Panels;

import db.HistoryDAO;
import model.HistoryRecord;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;

public class HistoryPanel extends JPanel {

    // ── Fields ──
    private final User currentUser;

    // ── UI ──
    private JTable            historyTable;
    private DefaultTableModel tableModel;
    private JLabel            statsLabel;
    private JLabel            statusLabel;
    private JComboBox<String> filterBox;

    // Column indices
    private static final int COL_ID         = 0;
    private static final int COL_DATE       = 1;
    private static final int COL_ACTION     = 2;
    private static final int COL_IMAGE      = 3;
    private static final int COL_SIZE       = 4;
    private static final int COL_TYPE       = 5;
    private static final int COL_HIDDEN     = 6;

    // ──────────────────────────────────────────────────────
    // Constructor — receives logged-in user
    // ──────────────────────────────────────────────────────
    public HistoryPanel(User user) {
        this.currentUser = user;

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(new Color(245, 245, 250));

        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildTableArea(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        // Load data on open
        loadHistory();
    }

    // ──────────────────────────────────────────────────────
    // TOP BAR — title + filter + buttons
    // ──────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel panel = new JPanel(new BorderLayout(10, 8));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder(
                "Encode / Decode History — " + currentUser.getUsername()));

        // Stats label
        statsLabel = new JLabel("Loading...");
        statsLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statsLabel.setForeground(new Color(60, 60, 120));

        // Filter + buttons row
        JPanel ctrlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        ctrlRow.setBackground(new Color(245, 245, 250));

        ctrlRow.add(makeBoldLabel("Show:"));
        filterBox = new JComboBox<>(
                new String[]{"All", "ENCODE only", "DECODE only"});
        filterBox.setFont(new Font("Arial", Font.PLAIN, 12));
        filterBox.addActionListener(e -> applyFilter());
        ctrlRow.add(filterBox);

        JButton refreshBtn = makeBtn("🔄 Refresh",      new Color(70, 130, 180));
        JButton deleteBtn  = makeBtn("🗑️ Delete Selected", new Color(160, 60, 60));
        JButton clearBtn   = makeBtn("❌ Clear All",    new Color(120, 40, 40));

        refreshBtn.addActionListener(e -> loadHistory());
        deleteBtn.addActionListener(e  -> deleteSelected());
        clearBtn.addActionListener(e   -> clearAll());

        ctrlRow.add(refreshBtn);
        ctrlRow.add(deleteBtn);
        ctrlRow.add(clearBtn);

        panel.add(statsLabel, BorderLayout.NORTH);
        panel.add(ctrlRow,    BorderLayout.CENTER);
        return panel;
    }

    // ──────────────────────────────────────────────────────
    // TABLE AREA
    // ──────────────────────────────────────────────────────
    private JPanel buildTableArea() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 250));

        // Table model — ID column hidden from user but used for delete
        String[] columns = {"ID", "Date & Time", "Action",
                "Image Name", "Image Size", "Hidden Type", "Hidden File"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false; // read-only table
            }
        };

        historyTable = new JTable(tableModel);
        historyTable.setFont(new Font("Arial", Font.PLAIN, 12));
        historyTable.setRowHeight(24);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.setGridColor(new Color(220, 220, 235));
        historyTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        historyTable.getTableHeader().setBackground(new Color(70, 100, 180));
        historyTable.getTableHeader().setForeground(Color.WHITE);

        // Hide the ID column (index 0) — used internally only
        historyTable.getColumnModel().getColumn(COL_ID).setMinWidth(0);
        historyTable.getColumnModel().getColumn(COL_ID).setMaxWidth(0);
        historyTable.getColumnModel().getColumn(COL_ID).setWidth(0);

        // Column widths
        historyTable.getColumnModel().getColumn(COL_DATE).setPreferredWidth(140);
        historyTable.getColumnModel().getColumn(COL_ACTION).setPreferredWidth(70);
        historyTable.getColumnModel().getColumn(COL_IMAGE).setPreferredWidth(180);
        historyTable.getColumnModel().getColumn(COL_SIZE).setPreferredWidth(90);
        historyTable.getColumnModel().getColumn(COL_TYPE).setPreferredWidth(80);
        historyTable.getColumnModel().getColumn(COL_HIDDEN).setPreferredWidth(180);

        // Alternating row colours
        historyTable.setDefaultRenderer(Object.class,
                new javax.swing.table.DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(
                            JTable table, Object value, boolean isSelected,
                            boolean hasFocus, int row, int col) {
                        super.getTableCellRendererComponent(
                                table, value, isSelected, hasFocus, row, col);
                        if (!isSelected) {
                            setBackground(row % 2 == 0
                                    ? Color.WHITE : new Color(240, 242, 255));
                        }
                        // Colour the Action column
                        if (col == COL_ACTION && value != null) {
                            if ("ENCODE".equals(value.toString())) {
                                setForeground(new Color(34, 100, 34));
                            } else {
                                setForeground(new Color(34, 34, 180));
                            }
                        } else {
                            setForeground(Color.BLACK);
                        }
                        return this;
                    }
                });

        // Allow sorting by clicking column headers
        TableRowSorter<DefaultTableModel> sorter =
                new TableRowSorter<>(tableModel);
        historyTable.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(historyTable);
        scroll.setBorder(BorderFactory.createLineBorder(
                new Color(180, 180, 220)));
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

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(180, 220, 180));

        JLabel hint = new JLabel(
                "Click a column header to sort  |  Select a row then click Delete to remove");
        hint.setFont(new Font("Arial", Font.ITALIC, 10));
        hint.setForeground(new Color(180, 180, 220));

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(hint,        BorderLayout.EAST);
        return panel;
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Load history from database
    // ══════════════════════════════════════════════════════
    public void loadHistory() {
        tableModel.setRowCount(0); // clear existing rows

        List<HistoryRecord> records =
                HistoryDAO.getHistory(currentUser.getId());

        for (HistoryRecord r : records) {
            tableModel.addRow(new Object[]{
                    r.getId(),
                    r.getTimestamp(),
                    r.getAction(),
                    r.getImageName(),
                    formatSize(r.getImageSize()),
                    r.getHiddenType(),
                    r.getHiddenName()
            });
        }

        updateStats(records);
        applyFilter();
        statusLabel.setText("Loaded " + records.size() + " record(s).");
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Apply filter (All / ENCODE only / DECODE only)
    // ══════════════════════════════════════════════════════
    private void applyFilter() {
        String selected = (String) filterBox.getSelectedItem();
        if (selected == null) return;

        TableRowSorter<DefaultTableModel> sorter =
                (TableRowSorter<DefaultTableModel>) historyTable.getRowSorter();

        switch (selected) {
            case "ENCODE only" -> sorter.setRowFilter(
                    RowFilter.regexFilter("ENCODE", COL_ACTION));
            case "DECODE only" -> sorter.setRowFilter(
                    RowFilter.regexFilter("DECODE", COL_ACTION));
            default -> sorter.setRowFilter(null);
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Delete selected row
    // ══════════════════════════════════════════════════════
    private void deleteSelected() {
        int viewRow = historyTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select a row to delete.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = historyTable.convertRowIndexToModel(viewRow);
        int recordId = (int) tableModel.getValueAt(modelRow, COL_ID);
        String image = (String) tableModel.getValueAt(modelRow, COL_IMAGE);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete record for: " + image + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            HistoryDAO.deleteRecord(recordId);
            loadHistory();
            statusLabel.setText("Record deleted.");
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Clear all history for this user
    // ══════════════════════════════════════════════════════
    private void clearAll() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "History is already empty.",
                    "Nothing to Clear", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete ALL history records for "
                        + currentUser.getUsername() + "?\n"
                        + "This cannot be undone!",
                "Confirm Clear All", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            HistoryDAO.clearAllHistory(currentUser.getId());
            loadHistory();
            statusLabel.setText("All history cleared.");
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Update stats label
    // ══════════════════════════════════════════════════════
    private void updateStats(List<HistoryRecord> records) {
        long encodes = records.stream()
                .filter(r -> "ENCODE".equals(r.getAction())).count();
        long decodes = records.stream()
                .filter(r -> "DECODE".equals(r.getAction())).count();
        long files   = records.stream()
                .filter(r -> "FILE".equals(r.getHiddenType())).count();

        statsLabel.setText(
                "Total: " + records.size()
                        + "   |   Encodes: " + encodes
                        + "   |   Decodes: " + decodes
                        + "   |   Files hidden: " + files);
    }

    // ══════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════
    private String formatSize(long bytes) {
        if (bytes <= 0)          return "—";
        if (bytes < 1024)        return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
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
