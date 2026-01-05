package org.crafted.e2ec.DedicatedServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    static final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    // Config / hardcoded for this dedicated server
    static final String password = "secret123"; // host password
    static final int PORT = 5000;
    static final String CHAT_NAME = "CoolRoom";
    static final String LOOKUP_HOST = "127.0.0.1";
    static final int LOOKUP_PORT = 6000;

    static UserManager userManager;

    public static void main(String[] args) throws IOException {
        // Load permission definitions
        PermissionManager.loadPermissions();

        // Initialize SQL user database (per-chat)
        try {
            userManager = new UserManager("chat.db");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Register with lookup server
        registerWithLookupServer();

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Chat server running on port " + PORT);

        while (true) {
            Socket socket = serverSocket.accept();
            // ClientHandler now takes the userManager and host password
            ClientHandler handler = new ClientHandler(socket, password, userManager);
            clients.add(handler);
            new Thread(handler).start();
        }
    }

    private static void registerWithLookupServer() {
        try (Socket s = new Socket(LOOKUP_HOST, LOOKUP_PORT);
             PrintWriter out = new PrintWriter(s.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

            out.println("register " + CHAT_NAME + " " + PORT);
            String resp = in.readLine();
            System.out.println("LookupServer response: " + resp);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler c : clients) {
            c.send(message);
        }
    }

    static void remove(ClientHandler handler) {
        clients.remove(handler);
        if (handler.getCurrentRoom() != null) {
            handler.getCurrentRoom().removeMember(handler);
        }
        broadcast(handler.getUsername() + " left the chat.", handler);
    }

    static Room getOrCreateRoom(String roomName) {
        return rooms.computeIfAbsent(roomName, Room::new);
    }
}
