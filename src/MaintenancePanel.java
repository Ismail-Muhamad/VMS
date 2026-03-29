
import DB.DB;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class MaintenancePanel extends JPanel {

    private JTable maintenanceTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    // شريط البحث + الفلاتر
    private JTextField txtSearch;
    private JComboBox<ComboItem> cbFilterCar;
    private JComboBox<ComboItem> cbFilterType;

    // حقول الفورم
    private JComboBox<ComboItem> cbCarId;
    private JComboBox<ComboItem> cbType;

    // بدل الكاليندر: تاريخ عرض فقط
    private JTextField txtMaintDate;

    private JSpinner spCost;
    private JTextArea txtNotes;

    private static final ComboItem ALL_CARS = new ComboItem(-1, "كل العربيات");
    private static final ComboItem ALL_TYPES = new ComboItem(-1, "كل الأنواع");
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");

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

    public MaintenancePanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(243, 244, 246));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JPanel main = new JPanel(new BorderLayout(16, 16));
        main.setBorder(new EmptyBorder(16, 16, 16, 16));
        main.setOpaque(false);

        JPanel tablePanel = createMaintenanceTablePanel();
        JPanel formPanel = createFormPanel();

        main.add(tablePanel, BorderLayout.CENTER);
        main.add(formPanel, BorderLayout.EAST);

        add(main, BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> {
            try {
                loadCombosFromDB();
                refreshMaintenanceTable();
                clearForm();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "مشكلة اتصال بقاعدة البيانات:\n" + ex.getMessage(),
                        "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private JPanel createMaintenanceTablePanel() {
        CardPanel panel = new CardPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setLayout(new BorderLayout(8, 8));

        JLabel title = new JLabel("سجل الصيانة");
        title.setFont(new Font("Tahoma", Font.BOLD, 18));
        title.setHorizontalAlignment(SwingConstants.RIGHT);

        txtSearch = new JTextField();
        txtSearch.setHorizontalAlignment(SwingConstants.RIGHT);
        txtSearch.setFont(new Font("Tahoma", Font.PLAIN, 13));
        TitledBorder searchBorder = BorderFactory.createTitledBorder("بحث (اسم العربية / نوع الصيانة)");
        searchBorder.setTitleJustification(TitledBorder.RIGHT);
        txtSearch.setBorder(searchBorder);

        cbFilterCar = new JComboBox<>();
        cbFilterCar.setFont(new Font("Tahoma", Font.PLAIN, 13));
        cbFilterCar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        cbFilterCar.setBorder(BorderFactory.createTitledBorder("اسم العربية"));

        cbFilterType = new JComboBox<>();
        cbFilterType.setFont(new Font("Tahoma", Font.PLAIN, 13));
        cbFilterType.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        cbFilterType.setBorder(BorderFactory.createTitledBorder("نوع الصيانة"));

        JPanel filtersRow = new JPanel(new GridLayout(1, 3, 8, 0));
        filtersRow.setOpaque(false);
        filtersRow.add(cbFilterCar);
        filtersRow.add(cbFilterType);
        filtersRow.add(txtSearch);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(filtersRow, BorderLayout.SOUTH);

        String[] columnNames = {
            "ID",
            "CAR_ID",
            "TYPE_ID",
            "اسم العربية",
            "نوع الصيانة",
            "تاريخ الصيانة",
            "التكلفة",
            "ملاحظات"
        };

        tableModel = new DefaultTableModel(new Object[][]{}, columnNames) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                if (c == 0 || c == 1 || c == 2) {
                    return Integer.class;
                }
                if (c == 6) {
                    return Double.class;
                }
                return String.class;
            }
        };

        maintenanceTable = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        maintenanceTable.setRowSorter(sorter);

        styleMaintenanceTable(maintenanceTable);

        hideColumnByModelIndex(maintenanceTable, 1); // CAR_ID
        hideColumnByModelIndex(maintenanceTable, 2); // TYPE_ID

        maintenanceTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedRowToForm();
            }
        });

        Runnable apply = this::applyMaintenanceFilter;

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                apply.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                apply.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                apply.run();
            }
        });

        cbFilterCar.addActionListener(e -> apply.run());
        cbFilterType.addActionListener(e -> apply.run());

        JScrollPane scroll = new JScrollPane(maintenanceTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void hideColumnByModelIndex(JTable table, int modelIndex) {
        int viewIndex = table.convertColumnIndexToView(modelIndex);
        if (viewIndex < 0) {
            return;
        }
        TableColumn col = table.getColumnModel().getColumn(viewIndex);
        col.setMinWidth(0);
        col.setMaxWidth(0);
        col.setPreferredWidth(0);
    }

    private void styleMaintenanceTable(JTable t) {
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

    private void applyMaintenanceFilter() {
        if (sorter == null || tableModel == null) {
            return;
        }

        String txt = (txtSearch == null) ? "" : txtSearch.getText().trim();

        ComboItem carItem = (cbFilterCar == null || cbFilterCar.getSelectedItem() == null)
                ? ALL_CARS : (ComboItem) cbFilterCar.getSelectedItem();

        ComboItem typeItem = (cbFilterType == null || cbFilterType.getSelectedItem() == null)
                ? ALL_TYPES : (ComboItem) cbFilterType.getSelectedItem();

        int colCarName = tableModel.findColumn("اسم العربية");
        int colTypeName = tableModel.findColumn("نوع الصيانة");

        int colCarId = tableModel.findColumn("CAR_ID");
        int colTypeId = tableModel.findColumn("TYPE_ID");

        List<RowFilter<Object, Object>> filters = new ArrayList<>();

        if (!txt.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(txt), colCarName, colTypeName));
        }

        if (carItem != null && carItem.id != -1) {
            final int wantedCarId = carItem.id;
            filters.add(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends Object, ? extends Object> entry) {
                    Object v = entry.getValue(colCarId);
                    if (v == null) {
                        return false;
                    }
                    try {
                        return (v instanceof Number) ? ((Number) v).intValue() == wantedCarId
                                : Integer.parseInt(String.valueOf(v).trim()) == wantedCarId;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            });
        }

        if (typeItem != null && typeItem.id != -1) {
            final int wantedTypeId = typeItem.id;
            filters.add(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends Object, ? extends Object> entry) {
                    Object v = entry.getValue(colTypeId);
                    if (v == null) {
                        return false;
                    }
                    try {
                        return (v instanceof Number) ? ((Number) v).intValue() == wantedTypeId
                                : Integer.parseInt(String.valueOf(v).trim()) == wantedTypeId;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            });
        }

        sorter.setRowFilter(filters.isEmpty()
                ? null
                : (filters.size() == 1 ? filters.get(0) : RowFilter.andFilter(filters)));
    }

    private JPanel createFormPanel() {
        CardPanel panel = new CardPanel();
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(380, 0));
        panel.setLayout(new BorderLayout(0, 12));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("بيانات الصيانة");
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

        cbCarId = new JComboBox<>();
        cbCarId.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        cbType = new JComboBox<>();
        cbType.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // بدل الكاليندر: تاريخ عرض فقط
        txtMaintDate = new JTextField();
        txtMaintDate.setEditable(false);
        txtMaintDate.setHorizontalAlignment(SwingConstants.CENTER);
        txtMaintDate.setText(SDF.format(new Date()));

        spCost = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100000000.0, 10.0));
        JComponent ed = spCost.getEditor();
        if (ed instanceof JSpinner.NumberEditor numberEditor) {
            numberEditor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
            numberEditor.getTextField().setFont(new Font("Tahoma", Font.PLAIN, 13));
        }

        txtNotes = new JTextArea(3, 20);
        txtNotes.setLineWrap(true);
        txtNotes.setWrapStyleWord(true);
        txtNotes.setFont(new Font("Tahoma", Font.PLAIN, 13));
        JScrollPane notesScroll = new JScrollPane(txtNotes);
        notesScroll.setBorder(BorderFactory.createLineBorder(new Color(209, 213, 219)));

        addLabeledField(form, gbc, "اسم العربية", cbCarId);
        addLabeledField(form, gbc, "نوع الصيانة", cbType);
        addLabeledField(form, gbc, "تاريخ الصيانة (تلقائي)", txtMaintDate);
        addLabeledField(form, gbc, "التكلفة", spCost);
        addLabeledField(form, gbc, "ملاحظات", notesScroll);

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

        styleActionButton(btnAdd, new Color(22, 163, 74), "icons/add_maint.png");
        styleActionButton(btnUpdate, new Color(37, 99, 235), "icons/edit_maint.png");
        styleActionButton(btnDelete, new Color(220, 38, 38), "icons/delete_maint.png");
        styleActionButton(btnClear, new Color(107, 114, 128), "icons/clear.png");

        btnAdd.addActionListener(e -> onAddMaintenance());
        btnUpdate.addActionListener(e -> onUpdateMaintenance());
        btnDelete.addActionListener(e -> onDeleteMaintenance());
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

    private void addLabeledField(JPanel panel, GridBagConstraints gbc, String labelText, JComponent field) {
        JLabel label = new JLabel(labelText);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setFont(new Font("Tahoma", Font.PLAIN, 13));

        gbc.gridx = 0;
        panel.add(label, gbc);
        gbc.gridy++;

        field.setFont(new Font("Tahoma", Font.PLAIN, 13));
        if (field instanceof JTextField jTextField) {
            jTextField.setHorizontalAlignment(SwingConstants.RIGHT);
        }

        gbc.gridx = 0;
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
    private void onAddMaintenance() {
        String error = validateMaintenanceForm();

        if (error != null) {
            warn(error);
            return;
        }

        Integer carItem = AppUtils.getSelectedComboId(cbCarId);
        Integer typeItem = AppUtils.getSelectedComboId(cbType);
        double cost = AppUtils.getSpinnerDouble(spCost, 0.0);
        String notes = txtNotes.getText().trim();

        try {
            DB.call("{ call PRC_MAINTENANCE_INS(?, ?, ?, ?) }",
                    cost, notes, carItem, typeItem
            );

            refreshMaintenanceTable();
            clearForm();

        } catch (Exception ex) {
            dbError("إضافة الصيانة", ex);
        }
    }

    private void onUpdateMaintenance() {
        int row = maintenanceTable.getSelectedRow();
        if (row == -1) {
            info("من فضلك اختر سجل صيانة للتعديل.");
            return;
        }

        String error = validateMaintenanceForm();

        if (error != null) {
            warn(error);
            return;
        }

        int modelRow = maintenanceTable.convertRowIndexToModel(row);
        int maintenanceId = Integer.parseInt(String.valueOf(tableModel.getValueAt(modelRow, 0)));

        Integer carItem = AppUtils.getSelectedComboId(cbCarId);
        Integer typeItem = AppUtils.getSelectedComboId(cbType);
        double cost = AppUtils.getSpinnerDouble(spCost, 0.0);
        String notes = txtNotes.getText().trim();

        try {
            DB.call("{ call PRC_MAINTENANCE_UPD(?, ?, ?, ?, ?) }",
                    maintenanceId, cost, notes, carItem, typeItem
            );

            refreshMaintenanceTable();
            clearForm();

        } catch (Exception ex) {
            dbError("تعديل الصيانة", ex);
        }
    }

    private void onDeleteMaintenance() {
        int row = maintenanceTable.getSelectedRow();
        if (row == -1) {
            info("من فضلك اختر عملية صيانة للحذف.");
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "هل أنت متأكد من حذف عملية الصيانة؟",
                "تأكيد الحذف", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        int modelRow = maintenanceTable.convertRowIndexToModel(row);
        int maintId = Integer.parseInt(String.valueOf(tableModel.getValueAt(modelRow, 0)));

        try {
            DB.call("{ call PRC_MAINTENANCE_DEL(?) }", maintId);
            refreshMaintenanceTable();
            clearForm();
        } catch (Exception ex) {
            dbError("حذف الصيانة", ex);
        }
    }

    private void clearForm() {
        if (cbCarId != null && cbCarId.getItemCount() > 0) {
            cbCarId.setSelectedIndex(-1);
        }
        if (cbType != null && cbType.getItemCount() > 0) {
            cbType.setSelectedIndex(-1);
        }

        if (txtMaintDate != null) {
            txtMaintDate.setText(SDF.format(new Date()));
            txtMaintDate.setHorizontalAlignment(SwingConstants.CENTER);
        }

        spCost.setValue(0.0);
        txtNotes.setText("");

        if (maintenanceTable != null) {
            maintenanceTable.clearSelection();
        }
    }

    // ============ Refresh & Load ===========
    private void loadSelectedRowToForm() {
        int row = maintenanceTable.getSelectedRow();
        if (row == -1) {
            return;
        }

        int modelRow = maintenanceTable.convertRowIndexToModel(row);

        int carId = Integer.parseInt(String.valueOf(tableModel.getValueAt(modelRow, 1)));
        int typeId = Integer.parseInt(String.valueOf(tableModel.getValueAt(modelRow, 2)));

        String dateStr = String.valueOf(tableModel.getValueAt(modelRow, 5));
        Object costObj = tableModel.getValueAt(modelRow, 6);
        Object notesObj = tableModel.getValueAt(modelRow, 7);

        AppUtils.selectComboById(cbCarId, carId);
        AppUtils.selectComboById(cbType, typeId);

        if (txtMaintDate != null) {
            txtMaintDate.setText(dateStr == null ? "" : dateStr.trim());
            txtMaintDate.setHorizontalAlignment(SwingConstants.CENTER);
        }

        if (costObj instanceof Number number) {
            spCost.setValue(number.doubleValue());
        } else {
            try {
                spCost.setValue(Double.valueOf(String.valueOf(costObj)));
            } catch (NumberFormatException ignored) {
                spCost.setValue(0.0);
            }
        }

        txtNotes.setText(notesObj == null ? "" : notesObj.toString());
    }

    private void loadCombosFromDB() {
        cbCarId.removeAllItems();
        cbFilterCar.removeAllItems();
        cbFilterCar.addItem(ALL_CARS);

        List<ComboItem> cars = DB.query(
                "SELECT Car_id, Car_Name FROM Cars ORDER BY Car_id DESC",
                rs -> new ComboItem(rs.getInt(1), rs.getString(2))
        );
        for (ComboItem c : cars) {
            cbCarId.addItem(c);
            cbFilterCar.addItem(c);
        }

        cbType.removeAllItems();
        cbFilterType.removeAllItems();
        cbFilterType.addItem(ALL_TYPES);

        List<ComboItem> types = DB.query(
                "SELECT Maintenance_Type_ID, Maintenance_Name FROM Maintenance_Type ORDER BY Maintenance_Name",
                rs -> new ComboItem(rs.getInt(1), rs.getString(2))
        );
        for (ComboItem t : types) {
            cbType.addItem(t);
            cbFilterType.addItem(t);
        }
    }

    private void refreshMaintenanceTable() {
        String sql
                = "SELECT "
                + "  m.maintenance_id AS ID, "
                + "  m.car_id         AS CAR_ID, "
                + "  m.Maintenance_Type_ID AS TYPE_ID, "
                + "  c.Car_Name       AS CAR_NAME, "
                + "  mt.Maintenance_Name AS TYPE_NAME, "
                + "  TO_CHAR(m.maintenance_date, 'YYYY-MM-DD') AS MAINT_DATE, "
                + "  m.costs          AS COSTS, "
                + "  m.notes           AS NOTES "
                + "FROM Maintenance m "
                + "JOIN Cars c ON c.Car_id = m.car_id "
                + "JOIN Maintenance_Type mt ON mt.Maintenance_Type_ID = m.Maintenance_Type_ID "
                + "ORDER BY m.maintenance_id DESC";

        DefaultTableModel m = DB.queryToTableModel(sql);
        m.setColumnIdentifiers(new Object[]{
            "ID", "CAR_ID", "TYPE_ID",
            "اسم العربية", "نوع الصيانة", "تاريخ الصيانة", "التكلفة", "ملاحظات"
        });

        tableModel = m;
        maintenanceTable.setModel(tableModel);

        sorter = new TableRowSorter<>(tableModel);
        maintenanceTable.setRowSorter(sorter);

        styleMaintenanceTable(maintenanceTable);
        hideColumnByModelIndex(maintenanceTable, 1);
        hideColumnByModelIndex(maintenanceTable, 2);

        applyMaintenanceFilter();
    }

    public void refreshAll() {
        SwingUtilities.invokeLater(() -> {
            try {
                Integer oldFilterCarId = AppUtils.getSelectedComboId(cbFilterCar);
                Integer oldFilterTypeId = AppUtils.getSelectedComboId(cbFilterType);

                loadCombosFromDB();

                if (oldFilterCarId != null) {
                    AppUtils.selectComboById(cbFilterCar, oldFilterCarId);
                }
                if (oldFilterTypeId != null) {
                    AppUtils.selectComboById(cbFilterType, oldFilterTypeId);
                }

                refreshMaintenanceTable();
                clearForm();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to refresh maintenance records:\n" + ex.getMessage(),
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

    private String validateMaintenanceForm() {
        Integer carId = AppUtils.getSelectedComboId(cbCarId);
        double cost = AppUtils.getSpinnerDouble(spCost, 0.0);
        String notes = txtNotes.getText().trim();

        return AppUtils.firstError(
                AppUtils.requireCombo(carId, "العربية"),
                AppUtils.requirePositiveDouble(cost, "التكلفة"),
                AppUtils.requireText(notes, "وصف الصيانة")
        );
    }

}
