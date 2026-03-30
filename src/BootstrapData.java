import DB.DB;

public final class BootstrapData {

    private BootstrapData() {
    }

    public static void seedAdminIfDatabaseEmpty() {
        try {
            Integer usersCount = DB.queryScalar(
                    "SELECT COUNT(*) FROM USERS",
                    Integer.class
            );

            if (usersCount != null && usersCount > 0) {
                return; // فيه يوزرات بالفعل، خلاص مش هنعمل حاجة
            }

            // 1) Department
            DB.executeUpdate(
                    "INSERT INTO Department_Type (dept_Name) VALUES (?)",
                    "Administration"
            );

            // 2) Itinerary
            DB.executeUpdate(
                    "INSERT INTO Itinerary (itinerary_name, distances, Fuel_consumption, Number_of_cars, Approximate_time) "
                    + "VALUES (?, ?, ?, ?, ?)",
                    "Main Route", 10, 5, 1, 30
            );

            // 3) Employee
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

            // 4) Admin User
            DB.executeUpdate(
                    "INSERT INTO Users (employee_id, username, Role_Name, password_hash) "
                    + "VALUES ("
                    + "(SELECT Employee_ID FROM Employees WHERE Phone = ?), "
                    + "?, ?, ?)",
                    "01000000000",
                    "admin",
                    "ADMIN",
                    "admin123"
            );

            System.out.println("✅ Default admin created successfully.");
            System.out.println("Username: admin");
            System.out.println("Password: admin123");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("فشل في إنشاء بيانات البداية: " + e.getMessage(), e);
        }
    }
}