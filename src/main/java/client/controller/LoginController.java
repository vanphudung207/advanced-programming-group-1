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
            // Đăng nhập đúng thì lưu tên vào biến tạm để màn hình sau còn biết ai đang đăng nhập
            MockDatabase.registeredUsername = enteredUsername; 
            
            System.out.println("Login successful! Transitioning to the home screen...");
            
            // Code chuyển sang màn hình Danh sách sản phẩm (ProductList.fxml)
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/ProductList.fxml"));
                javafx.scene.Parent root = loader.load();
                javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                stage.setScene(new javafx.scene.Scene(root, 900, 650)); // Kích thước to đùng để chứa nhiều hàng

                stage.setResizable(true);// Dòng này cho phép bạn dùng chuột kéo thả các góc để thay đổi kích thước tự do

                // Đảm bảo cửa sổ không bao giờ bị kéo nhỏ hơn 800x600
                stage.setMinWidth(800);
                stage.setMinHeight(600);

                stage.setMaximized(true);
                stage.centerOnScreen(); // Đẩy cửa sổ ra giữa màn hình máy tính
            } 
            catch (Exception e) {
                e.printStackTrace();
            }
        } 
        else {
            System.out.println("Login failed! Incorrect username or password.");
        }
    }

    @FXML
    private void switchToRegister(javafx.event.ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/Register.fxml"));
            javafx.scene.Parent registerRoot = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            
            stage.setScene(new javafx.scene.Scene(registerRoot, 600, 550));
            stage.setTitle("Online Auction System - Register");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}