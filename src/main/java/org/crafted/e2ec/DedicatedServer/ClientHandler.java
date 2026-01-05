package org.crafted.e2ec.DedicatedServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private int permissionLevel;
    private Room currentRoom = null;
    private UserManager userManager;
    private String hostPassword;

    public ClientHandler(Socket socket, String hostPassword, UserManager userManager) throws IOException {
        this.socket = socket;
        this.hostPassword = hostPassword;
        this.userManager = userManager;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }
    public String getUsername() {
        return username;
    }
    public Room getCurrentRoom() {
        return currentRoom;
    }
@Override
public void run() {
    try {
        // Host password check
        out.println("Enter host password to connect:");
        String enteredPassword = in.readLine();
        if (enteredPassword == null || !enteredPassword.equals(hostPassword)) {
            out.println("incorrect host password.");
            socket.close();
            return;
        }

        // Initial choice: login or register
        out.println("Welcome! Type 'login' to log in or 'register' to create a new account:");
        String choice = in.readLine();
        if (choice == null) return;

        if (choice.equalsIgnoreCase("register")) {
            if (!handleRegistration()) return;
        } else if (choice.equalsIgnoreCase("login")) {
            if (!handleLogin()) return;
        } else {
            out.println("Unknown option. Disconnecting.");
            socket.close();
            return;
        }

        // Logged in successfully
        out.println("OK: Logged in as " + username);
        Server.broadcast(username + " joined the chat.", this);

        // Main loop...
    } catch (IOException e) {
        System.out.println("Connection lost with user " + username);
    } finally {
        close();
    }
}


    private boolean handleRegistration() {
        try {
            out.println("Enter new username: ");
            String uname = in.readLine();
            out.println("Enter new password: ");
            String pass = in.readLine();

            // New users are registered as level 0 by default
            boolean success = userManager.register(uname, pass, 0);
            if (!success) {
                out.println("ERROR: Username already exists.");
                return false;
            }

            this.username = uname;
            this.permissionLevel = 0;
            out.println("Registration successful! You are logged in as " + username);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR: Registration failed.");
            return false;
        }
    }

    private boolean handleLogin() {
        try {
            out.println("Enter username: ");
            String uname = in.readLine();
            out.println("Enter password: ");
            String pass = in.readLine();

            UserManager.User user = userManager.login(uname, pass);
            if (user == null) {
                out.println("ERROR: Invalid username or password.");
                return false;
            }

            this.username = user.username;
            this.permissionLevel = user.permissionLevel;
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR: Login failed.");
            return false;
        }
    }

    private void handleCommand(String cmd) {
        String[] parts = cmd.split(" ", 2);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "/join":
                if (parts.length < 2) {
                    send("Usage: /join <roomname>");
                    return;
                }
                String roomName = parts[1];
                if (currentRoom != null) currentRoom.removeMember(this);
                currentRoom = Server.getOrCreateRoom(roomName);
                currentRoom.addMember(this);
                break;

            case "/leave":
                if (currentRoom != null) {
                    currentRoom.removeMember(this);
                    currentRoom = null;
                } else {
                    send("You are not in a room.");
                }
                break;

            case "/kick":
                if (!PermissionManager.canPerform(username, permissionLevel, "command.kick")) {
                    send("You do not have permission to kick users.");
                    return;
                }
                // Kick logic here
                send("Kick command executed (placeholder).");
                break;

            case "/ban":
                if (!PermissionManager.canPerform(username, permissionLevel, "command.ban")) {
                    send("You do not have permission to ban users.");
                    return;
                }
                send("Ban command executed (placeholder).");
                break;

            default:
                send("Unknown command: " + command);
        }
    }

    void send(String msg) {
        out.println(msg);
    }

    void close() {
        Server.remove(this);
        try {
            socket.close();
        } catch (IOException ignored) {}
    }
}
