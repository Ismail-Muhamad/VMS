
import DB.DB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class TransportDashboard extends JFrame {

    // ===== Panels =====
    private VehiclesPanel vehiclesPanel;
    private DriversPanel driversPanel;
    private EmployeesPanel employeesPanel;
    private LinesConfigPanel linesConfigPanel;
    private MaintenancePanel maintenancePanel;
    private FuelPanel fuelPanel;
    private SettingsPanel settingsPanel;

    // ===== Sidebar buttons =====
    private SidebarButton selectedSidebarButton;
    private SidebarButton homeBtn;
    private SidebarButton employeesBtn;
    private SidebarButton carsBtn;
    private SidebarButton driversBtn;
    private SidebarButton linesBtn;
    private SidebarButton maintBtn;
    private SidebarButton fuelBtn;
    private SidebarButton settingsBtn;

    private JPanel contentPanel;
    private CardLayout contentLayout;

    // ===== Top stats labels =====
    private JLabel lblLinesCount;
    private JLabel lblCarsCount;
    private JLabel lblDriversCount;
    private JLabel lblEmployeesCount;

    // ===== Session =====
    private final AuthUser session;
    private JLabel topUserNameLabel;

    // ===== Fleet health strip =====
    private JLabel fhCarsAssignedDrivers;
    private JLabel fhCarsAssignedLines;
    private JLabel fhLicensesSoon;
    private JProgressBar pbDriversAssigned;
    private JProgressBar pbLinesAssigned;
    private JProgressBar pbLicensesSoon;

    private JLabel fhFuel7d;
    private JLabel fhMaint30d;

    // ===== Alerts center =====
    private JTable alertsTable;
    private DefaultTableModel alertsModel;
    private JComboBox<String> cbAlertFilter;
    private JTextField txtAlertSearch;
    private List<AlertItem> allAlerts = new ArrayList<>();

    public TransportDashboard(AuthUser session) {
        this.session = session;

        setTitle("نظام إدارة حركة سيارات الموظفين");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1500, 900);
        setMinimumSize(new Dimension(1200, 700));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        applyRightToLeft(getContentPane());

        Color primaryBlue = new Color(18, 72, 132);
        Color bgColor = new Color(243, 244, 246);
        getContentPane().setBackground(bgColor);

        add(createTopBar(primaryBlue), BorderLayout.NORTH);
        add(createSidebar(primaryBlue), BorderLayout.EAST);

        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        contentPanel.setBackground(bgColor);

        JPanel dashboardPanel = new JPanel(new BorderLayout());
        dashboardPanel.setBorder(new EmptyBorder(16, 16, 16, 16));
        dashboardPanel.setBackground(bgColor);

        dashboardPanel.add(createStatsPanel(), BorderLayout.NORTH);
        dashboardPanel.add(createDashboardBody(), BorderLayout.CENTER);

        driversPanel = new DriversPanel();
        employeesPanel = new EmployeesPanel();
        vehiclesPanel = new VehiclesPanel();
        linesConfigPanel = new LinesConfigPanel();
        maintenancePanel = new MaintenancePanel();
        fuelPanel = new FuelPanel();
        settingsPanel = new SettingsPanel(session);

        contentPanel.add(dashboardPanel, "DASHBOARD");
        contentPanel.add(driversPanel, "DRIVERS");
        contentPanel.add(employeesPanel, "EMPLOYEES");
        contentPanel.add(vehiclesPanel, "VEHICLES");
        contentPanel.add(linesConfigPanel, "LINES");
        contentPanel.add(maintenancePanel, "MAINTENANCE");
        contentPanel.add(fuelPanel, "FUEL");
        contentPanel.add(settingsPanel, "SETTINGS");

        add(contentPanel, BorderLayout.CENTER);

        SwingUtilities.invokeLater(this::showDashboardScreen);
    }

    // ================== Dashboard Body ==================
    private JPanel createDashboardBody() {
        JPanel body = new JPanel(new BorderLayout(16, 16));
        body.setOpaque(false);

        body.add(createFleetHealthStrip(), BorderLayout.NORTH);
        body.add(createAlertsCenter(), BorderLayout.CENTER);

        return body;
    }

    // ===== Fleet Health Strip =====
    private JPanel createFleetHealthStrip() {
        JPanel strip = new JPanel(new GridLayout(1, 3, 16, 0));
        strip.setOpaque(false);

        strip.add(createFleetCard_Assignments());
        strip.add(createFleetCard_Compliance());
        strip.add(createFleetCard_Activity());

        return strip;
    }

    private JPanel createFleetCard_Assignments() {
        CardPanel card = new CardPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel("توزيع وربط البيانات");
        title.setFont(new Font("Tahoma", Font.BOLD, 14));
        title.setHorizontalAlignment(SwingConstants.RIGHT);

        fhCarsAssignedDrivers = new JLabel("ربط سيارات بسائق: ...");
        fhCarsAssignedLines = new JLabel("ربط سيارات بخط: ...");

        for (JLabel l : new JLabel[]{fhCarsAssignedDrivers, fhCarsAssignedLines}) {
            l.setFont(new Font("Tahoma", Font.PLAIN, 13));
            l.setForeground(new Color(30, 41, 59));
            l.setHorizontalAlignment(SwingConstants.RIGHT);
        }

        pbDriversAssigned = makeMiniProgress();
        pbLinesAssigned = makeMiniProgress();

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new GridLayout(4, 1, 8, 6));
        rows.add(fhCarsAssignedDrivers);
        rows.add(pbDriversAssigned);
        rows.add(fhCarsAssignedLines);
        rows.add(pbLinesAssigned);

        card.add(title, BorderLayout.NORTH);
        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    private JPanel createFleetCard_Compliance() {
        CardPanel card = new CardPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel("التراخيص والالتزام");
        title.setFont(new Font("Tahoma", Font.BOLD, 14));
        title.setHorizontalAlignment(SwingConstants.RIGHT);

        fhLicensesSoon = new JLabel("تراخيص قريبة الانتهاء (30 يوم): ...");
        fhLicensesSoon.setFont(new Font("Tahoma", Font.PLAIN, 13));
        fhLicensesSoon.setForeground(new Color(30, 41, 59));
        fhLicensesSoon.setHorizontalAlignment(SwingConstants.RIGHT);

        pbLicensesSoon = makeMiniProgress();

        JLabel hint = new JLabel("هدفنا: صفر مفاجآت 😄");
        hint.setFont(new Font("Tahoma", Font.PLAIN, 12));
        hint.setForeground(new Color(100, 116, 139));
        hint.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel center = new JPanel(new GridLayout(3, 1, 8, 6));
        center.setOpaque(false);
        center.add(fhLicensesSoon);
        center.add(pbLicensesSoon);
        center.add(hint);

        card.add(title, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private JPanel createFleetCard_Activity() {
        CardPanel card = new CardPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel("نشاط آخر فترة");
        title.setFont(new Font("Tahoma", Font.BOLD, 14));
        title.setHorizontalAlignment(SwingConstants.RIGHT);

        fhFuel7d = new JLabel("بنزين آخر 7 أيام: ...");
        fhMaint30d = new JLabel("صيانة آخر 30 يوم: ...");

        fhFuel7d.setFont(new Font("Tahoma", Font.PLAIN, 13));
        fhMaint30d.setFont(new Font("Tahoma", Font.PLAIN, 13));

        fhFuel7d.setForeground(new Color(30, 41, 59));
        fhMaint30d.setForeground(new Color(30, 41, 59));

        fhFuel7d.setHorizontalAlignment(SwingConstants.RIGHT);
        fhMaint30d.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel grid = new JPanel(new GridLayout(2, 1, 8, 6));
        grid.setOpaque(false);
        grid.add(fhFuel7d);
        grid.add(fhMaint30d);

        card.add(title, BorderLayout.NORTH);
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JProgressBar makeMiniProgress() {
        JProgressBar pb = new JProgressBar(0, 100);
        pb.setValue(0);
        pb.setStringPainted(false);
        pb.setBorderPainted(false);
        pb.setOpaque(false);
        pb.setPreferredSize(new Dimension(100, 10));
        return pb;
    }

    // ===== Alerts Center =====
    private JPanel createAlertsCenter() {
        CardPanel card = new CardPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("مركز التنبيهات");
        title.setFont(new Font("Tahoma", Font.BOLD, 16));
        title.setHorizontalAlignment(SwingConstants.RIGHT);

        cbAlertFilter = new JComboBox<>(new String[]{"الكل", "تراخيص", "صيانة", "بنزين"});
        cbAlertFilter.setFont(new Font("Tahoma", Font.PLAIN, 13));

        txtAlertSearch = new JTextField();
        txtAlertSearch.setHorizontalAlignment(SwingConstants.RIGHT);
        txtAlertSearch.setFont(new Font("Tahoma", Font.PLAIN, 13));

        TitledBorder tb = BorderFactory.createTitledBorder("بحث داخل التنبيهات");
        tb.setTitleJustification(TitledBorder.RIGHT);
        txtAlertSearch.setBorder(tb);

        JButton btnRefresh = new JButton("تحديث");
        btnRefresh.setFont(new Font("Tahoma", Font.PLAIN, 12));
        btnRefresh.addActionListener(e -> refreshDashboardBodyAsync());

        JPanel topRow = new JPanel(new BorderLayout(10, 10));
        topRow.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(btnRefresh);
        left.add(cbAlertFilter);

        topRow.add(title, BorderLayout.EAST);
        topRow.add(left, BorderLayout.WEST);
        topRow.add(txtAlertSearch, BorderLayout.CENTER);

        alertsModel = new DefaultTableModel(
                new Object[][]{},
                new Object[]{"الأولوية", "النوع", "الوصف", "التاريخ", "GO"}
        ) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        alertsTable = new JTable(alertsModel);
        styleAlertsTable(alertsTable);

        alertsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = alertsTable.getSelectedRow();
                    if (row >= 0) {
                        openAlertAction(row);
                    }
                }
            }
        });

        JScrollPane sp = new JScrollPane(alertsTable);
        sp.setBorder(BorderFactory.createEmptyBorder());

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(alertsModel);
        alertsTable.setRowSorter(sorter);

        Runnable apply = () -> applyAlertsFilter(sorter);

        cbAlertFilter.addActionListener(e -> apply.run());
        txtAlertSearch.getDocument().addDocumentListener(new DocumentListener() {
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

        card.add(topRow, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    private void styleAlertsTable(JTable table) {
        table.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        table.setFont(new Font("Tahoma", Font.PLAIN, 13));
        table.setRowHeight(32);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(220, 220, 220));
        table.setSelectionBackground(new Color(37, 99, 235));
        table.setSelectionForeground(Color.WHITE);
        table.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = new JTableHeader(table.getColumnModel());
        header.setFont(new Font("Tahoma", Font.BOLD, 13));
        header.setBackground(new Color(230, 235, 245));
        header.setForeground(new Color(30, 41, 59));
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        table.setTableHeader(header);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        table.getColumnModel().getColumn(0).setCellRenderer(new SeverityBadgeRenderer());
        table.getColumnModel().getColumn(1).setCellRenderer(center);
        table.getColumnModel().getColumn(3).setCellRenderer(center);

        table.getColumnModel().getColumn(4).setMinWidth(0);
        table.getColumnModel().getColumn(4).setMaxWidth(0);
        table.getColumnModel().getColumn(4).setWidth(0);
    }

    private void applyAlertsFilter(TableRowSorter<DefaultTableModel> sorter) {
        String text = txtAlertSearch.getText().trim();
        String type = String.valueOf(cbAlertFilter.getSelectedItem());

        List<RowFilter<Object, Object>> filters = new ArrayList<>();

        if (!text.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 1, 2, 3));
        }

        if (type != null && !"الكل".equals(type)) {
            filters.add(new RowFilter<Object, Object>() {
                @Override
                public boolean include(Entry<? extends Object, ? extends Object> entry) {
                    String tag = String.valueOf(entry.getValue(4));

                    if ("تراخيص".equals(type)) {
                        return tag.startsWith("LIC_");
                    }
                    if ("صيانة".equals(type)) {
                        return tag.startsWith("MAINT_");
                    }
                    if ("بنزين".equals(type)) {
                        return tag.startsWith("FUEL_");
                    }
                    return true;
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

    private void openAlertAction(int viewRow) {
        int modelRow = alertsTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0) {
            return;
        }

        String tag = String.valueOf(alertsModel.getValueAt(modelRow, 4));
        if (tag == null) {
            return;
        }

        if (tag.startsWith("LIC_CAR") || tag.startsWith("FUEL_")) {
            showVehiclesScreen();
            return;
        }
        if (tag.startsWith("LIC_DRV") || tag.startsWith("DATA_DRV")) {
            showDriversScreen();
            return;
        }
        if (tag.startsWith("MAINT_")) {
            showMaintenanceScreen();
            return;
        }

        showDashboardScreen();
    }

    // ================== Refresh dashboard body ==================
    private void refreshDashboardBodyAsync() {
        fhCarsAssignedDrivers.setText("ربط سيارات بسائق: ...");
        fhCarsAssignedLines.setText("ربط سيارات بخط: ...");
        fhLicensesSoon.setText("تراخيص قريبة الانتهاء (30 يوم): ...");
        fhFuel7d.setText("بنزين آخر 7 أيام: ...");
        fhMaint30d.setText("صيانة آخر 30 يوم: ...");

        pbDriversAssigned.setValue(0);
        pbLinesAssigned.setValue(0);
        pbLicensesSoon.setValue(0);

        alertsModel.setRowCount(0);

        new SwingWorker<Void, Void>() {
            int totalCars, carsWithDriver, carsWithLine;
            int soonCarsLic, soonDrvLic;
            int maintCount30d;
            double fuelLiters7d, fuelCost7d;

            List<AlertItem> alerts = new ArrayList<>();

            @Override
            protected Void doInBackground() {
                try {
                    totalCars = safeCount("SELECT COUNT(*) FROM CARS");

                    carsWithDriver = safeCount("SELECT COUNT(*) FROM CARS WHERE DRIVER_ID IS NOT NULL");
                    carsWithLine = safeCount("SELECT COUNT(*) FROM CARS WHERE ITINERARY_ID IS NOT NULL");

                    soonCarsLic = safeCount(
                            "SELECT COUNT(*) FROM CARS WHERE LICENSE_EXPIRY <= TRUNC(SYSDATE)+30"
                    );
                    soonDrvLic = safeCount(
                            "SELECT COUNT(*) FROM DRIVERS WHERE LICENSE_EXPIRY_DT <= TRUNC(SYSDATE)+30"
                    );

                    maintCount30d = safeCount(
                            "SELECT COUNT(*) FROM MAINTENANCE WHERE MAINTENANCE_DATE >= TRUNC(SYSDATE)-30"
                    );

                    Double liters = DB.queryScalar(
                            "SELECT SUM(NUMBER_LITRES) FROM FUEL_RECORD WHERE THE_DATE >= TRUNC(SYSDATE)-6",
                            Double.class
                    );
                    Double cost = DB.queryScalar(
                            "SELECT SUM(COST) FROM FUEL_RECORD WHERE THE_DATE >= TRUNC(SYSDATE)-6",
                            Double.class
                    );

                    fuelLiters7d = liters == null ? 0 : liters;
                    fuelCost7d = cost == null ? 0 : cost;

                    alerts.addAll(DB.query(
                            "SELECT * FROM ( "
                            + "SELECT c.CAR_ID, c.CAR_NAME AS CAR_NAME, "
                            + "       TO_CHAR(c.LICENSE_EXPIRY,'YYYY-MM-DD') AS D, "
                            + "       (TRUNC(c.LICENSE_EXPIRY)-TRUNC(SYSDATE)) AS DAYS_LEFT "
                            + "FROM CARS c "
                            + "WHERE c.LICENSE_EXPIRY <= TRUNC(SYSDATE)+30 "
                            + "ORDER BY c.LICENSE_EXPIRY ASC "
                            + ")",
                            rs -> {
                                int id = rs.getInt(1);
                                String name = rs.getString(2);
                                if (name == null || name.trim().isEmpty()) {
                                    name = "Car #" + id;
                                }
                                String d = rs.getString(3);
                                int days = rs.getInt(4);
                                Severity s = days <= 7 ? Severity.HIGH : Severity.MED;

                                return new AlertItem(
                                        s,
                                        "تراخيص",
                                        "رخصة عربية قريبة الانتهاء: " + name + " (بعد " + days + " يوم)",
                                        d,
                                        "LIC_CAR:" + id
                                );
                            }
                    ));

                    alerts.addAll(DB.query(
                            "SELECT * FROM ( "
                            + "SELECT d.DRIVER_ID, d.DRIVER_NAME, TO_CHAR(d.LICENSE_EXPIRY_DT,'YYYY-MM-DD') AS D, "
                            + "       (TRUNC(d.LICENSE_EXPIRY_DT)-TRUNC(SYSDATE)) AS DAYS_LEFT "
                            + "FROM DRIVERS d "
                            + "WHERE d.LICENSE_EXPIRY_DT <= TRUNC(SYSDATE)+30 "
                            + "ORDER BY d.LICENSE_EXPIRY_DT ASC "
                            + ")",
                            rs -> {
                                int id = rs.getInt(1);
                                String name = rs.getString(2);
                                String d = rs.getString(3);
                                int days = rs.getInt(4);
                                Severity s = days <= 7 ? Severity.HIGH : Severity.MED;

                                return new AlertItem(
                                        s,
                                        "تراخيص",
                                        "رخصة سائق قريبة الانتهاء: " + name + " (بعد " + days + " يوم)",
                                        d,
                                        "LIC_DRV:" + id
                                );
                            }
                    ));

                    if (maintCount30d > 0) {
                        alerts.add(new AlertItem(
                                Severity.LOW,
                                "صيانة",
                                "تم تسجيل " + maintCount30d + " عملية صيانة خلال آخر 30 يوم",
                                todayStr(),
                                "MAINT:30D"
                        ));
                    }

                    if (fuelCost7d > 0 || fuelLiters7d > 0) {
                        alerts.add(new AlertItem(
                                Severity.LOW,
                                "بنزين",
                                "بنزين آخر 7 أيام: " + formatNum(fuelLiters7d) + " لتر | " + formatNum(fuelCost7d) + " جنيه",
                                todayStr(),
                                "FUEL:7D"
                        ));
                    }

                    alerts.sort((a, b) -> Integer.compare(b.severity.rank, a.severity.rank));

                } catch (Exception ignored) {
                }
                return null;
            }

            @Override
            protected void done() {
                if (totalCars <= 0) {
                    totalCars = 1;
                }

                int p1 = (int) Math.round((carsWithDriver * 100.0) / totalCars);
                int p2 = (int) Math.round((carsWithLine * 100.0) / totalCars);

                fhCarsAssignedDrivers.setText("ربط سيارات بسائق: " + carsWithDriver + " / " + totalCars);
                fhCarsAssignedLines.setText("ربط سيارات بخط: " + carsWithLine + " / " + totalCars);

                pbDriversAssigned.setValue(clamp(p1));
                pbLinesAssigned.setValue(clamp(p2));

                int soonTotal = soonCarsLic + soonDrvLic;
                fhLicensesSoon.setText(
                        "تراخيص قريبة الانتهاء (30 يوم): " + soonTotal
                        + " (عربيات: " + soonCarsLic + " | سواقين: " + soonDrvLic + ")"
                );

                int licScore = (int) Math.min(100, (soonTotal * 100.0) / Math.max(1, totalCars));
                pbLicensesSoon.setValue(clamp(licScore));

                fhFuel7d.setText("بنزين آخر 7 أيام: " + formatNum(fuelLiters7d) + " لتر | " + formatNum(fuelCost7d) + " جنيه");
                fhMaint30d.setText("صيانة آخر 30 يوم: " + maintCount30d + " عملية");

                allAlerts = alerts;
                rebuildAlertsTable(alerts);

                if (alertsTable.getRowSorter() instanceof TableRowSorter) {
                    applyAlertsFilter((TableRowSorter<DefaultTableModel>) alertsTable.getRowSorter());
                }
            }
        }.execute();
    }

    private void rebuildAlertsTable(List<AlertItem> list) {
        alertsModel.setRowCount(0);
        for (AlertItem a : list) {
            alertsModel.addRow(new Object[]{
                a.severity.label,
                a.type,
                a.message,
                a.date,
                a.tag
            });
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private static String todayStr() {
        try {
            String d = DB.queryScalar("SELECT TO_CHAR(SYSDATE,'YYYY-MM-DD') FROM dual", String.class);
            return d == null ? "" : d;
        } catch (Exception e) {
            return "";
        }
    }

    private static String formatNum(double v) {
        if (Math.abs(v - Math.round(v)) < 0.0001) {
            return String.valueOf((long) Math.round(v));
        }
        return String.format(java.util.Locale.US, "%.1f", v);
    }

    // ================== Top stats ==================
    private JPanel createStatsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 16, 0));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(new EmptyBorder(0, 0, 16, 0));

        lblLinesCount = new JLabel("...");
        lblCarsCount = new JLabel("...");
        lblDriversCount = new JLabel("...");
        lblEmployeesCount = new JLabel("...");

        statsPanel.add(createStatCard("عدد الخطوط", lblLinesCount, "icons/route1.png"));
        statsPanel.add(createStatCard("السيارات", lblCarsCount, "icons/car.png"));
        statsPanel.add(createStatCard("السائقون", lblDriversCount, "icons/driver.png"));
        statsPanel.add(createStatCard("الموظفون", lblEmployeesCount, "icons/users.png"));

        return statsPanel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, String iconPath) {
        CardPanel card = new CardPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Tahoma", Font.PLAIN, 13));
        titleLabel.setForeground(new Color(75, 85, 99));
        titleLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        valueLabel.setFont(new Font("Tahoma", Font.BOLD, 30));
        valueLabel.setForeground(new Color(15, 23, 42));
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JLabel iconLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(37, 99, 235, 14));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };

        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setPreferredSize(new Dimension(44, 44));

        ImageIcon icon = loadIcon(iconPath, 22);
        if (icon != null) {
            iconLabel.setIcon(icon);
        }

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.add(titleLabel, BorderLayout.NORTH);
        topRow.add(valueLabel, BorderLayout.CENTER);

        card.add(iconLabel, BorderLayout.WEST);
        card.add(topRow, BorderLayout.CENTER);

        return card;
    }

    private void refreshDashboardStatsAsync() {
        lblLinesCount.setText("...");
        lblCarsCount.setText("...");
        lblDriversCount.setText("...");
        lblEmployeesCount.setText("...");

        new SwingWorker<int[], Void>() {
            @Override
            protected int[] doInBackground() {
                int lines = safeCount("SELECT COUNT(*) FROM ITINERARY");
                int cars = safeCount("SELECT COUNT(*) FROM CARS");
                int drivers = safeCount("SELECT COUNT(*) FROM DRIVERS");
                int employees = safeCount("SELECT COUNT(*) FROM EMPLOYEES");
                return new int[]{lines, cars, drivers, employees};
            }

            @Override
            protected void done() {
                try {
                    int[] v = get();
                    lblLinesCount.setText(String.valueOf(v[0]));
                    lblCarsCount.setText(String.valueOf(v[1]));
                    lblDriversCount.setText(String.valueOf(v[2]));
                    lblEmployeesCount.setText(String.valueOf(v[3]));
                } catch (Exception ex) {
                    lblLinesCount.setText("0");
                    lblCarsCount.setText("0");
                    lblDriversCount.setText("0");
                    lblEmployeesCount.setText("0");
                }
            }
        }.execute();
    }

    private int safeCount(String sql) {
        try {
            Integer v = DB.queryScalar(sql, Integer.class);
            return v == null ? 0 : v;
        } catch (Exception ex) {
            return 0;
        }
    }

    // ================== Top bar / Sidebar ==================
    private JPanel createTopBar(Color primaryBlue) {
        JPanel topBar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color c1 = new Color(18, 72, 132);
                Color c2 = new Color(37, 99, 235);
                GradientPaint gp = new GradientPaint(0, 0, c1, getWidth(), 0, c2);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(new Color(255, 255, 255, 35));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

                g2.dispose();
            }
        };

        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(12, 22, 12, 22));

        JLabel title = new JLabel("نظام إدارة حركة سيارات الموظفين");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Tahoma", Font.BOLD, 20));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel logo = new JLabel();
        ImageIcon logoIcon = loadIcon("icons/logo.png", 40);
        if (logoIcon != null) {
            logo.setIcon(logoIcon);
        }
        logo.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        userPanel.setOpaque(false);

        JLabel userIcon = new JLabel();
        ImageIcon userImg = loadIcon("icons/user.png", 34);
        if (userImg != null) {
            userIcon.setIcon(userImg);
        }

        topUserNameLabel = new JLabel(session != null ? session.displayName : "User");
        topUserNameLabel.setForeground(new Color(240, 249, 255));
        topUserNameLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));

        PillButton btnLogout = new PillButton("تسجيل خروج");
        btnLogout.setToolTipText("تسجيل خروج");

        ImageIcon logoutIcon = loadIcon("icons/logout.png", 18);
        if (logoutIcon != null) {
            btnLogout.setIcon(logoutIcon);
            btnLogout.setIconTextGap(6);
            btnLogout.setHorizontalTextPosition(SwingConstants.LEFT);
        }

        btnLogout.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new Login().setVisible(true));
        });

        userPanel.add(userIcon);
        userPanel.add(topUserNameLabel);
        userPanel.add(btnLogout);

        topBar.add(logo, BorderLayout.EAST);
        topBar.add(title, BorderLayout.CENTER);
        topBar.add(userPanel, BorderLayout.WEST);

        return topBar;
    }

    private JPanel createSidebar(Color primaryBlue) {
        JPanel sideBar = new JPanel();
        sideBar.setBackground(primaryBlue.darker());
        sideBar.setPreferredSize(new Dimension(220, 0));
        sideBar.setLayout(new BoxLayout(sideBar, BoxLayout.Y_AXIS));
        sideBar.setBorder(new EmptyBorder(20, 10, 20, 10));

        JLabel menuTitle = new JLabel("القائمة");
        menuTitle.setForeground(Color.WHITE);
        menuTitle.setFont(new Font("Tahoma", Font.BOLD, 25));
        menuTitle.setAlignmentX(Component.RIGHT_ALIGNMENT);
        sideBar.add(menuTitle);
        sideBar.add(Box.createVerticalStrut(20));

        homeBtn = createSidebarButton("الرئيسية", "icons/home.png", true);
        homeBtn.addActionListener(e -> showDashboardScreen());
        sideBar.add(homeBtn);

        employeesBtn = createSidebarButton("الموظفون", "icons/users.png", false);
        employeesBtn.addActionListener(e -> showEmployeesScreen());
        sideBar.add(employeesBtn);

        driversBtn = createSidebarButton("السائقون", "icons/driver.png", false);
        driversBtn.addActionListener(e -> showDriversScreen());
        sideBar.add(driversBtn);

        carsBtn = createSidebarButton("السيارات", "icons/car.png", false);
        carsBtn.addActionListener(e -> showVehiclesScreen());
        sideBar.add(carsBtn);

        linesBtn = createSidebarButton("الخطوط", "icons/route.png", false);
        linesBtn.addActionListener(e -> showLinesScreen());
        sideBar.add(linesBtn);

        maintBtn = createSidebarButton("الصيانة", "icons/tools.png", false);
        maintBtn.addActionListener(e -> showMaintenanceScreen());
        sideBar.add(maintBtn);

        fuelBtn = createSidebarButton("البنزين", "icons/benzene.png", false);
        fuelBtn.addActionListener(e -> showFuelScreen());
        sideBar.add(fuelBtn);

        sideBar.add(Box.createVerticalGlue());

        if (session != null && session.canOpenSettings()) {
            settingsBtn = createSidebarButton("الإعدادات", "icons/setting.png", false);
            settingsBtn.addActionListener(e -> showSettingsScreen());
            sideBar.add(settingsBtn);
        }

        return sideBar;
    }

    private SidebarButton createSidebarButton(String text, String iconPath, boolean selected) {
        SidebarButton button = new SidebarButton(text);
        button.setHorizontalAlignment(SwingConstants.RIGHT);
        button.setFont(new Font("Tahoma", Font.PLAIN, 16));
        button.setForeground(Color.WHITE);
        button.setMinimumSize(new Dimension(Integer.MIN_VALUE, 100));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 700));
        button.setAlignmentX(Component.RIGHT_ALIGNMENT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setIconTextGap(10);

        ImageIcon icon = loadIcon(iconPath, 20);
        if (icon != null) {
            button.setIcon(icon);
        }

        button.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        button.setHorizontalTextPosition(SwingConstants.LEFT);

        if (selected) {
            setActiveSidebarButton(button);
        }

        return button;
    }

    private void setActiveSidebarButton(SidebarButton btn) {
        if (selectedSidebarButton != null) {
            selectedSidebarButton.setActive(false);
        }
        selectedSidebarButton = btn;
        if (btn != null) {
            btn.setActive(true);
        }
    }

    // ================== Show methods ==================
    private void showDashboardScreen() {
        refreshDashboardStatsAsync();
        refreshDashboardBodyAsync();
        contentLayout.show(contentPanel, "DASHBOARD");
        setActiveSidebarButton(homeBtn);
    }

    private void showVehiclesScreen() {
        if (vehiclesPanel != null) {
            try {
                vehiclesPanel.refreshAll();
            } catch (Exception ignored) {
            }
        }
        contentLayout.show(contentPanel, "VEHICLES");
        setActiveSidebarButton(carsBtn);
    }

    private void showDriversScreen() {
        if (driversPanel != null) {
            try {
                driversPanel.refreshAll();
            } catch (Exception ignored) {
            }
        }
        contentLayout.show(contentPanel, "DRIVERS");
        setActiveSidebarButton(driversBtn);
    }

    private void showEmployeesScreen() {
        if (employeesPanel != null) {
            try {
                employeesPanel.refreshAll();
            } catch (Exception ignored) {
            }
        }
        contentLayout.show(contentPanel, "EMPLOYEES");
        setActiveSidebarButton(employeesBtn);
    }

    private void showLinesScreen() {
        if (linesConfigPanel != null) {
            try {
                linesConfigPanel.refreshAll();
            } catch (Exception ignored) {
            }
        }
        contentLayout.show(contentPanel, "LINES");
        setActiveSidebarButton(linesBtn);
    }

    private void showMaintenanceScreen() {
        if (maintenancePanel != null) {
            try {
                maintenancePanel.refreshAll();
            } catch (Exception ignored) {
            }
        }
        contentLayout.show(contentPanel, "MAINTENANCE");
        setActiveSidebarButton(maintBtn);
    }

    private void showFuelScreen() {
        if (fuelPanel != null) {
            try {
                fuelPanel.refreshAll();
            } catch (Exception ignored) {
            }
        }
        contentLayout.show(contentPanel, "FUEL");
        setActiveSidebarButton(fuelBtn);
    }

    private void showSettingsScreen() {
        if (settingsPanel != null) {
            settingsPanel.refreshAll();
        }
        contentLayout.show(contentPanel, "SETTINGS");
        setActiveSidebarButton(settingsBtn);
    }

    // ================== Helpers / UI ==================
    private void applyRightToLeft(Container container) {
        container.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }

    private ImageIcon loadIcon(String path, int size) {
        ImageIcon icon = new ImageIcon(path);
        if (icon.getIconWidth() <= 0) {
            return null;
        }
        Image img = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    // ===== Card panel =====
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

    // ===== Sidebar button =====
    private static class SidebarButton extends JButton {

        private boolean hover = false;
        private boolean active = false;

        private final Color baseBg = new Color(15, 58, 110);
        private final Color hoverBg = new Color(25, 78, 140);
        private final Color activeBg = new Color(37, 99, 235);
        private final int arc = 18;
        private final int indicatorW = 4;

        public SidebarButton(String text) {
            super(text);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        public void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            Color bg = baseBg;
            if (active) {
                bg = activeBg;
            } else if (hover) {
                bg = hoverBg;
            }

            g2.setColor(bg);
            g2.fillRoundRect(6, 2, w - 12, h - 4, arc, arc);

            if (active) {
                g2.setColor(new Color(191, 219, 254));
                g2.fillRoundRect(w - indicatorW - 2, 4, indicatorW, h - 8, 8, 8);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ===== TopBar pill button =====
    private static class PillButton extends JButton {

        private boolean hover = false;
        private final int arc = 16;

        PillButton(String text) {
            super(text);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setFont(new Font("Tahoma", Font.PLAIN, 13));
            setForeground(Color.WHITE);
            setMargin(new Insets(6, 10, 6, 10));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int alpha = hover ? 70 : 45;
            g2.setColor(new Color(255, 255, 255, alpha));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

            g2.setColor(new Color(255, 255, 255, 120));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ===== Alerts model =====
    private enum Severity {
        LOW("منخفض", 1),
        MED("متوسط", 2),
        HIGH("عالي", 3);

        final String label;
        final int rank;

        Severity(String label, int rank) {
            this.label = label;
            this.rank = rank;
        }
    }

    private static class AlertItem {

        final Severity severity;
        final String type;
        final String message;
        final String date;
        final String tag;

        AlertItem(Severity severity, String type, String message, String date, String tag) {
            this.severity = severity;
            this.type = type;
            this.message = message;
            this.date = date == null ? "" : date;
            this.tag = tag == null ? "" : tag;
        }
    }

    // ===== Severity badge renderer =====
    private static class SeverityBadgeRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String s = value == null ? "" : value.toString();
            setHorizontalAlignment(SwingConstants.CENTER);

            if (isSelected) {
                return this;
            }

            Color bg = new Color(241, 245, 249);
            Color fg = new Color(51, 65, 85);

            if ("عالي".equals(s)) {
                bg = new Color(254, 226, 226);
                fg = new Color(185, 28, 28);
            } else if ("متوسط".equals(s)) {
                bg = new Color(254, 243, 199);
                fg = new Color(180, 83, 9);
            } else if ("منخفض".equals(s)) {
                bg = new Color(219, 234, 254);
                fg = new Color(29, 78, 216);
            }

            setBackground(bg);
            setForeground(fg);
            return this;
        }
    }

    // JTableHeader custom
    private static class JTableHeader extends javax.swing.table.JTableHeader {

        public JTableHeader(javax.swing.table.TableColumnModel cm) {
            super(cm);
        }
    }

    // ================== main ==================
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new Login().setVisible(true));
    }
}
