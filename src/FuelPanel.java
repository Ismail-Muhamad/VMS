
import DB.DB;
// import com.toedter.calendar.JDateChooser;  // اتشال

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class FuelPanel extends JPanel {

    private JTable fuelTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    private JTextField txtSearch;

    // حقول الفورم
    private JComboBox<ComboItem> cbCarName;
    private JTextField txtDate;
    private JSpinner spLiters;
    private JSpinner spCost;
    private JComboBox<ComboItem> cbFuelType;
    private JTextField txtStation;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");

    private JComboBox<ComboItem> cbFilterCar;
    private static final ComboItem ALL_CARS = new ComboItem(-1, "كل العربيات");

    // Fuel types
    private static final ComboItem FUEL_80 = new ComboItem(80, "80");
    private static final ComboItem FUEL_92 = new ComboItem(92, "92");
    private static final ComboItem FUEL_95 = new ComboItem(95, "95");

    private static final ComboItem[] FUEL_TYPES = {FUEL_80, FUEL_92, FUEL_95};

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

    public FuelPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(243, 244, 246));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JPanel main = new JPanel(new BorderLayout(16, 16));
        main.setBorder(new EmptyBorder(16, 16, 16, 16));
        main.setOpaque(false);

        JPanel tablePanel = createFuelTablePanel();
        JPanel formPanel = createFormPanel();

        main.add(tablePanel, BorderLayout.CENTER);
        main.add(formPanel, BorderLayout.EAST);

        add(main, BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> {
            try {
                loadCarsFromDB();
                refreshFuelTableAsync();
                clearForm();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "مشكلة اتصال بقاعدة البيانات:\n" + ex.getMessage(),
                        "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private JPanel createFuelTablePanel() {
        CardPanel panel = new CardPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setLayout(new BorderLayout(8, 8));

        JLabel title = new JLabel("سجل تموين البنزين");
        title.setFont(new Font("Tahoma", Font.BOLD, 18));
        title.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setOpaque(false);

        txtSearch = new JTextField();
        txtSearch.setHorizontalAlignment(SwingConstants.RIGHT);
        txtSearch.setFont(new Font("Tahoma", Font.PLAIN, 13));
        TitledBorder searchBorder = BorderFactory.createTitledBorder("بحث باسم العربية / محطة البنزين");
        searchBorder.setTitleJustification(TitledBorder.RIGHT);
        txtSearch.setBorder(searchBorder);

        cbFilterCar = new JComboBox<>();
        cbFilterCar.setFont(new Font("Tahoma", Font.PLAIN, 13));
        cbFilterCar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        cbFilterCar.setBorder(BorderFactory.createTitledBorder("فلتر اسم العربية"));
        cbFilterCar.addActionListener(e -> applyFuelFilter());

        searchPanel.add(cbFilterCar, BorderLayout.WEST);
        searchPanel.add(txtSearch, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(searchPanel, BorderLayout.SOUTH);

        String[] columnNames = {
            "ID", "CAR_ID", "اسم العربية", "تاريخ اليوم", "عدد اللترات", "نوع البنزين", "التكلفة", "اسم محطة البنزين"
        };

        tableModel = new DefaultTableModel(new Object[][]{}, columnNames) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                if (c == 0 || c == 1 || c == 5) {
                    return Integer.class;
                }
                if (c == 4 || c == 6) {
                    return Double.class;
                }
                return String.class;
            }
        };

        fuelTable = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        fuelTable.setRowSorter(sorter);

        styleFuelTable(fuelTable);
        hideColumnByModelIndex(fuelTable, 1);

        fuelTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedRowToForm();
            }
        });

        Runnable applyFilter = this::applyFuelFilter;
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

        JScrollPane scroll = new JScrollPane(fuelTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void styleFuelTable(JTable t) {
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
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(center);
        }
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

    private void applyFuelFilter() {
        if (sorter == null) {
            return;
        }

        String txt = (txtSearch == null) ? "" : txtSearch.getText().trim();
        ComboItem carItem = (cbFilterCar == null || cbFilterCar.getSelectedItem() == null)
                ? ALL_CARS
                : (ComboItem) cbFilterCar.getSelectedItem();

        java.util.List<RowFilter<DefaultTableModel, Object>> filters = new java.util.ArrayList<>();

        if (!txt.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(txt), 2, 7));
        }

        if (carItem != null && carItem.id != -1) {
            final int wantedId = carItem.id;
            filters.add(new RowFilter<DefaultTableModel, Object>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Object> entry) {
                    Object v = entry.getValue(1);
                    if (v == null) {
                        return false;
                    }
                    try {
                        if (v instanceof Number number) {
                            return number.intValue() == wantedId;
                        }
                        return Integer.parseInt(String.valueOf(v).trim()) == wantedId;
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                }
            });
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

        JLabel title = new JLabel("بيانات عملية البنزين");
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

        cbCarName = new JComboBox<>();
        cbCarName.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        txtDate = new JTextField();
        txtDate.setEditable(false);
        txtDate.setHorizontalAlignment(SwingConstants.CENTER);
        txtDate.setText(SDF.format(new Date()));

        spLiters = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1000000.0, 1.0));
        spCost = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100000000.0, 10.0));
        fixSpinnerEditor(spLiters);
        fixSpinnerEditor(spCost);

        cbFuelType = new JComboBox<>(FUEL_TYPES);
        cbFuelType.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        cbFuelType.setSelectedIndex(-1);

        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.RIGHT);
        cbFuelType.setRenderer(renderer);

        txtStation = new JTextField();

        addLabeledField(form, gbc, "اسم العربية", cbCarName);

        addLabeledField(form, gbc, "تاريخ اليوم (تلقائي)", txtDate);
        txtDate.setHorizontalAlignment(SwingConstants.CENTER);

        addLabeledField(form, gbc, "عدد اللترات", spLiters);
        addLabeledField(form, gbc, "نوع البنزين", cbFuelType);
        addLabeledField(form, gbc, "التكلفة", spCost);
        addLabeledField(form, gbc, "اسم محطة البنزين", txtStation);

        gbc.gridy++;
        gbc.weighty = 1;
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        form.add(spacer, gbc);

        JPanel buttonsPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        buttonsPanel.setOpaque(false);

        JButton btnAdd = new JButton("إضافة");
        JButton btnDelete = new JButton("حذف");
        JButton btnClear = new JButton("تفريغ");

        styleActionButton(btnAdd, new Color(22, 163, 74), "icons/add_fuel.png");
        styleActionButton(btnDelete, new Color(220, 38, 38), "icons/delete_fuel.png");
        styleActionButton(btnClear, new Color(107, 114, 128), "icons/clear.png");

        btnAdd.addActionListener(e -> onAddFuelRecord());
        btnDelete.addActionListener(e -> onDeleteFuelRecord());
        btnClear.addActionListener(e -> clearForm());

        buttonsPanel.add(btnAdd);
        buttonsPanel.add(btnDelete);
        buttonsPanel.add(btnClear);

        panel.add(title, BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        panel.add(buttonsPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void fixSpinnerEditor(JSpinner sp) {
        JComponent ed = sp.getEditor();
        if (ed instanceof JSpinner.NumberEditor ne) {
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

    private void onAddFuelRecord() {
        String error = validateFuelForm();
        if (error != null) {
            warn(error);
            return;
        }

        Integer carId = AppUtils.getSelectedComboId(cbCarName);
        Integer fuelType = AppUtils.getSelectedComboId(cbFuelType);
        double liters = AppUtils.getSpinnerDouble(spLiters, 0.0);
        double cost = AppUtils.getSpinnerDouble(spCost, 0.0);
        String station = txtStation.getText().trim();

        try {
            DB.call("{ call PRC_FUEL_RECORD_INS(?, ?, ?, ?, ?) }",
                    carId, liters, fuelType, station, cost
            );

            refreshFuelTableAsync();
            clearForm();
        } catch (Exception ex) {
            dbError("إضافة عملية البنزين", ex);
        }
    }

    private void onDeleteFuelRecord() {
        int row = fuelTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "من فضلك اختر عملية للحذف.", "تنبيه", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "هل أنت متأكد من حذف هذه العملية؟",
                "تأكيد الحذف", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        int modelRow = fuelTable.convertRowIndexToModel(row);
        int id = Integer.parseInt(String.valueOf(tableModel.getValueAt(modelRow, 0)));

        try {
            DB.call("{ call PRC_FUEL_RECORD_DEL(?) }", id);

            refreshFuelTableAsync();
            clearForm();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "فشل حذف عملية البنزين:\n" + ex.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        if (cbCarName != null && cbCarName.getItemCount() > 0) {
            cbCarName.setSelectedIndex(-1);
        }

        if (cbFuelType != null && cbFuelType.getItemCount() > 0) {
            cbFuelType.setSelectedIndex(-1);
        }

        if (txtDate != null) {
            txtDate.setText(SDF.format(new Date()));
            txtDate.setHorizontalAlignment(SwingConstants.CENTER);
        }

        spLiters.setValue(0.0);
        spCost.setValue(0.0);
        txtStation.setText("");

        if (fuelTable != null) {
            fuelTable.clearSelection();
        }
    }

    // ============ Refresh & Load ===========
    private void loadSelectedRowToForm() {
        int row = fuelTable.getSelectedRow();
        if (row == -1) {
            return;
        }

        int modelRow = fuelTable.convertRowIndexToModel(row);

        int carId = Integer.parseInt(String.valueOf(tableModel.getValueAt(modelRow, 1)));
        String dateStr = String.valueOf(tableModel.getValueAt(modelRow, 3));
        Object litersObj = tableModel.getValueAt(modelRow, 4);
        Object fuelTypeObj = tableModel.getValueAt(modelRow, 5);
        Object costObj = tableModel.getValueAt(modelRow, 6);
        Object stObj = tableModel.getValueAt(modelRow, 7);

        AppUtils.selectComboById(cbCarName, carId);

        if (txtDate != null) {
            txtDate.setText(dateStr == null ? "" : dateStr.trim());
            txtDate.setHorizontalAlignment(SwingConstants.CENTER);
        }

        spLiters.setValue(litersObj instanceof Number ? ((Number) litersObj).doubleValue() : 0.0);

        if (cbFuelType != null) {
            if (fuelTypeObj instanceof Number) {
                AppUtils.selectComboById(cbFuelType, ((Number) fuelTypeObj).intValue());
            } else {
                cbFuelType.setSelectedIndex(-1);
            }
        }

        spCost.setValue(costObj instanceof Number ? ((Number) costObj).doubleValue() : 0.0);
        txtStation.setText(stObj == null ? "" : stObj.toString());
    }

    private void loadCarsFromDB() {
        cbCarName.removeAllItems();

        if (cbFilterCar != null) {
            cbFilterCar.removeAllItems();
            cbFilterCar.addItem(ALL_CARS);
        }

        List<ComboItem> cars = DB.query(
                "SELECT Car_id, Car_Name FROM Cars ORDER BY Car_id DESC",
                rs -> new ComboItem(rs.getInt(1), rs.getString(2))
        );

        for (ComboItem c : cars) {
            cbCarName.addItem(c);
            if (cbFilterCar != null) {
                cbFilterCar.addItem(c);
            }
        }
    }

    private void refreshFuelTableAsync() {
        new SwingWorker<DefaultTableModel, Void>() {
            @Override
            protected DefaultTableModel doInBackground() {
                String sql
                        = "SELECT "
                        + "  fr.fuel_record_id AS ID, "
                        + "  fr.car_id AS CAR_ID, "
                        + "  c.Car_Name AS CAR_NAME, "
                        + "  TO_CHAR(fr.the_date, 'YYYY-MM-DD') AS THE_DATE, "
                        + "  fr.number_litres AS LITERS, "
                        + "  fr.fuel_type AS FUEL_TYPE, "
                        + "  fr.cost AS COST, "
                        + "  fr.gas_station AS STATION "
                        + "FROM Fuel_Record fr "
                        + "JOIN Cars c ON c.Car_id = fr.car_id "
                        + "ORDER BY fr.fuel_record_id DESC";

                DefaultTableModel m = DB.queryToTableModel(sql);
                m.setColumnIdentifiers(new Object[]{
                    "ID", "CAR_ID", "اسم العربية", "تاريخ اليوم", "عدد اللترات", "نوع البنزين", "التكلفة", "اسم محطة البنزين"
                });
                return m;
            }

            @Override
            protected void done() {
                try {
                    tableModel = get();
                    fuelTable.setModel(tableModel);

                    sorter = new TableRowSorter<>(tableModel);
                    fuelTable.setRowSorter(sorter);

                    styleFuelTable(fuelTable);
                    hideColumnByModelIndex(fuelTable, 1);

                    applyFuelFilter();
                } catch (InterruptedException | ExecutionException ex) {
                    JOptionPane.showMessageDialog(FuelPanel.this,
                            "فشل تحميل سجل البنزين:\n" + ex.getMessage(),
                            "DB Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public void refreshAll() {
        SwingUtilities.invokeLater(() -> {
            try {
                Integer oldFilterCarId = AppUtils.getSelectedComboId(cbFilterCar);

                loadCarsFromDB();

                if (oldFilterCarId != null) {
                    AppUtils.selectComboById(cbFilterCar, oldFilterCarId);
                }

                refreshFuelTableAsync();
                clearForm();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to refresh fuel records:\n" + ex.getMessage(),
                        "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // ============ Validations Methods ===========
    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "تحذير", JOptionPane.WARNING_MESSAGE);
    }

    private void dbError(String action, Exception ex) {
        JOptionPane.showMessageDialog(this,
                "فشل " + action + ":\n" + ex.getMessage(),
                "DB Error",
                JOptionPane.ERROR_MESSAGE);
    }

    private String validateFuelForm() {
        Integer carId = AppUtils.getSelectedComboId(cbCarName);
        Integer fuelType = AppUtils.getSelectedComboId(cbFuelType);
        double liters = AppUtils.getSpinnerDouble(spLiters, 0.0);
        double cost = AppUtils.getSpinnerDouble(spCost, 0.0);
        String station = txtStation.getText().trim();

        return AppUtils.firstError(
                AppUtils.requireCombo(carId, "العربية"),
                AppUtils.requireCombo(fuelType, "نوع البنزين"),
                AppUtils.requireText(station, "اسم محطة البنزين"),
                AppUtils.requirePositiveDouble(liters, "عدد اللترات"),
                AppUtils.requirePositiveDouble(cost, "التكلفة")
        );
    }
}
