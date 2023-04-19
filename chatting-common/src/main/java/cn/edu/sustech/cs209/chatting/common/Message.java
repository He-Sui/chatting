package cn.edu.sustech.cs209.chatting.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Message {

    private Long timestamp;

    private String sentBy;

    private String sendTo;

    private String data;

    @JsonCreator
    public Message(@JsonProperty("timestamp") Long timestamp,
                   @JsonProperty("sentBy") String sentBy,
                   @JsonProperty("sendTo") String sendTo,
                   @JsonProperty("data") String data) {
        this.timestamp = timestamp;
        this.sentBy = sentBy;
        this.sendTo = sendTo;
        this.data = data;
    }
}
