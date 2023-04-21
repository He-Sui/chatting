package cn.edu.sustech.cs209.chatting.client;

import javafx.application.Application;
import javafx.stage.Stage;
import lombok.SneakyThrows;

public class Main extends Application {

    public static void main(String[] args) {
        launch();
    }

    @Override
    @SneakyThrows
    public void start(Stage stage) {
        stage.setTitle("Chatting Client");
        Client client = new Client("localhost", 2345);
        SceneManager sceneManager = new SceneManager(stage, client);
        sceneManager.showLoginScene();
    }
}
