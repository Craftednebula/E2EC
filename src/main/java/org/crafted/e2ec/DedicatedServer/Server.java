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
        loadRooms();
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
        // ---------- rooms.properties ----------
        File roomsFile = new File("rooms.properties");
        if (!roomsFile.exists()) {
            try (FileWriter fw = new FileWriter(roomsFile)) {
                fw.write("""
                0.name=general
                0.viewingallowed=0,1,100
                0.chattingallowed=0,1,100
                0.savehistory=true
                0.broadcastall=false
                0.tagmessageswithroom=true
                """);
            }
            System.out.println("Generated rooms.properties");
        }

    }
    static void broadcastRoomMessage(Room room, String message) {
        for (ClientHandler c : clients) {

            // Receiver must be allowed to view the room
            if (!room.canView(c.getPermissionLevel())) continue;

            c.send(message);
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
    static void loadRooms() throws IOException {
        Properties props = new Properties();
        File roomsFile = new File("rooms.properties");
        if (!roomsFile.exists()) {
            try (FileWriter fw = new FileWriter(roomsFile)) {
                fw.write("""
                0.name=general
                0.viewingallowed=0,1,100
                0.chattingallowed=0,1,100
                0.savehistory=true
                0.broadcastall=false
                0.tagmessageswithroom=true
                """);
            }
            System.out.println("Generated rooms.properties");
        }
        try (FileInputStream in = new FileInputStream("rooms.properties")) {
            props.load(in);
        }

        rooms.clear();

        int index = 0;
        while (true) {
            String name = props.getProperty(index + ".name");
            if (name == null) break;

            Set<Integer> viewing =
                Room.parsePermissionList(props.getProperty(index + ".viewingallowed"));

            Set<Integer> chatting =
                Room.parsePermissionList(props.getProperty(index + ".chattingallowed"));

            boolean saveHistory =
                Boolean.parseBoolean(props.getProperty(index + ".savehistory", "false"));

            boolean broadcastAll =
                Boolean.parseBoolean(props.getProperty(index + ".broadcastall", "false"));

            boolean tagMessagesWithRoom =
                Boolean.parseBoolean(props.getProperty(index + ".tagmessageswithroom", "true"));

            Room room = new Room(
                name,
                viewing,
                chatting,
                saveHistory,
                broadcastAll,
                tagMessagesWithRoom
            );

            rooms.put(name, room);
            index++;
        }


        System.out.println("Loaded " + rooms.size() + " rooms.");
    }
    static synchronized void saveRooms() {
        Properties props = new Properties();

        int index = 0;
        for (Room room : rooms.values()) {
            props.setProperty(index + ".name", room.getName());
            props.setProperty(index + ".viewingallowed", "0,1,100");
            props.setProperty(index + ".chattingallowed", "0,1,100");
            props.setProperty(index + ".savehistory", "true");
            props.setProperty(index + ".broadcastall", "true");
            props.setProperty(index + ".tagmessageswithroom", "true");
            index++;
        }

        try (FileOutputStream out = new FileOutputStream("rooms.properties")) {
            props.store(out, "E2EC Rooms Configuration");
        } catch (IOException e) {
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

    static Room getRoom(String searchedRoom) {
        if (rooms.containsKey(searchedRoom)) {
            return rooms.get(searchedRoom);
        }
        return null;
    }
    static Room createRoom(
        String name,
        Set<Integer> viewing,
        Set<Integer> chatting,
        boolean saveHistory,
        boolean broadcastAll,
        boolean tag
    ) {
        Room room = new Room(name, viewing, chatting, saveHistory, broadcastAll, tag);
        rooms.put(name, room);
        Room.saveRoomToProperties(room);
        return room;
    }


}
