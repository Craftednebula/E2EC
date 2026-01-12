package org.crafted.e2ec.E2client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

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
}
