
import DB.DB;
import com.ibm.icu.text.ArabicShaping;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import com.ibm.icu.text.Bidi;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * TransportDashboardPdfReportService
 *
 * Powerful PDF backend for the Transport Dashboard.
 *
 * Notes: 1) This class uses Apache PDFBox. 2) Best result: place a Unicode TTF
 * font inside: resources/fonts/NotoSans-Regular.ttf
 * resources/fonts/NotoSans-Bold.ttf or load any TTF from your machine. 3) The
 * SQL here is based on tables/columns already used by your dashboard:
 * ITINERARY, CARS, DRIVERS, EMPLOYEES, MAINTENANCE, FUEL_RECORD LICENSE_EXPIRY,
 * LICENSE_EXPIRY_DT, NUMBER_LITRES, COST
 *
 * Maven:
 * <dependency>
 * <groupId>org.apache.pdfbox</groupId>
 * <artifactId>pdfbox</artifactId>
 * <version>2.0.33</version>
 * </dependency>
 */
public final class TransportDashboardPdfReportService {

    private TransportDashboardPdfReportService() {
    }

    // ===== Theme =====
    private static final Color PAGE_BG = new Color(247, 250, 252);
    private static final Color INK = new Color(17, 24, 39);
    private static final Color MUTED = new Color(107, 114, 128);
    private static final Color PRIMARY = new Color(20, 79, 163);
    private static final Color PRIMARY_2 = new Color(38, 132, 255);
    private static final Color SOFT_BLUE = new Color(232, 242, 255);
    private static final Color BORDER = new Color(225, 231, 237);
    private static final Color SUCCESS = new Color(17, 138, 89);
    private static final Color WARNING = new Color(217, 119, 6);
    private static final Color DANGER = new Color(220, 38, 38);
    private static final Color CARD_BG = Color.WHITE;
    private static final DecimalFormat DF = new DecimalFormat("#,##0.0");

    private static final float MARGIN = 34f;
    private static final float FOOTER_H = 20f;
    private static final float HEADER_H = 76f;

    public static File exportDashboardReport(String outputFilePath, String generatedBy) throws Exception {
        return exportDashboardReport(Path.of(outputFilePath), generatedBy);
    }

    public static File exportDashboardReport(Path outputPath, String generatedBy) throws Exception {
        ReportData data = loadReportData();

        try (PDDocument doc = new PDDocument()) {
//            PDFont regular = tryLoadFont(doc,
//                    new File("C:/Windows/Fonts/arial.ttf"),
//                    new File("C:/Windows/Fonts/tahoma.ttf"),
//                    new File("fonts/NotoSans-Regular.ttf")
//            );
//            PDFont bold = tryLoadFont(doc,
//                    new File("C:/Windows/Fonts/arialbd.ttf"),
//                    new File("C:/Windows/Fonts/tahomabd.ttf"),
//                    new File("fonts/NotoSans-Bold.ttf")
//            );

            PDFont regular = PDType0Font.load(doc, new File("C:/Windows/Fonts/arial.ttf"));
            PDFont bold = PDType0Font.load(doc, new File("C:/Windows/Fonts/arialbd.ttf"));
            if (regular == null) {
                regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            }
            if (bold == null) {
                bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            }

            PDDocumentInformation info = doc.getDocumentInformation();
            info.setTitle("تقرير لوحة تحكم النقل");
            info.setAuthor(generatedBy == null || generatedBy.isBlank() ? "النظام" : generatedBy);
            info.setSubject("تقرير تنفيذي للسيارات والحركة");
            info.setCreator("خدمة تقارير لوحة التحكم");

            PdfPainter painter = new PdfPainter(doc, regular, bold, generatedBy, data.generatedAt);
            painter.addExecutivePage(data);
            painter.addVehiclesAssignmentPage(data);
            painter.addAlertsPage(data);

            File out = outputPath.toFile();
            File parent = out.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            doc.save(out);
            return out;
        }
    }

