public class ChatMessage {
    public String type;
    public String from;
    public String message;

    public ChatMessage() {
    }

    public ChatMessage(String type, String from, String message) {
        this.type = type;
        this.from = from;
        this.message = message;
    }

    public static ChatMessage clientMessage(String from, String message) {
        return new ChatMessage("client_message", from, message);
    }

    public static ChatMessage serverMessage(String from, String message) {
        return new ChatMessage("server_message", from, message);
    }

    public static ChatMessage ack(String from) {
        return new ChatMessage("ack", from, "ok");
    }
}

