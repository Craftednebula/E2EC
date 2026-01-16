package org.crafted.e2ec.DedicatedServer;
import java.util.Set;
// Facade interface for server operations
// Provides an abstraction layer over static Server methods
// haha i am the evil server facade muahahaha
public interface ServerFacade {
    void broadcastRoomMessage(Room room, String message);
    void broadcast(String message, ClientSession sender);
    void removeClient(ClientSession session);
    Room getRoom(String name);
    Room createRoom(String name, Set<Integer> viewing, Set<Integer> chatting,
                    boolean saveHistory, boolean broadcastAll, boolean tag);
    boolean setPermissionLevel(String username, int newLevel);

    // get a UserManager user by username
    UserManager.User getUser(String username);

    // find a ClientSession by username
    ClientSession findUser(String username);

    
}