    // =========================================================================================
    // Data loading
    // =========================================================================================
    private static ReportData loadReportData() {
        ReportData data = new ReportData();
        data.generatedAt = LocalDateTime.now();

        data.totalLines = safeCount("SELECT COUNT(*) FROM ITINERARY");
        data.totalCars = safeCount("SELECT COUNT(*) FROM CARS");
        data.totalDrivers = safeCount("SELECT COUNT(*) FROM DRIVERS");
        data.totalEmployees = safeCount("SELECT COUNT(*) FROM EMPLOYEES");

        data.carsWithDriver = safeCount("SELECT COUNT(*) FROM CARS WHERE DRIVER_ID IS NOT NULL");
        data.carsWithLine = safeCount("SELECT COUNT(*) FROM CARS WHERE ITINERARY_ID IS NOT NULL");

        data.soonCarsLic = safeCount("SELECT COUNT(*) FROM CARS WHERE LICENSE_EXPIRY <= TRUNC(SYSDATE)+30");
        data.soonDrvLic = safeCount("SELECT COUNT(*) FROM DRIVERS WHERE LICENSE_EXPIRY_DT <= TRUNC(SYSDATE)+30");

        data.maintenance30d = safeCount("SELECT COUNT(*) FROM MAINTENANCE WHERE MAINTENANCE_DATE >= TRUNC(SYSDATE)-30");

        Double fuelLiters = safeDouble("SELECT SUM(NUMBER_LITRES) FROM FUEL_RECORD WHERE THE_DATE >= TRUNC(SYSDATE)-6");
        Double fuelCost = safeDouble("SELECT SUM(COST) FROM FUEL_RECORD WHERE THE_DATE >= TRUNC(SYSDATE)-6");
        data.fuelLiters7d = fuelLiters == null ? 0 : fuelLiters;
        data.fuelCost7d = fuelCost == null ? 0 : fuelCost;

        data.vehicleRows = loadVehicleRows();
        data.alertRows = loadAlertRows(data);

        return data;
    }

    private static int safeCount(String sql) {
        try {
            Integer v = DB.queryScalar(sql, Integer.class);
            return v == null ? 0 : v;
        } catch (Exception ex) {
            return 0;
        }
    }

