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
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Slf4j
public class Client {
    private ChatLogger<Message> messageLogger;
    private ChatLogger<ChatRoom> romLogger;
    private ChatLogReader<Message> messageReader;
    private ChatLogReader<ChatRoom> roomReader;
    private static final String BASE_PATH = "/Users/suih/chatting/";
    private final Socket socket;

    public void setReceivingPacketThread(Thread receivingPacketThread) {
        this.receivingPacketThread = receivingPacketThread;
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    private Controller controller;
    private final BufferedReader in;
    private final BufferedWriter out;
    private String username;
    private final ObjectMapper objectMapper;
    private final Set<String> users;

    private final Set<ChatRoom> chatRooms;
    private final Map<String, ChatRoom> chatRoomMap;
    private final Map<String, List<Message>> messageList;
    private Thread receivingPacketThread;

    public Client(String host, int port) {
        chatRooms = new HashSet<>();
        objectMapper = new ObjectMapper();
        users = new HashSet<>();
        messageList = new ConcurrentHashMap<>();
        chatRoomMap = new ConcurrentHashMap<>();
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void initializeLogger() {
        Path messagePath = Path.of(BASE_PATH + username + "/message.dat");
        Path roomPath = Path.of(BASE_PATH + username + "/room.dat");
        messageLogger = new ChatLogger<>(messagePath);
        romLogger = new ChatLogger<>(roomPath);
        messageReader = new ChatLogReader<>(Message.class, messagePath);
        roomReader = new ChatLogReader<>(ChatRoom.class, roomPath);
        roomReader.readChatLog().forEach(chatRoom -> {
            chatRooms.add(chatRoom);
            chatRoomMap.put(chatRoom.getId(), chatRoom);
            messageList.put(chatRoom.getId(), new ArrayList<>());
        });
        messageReader.readChatLog().forEach(message -> {
            messageList.get(message.getChatRoomId()).add(message);
        });
        controller.updateChatList();
    }

    public void handlePacket(Packet packet) {
        if (packet == null)
            return;
        switch (packet.getType()) {
            case MESSAGE -> {
                messageList.get(packet.getMessage().getChatRoomId()).add(packet.getMessage());
                controller.updateMessage();
                chatRoomMap.get(packet.getMessage().getChatRoomId()).addUnreadMessageCount();
                messageLogger.log(packet.getMessage());
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
                chatRoomMap.put(packet.getChatRoom().getId(), packet.getChatRoom());
                messageList.put(packet.getChatRoom().getId(), new ArrayList<>());
                romLogger.log(packet.getChatRoom());
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

    public void register(String username, String password) {
        sendPacket(Packet.builder().type(PacketType.REGISTER).user(User.builder().username(username).password(password).build()).build());
        Packet receive = receivePacket();
        if (receive.getType() == PacketType.REGISTER_SUCCESS) {
            log.info("Register Success");
        } else {
            log.info("Register Failed");
            throw new RuntimeException(receive.getInfo());
        }
    }

    public void login(String username, String password) {
        sendPacket(Packet.builder().type(PacketType.LOGIN).user(User.builder().username(username).password(password).build()).build());
        Packet receive = receivePacket();
        if (receive.getType() == PacketType.LOGIN_SUCCESS) {
            log.info("Login Success");
            this.username = username;
        } else {
            log.info("Login Failed");
            throw new RuntimeException(receive.getInfo());
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
            chatRoomMap.put(chatRoom.getId(), chatRoom);
            romLogger.log(chatRoom);
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
            chatRoomMap.put(chatRoom.getId(), chatRoom);
            romLogger.log(chatRoom);
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

    public void setUsername(String username) {
        this.username = username;
    }

    public void downloadFile(Message msg) throws IOException {
        String filename = msg.getFileName();
        byte[] fileData = Base64.getDecoder().decode(msg.getData());
        String basePath = "/Users/suih/Downloads/chatting/";
        String savePath = basePath + username + "/";
        File dir = new File(savePath);
        if (!dir.exists())
            dir.mkdirs();
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
            messageLogger.log(message);
            controller.updateMessage();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
