import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ClientApp {
    private final String serverApiBaseUrl;
    private final int serverWebRtcPort;
    private final ObjectMapper mapper = new ObjectMapper();

    public ClientApp(String serverApiBaseUrl, int serverWebRtcPort) {
        this.serverApiBaseUrl = trimTrailingSlash(serverApiBaseUrl);
        this.serverWebRtcPort = serverWebRtcPort;
    }

    public void start() throws Exception {
        WebRtcRuntime.ensureLibraryPresent();

        String serverIp = fetchServerIp();
        String clientId = UUID.randomUUID().toString();

        System.out.println("[client] server global IP: " + serverIp);
        try (Socket socket = new Socket(serverIp, serverWebRtcPort);
             BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter serverWriter = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            Thread receiveThread = new Thread(() -> receiveLoop(serverReader), "client-receive-loop");
            receiveThread.setDaemon(true);
            receiveThread.start();

            System.out.println("[client] connected. Type messages to send to server.");
            while (true) {
                String line = consoleReader.readLine();
                if (line == null) {
                    return;
                }
                if (line.isBlank()) {
                    continue;
                }
                ChatMessage message = ChatMessage.clientMessage(clientId, line);
                serverWriter.println(mapper.writeValueAsString(message));
            }
        }
    }

    private void receiveLoop(BufferedReader serverReader) {
        try {
            while (true) {
                String line = serverReader.readLine();
                if (line == null) {
                    System.out.println("[client] disconnected from server.");
                    return;
                }
                ChatMessage message = mapper.readValue(line, ChatMessage.class);
                switch (message.type) {
                    case "server_message":
                        System.out.println("[server->all] " + message.message);
                        break;
                    case "ack":
                        System.out.println("[server] delivered");
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception ex) {
            System.err.println("[client] receive loop ended: " + ex.getMessage());
        }
    }

    private String fetchServerIp() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(serverApiBaseUrl + "/getip"))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Failed to get server IP. HTTP status=" + response.statusCode());
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode ipNode = root.get("ip");
        if (ipNode == null || ipNode.asText().isBlank()) {
            throw new IllegalStateException("Invalid /getip response: " + response.body());
        }
        return ipNode.asText();
    }

    private static String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}

