
import DB.DB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import com.toedter.calendar.JDateChooser;
import java.text.ParseException;

public class VehiclesPanel extends JPanel {

    private JTable vehiclesTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    // حقول الفورم
    private JTextField txtCarName;
    private JComboBox<ComboItem> cbType;
    private JComboBox<ComboItem> cbLineName; // itinerary
    private JTextField txtPlate;
    private JTextField txtLicenseNum;

    private JComboBox<ComboItem> cbDriverName;
    private JSpinner spPassengersCap;
    private JSpinner spFuelCap;
    private JDateChooser dcLicenseEnd;

    // بحث
    private JTextField txtSearch;

    private JComboBox<ComboItem> cbFilterLine;   // فلتر اسم الخط
    private JComboBox<ComboItem> cbFilterType;   // فلتر نوع العربية

    private static final ComboItem ALL_TYPES = new ComboItem(-1, "كل الأنواع");
    private static final ComboItem ALL_LINES = new ComboItem(-1, "كل الخطوط");

    private void applyVehiclesFilter() {
        if (sorter == null || tableModel == null) {
            return;
        }

        String text = (txtSearch == null) ? "" : txtSearch.getText().trim();

        ComboItem lineItem = (cbFilterLine == null || cbFilterLine.getSelectedItem() == null)
                ? ALL_LINES : (ComboItem) cbFilterLine.getSelectedItem();

        ComboItem typeItem = (cbFilterType == null || cbFilterType.getSelectedItem() == null)
                ? ALL_TYPES : (ComboItem) cbFilterType.getSelectedItem();

        int colCarName = tableModel.findColumn("اسم العربية");
        int colDriverName = tableModel.findColumn("اسم السائق");
        int colPlate = tableModel.findColumn("رقم اللوحة");
        int colLicense = tableModel.findColumn("رقم الرخصة");

        int colTypeId = tableModel.findColumn("TYPE_ID");
        int colLineId = tableModel.findColumn("LINE_ID");

        java.util.List<RowFilter<Object, Object>> filters = new java.util.ArrayList<>();

        if (!text.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), colCarName, colDriverName, colPlate, colLicense));
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

    public VehiclesPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(243, 244, 246));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JPanel mainContent = new JPanel(new BorderLayout(16, 16));
        mainContent.setBorder(new EmptyBorder(16, 16, 16, 16));
        mainContent.setOpaque(false);

        JPanel tablePanel = createVehiclesTablePanel();
        JPanel formPanel = createFormPanel();

        mainContent.add(tablePanel, BorderLayout.CENTER);
        mainContent.add(formPanel, BorderLayout.EAST);

        add(mainContent, BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> {
            try {
                loadCombosFromDB();
                refresVehiclesTable();
                clearForm();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "مشكلة اتصال بقاعدة البيانات:\n" + ex.getMessage(),
                        "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        });
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

    private JPanel createVehiclesTablePanel() {
        CardPanel panel = new CardPanel();
        panel.setBackground(Color.WHITE);
        panel.setLayout(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("قائمة السيارات");
        title.setFont(new Font("Tahoma", Font.BOLD, 16));
        title.setHorizontalAlignment(SwingConstants.RIGHT);

        txtSearch = new JTextField();
        txtSearch.setHorizontalAlignment(SwingConstants.RIGHT);
        txtSearch.setFont(new Font("Tahoma", Font.PLAIN, 13));
        TitledBorder searchBorder = BorderFactory.createTitledBorder("بحث باسم العربية / اسم السائق / اللوحة / الرخصة");
        searchBorder.setTitleJustification(TitledBorder.RIGHT);
        txtSearch.setBorder(searchBorder);

        cbFilterLine = new JComboBox<>();
        cbFilterLine.setFont(new Font("Tahoma", Font.PLAIN, 13));
        cbFilterLine.setBorder(BorderFactory.createTitledBorder("اسم الخط"));
        cbFilterLine.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        cbFilterLine.addItem(ALL_LINES);

        cbFilterType = new JComboBox<>();
        cbFilterType.setFont(new Font("Tahoma", Font.PLAIN, 13));
        cbFilterType.setBorder(BorderFactory.createTitledBorder("نوع العربية"));
        cbFilterType.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        cbFilterType.addItem(ALL_TYPES);

        JPanel filtersPanel = new JPanel(new GridLayout(1, 3, 8, 0));
        filtersPanel.setOpaque(false);
        filtersPanel.add(cbFilterLine);
        filtersPanel.add(cbFilterType);
        filtersPanel.add(txtSearch);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(filtersPanel, BorderLayout.SOUTH);

        String[] columnNames = {
            "ID",
            "DRIVER_ID",
            "TYPE_ID",
            "LINE_ID",
            "اسم العربية",
            "اسم السائق",
            "النوع",
            "اسم الخط",
            "سعة الركاب",
            "سعة البنزين",
            "السعة الفعلية للبنزين",
            "رقم اللوحة",
            "رقم الرخصة",
            "انتهاء الرخصة"
        };

        tableModel = new DefaultTableModel(new Object[][]{}, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 1 || columnIndex == 2 || columnIndex == 3) {
                    return Integer.class;
                }
                if (columnIndex == 8 || columnIndex == 9 || columnIndex == 10) {
                    return Integer.class;
                }
                return String.class;
            }
        };

        vehiclesTable = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        vehiclesTable.setRowSorter(sorter);

        vehiclesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedRowToForm();
            }
        });

        styleVehiclesTable(vehiclesTable);

        // اخفاء الأعمدة المخفية (IDs)
        hideColumnByModelIndex(vehiclesTable, 1);
        hideColumnByModelIndex(vehiclesTable, 2);
        hideColumnByModelIndex(vehiclesTable, 3);

        Runnable applyFilter = this::applyVehiclesFilter;

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

        cbFilterLine.addActionListener(e -> applyFilter.run());
        cbFilterType.addActionListener(e -> applyFilter.run());

        JScrollPane scrollPane = new JScrollPane(vehiclesTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(top, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void styleVehiclesTable(JTable table) {
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
        table.getColumn("ID").setMinWidth(50);
        table.getColumn("ID").setMaxWidth(60);

        table.getColumn("اسم العربية").setCellRenderer(right);
        table.getColumn("اسم السائق").setCellRenderer(right);
        table.getColumn("النوع").setCellRenderer(center);
        table.getColumn("اسم الخط").setCellRenderer(right);

        table.getColumn("سعة الركاب").setCellRenderer(center);
        table.getColumn("سعة البنزين").setCellRenderer(center);
        table.getColumn("السعة الفعلية للبنزين").setCellRenderer(center);
        table.getColumn("رقم اللوحة").setCellRenderer(center);
        table.getColumn("رقم الرخصة").setCellRenderer(center);
        table.getColumn("انتهاء الرخصة").setCellRenderer(center);
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

    private JPanel createFormPanel() {
        CardPanel panel = new CardPanel();
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(360, 0));
        panel.setLayout(new BorderLayout(0, 12));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("بيانات السيارة");
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

        txtCarName = new JTextField();
        cbDriverName = new JComboBox<>();
        cbType = new JComboBox<>();
        cbLineName = new JComboBox<>();

        cbDriverName.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        cbType.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        cbLineName.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        spPassengersCap = new JSpinner(new SpinnerNumberModel(0, 0, 60, 1));
        spFuelCap = new JSpinner(new SpinnerNumberModel(0, 0, 300, 5));

        txtPlate = new JTextField();
        txtLicenseNum = new JTextField();

        dcLicenseEnd = new JDateChooser();

        AppUtils.lockDateChooser(dcLicenseEnd);
        dcLicenseEnd.setDateFormatString("yyyy-MM-dd");

        addLabeledField(form, gbc, "اسم العربية", txtCarName);
        addLabeledField(form, gbc, "اسم السائق", cbDriverName);
        addLabeledField(form, gbc, "النوع", cbType);
        addLabeledField(form, gbc, "اسم الخط", cbLineName);
        addLabeledField(form, gbc, "سعة الركاب", spPassengersCap);
        addLabeledField(form, gbc, "سعة البنزين", spFuelCap);
        addLabeledField(form, gbc, "رقم اللوحة", txtPlate);
        addLabeledField(form, gbc, "رقم الرخصة", txtLicenseNum);
        addLabeledField(form, gbc, "انتهاء الرخصة", dcLicenseEnd);

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

        styleActionButton(btnAdd, new Color(22, 163, 74), "icons/add_car.png");
        styleActionButton(btnUpdate, new Color(37, 99, 235), "icons/edit_car.png");
        styleActionButton(btnDelete, new Color(220, 38, 38), "icons/delete_car.png");
        styleActionButton(btnClear, new Color(107, 114, 128), "icons/clear.png");

        buttonsPanel.add(btnAdd);
        buttonsPanel.add(btnUpdate);
        buttonsPanel.add(btnDelete);
        buttonsPanel.add(btnClear);

        btnAdd.addActionListener(e -> onAddVehicle());
        btnUpdate.addActionListener(e -> onUpdateVehicle());
        btnDelete.addActionListener(e -> onDeleteVehicle());
        btnClear.addActionListener(e -> clearForm());

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

    // ============ CRUD ===========
    private void onAddVehicle() {
        String error = validateVehicleForm();
        if (error != null) {
            warn(error);
            return;
        }

        String carName = txtCarName.getText().trim();
        String plateNumber = txtPlate.getText().trim().toUpperCase();
        String licenseNum = txtLicenseNum.getText().trim();
        Date endDate = dcLicenseEnd.getDate();
        Integer driverId = AppUtils.getSelectedComboId(cbDriverName);
        Integer carTypeId = AppUtils.getSelectedComboId(cbType);
        Integer itineraryId = AppUtils.getSelectedComboId(cbLineName);
        int passengers = AppUtils.getSpinnerInt(spPassengersCap, 0);
        int fuelCap = AppUtils.getSpinnerInt(spFuelCap, 0);

        try {
            DB.call("{ call PRC_CARS_INS(?, ?, ?, ?, ?, ?, ?, ?, ?) }",
                    carName, passengers, fuelCap, plateNumber, licenseNum, endDate, carTypeId, itineraryId, driverId
            );

            refresVehiclesTable();
            clearForm();
        } catch (Exception ex) {
            dbError("إضافة السيارة", ex);
        }
    }

    private void onUpdateVehicle() {
        int row = vehiclesTable.getSelectedRow();
        if (row == -1) {
            info("من فضلك اختر سيارة لتعديلها.");
            return;
        }

        String error = validateVehicleForm();
        if (error != null) {
            warn(error);
            return;
        }

        int modelRow = vehiclesTable.convertRowIndexToModel(row);
        int carId = Integer.parseInt(String.valueOf(tableModel.getValueAt(modelRow, 0)));

        String carName = txtCarName.getText().trim();
        String plateNumber = txtPlate.getText().trim().toUpperCase();
        String licenseNum = txtLicenseNum.getText().trim();
        Date endDate = dcLicenseEnd.getDate();
        Integer driverId = AppUtils.getSelectedComboId(cbDriverName);
        Integer carTypeId = AppUtils.getSelectedComboId(cbType);
        Integer itineraryId = AppUtils.getSelectedComboId(cbLineName);
        int passengers = AppUtils.getSpinnerInt(spPassengersCap, 0);
        int fuelCap = AppUtils.getSpinnerInt(spFuelCap, 0);

        try {
            DB.call("{ call PRC_CARS_UPD(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }",
                    carId, carName, passengers, fuelCap, plateNumber, licenseNum, endDate, carTypeId, itineraryId, driverId
            );

            refresVehiclesTable();
            clearForm();
        } catch (Exception ex) {
            dbError("تعديل السيارة", ex);
        }
    }

    private void onDeleteVehicle() {
        int row = vehiclesTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "من فضلك اختر سيارة لحذفها.", "تنبيه", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "هل انت متأكد من حذف هذه السيارة؟",
                "تأكيد الحذف", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        int modelRow = vehiclesTable.convertRowIndexToModel(row);
        int carId = Integer.parseInt(String.valueOf(tableModel.getValueAt(modelRow, 0)));

        try {
            DB.call("{ call PRC_CARS_DEL(?) }", carId);

            refresVehiclesTable();
            clearForm();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "فشل حذف السيارة:\n" + ex.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        txtCarName.setText("");
        txtPlate.setText("");
        txtLicenseNum.setText("");

        if (cbDriverName != null && cbDriverName.getItemCount() > 0) {
            cbDriverName.setSelectedIndex(-1);
        }
        if (cbType != null && cbType.getItemCount() > 0) {
            cbType.setSelectedIndex(-1);
        }
        if (cbLineName != null && cbLineName.getItemCount() > 0) {
            cbLineName.setSelectedIndex(-1);
        }

        spPassengersCap.setValue(0);
        spFuelCap.setValue(0);

        dcLicenseEnd.setDate(null);

        if (vehiclesTable != null) {
            vehiclesTable.clearSelection();
        }
    }

    // ============ Refresh & Load ===========
    private void loadSelectedRowToForm() {
        int row = vehiclesTable.getSelectedRow();
        if (row == -1 || tableModel == null) {
            return;
        }

        int modelRow = vehiclesTable.convertRowIndexToModel(row);

        int colDriverId = tableModel.findColumn("DRIVER_ID");
        int colTypeId = tableModel.findColumn("TYPE_ID");
        int colLineId = tableModel.findColumn("LINE_ID");

        int driverId = AppUtils.parseIntSafe(tableModel.getValueAt(modelRow, colDriverId), -1);
        int typeId = AppUtils.parseIntSafe(tableModel.getValueAt(modelRow, colTypeId), -1);
        int lineId = AppUtils.parseIntSafe(tableModel.getValueAt(modelRow, colLineId), -1);

        txtCarName.setText(String.valueOf(tableModel.getValueAt(modelRow, tableModel.findColumn("اسم العربية"))));

        if (driverId != -1) {
            AppUtils.selectComboById(cbDriverName, driverId);
        }
        if (typeId != -1) {
            AppUtils.selectComboById(cbType, typeId);
        }
        if (lineId != -1) {
            AppUtils.selectComboById(cbLineName, lineId);
        }

        spPassengersCap.setValue(AppUtils.parseIntSafe(tableModel.getValueAt(modelRow, tableModel.findColumn("سعة الركاب")), 14));
        spFuelCap.setValue(AppUtils.parseIntSafe(tableModel.getValueAt(modelRow, tableModel.findColumn("سعة البنزين")), 80));

        txtPlate.setText(String.valueOf(tableModel.getValueAt(modelRow, tableModel.findColumn("رقم اللوحة"))));
        txtLicenseNum.setText(String.valueOf(tableModel.getValueAt(modelRow, tableModel.findColumn("رقم الرخصة"))));

        String licEnd = String.valueOf(tableModel.getValueAt(modelRow, tableModel.findColumn("انتهاء الرخصة")));
        try {
            if (licEnd != null && !licEnd.trim().isEmpty() && !"null".equalsIgnoreCase(licEnd.trim())) {
                dcLicenseEnd.setDate(new SimpleDateFormat("yyyy-MM-dd").parse(licEnd));
            } else {
                dcLicenseEnd.setDate(null);
            }
        } catch (ParseException ex) {
            dcLicenseEnd.setDate(null);
        }
    }

    private void loadCombosFromDB() {
        cbDriverName.removeAllItems();
        cbType.removeAllItems();
        cbLineName.removeAllItems();

        cbFilterType.removeAllItems();
        cbFilterType.addItem(ALL_TYPES);

        cbFilterLine.removeAllItems();
        cbFilterLine.addItem(ALL_LINES);

        // Drivers
        List<ComboItem> drivers = DB.query(
                "SELECT driver_id, driver_name FROM Drivers ORDER BY driver_name",
                rs -> new ComboItem(rs.getInt(1), rs.getString(2))
        );
        for (ComboItem d : drivers) {
            cbDriverName.addItem(d);
        }

        // Car types
        List<ComboItem> types = DB.query(
                "SELECT Car_Type_ID, Car_Type_Name FROM Car_Type ORDER BY Car_Type_Name",
                rs -> new ComboItem(rs.getInt(1), rs.getString(2))
        );
        for (ComboItem t : types) {
            cbType.addItem(t);
            cbFilterType.addItem(t);
        }

        // Itinerary / Lines
        List<ComboItem> lines = DB.query(
                "SELECT itinerary_id, itinerary_name FROM Itinerary ORDER BY itinerary_name",
                rs -> new ComboItem(rs.getInt(1), rs.getString(2))
        );
        for (ComboItem l : lines) {
            cbLineName.addItem(l);
            cbFilterLine.addItem(l);
        }
    }

    private void refresVehiclesTable() {
        String sql = """
            SELECT
                c.Car_id                                   AS ID,
                c.driver_id                                AS DRIVER_ID,
                c.Car_Type_ID                              AS TYPE_ID,
                c.Itinerary_ID                             AS LINE_ID,
                c.Car_Name                                 AS CAR_NAME,
                d.driver_name                              AS DRIVER_NAME,
                ct.Car_Type_Name                           AS TYPE_NAME,
                i.itinerary_name                           AS LINE_NAME,
                c.Car_capacity                              AS PASSENGERS_CAP,
                c.Tank_Capacity                             AS FUEL_CAP,
                (SELECT MAX(fr.number_litres)
                 KEEP (DENSE_RANK LAST ORDER BY fr.the_date)
                 FROM Fuel_Record fr
                 WHERE fr.car_id = c.car_id)               AS FUEL_ACTUAL,
                TO_CHAR(c.Plate_Number)                     AS PLATE_NO,
                TO_CHAR(c.License_Number)                  AS LICENSE_NO,
                TO_CHAR(c.License_Expiry, 'YYYY-MM-DD')    AS LICENSE_EXP
            FROM Cars c
            JOIN Drivers d    ON d.driver_id = c.driver_id
            JOIN Car_Type ct  ON ct.Car_Type_ID = c.Car_Type_ID
            JOIN Itinerary i  ON i.itinerary_id = c.Itinerary_ID
            ORDER BY c.Car_id DESC
        """;

        DefaultTableModel newModel = DB.queryToTableModel(sql);
        newModel.setColumnIdentifiers(new Object[]{
            "ID",
            "DRIVER_ID",
            "TYPE_ID",
            "LINE_ID",
            "اسم العربية",
            "اسم السائق",
            "النوع",
            "اسم الخط",
            "سعة الركاب",
            "سعة البنزين",
            "السعة الفعلية للبنزين",
            "رقم اللوحة",
            "رقم الرخصة",
            "انتهاء الرخصة"
        });

        tableModel = newModel;
        vehiclesTable.setModel(tableModel);

        sorter = new TableRowSorter<>(tableModel);
        vehiclesTable.setRowSorter(sorter);

        styleVehiclesTable(vehiclesTable);

        hideColumnByModelIndex(vehiclesTable, 1);
        hideColumnByModelIndex(vehiclesTable, 2);
        hideColumnByModelIndex(vehiclesTable, 3);

        applyVehiclesFilter();
    }

    public void refreshAll() {
        SwingUtilities.invokeLater(() -> {
            try {
                Integer oldFilterLineId = AppUtils.getSelectedComboId(cbFilterLine);
                Integer oldFilterTypeId = AppUtils.getSelectedComboId(cbFilterType);

                loadCombosFromDB();

                if (oldFilterLineId != null) {
                    AppUtils.selectComboById(cbFilterLine, oldFilterLineId);
                }
                if (oldFilterTypeId != null) {
                    AppUtils.selectComboById(cbFilterType, oldFilterTypeId);
                }

                refresVehiclesTable();
                clearForm();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to refresh vehicles:\n" + ex.getMessage(),
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

    private String validateVehicleForm() {
        String carName = txtCarName.getText().trim();
        String plateNumber = txtPlate.getText().trim().toUpperCase();
        String licenseNum = txtLicenseNum.getText().trim();
        Date endDate = dcLicenseEnd.getDate();
        Integer driverId = AppUtils.getSelectedComboId(cbDriverName);
        Integer carTypeId = AppUtils.getSelectedComboId(cbType);
        Integer itineraryId = AppUtils.getSelectedComboId(cbLineName);
        int passengers = AppUtils.getSpinnerInt(spPassengersCap, 0);
        int fuelCap = AppUtils.getSpinnerInt(spFuelCap, 0);

        return AppUtils.firstError(
                AppUtils.requireText(carName, "اسم العربية"),
                AppUtils.requireText(plateNumber, "رقم اللوحة"),
                AppUtils.requireText(licenseNum, "رقم الرخصة"),
                AppUtils.requireDate(endDate, "تاريخ انتهاء رخصة العربية"),
                AppUtils.requireCombo(driverId, "السائق"),
                AppUtils.requireCombo(carTypeId, "نوع العربية"),
                AppUtils.requireCombo(itineraryId, "الخط"),
                AppUtils.requireFutureOrToday(endDate, "تاريخ انتهاء رخصة العربية"),
                AppUtils.requireValidPlateNumber(plateNumber),
                AppUtils.requirePositiveInt(passengers, "سعة الركاب"),
                AppUtils.requirePositiveInt(fuelCap, "سعة البنزين")
        );
    }
}
