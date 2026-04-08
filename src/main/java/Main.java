public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String mode = args[0].toLowerCase();
        switch (mode) {
            case "server":
                if (args.length < 3) {
                    printUsage();
                    return;
                }
                int webrtcPort = Integer.parseInt(args[1]);
                int ipApiPort = Integer.parseInt(args[2]);
                new ServerApp(webrtcPort, ipApiPort).start();
                break;

            case "client":
                if (args.length < 3) {
                    printUsage();
                    return;
                }
                String serverApiBaseUrl = args[1];
                int serverWebRtcPort = Integer.parseInt(args[2]);
                new ClientApp(serverApiBaseUrl, serverWebRtcPort).start();
                break;

            default:
                printUsage();
                break;
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  server <webrtcPort> <ipApiPort>");
        System.out.println("  client <serverApiBaseUrl> <serverWebrtcPort>");
        System.out.println("Examples:");
        System.out.println("  server 7000 8000");
        System.out.println("  client http://example.com:8000 7000");
        System.out.println("  client example.com:8000 7000");
    }
}
