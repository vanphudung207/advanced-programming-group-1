// Khai báo package chứa file controller của Client
package client.controller;

// Import các thư viện UI cần thiết từ JavaFX
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import java.io.IOException;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;

// Bắt đầu khai báo lớp RegisterController để xử lý sự kiện cho màn hình Đăng ký
public class RegisterController {

    // Liên kết với ô nhập Họ và Tên trên giao diện
    @FXML
    private TextField txtFullName;

    // Liên kết với ô nhập Tên đăng nhập
    @FXML
    private TextField txtUsername;

    // Liên kết với ô nhập Mật khẩu
    @FXML
    private PasswordField txtPassword;

    // Liên kết với ô nhập Xác nhận lại Mật khẩu
    @FXML
    private PasswordField txtConfirmPassword;

    // Liên kết với hộp thoại xổ xuống để chọn vai trò (Bidder/Seller)
    // ComboBox<String> nghĩa là danh sách các lựa chọn bên trong sẽ là kiểu chuỗi chữ (String)
    @FXML
    private ComboBox<String> cbRole;

    // Liên kết với nút Đăng ký
    @FXML
    private Button btnRegister;

    // Hàm initialize() là một hàm đặc biệt của JavaFX, nó sẽ tự động chạy MỘT LẦN DUY NHẤT 
    // ngay sau khi giao diện FXML được load lên, dùng để thiết lập các dữ liệu ban đầu.
    @FXML
    public void initialize() {
        // Thêm 2 lựa chọn vai trò vào hộp thoại xổ xuống để người dùng chọn
        cbRole.getItems().addAll("Người mua (Bidder)", "Người bán (Seller)");
        
        // Cài đặt giá trị mặc định được chọn sẵn là "Người mua" cho tiện
        cbRole.setValue("Người mua (Bidder)");
    }

    // Hàm xử lý logic khi người dùng click vào nút "Đăng ký"
    @FXML
    private void handleRegisterAction(ActionEvent event) {
        
        // Lấy toàn bộ dữ liệu người dùng vừa nhập gán vào các biến chuỗi tương ứng
        String fullName = txtFullName.getText();
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();
        String role = cbRole.getValue(); // Lấy giá trị vai trò đang được chọn

        // Kiểm tra cơ bản: Nếu mật khẩu nhập lại không khớp thì in ra cảnh báo (Sau này sẽ làm popup lỗi thật)
        if (!password.equals(confirmPassword)) {
            System.out.println("Lỗi: Mật khẩu xác nhận không khớp!");
            return; // Dừng hàm lại ngay tại đây, không cho đăng ký tiếp
        }
        // 2. LƯU DỮ LIỆU VÀO BỘ NHỚ TẠM (Giả lập việc lưu vào Database)
        MockDatabase.registeredUsername = username;
        MockDatabase.registeredPassword = password;
        System.out.println("Đăng ký thành công tài khoản: " + username);

        // 3. CODE CHUYỂN TRANG SANG MÀN HÌNH ĐĂNG NHẬP
        try {
            // Đọc file thiết kế giao diện Đăng nhập (Login.fxml)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/Login.fxml"));
            Parent loginRoot = loader.load();
            
            // Lấy ra cửa sổ (Stage) hiện tại đang chứa nút Đăng ký vừa bị click
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            // Đặt giao diện Đăng nhập vào một Scene mới và thay thế Scene cũ trên cửa sổ
            stage.setScene(new Scene(loginRoot, 450, 300));
            stage.setTitle("Hệ thống Đấu giá Trực tuyến - Đăng nhập");
            
        } catch (IOException e) {
            // Bắt lỗi nếu không tìm thấy file Login.fxml
            e.printStackTrace(); 
            System.out.println("Lỗi không tải được màn hình Đăng nhập!");
        }

        // Tạm thời in ra console để test xem dữ liệu đã được hứng đúng chưa
        System.out.println("Đang xử lý đăng ký tài khoản mới...");
        System.out.println("Họ tên: " + fullName + " | Tài khoản: " + username + " | Vai trò: " + role);
        
        // (Sau này code Network sẽ được gọi ở đây để gửi gói dữ liệu này lên Server lưu vào Database)
        
    } // Kết thúc hàm xử lý đăng ký

} // Kết thúc lớp RegisterController