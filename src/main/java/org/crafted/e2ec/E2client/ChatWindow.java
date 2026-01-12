package org.crafted.e2ec.E2client;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;

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
}
