package org.crafted.e2ec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {

    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("localhost", 5000);

        BufferedReader console =
                new BufferedReader(new InputStreamReader(System.in));
        BufferedReader in =
                new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out =
                new PrintWriter(socket.getOutputStream(), true);

        System.out.print("Enter alias: ");
        String alias = console.readLine();
        out.println(alias);

        Thread reader = new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println(msg);
                }
            } catch (IOException ignored) {}
        });
        reader.start();

        String input;
        while ((input = console.readLine()) != null) {
            out.println(input);
            if (input.equalsIgnoreCase("/quit")) break;
        }

        socket.close();
    }
}
