package cn.edu.sustech.cs209.chatting.client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@Getter
public class SceneManager {
    private final Stage stage;
    private Scene loginScene;
    private Scene registerScene;
    private Scene mainScene;
    private final Client client;

    public SceneManager(Stage stage, Client client) {
        this.stage = stage;
        this.client = client;
        initScenes();
    }

    private void initScenes() {
        try {
            FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("login.fxml"));
            FXMLLoader registerLoader = new FXMLLoader(getClass().getResource("register.fxml"));
            FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("main.fxml"));
            loginScene = new Scene(loginLoader.load());
            registerScene = new Scene(registerLoader.load());
            mainScene = new Scene(mainLoader.load());
            client.setController(mainLoader.getController());
            ((LoginController) loginLoader.getController()).setSceneManager(this);
            ((RegisterController) registerLoader.getController()).setSceneManager(this);
            ((Controller) mainLoader.getController()).setSceneManager(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showLoginScene() {
        stage.setScene(loginScene);
        stage.show();
    }

    public void showRegisterScene() {
        stage.setScene(registerScene);
        stage.show();
    }

    public void showMainScene() {
        stage.setScene(mainScene);
        client.getController().init();
        stage.show();
    }
}
