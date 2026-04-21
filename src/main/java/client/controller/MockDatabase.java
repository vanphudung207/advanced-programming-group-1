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
    // (MỚI) KHỞI TẠO KHO HÀNG "TĨNH" ĐỂ LƯU TRỮ DỮ LIỆU XUYÊN SUỐT
    // ==============================================================
    private static List<Product> productList = new ArrayList<>();

    // Khối static này chỉ chạy ĐÚNG 1 LẦN khi vừa mở app
    static {
        productList.add(new Product("SP01", "iPhone 15 Pro Max", 15000000.0, "1h 15m", "https://onewaymobile.vn/images/products/2024/12/02/original/11_1733114620.png", "Van Phu Dung", "Điện tử"));
        productList.add(new Product("SP02", "MacBook Air M3", 30000000.0, "45m", "https://via.placeholder.com/200x150/000000/FFFFFF?text=MacBook+Air", "Admin", "Điện tử"));
        productList.add(new Product("SP03", "PlayStation 5 Slim", 10000000.0, "3h 20m", "https://via.placeholder.com/200x150/000000/FFFFFF?text=PS5+Slim", "Trung Dick", "Giải trí"));
        productList.add(new Product("SP04", "Apple Watch Series 9", 4000000.0, "55m", "https://via.placeholder.com/200x150/000000/FFFFFF?text=Apple+Watch", "Seller", "Điện tử"));
    }

    // ==============================================================
    // (MỚI) HÀM THÊM SẢN PHẨM MỚI VÀO KHO
    // ==============================================================
    public static void addProduct(Product newProduct) {
        // Thêm vào vị trí số 0 để sản phẩm mới đăng sẽ hiện lên đầu trang chủ
        productList.add(0, newProduct); 
    }

    // Hàm lấy danh sách bây giờ chỉ cần móc cái kho tĩnh ra đưa cho giao diện
    public static List<Product> getAllProducts() {
        return productList;
    }

    // Các hàm Lọc và Tìm kiếm giữ nguyên logic cũ
    public static List<Product> getProductsByCategory(String categoryName) {
        if (categoryName.equals("Tất cả sản phẩm")) return getAllProducts();
        
        List<Product> filteredList = new ArrayList<>();
        for (Product p : getAllProducts()) {
            if (p.getCategory().equals(categoryName)) filteredList.add(p);
        }
        return filteredList;
    }

    public static List<Product> searchProducts(String keyword) {
        List<Product> result = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        for (Product p : getAllProducts()) {
            if (p.getName().toLowerCase().contains(lowerKeyword)) result.add(p); 
        }
        return result; 
    }

    // ==============================================================
    // CÁC HÀM XỬ LÝ TÀI KHOẢN (GIỮ NGUYÊN NHƯ CŨ)
    // ==============================================================
    public static boolean checkUserExists(String username) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return false; 
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 1 && parts[0].equals(username)) return true; 
            }
        } catch (IOException e) { e.printStackTrace(); }
        return false; 
    }

    public static boolean registerUser(String username, String password, String phone) {
        if (checkUserExists(username)) return false; 
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            writer.write(username + "," + password + "," + phone);
            writer.newLine();
            return true; 
        } catch (IOException e) { return false; }
    }

    public static boolean checkLogin(String username, String password) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return false;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2 && parts[0].equals(username) && parts[1].equals(password)) return true; 
            }
        } catch (IOException e) { e.printStackTrace(); }
        return false; 
    }

    public static String getUserPhone(String username) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return "Chưa cập nhật"; 
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3 && parts[0].equals(username)) return parts[2]; 
            }
        } catch (IOException e) { e.printStackTrace(); }
        return "Chưa cập nhật"; 
    }
}