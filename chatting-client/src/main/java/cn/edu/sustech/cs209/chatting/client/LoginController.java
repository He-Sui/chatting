package cn.edu.sustech.cs209.chatting.client;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginController {

  SceneManager sceneManager;

  public void setSceneManager(SceneManager sceneManager) {
    this.sceneManager = sceneManager;
  }

  @FXML
  TextField usernameTextField;
  @FXML
  PasswordField passwordTextField;

  @FXML
  public void login() {
    String username = usernameTextField.getText();
    String password = passwordTextField.getText();
    if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
      Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.setHeaderText("Login Failed");
      alert.setContentText("Username or Password is Empty");
      alert.showAndWait();
    } else {
      try {
        sceneManager.getClient().login(username, password);
        sceneManager.showMainScene();
      } catch (RuntimeException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Login Failed");
        alert.setContentText(e.getMessage());
        alert.showAndWait();

      }
    }
    passwordTextField.setText(null);
  }

  @FXML
  public void showRegister() throws IOException {
    sceneManager.showRegisterScene();
  }
}
