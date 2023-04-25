package cn.edu.sustech.cs209.chatting.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class User {

  private String username;
  private String password;

  @JsonCreator
  public User(@JsonProperty("username") String username,
      @JsonProperty("password") String password) {
    this.username = username;
    this.password = password;
  }
}
