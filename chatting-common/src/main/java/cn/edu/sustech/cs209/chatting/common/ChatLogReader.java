package cn.edu.sustech.cs209.chatting.common;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ChatLogReader<T> {
    private final ObjectMapper objectMapper;
    private final Class<T> type;
    private final Path path;

    public ChatLogReader(Class<T> type, Path path) {
        objectMapper = new ObjectMapper();
        this.type = type;
        this.path = path;
    }

    public List<T> readChatLog() {
        List<T> messages = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                T message = objectMapper.readValue(line, type);
                messages.add(message);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return messages;
    }
}
