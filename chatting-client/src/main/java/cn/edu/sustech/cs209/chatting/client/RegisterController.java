package cn.edu.sustech.cs209.chatting.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterController {
    SceneManager sceneManager;

    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    @FXML
    TextField usernameTextField;

    @FXML
    PasswordField passwordTextField;

    @FXML
    PasswordField confirmPasswordTextField;

    @FXML
    Button registerButton;

    @FXML
    void handleRegisterButtonAction(ActionEvent event) {
        String username = usernameTextField.getText();
        String password = passwordTextField.getText();
        String confirmPassword = confirmPasswordTextField.getText();
        if (username == null || username.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Register Failed");
            alert.setContentText("Username is empty");
            alert.showAndWait();
            usernameTextField.setText(null);
            passwordTextField.setText(null);
            confirmPasswordTextField.setText(null);
        } else if (password == null || password.length() < 6) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Register Failed");
            alert.setContentText("Password Should Longer than 6 Characters");
            alert.showAndWait();
            passwordTextField.setText(null);
            confirmPasswordTextField.setText(null);
        } else if (!password.equals(confirmPassword)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Register Failed");
            alert.setContentText("Password and Confirm Password are Not the Same");
            alert.showAndWait();
            passwordTextField.setText(null);
            confirmPasswordTextField.setText(null);
        } else {
            try {
                sceneManager.getClient().register(username, password);
                sceneManager.showLoginScene();
            } catch (RuntimeException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Register Failed");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
                usernameTextField.setText(null);
                passwordTextField.setText(null);
                confirmPasswordTextField.setText(null);
            }
        }
    }

    @FXML
    public void backToLogin() {
        sceneManager.showLoginScene();
    }
}
