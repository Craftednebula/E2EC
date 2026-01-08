package org.crafted.e2ec.E2client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;

    private PrintWriter out;
    private BufferedReader in;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new Client().start();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Connection error: " + e.getMessage());
            }
        });
    }

    private void start() throws IOException {

        /* ---------- Ask for chat name ---------- */
        String chatName = JOptionPane.showInputDialog(
                null,
                "Enter chat name:",
                "Connect",
                JOptionPane.QUESTION_MESSAGE
        );

        if (chatName == null || chatName.isBlank()) return;

        /* ---------- Lookup server ---------- */
        Socket lookup = new Socket("127.0.0.1", 6000);
        PrintWriter lookupOut = new PrintWriter(lookup.getOutputStream(), true);
        BufferedReader lookupIn = new BufferedReader(new InputStreamReader(lookup.getInputStream()));

        lookupOut.println("lookup " + chatName);
        String resp = lookupIn.readLine();
        lookup.close();

        if (resp == null || resp.equals("NOT_FOUND")) {
            JOptionPane.showMessageDialog(null, "Chat not found!");
            return;
        }

        String[] parts = resp.split(" ");
        String hostIp = parts[0];
        int hostPort = Integer.parseInt(parts[1]);

        /* ---------- Connect to chat server ---------- */
        Socket chatSocket = new Socket(hostIp, hostPort);
        out = new PrintWriter(chatSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));

        /* ---------- Login / register handshake ---------- */
        String serverLine;
        while ((serverLine = in.readLine()) != null) {

            // Stop once logged in
            if (serverLine.startsWith("OK: Logged in")) {
                JOptionPane.showMessageDialog(null, serverLine);
                break;
            }

            String response;

            if (serverLine.toLowerCase().contains("password")) {
                response = promptPassword(serverLine);
            } else {
                response = JOptionPane.showInputDialog(null, serverLine);
            }

            if (response == null) {
                chatSocket.close();
                return;
            }

            out.println(response);
        }

        /* ---------- Build chat GUI ---------- */
        buildChatWindow();

        /* ---------- Reader thread ---------- */
        Thread reader = new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    appendMessage(msg);
                }
            } catch (IOException ignored) {}
        });
        reader.setDaemon(true);
        reader.start();
    }

    /* ================= GUI ================= */

    private void buildChatWindow() {
        frame = new JFrame("E2EC Chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 450);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(chatArea);

        inputField = new JTextField();
        sendButton = new JButton("Send");

        ActionListener sendAction = e -> sendMessage();
        sendButton.addActionListener(sendAction);
        inputField.addActionListener(sendAction);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        frame.setLayout(new BorderLayout(5, 5));
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        out.println(text);
        inputField.setText("");
    }

    private void appendMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(msg + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    /* ================= Helpers ================= */

    private String promptPassword(String message) {
        JPasswordField pf = new JPasswordField();
        int ok = JOptionPane.showConfirmDialog(
                null,
                pf,
                message,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        return ok == JOptionPane.OK_OPTION ? new String(pf.getPassword()) : null;
    }
}
