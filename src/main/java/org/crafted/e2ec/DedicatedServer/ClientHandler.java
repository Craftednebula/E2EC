package org.crafted.e2ec.DedicatedServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;
public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private int permissionLevel;
    private Room currentRoom = null;
    private UserManager userManager;
    private String hostPassword;
    private ClientHandler lastWhisperFrom;


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
    public int getPermissionLevel() {
        return permissionLevel;
    }

    @Override
    public void run() {
        try {
            // Host password check
            out.println("Enter host password to connect:");
            String enteredPassword = in.readLine();
            if (enteredPassword == null || !enteredPassword.equals(hostPassword)) {
                out.println("incorrect host password. Disconnecting.");
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

            // ✅ MAIN CHAT LOOP (THIS WAS MISSING)
            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.startsWith("/")) {
                    handleCommand(msg);
                } else if (currentRoom != null) {

                    // 🚫 CHAT PERMISSION CHECK
                    if (!currentRoom.canChat(permissionLevel)) {
                        send("You do not have permission to chat in this room.");
                        return;
                    }

                    String formatted = "<" + PermissionManager.getName(permissionLevel) + ">"
                            + username + ": " + msg;

                    if (currentRoom.shouldTagMessages()) {
                        formatted = "[" + currentRoom.getName() + "] " + formatted;
                    }

                    // 📢 BROADCAST ALL OR ROOM-ONLY
                    if (currentRoom.shouldBroadcastAll()) {
                        Server.broadcastRoomMessage(currentRoom, formatted);
                    } else {
                        currentRoom.broadcast(formatted);
                    }
                }

            }
        } catch (IOException e) {
            System.out.println("Connection lost with user " + username);
        } finally {
            close();
        }
    }

    private ClientHandler findUser(String name) {
        for (ClientHandler c : Server.clients) {
            if (c.getUsername() != null &&
                c.getUsername().equalsIgnoreCase(name)) {
                return c;
            }
        }
        return null;
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

            case "/create": {
                if (parts.length < 2) {
                    send("Usage: /create <roomname>");
                    return;
                }

                if (!PermissionManager.canPerform(username, permissionLevel, "command.createroom")) {
                    send("You do not have permission to create rooms.");
                    return;
                }

                String roomName = parts[1];

                if (Server.getRoom(roomName) != null) {
                    send("Room already exists.");
                    return;
                }
                Set<Integer> defaultViewChat = Set.of(0,1,100);
                Server.createRoom(roomName, defaultViewChat, defaultViewChat, false, false, false);
                send("Room '" + roomName + "' created successfully.");
                break;
            }
            case "/editperms": {
                if (parts.length < 2) {
                    send("Usage: /editperms <username> <level>");
                    return;
                }

                String[] args = parts[1].split(" ");
                if (args.length != 2) {
                    send("Usage: /editperms <username> <level>");
                    return;
                }

                String targetName = args[0];
                int newLevel;

                try {
                    newLevel = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    send("Invalid permission level.");
                    return;
                }

                // 🔑 OWNER OVERRIDE
                boolean isOwner = username.equalsIgnoreCase(PermissionManager.ownerUsername);

                UserManager.User target = userManager.getUser(targetName);
                if (target == null) {
                    send("User not found.");
                    return;
                }

                if (!isOwner) {
                    if (target.permissionLevel >= permissionLevel) {
                        send("You cannot edit users with equal or higher permission than you.");
                        return;
                    }

                    if (newLevel >= permissionLevel) {
                        send("You cannot assign a permission level equal to or higher than your own.");
                        return;
                    }
                }

                boolean success = userManager.setPermissionLevel(targetName, newLevel);
                if (!success) {
                    send("Failed to update permissions.");
                    return;
                }

                send("Updated " + targetName + "'s permission level to " + newLevel + ".");
                break;
            }
            case "/msg": {
                if (parts.length < 2) {
                    send("Usage: /msg <user> <message>");
                    return;
                }

                String[] args = parts[1].split(" ", 2);
                if (args.length < 2) {
                    send("Usage: /msg <user> <message>");
                    return;
                }

                String targetName = args[0];
                String message = args[1];

                ClientHandler target = findUser(targetName);
                if (target == null) {
                    send("User not found.");
                    return;
                }

                target.lastWhisperFrom = this;

                target.send("[From " + username + "]: " + message);
                send("[To " + target.getUsername() + "]: " + message);
                break;
            }

            case "/r": {
                if (lastWhisperFrom == null) {
                    send("No one to reply to.");
                    return;
                }

                if (parts.length < 2) {
                    send("Usage: /r <message>");
                    return;
                }

                String message = parts[1];
                lastWhisperFrom.lastWhisperFrom = this;
                lastWhisperFrom.send("[From " + username + "]: " + message);
                send("[To " + lastWhisperFrom.getUsername() + "]: " + message);
                break;
            }

            case "/join": {
                if (parts.length < 2) {
                    send("Usage: /join <roomname>");
                    return;
                }

                String roomName = parts[1];
                Room room = Server.getRoom(roomName);

                if (room == null) {
                    send("Room does not exist.");
                    return;
                }

                // 🚫 VIEW PERMISSION CHECK
                if (!room.canView(permissionLevel)) {
                    send("You do not have permission to view this room.");
                    return;
                }

                if (currentRoom != null) {
                    currentRoom.removeMember(this);
                }

                currentRoom = room;
                currentRoom.addMember(this);
                send("Joined room: " + roomName);
                break;
            }
            case "/leave": {
                if (currentRoom == null) {
                    send("You are not in a room.");
                    return;
                }

                currentRoom.removeMember(this);
                currentRoom = null;
                send("You left the room.");
                break;
            }

            case "/kick": {
                if (!PermissionManager.canPerform(username, permissionLevel, "command.kick")) {
                    send("You do not have permission to kick users.");
                    return;
                }
                send("Kick command executed (placeholder).");
                break;
            }

            case "/ban": {
                if (!PermissionManager.canPerform(username, permissionLevel, "command.ban")) {
                    send("You do not have permission to ban users.");
                    return;
                }
                send("Ban command executed (placeholder).");
                break;
            }

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
