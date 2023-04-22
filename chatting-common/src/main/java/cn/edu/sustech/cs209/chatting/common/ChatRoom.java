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
    private int unreadMessageCount;

    @JsonCreator
    public ChatRoom(@JsonProperty("id") String id,
                    @JsonProperty("type") ChatType type,
                    @JsonProperty("users") Set<String> users,
                    @JsonProperty("unreadMessageCount") int unreadMessageCount) {
        this.id = id;
        this.type = type;
        this.users = users;
        this.unreadMessageCount = unreadMessageCount;
    }

    public void addUnreadMessageCount() {
        ++unreadMessageCount;
    }

    public void resetUnreadMessageCount() {
        unreadMessageCount = 0;
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
