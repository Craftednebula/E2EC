package org.crafted.e2ec.DedicatedServer;
// Represents a member of a chat room
// sorta like ClientSession, maybe?
// i dunno
public interface RoomMember {
    void send(String message);
    String getUsername();
    int getPermissionLevel();
}