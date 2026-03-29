
public class AuthUser {

    public final int userId;
    public final String username;
    public final String displayName;
    public final String role; // ADMIN / SUB_ADMIN / USER

    public AuthUser(int userId, String username, String displayName, String role) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.role = role == null ? "USER" : role.trim().toUpperCase();
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean isSubAdmin() {
        return "SUB_ADMIN".equals(role);
    }

    public boolean canOpenSettings() {
        return isAdmin() || isSubAdmin();
    }
}
