import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;

public class GlobalIpResolver {
    private static final String STUN_HOST = "stun.l.google.com";
    private static final int STUN_PORT = 19302;

    private GlobalIpResolver() {
    }

    public static String resolveByStun() throws Exception {
        byte[] transactionId = new byte[12];
        new SecureRandom().nextBytes(transactionId);

        ByteBuffer request = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN);
        request.putShort((short) 0x0001); // Binding Request
        request.putShort((short) 0x0000); // No attributes
        request.putInt(0x2112A442); // Magic cookie
        request.put(transactionId);

        InetAddress stunAddress = InetAddress.getByName(STUN_HOST);
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(3000);
            DatagramPacket out = new DatagramPacket(request.array(), request.array().length, stunAddress, STUN_PORT);
            socket.send(out);

            byte[] responseBytes = new byte[512];
            DatagramPacket in = new DatagramPacket(responseBytes, responseBytes.length);
            socket.receive(in);

            return parseXorMappedAddress(responseBytes, in.getLength());
        }
    }

    private static String parseXorMappedAddress(byte[] data, int length) {
        ByteBuffer buffer = ByteBuffer.wrap(data, 0, length).order(ByteOrder.BIG_ENDIAN);
        short messageType = buffer.getShort();
        if (messageType != 0x0101) {
            throw new IllegalStateException("Unexpected STUN response type: " + Integer.toHexString(messageType));
        }

        int messageLength = buffer.getShort() & 0xFFFF;
        int cookie = buffer.getInt();
        if (cookie != 0x2112A442) {
            throw new IllegalStateException("Invalid STUN magic cookie");
        }

        byte[] transactionId = new byte[12];
        buffer.get(transactionId);

        int parsed = 0;
        while (parsed < messageLength && buffer.remaining() >= 4) {
            int type = buffer.getShort() & 0xFFFF;
            int attrLength = buffer.getShort() & 0xFFFF;

            if (type == 0x0020) {
                if (attrLength < 8) {
                    throw new IllegalStateException("Invalid XOR-MAPPED-ADDRESS length");
                }
                buffer.get(); // reserved
                int family = buffer.get() & 0xFF;
                int xPort = buffer.getShort() & 0xFFFF;
                int port = xPort ^ (0x2112A442 >>> 16);

                if (family == 0x01) {
                    byte[] xAddress = new byte[4];
                    buffer.get(xAddress);
                    byte[] cookieBytes = ByteBuffer.allocate(4).putInt(0x2112A442).array();
                    for (int i = 0; i < 4; i++) {
                        xAddress[i] = (byte) (xAddress[i] ^ cookieBytes[i]);
                    }
                    try {
                        InetAddress ip = InetAddress.getByAddress(xAddress);
                        return ip.getHostAddress();
                    } catch (UnknownHostException ex) {
                        throw new IllegalStateException("Failed to decode STUN IP address", ex);
                    }
                }
                throw new IllegalStateException("Only IPv4 STUN response is supported");
            }

            int skip = attrLength;
            if (skip > buffer.remaining()) {
                throw new IllegalStateException("Invalid STUN attribute length");
            }
            buffer.position(buffer.position() + skip);
            if ((attrLength % 4) != 0 && buffer.remaining() >= (4 - (attrLength % 4))) {
                buffer.position(buffer.position() + (4 - (attrLength % 4)));
            }

            parsed += 4 + attrLength;
        }

        throw new IllegalStateException("XOR-MAPPED-ADDRESS not found in STUN response");
    }
}