    private static Double safeDouble(String sql) {
        try {
            return DB.queryScalar(sql, Double.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String safeString(String sql) {
        try {
            String v = DB.queryScalar(sql, String.class);
            return v == null ? "" : v;
        } catch (Exception ex) {
            return "";
        }
    }

    private static List<VehicleAssignmentRow> loadVehicleRows() {
        List<VehicleAssignmentRow> rows = new ArrayList<>();

        String sql
                = "SELECT "
                + "   c.CAR_ID, "
                + "   NVL(c.CAR_NAME, 'Car #' || c.CAR_ID) AS CAR_NAME, "
                + "   NVL(d.DRIVER_NAME, 'Unassigned') AS DRIVER_NAME, "
                + "   NVL(TO_CHAR(c.ITINERARY_ID), 'Unassigned') AS LINE_LABEL, "
                + "   NVL(TO_CHAR(c.LICENSE_EXPIRY, 'YYYY-MM-DD'), '-') AS LICENSE_EXPIRY "
                + "FROM CARS c "
                + "LEFT JOIN DRIVERS d ON d.DRIVER_ID = c.DRIVER_ID "
                + "ORDER BY c.CAR_ID";

        try {
            rows = DB.query(sql, rs -> {
                VehicleAssignmentRow row = new VehicleAssignmentRow();
                row.carId = rs.getInt("CAR_ID");
                row.carName = safeText(rs.getString("CAR_NAME"));
                row.driverName = safeText(rs.getString("DRIVER_NAME"));
                row.lineLabel = safeText(rs.getString("LINE_LABEL"));
                row.licenseExpiry = safeText(rs.getString("LICENSE_EXPIRY"));
                return row;
            });
        } catch (Exception ex) {
            // fallback in case one of the columns / join assumptions differs
            try {
                rows = DB.query(
                        "SELECT CAR_ID, NVL(CAR_NAME, 'Car #' || CAR_ID) AS CAR_NAME, "
                        + "NVL(TO_CHAR(DRIVER_ID), 'Unassigned') AS DRIVER_NAME, "
                        + "NVL(TO_CHAR(ITINERARY_ID), 'Unassigned') AS LINE_LABEL, "
                        + "NVL(TO_CHAR(LICENSE_EXPIRY, 'YYYY-MM-DD'), '-') AS LICENSE_EXPIRY "
                        + "FROM CARS ORDER BY CAR_ID",
                        rs -> {
                            VehicleAssignmentRow row = new VehicleAssignmentRow();
                            row.carId = rs.getInt("CAR_ID");
                            row.carName = safeText(rs.getString("CAR_NAME"));
                            row.driverName = safeText(rs.getString("DRIVER_NAME"));
                            row.lineLabel = safeText(rs.getString("LINE_LABEL"));
                            row.licenseExpiry = safeText(rs.getString("LICENSE_EXPIRY"));
                            return row;
                        }
                );
            } catch (Exception ignored) {
            }
        }

        return rows;
    }

    private static List<AlertRow> loadAlertRows(ReportData data) {
        List<AlertRow> rows = new ArrayList<>();

        try {
            rows.addAll(DB.query(
                    "SELECT * FROM ( "
                    + "   SELECT NVL(c.CAR_NAME, 'Car #' || c.CAR_ID) AS ITEM_NAME, "
                    + "          TO_CHAR(c.LICENSE_EXPIRY, 'YYYY-MM-DD') AS D, "
                    + "          (TRUNC(c.LICENSE_EXPIRY)-TRUNC(SYSDATE)) AS DAYS_LEFT "
                    + "   FROM CARS c "
                    + "   WHERE c.LICENSE_EXPIRY <= TRUNC(SYSDATE)+30 "
                    + "   ORDER BY c.LICENSE_EXPIRY ASC "
                    + ")",
                    rs -> {
                        int days = rs.getInt("DAYS_LEFT");
                        AlertRow row = new AlertRow();
                        row.priority = days <= 7 ? "عالي" : "متوسط";
                        row.type = "الترخيص";
                        row.message = "ترخيص السيارة أوشك على الانتهاء: " + safeText(rs.getString("ITEM_NAME")) + " (بعد " + days + " يوم)";
                        row.date = safeText(rs.getString("D"));
                        return row;
                    }
            ));
        } catch (Exception ignored) {
        }

        try {
            rows.addAll(DB.query(
                    "SELECT * FROM ( "
                    + "   SELECT NVL(d.DRIVER_NAME, 'Driver #' || d.DRIVER_ID) AS ITEM_NAME, "
                    + "          TO_CHAR(d.LICENSE_EXPIRY_DT, 'YYYY-MM-DD') AS D, "
                    + "          (TRUNC(d.LICENSE_EXPIRY_DT)-TRUNC(SYSDATE)) AS DAYS_LEFT "
                    + "   FROM DRIVERS d "
                    + "   WHERE d.LICENSE_EXPIRY_DT <= TRUNC(SYSDATE)+30 "
                    + "   ORDER BY d.LICENSE_EXPIRY_DT ASC "
                    + ")",
                    rs -> {
                        int days = rs.getInt("DAYS_LEFT");
                        AlertRow row = new AlertRow();
                        row.priority = days <= 7 ? "عالي" : "متوسط";
                        row.type = "الترخيص";
                        row.message = "رخصة السائق أوشكت على الانتهاء: " + safeText(rs.getString("ITEM_NAME")) + " (بعد " + days + " يوم)";
                        row.date = safeText(rs.getString("D"));
                        return row;
                    }
            ));
        } catch (Exception ignored) {
        }

        if (data.maintenance30d > 0) {
            AlertRow row = new AlertRow();
            row.priority = "منخفض";
            row.type = "الصيانة";
            row.message = "عمليات الصيانة خلال آخر 30 يوم: " + data.maintenance30d;
            row.date = data.generatedAt.toLocalDate().toString();
            rows.add(row);
        }

        if (data.fuelLiters7d > 0 || data.fuelCost7d > 0) {
            AlertRow row = new AlertRow();
            row.priority = "منخفض";
            row.type = "البنزين";
            row.message = "البنزين خلال آخر 7 أيام: " + fmt(data.fuelLiters7d) + " لتر | " + fmt(data.fuelCost7d) + " جنيه";
            row.date = data.generatedAt.toLocalDate().toString();
            rows.add(row);
        }

        rows.sort((a, b) -> rank(b.priority) - rank(a.priority));
        return rows;
    }

    private static int rank(String p) {
        if ("عالي".equalsIgnoreCase(p)) {
            return 3;
        }
        if ("متوسط".equalsIgnoreCase(p)) {
            return 2;
        }
        return 1;
    }

    private static String safeText(String s) {
        return s == null ? "" : s.trim();
    }

    private static String fmt(double v) {
        if (Math.abs(v - Math.round(v)) < 0.0001) {
            return String.valueOf((long) Math.round(v));
        }
        return DF.format(v);
    }

    // =========================================================================================
    // PDF painter
    // =========================================================================================
    private static final class PdfPainter {

        private final PDDocument doc;
        private final PDFont regular;
        private final PDFont bold;
        private final String generatedBy;
        private final LocalDateTime generatedAt;
        private int pageNo = 0;

        private PDPage page;
        private PDPageContentStream cs;
        private float y;

        PdfPainter(PDDocument doc, PDFont regular, PDFont bold, String generatedBy, LocalDateTime generatedAt) {
            this.doc = doc;
            this.regular = regular;
            this.bold = bold;
            this.generatedBy = generatedBy == null || generatedBy.isBlank() ? "System" : generatedBy;
            this.generatedAt = generatedAt;
        }

        void addExecutivePage(ReportData data) throws IOException {
            newPage("التقرير التنفيذي للوحة التحكم", "نظرة عامة سريعة تدعم اتخاذ القرار");

            drawStatGrid(data);
            y -= 8;
            drawSectionTitle("الحالة التشغيلية");
            drawHealthCards(data);
            y -= 6;
            drawSectionTitle("أهم المؤشرات");
            drawBullet(
                    "السيارات المرتبطة بسائق: " + data.carsWithDriver + " / " + data.totalCars
                    + " | السيارات المرتبطة بخط: " + data.carsWithLine + " / " + data.totalCars
            );
            drawBullet(
                    "التراخيص القريبة من الانتهاء خلال 30 يوم: السيارات " + data.soonCarsLic + "، السائقون " + data.soonDrvLic
            );
            drawBullet(
                    "البنزين خلال آخر 7 أيام: " + fmt(data.fuelLiters7d) + " لتر، التكلفة " + fmt(data.fuelCost7d) + " جنيه"
            );
            drawBullet(
                    "عمليات الصيانة خلال آخر 30 يوم: " + data.maintenance30d
            );

            finishPage();
        }

        void addVehiclesAssignmentPage(ReportData data) throws IOException {
            newPage("مصفوفة توزيع السيارات", "السيارات والسائقون والخطوط وتواريخ انتهاء التراخيص");

            drawSectionTitle("جدول التوزيع");
            List<String> headers = List.of("كود السيارة", "اسم السيارة", "السائق", "الخط", "انتهاء الترخيص");
            List<float[]> colors = null;

            List<List<String>> rows = new ArrayList<>();
            for (VehicleAssignmentRow r : data.vehicleRows) {
                rows.add(List.of(
                        String.valueOf(r.carId),
                        safeCell(r.carName),
                        safeCell(r.driverName),
                        safeCell(r.lineLabel),
                        safeCell(r.licenseExpiry)
                ));
            }

            float[] widths = new float[]{56, 160, 150, 90, 110};
            drawTable(headers, rows, widths, colors, 28);

            finishPage();
        }

        void addAlertsPage(ReportData data) throws IOException {
            newPage("التنبيهات والاستثناءات", "مراجعة حسب الأولوية لاتخاذ الإجراء");

            drawSectionTitle("التنبيهات");
            List<String> headers = List.of("الأولوية", "النوع", "الوصف", "التاريخ");
            List<List<String>> rows = new ArrayList<>();
            List<float[]> rowBg = new ArrayList<>();

            for (AlertRow r : data.alertRows) {
                rows.add(List.of(
                        safeCell(r.priority),
                        safeCell(r.type),
                        safeCell(r.message),
                        safeCell(r.date)
                ));
                rowBg.add(priorityColor(r.priority));
            }

            float[] widths = new float[]{70, 90, 300, 90};
            drawTable(headers, rows, widths, rowBg, 24);

            if (data.alertRows.isEmpty()) {
                y -= 10;
                drawMuted("لا توجد تنبيهات نشطة حاليًا.");
            }

            finishPage();
        }

        private void drawStatGrid(ReportData data) throws IOException {
            float gap = 12f;
            float cardW = (usableWidth() - gap) / 2f;
            float cardH = 72f;

            float xLeft = MARGIN;
            float xRight = MARGIN + cardW + gap;

            drawStatCard(xLeft, y - cardH, cardW, cardH, "الخطوط", String.valueOf(data.totalLines), PRIMARY);
            drawStatCard(xRight, y - cardH, cardW, cardH, "السيارات", String.valueOf(data.totalCars), SUCCESS);
            y -= (cardH + gap);

            drawStatCard(xLeft, y - cardH, cardW, cardH, "السائقون", String.valueOf(data.totalDrivers), WARNING);
            drawStatCard(xRight, y - cardH, cardW, cardH, "الموظفون", String.valueOf(data.totalEmployees), PRIMARY_2);
            y -= (cardH + 10);
        }

        private void drawHealthCards(ReportData data) throws IOException {
            float gap = 12f;
            float cardW = (usableWidth() - gap * 2) / 3f;
            float cardH = 102f;

            float x1 = MARGIN;
            float x2 = x1 + cardW + gap;
            float x3 = x2 + cardW + gap;

            int totalCarsSafe = Math.max(1, data.totalCars);
            double pctDrivers = (data.carsWithDriver * 100.0) / totalCarsSafe;
            double pctLines = (data.carsWithLine * 100.0) / totalCarsSafe;
            double pctLic = (Math.min(100, ((data.soonCarsLic + data.soonDrvLic) * 100.0) / totalCarsSafe));

            drawHealthCard(
                    x1, y - cardH, cardW, cardH,
                    "الربط والتوزيع",
                    "السيارات المرتبطة بسائق: " + data.carsWithDriver + "/" + data.totalCars,
                    "السيارات المرتبطة بخط: " + data.carsWithLine + "/" + data.totalCars,
                    pctDrivers, pctLines,
                    PRIMARY, PRIMARY_2
            );

            drawHealthCard(
                    x2, y - cardH, cardW, cardH,
                    "الالتزام والتراخيص",
                    "تراخيص السيارات القريبة: " + data.soonCarsLic,
                    "رخص السائقين القريبة: " + data.soonDrvLic,
                    pctLic, 100 - pctLic,
                    WARNING, DANGER
            );

            drawMiniMetricCard(
                    x3, y - cardH, cardW, cardH,
                    "النشاط",
                    "بنزين 7 أيام",
                    fmt(data.fuelLiters7d) + " لتر | " + fmt(data.fuelCost7d) + " جنيه",
                    "صيانة 30 يوم",
                    String.valueOf(data.maintenance30d)
            );

            y -= (cardH + 12);
        }

        private void drawTable(List<String> headers,
                List<List<String>> rows,
                float[] widths,
                List<float[]> rowBgColors,
                float rowH) throws IOException {

            float tableX = MARGIN;
            float tableW = sum(widths);
            float headerH = 28f;

            ensureSpace(headerH + rowH + 40);

            // header
            drawRoundedRect(tableX, y - headerH, tableW, headerH, 8, new Color(238, 244, 251), BORDER);
            float cx = tableX;
            for (int i = 0; i < headers.size(); i++) {
                drawText(headers.get(i), bold, 10, INK, cx + 6, y - 18);
                cx += widths[i];
                if (i < headers.size() - 1) {
                    drawLine(cx, y - headerH, cx, y, BORDER, 0.8f);
                }
            }
            y -= headerH;

            for (int r = 0; r < rows.size(); r++) {
                if (y - rowH < MARGIN + FOOTER_H + 14) {
                    finishPage();
                    newPage("التنبيهات والاستثناءات", "تكملة");
                    drawRoundedRect(tableX, y - headerH, tableW, headerH, 8, new Color(238, 244, 251), BORDER);

                    cx = tableX;
                    for (int i = 0; i < headers.size(); i++) {
                        drawText(headers.get(i), bold, 10, INK, cx + 6, y - 18);
                        cx += widths[i];
                        if (i < headers.size() - 1) {
                            drawLine(cx, y - headerH, cx, y, BORDER, 0.8f);
                        }
                    }
                    y -= headerH;
                }

                Color bg = Color.WHITE;
                if (rowBgColors != null && r < rowBgColors.size() && rowBgColors.get(r) != null) {
                    float[] c = rowBgColors.get(r);
                    bg = new Color((int) c[0], (int) c[1], (int) c[2], (int) c[3]);
                } else if (r % 2 == 1) {
                    bg = new Color(250, 252, 255);
                }

                drawRoundedRect(tableX, y - rowH, tableW, rowH, 6, bg, BORDER);

                cx = tableX;
                List<String> row = rows.get(r);

                for (int i = 0; i < row.size(); i++) {
                    String cell = row.get(i);
                    Color textColor = INK;
                    if (i == 0 && headers.get(0).equalsIgnoreCase("الأولوية")) {
                        textColor = priorityTextColor(cell);
                    }

                    drawFittedText(cell, regular, 9.5f, textColor, cx + 6, y - 16, widths[i] - 12);

                    cx += widths[i];
                    if (i < row.size() - 1) {
                        drawLine(cx, y - rowH, cx, y, BORDER, 0.5f);
                    }
                }

                y -= rowH;
            }
        }

        private void newPage(String title, String subtitle) throws IOException {
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            pageNo++;
            cs = new PDPageContentStream(doc, page);
            y = page.getMediaBox().getHeight() - MARGIN;

            drawPageBackground();
            drawTopBand(title, subtitle);
            y = page.getMediaBox().getHeight() - MARGIN - HEADER_H;
        }

        private void finishPage() throws IOException {
            drawFooter();
            cs.close();
        }

        private void ensureSpace(float needed) throws IOException {
            if (y - needed < MARGIN + FOOTER_H) {
                finishPage();
                newPage("تقرير لوحة التحكم", "تكملة");
            }
        }

        private void drawPageBackground() throws IOException {
            cs.setNonStrokingColor(PAGE_BG);
            cs.addRect(0, 0, page.getMediaBox().getWidth(), page.getMediaBox().getHeight());
            cs.fill();
        }

        private void drawTopBand(String title, String subtitle) throws IOException {
            float w = page.getMediaBox().getWidth() - MARGIN * 2;
            float h = 58f;
            float x = MARGIN;
            float yy = page.getMediaBox().getHeight() - MARGIN - h;

            drawGradientLikeBand(x, yy, w, h);

            drawText(title, bold, 18, Color.WHITE, x + 18, yy + 36);
            drawText(subtitle, regular, 10, new Color(225, 238, 255), x + 18, yy + 20);

            drawText("تم الإنشاء بواسطة: " + generatedBy, regular, 9, new Color(225, 238, 255), x + w - 190, yy + 36);
            drawText("تاريخ الإنشاء: " + generatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), regular, 9,
                    new Color(225, 238, 255), x + w - 210, yy + 20);
        }

        private void drawGradientLikeBand(float x, float y, float w, float h) throws IOException {
            // PDFBox doesn't do easy gradients in a tiny helper like this, so we fake it with strips.
            int strips = 32;
            for (int i = 0; i < strips; i++) {
                float ratio = i / (float) (strips - 1);
                int r = (int) (PRIMARY.getRed() * (1 - ratio) + PRIMARY_2.getRed() * ratio);
                int g = (int) (PRIMARY.getGreen() * (1 - ratio) + PRIMARY_2.getGreen() * ratio);
                int b = (int) (PRIMARY.getBlue() * (1 - ratio) + PRIMARY_2.getBlue() * ratio);
                cs.setNonStrokingColor(new Color(r, g, b));
                float sw = w / strips;
                cs.addRect(x + i * sw, y, sw + 1, h);
                cs.fill();
            }

            cs.setStrokingColor(new Color(255, 255, 255, 40));
            cs.addRect(x, y, w, h);
            cs.stroke();
        }

        private void drawFooter() throws IOException {
            String left = "نظام إدارة حركة السيارات";
            String right = "صفحة " + pageNo;

            drawText(left, regular, 8, MUTED, MARGIN, 16);
            drawText(right, regular, 8, MUTED, page.getMediaBox().getWidth() - MARGIN - 28, 16);

            drawLine(MARGIN, 24, page.getMediaBox().getWidth() - MARGIN, 24, BORDER, 0.8f);
        }

        private void drawSectionTitle(String text) throws IOException {
            ensureSpace(30);
            drawText(text, bold, 13, INK, MARGIN, y - 4);
            drawLine(MARGIN, y - 10, MARGIN + usableWidth(), y - 10, BORDER, 0.8f);
            y -= 22;
        }

        private void drawBullet(String text) throws IOException {
            ensureSpace(18);
            cs.setNonStrokingColor(PRIMARY);
            cs.addRect(MARGIN, y - 10, 5, 5);
            cs.fill();
            drawText(text, regular, 11, INK, MARGIN + 12, y - 9);
            y -= 18;
        }

        private void drawMuted(String text) throws IOException {
            ensureSpace(16);
            drawText(text, regular, 10, MUTED, MARGIN, y - 8);
            y -= 16;
        }

        private void drawStatCard(float x, float y, float w, float h, String label, String value, Color accent) throws IOException {
            drawShadowCard(x, y, w, h);
            drawRoundedRect(x, y, w, h, 12, CARD_BG, BORDER);

            cs.setNonStrokingColor(accent);
            cs.addRect(x, y + h - 4, w, 4);
            cs.fill();

            drawText(label, regular, 10, MUTED, x + 12, y + h - 18);
            drawText(value, bold, 22, INK, x + 12, y + 22);
        }

        private void drawHealthCard(float x, float y, float w, float h,
                String title,
                String line1, String line2,
                double p1, double p2,
                Color c1, Color c2) throws IOException {
            drawShadowCard(x, y, w, h);
            drawRoundedRect(x, y, w, h, 12, CARD_BG, BORDER);

            drawText(title, bold, 11, INK, x + 12, y + h - 16);
            drawText(line1, regular, 9.5f, INK, x + 12, y + h - 32);
            drawProgress(x + 12, y + h - 42, w - 24, 7, p1, c1);

            drawText(line2, regular, 9.5f, INK, x + 12, y + h - 58);
            drawProgress(x + 12, y + h - 68, w - 24, 7, p2, c2);
        }

        private void drawMiniMetricCard(float x, float y, float w, float h,
                String title,
                String k1, String v1,
                String k2, String v2) throws IOException {
            drawShadowCard(x, y, w, h);
            drawRoundedRect(x, y, w, h, 12, CARD_BG, BORDER);

            drawText(title, bold, 11, INK, x + 12, y + h - 16);

            drawRoundedRect(x + 12, y + h - 48, w - 24, 22, 8, SOFT_BLUE, null);
            drawText(k1 + ": " + v1, regular, 9.5f, INK, x + 18, y + h - 35);

            drawRoundedRect(x + 12, y + h - 78, w - 24, 22, 8, new Color(240, 253, 244), null);
            drawText(k2 + ": " + v2, regular, 9.5f, INK, x + 18, y + h - 65);
        }

        private void drawProgress(float x, float y, float w, float h, double percent, Color fill) throws IOException {
            drawRoundedRect(x, y, w, h, 4, new Color(229, 231, 235), null);
            float fw = (float) Math.max(0, Math.min(w, (percent / 100.0) * w));
            drawRoundedRect(x, y, fw, h, 4, fill, null);
        }

        private void drawShadowCard(float x, float y, float w, float h) throws IOException {
            cs.setNonStrokingColor(new Color(15, 23, 42, 18));
            cs.addRect(x + 2, y - 2, w, h);
            cs.fill();
        }

        private void drawRoundedRect(float x, float y, float w, float h, float r, Color fill, Color stroke) throws IOException {
            // simple rounded-ish box using normal rect for stability
            if (fill != null) {
                cs.setNonStrokingColor(fill);
                cs.addRect(x, y, w, h);
                cs.fill();
            }
            if (stroke != null) {
                cs.setStrokingColor(stroke);
                cs.addRect(x, y, w, h);
                cs.stroke();
            }
        }

        private void drawLine(float x1, float y1, float x2, float y2, Color color, float width) throws IOException {
            cs.setStrokingColor(color);
            cs.setLineWidth(width);
            cs.moveTo(x1, y1);
            cs.lineTo(x2, y2);
            cs.stroke();
        }

        private String shapeArabic(String text) {
            if (text == null || text.isBlank()) {
                return "";
            }

            try {
                ArabicShaping shaper = new ArabicShaping(ArabicShaping.LETTERS_SHAPE);
                String shaped = shaper.shape(text);

                Bidi bidi = new Bidi(shaped, Bidi.DIRECTION_RIGHT_TO_LEFT);
                return bidi.writeReordered(Bidi.DO_MIRRORING);
            } catch (Exception e) {
                return text;
            }
        }

        private void drawText(String text, PDFont font, float size, Color color, float x, float y) throws IOException {
            String value = sanitize(text);

            if (containsArabic(value)) {
                value = shapeArabic(value);
            }

            cs.beginText();
            cs.setFont(font, size);
            cs.setNonStrokingColor(color);
            cs.newLineAtOffset(x, y);
            cs.showText(value);
            cs.endText();
        }

        private boolean containsArabic(String text) {
            if (text == null) {
                return false;
            }
            for (char c : text.toCharArray()) {
                if (c >= 0x0600 && c <= 0x06FF) {
                    return true;
                }
            }
            return false;
        }

        private void drawFittedText(String text, PDFont font, float size, Color color, float x, float y, float maxWidth) throws IOException {
            String fitted = fitText(text, font, size, maxWidth);
            drawText(fitted, font, size, color, x, y);
        }

        private String fitText(String text, PDFont font, float size, float maxWidth) throws IOException {
            String s = sanitize(text);
            if (textWidth(s, font, size) <= maxWidth) {
                return s;
            }
            String ellipsis = "...";
            int hi = s.length();
            while (hi > 0) {
                String cut = s.substring(0, hi) + ellipsis;
                if (textWidth(cut, font, size) <= maxWidth) {
                    return cut;
                }
                hi--;
            }
            return ellipsis;
        }

        private float textWidth(String text, PDFont font, float size) throws IOException {
            return font.getStringWidth(sanitize(text)) / 1000f * size;
        }

        private float usableWidth() {
            return page.getMediaBox().getWidth() - (MARGIN * 2);
        }

        private float sum(float[] arr) {
            float s = 0;
            for (float v : arr) {
                s += v;
            }
            return s;
        }

        private String sanitize(String text) {
            if (text == null) {
                return "";
            }
            return text.replace('\n', ' ').replace('\r', ' ');
        }

        private String safeCell(String text) {
            return sanitize(text == null || text.isBlank() ? "-" : text);
        }

        private float[] priorityColor(String p) {
            if ("عالي".equalsIgnoreCase(p)) {
                return new float[]{254, 226, 226, 255};
            }
            if ("متوسط".equalsIgnoreCase(p)) {
                return new float[]{255, 247, 237, 255};
            }
            return new float[]{239, 246, 255, 255};
        }

        private Color priorityTextColor(String p) {
            if ("عالي".equalsIgnoreCase(p)) {
                return DANGER;
            }
            if ("متوسط".equalsIgnoreCase(p)) {
                return WARNING;
            }
            return PRIMARY;
        }
    }

    // =========================================================================================
    // Font loading
    // =========================================================================================
    private static PDFont tryLoadFont(PDDocument doc, File... candidates) {
        for (File f : candidates) {
            try {
                if (f != null && f.exists() && f.isFile()) {
                    return PDType0Font.load(doc, f);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    // =========================================================================================
    // Models
    // =========================================================================================
    private static final class ReportData {

        LocalDateTime generatedAt;

        int totalLines;
        int totalCars;
        int totalDrivers;
        int totalEmployees;

        int carsWithDriver;
        int carsWithLine;

        int soonCarsLic;
        int soonDrvLic;

        int maintenance30d;
        double fuelLiters7d;
        double fuelCost7d;

        List<VehicleAssignmentRow> vehicleRows = new ArrayList<>();
        List<AlertRow> alertRows = new ArrayList<>();
    }

    private static final class VehicleAssignmentRow {

        int carId;
        String carName;
        String driverName;
        String lineLabel;
        String licenseExpiry;
    }

    private static final class AlertRow {

        String priority;
        String type;
        String message;
        String date;
    }

}
