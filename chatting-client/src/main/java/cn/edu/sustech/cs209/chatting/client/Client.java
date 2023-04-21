package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

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
            throw new RuntimeException(e.getMessage());
        }
    }

    public void handlePacket(Packet packet) {
        if (packet == null)
            return;
        switch (packet.getType()) {
            case MESSAGE -> {
                messageList.get(packet.getMessage().getChatRoomId()).add(packet.getMessage());
                controller.updateMessage();
                controller.updateChatList();
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
            case LOGOUT -> {
                users.remove(packet.getUser().getUsername());
                controller.updateOnlineCnt();
                chatRooms.forEach(chatRoom -> {
                    if (chatRoom.getUsers().contains(packet.getUser().getUsername())) {
                        messageList.get(chatRoom.getId()).add(Message.builder()
                                .timestamp(System.currentTimeMillis())
                                .chatRoomId(chatRoom.getId())
                                .type(MessageType.TEXT)
                                .data("User " + packet.getUser().getUsername() + " has left the chat room")
                                .build());
                    }
                });
                controller.updateMessage();
            }
        }
    }

    public void startReceivingPacket() {
        receivingPacketThread = new Thread(() -> {
            while (true) {
                Packet packet = receivePacket();
                if (receivingPacketThread.isInterrupted())
                    break;
                if (packet == null) {
                    controller.serverLogout();
                    break;
                }
                handlePacket(packet);
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
            Message welcomeMessage = Message.builder()
                    .timestamp(System.currentTimeMillis())
                    .type(MessageType.TEXT)
                    .chatRoomId(chatRoom.getId())
                    .data("Successfully created a private chat")
                    .build();
            sendPacket(Packet.builder().type(PacketType.MESSAGE).message(welcomeMessage).build());
            return chatRoom;
        } else
            return chatRooms.stream()
                    .filter(r -> r.getType() == ChatType.PRIVATE_CHAT)
                    .filter(r -> r.getUsers().contains(name))
                    .findFirst()
                    .orElse(null);
    }

    public ChatRoom createGroupChat(Set<String> user) {
        user.add(username);
        if (chatRooms.stream()
                .filter(r -> r.getType() == ChatType.GROUP_CHAT)
                .map(ChatRoom::getUsers)
                .noneMatch(u -> u.equals(user))) {
            ChatRoom chatRoom = ChatRoom.builder()
                    .id(UUID.randomUUID().toString())
                    .type(ChatType.GROUP_CHAT).users(user).build();
            chatRooms.add(chatRoom);
            messageList.put(chatRoom.getId(), new ArrayList<>());
            sendPacket(Packet.builder().type(PacketType.CREATE_CHAT).user(User.builder().username(username).build()).chatRoom(chatRoom).build());
            Message welcomeMessage = Message.builder()
                    .timestamp(System.currentTimeMillis())
                    .type(MessageType.TEXT)
                    .chatRoomId(chatRoom.getId())
                    .data("Successfully created chat room")
                    .build();
            sendPacket(Packet.builder().type(PacketType.MESSAGE).message(welcomeMessage).build());
            return chatRoom;
        } else
            return chatRooms.stream()
                    .filter(r -> r.getType() == ChatType.GROUP_CHAT)
                    .filter(r -> r.getUsers().equals(user))
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
            log.error(e.getMessage());
        }
    }

    public Packet receivePacket() {
        try {
            String p = in.readLine();
            if (p != null) {
                Packet packet = objectMapper.readValue(p, Packet.class);
                log.info("Received packet from server: {}", packet);
                return packet;
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public void downloadFile(Message msg) throws IOException {
        String filename = msg.getFileName(); // 文件名
        byte[] fileData = Base64.getDecoder().decode(msg.getData()); // 文件数据
        String savePath = "/Users/suih/Downloads/"; // 默认下载路径
        File file = new File(savePath + filename);
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(fileData);
        fos.close();
        Desktop desktop = Desktop.getDesktop();
        desktop.open(file);
    }

    public void sendFile(String chatRoomID, File file) {
        try {
            Path filePath = file.toPath();
            byte[] fileContent = Files.readAllBytes(filePath);
            String base64Encoded = Base64.getEncoder().encodeToString(fileContent);
            Message message = Message.builder()
                    .chatRoomId(chatRoomID)
                    .timestamp(System.currentTimeMillis())
                    .sentBy(username)
                    .fileName(file.getName())
                    .data(base64Encoded)
                    .type(MessageType.FILE)
                    .build();
            sendPacket(Packet.builder().type(PacketType.MESSAGE).message(message).build());
            messageList.get(chatRoomID).add(message);
            controller.updateMessage();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
