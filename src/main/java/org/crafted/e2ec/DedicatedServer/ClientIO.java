package org.crafted.e2ec.DedicatedServer;

public interface ClientIO {
    String readLine() throws Exception;
    void writeLine(String line);
    void close();
}
