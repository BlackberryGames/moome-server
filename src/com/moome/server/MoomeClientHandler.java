package com.moome.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MoomeClientHandler {
    private Socket socket = null;
    private MoomeServer server = null;
    private int id = 0;
    private final HashMap<String, String> map = new HashMap(); // if error,
                                                                // check this!

    private String line = "";
    private BufferedReader in = null;
    private PrintWriter out = null;
    protected String token = UUID.randomUUID().toString();
    public String name = token;
    public boolean connected = true;
    public String csrf = UUID.randomUUID().toString();

    public MoomeClientHandler(Socket socket, MoomeServer server, int id) {
        this.socket = socket;
        this.server = server;
        this.id = id;

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            MoomeServer.log("warn", "Failed to get input and output stream from Client id " + this.id + "'s stream.");
            disconnect("IOException: Failed to get input and output stream.");
        }

        Thread recieve = new Thread(new Runnable() {

            @Override
            public void run() {
                while (connected) {
                    String input = null;
                    try {
                        input = in.readLine();
                        MoomeServer.log("info", "msgid=" + input);
                    } catch (IOException e) {
                        disconnect("!normal");
                        MoomeServer.log("warn", "Client id " + getId() + " is having issues communicating.");
                    }
                    if (input == null || input.equals("1/id=" + getId())) { // 1/id=theIDHere
                                                                            // to
                                                                            // exit
                        MoomeServer.log("info", "Client id " + getId() + " disconnected.");
                        server.removeClient(id);
                        disconnect("Disconnection requested.");
                        break;
                    } else
                        handle(input);
                }
            }
        });
        recieve.start();
    }

    public int getId() {
        return this.id;
    }

    public Socket getSocket() {
        return this.socket;
    }

    public void disconnect(final String reason) {
        if(!reason.equals("!normal"))
        this.send(query(1, reason));
        server.removeClient(this.getId());
        this.connected = false;
        this.token = "";
        System.out.println("DISCONNECT " + this.id + ": " + reason);
    }

    public void send(final String combined) {
        out.println(combined);
        out.flush();
    }

    private final String[] allowKeysRaw = {"name", "x", "y", "world", "version", "visible", "looks", "token", "csrf"};
    private final List<String> allowKeys = (List<String>) Arrays.asList(allowKeysRaw);

    public void handle(final String response) {
        //MoomeServer.log("info", "Request from Client id " + this.getId() + ": " + response);
        final int index = response.indexOf('/');
        final String type = response.substring(0, index); // ADD - 1 IF BUG!
        /*
         * type documentation: 
         * 0 = updated data request 
         * 1 = disconnect request 
         * 2 = new data request 
         * 3 = token request 
         * 4 = error
         * 5 = motd request
         * 
         */
        final List<String> keys = new ArrayList<>();
        final HashMap<String, String> pairZ = new HashMap<String, String>();
        String dataSplit = response.substring(index + 1);
        if(dataSplit.endsWith("#l")) {
            dataSplit = dataSplit.substring(0, dataSplit.length() - 2);
        }
        final String[] pairs = dataSplit.split("#l");
        for (final String pair : pairs) {
            try {
                String[] split = pair.split("=");
                keys.add(split[0].toLowerCase());
                pairZ.put(split[0].toLowerCase(), split[1]);
            } catch (final Exception e) {

            }
        }
        
        switch (type) {
        case "0":
            send("0/" + server.getData());
            System.out.println("TYPE 0");
            break;
        case "1":
            String tok = pairZ.get("token");
            if(this.token.equals(tok))
                this.disconnect("!normal");
            break;
        case "2":
            if(keys.contains("token")) {
                if(pairZ.get("token").equals(this.token)) {
                if(keys.contains("looks"))
                    System.out.println(pairZ.get("looks"));
            for(final String key : keys)
            if (allowKeys.contains(key)) {
                map.put(key, pairZ.get(key));
            }
            this.update();
                } else {
                    send("4/error=Valid token is required#ltype=2");
                }
            } else {
                send("4/error=Valid token is required#ltype=2");
            }
            break;
        case "3":
            map.put("csrf", this.csrf);
            System.out.println("TYPE 3");
            send("3/token=" + this.token + "#lcsrf=" + this.csrf);
            break;
        case "5": 
            send("5/motd=" + this.server.motd);
            break;
        }
        
        map.put("csrf", this.csrf);
    }

    public void update() {
        String lineTemp = "";
        final Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry pair = (Map.Entry) it.next();
            lineTemp += MoomeServer.escape(pair.getKey().toString()) + "="
                    + MoomeServer.escape(pair.getValue().toString()) + "#l";
            it.remove();
        }
        try {
            lineTemp.substring(0, lineTemp.length() - 3); // remove the extra #l at
        } catch(Exception e) {}
        System.out.println(lineTemp);
        line = lineTemp; // switch the line
    }

    private String query(final int type, final String message) {
        final String escapedMessage = MoomeServer.escape(message);
        final char dataSeparator = '=';
        final String combined = type + dataSeparator + escapedMessage;

        return combined;
    }

    public String getLine() {
        return this.line;
    }
}
