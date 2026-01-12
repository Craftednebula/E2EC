package org.crafted.e2ec.E2client;

import javax.swing.*;

public class ClientMain {

    public static void main(String[] args) {
        // entry point for the chat client application
        // initializes and starts the ChatClient
        // cough cough
        // cough
        // COUGH
        // COUGH cough COUGH
        // cough
        // ahem
        // excuse me
        SwingUtilities.invokeLater(() -> {
            // schedules a task to be executed on the Event Dispatch Thread (EDT) at some point in the future
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
