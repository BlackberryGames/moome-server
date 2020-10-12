import com.moome.server.*;

public class Main {
    public static void main(String[] args) {
        new Main();
    }
    
    public Main() {
        try {
            MoomeServer server = new MoomeServer(MoomeServer.DEFAULT_PORT);
            server.addLogger(new MoomeLogger() {
                public void log(final String type, final String message) {
                    System.out.println("[" + type + "] " + message);
                }
            });
            server.start();
        } catch (MoomeServerException e) {
            System.err.println("IOException: Failed to start server.");
        }
    }
}
