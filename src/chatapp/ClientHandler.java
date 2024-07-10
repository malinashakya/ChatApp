/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chatapp;

/**
 *
 * @author malin
 */
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String requestLine;
            while ((requestLine = in.readLine()) != null) {
                if (requestLine.startsWith("GET")) {
                    handleGetRequest(requestLine);
                } else if (requestLine.startsWith("POST")) {
                    handlePostRequest();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Server.removeClient(this);
        }
    }

    private void handleGetRequest(String requestLine) throws IOException {
        String path = requestLine.split(" ")[1];
        if (path.equals("/chat-history")) {
            List<String> chatHistory = Server.getChatHistory();
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: application/json");
            out.println("Connection: close");
            out.println();
            out.println("[");
            for (int i = 0; i < chatHistory.size(); i++) {
                out.printf("\"%s\"", chatHistory.get(i));
                if (i < chatHistory.size() - 1) {
                    out.println(",");
                }
            }
            out.println("]");
        } else if (path.equals("/login.html")) {
            sendHtmlResponse("web/login.html");
        } else if (path.equals("/chat.html")) {
            sendHtmlResponse("web/chat.html");
        } else {
            sendNotFoundResponse();
        }
    }

    private void handlePostRequest() throws IOException {
        String line;
        int contentLength = 0;
        while (!(line = in.readLine()).isEmpty()) {
            if (line.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            }
        }
        char[] body = new char[contentLength];
        in.read(body);
        String requestBody = new String(body);

        if (requestBody.startsWith("username=") && requestBody.contains("&password=")) {
            String[] parts = requestBody.split("&");
            String username = parts[0].split("=")[1];
            String password = parts[1].split("=")[1];
            if (Server.authenticate(username, password)) {
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: text/plain");
                out.println("Connection: close");
                out.println();
                out.println("OK");
            } else {
                out.println("HTTP/1.1 401 Unauthorized");
                out.println("Content-Type: text/plain");
                out.println("Connection: close");
                out.println();
                out.println("Unauthorized");
            }
        } else if (requestBody.startsWith("message=")) {
            String message = requestBody.split("=")[1];
            Server.addMessage(message);
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/plain");
            out.println("Connection: close");
            out.println();
            out.println("Message sent");
        } else {
            sendBadRequestResponse();
        }
    }

    private void sendHtmlResponse(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html");
            out.println("Connection: close");
            out.println();
            BufferedReader fileReader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = fileReader.readLine()) != null) {
                out.println(line);
            }
            fileReader.close();
        } else {
            sendNotFoundResponse();
        }
    }

    private void sendNotFoundResponse() {
        out.println("HTTP/1.1 404 Not Found");
        out.println("Content-Type: text/plain");
        out.println("Connection: close");
        out.println();
        out.println("Not Found");
    }

    private void sendBadRequestResponse() {
        out.println("HTTP/1.1 400 Bad Request");
        out.println("Content-Type: text/plain");
        out.println("Connection: close");
        out.println();
        out.println("Bad Request");
    }

    public void sendMessage(String message) {
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: text/plain");
        out.println("Connection: close");
        out.println();
        out.println(message);
    }
}
