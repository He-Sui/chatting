package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.Packet;
import cn.edu.sustech.cs209.chatting.common.PacketType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
public class Server {
    private final ServerSocket serverSocket;
    private final Map<String, ServerService> users;

    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        users = new ConcurrentHashMap<>();
    }

    public void addUser(String username, ServerService serverService) {
        notifyAllUsers(Packet.builder().type(PacketType.NEW_USER).addition(username).build());
        users.put(username, serverService);
    }

    public void notifyAllUsers(Packet packet) {
        users.values().forEach(serverService -> serverService.sendPacket(packet));
    }

    public void forward(Packet packet) {
        if (packet.getType() == PacketType.MESSAGE) {
            Message message = packet.getMessage();
            if (message == null || message.getSendTo() == null || !users.containsKey(message.getSendTo()))
                return;
            users.get(message.getSendTo()).sendPacket(packet);
        }
    }

    public void start() {
        try {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ServerService(socket, this)).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
