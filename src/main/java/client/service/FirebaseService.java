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
    // [GIỮ NGUYÊN] REST API cũ
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
    // 4. ĐĂNG SẢN PHẨM
    // ==============================================================
    public static String addProduct(Product newProduct) {
        try {
            DatabaseReference productRef = getDB().child("products").push();
            String key = productRef.getKey();
            CountDownLatch latch = new CountDownLatch(1);

            long endTime = newProduct.getEndTime() > 0
                ? newProduct.getEndTime()
                : System.currentTimeMillis() + 60_000L;

            productRef.child("id").setValueAsync(key);
            productRef.child("name").setValueAsync(newProduct.getName());
            productRef.child("description").setValueAsync(
                newProduct.getDescription() != null ? newProduct.getDescription() : "");
            productRef.child("currentBid").setValueAsync(newProduct.getCurrentBid());
            productRef.child("timeRemaining").setValueAsync(newProduct.getTimeRemaining());
            productRef.child("imagePath").setValueAsync(newProduct.getImagePath());
            productRef.child("sellerUsername").setValueAsync(newProduct.getSellerUsername());
            productRef.child("category").setValueAsync(newProduct.getCategory());
            productRef.child("stepPrice").setValueAsync(newProduct.getStepPrice());
            productRef.child("status").setValueAsync("active");
            productRef.child("endTime").setValueAsync(endTime);
            productRef.child("highestBidder").setValueAsync(null);
            productRef.child("highestBidderPhone").setValueAsync(null);
            productRef.child("highestBidderEmail").setValueAsync(null);

            productRef.child("name").setValue(newProduct.getName(), (error, r) -> {
                System.out.println(error == null
                    ? "✅ Đăng SP thành công: " + key
                    : "❌ Lỗi: " + error.getMessage());
                latch.countDown();
            });
            latch.await(); return key;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    // ==============================================================
    // 5. TẢI DANH SÁCH SẢN PHẨM
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
                        String description    = doc.child("description").getValue(String.class);
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

                        Long endTime = doc.child("endTime").getValue(Long.class);
                        if (endTime == null || endTime == 0) {
                            Long oldSecs = doc.child("secondsRemaining").getValue(Long.class);
                            endTime = (oldSecs != null && oldSecs > 0)
                                ? System.currentTimeMillis() + oldSecs * 1000
                                : 0L;
                        }

                        Product p = new Product(
                            id, name, description,
                            currentBid != null ? currentBid : 0.0,
                            timeRemaining, imagePath, sellerUsername,
                            category,
                            stepPrice != null ? stepPrice : 0.0,
                            status != null ? status : "active",
                            endTime
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
    public static void updateBid(String key, double bid, String bidder,
                                  String phone, String email) {
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
    // 9. ANTI-SNIPING
    // ==============================================================
    public static void extendEndTime(String key, int extraSeconds) {
        if (key == null) return;
        getDB().child("products").child(key).child("endTime")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(DataSnapshot snapshot) {
                    Long currentEndTime = snapshot.getValue(Long.class);
                    long now = System.currentTimeMillis();
                    long base = (currentEndTime != null && currentEndTime > now)
                                ? currentEndTime : now;
                    snapshot.getRef().setValueAsync(base + (long) extraSeconds * 1000);
                }
                @Override public void onCancelled(DatabaseError e) {}
            });
    }

    // ==============================================================
    // 10. LẤY THAM CHIẾU FIREBASE
    // ==============================================================
    public static DatabaseReference getProductRef(String key) {
        if (key == null) return null;
        return getDB().child("products").child(key);
    }

    // ==============================================================
    // 11. LƯU PHONE KHI ĐĂNG KÝ
    // ==============================================================
    public static void saveUserPhone(String email, String phone) {
        if (email == null || phone == null) return;
        String key = email.replace(".", ",");
        getDB().child("users").child(key).child("phone").setValueAsync(phone);
        getDB().child("users").child(key).child("email").setValueAsync(email);
    }

    // ==============================================================
    // 12. LƯU 1 DÒNG LỊCH SỬ ĐẤU GIÁ LÊN FIREBASE
    // Cấu trúc: products/{key}/bidHistory/{autoId}
    //   - bidder   : tên người trả
    //   - amount   : số tiền
    //   - timestamp: Unix ms lúc trả giá
    // ==============================================================
    public static void saveBidHistory(String productKey, String bidder, double amount) {
        if (productKey == null || bidder == null) return;
        DatabaseReference histRef = getDB()
            .child("products").child(productKey)
            .child("bidHistory").push();

        long now = System.currentTimeMillis();
        histRef.child("bidder").setValueAsync(bidder);
        histRef.child("amount").setValueAsync(amount);
        histRef.child("timestamp").setValueAsync(now);
    }

    // ==============================================================
    // 13. TẢI TOÀN BỘ LỊCH SỬ ĐẤU GIÁ CỦA 1 SẢN PHẨM
    // Trả về List<String> đã format sẵn để đổ vào ListView
    // Thứ tự: mới nhất lên đầu
    // ==============================================================
    public static List<String> loadBidHistory(String productKey) {
        List<String> result = new ArrayList<>();
        if (productKey == null) return result;
        try {
            CountDownLatch latch = new CountDownLatch(1);
            getDB().child("products").child(productKey).child("bidHistory")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot entry : snapshot.getChildren()) {
                            String bidder    = entry.child("bidder").getValue(String.class);
                            Double amount    = entry.child("amount").getValue(Double.class);
                            Long   timestamp = entry.child("timestamp").getValue(Long.class);
                            if (bidder == null || amount == null) continue;

                            // Format timestamp → "HH:mm:ss dd/MM"
                            String timeStr = formatTimestamp(timestamp);
                            String line = "🔔 [" + timeStr + "] " + bidder
                                + " trả: " + formatVND(amount);
                            // Thêm vào đầu để mới nhất lên trước
                            result.add(0, line);
                        }
                        latch.countDown();
                    }
                    @Override public void onCancelled(DatabaseError e) { latch.countDown(); }
                });
            latch.await();
        } catch (Exception e) { e.printStackTrace(); }
        return result;
    }

    // ==============================================================
    // HELPER: format Unix timestamp → "HH:mm:ss dd/MM/yyyy"
    // ==============================================================
    private static String formatTimestamp(Long ts) {
        if (ts == null) return "??:??:??";
        java.util.Date date = new java.util.Date(ts);
        java.text.SimpleDateFormat sdf =
            new java.text.SimpleDateFormat("HH:mm:ss dd/MM/yyyy",
                java.util.Locale.getDefault());
        return sdf.format(date);
    }

    // ==============================================================
    // HELPER: format VND (dùng nội bộ trong FirebaseService)
    // ==============================================================
    private static String formatVND(double amount) {
        return java.text.NumberFormat
            .getCurrencyInstance(new java.util.Locale("vi", "VN"))
            .format(amount);
    }
}
