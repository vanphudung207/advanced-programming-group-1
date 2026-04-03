package client.controller;

// Import các thư viện cần thiết
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert; // MỚI THÊM: Import Alert gốc
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;

public class RegisterController {

    @FXML private StackPane rootPane; // Khung nền để làm hiệu ứng
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Button btnRegister;
    @FXML private TextField txtPhone; // Biến nối với ô nhập Số điện thoại trên giao diện

    @FXML
    public void initialize() {
        // Hiệu ứng mờ dần (Fade In) sang chảnh lúc mở màn hình
        FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), rootPane);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    @FXML
    private void handleRegisterAction(ActionEvent event) {
        
        // Bước 1: Lấy dữ liệu từ các ô
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();
        String phone = txtPhone.getText();

        // 2. Kiểm tra điều kiện nhập liệu (Không được để trống)
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phone.isEmpty()) {
            showAlert(AlertType.WARNING, "Lỗi", "Vui lòng nhập đầy đủ thông tin (Kể cả số điện thoại)!");
            return;
        }

        // 3. Kiểm tra mật khẩu khớp nhau
        if (!password.equals(confirmPassword)) {
            // Đã sửa lại thành hiển thị Popup thay vì chỉ in ra Console
            showAlert(AlertType.WARNING, "Lỗi", "Mật khẩu xác nhận không khớp!");
            return; 
        }

        System.out.println("--- PROCESSING REGISTRATION ---");
        System.out.println("Username: " + username + " | Phone: " + phone);

        // 4. Gọi hàm MockDatabase ĐÃ NÂNG CẤP (truyền 3 tham số)
        boolean isSuccess = MockDatabase.registerUser(username, password, phone);

        // 5. Kiểm tra kết quả ghi file
        if (isSuccess) {
            showAlert(AlertType.INFORMATION, "Thành công", "Đăng ký thành công! Hãy quay lại trang Đăng nhập.");
            System.out.println("=> Account created successfully: " + username);
            
            // Chuyển sang màn hình Đăng nhập
            this.switchToLogin(event);
        } else {
            // Nếu hàm trả về false tức là tài khoản đã tồn tại
            showAlert(AlertType.ERROR, "Lỗi", "Tên tài khoản này đã tồn tại!");
            System.out.println("=> Account creation failed: Username exists.");
        }
    } 

    // ==============================================================
    // HÀM CHUYỂN TRANG ĐĂNG NHẬP
    // ==============================================================
    @FXML
    private void switchToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/Login.fxml"));
            Parent loginRoot = loader.load(); // Biến tên là loginRoot
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); 
            
            // ĐÃ SỬA: Dùng đúng tên biến loginRoot
            stage.getScene().setRoot(loginRoot); 
            
            stage.setTitle("Online Auction System - Login");
            
        } catch (IOException e) {
            e.printStackTrace(); 
            System.out.println("Error: Could not load Login screen!");
        }
    }

    // ==============================================================
    // HÀM HỖ TRỢ: HIỂN THỊ POPUP THÔNG BÁO (Bắt buộc phải có)
    // ==============================================================
    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null); 
        alert.setContentText(content);
        alert.showAndWait(); 
    }

} // Kết thúc class