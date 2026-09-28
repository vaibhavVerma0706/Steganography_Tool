import Panels.LoginPanel;
import model.User;

import javax.swing.*;


public class Main {

    // -------------------------------------------------------
    // main() — application entry point
    // Shows login screen first, then opens MainFrame
    // -------------------------------------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::showLogin);
    }

    // -------------------------------------------------------
    // showLogin()
    // Creates login window
    // Called on startup AND after logout
    // -------------------------------------------------------
    public static void showLogin() {
        JFrame loginFrame = new JFrame("Steganography Tool — Login");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setResizable(false);

        // When login succeeds → close login → open MainFrame with user
        LoginPanel loginPanel = new LoginPanel(user -> {
            loginFrame.dispose();
            SwingUtilities.invokeLater(() -> new MainFrame(user));
        });

        loginFrame.setContentPane(loginPanel);
        loginFrame.setSize(480, 560);
        loginFrame.setLocationRelativeTo(null);
        loginFrame.setVisible(true);
    }
}