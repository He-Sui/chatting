package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Packet;
import cn.edu.sustech.cs209.chatting.common.PacketType;
import cn.edu.sustech.cs209.chatting.common.User;
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

    private void handlePacket(Packet packet) {
        switch (packet.getType()) {
            case LOGIN -> {
                String username = packet.getUser().getUsername();
                String password = packet.getUser().getPassword();
                if (!server.getUsers().contains(new User(username, password)))
                    sendPacket(Packet.builder().info("Username or Password not Correct").type(PacketType.LOGIN_FAILED).build());
                else if (server.getOnlineUsers().containsKey(username))
                    sendPacket(Packet.builder().info("User Already Login"). type(PacketType.LOGIN_FAILED).build());
                else {
                    log.info("User {} logged in", username);
                    this.username = username;
                    sendPacket(Packet.builder().type(PacketType.LOGIN_SUCCESS).build());
                    server.getOnlineUsers().keySet().forEach(user -> sendPacket(Packet.builder().type(PacketType.NEW_USER).user(User.builder().username(user).build()).build()));
                    server.addUser(username, this);
                }
            }
            case REGISTER -> {
                String username = packet.getUser().getUsername();
                String password = packet.getUser().getPassword();
                if (server.getUsers().stream().map(User::getUsername).anyMatch(username::equals))
                    sendPacket(Packet.builder().info("Username Already Exist").type(PacketType.REGISTER_FAILED).build());
                else {
                    User user = new User(username, password);
                    server.getUsers().add(user);
                    server.getUserWriter().log(user);
                    sendPacket(Packet.builder().type(PacketType.REGISTER_SUCCESS).build());
                }
            }
            case MESSAGE -> server.forward(packet);
            case CREATE_CHAT -> {
                server.getChatroomWriter().log(packet.getChatRoom());
                server.getChatRooms().put(packet.getChatRoom().getId(), packet.getChatRoom());
                server.forward(packet);
            }
        }
    }

    @Override
    public void run() {
        try {
            try {
                while (true) {
                    Packet packet = receivePacket();
                    handlePacket(packet);
                }
            } catch (RuntimeException e) {
                server.clientLogout(username);
            } finally {
                socket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
