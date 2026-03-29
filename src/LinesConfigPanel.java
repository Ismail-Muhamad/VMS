
import DB.DB;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class LinesConfigPanel extends JPanel {

    private JTable linesTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    private JTextField txtSearch;

    private JTextField txtLineName;
    private JSpinner spDistance;
    private JSpinner spFuel;
    private JSpinner spApproxMinutes;

//    private Integer selectedLineId = null;

    public LinesConfigPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(243, 244, 246));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JPanel main = new JPanel(new BorderLayout(16, 16));
        main.setBorder(new EmptyBorder(16, 16, 16, 16));
        main.setOpaque(false);

        JPanel tablePanel = createLinesTablePanel();
        JPanel formPanel = createFormPanel();

        main.add(tablePanel, BorderLayout.CENTER);
        main.add(formPanel, BorderLayout.EAST);

        add(main, BorderLayout.CENTER);

        refreshAll();
    }

    //===== كارت بكورنرات ناعمة + ظل =====
    private static class CardPanel extends JPanel {

        private final int arc = 18;
        private final int shadowSize = 8;

        public CardPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = shadowSize;
            int y = shadowSize;
            int w = getWidth() - shadowSize * 2;
            int h = getHeight() - shadowSize * 2;

            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillRoundRect(x + 2, y + 3, w, h, arc, arc);

            g2.setColor(getBackground());
            g2.fillRoundRect(x, y, w, h, arc, arc);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    private ImageIcon loadIcon(String path, int size) {
        ImageIcon icon = new ImageIcon(path);
        if (icon.getIconWidth() <= 0) {
            return null;
        }
        Image img = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    //=========== جدول الخطوط + البحث ===========
    private JPanel createLinesTablePanel() {
        CardPanel panel = new CardPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setLayout(new BorderLayout(8, 8));

        JLabel title = new JLabel("قائمة الخطوط");
        title.setFont(new Font("Tahoma", Font.BOLD, 18));
        title.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel filtersPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        filtersPanel.setOpaque(false);

        txtSearch = new JTextField();
        txtSearch.setHorizontalAlignment(SwingConstants.RIGHT);
        txtSearch.setFont(new Font("Tahoma", Font.PLAIN, 13));
        TitledBorder searchBorder = BorderFactory.createTitledBorder("بحث باسم الخط");
        searchBorder.setTitleJustification(TitledBorder.RIGHT);
        txtSearch.setBorder(searchBorder);

        filtersPanel.add(txtSearch);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(filtersPanel, BorderLayout.SOUTH);

        tableModel = new DefaultTableModel(new Object[][]{}, new Object[]{
            "ID", "اسم الخط", "عدد السيارات علي الخط", "المسافة (كم)", "استهلاك البنزين (لتر)", "الوقت التقريبي"
        }) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        linesTable = new JTable(tableModel);
        linesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        linesTable.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            loadSelectedRowToForm();
        });

        styleLinesTable(linesTable);

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyLinesFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyLinesFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyLinesFilter();
            }
        });

        JScrollPane scroll = new JScrollPane(linesTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void styleLinesTable(JTable t) {
        t.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        t.setFont(new Font("Tahoma", Font.PLAIN, 13));
        t.setRowHeight(28);
        t.setGridColor(new Color(220, 220, 220));
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setSelectionBackground(new Color(37, 99, 235));
        t.setSelectionForeground(Color.WHITE);

        JTableHeader header = t.getTableHeader();
        header.setFont(new Font("Tahoma", Font.BOLD, 13));
        header.setBackground(new Color(230, 235, 245));
        header.setForeground(new Color(30, 41, 59));
        ((DefaultTableCellRenderer) header.getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(center);
        }
    }

    private void applyLinesFilter() {
        if (sorter == null) {
            return;
        }

        String txt = txtSearch.getText().trim();

        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        if (!txt.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(txt), 1));
        }

        if (filters.isEmpty()) {
            sorter.setRowFilter(null);
        } else if (filters.size() == 1) {
            sorter.setRowFilter(filters.get(0));
        } else {
            sorter.setRowFilter(RowFilter.andFilter(filters));
        }
    }

    private JPanel createFormPanel() {
        CardPanel panel = new CardPanel();
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(360, 0));
        panel.setLayout(new BorderLayout(0, 12));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("بيانات الخط");
        title.setFont(new Font("Tahoma", Font.BOLD, 16));
        title.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.PAGE_START;

        txtLineName = new JTextField();
        spDistance = new JSpinner(new SpinnerNumberModel(10.0, 0.0, 100000.0, 0.5));
        spFuel = new JSpinner(new SpinnerNumberModel(5.0, 0.0, 100000.0, 0.1));
        spApproxMinutes = new JSpinner(new SpinnerNumberModel(30, 0, 100000, 5));

        tuneNumberSpinner(spDistance);
        tuneNumberSpinner(spFuel);
        tuneNumberSpinner(spApproxMinutes);

        addLabeledField(form, gbc, "اسم الخط", txtLineName);
        addLabeledField(form, gbc, "المسافة (كم)", spDistance);
        addLabeledField(form, gbc, "استهلاك البنزين (لتر)", spFuel);
        addLabeledField(form, gbc, "الوقت التقريبي (دقيقة)", spApproxMinutes);

        gbc.gridy++;
        gbc.weighty = 1;
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        form.add(spacer, gbc);

        JPanel buttonsPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        buttonsPanel.setOpaque(false);

        JButton btnAdd = new JButton("إضافة");
        JButton btnUpdate = new JButton("تعديل");
        JButton btnDelete = new JButton("حذف");
        JButton btnClear = new JButton("تفريغ");

        styleActionButton(btnAdd, new Color(22, 163, 74), "icons/add_line.png");
        styleActionButton(btnUpdate, new Color(37, 99, 235), "icons/edit_line.png");
        styleActionButton(btnDelete, new Color(220, 38, 38), "icons/delete_line.png");
        styleActionButton(btnClear, new Color(107, 114, 128), "icons/clear.png");

        btnAdd.addActionListener(e -> onAddLine());
        btnUpdate.addActionListener(e -> onUpdateLine());
        btnDelete.addActionListener(e -> onDeleteLine());
        btnClear.addActionListener(e -> clearForm());

        buttonsPanel.add(btnAdd);
        buttonsPanel.add(btnUpdate);
        buttonsPanel.add(btnDelete);
        buttonsPanel.add(btnClear);

        panel.add(title, BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        panel.add(buttonsPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void tuneNumberSpinner(JSpinner spinner) {
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.NumberEditor ne) {
            ne.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
            ne.getTextField().setFont(new Font("Tahoma", Font.PLAIN, 13));
        }
    }

    private void addLabeledField(JPanel panel, GridBagConstraints gbc, String labelText, JComponent field) {
        JLabel label = new JLabel(labelText);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setFont(new Font("Tahoma", Font.PLAIN, 13));

        gbc.gridx = 0;
        panel.add(label, gbc);
        gbc.gridy++;

        field.setFont(new Font("Tahoma", Font.PLAIN, 13));
        panel.add(field, gbc);
        gbc.gridy++;
    }

    private void styleActionButton(JButton button, Color bgColor, String iconPath) {
        button.setFont(new Font("Tahoma", Font.PLAIN, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(6, 6, 6, 6));
        button.setIconTextGap(6);

        ImageIcon icon = loadIcon(iconPath, 16);
        if (icon != null) {
            button.setIcon(icon);
            button.setHorizontalTextPosition(SwingConstants.LEFT);
            button.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        }
    }

    //=========== CRUD ===========
    private void onAddLine() {
        String error = validateLineForm();

        if (error != null) {
            warn(error);
            return;
        }

        String line = txtLineName.getText().trim();
        double dist = AppUtils.getSpinnerDouble(spDistance, 0.0);
        double fuel = AppUtils.getSpinnerDouble(spFuel, 0.0);
        int approxMin = AppUtils.getSpinnerInt(spApproxMinutes, 0);

        try {
            DB.call("{ call PRC_ITINERARY_INS(?, ?, ?, ?, ?) }",
                    line, dist, fuel, 0, approxMin);

            refreshAll();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "فشل إضافة الخط:\n" + ex.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onUpdateLine() {
        int row = linesTable.getSelectedRow();
        if (row == -1) {
            info("من فضلك اختر خط للتعديل.");
            return;
        }

        String error = validateLineForm();

        if (error != null) {
            warn(error);
            return;
        }

        String line = txtLineName.getText().trim();
        double dist = AppUtils.getSpinnerDouble(spDistance, 0.0);
        double fuel = AppUtils.getSpinnerDouble(spFuel, 0.0);
        int approxMin = AppUtils.getSpinnerInt(spApproxMinutes, 0);

        int modelRow = linesTable.convertRowIndexToModel(row);
        int linesId = Integer.parseInt(String.valueOf(tableModel.getValueAt(modelRow, 0)));

        try {
            Integer carCount = DB.queryScalar(
                    "SELECT COUNT(*) FROM Cars c WHERE c.Itinerary_ID = ?",
                    Integer.class, linesId
            );
            if (carCount == null) {
                carCount = 0;
            }

            DB.call("{ call PRC_ITINERARY_UPD(?, ?, ?, ?, ?, ?) }",
                    linesId, line, dist, fuel, carCount, approxMin);

            refreshAll();

        } catch (Exception ex) {
            dbError("تعديل الخط", ex);
        }
    }

    private void onDeleteLine() {
        int row = linesTable.getSelectedRow();
        if (row == -1) {
            info("من فضلك اختر خط لحذفة.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "هل أنت متأكد من حذف هذا الخط؟",
                "تأكيد الحذف", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        int modelRow = linesTable.convertRowIndexToModel(row);
        int linesId = Integer.parseInt(String.valueOf(tableModel.getValueAt(modelRow, 0)));

        try {
            DB.call("{ call PRC_ITINERARY_DEL(?) }", linesId);
            refreshAll();
            
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this,
                    "مش قادر أحذف الخط ده لأنه مرتبط ببيانات (سيارات/موظفين). احذف الارتباط الأول.\n\n" + ex.getMessage(),
                    "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {

        txtLineName.setText("");
        spDistance.setValue(0);
        spFuel.setValue(0);
        spApproxMinutes.setValue(0);

        if (linesTable != null) {
            linesTable.clearSelection();
        }
    }

    // ============ Refresh & Load ===========
    private void loadSelectedRowToForm() {
        int row = linesTable.getSelectedRow();
        if (row < 0) {
            return;
        }

        int modelRow = linesTable.convertRowIndexToModel(row);

        txtLineName.setText(String.valueOf(tableModel.getValueAt(modelRow, 1)));

        Object distObj = tableModel.getValueAt(modelRow, 3);
        Object fuelObj = tableModel.getValueAt(modelRow, 4);
        Object minObj = tableModel.getValueAt(modelRow, 5);

        if (distObj instanceof Number number) {
            spDistance.setValue(number.doubleValue());
        }
        if (fuelObj instanceof Number number) {
            spFuel.setValue(number.doubleValue());
        }
        if (minObj instanceof Number number) {
            spApproxMinutes.setValue(number.intValue());
        }
    }

    private void refreshLinesTable() {
        final String sql
                = "SELECT "
                + "  i.itinerary_id   AS ID, "
                + "  i.itinerary_name AS LINE_NAME, "
                + "  (SELECT COUNT(*) FROM Cars c WHERE c.Itinerary_ID = i.itinerary_id) AS CAR_CNT, "
                + "  i.distances AS DIST_KM, "
                + "  i.Fuel_consumption AS FUEL_L, "
                + "  i.Approximate_time AS APPROX_MIN "
                + "FROM Itinerary i "
                + "ORDER BY i.itinerary_id DESC";

        DefaultTableModel m = DB.queryToTableModel(sql);

        m.setColumnIdentifiers(new Object[]{
            "ID",
            "اسم الخط",
            "عدد السيارات علي الخط",
            "المسافة (كم)",
            "استهلاك البنزين (لتر)",
            "الوقت التقريبي"
        });

        tableModel = m;
        linesTable.setModel(tableModel);

        sorter = new TableRowSorter<>(tableModel);
        linesTable.setRowSorter(sorter);

        styleLinesTable(linesTable);

        DefaultTableCellRenderer minutesRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);

                if (value == null) {
                    setText("");
                } else if (value instanceof Number number) {
                    setText(number.intValue() + " دقيقة");
                } else {
                    setText(value.toString());
                }
                return this;
            }
        };
        linesTable.getColumnModel().getColumn(5).setCellRenderer(minutesRenderer);

        applyLinesFilter();
    }

    public void refreshAll() {
        SwingUtilities.invokeLater(() -> {
            try {
                refreshLinesTable();
                clearForm();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to refresh lines:\n" + ex.getMessage(),
                        "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // ============ Validations Methods ===========
    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "تحذير", JOptionPane.WARNING_MESSAGE);
    }

    private void info(String message) {
        JOptionPane.showMessageDialog(this, message, "تنبيه", JOptionPane.INFORMATION_MESSAGE);
    }

    private void dbError(String action, Exception ex) {
        JOptionPane.showMessageDialog(this,
                "فشل " + action + ":\n" + ex.getMessage(),
                "DB Error",
                JOptionPane.ERROR_MESSAGE);
    }

    private String validateLineForm() {
        String line = txtLineName.getText().trim();
        double dist = AppUtils.getSpinnerDouble(spDistance, 0.0);
        double fuel = AppUtils.getSpinnerDouble(spFuel, 0.0);
        int approxMin = AppUtils.getSpinnerInt(spApproxMinutes, 0);

        return AppUtils.firstError(
                AppUtils.requireText(line, "اسم الخط"),
                AppUtils.requirePositiveDouble(dist, "المسافة"),
                AppUtils.requirePositiveDouble(fuel, "استهلاك الوقود"),
                AppUtils.requirePositiveInt(approxMin, "الوقت المتوقع")
        );
    }
}
