package org.crafted.e2ec.E2client;

import javax.swing.*;
import java.awt.*;

public class LoginDialogs {

    public static String askServerAddress() {
        // does what it says it does
        // pops up a dialog asking for server address
        // output: server address as string (ip:port) or null if cancelled
        String addr = JOptionPane.showInputDialog(
                null,
                "Enter server address (ip:port):",
                "Connect to Server",
                JOptionPane.PLAIN_MESSAGE
        );
        return (addr == null || addr.isBlank()) ? null : addr.trim();
    }

    public static String textPrompt(String prompt) {
        // pops up a dialog asking for text input
        // input: prompt string
        // output: user input string or null if cancelled
        return JOptionPane.showInputDialog(null, prompt);
    }

    public static String loginOrRegister(String prompt) {
        // pops up a dialog asking user to choose between login and register
        // input: prompt string
        // output: "login", "register", or null if cancelled
        final String[] result = new String[1];

        JDialog dialog = new JDialog((Frame) null, "Authentication", true);
        dialog.setSize(350, 140);
        dialog.setLayout(new BorderLayout(5, 5));
        dialog.setLocationRelativeTo(null);

        JLabel label = new JLabel("Press Login to log in or Register to create a new user.", SwingConstants.CENTER);

        JButton login = new JButton("Login");
        JButton register = new JButton("Register");

        login.addActionListener(e -> {
            result[0] = "login";
            dialog.dispose();
        });

        register.addActionListener(e -> {
            result[0] = "register";
            dialog.dispose();
        });

        JPanel buttons = new JPanel();
        buttons.add(login);
        buttons.add(register);

        dialog.add(label, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.setVisible(true);

        return result[0];
    }
}
