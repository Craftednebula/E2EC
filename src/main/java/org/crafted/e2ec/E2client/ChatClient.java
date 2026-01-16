package org.crafted.e2ec.E2client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.SwingUtilities;


public class ChatClient {

    private PrintWriter out;
    private BufferedReader in;

    private String chatName;
    private String address;
    private Socket socket;
    private RoomBrowserWindow roomBrowser;
    private ChatWindow chatWindow;

    private volatile boolean inRoom = false;

    /* ===================== ENTRY ===================== */

    public void start() throws Exception {
        // starts the chat client by prompting for server address and handling handshak
        address = LoginDialogs.askServerAddress();
        if (address == null) return;

        connect(address);
        handleHandshake(new DialogHandshakeProvider());

        showRoomBrowser();
        startReaderThread();
    }

    /* ===================== CONNECT ===================== */

    private void connect(String address) throws Exception {
        // connects to the chat server at the given address
        // input: address in the format "host:port"
        // output: none (initializes in and out streams)

        String[] parts = address.split(":");
        if (parts.length != 2)
            throw new IllegalArgumentException("Invalid address format");

        String host = parts[0].trim();
        int port = Integer.parseInt(parts[1].trim());

        socket = new Socket(host, port);  // SAVE the socket
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }


    /* ===================== HANDSHAKE ===================== */
    public interface HandshakeProvider {
        // provides responses to server prompts during handshake
        // input: server prompt string
        // output: client response string
        String respond(String serverPrompt) throws Exception;
    }
    public class DialogHandshakeProvider implements HandshakeProvider {
        // uses dialog boxes to get user input for server prompts
        @Override
        public String respond(String serverLine) throws Exception {
            // decide which dialog to show based on server prompt
            // input: server prompt string
            // output: user input string
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
        // uses saved credentials from a ChatEntry to respond to server prompts
        // input: ChatEntry with saved credentials
        // output: client response string based on prompts
        private final ChatManagerWindow.ChatEntry entry;
        private boolean loginAttempted = false;
        private boolean switchedToRegister = false;

        public SavedHandshakeProvider(ChatManagerWindow.ChatEntry entry) {
            this.entry = entry;
        }

        @Override
        public String respond(String serverPrompt) throws Exception {
            // respond to server prompts using saved credentials
            // input: server prompt string
            // output: client response string
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
        // handle the handshake process with the server using the provided HandshakeProvider
        // input: HandshakeProvider to get responses for server prompts
        // output: none (completes handshake and sets chatName)
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
        // show the room browser window
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
        // show the chat window for the given room
        // input: room name
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
        // starts the chat client using saved chat entry credentials
        this.address = entry.address;

        connect(entry.address);

        // let the SERVER prompt as usual
        handleHandshake(new SavedHandshakeProvider(entry));


        showRoomBrowser();
        startReaderThread();
    }

    private void startReaderThread() {
        // starts a background thread to read messages from the server
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
