package cn.edu.sustech.cs209.chatting.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Message {
    private String chatRoomId;

    private Long timestamp;

    private String sentBy;

    private String data;

    @JsonCreator
    public Message(@JsonProperty("chatRoomId") String chatRoomId,
                   @JsonProperty("timestamp") Long timestamp,
                   @JsonProperty("sentBy") String sentBy,
                   @JsonProperty("data") String data) {
        this.chatRoomId = chatRoomId;
        this.timestamp = timestamp;
        this.sentBy = sentBy;
        this.data = data;
    }
}
