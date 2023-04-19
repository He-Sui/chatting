package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.Packet;
import cn.edu.sustech.cs209.chatting.common.PacketType;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Slf4j
public class Client {
    private final Socket socket;
    private final Controller controller;
    private final BufferedReader in;
    private final BufferedWriter out;
    private String username;
    private final ObjectMapper objectMapper;
    private Set<String> users;

    private Set<String> privateChat;

    private List<Message> messageList;
    private Thread receivingPacketThread;

    public Client(String host, int port, String username, Controller controller) {
        privateChat = new HashSet<>();
        this.controller = controller;
        objectMapper = new ObjectMapper();
        this.username = username;
        users = new HashSet<>();
        messageList = new ArrayList<>();
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
                messageList.add(packet.getMessage());
                controller.updateMessage();
            }
            case NEW_USER -> {
                if (!packet.getAddition().equals(username)) {
                    users.add(packet.getAddition());
                    controller.updateOnlineCnt();
                }
            }
            case PRIVATE_CHAT -> {
                privateChat.add(packet.getAddition());
                Platform.runLater(() -> {
                    controller.getChatList().getItems().add(packet.getAddition());
                });
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
        sendPacket(Packet.builder().type(PacketType.LOGIN).addition(username).build());
        Packet receive = receivePacket();
        if (receive.getType() == PacketType.LOGIN_SUCCESS) {
            log.info("Login success");
        } else {
            log.info("Login failed: {}", receive.getAddition());
            throw new RuntimeException("Login failed: " + receive.getAddition());
        }
    }

    public void createPrivateChat(String username) {
        privateChat.add(username);
        sendPacket(Packet.builder().type(PacketType.PRIVATE_CHAT).addition(username).build());
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
