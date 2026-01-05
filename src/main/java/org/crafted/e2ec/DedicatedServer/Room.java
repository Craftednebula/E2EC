package org.crafted.e2ec.DedicatedServer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Room {
    String name;
    Set<ClientHandler> members = ConcurrentHashMap.newKeySet();

    Room(String name) {
        this.name = name;
    }

    void broadcast(String message) {
        for (ClientHandler c : members) {
            c.send(message);
        }
    }

    void addMember(ClientHandler c) {
        members.add(c);
        broadcast(c.getUsername() + " joined the room.");
    }

    void removeMember(ClientHandler c) {
        members.remove(c);
        broadcast(c.getUsername() + " left the room.");
    }
}
