import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.regex.Pattern;

public final class AppUtils {

    private AppUtils() {
    }

    public static int parseIntSafe(Object v, int def) {
        if (v == null) {
            return def;
        }
        try {
            String s = String.valueOf(v).trim();
            if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
                return def;
            }
            if (s.contains(".")) {
                return (int) Double.parseDouble(s);
            }
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static Integer getSelectedComboId(JComboBox<ComboItem> combo) {
        if (combo == null) {
            return null;
        }
        Object sel = combo.getSelectedItem();
        if (sel instanceof ComboItem it) {
            return it.id;
        }
        return null;
    }

    public static void selectComboById(JComboBox<ComboItem> combo, int id) {
        if (combo == null) {
            return;
        }
        ComboBoxModel<ComboItem> model = combo.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            ComboItem it = model.getElementAt(i);
            if (it != null && it.id == id) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    public static void commitSpinner(JSpinner sp) {
        if (sp == null) {
            return;
        }
        try {
            sp.commitEdit();
        } catch (ParseException ignored) {
        }
    }

    public static int getSpinnerInt(JSpinner sp, int def) {
        if (sp == null) {
            return def;
        }
        commitSpinner(sp);
        Object v = sp.getValue();
        if (v instanceof Number n) {
            return n.intValue();
        }
        return parseIntSafe(v, def);
    }

    public static double getSpinnerDouble(JSpinner sp, double def) {
        if (sp == null) {
            return def;
        }
        commitSpinner(sp);
        Object v = sp.getValue();
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        try {
            String s = String.valueOf(v).trim();
            if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
                return def;
            }
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static void lockDateChooser(JDateChooser dc) {
        if (dc == null) {
            return;
        }
        try {
            java.awt.Component c = dc.getDateEditor().getUiComponent();
            if (c instanceof JTextField tf) {
                tf.setEditable(false);
            }
        } catch (Exception ignored) {
        }
    }

    private static final Pattern EGY_MOBILE = Pattern.compile("^(010|011|012|015)\\d{8}$");
    private static final Pattern PLATE_NUMBER_PATTERN = Pattern.compile("^[A-Za-z]{1,4}\\d{1,3}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]{3,30}$");

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isPositive(int value) {
        return value > 0;
    }

    public static boolean isPositive(double value) {
        return value > 0;
    }

    public static boolean isValidEgyptMobile(String phone) {
        if (phone == null) {
            return false;
        }
        return EGY_MOBILE.matcher(phone.trim()).matches();
    }

    public static boolean isValidPlateNumber(String plate) {
        if (plate == null) {
            return false;
        }
        return PLATE_NUMBER_PATTERN.matcher(plate.trim()).matches();
    }

    public static boolean isValidUsername(String username) {
        if (username == null) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    public static boolean isFutureOrToday(Date date) {
        if (date == null) {
            return false;
        }
        LocalDate givenDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return !givenDate.isBefore(LocalDate.now());
    }

    // ========= New helpers for unified validation =========

    public static String requireText(String value, String fieldName) {
        return isBlank(value) ? fieldName + " مطلوب." : null;
    }

    public static String requireDate(Date value, String fieldName) {
        return value == null ? fieldName + " مطلوب." : null;
    }

    public static String requireCombo(Integer value, String fieldName) {
        return value == null ? "من فضلك اختر " + fieldName + "." : null;
    }

    public static String requirePositiveInt(int value, String fieldName) {
        return value <= 0 ? fieldName + " لازم تكون أكبر من صفر." : null;
    }

    public static String requirePositiveDouble(double value, String fieldName) {
        return value <= 0 ? fieldName + " لازم تكون أكبر من صفر." : null;
    }

    public static String requireFutureOrToday(Date value, String fieldName) {
        if (value == null) {
            return fieldName + " مطلوب.";
        }
        return isFutureOrToday(value) ? null : fieldName + " لا يمكن أن يكون قبل اليوم.";
    }

    public static String requireValidEgyptMobile(String phone) {
        return isValidEgyptMobile(phone) ? null : "رقم الهاتف غير صحيح.";
    }

    public static String requireValidPlateNumber(String plate) {
        return isValidPlateNumber(plate) ? null : "رقم اللوحة غير صحيح. لازم يبدأ من 1 إلى 4 حروف وبعدهم من 1 إلى 3 أرقام.";
    }

    public static String firstError(String... errors) {
        if (errors == null) {
            return null;
        }
        for (String e : errors) {
            if (e != null && !e.trim().isEmpty()) {
                return e;
            }
        }
        return null;
    }
}