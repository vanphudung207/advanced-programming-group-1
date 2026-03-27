// Khai báo package
package client.controller;

// Import các thư viện hỗ trợ thao tác với File (Đọc/Ghi dữ liệu)
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MockDatabase {
    
    // Khai báo đường dẫn và tên file sẽ lưu trên ổ cứng. 
    // File này sẽ tự động được tạo ra ngay trong thư mục dự án OnlineAuctionSystem của bạn.
    private static final String FILE_PATH = "mock_users.txt";

    // ==============================================================
    // HÀM LƯU NGƯỜI DÙNG VÀO FILE (Dùng lúc Đăng ký)
    // ==============================================================
    public static void saveUser(String username, String password) {
        // Sử dụng cú pháp try-with-resources để tự động đóng file sau khi ghi xong
        // Tham số 'true' trong FileWriter có nghĩa là: Ghi tiếp nối (append) vào cuối file chứ không xóa dữ liệu cũ.
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            
            // Ghi tên đăng nhập và mật khẩu vào file, cách nhau bởi dấu phẩy
            writer.write(username + "," + password);
            
            // Xuống dòng để chuẩn bị cho tài khoản đăng ký tiếp theo
            writer.newLine();
            
        } catch (IOException e) {
            System.out.println("Lỗi khi lưu file: " + e.getMessage());
        }
    }

    // ==============================================================
    // HÀM KIỂM TRA ĐĂNG NHẬP TỪ FILE (Dùng lúc Đăng nhập)
    // ==============================================================
    public static boolean checkLogin(String username, String password) {
        File file = new File(FILE_PATH);
        
        // Nếu file chưa tồn tại (chưa có ai đăng ký bao giờ) thì trả về false (đăng nhập thất bại) ngay lập tức
        if (!file.exists()) return false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            
            // Đọc từng dòng trong file cho đến khi hết (null)
            while ((line = reader.readLine()) != null) {
                
                // Tách dòng chữ thành mảng 2 phần tử: [tên đăng nhập, mật khẩu] dựa vào dấu phẩy
                String[] parts = line.split(",");
                
                // Kiểm tra xem mảng có đủ 2 phần và có khớp với dữ liệu người dùng vừa gõ không
                if (parts.length == 2) {
                    if (parts[0].equals(username) && parts[1].equals(password)) {
                        return true; // Khớp 100%, trả về true báo hiệu Đăng nhập thành công
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Lỗi khi đọc file: " + e.getMessage());
        }
        
        // Nếu đọc hết file mà không thấy dòng nào khớp, trả về false (đăng nhập thất bại)
        return false; 
    }
}