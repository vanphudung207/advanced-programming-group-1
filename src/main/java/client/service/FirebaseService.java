package client.service;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.google.firebase.database.*;
import client.model.Product;

public class FirebaseService {

    // URL gốc của Database (Của đồng đội)
    private static final String BASE_URL= "https://advanced-programming-group-1-default-rtdb.asia-southeast1.firebasedatabase.app/";
    
    // Biến lưu trữ người dùng đang đăng nhập
    public static String registeredUsername = null;

    // ==============================================================
    // [GIỮ NGUYÊN] HÀM CŨ CỦA ĐỒNG ĐỘI
    // ==============================================================
    public static String getAuction(String id) throws Exception{
        URL url = new URL(BASE_URL +"auctions/" + id+ ".json");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        StringBuilder result = new StringBuilder();
        while((line=in.readLine()) !=null){
            result.append(line);
        }
        in.close(); 
        return result.toString();
    }

    // ==============================================================
    // LẤY KẾT NỐI ĐẾN FIREBASE ADMIN SDK
    // ==============================================================
    private static DatabaseReference getDB() {
        return FirebaseDatabase.getInstance(BASE_URL).getReference();
    }

    // ==============================================================
    // 1. LƯU TÀI KHOẢN ĐĂNG KÝ
    // ==============================================================
    public static boolean registerUser(String username, String password, String phone) {
        try {
            DatabaseReference ref = getDB().child("users").child(username);
            CountDownLatch latch = new CountDownLatch(1);
            boolean[] success = new boolean[1];

            // Đọc xem User có tồn tại chưa
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        success[0] = false; // Bị trùng tên
                        latch.countDown();
                    } else {
                        // Nếu chưa có, tiến hành lưu mật khẩu và SĐT
                        ref.child("password").setValue(password, (error, dRef) -> {
                            if (error == null) {
                                ref.child("phone").setValueAsync(phone);
                                success[0] = true;
                            }
                            latch.countDown();
                        });
                    }
                }
                @Override
                public void onCancelled(DatabaseError error) { latch.countDown(); }
            });
            latch.await(); // Khóa chờ xử lý xong mới cho form báo thành công
            return success[0];
        } catch (Exception e) { return false; }
    }

    // ==============================================================
    // 2. KIỂM TRA ĐĂNG NHẬP
    // ==============================================================
    public static boolean checkLogin(String username, String password) {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            boolean[] isValid = new boolean[1];

            getDB().child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String dbPass = snapshot.child("password").getValue(String.class);
                        if (password.equals(dbPass)) isValid[0] = true;
                    }
                    latch.countDown();
                }
                @Override
                public void onCancelled(DatabaseError error) { latch.countDown(); }
            });
            latch.await();
            return isValid[0];
        } catch (Exception e) { return false; }
    }

    // ==============================================================
    // 3. LẤY SỐ ĐIỆN THOẠI
    // ==============================================================
    public static String getUserPhone(String username) {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            String[] phone = new String[]{"Chưa cập nhật"};

            getDB().child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.hasChild("phone")) {
                        phone[0] = snapshot.child("phone").getValue(String.class);
                    }
                    latch.countDown();
                }
                @Override
                public void onCancelled(DatabaseError error) { latch.countDown(); }
            });
            latch.await();
            return phone[0];
        } catch (Exception e) { return "Chưa cập nhật"; }
    }

    // ==============================================================
    // 4. LƯU SẢN PHẨM MỚI LÊN FIREBASE (Add Product)
    // ==============================================================
    public static void addProduct(Product newProduct) {
        try {
            DatabaseReference productRef = getDB().child("products").push(); // Tự sinh mã ID ngẫu nhiên trên Google
            CountDownLatch latch = new CountDownLatch(1);

            productRef.setValue(newProduct, (error, ref) -> {
                if (error == null) System.out.println("Đăng sản phẩm lên Firebase thành công!");
                else System.out.println("Lỗi lưu sản phẩm: " + error.getMessage());
                latch.countDown();
            });
            latch.await();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ==============================================================
    // 5. TẢI DANH SÁCH SẢN PHẨM TỪ FIREBASE VỀ TRANG CHỦ
    // ==============================================================
    public static List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        try {
            CountDownLatch latch = new CountDownLatch(1);
            getDB().child("products").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    for (DataSnapshot doc : snapshot.getChildren()) {
                        // Bóc tách dữ liệu từ Google về Model của bạn
                        String id = doc.child("id").getValue(String.class);
                        String name = doc.child("name").getValue(String.class);
                        Double currentBid = doc.child("currentBid").getValue(Double.class);
                        String timeRemaining = doc.child("timeRemaining").getValue(String.class);
                        String imagePath = doc.child("imagePath").getValue(String.class);
                        String sellerUsername = doc.child("sellerUsername").getValue(String.class);
                        String category = doc.child("category").getValue(String.class);

                        products.add(0, new Product(id, name, currentBid != null ? currentBid : 0.0, timeRemaining, imagePath, sellerUsername, category));
                    }
                    latch.countDown();
                }
                @Override
                public void onCancelled(DatabaseError error) { latch.countDown(); }
            });
            latch.await();
        } catch (Exception e) { e.printStackTrace(); }
        return products;
    }

    // ==============================================================
    // 6. CÁC HÀM LỌC TÌM KIẾM
    // ==============================================================
    public static List<Product> getProductsByCategory(String categoryName) {
        if (categoryName.equals("Tất cả sản phẩm")) return getAllProducts();
        List<Product> filteredList = new ArrayList<>();
        for (Product p : getAllProducts()) {
            if (p.getCategory() != null && p.getCategory().equals(categoryName)) filteredList.add(p);
        }
        return filteredList;
    }

    public static List<Product> searchProducts(String keyword) {
        List<Product> result = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        for (Product p : getAllProducts()) {
            if (p.getName() != null && p.getName().toLowerCase().contains(lowerKeyword)) result.add(p); 
        }
        return result; 
    }
}