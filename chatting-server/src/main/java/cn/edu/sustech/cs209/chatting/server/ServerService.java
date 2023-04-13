package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Packet;
import cn.edu.sustech.cs209.chatting.common.PacketType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;

@Slf4j
public class ServerService implements Runnable {

    private String username;
    private final Server server;
    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;
    private final ObjectMapper objectMapper;

    public ServerService(Socket socket, Server server) {
        this.server = server;
        this.socket = socket;
        objectMapper = new ObjectMapper();
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendPacket(Packet packet) {
        try {
            out.write(objectMapper.writeValueAsString(packet));
            out.newLine();
            out.flush();
            log.info("Sent packet to {}: {}", username, packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Packet receivePacket() {
        try {
            Packet packet = objectMapper.readValue(in.readLine(), Packet.class);
            log.info("Received packet from {}: {}", username, packet);
            return packet;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void login() {
        Packet packet = receivePacket();
        if (packet.getType() != PacketType.LOGIN)
            sendPacket(Packet.builder().type(PacketType.LOGIN_FAILED).addition("invalid request").build());
        else {
            username = packet.getAddition();
            if (server.getUsers().containsKey(username))
                sendPacket(Packet.builder().type(PacketType.LOGIN_FAILED).addition("username already exists").build());
            else if (username == null || username.equals(""))
                sendPacket(Packet.builder().type(PacketType.LOGIN_FAILED).addition("username cannot be empty").build());
            else {
                log.info("User {} logged in", username);
                sendPacket(Packet.builder().type(PacketType.LOGIN_SUCCESS).build());
                server.getUsers().keySet().forEach(user -> sendPacket(Packet.builder().type(PacketType.NEW_USER).addition(user).build()));
                server.addUser(username, this);
            }
        }
    }

    private void handlePacket(Packet packet) {
        switch (packet.getType()) {
            case MESSAGE -> server.forward(packet);
        }
    }

    @Override
    public void run() {
        try {
            try {
                login();
                while (true) {
                    Packet packet = receivePacket();
                    handlePacket(packet);
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            } finally {
                socket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
