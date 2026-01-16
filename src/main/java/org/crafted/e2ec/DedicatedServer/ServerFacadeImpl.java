package org.crafted.e2ec.DedicatedServer;

import java.util.Set;
// Facade implementation for server operations
// this is stupid, like entirely.
// but it makes testing easier, so whatever
public class ServerFacadeImpl implements ServerFacade {

    public ServerFacadeImpl() {
        // does nothing, all server methods are static
        // except when i remove this it breaks everything
        // for some reason
    }

    @Override
    public void removeClient(ClientSession session) {
        Server.removeClient(session); // static call
    }

    @Override
    public void broadcast(String message, ClientSession sender) {
        Server.broadcast(message, sender);
    }

    @Override
    public void broadcastRoomMessage(Room room, String message) {
        Server.broadcastRoomMessage(room, message);
    }

    @Override
    public Room getRoom(String roomName) {
        return Server.getRoom(roomName);
    }

    @Override
    public Room createRoom(String name, Set<Integer> viewing, Set<Integer> chatting,
                           boolean saveHistory, boolean broadcastAll, boolean tag) {
        return Server.createRoom(name, viewing, chatting, saveHistory, broadcastAll, tag);
    }
    @Override
    public boolean setPermissionLevel(String username, int newLevel) {
        return Server.userManager.setPermissionLevel(username, newLevel);
    }

    @Override
    public UserManager.User getUser(String username) {
        return Server.userManager.getUser(username);
    }

    @Override
    public ClientSession findUser(String username) {
        for (ClientSession s : Server.clients) {
            if (username.equalsIgnoreCase(s.getUsername())) return s;
        }
        return null;
    }

}
