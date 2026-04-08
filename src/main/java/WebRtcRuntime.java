public class WebRtcRuntime {
    private WebRtcRuntime() {
    }

    public static void ensureLibraryPresent() {
        try {
            Class.forName("dev.onvoid.webrtc.PeerConnectionFactory");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("webrtc-java is not available on classpath", ex);
        }
    }
}

