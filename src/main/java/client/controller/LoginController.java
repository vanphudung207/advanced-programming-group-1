package client.controller;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert; // MỚI THÊM: Import Alert để tạo thông báo
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.StackPane; 
import javafx.util.Duration;

public class LoginController {

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
            // Đăng nhập đúng thì lưu tên vào biến tạm
            MockDatabase.registeredUsername = enteredUsername; 
            
            System.out.println("Login successful! Transitioning to the home screen...");
            
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/ProductList.fxml"));
                javafx.scene.Parent root = loader.load();
                javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                
                // ==============================================================
                // ĐÃ SỬA: Áp dụng CÔNG THỨC VÀNG (Thay ruột giữ vỏ)
                // ==============================================================
                stage.getScene().setRoot(root);
                stage.setTitle("Online Auction System - Danh sách sản phẩm"); 
                
            } 
            catch (Exception e) {
                e.printStackTrace();
            }
        } 
        else {
            System.out.println("Login failed! Incorrect username or password.");
            // ĐÃ NÂNG CẤP: Hiển thị bảng thông báo lỗi chuyên nghiệp
            showAlert(AlertType.ERROR, "Đăng nhập thất bại", "Sai tên tài khoản hoặc mật khẩu! Vui lòng thử lại.");
        }
    }

    @FXML
    private void switchToRegister(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/Register.fxml"));
            javafx.scene.Parent registerRoot = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            
            // ==============================================================
            // ĐÃ SỬA: Áp dụng CÔNG THỨC VÀNG (Thay ruột giữ vỏ)
            // ==============================================================
            stage.getScene().setRoot(registerRoot);
            stage.setTitle("Online Auction System - Register");
            
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // ==============================================================
    // HÀM HỖ TRỢ: HIỂN THỊ POPUP THÔNG BÁO
    // ==============================================================
    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null); 
        alert.setContentText(content);
        alert.showAndWait(); 
    }
}