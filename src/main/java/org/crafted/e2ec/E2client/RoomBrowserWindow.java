package org.crafted.e2ec.E2client;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class RoomBrowserWindow {

    private final PrintWriter out;
    private final String chatName;
    private final String serverAddr;

    private JFrame frame;
    private DefaultListModel<String> listModel;
    private JList<String> roomList;

    private final Map<String, String> rooms = new HashMap<>();
    private Timer refreshTimer; // Auto-refresh timer

    public RoomBrowserWindow(PrintWriter out, String chatName, String serverAddr) {
        this.out = out;
        this.chatName = chatName;
        this.serverAddr = serverAddr;
    }

    public void show() {
        frame = new JFrame("Rooms");
        frame.setSize(400, 500);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        /* ---------- TOP ---------- */
        JLabel chatLabel = new JLabel("Chat: " + chatName + " (" + serverAddr + ")");
        chatLabel.setFont(chatLabel.getFont().deriveFont(Font.BOLD));

        JLabel roomLabel = new JLabel("Room: none");
        // Disconnect button (new)
        JButton disconnect = new JButton("Disconnect");
        disconnect.addActionListener(e -> {
            try {
                if (out != null) {
                    out.println("/quit");
                }
            } catch (Exception ignored) {}

            // Close this chat window
            close();

            // Open ChatManagerWindow
            SwingUtilities.invokeLater(() -> new ChatManagerWindow().show());
        });
        JPanel labels = new JPanel();
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        labels.add(chatLabel);
        labels.add(roomLabel);

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.add(labels, BorderLayout.WEST);
        top.add(disconnect, BorderLayout.EAST);



        /* ---------- ROOM LIST ---------- */
        listModel = new DefaultListModel<>();
        roomList = new JList<>(listModel);
        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(roomList);

        /* ---------- JOIN BUTTON ---------- */
        JButton join = new JButton("Join Selected Room");
        join.addActionListener(e -> joinSelected());

        roomList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) joinSelected();
            }
        });

        /* ---------- LAYOUT ---------- */
        frame.setLayout(new BorderLayout(5, 5));
        frame.add(top, BorderLayout.NORTH);
        frame.add(scroll, BorderLayout.CENTER);
        frame.add(join, BorderLayout.SOUTH);

        frame.setVisible(true);

        // Request initial room list
        requestRoomList();

        // Start auto-refresh every 5 seconds
        refreshTimer = new Timer(5000, e -> requestRoomList());
        refreshTimer.start();

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (refreshTimer != null) refreshTimer.stop();
            }
        });
    }

    private void joinSelected() {
        String selected = roomList.getSelectedValue();
        if (selected == null) return;

        // Extract room name
        String roomName = selected.split(" ")[0];
        out.println("/join " + roomName);

        // Stop auto-refresh when joining a room
        if (refreshTimer != null) refreshTimer.stop();
    }

    /** Add or update a room line dynamically :^) */
    public void updateRoom(String roomLine) {
        SwingUtilities.invokeLater(() -> {
            // Extract room name
            String roomName = roomLine.split(" ")[0];

            // Update map
            rooms.put(roomName, roomLine);

            // Refresh model without clearing selection
            int selectedIndex = roomList.getSelectedIndex();
            listModel.clear();
            rooms.values().forEach(listModel::addElement);
            if (selectedIndex >= 0 && selectedIndex < listModel.size()) {
                roomList.setSelectedIndex(selectedIndex);
            }
        });
    }

    /** Close the window safely */
    public void close() {
        SwingUtilities.invokeLater(() -> {
            if (refreshTimer != null) refreshTimer.stop();
            frame.dispose();
        });
    }

    /** Ask the server for the current room list */
    public void requestRoomList() {
        if (out != null) {
            out.println("/rooms");
        }
    }
}
