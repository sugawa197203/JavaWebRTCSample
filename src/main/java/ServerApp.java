import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerApp {
    private final int webrtcPort;
    private final int ipApiPort;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ConcurrentHashMap<String, ClientConnection> clients = new ConcurrentHashMap<>();

    public ServerApp(int webrtcPort, int ipApiPort) {
        this.webrtcPort = webrtcPort;
        this.ipApiPort = ipApiPort;
    }

    public void start() throws Exception {
        String webRtcIssue = WebRtcRuntime.diagnoseAvailability();
        if (webRtcIssue != null) {
            System.err.println("[server] warning: " + webRtcIssue);
            System.err.println("[server] warning: continuing in socket-chat mode.");
        }

        String globalIp = GlobalIpResolver.resolveByStun();
        System.out.println("[server] Resolved global IP: " + globalIp);

        Javalin ipApi = Javalin.create().start(ipApiPort);
        ipApi.get("/getip", ctx -> ctx.json(Map.of("ip", globalIp)));
        System.out.println("[server] /getip API started on port " + ipApiPort);

        Thread consoleThread = new Thread(this::runConsoleLoop, "server-console-loop");
        consoleThread.setDaemon(true);
        consoleThread.start();

        try (ServerSocket serverSocket = new ServerSocket(webrtcPort)) {
            System.out.println("[server] Waiting for client connections on port " + webrtcPort);
            while (true) {
                Socket socket = serverSocket.accept();
                Thread clientThread = new Thread(() -> handleClient(socket), "client-session-" + socket.getPort());
                clientThread.start();
            }
        }
    }

    private void runConsoleLoop() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    return;
                }
                if (line.isBlank()) {
                    continue;
                }

                ChatMessage message = ChatMessage.serverMessage("SERVER", line);
                broadcast(message);
                System.out.println("[server->all] " + line);
            }
        } catch (IOException ex) {
            System.err.println("[server] Console loop ended: " + ex.getMessage());
        }
    }

    private void handleClient(Socket socket) {
        String clientId = UUID.randomUUID().toString();
        try (Socket s = socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8)) {

            clients.put(clientId, new ClientConnection(writer));
            System.out.println("[server] client connected: " + clientId + " from " + s.getRemoteSocketAddress());

            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                ChatMessage message = mapper.readValue(line, ChatMessage.class);
                if ("client_message".equals(message.type)) {
                    // Client messages are visible only to the sender and server.
                    System.out.println("[client->server][" + clientId + "] " + message.message);
                    writer.println(mapper.writeValueAsString(ChatMessage.ack(clientId)));
                }
            }
        } catch (Exception ex) {
            System.err.println("[server] client session error: " + ex.getMessage());
        } finally {
            clients.remove(clientId);
            System.out.println("[server] client disconnected: " + clientId);
        }
    }

    private void broadcast(ChatMessage message) {
        String json;
        try {
            json = mapper.writeValueAsString(message);
        } catch (Exception ex) {
            System.err.println("[server] Failed to serialize broadcast message: " + ex.getMessage());
            return;
        }

        clients.values().forEach(client -> client.writer.println(json));
    }

    private static class ClientConnection {
        private final PrintWriter writer;

        private ClientConnection(PrintWriter writer) {
            this.writer = writer;
        }
    }
}



