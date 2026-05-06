package client.controller;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label; // ĐÃ THÊM: Import Label
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;

public class RegisterController {

    @FXML private StackPane rootPane; 
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Button btnRegister;
    @FXML private TextField txtPhone; 
    
    // ĐÃ THÊM: Liên kết với nhãn lỗi
    @FXML private Label lblError; 

    @FXML
    public void initialize() {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), rootPane);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    @FXML
    private void handleRegisterAction(ActionEvent event) {
        
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();
        String phone = txtPhone.getText();

        // 1. Reset nhãn thành màu đỏ và xóa chữ báo lỗi cũ đi
        lblError.setStyle("-fx-text-fill: #e74c3c; -fx-font-style: italic; -fx-font-size: 13px;");
        lblError.setText("");

        // 2. Kiểm tra điều kiện (Thay vì bật bảng, giờ ép chữ lỗi hiển thị)
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phone.isEmpty()) {
            lblError.setText("Vui lòng điền đầy đủ thông tin (Kể cả email)!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            lblError.setText("Mật khẩu xác nhận không khớp!");
            return; 
        }

        // 4. Ghi dữ liệu //gửi email+pw lên au firebase
        boolean isSuccess =client.service.AuthService.register(username,password);

        if (isSuccess) {
            // Nếu đăng ký thành công, đổi màu chữ thành Xanh Lá cho uy tín
            lblError.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 13px;");
            lblError.setText("Đăng ký thành công! Đang chuyển trang...");
            
            this.switchToLogin(event);
        } else {
            lblError.setText("Tên tài khoản này đã tồn tại!");
        }
    } 

    @FXML
    private void switchToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/Login.fxml"));
            Parent loginRoot = loader.load(); 
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); 
            stage.getScene().setRoot(loginRoot); 
            stage.setTitle("Online Auction System - Login");
        } catch (IOException e) {
            e.printStackTrace(); 
        }
    }
}