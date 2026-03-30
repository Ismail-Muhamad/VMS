
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        BootstrapData.seedAdminIfDatabaseEmpty();

        java.awt.EventQueue.invokeLater(() -> {
            new Login().setVisible(true);
        });
                try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }
}