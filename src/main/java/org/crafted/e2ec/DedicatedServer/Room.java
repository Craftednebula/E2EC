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

    private final String name;
    private final Set<RoomMember> members = ConcurrentHashMap.newKeySet();

    private final Set<Integer> viewingAllowed;
    private final Set<Integer> chattingAllowed;

    private final boolean saveHistory;
    private final boolean broadcastAll;
    private final boolean tagMessagesWithRoom;

    public Room(
        String name,
        Set<Integer> viewingAllowed,
        Set<Integer> chattingAllowed,
        boolean saveHistory,
        boolean broadcastAll,
        boolean tagMessagesWithRoom
    ) {
        this.name = name;
        this.viewingAllowed = viewingAllowed;
        this.chattingAllowed = chattingAllowed;
        this.saveHistory = saveHistory;
        this.broadcastAll = broadcastAll;
        this.tagMessagesWithRoom = tagMessagesWithRoom;
    }
    /*---------- getters ----------*/
    public String getName() {
        return name;
    }

    public int getMemberCount() {
        return members.size();
    }

    public boolean canView(int permissionLevel) {
        return viewingAllowed != null && viewingAllowed.contains(permissionLevel);
    }

    public boolean canChat(int permissionLevel) {
        return chattingAllowed != null && chattingAllowed.contains(permissionLevel);
    }

    public boolean shouldBroadcastAll() {
        return broadcastAll;
    }

    public boolean shouldTagMessages() {
        return tagMessagesWithRoom;
    }

    public boolean shouldSaveHistory() {
        return saveHistory;
    }

    /* ---------- helpers ---------- */

    public static Set<Integer> parsePermissionList(String raw) {
        // parses a comma-separated list of integers into a Set<Integer>
        // e.g. "1,2,3" -> {1, 2, 3}
        // input : raw string separated by commas
        // output: set of integers
        Set<Integer> set = new HashSet<>();

        if (raw == null || raw.isBlank()) return set;

        for (String s : raw.split(",")) {
            set.add(Integer.parseInt(s.trim()));
        }
        return set;
    }

    static String joinPermissionList(Set<Integer> set) {
        // joins a Set<Integer> into a comma-separated string
        // e.g. {1, 2, 3} -> "1,2,3"
        // input : set of integers
        // output: raw string separated by commas
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
        // saves the room configuration to rooms.properties
        // input : Room object
        // output: none (writes to file)
        try {
            File file = new File("rooms.properties");
            Properties props = new Properties();

            if (file.exists()) {
                try (FileInputStream in = new FileInputStream(file)) {
                    props.load(in);
                }
            }

            int index = 0;
            while (props.containsKey(index + ".name")) index++;

            props.setProperty(index + ".name", room.getName());
            props.setProperty(index + ".viewingallowed",
                    joinPermissionList(room.viewingAllowed));
            props.setProperty(index + ".chattingallowed",
                    joinPermissionList(room.chattingAllowed));
            props.setProperty(index + ".savehistory",
                    String.valueOf(room.shouldSaveHistory()));
            props.setProperty(index + ".broadcastall",
                    String.valueOf(room.shouldBroadcastAll()));
            props.setProperty(index + ".tagmessageswithroom",
                    String.valueOf(room.shouldTagMessages()));

            try (FileOutputStream out = new FileOutputStream(file)) {
                props.store(out, "Chat Rooms");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* ---------- room logic ---------- */

    void broadcast(String message) {
        // sends a message to all members in the room
        // input : message string
        // output: none
        for (RoomMember m : members) {
            m.send(message);
        }
    }

    void addMember(RoomMember m) {
        // adds a member to the room
        // input : RoomMember object
        // output: none
        members.add(m);
        broadcast(m.getUsername() + " joined the room.");
    }

    void removeMember(RoomMember m) {
        // ditto, but removes
        // input : RoomMember object
        // output: none
        members.remove(m);
        broadcast(m.getUsername() + " left the room.");
    }
}
