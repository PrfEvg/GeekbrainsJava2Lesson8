package com.geekbrains.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Socket socket;
    private Server server;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.server = server;

        new Thread(() -> {
            try {
                //цикл общения
                while (true) {
                    String msg = in.readUTF();
                    if(msg.startsWith("/")) {
                        executeCommand(msg);
                        continue;
                    }
                    server.broadcastMessage(username + ": " + msg);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    private void executeCommand(String cmd) throws IOException {
        if(cmd.startsWith("/login ")) {
            String usernameFromLogin = cmd.split("\\s")[1];

            if(server.isUserOnline(usernameFromLogin)) {
                sendMessage("/login_failed Current nickname has already been occupied");
            }

            username = usernameFromLogin;
            sendMessage("/login_ok " + username);
            server.subscribe(this);
        }
        if(cmd.startsWith("/w ")) {
            String[] tokens = cmd.split("\\s", 3);
            server.sendPrivateMessage(this, tokens[1], tokens[2]);
            return;
        }
        if (cmd.startsWith("/end")) {
            sendMessage("Вы отключились \n");
            disconnect();
            return;
        }
        if (cmd.startsWith("/who_am_i")) {
            sendMessage("You are: " + getUsername() + "\n");

        }
    }

    private void disconnect() {
        server.unsubscribe(this);
        if(in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(out != null) {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            disconnect();
        }
    }

    public String getUsername() {
        return username;
    }
}
