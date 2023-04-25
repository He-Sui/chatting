package cn.edu.sustech.cs209.chatting.server;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

  public static void main(String[] args) {
    Server server = new Server(2345);
    server.start();
    log.info("Starting server");
  }
}
