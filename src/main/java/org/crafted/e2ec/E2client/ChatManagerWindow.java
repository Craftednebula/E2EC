package org.crafted.e2ec.E2client;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

public class ChatManagerWindow {

    private JFrame frame;
    private DefaultListModel<String> chatListModel;
    private JList<String> chatList;
    private Map<String, ChatEntry> savedChats = new LinkedHashMap<>();
    private final File chatFile = new File("chats.properties");

    public static class ChatEntry {
        String name;
        String address;
        String hostPassword;
        String username;
        String userPassword;

        public ChatEntry(String name, String address, String hostPassword, String username, String userPassword) {
            this.name = name;
            this.address = address;
            this.hostPassword = hostPassword;
            this.username = username;
            this.userPassword = userPassword;
        }
    }

    public ChatManagerWindow() {
        loadChats();
    }

    public void show() {
        frame = new JFrame("Chat Manager");
        frame.setSize(500, 400);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chatListModel = new DefaultListModel<>();
        chatList = new JList<>(chatListModel);
        chatList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        refreshList();

        JScrollPane scroll = new JScrollPane(chatList);

        /* ---------- BUTTONS ---------- */
        JButton newBtn = new JButton("New Chat");
        JButton editBtn = new JButton("Edit Chat");
        JButton removeBtn = new JButton("Remove Chat");
        JButton connectBtn = new JButton("Connect");
        JButton directBtn = new JButton("Direct Connect");

        newBtn.addActionListener(e -> addOrEditChat(null));
        editBtn.addActionListener(e -> {
            String selected = chatList.getSelectedValue();
            if (selected != null) addOrEditChat(savedChats.get(selected));
        });
        removeBtn.addActionListener(e -> {
            String selected = chatList.getSelectedValue();
            if (selected != null) {
                savedChats.remove(selected);
                saveChats();
                refreshList();
            }
        });
        connectBtn.addActionListener(e -> {
            String selected = chatList.getSelectedValue();
            if (selected != null) connectToChat(savedChats.get(selected));
        });
        directBtn.addActionListener(e -> directConnect());

        JPanel buttons = new JPanel();
        buttons.add(newBtn);
        buttons.add(editBtn);
        buttons.add(removeBtn);
        buttons.add(connectBtn);
        buttons.add(directBtn);

        frame.setLayout(new BorderLayout(5, 5));
        frame.add(scroll, BorderLayout.CENTER);
        frame.add(buttons, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void refreshList() {
        chatListModel.clear();
        for (String name : savedChats.keySet()) {
            chatListModel.addElement(name);
        }
    }

    private void addOrEditChat(ChatEntry entry) {
        JTextField nameField = new JTextField(entry != null ? entry.name : "");
        JTextField addressField = new JTextField(entry != null ? entry.address : "");
        JTextField hostPassField = new JTextField(entry != null ? entry.hostPassword : "");
        JTextField userField = new JTextField(entry != null ? entry.username : "");
        JTextField userPassField = new JPasswordField(entry != null ? entry.userPassword : "");

        JPanel panel = new JPanel(new GridLayout(5, 2));
        panel.add(new JLabel("Chat Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Address (ip:port):"));
        panel.add(addressField);
        panel.add(new JLabel("Host Password:"));
        panel.add(hostPassField);
        panel.add(new JLabel("Username:"));
        panel.add(userField);
        panel.add(new JLabel("User Password:"));
        panel.add(userPassField);

        int result = JOptionPane.showConfirmDialog(frame, panel,
                entry == null ? "New Chat" : "Edit Chat",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            if (name.isEmpty()) return;
            ChatEntry newEntry = new ChatEntry(
                    name,
                    addressField.getText().trim(),
                    hostPassField.getText().trim(),
                    userField.getText().trim(),
                    userPassField.getText().trim()
            );
            savedChats.put(name, newEntry);
            saveChats();
            refreshList();
        }
    }

    private void connectToChat(ChatEntry entry) {
        frame.dispose();
        try {
            ChatClient client = new ChatClient();
            client.startWithSavedChat(entry);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Failed to connect: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }


    private void directConnect() {
        frame.dispose();
        try {
            ChatClient client = new ChatClient();
            client.start(); // existing direct connect dialogs
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Failed to connect: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadChats() {
        if (!chatFile.exists()) return;

        try (InputStream in = new FileInputStream(chatFile)) {
            Properties props = new Properties();
            props.load(in);
            for (String key : props.stringPropertyNames()) {
                String[] parts = props.getProperty(key).split(";", -1);
                if (parts.length != 4) continue;
                savedChats.put(key, new ChatEntry(key, parts[0], parts[1], parts[2], parts[3]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveChats() {
        Properties props = new Properties();
        for (ChatEntry entry : savedChats.values()) {
            props.setProperty(entry.name,
                    entry.address + ";" + entry.hostPassword + ";" + entry.username + ";" + entry.userPassword);
        }
        try (FileOutputStream out = new FileOutputStream(chatFile)) {
            props.store(out, "Saved Chats");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // tests
    private static boolean testChatEntryCreation() {
        System.out.print("[TEST] ChatEntry creation ... ");

        try {
            ChatEntry entry = new ChatEntry(
                    "TestChat",
                    "127.0.0.1:1234",
                    "hostPass",
                    "user",
                    "pass"
            );

            if (!entry.name.equals("TestChat")) throw new AssertionError();
            if (!entry.address.equals("127.0.0.1:1234")) throw new AssertionError();
            if (!entry.hostPassword.equals("hostPass")) throw new AssertionError();
            if (!entry.username.equals("user")) throw new AssertionError();
            if (!entry.userPassword.equals("pass")) throw new AssertionError();

            System.out.println("PASS");
            return true;

        } catch (Throwable t) {
            System.out.println("FAIL");
            t.printStackTrace();
            return false;
        }
    }
private static boolean testSaveAndLoadChats() {
    System.out.print("[TEST] saveChats() / loadChats() ... ");

    try {
        ChatManagerWindow window = new ChatManagerWindow();

        File tempFile = File.createTempFile("chat-test", ".properties");
        tempFile.deleteOnExit();

        // inject test file
        java.lang.reflect.Field fileField =
                ChatManagerWindow.class.getDeclaredField("chatFile");
        fileField.setAccessible(true);
        fileField.set(window, tempFile);

        // prepare test data
        window.savedChats.clear();
        window.savedChats.put("A",
                new ChatEntry("A", "addr1", "hp1", "u1", "p1"));
        window.savedChats.put("B",
                new ChatEntry("B", "addr2", "hp2", "u2", "p2"));

        window.saveChats();

        // ===== LOAD PHASE =====
        ChatManagerWindow loaded = new ChatManagerWindow();

        // inject same temp file
        fileField.set(loaded, tempFile);

        loaded.savedChats.clear();
        loaded.loadChats();

        if (loaded.savedChats.size() != 2)
            throw new AssertionError("Incorrect number of chats loaded");

        if (!loaded.savedChats.containsKey("A"))
            throw new AssertionError("Missing chat A");

        if (!loaded.savedChats.containsKey("B"))
            throw new AssertionError("Missing chat B");

        System.out.println("PASS");
        return true;

    } catch (Throwable t) {
        System.out.println("FAIL");
        t.printStackTrace();
        return false;
    }
}

    private static boolean testConstructorLoadsChats() {
        System.out.print("[TEST] constructor loads chats ... ");

        try {
            File tempFile = File.createTempFile("chat-test", ".properties");
            tempFile.deleteOnExit();

            Properties props = new Properties();
            props.setProperty("X", "addr;hp;u;p");

            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                props.store(out, "test");
            }

            ChatManagerWindow window = new ChatManagerWindow();

            java.lang.reflect.Field fileField =
                    ChatManagerWindow.class.getDeclaredField("chatFile");
            fileField.setAccessible(true);
            fileField.set(window, tempFile);

            window.loadChats();

            if (!window.savedChats.containsKey("X"))
                throw new AssertionError("Chat not loaded");

            System.out.println("PASS");
            return true;

        } catch (Throwable t) {
            System.out.println("FAIL");
            t.printStackTrace();
            return false;
        }
    }

    private static boolean testRefreshList() {
        System.out.print("[TEST] refreshList() ... ");

        try {
            ChatManagerWindow window = new ChatManagerWindow();

            window.chatListModel = new DefaultListModel<>();
            window.savedChats.clear();

            window.savedChats.put("Chat1",
                    new ChatEntry("Chat1", "", "", "", ""));
            window.savedChats.put("Chat2",
                    new ChatEntry("Chat2", "", "", "", ""));

            window.refreshList();

            if (window.chatListModel.size() != 2)
                throw new AssertionError("List size mismatch");

            if (!window.chatListModel.contains("Chat1"))
                throw new AssertionError("Chat1 missing");

            if (!window.chatListModel.contains("Chat2"))
                throw new AssertionError("Chat2 missing");

            System.out.println("PASS");
            return true;

        } catch (Throwable t) {
            System.out.println("FAIL");
            t.printStackTrace();
            return false;
        }
    }
    public static void runAllTests() {
    System.out.println("ChatManagerWindow tests");

    int passed = 0;
    int failed = 0;

    if (testChatEntryCreation()) passed++; else failed++;
    if (testSaveAndLoadChats()) passed++; else failed++;
    if (testRefreshList()) passed++; else failed++;
    if (testConstructorLoadsChats()) passed++; else failed++;

    System.out.println();
    System.out.println("test summary");
    System.out.println("Passed: " + passed);
    System.out.println("Failed: " + failed);
}

}
