package client.controller;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
// Import StackPane thay cho VBox
import javafx.scene.layout.StackPane; 
import javafx.util.Duration;

public class LoginController {

    // CHÚ Ý: Đã đổi thành StackPane để khớp với file Login.fxml mới
    @FXML
    private StackPane rootPane; 

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    @FXML
    public void initialize() {
        // Hiệu ứng mờ dần
        FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), rootPane);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    @FXML
    private void handleLoginAction(ActionEvent event) {
        String enteredUsername = txtUsername.getText(); 
        String enteredPassword = txtPassword.getText(); 

        if (MockDatabase.checkLogin(enteredUsername, enteredPassword)) {
            System.out.println("Login successful! Welcome: " + enteredUsername);
        } else {
            System.out.println("Login failed! Incorrect username or password.");
        }
    }
}