
import DB.DB;
import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

public class DriversPanel extends JPanel {

    private JTable driversTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    // Form fields
    private JTextField txtName;
    private JTextField txtPhone;
    private JTextField txtLicenseNumber;
    private JComboBox<ComboItem> cbLicenseType;
    private JDateChooser dcLicenseExpiry;

    // Search + Filters
    private JTextField txtSearch;
    private JComboBox<ComboItem> cbLicenseFilter;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");

    // Licenses Types    
    private static final ComboItem ALL_LICENSES = new ComboItem(-1, "كل الدرجات");
    private static final ComboItem LIC_1 = new ComboItem(1, "درجة أولى");
    private static final ComboItem LIC_2 = new ComboItem(2, "درجة ثانية");
    private static final ComboItem LIC_3 = new ComboItem(3, "درجة ثالثة");
    private static final ComboItem[] LICENSE_TYPES = new ComboItem[]{LIC_1, LIC_2, LIC_3};

    public DriversPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(243, 244, 246));
        applyRightToLeft(this);

        JPanel mainContent = new JPanel(new BorderLayout(16, 16));
        mainContent.setBorder(new EmptyBorder(16, 16, 16, 16));
        mainContent.setOpaque(false);

        JPanel tablePanel = createDriversTablePanel();
        JPanel formPanel = createFormPanel();

        mainContent.add(tablePanel, BorderLayout.CENTER);
        mainContent.add(formPanel, BorderLayout.EAST);

        add(mainContent, BorderLayout.CENTER);

        SwingUtilities.invokeLater(this::refreshAll);
    }

    private void applyRightToLeft(Container container) {
        container.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        for (Component c : container.getComponents()) {
            c.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            if (c instanceof Container container1) {
                applyRightToLeft(container1);
            }
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

    private JPanel createDriversTablePanel() {
        CardPanel panel = new CardPanel();
        panel.setBackground(Color.WHITE);
        panel.setLayout(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("قائمة السائقين");
        title.setFont(new Font("Tahoma", Font.BOLD, 16));
        title.setHorizontalAlignment(SwingConstants.RIGHT);

        txtSearch = new JTextField();
        txtSearch.setFont(new Font("Tahoma", Font.PLAIN, 13));
        txtSearch.setHorizontalAlignment(SwingConstants.RIGHT);
        TitledBorder searchBorder = BorderFactory.createTitledBorder("بحث بالاسم / رقم الهاتف / رقم الرخصة");
        searchBorder.setTitleJustification(TitledBorder.RIGHT);
        txtSearch.setBorder(searchBorder);

        cbLicenseFilter = new JComboBox<>();
        cbLicenseFilter.setFont(new Font("Tahoma", Font.PLAIN, 13));
        cbLicenseFilter.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        TitledBorder licBorder = BorderFactory.createTitledBorder("نوع الرخصة");
        licBorder.setTitleJustification(TitledBorder.RIGHT);
        cbLicenseFilter.setBorder(licBorder);

        cbLicenseFilter.addItem(ALL_LICENSES);
        for (ComboItem it : LICENSE_TYPES) {
            cbLicenseFilter.addItem(it);
        }

        String[] columnNames = {"ID", "اسم السائق", "رقم الهاتف", "رقم الرخصة", "نوع الرخصة", "تاريخ انتهاء"};
        tableModel = new DefaultTableModel(new Object[][]{}, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        driversTable = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        driversTable.setRowSorter(sorter);

        driversTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedRowToForm();
            }
        });

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilters();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilters();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilters();
            }
        });
        cbLicenseFilter.addActionListener(e -> applyFilters());

        styleDriversTable(driversTable);

        JScrollPane scrollPane = new JScrollPane(driversTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setOpaque(false);
        searchPanel.add(txtSearch, BorderLayout.CENTER);
        searchPanel.add(cbLicenseFilter, BorderLayout.WEST);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(searchPanel, BorderLayout.SOUTH);

        panel.add(top, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void applyFilters() {
        if (sorter == null) {
            return;
        }

        String text = (txtSearch == null) ? "" : txtSearch.getText().trim();

        ComboItem licenseItem = (cbLicenseFilter == null || cbLicenseFilter.getSelectedItem() == null)
                ? ALL_LICENSES
                : (ComboItem) cbLicenseFilter.getSelectedItem();

        java.util.List<RowFilter<DefaultTableModel, Object>> filters = new java.util.ArrayList<>();

        if (!text.isEmpty()) {
            // الاسم (1) + الهاتف (2) + رقم الرخصة (3)
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 1, 2, 3));
        }

        if (licenseItem != null && licenseItem.id != -1) {
            // نوع الرخصة (4)
            filters.add(RowFilter.regexFilter("^" + Pattern.quote(licenseItem.label) + "$", 4));
        }

        if (filters.isEmpty()) {
            sorter.setRowFilter(null);
        } else if (filters.size() == 1) {
            sorter.setRowFilter(filters.get(0));
        } else {
            sorter.setRowFilter(RowFilter.andFilter(filters));
        }
    }

    private void selectLicenseByLabel(String label) {
        if (cbLicenseType == null || label == null) {
            return;
        }
        ComboBoxModel<ComboItem> m = cbLicenseType.getModel();
        for (int i = 0; i < m.getSize(); i++) {
            ComboItem it = m.getElementAt(i);
            if (it != null && it.label.equals(label)) {
                cbLicenseType.setSelectedIndex(i);
                return;
            }
        }
        if (cbLicenseType.getItemCount() > 0) {
            cbLicenseType.setSelectedIndex(0);
        }
    }

    private void styleDriversTable(JTable table) {
        table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        table.setFont(new Font("Tahoma", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(220, 220, 220));
        table.setSelectionBackground(new Color(37, 99, 235));
        table.setSelectionForeground(Color.WHITE);
        table.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Tahoma", Font.BOLD, 13));
        header.setBackground(new Color(230, 235, 245));
        header.setForeground(new Color(30, 41, 59));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        table.getColumn("ID").setCellRenderer(center);
        table.getColumn("اسم السائق").setCellRenderer(right);
        table.getColumn("رقم الهاتف").setCellRenderer(center);
        table.getColumn("رقم الرخصة").setCellRenderer(center);
        table.getColumn("نوع الرخصة").setCellRenderer(center);
        table.getColumn("تاريخ انتهاء").setCellRenderer(center);

        table.getColumn("ID").setMinWidth(40);
        table.getColumn("ID").setMaxWidth(60);
    }

    private JPanel createFormPanel() {
        CardPanel panel = new CardPanel();
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(340, 0));
        panel.setLayout(new BorderLayout(0, 12));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("بيانات السائق");
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

        txtName = new JTextField();
        txtPhone = new JTextField();
        txtLicenseNumber = new JTextField();

        cbLicenseType = new JComboBox<>();
        cbLicenseType.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        for (ComboItem it : LICENSE_TYPES) {
            cbLicenseType.addItem(it);
        }

        dcLicenseExpiry = new JDateChooser();

        AppUtils.lockDateChooser(dcLicenseExpiry);
        dcLicenseExpiry.setDateFormatString("yyyy-MM-dd");

        addLabeledField(form, gbc, "اسم السائق", txtName);
        addLabeledField(form, gbc, "رقم الهاتف", txtPhone);
        addLabeledField(form, gbc, "رقم الرخصة", txtLicenseNumber);
        addLabeledField(form, gbc, "نوع الرخصة", cbLicenseType);
        addLabeledField(form, gbc, "تاريخ انتهاء", dcLicenseExpiry);

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

        styleActionButton(btnAdd, new Color(22, 163, 74), "icons/add.png");
        styleActionButton(btnUpdate, new Color(37, 99, 235), "icons/edit.png");
        styleActionButton(btnDelete, new Color(220, 38, 38), "icons/delete.png");
        styleActionButton(btnClear, new Color(107, 114, 128), "icons/clear.png");

        btnAdd.addActionListener(e -> onAddDriver());
        btnUpdate.addActionListener(e -> onUpdateDriver());
        btnDelete.addActionListener(e -> onDeleteDriver());
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

    //========== CRUD ==========
    private void onAddDriver() {
        String error = validateDriverForm();
        if (error != null) {
            warn(error);
            return;
        }

        String name = txtName.getText().trim();
        String phone = txtPhone.getText().trim();
        ComboItem typeItem = (cbLicenseType.getSelectedItem() == null) ? null : (ComboItem) cbLicenseType.getSelectedItem();
        String licenseType = (typeItem == null) ? "" : typeItem.label;
        String licenseNum = txtLicenseNumber.getText().trim();
        Date licenseExpiry = dcLicenseExpiry.getDate();

        try {
            DB.call("{ call PRC_DRIVERS_INS(?, ?, ?, ?, ?) }",
                    name, phone, licenseType, licenseNum, licenseExpiry
            );

            refreshDriversFromDB();
            clearForm();
        } catch (Exception ex) {
            dbError("إضافة السائق", ex);
        }
    }

    private void onUpdateDriver() {
        int row = driversTable.getSelectedRow();
        if (row == -1) {
            info("من فضلك اختر سائق لتعديله.");
            return;
        }

        String error = validateDriverForm();
        if (error != null) {
            warn(error);
            return;
        }

        int modelRow = driversTable.convertRowIndexToModel(row);
        int driverId = Integer.parseInt(String.valueOf(tableModel.getValueAt(modelRow, 0)));

        String name = txtName.getText().trim();
        String phone = txtPhone.getText().trim();
        ComboItem typeItem = (cbLicenseType.getSelectedItem() == null) ? null : (ComboItem) cbLicenseType.getSelectedItem();
        String licenseType = (typeItem == null) ? "" : typeItem.label;
        String licenseNum = txtLicenseNumber.getText().trim();
        Date licenseExpiry = dcLicenseExpiry.getDate();

        try {
            DB.call("{ call PRC_DRIVERS_UPD(?, ?, ?, ?, ?, ?) }",
                    driverId, name, phone, licenseType, licenseNum, licenseExpiry
            );

            refreshDriversFromDB();
            clearForm();
        } catch (Exception ex) {
            dbError("تعديل السائق", ex);
        }
    }

    private void onDeleteDriver() {
        int row = driversTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "من فضلك اختر سائق لحذفه.", "تنبيه", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "هل انت متأكد من حذف هذا السائق؟",
                "تأكيد الحذف",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        int modelRow = driversTable.convertRowIndexToModel(row);
        int driverId = Integer.parseInt(String.valueOf(tableModel.getValueAt(modelRow, 0)));

        try {
            DB.call("{ call PRC_DRIVERS_DEL(?) }", driverId);

            refreshDriversFromDB();
            clearForm();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "فشل حذف السائق:\n" + ex.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        txtName.setText("");
        txtPhone.setText("");
        txtLicenseNumber.setText("");
        dcLicenseExpiry.setDate(null);
        cbLicenseType.setSelectedIndex(-1);

        if (driversTable != null) {
            driversTable.clearSelection();
        }
    }

    // ============ Refresh & Load ===========
    private void loadSelectedRowToForm() {
        int row = driversTable.getSelectedRow();
        if (row == -1) {
            return;
        }

        int modelRow = driversTable.convertRowIndexToModel(row);

        txtName.setText(String.valueOf(tableModel.getValueAt(modelRow, 1)));
        txtPhone.setText(String.valueOf(tableModel.getValueAt(modelRow, 2)));
        txtLicenseNumber.setText(String.valueOf(tableModel.getValueAt(modelRow, 3)));

        Object typeObj = tableModel.getValueAt(modelRow, 4);
        selectLicenseByLabel(typeObj == null ? "درجة أولى" : typeObj.toString());

        String expiryStr = String.valueOf(tableModel.getValueAt(modelRow, 5));
        try {
            if (expiryStr != null && !expiryStr.trim().isEmpty()) {
                dcLicenseExpiry.setDate(SDF.parse(expiryStr.trim()));
            } else {
                dcLicenseExpiry.setDate(null);
            }
        } catch (ParseException ignored) {
            dcLicenseExpiry.setDate(null);
        }
    }

    private void refreshDriversFromDB() {
        String sql
                = "SELECT "
                + "  d.driver_id AS ID, "
                + "  d.driver_name AS DRIVER_NAME, "
                + "  d.phone AS PHONE, "
                + "  d.license_number AS LICENSE_NUMBER, "
                + "  d.license_type AS LICENSE_TYPE, "
                + "  TO_CHAR(d.license_expiry_dt, 'YYYY-MM-DD') AS LICENSE_EXPIRY "
                + "FROM Drivers d "
                + "ORDER BY d.driver_id DESC";

        DefaultTableModel m = DB.queryToTableModel(sql);
        m.setColumnIdentifiers(new Object[]{
            "ID", "اسم السائق", "رقم الهاتف", "رقم الرخصة", "نوع الرخصة", "تاريخ انتهاء"
        });

        tableModel = m;
        driversTable.setModel(tableModel);

        sorter = new TableRowSorter<>(tableModel);
        driversTable.setRowSorter(sorter);

        styleDriversTable(driversTable);
        applyFilters();
    }

    public void refreshAll() {
        SwingUtilities.invokeLater(() -> {
            try {
                refreshDriversFromDB();
                clearForm();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to refresh drivers:\n" + ex.getMessage(),
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

    private String validateDriverForm() {
        String name = txtName.getText().trim();
        String phone = txtPhone.getText().trim();
        String licenseNum = txtLicenseNumber.getText().trim();
        ComboItem typeItem = (cbLicenseType.getSelectedItem() == null) ? null : (ComboItem) cbLicenseType.getSelectedItem();
        String licenseType = (typeItem == null) ? "" : typeItem.label;
        Date licenseExpiry = dcLicenseExpiry.getDate();

        return AppUtils.firstError(
                AppUtils.requireText(name, "اسم السائق"),
                AppUtils.requireText(phone, "رقم الهاتف"),
                AppUtils.requireText(licenseNum, "رقم الرخصة"),
                AppUtils.requireText(licenseType, "نوع الرخصة"),
                AppUtils.requireDate(licenseExpiry, "تاريخ انتهاء الرخصة"),
                AppUtils.requireValidEgyptMobile(phone),
                AppUtils.requireFutureOrToday(licenseExpiry, "تاريخ انتهاء الرخصة")
        );
    }

}
