package cn.edu.sustech.cs209.chatting.common;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ChatLogger<T> {
    private final ObjectMapper objectMapper;
    private final Path path;

    public ChatLogger(Path path) {
        this.path = path;
        objectMapper = new ObjectMapper();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public synchronized void log(T message) {
        try {
            if (message instanceof ChatRoom)
                ((ChatRoom) message).resetUnreadMessageCount();
            String messageJson = objectMapper.writeValueAsString(message);
            Files.write(path, (messageJson + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
