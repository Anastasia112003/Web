package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;


public class Server {
    ExecutorService service;
    final ConcurrentHashMap<String, Map<String, Handler>> handlers;
    final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    public Server(int pool) {
        this.service = Executors.newFixedThreadPool(pool);
        handlers = new ConcurrentHashMap<>();
    }

    public void start(int port) {
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                final var socket = serverSocket.accept();
                service.submit(() -> connect(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connect(Socket socket) {
        try (socket;
             final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            // read only request line for simplicity
            // must be in form GET /path HTTP/1.1
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");

            if (parts.length != 3) {
                // just close socket
                return;
            }

            String method = parts[0];
            final var path = parts[1];
            Request request = new Request(method, path);

            if (request == null || !handlers.containsKey(request.getMethod())) {
                responseWithoutContent(out, "404", "Not found");
            }

            Map<String, Handler> handlerMap = handlers.get(request.getMethod());
            String requestPath = request.getPath();
            if (handlerMap.containsKey(requestPath)) {
                Handler handler = handlerMap.get(requestPath);
                handler.handle(request, out);
            } else {
                if (!validPaths.contains(request.getPath())) {
                    responseWithoutContent(out, "404", "Not found");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void handlers(BufferedOutputStream out, String path) throws IOException {

        final var filePath = Path.of(".", "public", path);
        final String mimeType;
        try {
            mimeType = Files.probeContentType(filePath);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        // special case for classic
        if (path.equals("/classic.html")) {
            final String template;
            try {
                template = Files.readString(filePath);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(content);
            out.flush();
            return;
        }

        final long length;
        try {
            length = Files.size(filePath);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }


    protected void addHandler(String method, String path, Handler handler) {
        if (!handlers.containsKey(method)) {
            handlers.put(method, new HashMap<>());
        }
        handlers.get(method).put(path, handler);
    }

    protected void responseWithoutContent(BufferedOutputStream out, String responseCode, String responseStatus) throws IOException {
        out.write((
                "HTTP/1.1 " + responseCode + " " + responseStatus + "\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }
}


