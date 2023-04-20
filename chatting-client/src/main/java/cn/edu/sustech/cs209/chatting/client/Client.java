package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.util.*;

@Getter
@Slf4j
public class Client {
    private final Socket socket;
    private final Controller controller;
    private final BufferedReader in;
    private final BufferedWriter out;
    private String username;
    private final ObjectMapper objectMapper;
    private final Set<String> users;

    private final Set<ChatRoom> chatRooms;
    private final Map<String, List<Message>> messageList;
    private Thread receivingPacketThread;

    public Client(String host, int port, String username, Controller controller) {
        chatRooms = new HashSet<>();
        this.controller = controller;
        objectMapper = new ObjectMapper();
        this.username = username;
        users = new HashSet<>();
        messageList = new HashMap<>();
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handlePacket(Packet packet) {
        switch (packet.getType()) {
            case MESSAGE -> {
                messageList.get(packet.getMessage().getChatRoomId()).add(packet.getMessage());
                controller.updateMessage();
            }
            case NEW_USER -> {
                if (!packet.getUser().getUsername().equals(username)) {
                    users.add(packet.getUser().getUsername());
                    controller.updateOnlineCnt();
                }
            }
            case CREATE_CHAT -> {
                chatRooms.add(packet.getChatRoom());
                messageList.put(packet.getChatRoom().getId(), new ArrayList<>());
                controller.updateChatList();
            }
        }
    }

    public void startReceivingPacket() {
        receivingPacketThread = new Thread(() -> {
            while (!receivingPacketThread.isInterrupted()) {
                handlePacket(receivePacket());
            }
        });
        receivingPacketThread.start();
    }

    public void login() {
        sendPacket(Packet.builder().type(PacketType.LOGIN).user(User.builder().username(username).build()).build());
        Packet receive = receivePacket();
        if (receive.getType() == PacketType.LOGIN_SUCCESS) {
            log.info("Login Success");
        } else {
            log.info("Login Failed");
            throw new RuntimeException("Login Failed");
        }
    }

    public ChatRoom createPrivateChat(String name) {
        if (chatRooms.stream()
                .filter(r -> r.getType() == ChatType.PRIVATE_CHAT)
                .map(ChatRoom::getUsers)
                .noneMatch(u -> u.contains(name))) {
            Set<String> user = new HashSet<>();
            user.add(username);
            user.add(name);
            ChatRoom chatRoom = ChatRoom.builder()
                    .id(UUID.randomUUID().toString())
                    .type(ChatType.PRIVATE_CHAT).users(user).build();
            chatRooms.add(chatRoom);
            messageList.put(chatRoom.getId(), new ArrayList<>());
            sendPacket(Packet.builder().type(PacketType.CREATE_CHAT).user(User.builder().username(username).build()).chatRoom(chatRoom).build());
            return chatRoom;
        } else
            return chatRooms.stream()
                    .filter(r -> r.getType() == ChatType.PRIVATE_CHAT)
                    .filter(r -> r.getUsers().contains(name))
                    .findFirst()
                    .orElse(null);
    }

    public void sendPacket(Packet packet) {
        try {
            out.write(objectMapper.writeValueAsString(packet));
            out.newLine();
            out.flush();
            log.info("Sent packet to server: {}", packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Packet receivePacket() {
        try {
            Packet packet = objectMapper.readValue(in.readLine(), Packet.class);
            log.info("Received packet from server: {}", packet);
            return packet;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
