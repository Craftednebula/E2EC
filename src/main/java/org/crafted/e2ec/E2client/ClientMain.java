package org.crafted.e2ec.E2client;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ClientMain {

    public static void main(String[] args) {

        // run tests.. i hate tests
        //ChatClient.runAllTests();
        //ChatManagerWindow.runAllTests();
        //ChatWindow.runAllTests();
        //RoomBrowserWindow.runAllTests();

        SwingUtilities.invokeLater(() -> {
            try {
                ChatManagerWindow manager = new ChatManagerWindow();
                manager.show();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null,
                        e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
}
