package org.crafted.e2ec.E2client;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Queue;

public class ChatClient {

    private PrintWriter out;
    private BufferedReader in;

    private String chatName;
    private String address;

    private RoomBrowserWindow roomBrowser;
    private ChatWindow chatWindow;

    private volatile boolean inRoom = false;

    /* ===================== ENTRY ===================== */

    public void start() throws Exception {

        address = LoginDialogs.askServerAddress();
        if (address == null) return;

        connect(address);
        handleHandshake(new DialogHandshakeProvider());

        showRoomBrowser();
        startReaderThread();
    }

    /* ===================== CONNECT ===================== */

    private void connect(String address) throws Exception {
        String[] parts = address.split(":");
        if (parts.length != 2)
            throw new IllegalArgumentException("Invalid address format");

        String host = parts[0].trim();
        int port = Integer.parseInt(parts[1].trim());

        Socket socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /* ===================== HANDSHAKE ===================== */
    public interface HandshakeProvider {
        String respond(String serverPrompt) throws Exception;
    }
    public class DialogHandshakeProvider implements HandshakeProvider {

        @Override
        public String respond(String serverLine) throws Exception {
            String response;
            if (serverLine.toLowerCase().contains("login")
                    && serverLine.toLowerCase().contains("register")) {
                response = LoginDialogs.loginOrRegister(serverLine);
            } else {
                response = LoginDialogs.textPrompt(serverLine);
            }

            if (response == null)
                throw new IOException("Login cancelled.");

            return response;
        }
    }
    public class SavedHandshakeProvider implements HandshakeProvider {

        private final ChatManagerWindow.ChatEntry entry;
        private boolean loginAttempted = false;
        private boolean switchedToRegister = false;

        public SavedHandshakeProvider(ChatManagerWindow.ChatEntry entry) {
            this.entry = entry;
        }

        @Override
        public String respond(String serverPrompt) throws Exception {
            String p = serverPrompt.toLowerCase();

            // Host password prompt
            if (p.contains("host") && p.contains("password")) {
                return entry.hostPassword;
            }

            // Initial login/register choice
            if (p.contains("login") && p.contains("register")) {
                if (!loginAttempted) {
                    loginAttempted = true;
                    return "login"; // first attempt
                } else if (!switchedToRegister) {
                    switchedToRegister = true;
                    return "register"; // login failed → switch to register
                } else {
                    throw new IOException("Both login and registration failed for saved credentials.");
                }
            }

            // Username prompt
            if (p.contains("username") || p.contains("user name")) {
                return entry.username;
            }

            // Password prompt
            if (p.contains("password")) {
                return entry.userPassword;
            }

            // Login failed prompt from server
            if (p.contains("login failed") && !switchedToRegister) {
                switchedToRegister = false; // allow switching on next prompt
                return "register";
            }

            // Registration successful
            if (p.contains("registration successful")) {
                return ""; // continue handshake
            }

            throw new IOException("Unhandled server prompt: " + serverPrompt);
        }
    }




    private void handleHandshake(HandshakeProvider provider) throws Exception {
        while (true) {
            String serverLine = in.readLine();
            if (serverLine == null)
                throw new IOException("Disconnected during login.");

            if (serverLine.startsWith("OK: Logged in")) {
                chatName = serverLine.substring(serverLine.indexOf("j") + 1);
                break;
            }

            System.out.println("[SERVER] " + serverLine);
            String response = provider.respond(serverLine);
            System.out.println("[CLIENT] " + response);
            out.println(response.trim());

        }
    }


    /* ===================== WINDOWS ===================== */

    private void showRoomBrowser() {
        SwingUtilities.invokeLater(() -> {
            inRoom = false;

            if (chatWindow != null) {
                chatWindow.close();
                chatWindow = null;
            }

            roomBrowser = new RoomBrowserWindow(out, chatName, address);
            roomBrowser.show();
        });
    }

    private void showChatWindow(String room) {
        SwingUtilities.invokeLater(() -> {
            inRoom = true;

            if (roomBrowser != null) {
                roomBrowser.close();
                roomBrowser = null;
            }

            if (chatWindow == null) {
                chatWindow = new ChatWindow(out, chatName, address);
                chatWindow.show();
            }

            chatWindow.setRoom(room);
        });
    }

    /* ===================== READER ===================== */
    public void startWithSavedChat(ChatManagerWindow.ChatEntry entry) throws Exception {
        this.address = entry.address;

        connect(entry.address);

        // let the SERVER prompt as usual
        handleHandshake(new SavedHandshakeProvider(entry));


        showRoomBrowser();
        startReaderThread();
    }

    private void startReaderThread() {
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {

                    // Joined room
                    if (msg.startsWith("Joined room: ")) {
                        showChatWindow(msg.substring(13).trim());
                        continue;
                    }

                    // Left room
                    if (msg.equalsIgnoreCase("You left the room.")) {
                        showRoomBrowser();
                        continue;
                    }

                    // Room list updates
                    if (!inRoom && roomBrowser != null && msg.startsWith(" - ")) {
                        roomBrowser.updateRoom(msg.substring(3));
                        continue;
                    }

                    // Chat messages
                    if (inRoom && chatWindow != null) {
                        chatWindow.append(msg);
                    }
                }
            } catch (IOException e) {
                if (chatWindow != null)
                    chatWindow.append("Disconnected from server.");
            }
        }, "Chat-Reader").start();
    }
}
