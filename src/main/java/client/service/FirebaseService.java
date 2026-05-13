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

    public static class BidResult {
        public final boolean success;
        public final String message;
        public final double currentBid;
        public final String highestBidder;
        public final String highestBidderPhone;
        public final String highestBidderEmail;
        public final long endTime;
        public final boolean extended;

        public BidResult(boolean success, String message, double currentBid,
                         String highestBidder, String highestBidderPhone,
                         String highestBidderEmail, long endTime,
                         boolean extended) {
            this.success = success;
            this.message = message;
            this.currentBid = currentBid;
            this.highestBidder = highestBidder;
            this.highestBidderPhone = highestBidderPhone;
            this.highestBidderEmail = highestBidderEmail;
            this.endTime = endTime;
            this.extended = extended;
        }
    }

    public static class BidHistoryEntry {
        private final String key;
        private final String line;

        public BidHistoryEntry(String key, String line) {
            this.key = key;
            this.line = line;
        }

        public String getKey() { return key; }
        public String getLine() { return line; }
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
                        if (error == null) {
                            ref.child("phone").setValueAsync(phone);
                            success[0] = true;
                        }
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
            getDB().child("users").child(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
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
            getDB().child("users").child(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
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
                                ? System.currentTimeMillis() + oldSecs * 1000 : 0L;
                        }

                        Product p = new Product(
                            id, name, description,
                            currentBid != null ? currentBid : 0.0,
                            timeRemaining, imagePath, sellerUsername, category,
                            stepPrice != null ? stepPrice : 0.0,
                            status != null ? status : "active", endTime
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
    public static BidResult placeBid(String key, double bid, String bidder,
                                     String phone, String email) {
        if (key == null || key.isEmpty()) {
            return new BidResult(false, "Không tìm thấy sản phẩm.", 0,
                null, null, null, 0L, false);
        }
        if (bidder == null || bidder.isEmpty()) {
            return new BidResult(false, "Bạn cần đăng nhập để trả giá.", 0,
                null, null, null, 0L, false);
        }

        DatabaseReference ref = getDB().child("products").child(key);
        String historyKey = ref.child("bidHistory").push().getKey();
        CountDownLatch latch = new CountDownLatch(1);
        BidResult[] result = new BidResult[] {
            new BidResult(false, "Không thể gửi giá. Vui lòng thử lại.", 0,
                null, null, null, 0L, false)
        };
        String[] rejectReason = {result[0].message};
        boolean[] extendedInTransaction = {false};

        ref.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData data) {
                if (data.getValue() == null) {
                    rejectReason[0] = "Sản phẩm không tồn tại.";
                    return Transaction.abort();
                }

                String status = data.child("status").getValue(String.class);
                Long endTimeObj = data.child("endTime").getValue(Long.class);
                long now = System.currentTimeMillis();
                long endTime = endTimeObj != null ? endTimeObj : 0L;
                if ("ended".equals(status) || endTime <= now) {
                    rejectReason[0] = "Phiên đấu giá đã kết thúc.";
                    return Transaction.abort();
                }

                String seller = data.child("sellerUsername").getValue(String.class);
                if (seller != null && seller.equals(bidder)) {
                    rejectReason[0] = "Bạn không thể tự đấu giá hàng của mình.";
                    return Transaction.abort();
                }

                Double currentObj = data.child("currentBid").getValue(Double.class);
                Double stepObj = data.child("stepPrice").getValue(Double.class);
                double current = currentObj != null ? currentObj : 0.0;
                double step = stepObj != null ? stepObj : 0.0;
                if (step > 0 && bid < current + step) {
                    rejectReason[0] = "Giá tối thiểu là "
                        + formatVNDStatic(current + step)
                        + " (bước giá: " + formatVNDStatic(step) + ")";
                    return Transaction.abort();
                }
                if (bid <= current) {
                    rejectReason[0] = "Giá phải cao hơn " + formatVNDStatic(current);
                    return Transaction.abort();
                }

                long newEndTime = endTime;
                extendedInTransaction[0] = false;
                if (endTime - now > 0 && endTime - now <= 10_000L) {
                    newEndTime = endTime + 10_000L;
                    data.child("endTime").setValue(newEndTime);
                    extendedInTransaction[0] = true;
                }

                data.child("currentBid").setValue(bid);
                data.child("highestBidder").setValue(bidder);
                data.child("highestBidderPhone").setValue(phone);
                data.child("highestBidderEmail").setValue(email);

                MutableData history = data.child("bidHistory").child(historyKey);
                history.child("bidder").setValue(bidder);
                history.child("amount").setValue(bid);
                history.child("timestamp").setValue(ServerValue.TIMESTAMP);

                return Transaction.success(data);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot snapshot) {
                if (error != null) {
                    result[0] = new BidResult(false,
                        "Lỗi kết nối Firebase: " + error.getMessage(),
                        0, null, null, null, 0L, false);
                    latch.countDown();
                    return;
                }
                if (!committed) {
                    result[0] = new BidResult(false, rejectReason[0],
                        0, null, null, null, 0L, false);
                    latch.countDown();
                    return;
                }

                Double finalBid = snapshot.child("currentBid").getValue(Double.class);
                String finalBidder = snapshot.child("highestBidder").getValue(String.class);
                String finalPhone = snapshot.child("highestBidderPhone").getValue(String.class);
                String finalEmail = snapshot.child("highestBidderEmail").getValue(String.class);
                Long finalEndTime = snapshot.child("endTime").getValue(Long.class);
                long originalEndTime = 0L;
                result[0] = new BidResult(true, "OK",
                    finalBid != null ? finalBid : bid,
                    finalBidder, finalPhone, finalEmail,
                    finalEndTime != null ? finalEndTime : originalEndTime,
                    extendedInTransaction[0]);
                latch.countDown();
            }
        });

        try { latch.await(); } catch (Exception e) {
            Thread.currentThread().interrupt();
            return new BidResult(false, "Gửi giá bị gián đoạn.", 0,
                null, null, null, 0L, false);
        }
        return result[0];
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
                    Long cur = snapshot.getValue(Long.class);
                    long now = System.currentTimeMillis();
                    long base = (cur != null && cur > now) ? cur : now;
                    snapshot.getRef().setValueAsync(base + (long) extraSeconds * 1000);
                }
                @Override public void onCancelled(DatabaseError e) {}
            });
    }

    // ==============================================================
    // 10. THAM CHIẾU FIREBASE
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
    // 12. LƯU 1 DÒNG LỊCH SỬ ĐẤU GIÁ
    // ==============================================================
    public static void saveBidHistory(String productKey, String bidder, double amount) {
        if (productKey == null || bidder == null) return;
        DatabaseReference ref = getDB()
            .child("products").child(productKey).child("bidHistory").push();

        java.util.Map<String, Object> bidRecord = new java.util.HashMap<>();
        bidRecord.put("bidder", bidder);
        bidRecord.put("amount", amount);
        bidRecord.put("timestamp", ServerValue.TIMESTAMP);
        ref.setValueAsync(bidRecord);
    }

    // ==============================================================
    // 13. TẢI TOÀN BỘ LỊCH SỬ (ĐỒNG BỘ, BLOCKING)
    // Gọi trên background thread, trả về List đã sort mới → cũ
    // Đồng thời trả về hasBids qua mảng boolean[1]
    // ==============================================================
    public static List<BidHistoryEntry> loadBidHistorySync(String productKey, boolean[] hasBidsOut) {
        List<BidHistoryEntry> result = new ArrayList<>();
        if (productKey == null) return result;
        try {
            CountDownLatch latch = new CountDownLatch(1);
            getDB().child("products").child(productKey).child("bidHistory")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        // snapshot.getChildren() trả về thứ tự cũ → mới
                        for (DataSnapshot entry : snapshot.getChildren()) {
                            String bidder    = entry.child("bidder").getValue(String.class);
                            Double amount    = entry.child("amount").getValue(Double.class);
                            Long   timestamp = entry.child("timestamp").getValue(Long.class);
                            if (bidder == null || amount == null) continue;
                            String line = "🔔 [" + formatTimestamp(timestamp) + "] "
                                        + bidder + " trả: " + formatVNDStatic(amount);
                            result.add(0, new BidHistoryEntry(entry.getKey(), line)); // đảo → mới lên đầu
                        }
                        // Nếu có ít nhất 1 record → hasBids = true
                        if (hasBidsOut != null && snapshot.getChildrenCount() > 0)
                            hasBidsOut[0] = true;
                        latch.countDown();
                    }
                    @Override public void onCancelled(DatabaseError e) { latch.countDown(); }
                });
            latch.await();
        } catch (Exception e) { e.printStackTrace(); }
        return result;
    }

    // ==============================================================
    // 14. LẤY REFERENCE bidHistory CHO CHILD LISTENER (realtime mới)
    // ==============================================================
    public static DatabaseReference getBidHistoryRef(String productKey) {
        if (productKey == null) return null;
        return getDB().child("products").child(productKey).child("bidHistory");
    }

    // ==============================================================
    // HELPER
    // ==============================================================
    public static String formatTimestamp(Long ts) {
        long timestamp = ts != null ? ts : System.currentTimeMillis();
        java.text.SimpleDateFormat sdf =
            new java.text.SimpleDateFormat("HH:mm:ss dd/MM/yyyy",
                java.util.Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        return sdf.format(new java.util.Date(timestamp));
    }

    public static String formatVNDStatic(double amount) {
        return java.text.NumberFormat
            .getCurrencyInstance(new java.util.Locale("vi", "VN"))
            .format(amount);
    }
}
