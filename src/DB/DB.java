package DB;

import javax.swing.table.DefaultTableModel;
import java.io.InputStream;
import java.sql.*;
import java.util.*;

public final class DB {

    private static String URL = "jdbc:oracle:thin:@localhost:1521:XE";
    private static String USER = "VMS_2";
    private static String PASS = "VMS";

    private static boolean DEFAULT_AUTOCOMMIT = true;

    private DB() {
    }

    // ============ RowMapper ============ //
    @FunctionalInterface
    public interface RowMapper<T> {

        T map(ResultSet rs) throws SQLException;
    }

    // ============ Init once (static) ============ //
    static {
        try (InputStream in = DB.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);

                URL = p.getProperty("db.url", URL).trim();
                USER = p.getProperty("db.user", USER).trim();
                PASS = p.getProperty("db.password", PASS).trim();
                DEFAULT_AUTOCOMMIT = Boolean.parseBoolean(
                        p.getProperty("db.pool.autocommit", String.valueOf(DEFAULT_AUTOCOMMIT)).trim()
                );
            }
        } catch (Exception ignored) {
        }

        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Oracle JDBC Driver مش موجود. ضيف ojdbc jar للـ classpath.", e);
        }
    }

    // ============ Connection ============ //
    public static Connection getConnection() throws SQLException {
        Connection con = DriverManager.getConnection(URL, USER, PASS);
        return con;
    }

    // ============ Core helpers ============ //
    private static PreparedStatement prepare(Connection con, String sql, Object... params) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        bindParams(ps, params);
        return ps;
    }

    private static void bindParams(PreparedStatement ps, Object... params) throws SQLException {
        if (params == null) {
            return;
        }

        for (int i = 0; i < params.length; i++) {
            Object v = params[i];
            int idx = i + 1;

            if (v == null) {
                ps.setObject(idx, null);
            } else if (v instanceof java.util.Date date) {
                ps.setDate(idx, new java.sql.Date(date.getTime())); // util.Date -> SQL DATE
            } else {
                ps.setObject(idx, v);
            }
        }
    }

    // ============ Query methods ============ //
    public static <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection con = getConnection(); PreparedStatement ps = prepare(con, sql, params); ResultSet rs = ps.executeQuery()) {

            List<T> out = new ArrayList<>();
            while (rs.next()) {
                out.add(mapper.map(rs));
            }
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("DB.query failed: " + e.getMessage() + " | SQL=" + sql, e);
        }
    }

    public static <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = query(sql, mapper, params);
        return list.isEmpty() ? null : list.get(0);
    }

    @SuppressWarnings("unchecked")
    public static <T> T queryScalar(String sql, Class<T> type, Object... params) {
        return queryOne(sql, rs -> {
            Object v = rs.getObject(1);
            if (v == null) {
                return null;
            }

            if (type.isInstance(v)) {
                return (T) v;
            }

            if (type == Integer.class) {
                return (T) Integer.valueOf(((Number) v).intValue());
            }
            if (type == Long.class) {
                return (T) Long.valueOf(((Number) v).longValue());
            }
            if (type == Double.class) {
                return (T) Double.valueOf(((Number) v).doubleValue());
            }
            if (type == String.class) {
                return (T) String.valueOf(v);
            }
            return (T) v;
        }, params);
    }

    // ============ Update methods ============ //
    public static int executeUpdate(String sql, Object... params) {
        try (Connection con = getConnection(); PreparedStatement ps = prepare(con, sql, params)) {

            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("DB.executeUpdate failed: " + e.getMessage() + " | SQL=" + sql, e);
        }
    }

    // ============ Procedures ============ //
    public static void call(String callSql, Object... params) {
        try (Connection con = getConnection(); CallableStatement cs = con.prepareCall(callSql)) {
            bindParams(cs, params);
            cs.execute();

        } catch (SQLException e) {
            throw new RuntimeException("DB.call failed: " + e.getMessage() + " | SQL=" + callSql, e);
        }
    }

    // ============ JTable helper ============ //
    public static DefaultTableModel queryToTableModel(String sql, Object... params) {
        try (Connection con = getConnection(); PreparedStatement ps = prepare(con, sql, params); ResultSet rs = ps.executeQuery()) {

            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();

            Vector<String> colNames = new Vector<>();
            for (int i = 1; i <= cols; i++) {
                colNames.add(md.getColumnLabel(i));
            }

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= cols; i++) {
                    row.add(rs.getObject(i));
                }
                data.add(row);
            }

            return new DefaultTableModel(data, colNames) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

        } catch (SQLException e) {
            throw new RuntimeException("DB.queryToTableModel failed: " + e.getMessage() + " | SQL=" + sql, e);
        }
    }

}
