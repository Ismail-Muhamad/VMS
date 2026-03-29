
import DB.DB;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.*;
import java.awt.*;

public class SettingsPanel extends JPanel {

    // ===== Style =====
    private static final Color BG = new Color(243, 244, 246);

    // ===== Tabs =====
    private final JTabbedPane tabs;

    // ===== Company =====
    private JTextField txtCompanyName;
    private JTextField txtHQRegion;

    // ===== Departments =====
    private JTable deptTable;
    private DefaultTableModel deptModel;
    private JTextField txtDeptName;
    private Integer selectedDeptId = null;

    // ===== Maintenance Types =====
    private JTable mtTable;
    private DefaultTableModel mtModel;
    private JTextField txtMtName;
    private Integer selectedMtId = null;

    // ===== Car Types =====
    private JTable carTypeTable;
    private DefaultTableModel carTypeModel;
    private JTextField txtCarTypeName;
    private Integer selectedCarTypeId = null;

    // ===== Users =====
    private JTable usersTable;
    private DefaultTableModel usersModel;
    private JComboBox<ComboItem> cbEmployee;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JComboBox<String> cbRole;
    private Integer selectedUserId = null;

    // ===== Card Panel =====
    private static class CardPanel extends JPanel {

        private final int arc = 18;
        private final int shadowSize = 8;

        CardPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // shadow
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillRoundRect(shadowSize, shadowSize, w - shadowSize * 2, h - shadowSize * 2, arc, arc);

