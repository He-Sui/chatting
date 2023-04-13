package cn.edu.sustech.cs209.chatting.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Packet {
    private PacketType type;
    private String addition;
    private Message message;

    @JsonCreator
    public Packet(@JsonProperty("type") PacketType type,
                  @JsonProperty("addition") String addition,
                  @JsonProperty("message") Message message) {
        this.type = type;
        this.addition = addition;
        this.message = message;
    }
}
