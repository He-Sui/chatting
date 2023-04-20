package cn.edu.sustech.cs209.chatting.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class ChatRoom {
    private String id;
    private ChatType type;
    private Set<String> users;

    @JsonCreator
    public ChatRoom(@JsonProperty("id") String id,
                    @JsonProperty("type") ChatType type,
                    @JsonProperty("users") Set<String> users) {
        this.id = id;
        this.type = type;
        this.users = users;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj instanceof ChatRoom chatRoom) {
            return chatRoom.getId().equals(this.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
