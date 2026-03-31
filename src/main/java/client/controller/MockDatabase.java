// Khai báo package
package client.controller;

// Import các thư viện hỗ trợ thao tác với File (Đọc/Ghi dữ liệu)
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import client.model.Product;

public class MockDatabase {

    public static String registeredUsername = null;
    
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
    // ==============================================================
    // HÀM LẤY DANH SÁCH SẢN PHẨM (Dùng cho màn hình ProductList)
    // TODO: TEAM DATABASE - Hãy xóa dữ liệu giả này đi và thay bằng lệnh SELECT từ MySQL
    // ==============================================================
    public static List<Product> getAllProducts() {
        // Tạo một danh sách rỗng để chứa các sản phẩm
        List<Product> products = new ArrayList<>();

        // Tạo thủ công 4 sản phẩm giả lập giống hệt như ảnh thiết kế của bạn
        // (ID, Tên, Giá, Thời gian, Đường dẫn ảnh - Tạm thời dùng icon thay cho ảnh thật)
        products.add(new Product("SP01", "iPhone 15 Pro Max", 950.0, "1h 15m", "https://onewaymobile.vn/images/products/2024/12/02/original/11_1733114620.png"));
        products.add(new Product("SP02", "MacBook Air M3", 1200.0, "45m", "https://via.placeholder.com/200x150/000000/FFFFFF?text=MacBook+Air"));
        products.add(new Product("SP03", "PlayStation 5 Slim", 480.0, "3h 20m", "https://via.placeholder.com/200x150/000000/FFFFFF?text=PS5+Slim"));
        products.add(new Product("SP04", "Apple Watch Series 9", 320.0, "55m", "https://via.placeholder.com/200x150/000000/FFFFFF?text=Apple+Watch"));
        // Trả danh sách này về cho bên Giao diện (Controller) xử lý
        return products;
    }
}