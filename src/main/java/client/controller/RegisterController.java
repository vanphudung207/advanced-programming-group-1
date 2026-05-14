package client.controller;

import client.service.AuthService;
import client.service.FirebaseService;
import javafx.animation.FadeTransition;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;

public class RegisterController {

    @FXML private StackPane rootPane;
    @FXML private TextField txtUsername;   // dùng làm email
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Button btnRegister;
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
        String email           = txtUsername.getText().trim();
        String password        = txtPassword.getText().trim();
        String confirmPassword = txtConfirmPassword.getText().trim();

        lblError.setStyle("-fx-text-fill: #e74c3c; -fx-font-style: italic; -fx-font-size: 13px;");
        lblError.setText("");

        // Kiểm tra trống
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            lblError.setText("Vui lòng điền đầy đủ thông tin!");
            return;
        }
        
        // Kiểm tra xác nhận mật khẩu
        if (!password.equals(confirmPassword)) {
            lblError.setText("Mật khẩu xác nhận không khớp!");
            return;
        }
        
        // Kiểm tra độ dài mật khẩu (Code của bạn thêm vào - Rất chuẩn!)
        if (password.length() < 6) {
            lblError.setText("Mật khẩu phải có ít nhất 6 ký tự!");
            return;
        }

        // --- Bắt đầu xử lý đăng ký (Code giao diện Loading) ---
        if (btnRegister != null) btnRegister.setDisable(true);
        lblError.setStyle("-fx-text-fill: #3498db; -fx-font-size: 13px;");
        lblError.setText("Đang đăng ký...");

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                // Chỉ đăng ký qua Firebase Auth
                return AuthService.register(email, password);
            }
        };

        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                Thread syncThread = new Thread(() -> FirebaseService.syncOldUserIfNeeded(email));
                syncThread.setDaemon(true);
                syncThread.start();
                lblError.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 13px;");
                lblError.setText("Đăng ký thành công! Đang chuyển trang...");
                switchToLogin(event);
            } else {
                lblError.setStyle("-fx-text-fill: #e74c3c; -fx-font-style: italic; -fx-font-size: 13px;");
                lblError.setText("Email này đã được đăng ký hoặc không hợp lệ!");
                if (btnRegister != null) btnRegister.setDisable(false);
            }
        });

        task.setOnFailed(e -> {
            lblError.setText("Lỗi kết nối. Kiểm tra lại mạng!");
            if (btnRegister != null) btnRegister.setDisable(false);
        });

        new Thread(task).start();
    }

    @FXML
    private void switchToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Online Auction System - Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}