package org.crafted.e2ec.DedicatedServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    // All connected sessions
    static final Set<ClientSession> clients = ConcurrentHashMap.newKeySet();
    static final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    static String HOST_PASSWORD;
    static int PORT;
    static String BIND_IP;
    static String CHAT_NAME;
    static String OWNER_USERNAME;

    static UserManager userManager;

    public static void main(String[] args) throws IOException {
        ensureConfigFiles();
        loadServerProperties();
        loadRooms();

        PermissionManager.loadPermissions();

        try {
            userManager = new UserManager("chat.db");
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return;
        }

        ServerSocket serverSocket =
                new ServerSocket(PORT, 50, java.net.InetAddress.getByName(BIND_IP));

        System.out.println("Chat server running on port " + PORT);

        while (true) {
            Socket socket = serverSocket.accept();

            // wrap socket in concrete ClientIO
            ClientIO io = new SocketClientIO(socket);

            // create concrete server facade
            ServerFacade serverFacade = new ServerFacadeImpl();

            // create session
            ClientSession session = new ClientSession(io, HOST_PASSWORD, userManager, serverFacade);

            // add session (client)
            clients.add(session);

            // start session in new thread
            new Thread(() -> {
                // this might be horrible for cpu usage
                try {
                    session.start();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    serverFacade.removeClient(session);
                }
            }).start();

        }
    }

    /* ================= ROOM & CHAT ================= */

    static void broadcastRoomMessage(Room room, String message) {
        // send message to all clients in the room who can view it
        // input : Room object, message string
        // output: none
        for (RoomMember m : clients) {
            if (!room.canView(m.getPermissionLevel())) continue;
            m.send(message);
        }
    }

    static void broadcast(String message, ClientSession sender) {
        // send message to all connected clients
        // input : message string, sender ClientSession
        // output: none
        for (RoomMember m : clients) {
            m.send(message);
        }
    }

    public static void removeClient(ClientSession session) {
        // removes a client from the server
        // input : ClientSession object
        // output: none
        clients.remove(session);
        if (session.getCurrentRoom() != null) {
            session.getCurrentRoom().removeMember(session);
        }
        if (session.getUsername() != null) {
            broadcast(session.getUsername() + " left the chat.", session);
        }
    }


    static Room getRoom(String searchedRoom) {
        //room getter
        return rooms.get(searchedRoom);
    }

    static Room createRoom(
            String name,
            Set<Integer> viewing,
            Set<Integer> chatting,
            boolean saveHistory,
            boolean broadcastAll,
            boolean tag
    ) {
        // creates a new room and adds it to the server
        // input : room parameters
        // output: created Room object
        Room room = new Room(name, viewing, chatting, saveHistory, broadcastAll, tag);
        rooms.put(name, room);
        Room.saveRoomToProperties(room);
        return room;
    }

    /* ----------------- config files ----------------- */

    private static void ensureConfigFiles() throws IOException {
        // create default config files if they don't exist
        // ---------- server.properties ----------
        File file = new File("server.properties");

        if (!file.exists()) {
            Properties props = new Properties();
            props.setProperty("chat.name", "CoolRoom");
            props.setProperty("bind.ip", "0.0.0.0"); // listen on all interfaces by default
            props.setProperty("port", "5000");
            props.setProperty("owner", "admin");
            props.setProperty("host.password", "secret123");



            try (FileOutputStream out = new FileOutputStream(file)) {
                props.store(out, "E2EC Dedicated Server Configuration");
            }

            System.out.println("Generated server.properties");
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

    /* ---------------- loading ---------------- */

    private static void loadServerProperties() throws IOException {
        // load server.properties... cough
        Properties props = new Properties();

        try (FileInputStream in = new FileInputStream("server.properties")) {
            props.load(in);
        }

        CHAT_NAME = props.getProperty("chat.name", "DefaultChat");
        BIND_IP = props.getProperty("bind.ip", "0.0.0.0");
        PORT = Integer.parseInt(props.getProperty("port", "5000"));
        OWNER_USERNAME = props.getProperty("owner", "admin");
        HOST_PASSWORD = props.getProperty("host.password", "changeme");


        System.out.println("Loaded server.properties:");
        System.out.println(" Chat Name: " + CHAT_NAME);
        System.out.println(" Bind IP: " + BIND_IP);
        System.out.println(" Port: " + PORT);
        System.out.println(" Owner: " + OWNER_USERNAME);
    }

    static void loadRooms() throws IOException {
        // load rooms from rooms.properties 
        // what did you expect?
        Properties props = new Properties();

        try (FileInputStream in = new FileInputStream("rooms.properties")) {
            props.load(in);
        }

        rooms.clear();

        int index = 0;
        while (true) {
            String name = props.getProperty(index + ".name");
            if (name == null) break;

            Room room = new Room(
                name,
                Room.parsePermissionList(props.getProperty(index + ".viewingallowed")),
                Room.parsePermissionList(props.getProperty(index + ".chattingallowed")),
                Boolean.parseBoolean(props.getProperty(index + ".savehistory", "false")),
                Boolean.parseBoolean(props.getProperty(index + ".broadcastall", "false")),
                Boolean.parseBoolean(props.getProperty(index + ".tagmessageswithroom", "true"))
            );

            rooms.put(name, room);
            index++;
        }

        System.out.println("Loaded " + rooms.size() + " rooms.");
    }
}
