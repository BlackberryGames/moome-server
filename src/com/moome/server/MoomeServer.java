package com.moome.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MoomeServer implements Runnable {
    public static final int DEFAULT_PORT = 29798;
    public static final int REASON_OKAY = 0;
    public static final int REASON_ERROR = 1;
    public static final int MAX_PLAYERS = 2; // implement later
    public String VERSION = "1.0.0"; // Insert the version of the client that should connect here...
    
    private int port = DEFAULT_PORT;
    private ServerSocket socket = null;
    private boolean running = false;
    private Thread serverThread = null;
    private final List<MoomeClientHandler> clients = new ArrayList<>();
    private final static List<MoomeLogger> loggers = new ArrayList<>();
    private int id = 0;
    private long tick = 0;
    private String entailed = "";
    private final String defaultEntail = ""; // LEAVE EMPTY!
    
    public MoomeServer(int port) throws MoomeServerException {
        log("info", "Instance started.");
        if(port <= 1024) {
            throw new MoomeServerException();
        } else {
            try {
                this.port = port;
                final ServerSocket socket = new ServerSocket(this.getPort());
                socket.close();
            } catch (IOException e) {
                throw new MoomeServerException();
            }
        }
    }
    
    public void start() throws MoomeServerException {
        if(running)
            throw new MoomeServerException();
        else {
            log("info", "Starting server on port [" + this.port + "]...");
            Thread serverThread = new Thread(this);
            this.serverThread = serverThread;
            serverThread.start();
            log("info", "Done. Server thread active.");
            
        
        Timer t = new Timer();
        t.schedule(new TimerTask(){public void run() {update();tick++;/*System.out.println("MEOW!!~!!!! " + clients.size());*/}}, 0, 40);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                log("info", "Saving server status...");
                MoomeServer.this.stop(MoomeServer.REASON_OKAY);
                try {
                    MoomeServer.this.socket.close(); // Patch resource leak bug
                } catch (IOException e) {
                    log("err", "IOException: Failed to close socket.");
                    e.printStackTrace();
                }
                MoomeServer.this.running = false;
                clients.clear();
                MoomeServer.this.serverThread = null;
                socket = null;
                
                log("info", "Complete. Exiting!");
            }
         });
        }
        
        /*MoomeServer server = this; // DEPRECATED CODE
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                server.stop(1);
                System.exit(0);
            }
        }));*/
    }

    public int getPort() {
        return this.port;
    }

    public ServerSocket getSocket() {
        return this.socket;
    }
    
    public void stop(final int reason) {
        switch(reason) {
        case MoomeServer.REASON_OKAY:
            this.running = false;
            this.sendAll(1, "reason=normal"); // Send message type 1 (shutdown request) with contents "normal"
            for(final MoomeClientHandler handler : this.getClients())
                try {
                    handler.getSocket().close();
                } catch (IOException e) {
                    MoomeServer.log("warn", "Failed to properly remove client " + handler.getId() + " from server: " + e.getMessage());
                }
            
            log("info", "Requests sent properly, preparing to shut down.");
            break;
        case MoomeServer.REASON_ERROR:
            this.running = false;
            this.getServerThread().interrupt();
            log("warn", "Dangerous emergency server interrupt called, shutting down.");
            break;
        }
    }

    public static void log(final String type, final String message) {
        for(final MoomeLogger logger : getLoggers())
            logger.log(type.toUpperCase(), message);
    }
    
    public void update() {
        String entailedTemp = defaultEntail; // portHere/name=clientname#lx=1#ly=2#vname=clientname2#lx=12#ly=4
        for(final MoomeClientHandler handler : this.getClients())
            entailedTemp += handler.getLine() + "#v";
        
        try {entailedTemp.substring(0, entailedTemp.length() - 3);} catch(Exception e) {} // remove extra #v from the end
        this.entailed = entailedTemp;
        tick++;
    }
    
    public String getData() {
        return entailed;
    }

    private void sendAll(final int type, final String message) {
        final String escapedMessage = escape(message);
        final char dataSeparator = '=';
        final String combined = type + dataSeparator + escapedMessage;
        
        for(final MoomeClientHandler handler : this.getClients()) {
            handler.send(combined);
        }
    }
    
    public static String escape(final String message) {
        return message.replaceAll("#v", "v").replaceAll("#l", "l").replaceAll("=", "~");
    }
    
    public String motd = "Welcome to the server!" + "#lport=" + DEFAULT_PORT + "#lversion=" + this.getClientVersion();
    
    @Override
    public void run() {
        this.running = true;
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(this.getPort());
        } catch (IOException e) {
            log("err", "Failed to open server on port " + this.getPort() + "." + (((this.getPort() > 60000) || (this.getPort() < 2000)) ? " Is port under 1256 or over 60000?" : ""));
        }
        this.socket = socket;
        
        if(socket == null)
            this.running = false;
        
        while(this.running) {
            MoomeClientHandler client = null;
            try {
                client = new MoomeClientHandler(socket.accept(), this, id);
                log("info", "Accepted client id " + id);
            } catch (IOException e) {
                log("warn", "Failed to accept join request from " + id + ": " + e.getMessage());
            }
            id++;
            this.clients.add(client);
        }
    }
    
    private String getClientVersion() {
        return this.VERSION;
    }
    
    public void setClientVersion(final String version) {
        this.VERSION = version;
    }

    public static List<MoomeLogger> getLoggers() {
        return loggers;
    }
    
    public void addLogger(final MoomeLogger logger) {
        loggers.add(logger);
    }

    public Thread getServerThread() {
        return serverThread;
    }

    public List<MoomeClientHandler> getClients() {
        return clients;
    }

    public void removeClient(final int id2) {
        int index = 0;
        List<MoomeClientHandler> list2 = this.getClients();
        for(final MoomeClientHandler handler : list2) {
           if(handler.getId() == id2) {
                list2.remove(index);
                break;        
           }
           index++;
        }
        this.clients.clear();
        this.clients.addAll(list2);
    }
    
    public void removeClient(final MoomeClientHandler handler) {
        this.clients.remove(handler);
    }
}
