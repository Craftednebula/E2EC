package org.crafted.e2ec.DedicatedServer;

public class ClientSession implements RoomMember {

    private final ClientIO io;
    private final UserManager userManager;
    private final ServerFacade server;
    private final CommandProcessor commands;
    private final String hostPassword;

    private String username;
    private int permissionLevel;
    private Room currentRoom;
    ClientSession lastWhisperFrom;

    public ClientSession(
            ClientIO io,
            String hostPassword,
            UserManager userManager,
            ServerFacade server
    ) {
        this.io = io;
        this.hostPassword = hostPassword;
        this.userManager = userManager;
        this.server = server;
        this.commands = new CommandProcessor(this, server);
    }

    public void start() throws Exception {
        // main session logic
        if (!checkHostPassword()) return;
        if (!authenticate()) return;

        io.writeLine("OK: Logged inj" + Server.CHAT_NAME);
        server.broadcast(username + " joined the chat.", this);

        messageLoop();
    }

    private boolean checkHostPassword() throws Exception {
        // check host password
        // input: none
        // output: true if correct, false if incorrect
        io.writeLine("Enter host password to connect:");
        String entered = io.readLine();

        if (!hostPassword.equals(entered)) {
            io.writeLine("incorrect host password. Disconnecting.");
            return false;
        }
        return true;
    }

    private boolean authenticate() throws Exception {
        // authentication process
        // input: none
        // output: true if successful, false if failed
        io.writeLine("Welcome! Type 'login' or 'register':");

        while (true) {
            String choice = io.readLine();
            if (choice == null) return false;

            if (choice.equalsIgnoreCase("login") && login()) return true;
            if (choice.equalsIgnoreCase("register") && register()) return true;

            io.writeLine("Invalid option.");
        }
    }

    private boolean login() throws Exception {
        // login process
        // input: none
        // output: true if successful, false if failed
        io.writeLine("Enter username:");
        String u = io.readLine();
        io.writeLine("Enter password:");
        String p = io.readLine();

        UserManager.User user = userManager.login(u, p);
        if (user == null) {
            io.writeLine("Login failed.");
            return false;
        }

        username = user.username;
        permissionLevel = user.permissionLevel;
        return true;
    }

    private boolean register() throws Exception {
        // registration process
        // input: none
        // output: true if successful, false if failed
        io.writeLine("Enter new username:");
        String u = io.readLine();
        io.writeLine("Enter new password:");
        String p = io.readLine();

        if (!userManager.register(u, p, 0)) {
            io.writeLine("Registration failed.");
            return false;
        }

        username = u;
        permissionLevel = 0;
        return true;
    }

    private void messageLoop() {
        // main loop to read messages from client
        try {
            String msg;
            while ((msg = io.readLine()) != null) {
                if (msg.startsWith("/")) {
                    commands.handle(msg);
                } else {
                    sendChat(msg);
                }
            }
        } catch (Exception e) {
            // most likely a disconnect
            System.out.println("Client " + username + " disconnected unexpectedly: " + e.getMessage());
        } finally {
            disconnect();
        }
    }


    private void sendChat(String msg) {
        if (currentRoom == null) return;
        if (!currentRoom.canChat(permissionLevel)) {
            send("You do not have permission to chat.");
            return;
        }

        String formatted = "<" + PermissionManager.getName(permissionLevel) + ">"
                + username + ": " + msg;

        if (currentRoom.shouldTagMessages()) {
            formatted = "[" + currentRoom.getName() + "] " + formatted;
        }

        if (currentRoom.shouldBroadcastAll()) {
            server.broadcastRoomMessage(currentRoom, formatted);
        } else {
            currentRoom.broadcast(formatted);
        }
    }
    @Override
    public void send(String msg) {
        io.writeLine(msg);
    }

    public void disconnect() {
        try {
            server.removeClient(this);  // remove from rooms + client list
        } catch (Exception ignored) {}

        try {
            io.close();
        } catch (Exception ignored) {}

        System.out.println("Client " + username + " fully disconnected.");
    }


    // getters + setters for CommandProcessor
    @Override
    public String getUsername() { return username; }
    @Override
    public int getPermissionLevel() { return permissionLevel; }
    public Room getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(Room r) { currentRoom = r; }
}
