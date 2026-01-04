package org.crafted.e2ec;

import java.io.*;
import java.net.Socket;

class ClientHandler implements Runnable {

    Socket socket;
    BufferedReader in;
    PrintWriter out;
    String alias = "Unknown";
    Room currentRoom = null;

    ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            out.println("Enter alias: ");
            alias = in.readLine();
            Server.broadcast(alias + " joined the chat.");

            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.startsWith("/")) {
                    handleCommand(msg);
                } else if (currentRoom != null) {
                    currentRoom.broadcast(alias + ": " + msg);
                } else {
                    send("Join a room first with /join <roomname>");
                }
            }
        } catch (IOException ignored) {
        } finally {
            close();
        }
    }

    void handleCommand(String cmd) {
        String[] parts = cmd.split(" ", 2);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "/join":
                if (parts.length < 2) {
                    send("Usage: /join <roomname>");
                    return;
                }
                String roomName = parts[1];
                if (currentRoom != null) {
                    currentRoom.removeMember(this);
                }
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

            case "/rooms":
                send("Available rooms: " + Server.rooms.keySet());
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
