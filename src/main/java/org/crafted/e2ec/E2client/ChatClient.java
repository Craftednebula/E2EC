package org.crafted.e2ec.E2client;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

public class ChatClient {

    private PrintWriter out;
    private BufferedReader in;
    private String chatName;
    private RoomBrowserWindow roomBrowser;
    private ChatWindow chatWindow;
    private boolean inRoom = false;
    private String address;

    public void start() throws Exception {
        // start the chat client
        
        // Ask for server address
        address = LoginDialogs.askServerAddress();
        if (address == null) return;

        String host;
        int port;

        try {
            String[] parts = address.split(":");
            if (parts.length != 2) throw new IllegalArgumentException();

            host = parts[0].trim();
            port = Integer.parseInt(parts[1].trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Invalid address format.\nUse: ip:port\nExample: 127.0.0.1:7000",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Connect directly
        Socket chatSocket = new Socket(host, port);
        out = new PrintWriter(chatSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));

        handleHandshake();

        // Initially, user is NOT in a room -> show room browser
        showRoomBrowser();

        // Start reader thread
        startReaderThread();
    }

    private void handleHandshake() throws Exception {
        // handle client-server handshake (not just login and register but also gives input i thinks)
        // not input!! its output from server to client look at the bottom line
        while (true) {
            String serverLine = in.readLine();
            if (serverLine == null)
                throw new IOException("Disconnected during login.");

            if (serverLine.startsWith("OK: Logged in")) {
                chatName = serverLine.split("j")[1];
                System.out.println("chatname: " + chatName);
                System.out.println(serverLine);
                break;
            }

            String response;
            if (serverLine.toLowerCase().contains("login")
                    && serverLine.toLowerCase().contains("register")) {

                response = LoginDialogs.loginOrRegister(serverLine);
            } else {
                response = LoginDialogs.textPrompt(serverLine);
            }

            if (response == null)
                throw new IOException("Login cancelled.");

            out.println(response);
        }
    }

    private void showRoomBrowser() {
        // shows the room browser window
        if (chatWindow != null) {
            chatWindow.close(); // dispose chat window if open
            chatWindow = null;
        }

        roomBrowser = new RoomBrowserWindow(out, chatName, address);
        roomBrowser.show();

    }

    private void showChatWindow(String room) {
        // shows the chat window for the given room
        // inputs: room name
        // outputs: none
        if (roomBrowser != null) {
            roomBrowser.close();
            roomBrowser = null;
        }

        chatWindow = new ChatWindow(out, chatName, address);
        chatWindow.show();
        chatWindow.setRoom(room);
    }

    private void startReaderThread() {
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {

                    // Joined a room
                    if (msg.startsWith("Joined room: ")) {
                        String room = msg.substring("Joined room: ".length()).trim();
                        inRoom = true;

                        SwingUtilities.invokeLater(() -> showChatWindow(room));
                    }

                    // Left room
                    else if (msg.equalsIgnoreCase("You left the room.")) {
                        inRoom = false;

                        SwingUtilities.invokeLater(this::showRoomBrowser);
                    }

                    // Room list lines (only when browsing)
                    else if (!inRoom && roomBrowser != null && msg.startsWith(" - ")) {
                        roomBrowser.updateRoom(msg.substring(3)); // Strip leading " - "
                    }


                    // Chat messages
                    if (inRoom && chatWindow != null) {
                        chatWindow.append(msg);
                    }
                }
            } catch (IOException e) {
                if (chatWindow != null) chatWindow.append("Disconnected from server.");
                else if (roomBrowser != null) roomBrowser.close();
            }
        }).start();
    }
}
