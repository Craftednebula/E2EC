package org.crafted.e2ec.E2client;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.PrintWriter;
import java.io.StringWriter;
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
    //tests
    public static void runAllTests() {
        System.out.println("RoomBrowserWindow tests");

        int passed = 0;
        int failed = 0;

        if (testShowWindow()) passed++; else failed++;
        if (testRequestRoomList()) passed++; else failed++;
        if (testUpdateRoom()) passed++; else failed++;
        if (testJoinSelected()) passed++; else failed++;
        if (testCloseWindow()) passed++; else failed++;

        System.out.println();
        System.out.println("test summary");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
    }
    private static boolean testShowWindow() {
        System.out.print("[TEST] show() initializes window ... ");

        try {
            RoomBrowserWindow window = new RoomBrowserWindow(
                    new PrintWriter(System.out, true),
                    "Tester",
                    "localhost:1234"
            );

            SwingUtilities.invokeAndWait(window::show);

            if (window.frame == null)
                throw new AssertionError("Frame not created");

            if (!window.frame.isVisible())
                throw new AssertionError("Frame not visible");

            if (window.refreshTimer == null || !window.refreshTimer.isRunning())
                throw new AssertionError("Refresh timer not running");

            System.out.println("PASS");
            return true;

        } catch (Throwable t) {
            System.out.println("FAIL");
            t.printStackTrace();
            return false;
        }
    }
    private static boolean testRequestRoomList() {
        System.out.print("[TEST] requestRoomList() ... ");

        try {
            StringWriter writer = new StringWriter();
            PrintWriter out = new PrintWriter(writer, true);

            RoomBrowserWindow window = new RoomBrowserWindow(out, "Tester", "localhost");

            window.requestRoomList();

            if (!writer.toString().contains("/rooms"))
                throw new AssertionError("Did not send /rooms command");

            System.out.println("PASS");
            return true;

        } catch (Throwable t) {
            System.out.println("FAIL");
            t.printStackTrace();
            return false;
        }
    }
    private static boolean testUpdateRoom() {
        System.out.print("[TEST] updateRoom() ... ");

        try {
            RoomBrowserWindow window = new RoomBrowserWindow(
                    new PrintWriter(System.out, true),
                    "Tester",
                    "localhost"
            );

            SwingUtilities.invokeAndWait(window::show);

            window.updateRoom("RoomA (2 users)");
            window.updateRoom("RoomB (5 users)");

            SwingUtilities.invokeAndWait(() -> {});

            if (window.listModel.size() != 2)
                throw new AssertionError("Incorrect room count");

            if (!window.listModel.contains("RoomA (2 users)"))
                throw new AssertionError("RoomA missing");

            if (!window.listModel.contains("RoomB (5 users)"))
                throw new AssertionError("RoomB missing");

            System.out.println("PASS");
            return true;

        } catch (Throwable t) {
            System.out.println("FAIL");
            t.printStackTrace();
            return false;
        }
    }
    private static boolean testJoinSelected() {
        System.out.print("[TEST] joinSelected() ... ");

        try {
            StringWriter writer = new StringWriter();
            PrintWriter out = new PrintWriter(writer, true);

            RoomBrowserWindow window = new RoomBrowserWindow(out, "Tester", "localhost");

            SwingUtilities.invokeAndWait(window::show);

            window.updateRoom("RoomA (3 users)");
            SwingUtilities.invokeAndWait(() -> {});

            window.roomList.setSelectedIndex(0);

            // invoke private method via reflection
            var m = RoomBrowserWindow.class.getDeclaredMethod("joinSelected");
            m.setAccessible(true);
            m.invoke(window);

            if (!writer.toString().contains("/join RoomA"))
                throw new AssertionError("Join command not sent");

            if (window.refreshTimer != null && window.refreshTimer.isRunning())
                throw new AssertionError("Refresh timer not stopped");

            System.out.println("PASS");
            return true;

        } catch (Throwable t) {
            System.out.println("FAIL");
            t.printStackTrace();
            return false;
        }
    }
    private static boolean testCloseWindow() {
        System.out.print("[TEST] close() ... ");

        try {
            RoomBrowserWindow window = new RoomBrowserWindow(
                    new PrintWriter(System.out, true),
                    "Tester",
                    "localhost"
            );

            SwingUtilities.invokeAndWait(window::show);
            window.close();

            SwingUtilities.invokeAndWait(() -> {});

            if (window.frame.isDisplayable())
                throw new AssertionError("Frame not disposed");

            if (window.refreshTimer != null && window.refreshTimer.isRunning())
                throw new AssertionError("Timer not stopped");

            System.out.println("PASS");
            return true;

        } catch (Throwable t) {
            System.out.println("FAIL");
            t.printStackTrace();
            return false;
        }
    }

}