            // card bg
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, w - shadowSize * 2, h - shadowSize * 2, arc, arc);

            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        public Insets getInsets() {
            return new Insets(10, 10, 10, 10);
        }
    }

    public SettingsPanel(AuthUser session) {

        setLayout(new BorderLayout());
        setBackground(BG);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("الإعدادات (إدارة النظام)");
        title.setFont(new Font("Tahoma", Font.BOLD, 22));
        title.setHorizontalAlignment(SwingConstants.RIGHT);

        JLabel sub = new JLabel("بيانات الشركة - المستخدمين - الأقسام- أنواع السيارات - أنواع الصيانة");
        sub.setFont(new Font("Tahoma", Font.PLAIN, 13));
        sub.setForeground(new Color(75, 85, 99));
        sub.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setOpaque(false);
        header.add(title);
        header.add(sub);

        tabs = new JTabbedPane();
        tabs.setFont(new Font("Tahoma", Font.PLAIN, 13));
        tabs.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        boolean isAdmin = session != null && session.isAdmin();

        // ADMIN بس
        if (isAdmin) {
            tabs.addTab("بيانات الشركة", createCompanyTab());
            tabs.addTab("المستخدمين", createUsersTab());
        }

        // ADMIN + SUB_ADMIN
        tabs.addTab("الأقسام", createDepartmentsTab());
        tabs.addTab("أنواع الصيانة", createMaintenanceTypesTab());
        tabs.addTab("أنواع السيارات", createCarTypesTab());

        root.add(header, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);

        add(root, BorderLayout.CENTER);

        SwingUtilities.invokeLater(this::refreshAll);
    }

    // ====================== Company Tab ======================
    private JPanel createCompanyTab() {
        CardPanel card = new CardPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BorderLayout(12, 12));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel h = new JLabel("بيانات الشركة");
        h.setFont(new Font("Tahoma", Font.BOLD, 16));
        h.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        txtCompanyName = new JTextField();
        txtCompanyName.setHorizontalAlignment(SwingConstants.RIGHT);
        txtCompanyName.setBorder(titled("اسم الشركة"));

        txtHQRegion = new JTextField();
        txtHQRegion.setHorizontalAlignment(SwingConstants.RIGHT);
        txtHQRegion.setBorder(titled("مقر/منطقة الشركة"));

        gbc.gridy = 0;
        form.add(txtCompanyName, gbc);
        gbc.gridy++;
        form.add(txtHQRegion, gbc);

        JButton save = new JButton("حفظ");
        styleButton(save, new Color(37, 99, 235));
        save.addActionListener(e -> saveCompanyInfo());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(save);

        card.add(h, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);

        return wrap(card);
    }

    private void loadCompanyInfo() {
        try {
            Object[] row = DB.queryOne(
                    "SELECT company_name, hq_region FROM Company_Info WHERE company_id = 1",
                    rs -> new Object[]{rs.getString(1), rs.getString(2)}
            );

            if (row != null) {
                txtCompanyName.setText(row[0] == null ? "" : String.valueOf(row[0]));
                txtHQRegion.setText(row[1] == null ? "" : String.valueOf(row[1]));
            } else {
                txtCompanyName.setText("");
                txtHQRegion.setText("");
            }

        } catch (Exception ex) {
            showDbErr("فشل تحميل بيانات الشركة", ex);
        }
    }

    private void saveCompanyInfo() {
        String name = txtCompanyName.getText().trim();
        String region = txtHQRegion.getText().trim();

        if (name.isEmpty() || region.isEmpty()) {
            warn("اسم الشركة ومقر/منطقة الشركة مطلوبين.");
            return;
        }

        try {
            DB.executeUpdate(
                    "MERGE INTO Company_Info t "
                    + "USING (SELECT 1 AS company_id, ? AS company_name, ? AS hq_region FROM dual) s "
                    + "ON (t.company_id = s.company_id) "
                    + "WHEN MATCHED THEN UPDATE SET t.company_name = s.company_name, t.hq_region = s.hq_region "
                    + "WHEN NOT MATCHED THEN INSERT (company_id, company_name, hq_region) VALUES (s.company_id, s.company_name, s.hq_region)",
                    name, region
            );

            info("تم حفظ بيانات الشركة.");

        } catch (Exception ex) {
            showDbErr("فشل حفظ بيانات الشركة", ex);
        }
    }

    // ====================== Departments Tab ======================
    private JPanel createDepartmentsTab() {
        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setOpaque(false);

        CardPanel tableCard = new CardPanel();
        tableCard.setBackground(Color.WHITE);
        tableCard.setLayout(new BorderLayout(8, 8));
        tableCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel h = new JLabel("الأقسام (Department_Type)");
        h.setFont(new Font("Tahoma", Font.BOLD, 16));
        h.setHorizontalAlignment(SwingConstants.RIGHT);

        deptModel = new DefaultTableModel(new Object[][]{}, new Object[]{"ID", "اسم القسم"}) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        deptTable = new JTable(deptModel);
        styleTable(deptTable);

        deptTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int row = deptTable.getSelectedRow();
            if (row == -1) {
                return;
            }

            int mrow = deptTable.convertRowIndexToModel(row);
            int id = AppUtils.parseIntSafe(deptModel.getValueAt(mrow, 0), -1);
            selectedDeptId = (id <= 0) ? null : id;
            txtDeptName.setText(String.valueOf(deptModel.getValueAt(mrow, 1)));
        });

        JScrollPane sp = new JScrollPane(deptTable);
        sp.setBorder(BorderFactory.createEmptyBorder());

        tableCard.add(h, BorderLayout.NORTH);
        tableCard.add(sp, BorderLayout.CENTER);

        CardPanel formCard = new CardPanel();
        formCard.setBackground(Color.WHITE);
        formCard.setPreferredSize(new Dimension(360, 0));
        formCard.setLayout(new BorderLayout(10, 10));
        formCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel fh = new JLabel("إدارة القسم");
        fh.setFont(new Font("Tahoma", Font.BOLD, 16));
        fh.setHorizontalAlignment(SwingConstants.RIGHT);

        txtDeptName = new JTextField();
        txtDeptName.setHorizontalAlignment(SwingConstants.RIGHT);
        txtDeptName.setBorder(titled("اسم القسم"));

        JButton add = new JButton("إضافة");
        styleButton(add, new Color(22, 163, 74));
        add.addActionListener(e -> addDepartment());

        JButton upd = new JButton("تعديل");
        styleButton(upd, new Color(37, 99, 235));
        upd.addActionListener(e -> updateDepartment());

        JButton del = new JButton("حذف");
        styleButton(del, new Color(220, 38, 38));
        del.addActionListener(e -> deleteDepartment());

        JButton clr = new JButton("مسح");
        styleButton(clr, new Color(100, 116, 139));
        clr.addActionListener(e -> clearDepartmentForm());

        JPanel actions = new JPanel(new GridLayout(2, 2, 8, 8));
        actions.setOpaque(false);
        actions.add(add);
        actions.add(upd);
        actions.add(del);
        actions.add(clr);

        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.setOpaque(false);
        form.add(txtDeptName, BorderLayout.NORTH);
        form.add(actions, BorderLayout.CENTER);

        formCard.add(fh, BorderLayout.NORTH);
        formCard.add(form, BorderLayout.CENTER);

        root.add(tableCard, BorderLayout.CENTER);
        root.add(formCard, BorderLayout.EAST);

        return root;
    }

    private void loadDepartments() {
        if (deptTable == null) {
            return;
        }
        try {
            DefaultTableModel m = DB.queryToTableModel(
                    "SELECT dept_id, dept_name FROM Department_Type ORDER BY dept_id DESC"
            );
            m.setColumnIdentifiers(new Object[]{"ID", "اسم القسم"});
            deptModel = m;
            deptTable.setModel(deptModel);
            styleTable(deptTable);
        } catch (Exception ex) {
            showDbErr("فشل تحميل الأقسام", ex);
        }
    }

    private void addDepartment() {
        String name = txtDeptName.getText().trim();
        if (name.isEmpty()) {
            warn("اسم القسم مطلوب.");
            return;
        }

        try {
            DB.call("{ call PRC_DEPARTMENT_TYPE_INS(?) }", name);
            loadDepartments();
            clearDepartmentForm();
        } catch (Exception ex) {
            showDbErr("فشل إضافة القسم", ex);
        }
    }

    private void updateDepartment() {
        if (selectedDeptId == null) {
            warn("اختر قسم من الجدول للتعديل.");
            return;
        }
        String name = txtDeptName.getText().trim();
        if (name.isEmpty()) {
            warn("اسم القسم مطلوب.");
            return;
        }

        try {
            DB.call("{ call PRC_DEPARTMENT_TYPE_UPD(?, ?) }", selectedDeptId, name);
            loadDepartments();
            clearDepartmentForm();
        } catch (Exception ex) {
            showDbErr("فشل تعديل القسم", ex);
        }
    }

    private void deleteDepartment() {
        if (selectedDeptId == null) {
            warn("اختر قسم من الجدول للحذف.");
            return;
        }

        int ok = JOptionPane.showConfirmDialog(this,
                "متأكد تحذف القسم؟", "تأكيد", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            DB.call("{ call PRC_DEPARTMENT_TYPE_DEL(?) }", selectedDeptId);
            loadDepartments();
            clearDepartmentForm();
        } catch (Exception ex) {
            showDbErr("فشل حذف القسم", ex);
        }
    }

    private void clearDepartmentForm() {
        selectedDeptId = null;
        txtDeptName.setText("");
        deptTable.clearSelection();
    }

    // ====================== Car Types Tab ======================
    private JPanel createCarTypesTab() {
        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setOpaque(false);

        CardPanel tableCard = new CardPanel();
        tableCard.setBackground(Color.WHITE);
        tableCard.setLayout(new BorderLayout(8, 8));
        tableCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel h = new JLabel("أنواع السيارات (Car_Type)");
        h.setFont(new Font("Tahoma", Font.BOLD, 16));
        h.setHorizontalAlignment(SwingConstants.RIGHT);

        carTypeModel = new DefaultTableModel(new Object[][]{}, new Object[]{"ID", "اسم النوع"}) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        carTypeTable = new JTable(carTypeModel);
        styleTable(carTypeTable);

        carTypeTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int row = carTypeTable.getSelectedRow();
            if (row == -1) {
                return;
            }

            int mrow = carTypeTable.convertRowIndexToModel(row);
            int id = AppUtils.parseIntSafe(carTypeModel.getValueAt(mrow, 0), -1);
            selectedCarTypeId = (id <= 0) ? null : id;
            txtCarTypeName.setText(String.valueOf(carTypeModel.getValueAt(mrow, 1)));
        });

        JScrollPane sp = new JScrollPane(carTypeTable);
        sp.setBorder(BorderFactory.createEmptyBorder());

        tableCard.add(h, BorderLayout.NORTH);
        tableCard.add(sp, BorderLayout.CENTER);

        CardPanel formCard = new CardPanel();
        formCard.setBackground(Color.WHITE);
        formCard.setPreferredSize(new Dimension(360, 0));
        formCard.setLayout(new BorderLayout(10, 10));
        formCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel fh = new JLabel("إدارة نوع السيارة");
        fh.setFont(new Font("Tahoma", Font.BOLD, 16));
        fh.setHorizontalAlignment(SwingConstants.RIGHT);

        txtCarTypeName = new JTextField();
        txtCarTypeName.setHorizontalAlignment(SwingConstants.RIGHT);
        txtCarTypeName.setBorder(titled("اسم النوع"));

        JButton add = new JButton("إضافة");
        styleButton(add, new Color(22, 163, 74));
        add.addActionListener(e -> addCarType());

        JButton upd = new JButton("تعديل");
        styleButton(upd, new Color(37, 99, 235));
        upd.addActionListener(e -> updateCarType());

        JButton del = new JButton("حذف");
        styleButton(del, new Color(220, 38, 38));
        del.addActionListener(e -> deleteCarType());

        JButton clr = new JButton("مسح");
        styleButton(clr, new Color(100, 116, 139));
        clr.addActionListener(e -> clearCarTypeForm());

        JPanel actions = new JPanel(new GridLayout(2, 2, 8, 8));
        actions.setOpaque(false);
        actions.add(add);
        actions.add(upd);
        actions.add(del);
        actions.add(clr);

        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.setOpaque(false);
        form.add(txtCarTypeName, BorderLayout.NORTH);
        form.add(actions, BorderLayout.CENTER);

        formCard.add(fh, BorderLayout.NORTH);
        formCard.add(form, BorderLayout.CENTER);

        root.add(tableCard, BorderLayout.CENTER);
        root.add(formCard, BorderLayout.EAST);

        return root;
    }

    private void loadCarTypes() {
        if (carTypeTable == null) {
            return;
        }
        try {
            DefaultTableModel m = DB.queryToTableModel(
                    "SELECT car_type_id, car_type_name FROM Car_Type ORDER BY car_type_id DESC"
            );
            m.setColumnIdentifiers(new Object[]{"ID", "اسم النوع"});
            carTypeModel = m;
            carTypeTable.setModel(carTypeModel);
            styleTable(carTypeTable);
        } catch (Exception ex) {
            showDbErr("فشل تحميل أنواع السيارات", ex);
        }
    }

    private void addCarType() {
        String name = txtCarTypeName.getText().trim();
        if (name.isEmpty()) {
            warn("اسم النوع مطلوب.");
            return;
        }

        try {
            DB.call("{ call PRC_CAR_TYPE_INS(?) }", name);
            loadCarTypes();
            clearCarTypeForm();
        } catch (Exception ex) {
            showDbErr("فشل إضافة نوع السيارة", ex);
        }
    }

    private void updateCarType() {
        if (selectedCarTypeId == null) {
            warn("اختر نوع سيارة من الجدول للتعديل.");
            return;
        }
        String name = txtCarTypeName.getText().trim();
        if (name.isEmpty()) {
            warn("اسم النوع مطلوب.");
            return;
        }

        try {
            DB.call("{ call PRC_CAR_TYPE_UPD(?, ?) }", selectedCarTypeId, name);
            loadCarTypes();
            clearCarTypeForm();
        } catch (Exception ex) {
            showDbErr("فشل تعديل نوع السيارة", ex);
        }
    }

    private void deleteCarType() {
        if (selectedCarTypeId == null) {
            warn("اختر نوع سيارة من الجدول للحذف.");
            return;
        }

        int ok = JOptionPane.showConfirmDialog(this,
                "متأكد تحذف نوع السيارة؟", "تأكيد", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            DB.call("{ call PRC_CAR_TYPE_DEL(?) }", selectedCarTypeId);
            loadCarTypes();
            clearCarTypeForm();
        } catch (Exception ex) {
            showDbErr("فشل حذف نوع السيارة", ex);
        }
    }

    private void clearCarTypeForm() {
        selectedCarTypeId = null;
        txtCarTypeName.setText("");
        carTypeTable.clearSelection();
    }

    // ====================== Maintenance Types Tab ======================
    private JPanel createMaintenanceTypesTab() {
        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setOpaque(false);

        CardPanel tableCard = new CardPanel();
        tableCard.setBackground(Color.WHITE);
        tableCard.setLayout(new BorderLayout(8, 8));
        tableCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel h = new JLabel("أنواع الصيانة (Maintenance_Type)");
        h.setFont(new Font("Tahoma", Font.BOLD, 16));
        h.setHorizontalAlignment(SwingConstants.RIGHT);

        mtModel = new DefaultTableModel(new Object[][]{}, new Object[]{"ID", "اسم النوع"}) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        mtTable = new JTable(mtModel);
        styleTable(mtTable);

        mtTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int row = mtTable.getSelectedRow();
            if (row == -1) {
                return;
            }

            int mrow = mtTable.convertRowIndexToModel(row);
            int id = AppUtils.parseIntSafe(mtModel.getValueAt(mrow, 0), -1);
            selectedMtId = (id <= 0) ? null : id;
            txtMtName.setText(String.valueOf(mtModel.getValueAt(mrow, 1)));
        });

        JScrollPane sp = new JScrollPane(mtTable);
        sp.setBorder(BorderFactory.createEmptyBorder());

        tableCard.add(h, BorderLayout.NORTH);
        tableCard.add(sp, BorderLayout.CENTER);

        CardPanel formCard = new CardPanel();
        formCard.setBackground(Color.WHITE);
        formCard.setPreferredSize(new Dimension(360, 0));
        formCard.setLayout(new BorderLayout(10, 10));
        formCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel fh = new JLabel("إدارة نوع الصيانة");
        fh.setFont(new Font("Tahoma", Font.BOLD, 16));
        fh.setHorizontalAlignment(SwingConstants.RIGHT);

        txtMtName = new JTextField();
        txtMtName.setHorizontalAlignment(SwingConstants.RIGHT);
        txtMtName.setBorder(titled("اسم النوع"));

        JButton add = new JButton("إضافة");
        styleButton(add, new Color(22, 163, 74));
        add.addActionListener(e -> addMaintenanceType());

        JButton upd = new JButton("تعديل");
        styleButton(upd, new Color(37, 99, 235));
        upd.addActionListener(e -> updateMaintenanceType());

        JButton del = new JButton("حذف");
        styleButton(del, new Color(220, 38, 38));
        del.addActionListener(e -> deleteMaintenanceType());

        JButton clr = new JButton("مسح");
        styleButton(clr, new Color(100, 116, 139));
        clr.addActionListener(e -> clearMaintenanceTypeForm());

        JPanel actions = new JPanel(new GridLayout(2, 2, 8, 8));
        actions.setOpaque(false);
        actions.add(add);
        actions.add(upd);
        actions.add(del);
        actions.add(clr);

        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.setOpaque(false);
        form.add(txtMtName, BorderLayout.NORTH);
        form.add(actions, BorderLayout.CENTER);

        formCard.add(fh, BorderLayout.NORTH);
        formCard.add(form, BorderLayout.CENTER);

        root.add(tableCard, BorderLayout.CENTER);
        root.add(formCard, BorderLayout.EAST);

        return root;
    }

    private void loadMaintenanceTypes() {
        if (mtTable == null) {
            return;
        }
        try {
            DefaultTableModel m = DB.queryToTableModel(
                    "SELECT maintenance_type_id, maintenance_name FROM Maintenance_Type ORDER BY maintenance_type_id DESC"
            );
            m.setColumnIdentifiers(new Object[]{"ID", "اسم النوع"});
            mtModel = m;
            mtTable.setModel(mtModel);
            styleTable(mtTable);
        } catch (Exception ex) {
            showDbErr("فشل تحميل أنواع الصيانة", ex);
        }
    }

    private void addMaintenanceType() {
        String name = txtMtName.getText().trim();
        if (name.isEmpty()) {
            warn("اسم النوع مطلوب.");
            return;
        }

        try {
            DB.call("{ call PRC_MAINTENANCE_TYPE_INS(?) }", name);
            loadMaintenanceTypes();
            clearMaintenanceTypeForm();
        } catch (Exception ex) {
            showDbErr("فشل إضافة نوع الصيانة", ex);
        }
    }

    private void updateMaintenanceType() {
        if (selectedMtId == null) {
            warn("اختر نوع صيانة من الجدول للتعديل.");
            return;
        }
        String name = txtMtName.getText().trim();
        if (name.isEmpty()) {
            warn("اسم النوع مطلوب.");
            return;
        }

        try {
            DB.call("{ call PRC_MAINTENANCE_TYPE_UPD(?, ?) }", selectedMtId, name);
            loadMaintenanceTypes();
            clearMaintenanceTypeForm();
        } catch (Exception ex) {
            showDbErr("فشل تعديل نوع الصيانة", ex);
        }
    }

    private void deleteMaintenanceType() {
        if (selectedMtId == null) {
            warn("اختر نوع صيانة من الجدول للحذف.");
            return;
        }

        int ok = JOptionPane.showConfirmDialog(this,
                "متأكد تحذف نوع الصيانة؟", "تأكيد", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            DB.call("{ call PRC_MAINTENANCE_TYPE_DEL(?) }", selectedMtId);
            loadMaintenanceTypes();
            clearMaintenanceTypeForm();
        } catch (Exception ex) {
            showDbErr("فشل حذف نوع الصيانة", ex);
        }
    }

    private void clearMaintenanceTypeForm() {
        selectedMtId = null;
        txtMtName.setText("");
        mtTable.clearSelection();
    }

    // ====================== Users Tab ======================
    private JPanel createUsersTab() {
        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setOpaque(false);

        // ===== Users Table Card =====
        CardPanel tableCard = new CardPanel();
        tableCard.setBackground(Color.WHITE);
        tableCard.setLayout(new BorderLayout(8, 8));
        tableCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel h = new JLabel("المستخدمين (Users)");
        h.setFont(new Font("Tahoma", Font.BOLD, 16));
        h.setHorizontalAlignment(SwingConstants.RIGHT);

        usersModel = new DefaultTableModel(new Object[][]{}, new Object[]{
            "ID", "Employee_ID", "الموظف", "Username", "Role", "Created"
        }) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        usersTable = new JTable(usersModel);
        styleTable(usersTable);
        hideColumnByModelIndex(usersTable, 1); // Employee_ID مخفي

        usersTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int row = usersTable.getSelectedRow();
            if (row == -1) {
                return;
            }

            int mrow = usersTable.convertRowIndexToModel(row);
            int uid = AppUtils.parseIntSafe(usersModel.getValueAt(mrow, 0), -1);
            int empId = AppUtils.parseIntSafe(usersModel.getValueAt(mrow, 1), -1);
            String username = String.valueOf(usersModel.getValueAt(mrow, 3));
            String role = String.valueOf(usersModel.getValueAt(mrow, 4));

            selectedUserId = (uid <= 0) ? null : uid;
            if (empId > 0) {
                AppUtils.selectComboById(cbEmployee, empId);
            }
            txtUsername.setText(username);
            txtPassword.setText(""); // ما نعرضش الباسورد
            cbRole.setSelectedItem(role == null ? "USER" : role);
        });

        JScrollPane sp = new JScrollPane(usersTable);
        sp.setBorder(BorderFactory.createEmptyBorder());

        tableCard.add(h, BorderLayout.NORTH);
        tableCard.add(sp, BorderLayout.CENTER);

        // ===== Form Card =====
        CardPanel formCard = new CardPanel();
        formCard.setBackground(Color.WHITE);
        formCard.setPreferredSize(new Dimension(420, 0));
        formCard.setLayout(new BorderLayout(10, 10));
        formCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel fh = new JLabel("إدارة المستخدم");
        fh.setFont(new Font("Tahoma", Font.BOLD, 16));
        fh.setHorizontalAlignment(SwingConstants.RIGHT);

        cbEmployee = new JComboBox<>();
        cbEmployee.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        cbEmployee.setBorder(titled("الموظف"));

        txtUsername = new JTextField();
        txtUsername.setHorizontalAlignment(SwingConstants.RIGHT);
        txtUsername.setBorder(titled("Username"));

        txtPassword = new JPasswordField();
        txtPassword.setHorizontalAlignment(SwingConstants.RIGHT);
        txtPassword.setBorder(titled("Password (نص/VARCHAR - اكتب للتغيير)"));

        cbRole = new JComboBox<>(new String[]{"ADMIN", "SUB_ADMIN", "USER"});
        cbRole.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        cbRole.setBorder(titled("Role"));

        JButton add = new JButton("إضافة");
        styleButton(add, new Color(22, 163, 74));
        add.addActionListener(e -> addUser());

        JButton upd = new JButton("تعديل");
        styleButton(upd, new Color(37, 99, 235));
        upd.addActionListener(e -> updateUser());

        JButton del = new JButton("حذف");
        styleButton(del, new Color(220, 38, 38));
        del.addActionListener(e -> deleteUser());

        JButton clr = new JButton("مسح");
        styleButton(clr, new Color(100, 116, 139));
        clr.addActionListener(e -> clearUserForm());

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridy = 0;

        form.add(cbEmployee, gbc);
        gbc.gridy++;
        form.add(txtUsername, gbc);
        gbc.gridy++;
        form.add(txtPassword, gbc);
        gbc.gridy++;
        form.add(cbRole, gbc);
        gbc.gridy++;

        JPanel actions = new JPanel(new GridLayout(2, 2, 8, 8));
        actions.setOpaque(false);
        actions.add(add);
        actions.add(upd);
        actions.add(del);
        actions.add(clr);

        formCard.add(fh, BorderLayout.NORTH);
        formCard.add(form, BorderLayout.CENTER);
        formCard.add(actions, BorderLayout.SOUTH);

        root.add(tableCard, BorderLayout.CENTER);
        root.add(formCard, BorderLayout.EAST);

        return root;
    }

    private void loadEmployeesForUsers() {
        if (cbEmployee == null) {
            return;
        }
        cbEmployee.removeAllItems();
        try {
            var list = DB.query(
                    "SELECT Employee_ID, Employee_Name FROM Employees ORDER BY Employee_ID DESC",
                    rs -> new ComboItem(rs.getInt(1), rs.getString(2))
            );
            for (ComboItem it : list) {
                cbEmployee.addItem(it);
            }
        } catch (Exception ex) {
            showDbErr("فشل تحميل الموظفين", ex);
        }
    }

    private void loadUsers() {
        if (usersTable == null) {
            return;
        }
        try {
            DefaultTableModel m = DB.queryToTableModel(
                    "SELECT "
                    + "  u.user_id AS ID, "
                    + "  u.employee_id AS EMP_ID, "
                    + "  e.employee_name AS EMP_NAME, "
                    + "  u.username AS UNAME, "
                    + "  u.role_name AS RNAME, "
                    + "  TO_CHAR(u.created_at,'YYYY-MM-DD') AS C_AT "
                    + "FROM Users u "
                    + "JOIN Employees e ON e.employee_id = u.employee_id "
                    + "ORDER BY u.user_id DESC"
            );
            m.setColumnIdentifiers(new Object[]{"ID", "Employee_ID", "الموظف", "Username", "Role", "Created"});

            usersModel = m;
            usersTable.setModel(usersModel);
            styleTable(usersTable);
            hideColumnByModelIndex(usersTable, 1);
        } catch (Exception ex) {
            showDbErr("فشل تحميل المستخدمين", ex);
        }
    }

    private static final class UserSnapshot {

        final String storedPassword;

        UserSnapshot(String storedPassword) {
            this.storedPassword = storedPassword;
        }
    }

    private UserSnapshot loadUserSnapshot(int userId) {
        return DB.queryOne(
                "SELECT password_hash FROM Users WHERE user_id = ?",
                rs -> new UserSnapshot(rs.getString(1)),
                userId
        );
    }

    private void addUser() {
        Integer empId = AppUtils.getSelectedComboId(cbEmployee);
        String username = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword());
        String role = String.valueOf(cbRole.getSelectedItem());

        if (empId == null) {
            warn("اختر موظف.");
            return;
        }
        if (username.isEmpty()) {
            warn("Username مطلوب.");
            return;
        }
        if (!AppUtils.isValidUsername(username)) {
            warn("Username لازم يكون من 3 إلى 30 حرف/رقم، ومسموح بـ . _ - فقط.");
            return;
        }
        if (pass.isEmpty()) {
            warn("Password مطلوب.");
            return;
        }
        if (pass.length() < 4) {
            warn("Password لازم يكون 4 حروف أو أرقام على الأقل.");
            return;
        }

        try {
            String storedPass = pass;
            DB.call("{ call PRC_USERS_INS(?, ?, ?, ?) }",
                    empId, username, role, storedPass
            );

            loadUsers();
            clearUserForm();

        } catch (Exception ex) {
            showDbErr("فشل إضافة المستخدم", ex);
        }
    }

    private void updateUser() {
        if (selectedUserId == null) {
            warn("اختر مستخدم من الجدول للتعديل.");
            return;
        }

        Integer empId = AppUtils.getSelectedComboId(cbEmployee);
        String username = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword());
        String role = String.valueOf(cbRole.getSelectedItem());

        if (empId == null) {
            warn("اختر موظف.");
            return;
        }
        if (username.isEmpty()) {
            warn("Username مطلوب.");
            return;
        }
        if (!AppUtils.isValidUsername(username)) {
            warn("Username لازم يكون من 3 إلى 30 حرف/رقم، ومسموح بـ . _ - فقط.");
            return;
        }
        if (!pass.isEmpty() && pass.length() < 4) {
            warn("Password لازم يكون 4 حروف أو أرقام على الأقل.");
            return;
        }

        try {
            UserSnapshot snap = loadUserSnapshot(selectedUserId);
            if (snap == null) {
                throw new RuntimeException("المستخدم غير موجود.");
            }

            String finalPass = (pass.isEmpty()) ? snap.storedPassword : pass;

            DB.call("{ call PRC_USERS_UPD(?, ?, ?, ?, ?) }",
                    selectedUserId,
                    empId,
                    username,
                    role,
                    finalPass
            );

            loadUsers();
            clearUserForm();

        } catch (RuntimeException ex) {
            showDbErr("فشل تعديل المستخدم", ex);
        }
    }

    private void deleteUser() {
        if (selectedUserId == null) {
            warn("اختر مستخدم من الجدول للحذف.");
            return;
        }

        int ok = JOptionPane.showConfirmDialog(this,
                "متأكد تحذف المستخدم؟", "تأكيد", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            // ✅ Procedure DEL
            DB.call("{ call PRC_USERS_DEL(?) }", selectedUserId);

            loadUsers();
            clearUserForm();
        } catch (Exception ex) {
            showDbErr("فشل حذف المستخدم", ex);
        }
    }

    private void clearUserForm() {
        selectedUserId = null;
        if (cbEmployee.getItemCount() > 0) {
            cbEmployee.setSelectedIndex(-1);
        }
        txtUsername.setText("");
        txtPassword.setText("");
        cbRole.setSelectedIndex(0);
        usersTable.clearSelection();
    }

    // ====================== Public refresh ======================
    public void refreshAll() {
        SwingUtilities.invokeLater(() -> {
            try {

                // Company info (ADMIN only)
                if (txtCompanyName != null && txtHQRegion != null) {
                    loadCompanyInfo();
                }

                // Departments / Maintenance Types / Car Types (ADMIN & SUB_ADMIN)
                if (deptTable != null) {
                    loadDepartments();
                    clearDepartmentForm();
                }
                if (mtTable != null) {
                    loadMaintenanceTypes();
                    clearMaintenanceTypeForm();
                }
                if (carTypeTable != null) {
                    loadCarTypes();
                    clearCarTypeForm();
                }

                // Users (ADMIN only)
                if (usersTable != null) {
                    loadEmployeesForUsers();
                    loadUsers();
                    clearUserForm();
                }

            } catch (Exception ex) {
                showDbErr("Failed to refresh settings", ex);
            }
        });
    }

    // ====================== UI helpers ======================
    private JPanel wrap(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private TitledBorder titled(String t) {
        TitledBorder b = BorderFactory.createTitledBorder(t);
        b.setTitleFont(new Font("Tahoma", Font.PLAIN, 12));
        b.setTitleJustification(TitledBorder.RIGHT);
        return b;
    }

    private void styleButton(JButton b, Color bg) {
        b.setFont(new Font("Tahoma", Font.PLAIN, 13));
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(8, 10, 8, 10));
    }

    private void styleTable(JTable t) {
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
    }

    private void hideColumnByModelIndex(JTable table, int modelIndex) {
        try {
            int view = table.convertColumnIndexToView(modelIndex);
            if (view < 0) {
                return;
            }
            TableColumn col = table.getColumnModel().getColumn(view);
            col.setMinWidth(0);
            col.setMaxWidth(0);
            col.setPreferredWidth(0);
        } catch (Exception ignored) {
        }
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "تنبيه", JOptionPane.WARNING_MESSAGE);
    }

    private void info(String msg) {
        JOptionPane.showMessageDialog(this, msg, "تم", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showDbErr(String title, Exception ex) {
        JOptionPane.showMessageDialog(this,
                title + "\n" + ex.getMessage(),
                "DB Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
