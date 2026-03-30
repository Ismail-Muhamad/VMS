package DB;

import DB.DB;

public final class BootstrapData {

    private BootstrapData() {
    }

    public static void seedAdminIfDatabaseEmpty() {
        try {
            Integer usersCount = DB.queryScalar("SELECT COUNT(*) FROM USERS", Integer.class);

            if (usersCount != null && usersCount > 0) {
                return;
            }

            ensureDepartmentExists();
            ensureItineraryExists();
            ensureEmployeeExists();
            ensureAdminUserExists();

            System.out.println("✅ Default admin created successfully.");
            System.out.println("Username: 123");
            System.out.println("Password: 123");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("فشل في إنشاء بيانات البداية: " + e.getMessage(), e);
        }
    }

    private static void ensureDepartmentExists() {
        Integer count = DB.queryScalar(
                "SELECT COUNT(*) FROM Department_Type WHERE dept_Name = ?",
                Integer.class,
                "Administration"
        );

        if (count == null || count == 0) {
            DB.executeUpdate(
                    "INSERT INTO Department_Type (dept_Name) VALUES (?)",
                    "Administration"
            );
        }
    }

    private static void ensureItineraryExists() {
        Integer count = DB.queryScalar(
                "SELECT COUNT(*) FROM Itinerary WHERE itinerary_name = ?",
                Integer.class,
                "Main Route"
        );

        if (count == null || count == 0) {
            DB.executeUpdate(
                    "INSERT INTO Itinerary (itinerary_name, distances, Fuel_consumption, Number_of_cars, Approximate_time) "
                    + "VALUES (?, ?, ?, ?, ?)",
                    "Main Route", 10, 5, 1, 30
            );
        }
    }

    private static void ensureEmployeeExists() {
        Integer count = DB.queryScalar(
                "SELECT COUNT(*) FROM Employees WHERE Phone = ?",
                Integer.class,
                "01000000000"
        );

        if (count == null || count == 0) {
            DB.executeUpdate(
                    "INSERT INTO Employees (Employee_Name, Phone, dept_ID, itinerary_id) "
                    + "VALUES (?, ?, "
                    + "(SELECT dept_ID FROM Department_Type WHERE dept_Name = ?), "
                    + "(SELECT itinerary_id FROM Itinerary WHERE itinerary_name = ?))",
                    "System Admin",
                    "01000000000",
                    "Administration",
                    "Main Route"
            );
        }
    }

    private static void ensureAdminUserExists() {
        Integer count = DB.queryScalar(
                "SELECT COUNT(*) FROM Users WHERE LOWER(username) = LOWER(?)",
                Integer.class,
                "admin"
        );

        if (count == null || count == 0) {
            DB.executeUpdate(
                    "INSERT INTO Users (employee_id, username, Role_Name, password_hash) "
                    + "VALUES ("
                    + "(SELECT Employee_ID FROM Employees WHERE Phone = ?), "
                    + "?, ?, ?)",
                    "01000000000",
                    "123",
                    "ADMIN",
                    "123"
            );
        }
    }
}