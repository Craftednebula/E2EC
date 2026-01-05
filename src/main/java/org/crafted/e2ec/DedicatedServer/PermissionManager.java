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

    // Check if a user has permission for an action
    public static boolean canPerform(String username, int userLevel, String action) {
        if (username.equals(ownerUsername)) return true; // owner bypasses all
        PermissionLevel pl = levels.get(userLevel);
        if (pl == null) return false;
        return pl.actions.getOrDefault(action, false);
    }
}
