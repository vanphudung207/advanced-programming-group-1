// Khai báo package chứa file
package client.controller;

// Lớp này đóng vai trò như một cơ sở dữ liệu giả lập tạm thời trên máy người dùng
public class MockDatabase {
    
    // Biến static (tĩnh) để lưu tên đăng nhập. 
    // Từ khóa 'static' giúp biến này tồn tại xuyên suốt quá trình chạy app và dùng chung cho mọi màn hình.
    public static String registeredUsername = "";
    
    // Biến static để lưu mật khẩu vừa đăng ký
    public static String registeredPassword = "";
    
} // Kết thúc lớp MockDatabase