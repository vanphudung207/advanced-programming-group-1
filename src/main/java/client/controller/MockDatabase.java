package client.controller;

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
    private static final String FILE_PATH = "mock_users.txt";

    // ==============================================================
    // (MỚI THÊM) HÀM KIỂM TRA TÀI KHOẢN ĐÃ TỒN TẠI CHƯA
    // Công dụng: Quét file txt xem cái tên người dùng định đăng ký đã có ai xài chưa
    // ==============================================================
    public static boolean checkUserExists(String username) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return false; // Nếu file chưa có thì chắc chắn tên này chưa ai xài

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                // Chỉ cần kiểm tra phần tử đầu tiên (parts[0]) vì đó là Username
                if (parts.length >= 1 && parts[0].equals(username)) {
                    return true; // Báo động: Đã có người xài tên này!
                }
            }
        } catch (IOException e) {
            System.out.println("Lỗi khi đọc file: " + e.getMessage());
        }
        return false; // Tên hợp lệ, chưa ai xài
    }

    // ==============================================================
    // HÀM ĐĂNG KÝ (Đã nâng cấp: Đổi tên thành registerUser và thêm Số điện thoại)
    // ==============================================================
    public static boolean registerUser(String username, String password, String phone) {
        
        // 1. Kiểm tra trùng lặp trước khi cho phép lưu
        if (checkUserExists(username)) {
            return false; // Trả về false để Controller biết mà báo lỗi cho người dùng
        }

        // 2. Ghi nối tiếp vào file (true)
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            
            // Ghi 3 thông tin cách nhau bằng dấu phẩy: username,password,phone
            writer.write(username + "," + password + "," + phone);
            writer.newLine();
            
            return true; // Ghi thành công
            
        } catch (IOException e) {
            System.out.println("Lỗi khi lưu file: " + e.getMessage());
            return false;
        }
    }

    // ==============================================================
    // HÀM KIỂM TRA ĐĂNG NHẬP (Đã tinh chỉnh để đọc chuẩn file có 3 cột)
    // ==============================================================
    public static boolean checkLogin(String username, String password) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                
                String[] parts = line.split(",");
                
                // Đã sửa thành ">= 2" thay vì "== 2"
                // Lý do: Các dòng bây giờ có 3 phần (user,pass,phone). 
                // Thậm chí nếu có tài khoản cũ chỉ có 2 phần (user,pass) thì nó vẫn đọc được không bị lỗi.
                if (parts.length >= 2) {
                    if (parts[0].equals(username) && parts[1].equals(password)) {
                        return true; 
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Lỗi khi đọc file: " + e.getMessage());
        }
        return false; 
    }

    // ==============================================================
    // HÀM LẤY DANH SÁCH SẢN PHẨM (Giữ nguyên không thay đổi)
    // ==============================================================
    public static List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        products.add(new Product("SP01", "iPhone 15 Pro Max", 15000000.0, "1h 15m", "https://onewaymobile.vn/images/products/2024/12/02/original/11_1733114620.png", "Van Phu Dung"));
        products.add(new Product("SP02", "MacBook Air M3", 30000000.0, "45m", "https://via.placeholder.com/200x150/000000/FFFFFF?text=MacBook+Air", "Admin"));
        products.add(new Product("SP03", "PlayStation 5 Slim", 10000000.0, "3h 20m", "https://via.placeholder.com/200x150/000000/FFFFFF?text=PS5+Slim", "Trung Dick"));
        products.add(new Product("SP04", "Apple Watch Series 9", 4000000.0, "55m", "https://via.placeholder.com/200x150/000000/FFFFFF?text=Apple+Watch", "Seller"));
        return products;
    }

    // ==============================================================
    // (MỚI THÊM) HÀM LẤY SỐ ĐIỆN THOẠI CỦA NGƯỜI DÙNG
    // Công dụng: Tìm trong file txt xem tài khoản này có số điện thoại là gì
    // ==============================================================
    public static String getUserPhone(String username) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return "Chưa cập nhật"; // Đề phòng file mất

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                // Cấu trúc file hiện tại là: parts[0]=Tên, parts[1]=Pass, parts[2]=SĐT
                // Kiểm tra xem dòng này có đủ 3 phần và có khớp tên không
                if (parts.length >= 3 && parts[0].equals(username)) {
                    return parts[2]; // Trả về số điện thoại
                }
            }
        } catch (IOException e) {
            System.out.println("Lỗi khi đọc file: " + e.getMessage());
        }
        
        // Nếu đọc hết file mà không thấy, hoặc tài khoản cũ hồi xưa chưa có SĐT
        return "Chưa cập nhật"; 
    }
}