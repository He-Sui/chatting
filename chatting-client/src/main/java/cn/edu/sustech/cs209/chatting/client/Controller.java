package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.Packet;
import cn.edu.sustech.cs209.chatting.common.PacketType;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Controller implements Initializable {

    @FXML
    ListView<Message> chatContentList;

    public ListView<String> getChatList() {
        return chatList;
    }

    @FXML
    Label currentUsername;

    @FXML
    Label currentOnlineCnt;

    @FXML
    ListView<String> chatList;

    @FXML
    TextArea inputArea;

    String username;

    Client client;

    String currentChat;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");

        Optional<String> input = dialog.showAndWait();
        if (input.isPresent() && !input.get().isEmpty()) {
            username = input.get();
            client = new Client("localhost", 2345, username, this);
            try {
                client.login();
            } catch (Exception e) {
                log.error("Invalid username {}, exiting", input);
                Platform.exit();
            }
        } else {
            log.warn("Invalid username {}, exiting", input);
            Platform.exit();
        }
        client.startReceivingPacket();
        currentUsername.setText("Current User: " + username);
        chatContentList.setCellFactory(new MessageCellFactory());
    }

    public void closeClient() {
        client.getReceivingPacketThread().interrupt();
        try {
            client.getSocket().close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    @FXML
    public void createPrivateChat() {
        AtomicReference<String> user = new AtomicReference<>();
        val stage = new Stage();
        val userSel = new ComboBox<String>();
        log.info("Users: {}", client.getUsers());
        userSel.getItems().addAll(client.getUsers());

        val okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();
        });

        val box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();
        if (!client.getPrivateChat().contains(user.get())) {
            client.createPrivateChat(Objects.requireNonNull(user.get()));
            chatList.getItems().add(user.get());
        }
        currentChat = user.get();
        ObservableList<String> items = chatList.getItems();
        int index = items.indexOf(currentChat);
        chatList.getSelectionModel().select(index);
    }

    void updateOnlineCnt() {
        Platform.runLater(() -> {
            currentOnlineCnt.setText("Online: " + (client.getUsers().size() + 1));
        });
    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() {
    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() {
        if (currentChat != null && !inputArea.getText().isEmpty()) {
            Message message = Message.builder()
                    .timestamp(System.currentTimeMillis())
                    .sentBy(username)
                    .sendTo(currentChat)
                    .data(inputArea.getText())
                    .build();
            client.sendPacket(Packet.builder()
                    .type(PacketType.MESSAGE)
                    .message(message)
                    .build());
            client.getMessageList().add(message);
            chatContentList.getItems().add(message);
            inputArea.clear();
        }
    }

    @FXML
    public void handleChatSelection() {
        currentChat = chatList.getSelectionModel().getSelectedItem();
        updateMessage();
    }

    public void updateMessage() {
        log.info(client.getMessageList().stream().map(Message::toString).reduce("", (a, b) -> a + b));
        Platform.runLater(() -> {
            chatContentList.getItems().clear();
            client.getMessageList().stream()
                    .filter(m -> m.getSendTo().equals(currentChat) || m.getSentBy().equals(currentChat))
                    .forEach(m -> chatContentList.getItems().add(m));
        });
    }

    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {
                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || msg == null) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    Label senderLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());
                    msgLabel.setWrapText(true);

                    HBox messagePane = new HBox();
                    if (msg.getSentBy().equals(username)) {
                        messagePane.setAlignment(Pos.TOP_RIGHT);
                        msgLabel.setStyle("-fx-background-color: #ADD8E6; -fx-padding: 5px;");
                        senderLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #0000FF;");
                        messagePane.getChildren().addAll(msgLabel, senderLabel);
                    } else {
                        messagePane.setAlignment(Pos.TOP_LEFT);
                        senderLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #008000;");
                        msgLabel.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 5px;");
                        messagePane.getChildren().addAll(senderLabel, msgLabel);
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(messagePane);
                }
            };
        }
    }
}
