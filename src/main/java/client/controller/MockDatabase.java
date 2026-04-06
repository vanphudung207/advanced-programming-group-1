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
    // HÀM KIỂM TRA TÀI KHOẢN ĐÃ TỒN TẠI CHƯA
    // Công dụng: Quét file txt xem cái tên người dùng định đăng ký đã có ai xài chưa
    // ==============================================================
    public static boolean checkUserExists(String username) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return false; 

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 1 && parts[0].equals(username)) {
                    return true; 
                }
            }
        } catch (IOException e) {
            System.out.println("Lỗi khi đọc file: " + e.getMessage());
        }
        return false; 
    }

    // ==============================================================
    // HÀM ĐĂNG KÝ
    // ==============================================================
    public static boolean registerUser(String username, String password, String phone) {
        if (checkUserExists(username)) {
            return false; 
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            writer.write(username + "," + password + "," + phone);
            writer.newLine();
            return true; 
        } catch (IOException e) {
            System.out.println("Lỗi khi lưu file: " + e.getMessage());
            return false;
        }
    }

    // ==============================================================
    // HÀM KIỂM TRA ĐĂNG NHẬP 
    // ==============================================================
    public static boolean checkLogin(String username, String password) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
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
    // HÀM LẤY DANH SÁCH SẢN PHẨM (ĐÃ SỬA LỖI & THÊM DANH MỤC)
    // ==============================================================
    public static List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        
        // ĐÃ FIX: Xóa dấu phẩy thừa ở SP01 và thêm Danh mục (Tham số thứ 7) cho tất cả sản phẩm
        products.add(new Product("SP01", "iPhone 15 Pro Max", 15000000.0, "1h 15m", "https://onewaymobile.vn/images/products/2024/12/02/original/11_1733114620.png", "Van Phu Dung", "Điện tử"));
        products.add(new Product("SP02", "MacBook Air M3", 30000000.0, "45m", "https://via.placeholder.com/200x150/000000/FFFFFF?text=MacBook+Air", "Admin", "Điện tử"));
        products.add(new Product("SP03", "PlayStation 5 Slim", 10000000.0, "3h 20m", "https://via.placeholder.com/200x150/000000/FFFFFF?text=PS5+Slim", "Trung Dick", "Giải trí"));
        products.add(new Product("SP04", "Apple Watch Series 9", 4000000.0, "55m", "https://via.placeholder.com/200x150/000000/FFFFFF?text=Apple+Watch", "Seller", "Điện tử"));
        
        return products;
    }

    // ==============================================================
    // (MỚI THÊM LẠI) HÀM LỌC SẢN PHẨM THEO DANH MỤC
    // Công dụng: Dùng để phục vụ các nút bấm Lọc trên thanh Menu
    // ==============================================================
    public static List<Product> getProductsByCategory(String categoryName) {
        // Nếu người dùng bấm "Tất cả sản phẩm" thì lấy toàn bộ
        if (categoryName.equals("Tất cả sản phẩm")) {
            return getAllProducts();
        }
        
        List<Product> filteredList = new ArrayList<>();
        for (Product p : getAllProducts()) {
            // Kiểm tra xem danh mục của sản phẩm có khớp với nút vừa bấm không
            if (p.getCategory().equals(categoryName)) {
                filteredList.add(p);
            }
        }
        return filteredList;
    }

    // ==============================================================
    // HÀM LẤY SỐ ĐIỆN THOẠI CỦA NGƯỜI DÙNG
    // ==============================================================
    public static String getUserPhone(String username) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return "Chưa cập nhật"; 

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3 && parts[0].equals(username)) {
                    return parts[2]; 
                }
            }
        } catch (IOException e) {
            System.out.println("Lỗi khi đọc file: " + e.getMessage());
        }
        
        return "Chưa cập nhật"; 
    }

    // ==============================================================
    // HÀM TÌM KIẾM SẢN PHẨM THEO TỪ KHÓA
    // ==============================================================
    public static List<Product> searchProducts(String keyword) {
        // 1. Tạo một cái túi rỗng để chứa các sản phẩm tìm thấy
        List<Product> result = new ArrayList<>();
        
        // 2. Chuyển từ khóa của người dùng thành chữ thường hết (Ví dụ: "iPhOne" -> "iphone") 
        // Mục đích để tìm kiếm không bị phân biệt hoa/thường, chuẩn trải nghiệm người dùng
        String lowerKeyword = keyword.toLowerCase();

        // 3. Quét qua tất cả sản phẩm đang có trên sàn
        for (Product p : getAllProducts()) {
            // Lấy tên sản phẩm đổi sang chữ thường, rồi xem nó có chứa từ khóa không
            if (p.getName().toLowerCase().contains(lowerKeyword)) {
                // Nếu khớp, nhét ngay sản phẩm này vào túi kết quả
                result.add(p); 
            }
        }
        
        // 4. Trả túi kết quả về cho giao diện hiển thị
        return result; 
    }
}