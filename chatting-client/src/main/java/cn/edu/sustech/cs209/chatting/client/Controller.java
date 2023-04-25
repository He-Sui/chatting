package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.*;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.util.concurrent.atomic.AtomicReference;
import javafx.stage.Stage;
import java.util.*;
import java.util.stream.Collectors;
import javafx.util.Callback;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Controller {

  public void setSceneManager(SceneManager sceneManager) {
    this.sceneManager = sceneManager;
  }

  SceneManager sceneManager;
  Client client;

  @FXML
  ListView<Message> chatContentList;

  @FXML
  Label currentUsername;

  @FXML
  Label currentOnlineCnt;

  @FXML
  ListView<ChatRoom> chatList;

  @FXML
  TextArea inputArea;

  ChatRoom currentChat;

  public void init() {
    client = sceneManager.getClient();
    inputArea.setOnDragOver(event -> {
      if (event.getGestureSource() != inputArea && event.getDragboard().hasFiles()) {
        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        event.consume();
      }
    });
    inputArea.setOnDragDropped(event -> {
      if (currentChat == null) {
        return;
      }
      Dragboard db = event.getDragboard();
      if (db.hasFiles()) {
        File file = db.getFiles().get(0);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Are you sure you want to send this file?");
        alert.setContentText("Press OK to confirm, or Cancel to abort.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
          client.sendFile(currentChat.getId(), file);
        }
      }
      event.setDropCompleted(true);
      event.consume();
    });
    sceneManager.getStage().setOnCloseRequest(e -> {
      closeClient();
      Platform.exit();
    });
    client.startReceivingPacket();
    currentUsername.setText("Current User: " + client.getUsername());
    chatContentList.setCellFactory(new MessageCellFactory());
    chatList.setCellFactory(new ChatRoomCellFactory());
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
    userSel.getItems().addAll(client.getUsers());
    val okBtn = new Button("OK");
    okBtn.setOnAction(e -> {
      user.set(userSel.getSelectionModel().getSelectedItem());
      if (user.get() == null) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText("Please select a user to start a private chat.");
        alert.showAndWait();
      } else {
        currentChat = client.createPrivateChat(user.get());
        updateChatList();
        updateMessage();
        stage.close();
      }
    });
    val cancelBtn = new Button("Cancel");
    cancelBtn.setOnAction(e -> {
      stage.close();
    });
    val vbox = new VBox(10);
    vbox.setAlignment(Pos.CENTER);
    vbox.setPadding(new Insets(20, 20, 20, 20));
    vbox.getChildren().addAll(
        new Label("Select a user to start a private chat with:"),
        userSel,
        new HBox(10, okBtn, cancelBtn) {{
          setAlignment(Pos.CENTER);
        }}
    );
    Scene scene = new Scene(vbox, 300, 150);
    stage.setScene(scene);
    stage.showAndWait();
  }


  public void updateOnlineCnt() {
    Platform.runLater(() -> {
      currentOnlineCnt.setText("Online: " + (client.getUsers().size() + 1));
    });
  }

  public void updateChatList() {
    Platform.runLater(() -> {
      if (currentChat != null) {
        currentChat.resetUnreadMessageCount();
      }
      chatList.getItems().clear();
      chatList.getItems().addAll(client.getChatRooms().stream().sorted((c1, c2) -> {
            List<Message> m1 = client.getMessageList().get(c1.getId());
            List<Message> m2 = client.getMessageList().get(c2.getId());
            Long t1 = m1 == null || m1.isEmpty() ? 0 : m1.get(m1.size() - 1).getTimestamp();
            Long t2 = m2 == null || m2.isEmpty() ? 0 : m2.get(m2.size() - 1).getTimestamp();
            return t2.compareTo(t1);
          }
      ).toList());
      ObservableList<ChatRoom> chatRooms = chatList.getItems();
      int index = chatRooms.indexOf(currentChat);
      chatList.getSelectionModel().select(index);
    });
  }

  @FXML
  public void createGroupChat() {
    val stage = new Stage();
    val userListView = new ListView<String>();
    Set<String> selectedUsers = new HashSet<>();
    userListView.getItems().addAll(client.getUsers());
    userListView.setCellFactory(
        CheckBoxListCell.forListView(new Callback<String, ObservableValue<Boolean>>() {
          @Override
          public ObservableValue<Boolean> call(String item) {
            CheckBox checkBox = new CheckBox(item);
            checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
              if (newValue) {
                selectedUsers.add(item);
              } else {
                selectedUsers.remove(item);
              }
            });
            return checkBox.selectedProperty();
          }
        }));

    val okBtn = new Button("OK");
    okBtn.setOnAction(e -> {
      if (selectedUsers.isEmpty()) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText("Please select at least one user to start a group chat with.");
        alert.showAndWait();
      } else {
        currentChat = client.createGroupChat(selectedUsers);
        updateChatList();
        updateMessage();
        stage.close();
      }
    });

    val cancelBtn = new Button("Cancel");
    cancelBtn.setOnAction(e -> stage.close());

    val buttonBox = new HBox(10, okBtn, cancelBtn);
    buttonBox.setAlignment(Pos.CENTER);

    val vbox = new VBox(10);
    vbox.setAlignment(Pos.CENTER);
    vbox.setPadding(new Insets(20, 20, 20, 20));
    vbox.getChildren().addAll(
        new Label("Select users to start a group chat with:"),
        userListView,
        buttonBox
    );

    Scene scene = new Scene(vbox, 300, 400);
    stage.setScene(scene);
    stage.showAndWait();
  }

  @FXML
  public void doSendMessage() {
    if (currentChat != null && !inputArea.getText().isEmpty()) {
      Message message = Message.builder()
          .type(MessageType.TEXT)
          .timestamp(System.currentTimeMillis())
          .sentBy(client.getUsername())
          .chatRoomId(currentChat.getId())
          .data(inputArea.getText())
          .build();
      client.sendPacket(Packet.builder()
          .type(PacketType.MESSAGE)
          .message(message)
          .build());
      client.getMessageLogger().log(message);
      client.getMessageList().get(currentChat.getId()).add(message);
      chatContentList.getItems().add(message);
      inputArea.clear();
      updateChatList();
    }
  }

  @FXML
  public void handleChatSelection() {
    currentChat = chatList.getSelectionModel().getSelectedItem();
    updateChatList();
    updateMessage();
  }

  public void updateMessage() {
    Platform.runLater(() -> {
      if (currentChat == null) {
        return;
      }
      chatContentList.getItems().clear();
      client.getMessageList().get(currentChat.getId())
          .forEach(chatContentList.getItems()::add);
    });
  }

  public void serverLogout() {
    Platform.runLater(() -> {
      Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.setHeaderText(null);
      alert.setContentText("The server has been shut down");
      alert.showAndWait();
      Platform.exit();
    });

  }

  public class ChatRoomCellFactory implements Callback<ListView<ChatRoom>, ListCell<ChatRoom>> {

    @Override
    public ListCell<ChatRoom> call(ListView<ChatRoom> param) {
      return new ListCell<ChatRoom>() {
        private final StackPane pane = new StackPane();
        private final Circle dot = new Circle(4, Color.RED);

        {
          pane.getChildren().add(dot);
          dot.setVisible(false);
          pane.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        public void updateItem(ChatRoom item, boolean empty) {
          super.updateItem(item, empty);
          if (empty || item == null) {
            setText(null);
            setGraphic(null);
            return;
          }
          List<Message> m = client.getMessageList().get(item.getId());
          long t = m == null || m.isEmpty() ? 0 : m.get(m.size() - 1).getTimestamp();
          Date date = new Date(t);
          SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
          if (item.getType() == ChatType.PRIVATE_CHAT) {
            StringBuilder sb = new StringBuilder();
            sb.append(
                item.getUsers().stream().filter(u -> !u.equals(client.getUsername())).findFirst()
                    .orElse(""));
            if (client.getUsers().contains(
                item.getUsers().stream().filter(u -> !u.equals(client.getUsername())).findFirst()
                    .orElse(""))) {
              sb.append(" [Online] ");
            } else {
              sb.append(" [Offline] ");
            }
            if (t > 0) {
              sb.append(sdf.format(date));
            }
            setText(sb.toString());
          } else {
            StringBuilder sb = new StringBuilder();
            sb.append(item.getUsers().stream()
                    .sorted()
                    .collect(Collectors.joining(", ")))
                .append(" (")
                .append(item.getUsers().size())
                .append(") ");
            if (t > 0) {
              sb.append(sdf.format(date));
            }
            setText(sb.toString());
          }
          if (item.getUnreadMessageCount() > 0 && item != currentChat) {
            dot.setVisible(true);
            setGraphic(pane);
          } else {
            dot.setVisible(false);
            setGraphic(pane);
          }
        }
      };
    }
  }

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

          HBox messagePane = new HBox();
          Label senderLabel = new Label(msg.getSentBy());
          senderLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
          if (msg.getType() == MessageType.TEXT) {
            Label msgLabel = new Label(msg.getData());
            msgLabel.setWrapText(true);
            if (msg.getSentBy() == null) {
              messagePane.setAlignment(Pos.CENTER);
              msgLabel.setStyle("-fx-background-color: #CCCCCC; -fx-padding: 5px;");
              senderLabel.setStyle("-fx-text-fill: #808080;");
              messagePane.getChildren().add(msgLabel);
            } else if (msg.getSentBy().equals(client.getUsername())) {
              messagePane.setAlignment(Pos.TOP_RIGHT);
              msgLabel.setStyle("-fx-background-color: #ADD8E6; -fx-padding: 5px;");
              senderLabel.setStyle("-fx-text-fill: #0000FF;");
              messagePane.getChildren().addAll(msgLabel, senderLabel);
            } else {
              messagePane.setAlignment(Pos.TOP_LEFT);
              msgLabel.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 5px;");
              senderLabel.setStyle("-fx-text-fill: #008000;");
              messagePane.getChildren().addAll(senderLabel, msgLabel);
            }

          } else if (msg.getType() == MessageType.FILE) {
            Label fileLabel = new Label(msg.getFileName());
            fileLabel.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 5px;");
            fileLabel.setCursor(Cursor.HAND);
            fileLabel.setOnMouseClicked(e -> {
              try {
                client.downloadFile(msg);
              } catch (IOException ex) {
                ex.printStackTrace();
              }
            });
            if (msg.getSentBy() == null) {
              messagePane.setAlignment(Pos.CENTER);
              senderLabel.setStyle("-fx-text-fill: #808080;");
              messagePane.getChildren().add(fileLabel);
            } else if (msg.getSentBy().equals(client.getUsername())) {
              messagePane.setAlignment(Pos.TOP_RIGHT);
              senderLabel.setStyle("-fx-text-fill: #0000FF;");
              messagePane.getChildren().addAll(fileLabel, senderLabel);
            } else {
              messagePane.setAlignment(Pos.TOP_LEFT);
              senderLabel.setStyle("-fx-text-fill: #008000;");
              messagePane.getChildren().addAll(senderLabel, fileLabel);
            }
          }
          setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
          setGraphic(messagePane);
        }
      };
    }
  }
}
