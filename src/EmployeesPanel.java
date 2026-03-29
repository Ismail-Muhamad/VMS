
import DB.DB;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.RowFilter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class EmployeesPanel extends JPanel {

    private JTable employeesTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    // فورم
    private JTextField txtEmpName;
    private JTextField txtPhone;
    private JComboBox<ComboItem> cbDepartment;
    private JComboBox<ComboItem> cbLine;

    // فلاتر
    private JTextField txtSearch;

    private JComboBox<ComboItem> cbFilterDept;
    private JComboBox<ComboItem> cbFilterLine;

    private static final ComboItem ALL_DEPTS = new ComboItem(-1, "كل الإدارات");
    private static final ComboItem ALL_LINES = new ComboItem(-1, "كل الخطوط");

    public EmployeesPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(243, 244, 246));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JPanel main = new JPanel(new BorderLayout(16, 16));
        main.setBorder(new EmptyBorder(16, 16, 16, 16));
        main.setOpaque(false);

        JPanel tablePanel = createEmployeesTablePanel();
        JPanel formPanel = createFormPanel();

        main.add(tablePanel, BorderLayout.CENTER);
        main.add(formPanel, BorderLayout.EAST);

        add(main, BorderLayout.CENTER);

        SwingUtilities.invokeLater(this::refreshAll);
    }

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

    private JPanel createEmployeesTablePanel() {
        CardPanel panel = new CardPanel();
        panel.setBackground(Color.WHITE);
        panel.setLayout(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("قائمة الموظفين");
        title.setFont(new Font("Tahoma", Font.BOLD, 16));
        title.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel filtersPanel = new JPanel(new GridLayout(1, 3, 8, 0));
        filtersPanel.setOpaque(false);

        txtSearch = new JTextField();
        txtSearch.setHorizontalAlignment(SwingConstants.RIGHT);
        txtSearch.setFont(new Font("Tahoma", Font.PLAIN, 13));
        TitledBorder searchBorder = BorderFactory.createTitledBorder("بحث بالاسم / الهاتف");
        searchBorder.setTitleJustification(TitledBorder.RIGHT);
        txtSearch.setBorder(searchBorder);

        cbFilterDept = new JComboBox<>();
        cbFilterDept.setFont(new Font("Tahoma", Font.PLAIN, 13));
        cbFilterDept.setBorder(BorderFactory.createTitledBorder("القسم / الإدارة"));
        cbFilterDept.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        cbFilterDept.addItem(ALL_DEPTS);

        cbFilterLine = new JComboBox<>();
        cbFilterLine.setFont(new Font("Tahoma", Font.PLAIN, 13));
        cbFilterLine.setBorder(BorderFactory.createTitledBorder("الخط"));
        cbFilterLine.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        cbFilterLine.addItem(ALL_LINES);

        filtersPanel.add(cbFilterDept);
        filtersPanel.add(cbFilterLine);
        filtersPanel.add(txtSearch);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(filtersPanel, BorderLayout.SOUTH);

        String[] columnNames = {
            "ID",
            "DEPT_ID",
            "LINE_ID",
            "اسم",
            "الهاتف",
            "القسم / الإدارة",
            "الخط"
        };

        tableModel = new DefaultTableModel(new Object[][]{}, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 1 || columnIndex == 2) {
                    return Integer.class;
                }
                return String.class;
            }
        };

        employeesTable = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        employeesTable.setRowSorter(sorter);

        employeesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedRowToForm();
            }
        });

        styleEmployeesTable(employeesTable);

        hideColumnByModelIndex(employeesTable, 1);
        hideColumnByModelIndex(employeesTable, 2);

        Runnable applyFilter = this::applyEmployeesFilter;

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter.run();
            }
        });

        cbFilterDept.addActionListener(e -> applyFilter.run());
        cbFilterLine.addActionListener(e -> applyFilter.run());

        JScrollPane sp = new JScrollPane(employeesTable);
        sp.setBorder(BorderFactory.createEmptyBorder());

        panel.add(top, BorderLayout.NORTH);
        panel.add(sp, BorderLayout.CENTER);
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

    private void applyEmployeesFilter() {
        if (sorter == null || tableModel == null) {
            return;
        }

        String text = (txtSearch == null) ? "" : txtSearch.getText().trim();

        ComboItem deptItem = (cbFilterDept == null || cbFilterDept.getSelectedItem() == null)
                ? ALL_DEPTS : (ComboItem) cbFilterDept.getSelectedItem();

        ComboItem lineItem = (cbFilterLine == null || cbFilterLine.getSelectedItem() == null)
                ? ALL_LINES : (ComboItem) cbFilterLine.getSelectedItem();

        int colName = tableModel.findColumn("اسم");
        int colPhone = tableModel.findColumn("الهاتف");

        int colDeptId = tableModel.findColumn("DEPT_ID");
        int colLineId = tableModel.findColumn("LINE_ID");

        List<RowFilter<Object, Object>> filters = new ArrayList<>();

        if (!text.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), colName, colPhone));
        }

        if (deptItem != null && deptItem.id != -1) {
            final int wantedDeptId = deptItem.id;
            filters.add(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends Object, ? extends Object> entry) {
                    Object v = entry.getValue(colDeptId);
                    if (v == null) {
                        return false;
                    }
                    try {
                        return (v instanceof Number) ? ((Number) v).intValue() == wantedDeptId
                                : Integer.parseInt(String.valueOf(v).trim()) == wantedDeptId;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            });
        }

        if (lineItem != null && lineItem.id != -1) {
            final int wantedLineId = lineItem.id;
            filters.add(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends Object, ? extends Object> entry) {
                    Object v = entry.getValue(colLineId);
                    if (v == null) {
                        return false;
                    }
                    try {
                        return (v instanceof Number) ? ((Number) v).intValue() == wantedLineId
                                : Integer.parseInt(String.valueOf(v).trim()) == wantedLineId;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            });
        }

        sorter.setRowFilter(filters.isEmpty() ? null : (filters.size() == 1 ? filters.get(0) : RowFilter.andFilter(filters)));
    }

    private void styleEmployeesTable(JTable table) {
        table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        table.setFont(new Font("Tahoma", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(220, 220, 220));
        table.setSelectionBackground(new Color(37, 99, 235));
        table.setSelectionForeground(Color.WHITE);
        table.setIntercellSpacing(new Dimension(0, 0));

        javax.swing.table.JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Tahoma", Font.BOLD, 13));
        header.setBackground(new Color(230, 235, 245));
        header.setForeground(new Color(30, 41, 59));
        ((DefaultTableCellRenderer) header.getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        table.getColumn("ID").setCellRenderer(centerRenderer);
        table.getColumn("ID").setMinWidth(50);
        table.getColumn("ID").setMaxWidth(60);

        table.getColumn("اسم").setCellRenderer(rightRenderer);
        table.getColumn("الهاتف").setCellRenderer(centerRenderer);
        table.getColumn("القسم / الإدارة").setCellRenderer(rightRenderer);
        table.getColumn("الخط").setCellRenderer(centerRenderer);
    }

    private JPanel createFormPanel() {
        CardPanel panel = new CardPanel();
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(340, 0));
        panel.setLayout(new BorderLayout(0, 12));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("بيانات الموظف");
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

        txtEmpName = new JTextField();
        txtPhone = new JTextField();

        cbDepartment = new JComboBox<>();
        cbLine = new JComboBox<>();

        cbDepartment.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        cbLine.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        addLabeledField(form, gbc, "اسم", txtEmpName);
        addLabeledField(form, gbc, "الهاتف", txtPhone);
        addLabeledField(form, gbc, "القسم / الإدارة", cbDepartment);
        addLabeledField(form, gbc, "الخط", cbLine);

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

        styleActionButton(btnAdd, new Color(22, 163, 74), "icons/add_user.png");
        styleActionButton(btnUpdate, new Color(37, 99, 235), "icons/edit_user.png");
        styleActionButton(btnDelete, new Color(220, 38, 38), "icons/delete_user.png");
        styleActionButton(btnClear, new Color(107, 114, 128), "icons/clear.png");

        btnAdd.addActionListener(e -> onAddEmployee());
        btnUpdate.addActionListener(e -> onUpdateEmployee());
        btnDelete.addActionListener(e -> onDeleteEmployee());
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

    //======== CRUD ========
    private void onAddEmployee() {
        String error = validateEmployeeForm();

        if (error != null) {
            warn(error);
            return;
        }

        String name = txtEmpName.getText().trim();
        String phone = txtPhone.getText().trim();
        Integer deptId = AppUtils.getSelectedComboId(cbDepartment);
        Integer lineId = AppUtils.getSelectedComboId(cbLine);

        try {
            DB.call("{ call PRC_EMPLOYEES_INS(?, ?, ?, ?) }",
                    name, phone, deptId, lineId
            );

            refreshEmployeesFromDB();
            clearForm();

        } catch (Exception ex) {
            dbError("إضافة الموظف", ex);
        }
    }

    private void onUpdateEmployee() {
        int row = employeesTable.getSelectedRow();
        if (row == -1) {
            info("من فضلك اختر موظف لتعديله.");
            return;
        }

        String error = validateEmployeeForm();

        if (error != null) {
            warn(error);
            return;
        }

        int modelRow = employeesTable.convertRowIndexToModel(row);
        int empId = Integer.parseInt(String.valueOf(tableModel.getValueAt(modelRow, 0)));

        String name = txtEmpName.getText().trim();
        String phone = txtPhone.getText().trim();
        Integer deptId = AppUtils.getSelectedComboId(cbDepartment);
        Integer lineId = AppUtils.getSelectedComboId(cbLine);

        try {
            DB.call("{ call PRC_EMPLOYEES_UPD(?, ?, ?, ?, ?) }",
                    empId, name, phone, deptId, lineId
            );

            refreshEmployeesFromDB();
            clearForm();

        } catch (Exception ex) {
            dbError("تعديل الموظف", ex);
        }
    }

    private void onDeleteEmployee() {
        int row = employeesTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "من فضلك اختر موظف لحذفه.", "تنبيه", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "هل انت متأكد من حذف هذا الموظف؟",
                "تأكيد الحذف",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        int modelRow = employeesTable.convertRowIndexToModel(row);
        int empId = Integer.parseInt(String.valueOf(tableModel.getValueAt(modelRow, 0)));

        try {
            DB.call("{ call PRC_EMPLOYEES_DEL(?) }", empId);

            refreshEmployeesFromDB();
            clearForm();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "فشل حذف الموظف:\n" + ex.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        txtEmpName.setText("");
        txtPhone.setText("");

        if (cbDepartment != null && cbDepartment.getItemCount() > 0) {
            cbDepartment.setSelectedIndex(-1);
        }
        if (cbLine != null && cbLine.getItemCount() > 0) {
            cbLine.setSelectedIndex(-1);
        }

        if (employeesTable != null) {
            employeesTable.clearSelection();
        }
    }

    // ============ Refresh & Load ===========
    private void loadSelectedRowToForm() {
        int row = employeesTable.getSelectedRow();
        if (row == -1 || tableModel == null) {
            return;
        }

        int modelRow = employeesTable.convertRowIndexToModel(row);

        txtEmpName.setText(String.valueOf(tableModel.getValueAt(modelRow, tableModel.findColumn("اسم"))));
        txtPhone.setText(String.valueOf(tableModel.getValueAt(modelRow, tableModel.findColumn("الهاتف"))));

        int deptId = AppUtils.parseIntSafe(tableModel.getValueAt(modelRow, tableModel.findColumn("DEPT_ID")), -1);
        int lineId = AppUtils.parseIntSafe(tableModel.getValueAt(modelRow, tableModel.findColumn("LINE_ID")), -1);

        if (deptId != -1) {
            AppUtils.selectComboById(cbDepartment, deptId);
        }
        if (lineId != -1) {
            AppUtils.selectComboById(cbLine, lineId);
        }
    }

    private void loadCombosFromDB() {

        // ====== Lines ======
        cbLine.removeAllItems();
        cbFilterLine.removeAllItems();
        cbFilterLine.addItem(ALL_LINES);

        List<ComboItem> lines = DB.query(
                "SELECT itinerary_id, itinerary_name FROM Itinerary ORDER BY itinerary_name",
                rs -> new ComboItem(rs.getInt(1), rs.getString(2))
        );

        for (ComboItem x : lines) {
            cbLine.addItem(x);
            cbFilterLine.addItem(x);
        }

        // ====== Departments ======
        cbDepartment.removeAllItems();
        cbFilterDept.removeAllItems();
        cbFilterDept.addItem(ALL_DEPTS);

        List<ComboItem> deps = DB.query(
                "SELECT dept_ID, dept_Name FROM Department_Type ORDER BY dept_Name",
                rs -> new ComboItem(rs.getInt(1), rs.getString(2))
        );

        for (ComboItem d : deps) {
            cbDepartment.addItem(d);
            cbFilterDept.addItem(d);
        }
    }

    private void refreshEmployeesFromDB() {
        String sql = """
            SELECT 
                e.Employee_ID      AS ID,
                e.dept_ID          AS DEPT_ID,
                e.itinerary_id     AS LINE_ID,
                e.Employee_Name    AS EMP_NAME,
                e.Phone            AS PHONE,
                d.dept_Name        AS DEPT_NAME,
                i.itinerary_name   AS LINE_NAME
            FROM Employees e
            JOIN Department_Type d ON d.dept_ID = e.dept_ID
            JOIN Itinerary i       ON i.itinerary_id = e.itinerary_id
            ORDER BY e.Employee_ID
        """;

        DefaultTableModel m = DB.queryToTableModel(sql);
        m.setColumnIdentifiers(new Object[]{
            "ID", "DEPT_ID", "LINE_ID", "اسم", "الهاتف", "القسم / الإدارة", "الخط"
        });

        tableModel = m;
        employeesTable.setModel(tableModel);

        sorter = new TableRowSorter<>(tableModel);
        employeesTable.setRowSorter(sorter);

        styleEmployeesTable(employeesTable);

        hideColumnByModelIndex(employeesTable, 1);
        hideColumnByModelIndex(employeesTable, 2);

        applyEmployeesFilter();
    }

    public void refreshAll() {
        SwingUtilities.invokeLater(() -> {
            try {
                Integer oldFilterDeptId = AppUtils.getSelectedComboId(cbFilterDept);
                Integer oldFilterLineId = AppUtils.getSelectedComboId(cbFilterLine);

                loadCombosFromDB();

                if (oldFilterDeptId != null) {
                    AppUtils.selectComboById(cbFilterDept, oldFilterDeptId);
                }
                if (oldFilterLineId != null) {
                    AppUtils.selectComboById(cbFilterLine, oldFilterLineId);
                }

                refreshEmployeesFromDB();
                clearForm();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to refresh employees:\n" + ex.getMessage(),
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

    private String validateEmployeeForm() {
        String name = txtEmpName.getText().trim();
        String phone = txtPhone.getText().trim();
        Integer deptId = AppUtils.getSelectedComboId(cbDepartment);
        Integer lineId = AppUtils.getSelectedComboId(cbLine);

        return AppUtils.firstError(
                AppUtils.requireText(name, "اسم الموظف"),
                AppUtils.requireText(phone, "رقم الهاتف"),
                AppUtils.requireCombo(deptId, "القسم"),
                AppUtils.requireCombo(lineId, "الخط"),
                AppUtils.requireValidEgyptMobile(phone)
        );
    }
}
