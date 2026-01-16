package org.crafted.e2ec.DedicatedServer;

import java.util.Set;

public class CommandProcessor {

    private final ClientSession client;
    private final ServerFacade server;

    public CommandProcessor(ClientSession client, ServerFacade server) {
        this.client = client;
        this.server = server;
    }

    public void handle(String cmd) {
        if (cmd == null || cmd.isBlank()) return;

        String[] parts = cmd.split(" ", 2);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "/create" -> createRoom(parts);
            case "/join" -> join(parts);
            case "/leave" -> leave();
            case "/rooms" -> listRooms();
            case "/msg", "/whisper", "/w", "/m", "/message" -> whisper(parts);
            case "/r", "/reply" -> reply(parts);
            case "/quit", "/exit" -> quit();
            default -> client.send("Unknown command.");
        }
    }

    /* ---------- Command Implementations ---------- */

    private void createRoom(String[] parts) {
        if (parts.length < 2) {
            client.send("Usage: /create <roomname>");
            return;
        }

        if (!PermissionManager.canPerform(client.getUsername(),
                client.getPermissionLevel(),
                "command.createroom")) {
            client.send("You do not have permission to create rooms.");
            return;
        }

        String roomName = parts[1].trim();
        if (server.getRoom(roomName) != null) {
            client.send("Room already exists.");
            return;
        }

        Set<Integer> defaultViewChat = Set.of(0, 1, 100);
        server.createRoom(roomName, defaultViewChat, defaultViewChat,
                false, false, false);

        client.send("Room '" + roomName + "' created successfully.");
    }

    private void join(String[] parts) {
        if (parts.length < 2) {
            client.send("Usage: /join <roomname>");
            return;
        }

        String roomName = parts[1].trim();
        Room room = server.getRoom(roomName);

        if (room == null) {
            client.send("Room does not exist.");
            return;
        }

        if (!room.canView(client.getPermissionLevel())) {
            client.send("You do not have permission to view this room.");
            return;
        }

        // Leave current room if in one
        if (client.getCurrentRoom() != null) {
            client.getCurrentRoom().removeMember(client);
        }

        client.setCurrentRoom(room);
        room.addMember(client);
        client.send("Joined room: " + roomName);
    }

    private void leave() {
        Room room = client.getCurrentRoom();
        if (room == null) {
            client.send("You are not in a room.");
            return;
        }

        room.removeMember(client);
        client.setCurrentRoom(null);
        client.send("You left the room.");
    }

    private void listRooms() {
        client.send("Available rooms:");

        boolean foundAny = false;

        for (Room room : Server.rooms.values()) {
            if (!room.canView(client.getPermissionLevel())) continue;

            foundAny = true;
            boolean canChat = room.canChat(client.getPermissionLevel());
            boolean isCurrent = room == client.getCurrentRoom();

            StringBuilder line = new StringBuilder(" - " + room.getName());
            line.append(" (").append(room.getMemberCount()).append(" users)");
            line.append(" (").append(canChat ? "chat ✓" : "view only").append(")");
            if (isCurrent) line.append(" [current]");

            client.send(line.toString());
        }

        if (!foundAny) client.send("No rooms are visible to you.");
    }
    private void quit() {
            client.send("Disconnecting from server...");
            client.disconnect();
    }
    private void whisper(String[] parts) {
        if (parts.length < 2) {
            client.send("Usage: /msg <user> <message>");
            return;
        }

        String[] args = parts[1].split(" ", 2);
        if (args.length < 2) {
            client.send("Usage: /msg <user> <message>");
            return;
        }

        String targetName = args[0];
        String message = args[1];

        ClientSession target = server.findUser(targetName);
        if (target == null) {
            client.send("User not found.");
            return;
        }

        target.lastWhisperFrom = client;
        client.lastWhisperFrom = target;

        target.send("[From " + client.getUsername() + "]: " + message);
        client.send("[To " + target.getUsername() + "]: " + message);
    }

    private void reply(String[] parts) {
        if (client.lastWhisperFrom == null) {
            client.send("No one to reply to.");
            return;
        }

        if (parts.length < 2) {
            client.send("Usage: /r <message>");
            return;
        }

        String message = parts[1];
        client.lastWhisperFrom.lastWhisperFrom = client;
        client.lastWhisperFrom.send("[From " + client.getUsername() + "]: " + message);
        client.send("[To " + client.lastWhisperFrom.getUsername() + "]: " + message);
    }
}
