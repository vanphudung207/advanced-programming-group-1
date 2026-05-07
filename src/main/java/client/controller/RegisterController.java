package client.controller;

import client.service.AuthService;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
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
    
    // Ô nhập email
    @FXML private TextField txtEmail;

    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Button btnRegister;
    
    // ĐÃ THÊM: Liên kết với nhãn lỗi
    @FXML private Label lblError; 

    @FXML
    public void initialize() {

        FadeTransition fadeIn =
            new FadeTransition(
                Duration.millis(1000),
                rootPane
            );

        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        fadeIn.play();
    }

    @FXML
    private void handleRegisterAction(ActionEvent event) {
        
        // Lấy dữ liệu người dùng nhập
        String email =
            txtEmail.getText();

        String password =
            txtPassword.getText();

        String confirmPassword =
            txtConfirmPassword.getText();

        // 1. Reset nhãn thành màu đỏ và xóa chữ báo lỗi cũ đi
        lblError.setStyle(
            "-fx-text-fill: #e74c3c; "
            + "-fx-font-style: italic; "
            + "-fx-font-size: 13px;"
        );

        lblError.setText("");

        // 2. Kiểm tra điều kiện
        if (
            email.isEmpty()
            || password.isEmpty()
            || confirmPassword.isEmpty()
        ) {

            lblError.setText(
                "Vui lòng điền đầy đủ thông tin!"
            );

            return;
        }
        // Kiểm tra độ dài mật khẩu
        if (password.length() < 6) {

            lblError.setText(
                "Mật khẩu phải có ít nhất 6 ký tự!"
            );

            return;
        }
        // Kiểm tra xác nhận mật khẩu
        if (!password.equals(confirmPassword)) {

            lblError.setText(
                "Mật khẩu xác nhận không khớp!"
            );

            return; 
        }

        // 4. Ghi dữ liệu
        // gửi email + pw lên Authentication Firebase
        boolean isSuccess =
            AuthService.register(
                email,
                password
            );

        if (isSuccess) {

            // Nếu đăng ký thành công, đổi màu chữ thành Xanh Lá cho uy tín
            lblError.setStyle(
                "-fx-text-fill: #27ae60; "
                + "-fx-font-weight: bold; "
                + "-fx-font-size: 13px;"
            );

            lblError.setText(
                "Đăng ký thành công! Đang chuyển trang..."
            );
            
            this.switchToLogin(event);

        } else {

            lblError.setText(
                "Email đã tồn tại hoặc mật khẩu dưới 6 ký tự!"
            );
        }
    } 

    @FXML
    private void switchToLogin(ActionEvent event) {

        try {

            FXMLLoader loader =
                new FXMLLoader(
                    getClass().getResource(
                        "/client/view/Login.fxml"
                    )
                );

            Parent loginRoot =
                loader.load(); 

            Stage stage =
                (Stage)
                ((Node)
                event.getSource())
                .getScene()
                .getWindow(); 

            stage.getScene().setRoot(loginRoot); 

            stage.setTitle(
                "Online Auction System - Login"
            );

        } catch (IOException e) {

            e.printStackTrace(); 
        }
    }
}