package org.crafted.e2ec.DedicatedServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Room {
    String name;
    Set<ClientHandler> members = ConcurrentHashMap.newKeySet();

    private final Set<Integer> viewingAllowed;
    private final Set<Integer> chattingAllowed;

    private final boolean saveHistory;
    private final boolean broadcastAll;
    private final boolean tagMessagesWithRoom;

    public Room(
        // room constructor
        // just read the names, they're obvious
        String name,
        Set<Integer> viewingAllowed,
        Set<Integer> chattingAllowed,
        boolean saveHistory,
        boolean broadcastAll,
        boolean tagMessagesWithRoom
    ) {
        //the other part of the constructor
        //:thumbs-up:
        this.name = name;
        this.viewingAllowed = viewingAllowed;
        this.chattingAllowed = chattingAllowed;
        this.saveHistory = saveHistory;
        this.broadcastAll = broadcastAll;
        this.tagMessagesWithRoom = tagMessagesWithRoom;
    }

    public String getName() {
        // get the room name
        // input: none
        // output: room name string
        return name;
    }
    public int getMemberCount() {
        // get number of members in the room
        // input: none
        // output: number of members as int
        return members.size();
    }

    public boolean canView(int permissionLevel) {
        // check if a permission level can view the room
        // input: permission level as int
        // output: true if can view, false if not
        return viewingAllowed != null && viewingAllowed.contains(permissionLevel);
    }

    public boolean canChat(int permissionLevel) {
        // ditto but for chatting
        // input: permission level as int
        // output: true if can chat, false if not
        return chattingAllowed != null && chattingAllowed.contains(permissionLevel);
    }


    public boolean shouldBroadcastAll() {
        // check if messages should be broadcast to all clients
        // this method is so stupid
        return broadcastAll;
    }

    public boolean shouldTagMessages() {
        //why not just use the variable directly?
        //shut up me from the past
        return tagMessagesWithRoom;
    }

    public boolean shouldSaveHistory() {
        //not used for some reason because chat history is not implemented yet
        //output what it says on the tin
        return saveHistory;
    }

    /* ---------- helpers ---------- */

    public static Set<Integer> parsePermissionList(String raw) {
        // parse a comma-separated list of permission levels into a Set<Integer>
        // input: raw string
        // output: set of permission levels
        // example: "0,1,100" -> {0, 1, 100} :thumbs-up:
        Set<Integer> set = new HashSet<>();

        if (raw == null || raw.isBlank()) {
            return set;
        }

        for (String s : raw.split(",")) {
            set.add(Integer.parseInt(s.trim()));
        }
        return set;
    }

    static String joinPermissionList(Set<Integer> set) {
        //ditto but backwards
        // input: set of permission levels
        // output: comma-separated string
        // example: {0, 1, 100} -> "0,1,100" :)
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (int i : set) {
            if (!first) sb.append(",");
            sb.append(i);
            first = false;
        }
        return sb.toString();
    }

    static synchronized void saveRoomToProperties(Room room) {
        // something something obvious method name something something
        // input: room object
        // output: none (writes to rooms.properties)
        try {
            File file = new File("rooms.properties");
            Properties props = new Properties();

            if (file.exists()) {
                try (FileInputStream in = new FileInputStream(file)) {
                    props.load(in);
                }
            }

            int index = 0;
            while (props.containsKey(index + ".name")) {
                index++;
            }

            props.setProperty(index + ".name", room.getName());
            props.setProperty(index + ".viewingallowed",
                    joinPermissionList(room.viewingAllowed));
            props.setProperty(index + ".chattingallowed",
                    joinPermissionList(room.chattingAllowed));
            props.setProperty(index + ".savehistory", String.valueOf(room.shouldSaveHistory()));
            props.setProperty(index + ".broadcastall", String.valueOf(room.shouldBroadcastAll()));
            props.setProperty(index + ".tagmessageswithroom", String.valueOf(room.shouldTagMessages()));

            try (FileOutputStream out = new FileOutputStream(file)) {
                props.store(out, "Chat Rooms");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    void broadcast(String message) {
        // broadcast a message to all room members
        // input: message string
        for (ClientHandler c : members) {
            c.send(message);
        }
    }

    void addMember(ClientHandler c) {
        // add a member to the room
        // input: ClientHandler (a client)
        members.add(c);
        broadcast(c.getUsername() + " joined the room.");
    }

    void removeMember(ClientHandler c) {
        //ditto but for removing
        // input: ClientHandler (a client)
        members.remove(c);
        broadcast(c.getUsername() + " left the room.");
    }
}
