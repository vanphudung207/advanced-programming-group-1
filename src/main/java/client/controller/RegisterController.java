package client.controller;

// Import các thư viện cần thiết
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;

public class RegisterController {

    @FXML private StackPane rootPane; // Khung nền để làm hiệu ứng

    // === Đã xóa biến fx:id="txtFullName" tại đây ===

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private ComboBox<String> cbRole;
    @FXML private Button btnRegister;

    @FXML
    public void initialize() {
        // Khởi tạo ComboBox
        cbRole.getItems().addAll("Bidder", "Seller");
        cbRole.setValue("Bidder");

        // Hiệu ứng mờ dần (Fade In) sang chảnh lúc mở màn hình
        FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), rootPane);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    @FXML
    private void handleRegisterAction(ActionEvent event) {
        
        // === Đã xóa dòng lấy dữ liệu txtFullName.getText() tại đây ===

        // Bước 1: Lấy dữ liệu từ các ô còn lại
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();
        String role = cbRole.getValue(); 

        // Bước 2: Kiểm tra mật khẩu khớp nhau
        if (!password.equals(confirmPassword)) {
            System.out.println("Error: Verification password does not match!");
            return; 
        }

        System.out.println("--- PROCESSING REGISTRATION ---");
        // === Đã xóa phần in 'Họ tên' ra Console ===
        System.out.println("Username: " + username + " | Role: " + role);

        // Bước 3: Lưu vào file text thông qua MockDatabase
        MockDatabase.saveUser(username, password);
        System.out.println("=> Account created successfully: " + username);
        
        // Bước 4: Chuyển sang màn hình Đăng nhập
        this.switchToLogin(event);
        
    } // Kết thúc hàm đăng ký

    // Hàm chuyển trang dùng chung
    @FXML
    private void switchToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/Login.fxml"));
            Parent loginRoot = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            // Nhớ tăng kích thước Scene lên (ví dụ 600x550) để không bị che mất nút bấm nhé
            stage.setScene(new Scene(loginRoot, 600, 550));
            stage.setTitle("Online Auction System - Login");
            
        } catch (IOException e) {
            e.printStackTrace(); 
            System.out.println("Error: Could not load Login screen!");
        }
    }

} // Kết thúc class