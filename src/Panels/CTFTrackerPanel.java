package Panels;

import db.CTFTrackerDAO;
import model.CTFChallenge;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;

public class CTFTrackerPanel extends JPanel {

    // ── Fields ──
    private final User currentUser;

    // ── UI ──
    private JTable            challengeTable;
    private DefaultTableModel tableModel;
    private JLabel            statsLabel;
    private JLabel            statusLabel;

    // Detail panel fields (shown when row selected)
    private JLabel     detailNameLabel;
    private JLabel     detailImageLabel;
    private JLabel     detailStatusLabel;
    private JLabel     detailCreatedLabel;
    private JLabel     detailSolvedLabel;
    private JTextField flagField;
    private JTextArea  notesArea;
    private JComboBox<String> statusBox;
    private JButton    saveFlagBtn;
    private JButton    saveNotesBtn;
    private JButton    updateStatusBtn;

    // Currently selected challenge
    private CTFChallenge selectedChallenge;

    // Column indices
    private static final int COL_ID     = 0;
    private static final int COL_NAME   = 1;
    private static final int COL_IMAGE  = 2;
    private static final int COL_STATUS = 3;
    private static final int COL_FLAG   = 4;
    private static final int COL_DATE   = 5;

    // ──────────────────────────────────────────────────────
    // Constructor
    // ──────────────────────────────────────────────────────
    public CTFTrackerPanel(User user) {
        this.currentUser = user;

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(new Color(245, 245, 250));

        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildMainArea(),  BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        loadChallenges();
    }

