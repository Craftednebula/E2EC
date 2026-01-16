package org.crafted.e2ec.E2client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class ChatWindow {

    private final JTextArea chatArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final PrintWriter out;

    private JFrame frame;
    private JLabel chatLabel;
    private JLabel roomLabel;


    private final String chatName;

    private final String connectionInfo;

    public ChatWindow(PrintWriter out, String chatName, String connectionInfo) {
        this.out = out;
        this.chatName = chatName;
        this.connectionInfo = connectionInfo;
    }


    public void show() {
        // creates and shows the chat window (where the user chats and sees messages)
        frame = new JFrame("E2EC Chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 600);

        /* ---------- TOP BAR ---------- */
        chatLabel = new JLabel("Chat: " + chatName + " (" + connectionInfo + ")");
        chatLabel.setFont(chatLabel.getFont().deriveFont(Font.BOLD));

        roomLabel = new JLabel("Room: none");

        JButton leaveRoom = new JButton("Leave Room");
        leaveRoom.addActionListener(e -> out.println("/leave"));

        JPanel labels = new JPanel();
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        labels.add(chatLabel);
        labels.add(roomLabel);

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.add(labels, BorderLayout.WEST);
        top.add(leaveRoom, BorderLayout.EAST);


        /* ---------- CHAT ---------- */
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(chatArea);

        /* ---------- INPUT ---------- */
        JButton sendButton = new JButton("Send");
        sendButton.setPreferredSize(new Dimension(80, 30));

        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);

        frame.setLayout(new BorderLayout(5, 5));
        frame.add(top, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);

        Runnable sendAction = () -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                out.println(text);
                inputField.setText("");
            }
        };

        sendButton.addActionListener(e -> sendAction.run());
        inputField.addActionListener(e -> sendAction.run());

        frame.setVisible(true);
    }

    public void setRoom(String roomName) {
        // sets the current room name in the chat window
        // input: room name
        // output: none (sets room label)
        SwingUtilities.invokeLater(() ->
                roomLabel.setText("Room: " + roomName)
        );
    }

    public void clearRoom() {
        // clears the current room name in the chat window
        SwingUtilities.invokeLater(() ->
                roomLabel.setText("Room: none")
        );
    }
    public void close() {
        // closes the chat window
        SwingUtilities.invokeLater(() -> frame.dispose());
    }

    public void append(String msg) {
        // appends a message to the chat area
        SwingUtilities.invokeLater(() -> {
            chatArea.append(msg + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }
    //tests
    public static void runAllTests() {
        System.out.println("ChatWindow tests");

        int passed = 0;
        int failed = 0;

        if (testShowWindow()) passed++; else failed++;
        if (testSetRoom()) passed++; else failed++;
        if (testClearRoom()) passed++; else failed++;
        if (testAppendMessage()) passed++; else failed++;
        if (testSendAction()) passed++; else failed++;
        if (testCloseWindow()) passed++; else failed++;

        System.out.println();
        System.out.println("test summary");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
    }
    private static boolean testShowWindow() {
        System.out.print("[TEST] show() initializes window ... ");

        try {
            ChatWindow window = new ChatWindow(
                    new PrintWriter(System.out, true),
                    "Tester",
                    "localhost:1234"
            );

            SwingUtilities.invokeAndWait(window::show);

            if (window.frame == null)
                throw new AssertionError("Frame not created");

            if (!window.frame.isVisible())
                throw new AssertionError("Frame not visible");

            System.out.println("PASS");
            return true;

        } catch (Throwable t) {
            System.out.println("FAIL");
            t.printStackTrace();
            return false;
        }
    }
    private static boolean testSetRoom() {
        System.out.print("[TEST] setRoom() ... ");

        try {
            ChatWindow window = new ChatWindow(
                    new PrintWriter(System.out, true),
                    "Tester",
                    "localhost"
            );

            SwingUtilities.invokeAndWait(window::show);
            window.setRoom("RoomA");

            SwingUtilities.invokeAndWait(() -> {});

            if (!window.roomLabel.getText().equals("Room: RoomA"))
                throw new AssertionError("Room label incorrect");

            System.out.println("PASS");
            return true;

        } catch (Throwable t) {
            System.out.println("FAIL");
            t.printStackTrace();
            return false;
        }
    }
    private static boolean testClearRoom() {
        System.out.print("[TEST] clearRoom() ... ");

        try {
            ChatWindow window = new ChatWindow(
                    new PrintWriter(System.out, true),
                    "Tester",
                    "localhost"
            );

            SwingUtilities.invokeAndWait(window::show);
            window.setRoom("RoomA");
            window.clearRoom();

            SwingUtilities.invokeAndWait(() -> {});

            if (!window.roomLabel.getText().equals("Room: none"))
                throw new AssertionError("Room not cleared");

            System.out.println("PASS");
            return true;

        } catch (Throwable t) {
            System.out.println("FAIL");
            t.printStackTrace();
            return false;
        }
    }
    private static boolean testAppendMessage() {
        System.out.print("[TEST] append() ... ");

        try {
            ChatWindow window = new ChatWindow(
                    new PrintWriter(System.out, true),
                    "Tester",
                    "localhost"
            );

            SwingUtilities.invokeAndWait(window::show);
            window.append("Hello");
            window.append("World");

            SwingUtilities.invokeAndWait(() -> {});

            String text = window.chatArea.getText();

            if (!text.contains("Hello\n"))
                throw new AssertionError("Missing message");

            if (!text.endsWith("World\n"))
                throw new AssertionError("Caret not moved");

            System.out.println("PASS");
            return true;

        } catch (Throwable t) {
            System.out.println("FAIL");
            t.printStackTrace();
            return false;
        }
    }
    private static boolean testSendAction() {
        System.out.print("[TEST] send action ... ");

        try {
            StringWriter writer = new StringWriter();
            PrintWriter out = new PrintWriter(writer, true);

            ChatWindow window = new ChatWindow(out, "Tester", "localhost");

            SwingUtilities.invokeAndWait(window::show);

            window.inputField.setText("Test message");
            window.inputField.postActionEvent();

            SwingUtilities.invokeAndWait(() -> {});

            if (!writer.toString().contains("Test message"))
                throw new AssertionError("Message not sent");

            if (!window.inputField.getText().isEmpty())
                throw new AssertionError("Input not cleared");

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
            ChatWindow window = new ChatWindow(
                    new PrintWriter(System.out, true),
                    "Tester",
                    "localhost"
            );

            SwingUtilities.invokeAndWait(window::show);
            window.close();

            SwingUtilities.invokeAndWait(() -> {});

            if (window.frame.isDisplayable())
                throw new AssertionError("Frame not disposed");

            System.out.println("PASS");
            return true;

        } catch (Throwable t) {
            System.out.println("FAIL");
            t.printStackTrace();
            return false;
        }
    }

}
