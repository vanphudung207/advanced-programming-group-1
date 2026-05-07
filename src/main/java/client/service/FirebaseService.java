package client.service;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.google.firebase.database.*;
import client.model.Product;

public class FirebaseService {

    private static final String BASE_URL =
        "https://advanced-programming-group-1-default-rtdb.asia-southeast1.firebasedatabase.app/";

    public static String registeredUsername = null;

    private static DatabaseReference getDB() {
        return FirebaseDatabase.getInstance(BASE_URL).getReference();
    }

    // ==============================================================
    // [GIỮ NGUYÊN] REST API cũ của đồng đội
    // ==============================================================
    public static String getAuction(String id) throws Exception {
        URL url = new URL(BASE_URL + "auctions/" + id + ".json");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line; StringBuilder result = new StringBuilder();
        while ((line = in.readLine()) != null) result.append(line);
        in.close();
        return result.toString();
    }

    // ==============================================================
    // 1. ĐĂNG KÝ
    // ==============================================================
    public static boolean registerUser(String username, String password, String phone) {
        try {
            DatabaseReference ref = getDB().child("users").child(username);
            CountDownLatch latch = new CountDownLatch(1);
            boolean[] success = new boolean[1];
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) { latch.countDown(); return; }
                    ref.child("password").setValue(password, (error, r) -> {
                        if (error == null) { ref.child("phone").setValueAsync(phone); success[0] = true; }
                        latch.countDown();
                    });
                }
                @Override public void onCancelled(DatabaseError e) { latch.countDown(); }
            });
            latch.await(); return success[0];
        } catch (Exception e) { return false; }
    }

    // ==============================================================
    // 2. ĐĂNG NHẬP
    // ==============================================================
    public static boolean checkLogin(String username, String password) {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            boolean[] ok = new boolean[1];
            getDB().child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(DataSnapshot s) {
                    if (s.exists()) {
                        String dbPass = s.child("password").getValue(String.class);
                        if (password.equals(dbPass)) ok[0] = true;
                    }
                    latch.countDown();
                }
                @Override public void onCancelled(DatabaseError e) { latch.countDown(); }
            });
            latch.await(); return ok[0];
        } catch (Exception e) { return false; }
    }

    // ==============================================================
    // 3. LẤY SỐ ĐIỆN THOẠI
    // ==============================================================
    public static String getUserPhone(String username) {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            String[] phone = {"Chưa cập nhật"};
            getDB().child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(DataSnapshot s) {
                    if (s.exists() && s.hasChild("phone"))
                        phone[0] = s.child("phone").getValue(String.class);
                    latch.countDown();
                }
                @Override public void onCancelled(DatabaseError e) { latch.countDown(); }
            });
            latch.await(); return phone[0];
        } catch (Exception e) { return "Chưa cập nhật"; }
    }

    // ==============================================================
    // 4. ĐĂNG SẢN PHẨM — lưu endTime thay vì secondsRemaining
    // ==============================================================
    public static String addProduct(Product newProduct) {
        try {
            DatabaseReference productRef = getDB().child("products").push();
            String key = productRef.getKey();
            CountDownLatch latch = new CountDownLatch(1);

            // Tính endTime = bây giờ + 60 giây (hoặc tuỳ chỉnh qua Product.getEndTime())
            long endTime = newProduct.getEndTime() > 0
                ? newProduct.getEndTime()
                : System.currentTimeMillis() + 60_000L;

            productRef.child("id").setValueAsync(key);
            productRef.child("name").setValueAsync(newProduct.getName());
            productRef.child("currentBid").setValueAsync(newProduct.getCurrentBid());
            productRef.child("timeRemaining").setValueAsync(newProduct.getTimeRemaining());
            productRef.child("imagePath").setValueAsync(newProduct.getImagePath());
            productRef.child("sellerUsername").setValueAsync(newProduct.getSellerUsername());
            productRef.child("category").setValueAsync(newProduct.getCategory());
            productRef.child("stepPrice").setValueAsync(newProduct.getStepPrice());
            productRef.child("status").setValueAsync("active");
            // FIX: Lưu endTime tuyệt đối thay vì secondsRemaining
            productRef.child("endTime").setValueAsync(endTime);
            productRef.child("highestBidder").setValueAsync(null);
            productRef.child("highestBidderPhone").setValueAsync(null);
            productRef.child("highestBidderEmail").setValueAsync(null);

            productRef.child("name").setValue(newProduct.getName(), (error, r) -> {
                System.out.println(error == null ? "✅ Đăng SP thành công: " + key : "❌ Lỗi: " + error.getMessage());
                latch.countDown();
            });
            latch.await(); return key;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    // ==============================================================
    // 5. TẢI DANH SÁCH SẢN PHẨM — đọc endTime thay vì secondsRemaining
    // ==============================================================
    public static List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        try {
            CountDownLatch latch = new CountDownLatch(1);
            getDB().child("products").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(DataSnapshot snapshot) {
                    for (DataSnapshot doc : snapshot.getChildren()) {
                        String id = doc.child("id").getValue(String.class);
                        if (id == null) id = doc.getKey();
                        String firebaseKey    = doc.getKey();
                        String name           = doc.child("name").getValue(String.class);
                        Double currentBid     = doc.child("currentBid").getValue(Double.class);
                        String timeRemaining  = doc.child("timeRemaining").getValue(String.class);
                        String imagePath      = doc.child("imagePath").getValue(String.class);
                        String sellerUsername = doc.child("sellerUsername").getValue(String.class);
                        String category       = doc.child("category").getValue(String.class);
                        Double stepPrice      = doc.child("stepPrice").getValue(Double.class);
                        String status         = doc.child("status").getValue(String.class);
                        String highestBidder  = doc.child("highestBidder").getValue(String.class);
                        String bidderPhone    = doc.child("highestBidderPhone").getValue(String.class);
                        String bidderEmail    = doc.child("highestBidderEmail").getValue(String.class);

                        // FIX: Đọc endTime tuyệt đối
                        Long endTime = doc.child("endTime").getValue(Long.class);
                        // Tương thích ngược: nếu chưa có endTime, tính từ secondsRemaining cũ
                        if (endTime == null || endTime == 0) {
                            Long oldSecs = doc.child("secondsRemaining").getValue(Long.class);
                            endTime = (oldSecs != null && oldSecs > 0)
                                ? System.currentTimeMillis() + oldSecs * 1000
                                : 0L;
                        }

                        Product p = new Product(
                            id, name,
                            currentBid != null ? currentBid : 0.0,
                            timeRemaining, imagePath, sellerUsername,
                            category,
                            stepPrice != null ? stepPrice : 0.0,
                            status != null ? status : "active",
                            endTime   // constructor mới nhận endTime tuyệt đối
                        );
                        p.setFirebaseKey(firebaseKey);
                        p.setHighestBidder(highestBidder);
                        p.setHighestBidderPhone(bidderPhone);
                        p.setHighestBidderEmail(bidderEmail);
                        products.add(0, p);
                    }
                    latch.countDown();
                }
                @Override public void onCancelled(DatabaseError e) { latch.countDown(); }
            });
            latch.await();
        } catch (Exception e) { e.printStackTrace(); }
        return products;
    }

    // ==============================================================
    // 6. LỌC / TÌM KIẾM
    // ==============================================================
    public static List<Product> getProductsByCategory(String cat) {
        if ("Tất cả sản phẩm".equals(cat)) return getAllProducts();
        List<Product> r = new ArrayList<>();
        for (Product p : getAllProducts())
            if (p.getCategory() != null && p.getCategory().equals(cat)) r.add(p);
        return r;
    }

    public static List<Product> searchProducts(String keyword) {
        List<Product> r = new ArrayList<>();
        String kw = keyword.toLowerCase();
        for (Product p : getAllProducts())
            if (p.getName() != null && p.getName().toLowerCase().contains(kw)) r.add(p);
        return r;
    }

    // ==============================================================
    // 7. CẬP NHẬT GIÁ
    // ==============================================================
    public static void updateBid(String key, double bid, String bidder, String phone, String email) {
        if (key == null) return;
        DatabaseReference ref = getDB().child("products").child(key);
        ref.child("currentBid").setValueAsync(bid);
        ref.child("highestBidder").setValueAsync(bidder);
        if (phone != null) ref.child("highestBidderPhone").setValueAsync(phone);
        if (email != null) ref.child("highestBidderEmail").setValueAsync(email);
    }

    // ==============================================================
    // 8. ĐÁNH DẤU KẾT THÚC
    // ==============================================================
    public static void markAuctionEnded(String key) {
        if (key == null) return;
        DatabaseReference ref = getDB().child("products").child(key);
        ref.child("status").setValueAsync("ended");
        ref.child("endTime").setValueAsync(0L);
    }

    // ==============================================================
    // 9. FIX: ANTI-SNIPING — cộng thêm giây vào endTime tuyệt đối trên Firebase
    // ==============================================================
    public static void extendEndTime(String key, int extraSeconds) {
        if (key == null) return;
        // Đọc endTime hiện tại rồi cộng thêm
        getDB().child("products").child(key).child("endTime")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(DataSnapshot snapshot) {
                    Long currentEndTime = snapshot.getValue(Long.class);
                    long now = System.currentTimeMillis();
                    // Nếu endTime đã qua thì lấy "now" làm gốc cộng thêm
                    long base = (currentEndTime != null && currentEndTime > now)
                                ? currentEndTime : now;
                    Long newEndTime = base + (long) extraSeconds * 1000;
                    snapshot.getRef().setValueAsync(newEndTime);
                }
                @Override public void onCancelled(DatabaseError e) {}
            });
    }

    // ==============================================================
    // 10. LẤY THAM CHIẾU FIREBASE CHO REALTIME LISTENER
    // ==============================================================
    public static DatabaseReference getProductRef(String key) {
        if (key == null) return null;
        return getDB().child("products").child(key);
    }
}
