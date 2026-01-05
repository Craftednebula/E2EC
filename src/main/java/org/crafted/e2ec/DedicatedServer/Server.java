package org.crafted.e2ec.DedicatedServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Properties;
import java.io.FileInputStream;
import java.util.Properties;

public class Server {

    static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    static final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    // Config / hardcoded for this dedicated server
    static String HOST_PASSWORD;
    static int PORT;
    static String CHAT_NAME;
    static String OWNER_USERNAME;

    static final String LOOKUP_HOST = "127.0.0.1";
    static final int LOOKUP_PORT = 6000;

    static UserManager userManager;

    public static void main(String[] args) throws IOException {
        ensureConfigFiles();
        loadServerProperties();
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
            ClientHandler handler = new ClientHandler(socket, HOST_PASSWORD, userManager);
            clients.add(handler);
            new Thread(handler).start();
        }
    }
    private static void ensureConfigFiles() throws IOException {

        // ---------- server.properties ----------
        File file = new File("server.properties");

        if (file.exists()) return;

        System.out.println("server.properties not found, generating default config...");

        Properties props = new Properties();

        // ⚠️ USE LITERALS — NEVER VARIABLES HERE
        props.setProperty("chat.name", "CoolRoom");
        props.setProperty("port", "5000");
        props.setProperty("owner", "admin");
        props.setProperty("host.password", "secret123");

        try (FileOutputStream out = new FileOutputStream(file)) {
            props.store(out, "E2EC Dedicated Server Configuration");
        }

        // ---------- permissions.properties ----------
        File perms = new File("permissions.properties");
        if (!perms.exists()) {
            try (FileWriter fw = new FileWriter(perms)) {
                fw.write("""

                0.name=Untrusted User
                0.uploadfiles=false
                0.changeselfusername=false
                0.changeotherusername=false
                0.uploadimages=false
                0.command.createroom=false
                0.command.mute=false
                0.command.kick=false
                0.command.ban=false
                0.command.unban=false
                0.command.unmute=false

                100.name=Owner
                100.uploadfiles=true
                100.changeselfusername=true
                100.changeotherusername=true
                100.uploadimages=true
                100.command.createroom=true
                100.command.mute=true
                100.command.kick=true
                100.command.ban=true
                100.command.unban=true
                100.command.unmute=true
                """);
            }
            System.out.println("Generated permissions.properties");
        }

        // ---------- banned.properties ----------
        File banned = new File("banned.properties");
        if (!banned.exists()) {
            try (FileWriter fw = new FileWriter(banned)) {
                fw.write("# username=true\n");
            }
            System.out.println("Generated banned.properties");
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
    private static void loadServerProperties() throws IOException {
        Properties props = new Properties();

        try (FileInputStream in = new FileInputStream("server.properties")) {
            props.load(in);
        }

        CHAT_NAME = props.getProperty("chat.name", "DefaultChat");
        PORT = Integer.parseInt(props.getProperty("port", "5000"));
        OWNER_USERNAME = props.getProperty("owner", "admin");
        HOST_PASSWORD = props.getProperty("host.password", "changeme");

        System.out.println("Loaded server.properties:");
        System.out.println(" Chat Name: " + CHAT_NAME);
        System.out.println(" Port: " + PORT);
        System.out.println(" Owner: " + OWNER_USERNAME);
        System.out.println(" Lookup Server: " + LOOKUP_HOST + ":" + LOOKUP_PORT + " (hardcoded)");
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

    static Room getRoom(String searchedRoom) {
        if (rooms.containsKey(searchedRoom)) {
            return rooms.get(searchedRoom);
        }
        return null;
    }
    static Room createRoom(String roomName) {
        return new Room(roomName);
    }
}
