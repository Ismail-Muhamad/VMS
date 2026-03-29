
import DB.DB;

public final class AuthService {

    private AuthService() {
    }

    public static AuthUser login(String username, String password) {

        String sql
                = "SELECT "
                + "  u.USER_ID, "
                + "  u.USERNAME, "
                + "  u.PASSWORD_HASH, "
                + "  e.EMPLOYEE_NAME AS DISP, "
                + "  u.ROLE_NAME "
                + "FROM USERS u "
                + "LEFT JOIN EMPLOYEES e ON e.EMPLOYEE_ID = u.EMPLOYEE_ID "
                + "WHERE LOWER(u.USERNAME) = LOWER(?)";

        Object[] row = DB.queryOne(sql, rs -> new Object[]{
            rs.getInt(1),
            rs.getString(2),
            rs.getString(3),
            rs.getString(4),
            rs.getString(5)
        }, username);

        if (row == null) {
            return null;
        }

        String storedPass = (String) row[2];
        if (storedPass == null || !storedPass.equals(password)) {
            return null;
        }

        return new AuthUser(
                (Integer) row[0],
                (String) row[1],
                (String) row[3],
                (String) row[4]
        );
    }
}
