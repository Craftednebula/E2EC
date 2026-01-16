package org.crafted.e2ec.DedicatedServer;

public interface RoomMember {
    void send(String message);
    String getUsername();
    int getPermissionLevel();
}