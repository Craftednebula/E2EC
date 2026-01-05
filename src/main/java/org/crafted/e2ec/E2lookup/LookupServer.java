package org.crafted.e2ec.E2lookup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class LookupServer {

    static class ChatInfo {
        String ip;
        int port;

        ChatInfo(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
    }

    static ConcurrentHashMap<String, ChatInfo> chats = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(6000);
        System.out.println("Lookup server running on port 6000");

        while (true) {
            Socket socket = server.accept();
            new Thread(() -> handle(socket)).start();
        }
    }

    static void handle(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String line = in.readLine();
            if (line == null) return;

            String[] parts = line.split(" ");
            String cmd = parts[0];

            switch (cmd.toLowerCase()) {
                case "register": // register <chatName> <port>
                    if (parts.length != 3) { out.println("ERROR"); return; }
                    String chatName = parts[1];
                    int port = Integer.parseInt(parts[2]);
                    String ip = socket.getInetAddress().getHostAddress();
                    chats.put(chatName, new ChatInfo(ip, port));
                    out.println("OK");
                    System.out.println("Registered chat " + chatName + " at " + ip + ":" + port);
                    break;

                case "lookup": // lookup <chatName>
                    if (parts.length != 2) { out.println("ERROR"); return; }
                    chatName = parts[1];
                    ChatInfo info = chats.get(chatName);
                    if (info != null) {
                        out.println(info.ip + " " + info.port);
                    } else {
                        out.println("NOT_FOUND");
                    }
                    break;

                default:
                    out.println("UNKNOWN_COMMAND");
            }

        } catch (IOException ignored) {}
    }
}
