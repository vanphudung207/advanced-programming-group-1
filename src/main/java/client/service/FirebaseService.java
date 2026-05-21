package client.service;

import client.model.Product;
import client.model.User;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

public class FirebaseService {

    private static final String BASE_URL =
        "https://advanced-programming-group-1-default-rtdb.asia-southeast1.firebasedatabase.app";
    private static final String STORAGE_BUCKET =
        "advanced-programming-group-1.firebasestorage.app";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public static String currentUserEmail = null;
    public static String registeredUsername = null;
    private static String lastError = null;

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

        public String getKey() {
            return key;
        }

        public String getLine() {
            return line;
        }
    }

    public static class WinnerNotification {
        private final String productKey;
        private final String productName;
        private final String winner;
        private final String winnerPhone;
        private final String winnerEmail;
        private final double finalPrice;
        private final long endedAt;
        private final boolean seen;

        public WinnerNotification(String productKey, String productName,
                                  String winner, String winnerPhone,
                                  String winnerEmail, double finalPrice,
                                  long endedAt, boolean seen) {
            this.productKey = productKey;
            this.productName = productName;
            this.winner = winner;
            this.winnerPhone = winnerPhone;
            this.winnerEmail = winnerEmail;
            this.finalPrice = finalPrice;
            this.endedAt = endedAt;
            this.seen = seen;
        }

        public String getProductKey() { return productKey; }
        public String getProductName() { return productName; }
        public String getWinner() { return winner; }
        public String getWinnerPhone() { return winnerPhone; }
        public String getWinnerEmail() { return winnerEmail; }
        public double getFinalPrice() { return finalPrice; }
        public long getEndedAt() { return endedAt; }
        public boolean isSeen() { return seen; }
    }

    public static String getAuction(String id) throws Exception {
        return request("GET", "/auctions/" + id + ".json", null);
    }

    public static String addProduct(Product newProduct) {
        lastError = null;
        try {
            String key = "product_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().replace("-", "");
            long endTime = newProduct.getEndTime() > 0
                ? newProduct.getEndTime()
                : System.currentTimeMillis() + 60_000L;

            String json = "{"
                + "\"id\":\"" + escape(key) + "\","
                + "\"name\":\"" + escape(newProduct.getName()) + "\","
                + "\"description\":\"" + escape(newProduct.getDescription()) + "\","
                + "\"currentBid\":" + newProduct.getCurrentBid() + ","
                + "\"timeRemaining\":\"" + escape(newProduct.getTimeRemaining()) + "\","
                + "\"imagePath\":\"" + escape(newProduct.getImagePath()) + "\","
                + "\"sellerUsername\":\"" + escape(newProduct.getSellerUsername()) + "\","
                + "\"category\":\"" + escape(newProduct.getCategory()) + "\","
                + "\"stepPrice\":" + newProduct.getStepPrice() + ","
                + "\"status\":\"active\","
                + "\"endTime\":" + endTime + ","
                + "\"highestBidder\":\"\","
                + "\"highestBidderPhone\":\"\","
                + "\"highestBidderEmail\":\"\""
                + "}";

            request("PUT", "/products/" + key + ".json", json);
            return key;
        } catch (Exception e) {
            lastError = e.getMessage();
            e.printStackTrace();
            return null;
        }
    }

    public static List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        try {
            String json = request("GET", "/products.json", null);
            if (json == null || json.equals("null") || json.isBlank()) {
                return products;
            }

            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                Product product = productFromJson(entry.getKey(), entry.getValue().getAsJsonObject());
                products.add(0, product);
            }
        } catch (Exception e) {
            lastError = e.getMessage();
            e.printStackTrace();
        }
        return products;
    }

    public static Product getProductByKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        try {
            String json = request("GET", "/products/" + key + ".json", null);
            if (json == null || json.equals("null") || json.isBlank()) {
                return null;
            }
            return productFromJson(key, JsonParser.parseString(json).getAsJsonObject());
        } catch (Exception e) {
            lastError = e.getMessage();
            e.printStackTrace();
            return null;
        }
    }

    public static BidResult placeBid(String key, double bid, String bidder,
                                     String phone, String email) {
        if (key == null || key.isBlank()) {
            return new BidResult(false, "Không tìm thấy sản phẩm.", 0,
                null, null, null, 0L, false);
        }
        if (bidder == null || bidder.isBlank()) {
            return new BidResult(false, "Bạn cần đăng nhập để trả giá.", 0,
                null, null, null, 0L, false);
        }

        Product product = getProductByKey(key);
        if (product == null) {
            return new BidResult(false, "Sản phẩm không tồn tại.", 0,
                null, null, null, 0L, false);
        }
        if (product.isEnded()) {
            return new BidResult(false, "Phiên đấu giá đã kết thúc.", product.getCurrentBid(),
                product.getHighestBidder(), product.getHighestBidderPhone(),
                product.getHighestBidderEmail(), product.getEndTime(), false);
        }
        if (bidder.equals(product.getSellerUsername())) {
            return new BidResult(false, "Bạn không thể tự đấu giá hàng của mình.", product.getCurrentBid(),
                product.getHighestBidder(), product.getHighestBidderPhone(),
                product.getHighestBidderEmail(), product.getEndTime(), false);
        }

        double minimum = product.getCurrentBid() + product.getStepPrice();
        if (product.getStepPrice() > 0 && bid < minimum) {
            return new BidResult(false,
                "Giá tối thiểu là " + formatVNDStatic(minimum)
                    + " (bước giá: " + formatVNDStatic(product.getStepPrice()) + ")",
                product.getCurrentBid(), product.getHighestBidder(),
                product.getHighestBidderPhone(), product.getHighestBidderEmail(),
                product.getEndTime(), false);
        }
        if (bid <= product.getCurrentBid()) {
            return new BidResult(false,
                "Giá phải cao hơn " + formatVNDStatic(product.getCurrentBid()),
                product.getCurrentBid(), product.getHighestBidder(),
                product.getHighestBidderPhone(), product.getHighestBidderEmail(),
                product.getEndTime(), false);
        }

        long now = System.currentTimeMillis();
        long endTime = product.getEndTime();
        boolean extended = endTime > now && endTime - now <= 10_000L;
        if (extended) {
            endTime += 10_000L;
        }

        if (!updateBid(key, bid, bidder, phone, email, endTime)) {
            return new BidResult(false, "Không thể cập nhật giá hiện tại. Vui lòng thử lại.",
                product.getCurrentBid(), product.getHighestBidder(),
                product.getHighestBidderPhone(), product.getHighestBidderEmail(),
                product.getEndTime(), false);
        }
        saveBidHistory(key, bidder, bid);

        return new BidResult(true, "OK", bid, bidder, phone, email, endTime, extended);
    }

    public static boolean updateBid(String firebaseKey, double bid, String bidder,
                                    String phone, String email) {
        return updateBid(firebaseKey, bid, bidder, phone, email, -1L);
    }

    private static boolean updateBid(String firebaseKey, double bid, String bidder,
                                     String phone, String email, long endTime) {
        if (firebaseKey == null || firebaseKey.isBlank()) {
            return false;
        }
        try {
            String json = "{"
                + "\"currentBid\":" + bid + ","
                + "\"highestBidder\":\"" + escape(bidder) + "\","
                + "\"highestBidderPhone\":\"" + escape(phone) + "\","
                + "\"highestBidderEmail\":\"" + escape(email) + "\"";
            if (endTime > 0) {
                json += ",\"endTime\":" + endTime;
            }
            json += "}";
            request("PATCH", "/products/" + firebaseKey + ".json", json);
            return true;
        } catch (Exception e) {
            lastError = e.getMessage();
            e.printStackTrace();
            return false;
        }
    }

    public static void markAuctionEnded(String firebaseKey) {
        if (firebaseKey == null || firebaseKey.isBlank()) {
            return;
        }
        try {
            request("PATCH", "/products/" + firebaseKey + ".json",
                "{\"status\":\"ended\"}");
        } catch (Exception e) {
            lastError = e.getMessage();
            e.printStackTrace();
        }
    }

    public static List<Product> searchProducts(String keyword) {
        List<Product> result = new ArrayList<>();
        String kw = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        for (Product product : getAllProducts()) {
            if (product.getName() != null && product.getName().toLowerCase(Locale.ROOT).contains(kw)) {
                result.add(product);
            }
        }
        return result;
    }

    public static List<Product> getProductsByCategory(String category) {
        if (category == null || category.equals("Tất cả sản phẩm")) {
            return getAllProducts();
        }
        List<Product> result = new ArrayList<>();
        for (Product product : getAllProducts()) {
            if (category.equals(product.getCategory())) {
                result.add(product);
            }
        }
        return result;
    }

    public static List<Product> getProductsBySeller(String sellerUsername) {
        List<Product> result = new ArrayList<>();
        if (sellerUsername == null || sellerUsername.isBlank()) {
            return result;
        }
        for (Product product : getAllProducts()) {
            if (sellerUsername.equals(product.getSellerUsername())) {
                result.add(product);
            }
        }
        return result;
    }

    public static List<WinnerNotification> getWinnerNotificationsForSeller(String sellerUsername) {
        List<WinnerNotification> notifications = new ArrayList<>();
        if (sellerUsername == null || sellerUsername.isBlank()) {
            return notifications;
        }

        Set<String> seenKeys = getSeenWinnerNotificationKeys(sellerUsername);
        for (Product product : getProductsBySeller(sellerUsername)) {
            if (!isWinnerNotificationProduct(product)) {
                continue;
            }

            String productKey = notificationProductKey(product);
            notifications.add(new WinnerNotification(
                productKey,
                product.getName(),
                product.getHighestBidder(),
                product.getHighestBidderPhone(),
                product.getHighestBidderEmail(),
                product.getCurrentBid(),
                product.getEndTime(),
                seenKeys.contains(productKey)
            ));
        }

        notifications.sort(Comparator.comparingLong(WinnerNotification::getEndedAt).reversed());
        return notifications;
    }

    public static void markWinnerNotificationsSeen(String sellerUsername, List<String> productKeys) {
        if (sellerUsername == null || sellerUsername.isBlank()
                || productKeys == null || productKeys.isEmpty()) {
            return;
        }

        try {
            StringBuilder json = new StringBuilder("{");
            for (String productKey : productKeys) {
                if (productKey == null || productKey.isBlank()) {
                    continue;
                }
                appendCommaIfNeeded(json);
                json.append("\"").append(escape(productKey)).append("\":true");
            }
            json.append("}");

            if (json.length() > 2) {
                request("PATCH",
                    "/users/" + userKey(sellerUsername) + "/seenWinnerNotifications.json",
                    json.toString());
            }
        } catch (Exception e) {
            lastError = e.getMessage();
            e.printStackTrace();
        }
    }

    private static Set<String> getSeenWinnerNotificationKeys(String sellerUsername) {
        Set<String> seenKeys = new HashSet<>();
        try {
            String json = request("GET",
                "/users/" + userKey(sellerUsername) + "/seenWinnerNotifications.json",
                null);
            if (json == null || json.equals("null") || json.isBlank()) {
                return seenKeys;
            }

            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                try {
                    if (entry.getValue().getAsBoolean()) {
                        seenKeys.add(entry.getKey());
                    }
                } catch (Exception ignored) {
                    seenKeys.add(entry.getKey());
                }
            }
        } catch (Exception e) {
            lastError = e.getMessage();
        }
        return seenKeys;
    }

    private static boolean isWinnerNotificationProduct(Product product) {
        return product != null
            && product.isEnded()
            && product.getHighestBidder() != null
            && !product.getHighestBidder().isBlank();
    }

    private static String notificationProductKey(Product product) {
        if (product.getFirebaseKey() != null && !product.getFirebaseKey().isBlank()) {
            return product.getFirebaseKey();
        }
        if (product.getId() != null && !product.getId().isBlank()) {
            return product.getId();
        }
        return product.getName() != null ? product.getName() : "";
    }

    public static boolean updateProductIfOwner(String key, Product updatedProduct, String currentUser) {
        if (key == null || key.isBlank() || updatedProduct == null
                || currentUser == null || currentUser.isBlank()) {
            return false;
        }
        try {
            Product existing = getProductByKey(key);
            if (existing == null || !currentUser.equals(existing.getSellerUsername())) {
                return false;
            }

            StringBuilder json = new StringBuilder("{");
            appendJsonField(json, "name", updatedProduct.getName());
            appendJsonField(json, "description", updatedProduct.getDescription());
            appendJsonField(json, "timeRemaining", updatedProduct.getTimeRemaining());
            appendJsonField(json, "imagePath", updatedProduct.getImagePath());
            appendJsonField(json, "category", updatedProduct.getCategory());
            appendJsonField(json, "status", updatedProduct.getStatus());
            appendJsonNumber(json, "stepPrice", updatedProduct.getStepPrice());
            appendJsonNumber(json, "endTime", updatedProduct.getEndTime());
            if (existing.getHighestBidder() == null || existing.getHighestBidder().isBlank()) {
                appendJsonNumber(json, "currentBid", updatedProduct.getCurrentBid());
            }
            json.append("}");

            request("PATCH", "/products/" + key + ".json", json.toString());
            return true;
        } catch (Exception e) {
            lastError = e.getMessage();
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteProductIfOwner(String key, String currentUser) {
        if (key == null || key.isBlank() || currentUser == null || currentUser.isBlank()) {
            return false;
        }
        try {
            Product existing = getProductByKey(key);
            if (existing == null || !currentUser.equals(existing.getSellerUsername())) {
                return false;
            }
            request("DELETE", "/products/" + key + ".json", null);
            return true;
        } catch (Exception e) {
            lastError = e.getMessage();
            e.printStackTrace();
            return false;
        }
    }

    public static String getUserPhone(String email) {
        if (email == null || email.isBlank()) {
            return "Chưa cập nhật";
        }
        try {
            String json = request("GET", "/users/" + userKey(email) + "/phone.json", null);
            if (json == null || json.equals("null") || json.isBlank()) {
                return "Chưa cập nhật";
            }
            return json.replace("\"", "");
        } catch (Exception e) {
            return "Chưa cập nhật";
        }
    }

    public static void saveBasicUser(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        syncOldUserIfNeeded(email);
    }

    public static void saveUserPhone(String email, String phone) {
        if (email == null || email.isBlank()) {
            return;
        }
        try {
            String role = email.equalsIgnoreCase("Admin@gmail.com") ? "admin" : "user";
            String json = "{"
                + "\"email\":\"" + escape(email) + "\","
                + "\"phone\":\"" + escape(phone) + "\","
                + "\"role\":\"" + role + "\","
                + "\"status\":\"active\""
                + "}";
            request("PATCH", "/users/" + userKey(email) + ".json", json);
        } catch (Exception e) {
            lastError = e.getMessage();
            e.printStackTrace();
        }
    }

    public static List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        try {
            String json = request("GET", "/users.json", null);
            if (json == null || json.equals("null") || json.isBlank()) {
                return users;
            }
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject obj = entry.getValue().getAsJsonObject();
                String email = stringValue(obj, "email", entry.getKey().replace(",", "."));
                String role = stringValue(obj, "role",
                    email.equalsIgnoreCase("Admin@gmail.com") ? "admin" : "user");
                String status = stringValue(obj, "status", "active");
                users.add(new User(email, role, status));
            }
        } catch (Exception e) {
            lastError = e.getMessage();
            e.printStackTrace();
        }
        return users;
    }

    public static void toggleUserStatus(String email, String newStatus) {
        if (email == null || newStatus == null) {
            return;
        }
        try {
            request("PATCH", "/users/" + userKey(email) + ".json",
                "{\"status\":\"" + escape(newStatus) + "\"}");
        } catch (Exception e) {
            lastError = e.getMessage();
            e.printStackTrace();
        }
    }

    public static void syncOldUserIfNeeded(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        try {
            String key = userKey(email);
            String existing = request("GET", "/users/" + key + ".json", null);
            if (existing != null && !existing.equals("null")) {
                return;
            }
            String role = email.equalsIgnoreCase("Admin@gmail.com") ? "admin" : "user";
            String json = "{"
                + "\"email\":\"" + escape(email) + "\","
                + "\"status\":\"active\","
                + "\"role\":\"" + role + "\""
                + "}";
            request("PUT", "/users/" + key + ".json", json);
        } catch (Exception e) {
            lastError = e.getMessage();
            e.printStackTrace();
        }
    }

    public static String getUserStatus(String email) {
        if (email == null || email.isBlank()) {
            return "active";
        }
        try {
            String json = request("GET", "/users/" + userKey(email) + "/status.json", null);
            if (json == null || json.equals("null") || json.isBlank()) {
                return "active";
            }
            return json.replace("\"", "");
        } catch (Exception e) {
            lastError = e.getMessage();
            e.printStackTrace();
            return "active";
        }
    }

    public static void deleteProduct(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            request("DELETE", "/products/" + key + ".json", null);
        } catch (Exception e) {
            lastError = e.getMessage();
            e.printStackTrace();
        }
    }

    public static void saveBidHistory(String productKey, String bidder, double amount) {
        if (productKey == null || productKey.isBlank() || bidder == null || bidder.isBlank()) {
            return;
        }
        try {
            String json = "{"
                + "\"bidder\":\"" + escape(bidder) + "\","
                + "\"amount\":" + amount + ","
                + "\"timestamp\":" + System.currentTimeMillis()
                + "}";
            request("POST", "/products/" + productKey + "/bidHistory.json", json);
        } catch (Exception e) {
            lastError = e.getMessage();
            e.printStackTrace();
        }
    }

    public static List<BidHistoryEntry> loadBidHistorySync(String productKey, boolean[] hasBidsOut) {
        List<BidHistoryEntry> result = new ArrayList<>();
        if (productKey == null || productKey.isBlank()) {
            return result;
        }
        try {
            String json = request("GET", "/products/" + productKey + "/bidHistory.json", null);
            if (json == null || json.equals("null") || json.isBlank()) {
                return result;
            }
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject bid = entry.getValue().getAsJsonObject();
                String bidder = stringValue(bid, "bidder", "");
                double amount = doubleValue(bid, "amount", 0.0);
                long timestamp = longValue(bid, "timestamp", System.currentTimeMillis());
                if (bidder.isBlank()) {
                    continue;
                }
                String line = "[" + formatTimestamp(timestamp) + "] "
                    + bidder + " trả: " + formatVNDStatic(amount);
                result.add(0, new BidHistoryEntry(entry.getKey(), line));
            }
            if (hasBidsOut != null && !result.isEmpty()) {
                hasBidsOut[0] = true;
            }
        } catch (Exception e) {
            lastError = e.getMessage();
            e.printStackTrace();
        }
        return result;
    }

    public static String formatTimestamp(Long timestamp) {
        long value = timestamp != null ? timestamp : System.currentTimeMillis();
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
        format.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        return format.format(new Date(value));
    }

    public static String formatVNDStatic(double amount) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount) + " VNĐ";
    }

    public static String getLastError() {
        return lastError;
    }

    public static String uploadImage(java.io.File imageFile) {
        lastError = null;
        if (imageFile == null) {
            lastError = "Chưa chọn file ảnh.";
            return null;
        }

        try {
            String path = "products/" + System.currentTimeMillis() + "_" + imageFile.getName();
            String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8);
            URL url = new URL(
                "https://firebasestorage.googleapis.com/v0/b/"
                    + STORAGE_BUCKET + "/o?uploadType=media&name=" + encodedPath);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(20_000);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", contentTypeFor(imageFile.getName()));

            String token = AuthService.currentUserIdToken != null
                ? AuthService.currentUserIdToken
                : AuthService.idToken;
            if (token != null && !token.isBlank()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            try (OutputStream output = conn.getOutputStream()) {
                Files.copy(imageFile.toPath(), output);
            }

            int code = conn.getResponseCode();
            String response = readConnectionResponse(conn, code);
            if (code < 200 || code >= 300) {
                lastError = "Firebase Storage HTTP " + code + ": " + response;
                return null;
            }

            String downloadUrl = "https://firebasestorage.googleapis.com/v0/b/"
                + STORAGE_BUCKET + "/o/" + encodedPath + "?alt=media";
            try {
                JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
                String tokenValue = stringValue(obj, "downloadTokens", "");
                if (!tokenValue.isBlank()) {
                    downloadUrl += "&token=" + URLEncoder.encode(tokenValue, StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) {
            }
            return downloadUrl;
        } catch (Exception e) {
            lastError = e.getMessage();
            e.printStackTrace();
            return null;
        }
    }

    private static Product productFromJson(String key, JsonObject object) {
        Product product = new Product(
            stringValue(object, "id", key),
            stringValue(object, "name", "Không tên"),
            stringValue(object, "description", ""),
            doubleValue(object, "currentBid", 0.0),
            stringValue(object, "timeRemaining", ""),
            stringValue(object, "imagePath", ""),
            stringValue(object, "sellerUsername", "Ẩn danh"),
            stringValue(object, "category", "Khác"),
            doubleValue(object, "stepPrice", 0.0),
            stringValue(object, "status", "active"),
            longValue(object, "endTime", 0L)
        );
        product.setFirebaseKey(key);
        product.setHighestBidder(stringValue(object, "highestBidder", ""));
        product.setHighestBidderPhone(stringValue(object, "highestBidderPhone", ""));
        product.setHighestBidderEmail(stringValue(object, "highestBidderEmail", ""));
        if (product.isEnded()) {
            product.setStatus("ended");
        }
        return product;
    }

    private static String request(String method, String path, String body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + withAuth(path)))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json; charset=utf-8")
            .header("Accept", "application/json")
            .method(method, publisher)
            .build();

        HttpResponse<String> response = HTTP_CLIENT.send(
            request,
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() >= 400) {
            throw new IllegalStateException(
                "Firebase HTTP " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private static String withAuth(String path) {
        String token = AuthService.currentUserIdToken;
        if (token == null || token.isBlank()) {
            return path;
        }
        String separator = path.contains("?") ? "&" : "?";
        return path + separator + "auth="
            + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private static void appendJsonField(StringBuilder json, String key, String value) {
        appendCommaIfNeeded(json);
        json.append("\"").append(escape(key)).append("\":\"")
            .append(escape(value)).append("\"");
    }

    private static void appendJsonNumber(StringBuilder json, String key, double value) {
        appendCommaIfNeeded(json);
        json.append("\"").append(escape(key)).append("\":").append(value);
    }

    private static void appendJsonNumber(StringBuilder json, String key, long value) {
        appendCommaIfNeeded(json);
        json.append("\"").append(escape(key)).append("\":").append(value);
    }

    private static void appendCommaIfNeeded(StringBuilder json) {
        if (json.length() > 1) {
            json.append(",");
        }
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    private static String userKey(String email) {
        return email.replace(".", ",");
    }

    private static boolean hasValue(JsonObject obj, String member) {
        return obj.has(member)
            && !(obj.get(member) instanceof JsonNull)
            && !obj.get(member).isJsonNull();
    }

    private static String stringValue(JsonObject obj, String member, String fallback) {
        return hasValue(obj, member) ? obj.get(member).getAsString() : fallback;
    }

    private static double doubleValue(JsonObject obj, String member, double fallback) {
        return hasValue(obj, member) ? obj.get(member).getAsDouble() : fallback;
    }

    private static long longValue(JsonObject obj, String member, long fallback) {
        return hasValue(obj, member) ? obj.get(member).getAsLong() : fallback;
    }

    private static String contentTypeFor(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private static String readConnectionResponse(HttpURLConnection conn, int statusCode) throws Exception {
        InputStream stream = statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        }
    }
}
