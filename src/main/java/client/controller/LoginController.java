
package client.controller;

// Import annotation FXML để liên kết biến trong Java với các thành phần thiết kế bên file .fxml
import javafx.fxml.FXML; 
// Import class để tạo ô nhập mật khẩu (tự động ẩn ký tự thành dấu sao)
import javafx.scene.control.PasswordField; 
// Import class để tạo ô nhập văn bản bình thường (dùng cho tên đăng nhập)
import javafx.scene.control.TextField; 
// Import class để tạo nút bấm trên màn hình
import javafx.scene.control.Button; 
// Import class để xử lý các sự kiện tương tác của người dùng (như click chuột, ấn Enter)
import javafx.event.ActionEvent; 

// Bắt đầu khai báo lớp LoginController để xử lý toàn bộ logic của màn hình đăng nhập
public class LoginController {

    // Dùng @FXML báo cho chương trình biết biến này sẽ gắn với ô TextBox có ID tương ứng bên giao diện
    @FXML 
    // Khai báo biến đại diện cho ô nhập tên tài khoản
    private TextField txtUsername; 

    // Liên kết biến với ô nhập mật khẩu bên giao diện FXML
    @FXML 
    // Khai báo biến đại diện cho ô nhập mật khẩu
    private PasswordField txtPassword; 

    // Liên kết biến với nút bấm bên giao diện FXML
    @FXML 
    // Khai báo biến đại diện cho nút "Đăng nhập"
    private Button btnLogin; 

    // Hàm này sẽ được tự động kích hoạt khi người dùng click vào nút Đăng nhập trên màn hình
    @FXML 
    private void handleLoginAction(ActionEvent event) {
        
        // Lấy dữ liệu chữ mà người dùng vừa gõ vào ô tài khoản và lưu vào biến username dạng String
        String enteredUsername = txtUsername.getText(); 
        String enteredPassword = txtPassword.getText();

        // ĐỐI CHIẾU DỮ LIỆU VỚI BỘ NHỚ TẠM
        // Kiểm tra xem tên đăng nhập và mật khẩu có khớp y chang lúc đăng ký không
        if (enteredUsername.equals(MockDatabase.registeredUsername) && 
            enteredPassword.equals(MockDatabase.registeredPassword)) {
                System.out.println("Đăng nhập THÀNH CÔNG! Chào mừng: " + enteredUsername);
            }
        else {
            // Nếu sai tài khoản hoặc mật khẩu
            System.out.println("Đăng nhập THẤT BẠI! Sai tài khoản hoặc mật khẩu.");
        }


        // (Phần này sau sẽ gọi đến class Network để gửi username/password lên Server xác thực)
        
        // Tạm thời in ra màn hình console để test xem hàm có chạy và lấy đúng dữ liệu không 
        
    } // Kết thúc hàm xử lý sự kiện bấm nút đăng nhập

} // Kết thúc lớp LoginController