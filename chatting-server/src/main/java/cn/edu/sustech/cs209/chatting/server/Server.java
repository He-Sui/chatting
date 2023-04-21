package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
public class Server {
    private final ServerSocket serverSocket;
    private final Map<String, ServerService> onlineUsers;
    private final Map<String, ChatRoom> chatRooms = new HashMap<>();
    private final Set<User> users;

    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
            users = new HashSet<>();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        onlineUsers = new ConcurrentHashMap<>();
    }

    public void addUser(String username, ServerService serverService) {
        notifyAllUsers(Packet.builder().type(PacketType.NEW_USER).user(User.builder().username(username).build()).build());
        onlineUsers.put(username, serverService);
    }

    public void notifyAllUsers(Packet packet) {
        onlineUsers.values().forEach(serverService -> serverService.sendPacket(packet));
    }

    public void forward(Packet packet) {
        if (packet.getType() == PacketType.MESSAGE) {
            Message message = packet.getMessage();
            if (message == null || message.getChatRoomId() == null || chatRooms.get(message.getChatRoomId()) == null)
                return;
            Set<String> userInChatRoom = chatRooms.get(message.getChatRoomId()).getUsers();
            userInChatRoom.stream()
                    .filter(user -> !user.equals(message.getSentBy()))
                    .filter(onlineUsers::containsKey)
                    .forEach(user -> onlineUsers.get(user).sendPacket(Packet.builder().type(PacketType.MESSAGE).message(message).build()));
        } else if (packet.getType() == PacketType.CREATE_CHAT) {
            ChatRoom chatRoom = packet.getChatRoom();
            chatRoom.getUsers().stream()
                    .filter(user -> !user.equals(packet.getUser().getUsername()))
                    .filter(onlineUsers::containsKey)
                    .forEach(user -> onlineUsers.get(user).sendPacket(Packet.builder().type(PacketType.CREATE_CHAT).chatRoom(chatRoom).build()));
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

    public void clientLogout(String username) {
        if (username != null && onlineUsers.containsKey(username)) {
            onlineUsers.remove(username);
            notifyAllUsers(Packet.builder().type(PacketType.LOGOUT).user(User.builder().username(username).build()).build());
        }
    }
}