    // ──────────────────────────────────────────────────────
    // TOP BAR
    // ──────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel panel = new JPanel(new BorderLayout(10, 8));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder(
                "CTF Challenge Tracker — " + currentUser.getUsername()));

        statsLabel = new JLabel("Loading...");
        statsLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statsLabel.setForeground(new Color(60, 60, 120));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRow.setBackground(new Color(245, 245, 250));

        JButton addBtn     = makeBtn("➕ Add Challenge",     new Color(34, 139, 34));
        JButton refreshBtn = makeBtn("🔄 Refresh",           new Color(70, 130, 180));
        JButton deleteBtn  = makeBtn("🗑️ Delete Selected",  new Color(160, 60, 60));

        addBtn.addActionListener(e     -> showAddChallengeDialog());
        refreshBtn.addActionListener(e -> loadChallenges());
        deleteBtn.addActionListener(e  -> deleteSelected());

        btnRow.add(addBtn);
        btnRow.add(refreshBtn);
        btnRow.add(deleteBtn);

        panel.add(statsLabel, BorderLayout.NORTH);
        panel.add(btnRow,     BorderLayout.CENTER);
        return panel;
    }

    // ──────────────────────────────────────────────────────
    // MAIN AREA — table left, detail panel right
    // ──────────────────────────────────────────────────────
    private JPanel buildMainArea() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(new Color(245, 245, 250));
        panel.add(buildTablePanel(),  BorderLayout.CENTER);
        panel.add(buildDetailPanel(), BorderLayout.EAST);
        return panel;
    }

    // ── Challenge table ──
    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(new TitledBorder("Challenges"));

        String[] cols = {"ID", "Challenge Name", "Image",
                "Status", "Flag Found", "Created"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        challengeTable = new JTable(tableModel);
        challengeTable.setFont(new Font("Arial", Font.PLAIN, 12));
        challengeTable.setRowHeight(24);
        challengeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        challengeTable.setGridColor(new Color(220, 220, 235));
        challengeTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        challengeTable.getTableHeader().setBackground(new Color(60, 80, 160));
        challengeTable.getTableHeader().setForeground(Color.WHITE);

        // Hide ID column
        challengeTable.getColumnModel().getColumn(COL_ID).setMinWidth(0);
        challengeTable.getColumnModel().getColumn(COL_ID).setMaxWidth(0);
        challengeTable.getColumnModel().getColumn(COL_ID).setWidth(0);

        // Column widths
        challengeTable.getColumnModel().getColumn(COL_NAME).setPreferredWidth(160);
        challengeTable.getColumnModel().getColumn(COL_IMAGE).setPreferredWidth(140);
        challengeTable.getColumnModel().getColumn(COL_STATUS).setPreferredWidth(90);
        challengeTable.getColumnModel().getColumn(COL_FLAG).setPreferredWidth(160);
        challengeTable.getColumnModel().getColumn(COL_DATE).setPreferredWidth(130);

        // Colour status column
        challengeTable.setDefaultRenderer(Object.class,
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
                        if (col == COL_STATUS && value != null) {
                            setForeground(switch (value.toString()) {
                                case "SOLVED"      -> new Color(0, 140, 0);
                                case "IN PROGRESS" -> new Color(180, 100, 0);
                                default            -> new Color(180, 0, 0);
                            });
                            setFont(new Font("Arial", Font.BOLD, 12));
                        } else {
                            setForeground(Color.BLACK);
                            setFont(new Font("Arial", Font.PLAIN, 12));
                        }
                        return this;
                    }
                });

        // Row sorter
        TableRowSorter<DefaultTableModel> sorter =
                new TableRowSorter<>(tableModel);
        challengeTable.setRowSorter(sorter);

        // Show details when row selected
        challengeTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelectedDetail();
        });

        JScrollPane scroll = new JScrollPane(challengeTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 220)));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ── Detail panel (right side) ──
    private JPanel buildDetailPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(245, 245, 255));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder("Challenge Details"),
                new EmptyBorder(8, 10, 8, 10)));
        panel.setPreferredSize(new Dimension(260, 0));

        // Info labels
        detailNameLabel   = makeDetailLabel("Name: —");
        detailImageLabel  = makeDetailLabel("Image: —");
        detailStatusLabel = makeDetailLabel("Status: —");
        detailCreatedLabel= makeDetailLabel("Created: —");
        detailSolvedLabel = makeDetailLabel("Solved: —");

        panel.add(detailNameLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(detailImageLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(detailStatusLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(detailCreatedLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(detailSolvedLabel);
        panel.add(Box.createVerticalStrut(14));

        // Status update
        panel.add(makeSectionLabel("── Update Status ──"));
        panel.add(Box.createVerticalStrut(4));
        statusBox = new JComboBox<>(
                new String[]{"UNSOLVED", "IN PROGRESS", "SOLVED"});
        statusBox.setFont(new Font("Arial", Font.PLAIN, 12));
        statusBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        statusBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(statusBox);
        panel.add(Box.createVerticalStrut(6));

        updateStatusBtn = makeBtn("💾 Update Status", new Color(70, 130, 180));
        updateStatusBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        updateStatusBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        updateStatusBtn.addActionListener(e -> updateStatus());
        panel.add(updateStatusBtn);
        panel.add(Box.createVerticalStrut(14));

        // Flag field
        panel.add(makeSectionLabel("── Flag Found ──"));
        panel.add(Box.createVerticalStrut(4));
        flagField = new JTextField();
        flagField.setFont(new Font("Monospaced", Font.PLAIN, 11));
        flagField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        flagField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(flagField);
        panel.add(Box.createVerticalStrut(6));

        saveFlagBtn = makeBtn("🚩 Save Flag", new Color(180, 120, 0));
        saveFlagBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        saveFlagBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveFlagBtn.addActionListener(e -> saveFlag());
        panel.add(saveFlagBtn);
        panel.add(Box.createVerticalStrut(14));

        // Notes
        panel.add(makeSectionLabel("── Notes ──"));
        panel.add(Box.createVerticalStrut(4));
        notesArea = new JTextArea(5, 1);
        notesArea.setFont(new Font("Arial", Font.PLAIN, 11));
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBorder(new EmptyBorder(4, 4, 4, 4));
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        notesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(notesScroll);
        panel.add(Box.createVerticalStrut(6));

        saveNotesBtn = makeBtn("📝 Save Notes", new Color(100, 60, 160));
        saveNotesBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        saveNotesBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveNotesBtn.addActionListener(e -> saveNotes());
        panel.add(saveNotesBtn);

        // Disable detail panel until row selected
        setDetailEnabled(false);
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
                "Select a challenge to edit its status, flag and notes");
        hint.setFont(new Font("Arial", Font.ITALIC, 10));
        hint.setForeground(new Color(180, 180, 220));

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(hint,        BorderLayout.EAST);
        return panel;
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Load challenges from database
    // ══════════════════════════════════════════════════════
    public void loadChallenges() {
        tableModel.setRowCount(0);

        List<CTFChallenge> challenges =
                CTFTrackerDAO.getChallenges(currentUser.getId());

        for (CTFChallenge c : challenges) {
            tableModel.addRow(new Object[]{
                    c.getId(),
                    c.getName(),
                    c.getImageName(),
                    c.getStatus(),
                    c.getFlagFound().isEmpty() ? "—" : c.getFlagFound(),
                    c.getCreatedAt()
            });
        }

        updateStats(challenges);
        statusLabel.setText("Loaded " + challenges.size() + " challenge(s).");
        selectedChallenge = null;
        setDetailEnabled(false);
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Show selected challenge details
    // ══════════════════════════════════════════════════════
    private void showSelectedDetail() {
        int viewRow = challengeTable.getSelectedRow();
        if (viewRow < 0) {
            setDetailEnabled(false);
            return;
        }

        int modelRow   = challengeTable.convertRowIndexToModel(viewRow);
        int id         = (int) tableModel.getValueAt(modelRow, COL_ID);

        // Find the challenge object
        List<CTFChallenge> challenges =
                CTFTrackerDAO.getChallenges(currentUser.getId());
        selectedChallenge = challenges.stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElse(null);

        if (selectedChallenge == null) return;

        // Update detail labels
        detailNameLabel.setText("Name   : " + selectedChallenge.getName());
        detailImageLabel.setText("Image  : "
                + (selectedChallenge.getImageName().isEmpty()
                ? "—" : selectedChallenge.getImageName()));
        detailStatusLabel.setText("Status : " + selectedChallenge.getStatus());
        detailCreatedLabel.setText("Created: " + selectedChallenge.getCreatedAt());
        detailSolvedLabel.setText("Solved : "
                + (selectedChallenge.getSolvedAt().isEmpty()
                ? "—" : selectedChallenge.getSolvedAt()));

        // Populate editable fields
        statusBox.setSelectedItem(selectedChallenge.getStatus());
        flagField.setText(selectedChallenge.getFlagFound());
        notesArea.setText(selectedChallenge.getNotes());

        setDetailEnabled(true);
        statusLabel.setText("Selected: " + selectedChallenge.getName());
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Add new challenge dialog
    // ══════════════════════════════════════════════════════
    private void showAddChallengeDialog() {
        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextField nameField  = new JTextField(20);
        JTextField imageField = new JTextField(20);

        form.add(new JLabel("Challenge Name:"));
        form.add(nameField);
        form.add(new JLabel("Image Filename:"));
        form.add(imageField);
        form.add(new JLabel("(optional)"));
        form.add(new JLabel(""));

        int result = JOptionPane.showConfirmDialog(this,
                form, "Add New CTF Challenge",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a challenge name.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean saved = CTFTrackerDAO.addChallenge(
                currentUser.getId(), name, imageField.getText().trim());

        if (saved) {
            loadChallenges();
            statusLabel.setText("Challenge added: " + name);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Failed to save. Check database connection.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Update status of selected challenge
    // ══════════════════════════════════════════════════════
    private void updateStatus() {
        if (selectedChallenge == null) return;

        String newStatus = (String) statusBox.getSelectedItem();
        if (newStatus == null) return;

        CTFTrackerDAO.updateStatus(selectedChallenge.getId(), newStatus);
        loadChallenges();
        statusLabel.setText("Status updated to: " + newStatus);
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Save flag for selected challenge
    // ══════════════════════════════════════════════════════
    private void saveFlag() {
        if (selectedChallenge == null) return;

        String flag = flagField.getText().trim();
        if (flag.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a flag to save.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        CTFTrackerDAO.saveFlag(selectedChallenge.getId(), flag);
        loadChallenges();
        statusLabel.setText("Flag saved: " + flag);
        JOptionPane.showMessageDialog(this,
                "✅ Flag saved and challenge marked as SOLVED!",
                "Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Save notes for selected challenge
    // ══════════════════════════════════════════════════════
    private void saveNotes() {
        if (selectedChallenge == null) return;

        CTFTrackerDAO.saveNotes(
                selectedChallenge.getId(), notesArea.getText());
        statusLabel.setText("Notes saved for: " + selectedChallenge.getName());
        JOptionPane.showMessageDialog(this,
                "✅ Notes saved!", "Saved",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Delete selected challenge
    // ══════════════════════════════════════════════════════
    private void deleteSelected() {
        int viewRow = challengeTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select a challenge to delete.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = challengeTable.convertRowIndexToModel(viewRow);
        int id       = (int) tableModel.getValueAt(modelRow, COL_ID);
        String name  = (String) tableModel.getValueAt(modelRow, COL_NAME);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete challenge: '" + name + "'?\nThis cannot be undone!",
                "Confirm Delete", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            CTFTrackerDAO.deleteChallenge(id);
            loadChallenges();
            statusLabel.setText("Deleted: " + name);
        }
    }

    // ══════════════════════════════════════════════════════
    // LOGIC: Update stats label
    // ══════════════════════════════════════════════════════
    private void updateStats(List<CTFChallenge> challenges) {
        long solved     = challenges.stream()
                .filter(c -> "SOLVED".equals(c.getStatus())).count();
        long inProgress = challenges.stream()
                .filter(c -> "IN PROGRESS".equals(c.getStatus())).count();
        long unsolved   = challenges.stream()
                .filter(c -> "UNSOLVED".equals(c.getStatus())).count();

        statsLabel.setText(
                "Total: " + challenges.size()
                        + "   |   ✅ Solved: " + solved
                        + "   |   🔄 In Progress: " + inProgress
                        + "   |   ❌ Unsolved: " + unsolved);
    }

    // ══════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════
    private void setDetailEnabled(boolean enabled) {
        statusBox.setEnabled(enabled);
        updateStatusBtn.setEnabled(enabled);
        flagField.setEnabled(enabled);
        saveFlagBtn.setEnabled(enabled);
        notesArea.setEnabled(enabled);
        saveNotesBtn.setEnabled(enabled);

        if (!enabled) {
            detailNameLabel.setText("Name   : —");
            detailImageLabel.setText("Image  : —");
            detailStatusLabel.setText("Status : —");
            detailCreatedLabel.setText("Created: —");
            detailSolvedLabel.setText("Solved : —");
            flagField.setText("");
            notesArea.setText("");
        }
    }

    private JLabel makeDetailLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", Font.PLAIN, 11));
        l.setForeground(new Color(50, 50, 100));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel makeSectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", Font.BOLD, 10));
        l.setForeground(new Color(80, 80, 140));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
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
