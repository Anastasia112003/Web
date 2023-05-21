package org.example;


import java.io.BufferedOutputStream;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Server server = new Server(64);
        server.addHandler("GET", "/messages", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) {
                try {
                    server.responseWithoutContent(responseStream, "404", "Not found");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        server.addHandler("POST", "/messages", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) {
                try {
                    server.responseWithoutContent(responseStream, "404", "Not found");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        server.start(9999);
    }
}
