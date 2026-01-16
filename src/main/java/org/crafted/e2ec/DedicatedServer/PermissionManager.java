package org.crafted.e2ec.DedicatedServer;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PermissionManager {

    static class PermissionLevel {
        String name;
        Map<String, Boolean> actions = new HashMap<>();
    }

    // level number -> PermissionLevel
    static Map<Integer, PermissionLevel> levels = new HashMap<>();
    static String ownerUsername;

    public static void loadPermissions() {
        // Load owner from server.properties
        Properties serverProps = new Properties();
        try (FileInputStream fis = new FileInputStream("server.properties")) {
            serverProps.load(fis);
            ownerUsername = serverProps.getProperty("owner", "Owner");
        } catch (IOException e) {
            System.out.println("server.properties not found, default owner=Owner");
            ownerUsername = "Owner";
        }

        // Load permission levels from permissions.properties
        Properties permProps = new Properties();
        try (FileInputStream fis = new FileInputStream("permissions.properties")) {
            permProps.load(fis);
            for (String key : permProps.stringPropertyNames()) {
                String[] parts = key.split("\\.");
                if (parts.length != 2) continue;

                int level = Integer.parseInt(parts[0]);
                String action = parts[1];
                boolean value = Boolean.parseBoolean(permProps.getProperty(key));

                PermissionLevel pl = levels.computeIfAbsent(level, k -> new PermissionLevel());
                pl.actions.put(action, value);

                if (action.equals("name")) {
                    pl.name = permProps.getProperty(key);
                }
            }
        } catch (IOException e) {
            System.out.println("permissions.properties not found, using empty defaults");
        }
    }

    
    public static boolean canPerform(String username, int userLevel, String action) {
        // Check if a user has permission for an action
        // input: username, userLevel, and action. obvious naming convention is obvious
        // output: true if user can perform action, false if not
        if (username.equals(ownerUsername)) return true; // owner bypasses all
        PermissionLevel pl = levels.get(userLevel);
        if (pl == null) return false;
        return pl.actions.getOrDefault(action, false);
    }
    public static String getName(int userLevel) {
        //get the name of a permission level
        //input: the permission level number... yeah
        //output: the name of that permission level (or "Unknown" if not found)
        PermissionLevel pl = levels.get(userLevel);
        if (pl == null) return "Unknown";
        return pl.name;
    }
}
