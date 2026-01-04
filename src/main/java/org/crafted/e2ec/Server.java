package org.crafted.e2ec;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    static final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("Server running on port 5000");

        while (true) {
            Socket socket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(socket);
            clients.add(handler);
            new Thread(handler).start();
        }
    }

    // Broadcast to all clients in all rooms (optional)
    static void broadcast(String message) {
        for (ClientHandler c : clients) {
            c.send(message);
        }
    }

    static void remove(ClientHandler handler) {
        clients.remove(handler);
        // Remove from any room they are in
        if (handler.currentRoom != null) {
            handler.currentRoom.removeMember(handler);
        }
        broadcast(handler.alias + " left the chat.");
    }

    static Room getOrCreateRoom(String roomName) {
        return rooms.computeIfAbsent(roomName, Room::new);
    }
}
