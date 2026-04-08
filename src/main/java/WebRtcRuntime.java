public class WebRtcRuntime {
    private WebRtcRuntime() {
    }

    public static String diagnoseAvailability() {
        try {
            // Force class initialization so native load issues are surfaced explicitly.
            Class.forName("dev.onvoid.webrtc.PeerConnectionFactory");
            return null;
        } catch (ClassNotFoundException ex) {
            return "webrtc-java is not available on classpath: " + ex.getMessage();
        } catch (Throwable ex) {
            return "webrtc-java native initialization failed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }
    }
}

