package org.crafted.e2ec.E2client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {

    public static void main(String[] args) throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("Enter chat name: ");
        String chatName = console.readLine();

        // 1️⃣ Ask lookup server
        Socket lookup = new Socket("127.0.0.1", 6000);
        PrintWriter lookupOut = new PrintWriter(lookup.getOutputStream(), true);
        BufferedReader lookupIn = new BufferedReader(new InputStreamReader(lookup.getInputStream()));

        lookupOut.println("lookup " + chatName);
        String resp = lookupIn.readLine();
        if (resp.equals("NOT_FOUND")) {
            System.out.println("Chat not found!");
            lookup.close();
            return;
        }
        lookup.close();

        String[] parts = resp.split(" ");
        String hostIp = parts[0];
        int hostPort = Integer.parseInt(parts[1]);

        // 2️⃣ Connect to chat server
        Socket chatSocket = new Socket(hostIp, hostPort);
        PrintWriter out = new PrintWriter(chatSocket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));

        // 3️⃣ Read alias prompt
        System.out.println(in.readLine());
        String alias = console.readLine();
        out.println(alias);

        // 4️⃣ Read password prompt
        System.out.println(in.readLine());
        String password = console.readLine();
        out.println(password);

        String joinResp = in.readLine();
        System.out.println(joinResp);
        if (!joinResp.startsWith("OK")) {
            chatSocket.close();
            return;
        }

        // 5️⃣ Start chat loop
        Thread reader = new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println(msg);
                }
            } catch (IOException ignored) {}
        });
        reader.start();

        String line;
        while ((line = console.readLine()) != null) {
            out.println(line);
        }
    }
}
