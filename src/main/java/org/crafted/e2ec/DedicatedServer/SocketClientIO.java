package org.crafted.e2ec.DedicatedServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketClientIO implements ClientIO {
// wraps a Socket for ClientIO operations
// uses BufferedReader and PrintWriter for I/O

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    public SocketClientIO(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        // toliet flush enabled
    }

    @Override
    public String readLine() throws IOException {
        // read a line from the input stream
        return in.readLine();
    }

    @Override
    public void writeLine(String line) {
        // write a line to the output stream
        out.println(line);
    }

    @Override
    public void close() {
        // close the socket and associated streams
        try {
            socket.close();
        } catch (IOException ignored) {}
    }
}
