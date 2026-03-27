// Khai báo package chứa file controller của Client
package client.controller;

// Import các thư viện UI cần thiết từ JavaFX
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.io.IOException;

// Bắt đầu khai báo lớp RegisterController để xử lý sự kiện cho màn hình Đăng ký
public class RegisterController {

    // ==============================================================
    // 1. KHAI BÁO CÁC BIẾN LIÊN KẾT GIAO DIỆN (@FXML)
    // ==============================================================

    @FXML private TextField txtFullName;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private ComboBox<String> cbRole;
    @FXML private Button btnRegister;

    // ==============================================================
    // 2. HÀM KHỞI TẠO DỮ LIỆU BAN ĐẦU
    // ==============================================================

    // Hàm initialize() sẽ tự động chạy MỘT LẦN DUY NHẤT ngay sau khi FXML load lên
    @FXML
    public void initialize() {
        // Thêm 2 lựa chọn vai trò vào hộp thoại xổ xuống
        cbRole.getItems().addAll("Bidder", "Seller");
        // Cài đặt giá trị mặc định được chọn sẵn là "Bidder"
        cbRole.setValue("Bidder");
    }

    // ==============================================================
    // 3. HÀM XỬ LÝ KHI BẤM NÚT "ĐĂNG KÝ"
    // ==============================================================

    @FXML
    private void handleRegisterAction(ActionEvent event) {
        
        // Bước 1: Lấy toàn bộ dữ liệu người dùng vừa nhập
        String fullName = txtFullName.getText();
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();
        String role = cbRole.getValue(); 

        // Bước 2: Kiểm tra dữ liệu hợp lệ (Mật khẩu phải khớp nhau)
        if (!password.equals(confirmPassword)) {
            System.out.println("Error: Verification password does not match!");
            return; // Dừng hàm lại ngay tại đây, không cho đăng ký tiếp
        }

        // Bước 3: In thông tin ra Console để dev kiểm tra luồng dữ liệu
        System.out.println("--- NEW ACCOUNT REGISTRATION IS BEING PROCESSED ---");
        System.out.println("Full name: " + fullName + " | Account: " + username + " | Role: " + role);

        // Bước 4: Lưu dữ liệu vào Bộ nhớ tạm (Giả lập việc lưu vào Database)
        // Bước 4: Lưu dữ liệu vào Bộ nhớ tạm (Giả lập việc lưu vào Database)
        MockDatabase.saveUser(username, password);
        System.out.println("=> Registration successful and saved to file: " + username);
        
        // (Sau này code Network sẽ được gọi ở đây để gửi dữ liệu lên Server)

        // Bước 5: Chuyển sang màn hình Đăng nhập (Tái sử dụng luôn hàm switchToLogin cho gọn)
        this.switchToLogin(event);
        
    } // Kết thúc hàm đăng ký

    // ==============================================================
    // 4. HÀM XỬ LÝ CHUYỂN TRANG (Dùng chung cho cả nút Đăng ký và Hyperlink)
    // ==============================================================

    @FXML
    private void switchToLogin(ActionEvent event) {
        try {
            // Chỉ định đường dẫn tới file thiết kế giao diện Đăng nhập
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/Login.fxml"));
            Parent loginRoot = loader.load();
            
            // Lấy ra cửa sổ (Stage) hiện tại đang hiển thị
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            // Tạo cảnh mới và thay thế cảnh cũ trên cửa sổ
            stage.setScene(new Scene(loginRoot, 450, 300));
            stage.setTitle("Hệ thống Đấu giá Trực tuyến - Đăng nhập");
            
        } catch (IOException e) {
            e.printStackTrace(); 
            System.out.println("Lỗi: Không thể tải màn hình Đăng nhập!");
        }
    }

} // Kết thúc lớp RegisterController