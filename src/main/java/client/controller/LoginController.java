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

public class LoginController {

    @FXML private StackPane rootPane;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;
    @FXML private Button btnLogin;

    @FXML
    public void initialize() {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), rootPane);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    @FXML
    private void handleLoginAction(ActionEvent event) {
        String enteredEmail    = txtEmail.getText().trim();
        String enteredPassword = txtPassword.getText().trim();

        lblError.setText("");

        if (enteredEmail.isEmpty() || enteredPassword.isEmpty()) {
            lblError.setText("Vui lòng nhập email và mật khẩu!");
            lblError.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        // Tài khoản Admin cố định — không cần gọi Firebase
        if (enteredEmail.equals("Admin@gmail.com") && enteredPassword.equals("nhom1")) {
            FirebaseService.registeredUsername = "Admin";
            navigateTo(event, "/client/view/Admin.fxml", "Online Auction System - Admin");
            return;
        }

        // FIX: Chạy AuthService.login() trên background thread
        // tránh block JavaFX thread gây đơ / không đăng nhập được
        if (btnLogin != null) btnLogin.setDisable(true);
        lblError.setText("Đang đăng nhập...");
        lblError.setStyle("-fx-text-fill: #3498db;");

        Task<Boolean> loginTask = new Task<>() {
            @Override
            protected Boolean call() {
                return AuthService.login(enteredEmail, enteredPassword);
            }
        };

        loginTask.setOnSucceeded(e -> {
            if (loginTask.getValue()) {
                // Lưu email làm username để các màn hình khác dùng
                FirebaseService.registeredUsername = enteredEmail;
                navigateTo(event,
                    "/client/view/ProductList.fxml",
                    "Online Auction System - Danh sách sản phẩm");
            } else {
                lblError.setText("Sai email hoặc mật khẩu! Vui lòng thử lại.");
                lblError.setStyle("-fx-text-fill: #e74c3c;");
                if (btnLogin != null) btnLogin.setDisable(false);
            }
        });

        loginTask.setOnFailed(e -> {
            lblError.setText("Lỗi kết nối. Kiểm tra lại mạng!");
            lblError.setStyle("-fx-text-fill: #e74c3c;");
            if (btnLogin != null) btnLogin.setDisable(false);
        });

        new Thread(loginTask).start();
    }

    @FXML
    private void switchToRegister(ActionEvent event) {
        navigateTo(event, "/client/view/Register.fxml", "Online Auction System - Register");
    }

    private void navigateTo(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
