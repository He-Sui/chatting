package cn.edu.sustech.cs209.chatting.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Packet {
    private PacketType type;
    private String info;
    private User user;
    private ChatRoom chatRoom;
    private Message message;

    @JsonCreator
    public Packet(@JsonProperty("type") PacketType type,
                  @JsonProperty("info") String info,
                  @JsonProperty("user") User user,
                  @JsonProperty("chatRoom") ChatRoom chatRoom,
                  @JsonProperty("message") Message message) {
        this.type = type;
        this.info = info;
        this.user = user;
        this.chatRoom = chatRoom;
        this.message = message;
    }
}
