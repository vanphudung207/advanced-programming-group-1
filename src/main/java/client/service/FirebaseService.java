package client.service;

import client.model.Product;
import client.model.User;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

public class FirebaseService {

    private static final String BASE_URL =
        "https://advanced-programming-group-1-default-rtdb.asia-southeast1.firebasedatabase.app";

    public static String currentUserEmail = null;
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

                String key = entry.getKey();
                JsonObject obj = entry.getValue().getAsJsonObject();
                Product p = new Product(
                    stringValue(obj, "id", key),
                    stringValue(obj, "name", "Không tên"),
                    stringValue(obj, "description", ""),
                    doubleValue(obj, "currentBid", 0.0),
                    stringValue(obj, "timeRemaining", ""),
                    stringValue(obj, "imagePath", ""),
                    stringValue(obj, "sellerUsername", "Ẩn danh"),
                    stringValue(obj, "category", "Khác"),
                    doubleValue(obj, "stepPrice", 0.0),
                    stringValue(obj, "status", "active"),
                    longValue(obj, "endTime", 0L)
                );
                p.setFirebaseKey(key);
                p.setHighestBidder(stringValue(obj, "highestBidder", ""));
                p.setHighestBidderPhone(stringValue(obj, "highestBidderPhone", ""));
                p.setHighestBidderEmail(stringValue(obj, "highestBidderEmail", ""));
                products.add(0, p);
            }
        } catch (Exception e) {
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
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            Product p = new Product(
                stringValue(obj, "id", key),
                stringValue(obj, "name", "Không tên"),
                stringValue(obj, "description", ""),
                doubleValue(obj, "currentBid", 0.0),
                stringValue(obj, "timeRemaining", ""),
                stringValue(obj, "imagePath", ""),
                stringValue(obj, "sellerUsername", "Ẩn danh"),
                stringValue(obj, "category", "Khác"),
                doubleValue(obj, "stepPrice", 0.0),
                stringValue(obj, "status", "active"),
                longValue(obj, "endTime", 0L)
            );
            p.setFirebaseKey(key);
            p.setHighestBidder(stringValue(obj, "highestBidder", ""));
            p.setHighestBidderPhone(stringValue(obj, "highestBidderPhone", ""));
            p.setHighestBidderEmail(stringValue(obj, "highestBidderEmail", ""));
            return p;
        } catch (Exception e) {
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

        updateBid(key, bid, bidder, phone, email, endTime);
        saveBidHistory(key, bidder, bid);

        return new BidResult(true, "OK", bid, bidder, phone, email, endTime, extended);
    }

    public static void updateBid(String firebaseKey, double bid, String bidder,
                                 String phone, String email) {
        updateBid(firebaseKey, bid, bidder, phone, email, -1L);
    }

    private static void updateBid(String firebaseKey, double bid, String bidder,
                                  String phone, String email, long endTime) {
        if (firebaseKey == null || firebaseKey.isBlank()) {
            return;
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
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    public static List<Product> searchProducts(String keyword) {
        List<Product> result = new ArrayList<>();
        String kw = keyword == null ? "" : keyword.toLowerCase();
        for (Product p : getAllProducts()) {
            if (p.getName() != null && p.getName().toLowerCase().contains(kw)) {
                result.add(p);
            }
        }
        return result;
    }

    public static List<Product> getProductsByCategory(String category) {
        if (category == null || category.equals("Tất cả sản phẩm")) {
            return getAllProducts();
        }
        List<Product> result = new ArrayList<>();
        for (Product p : getAllProducts()) {
            if (category.equals(p.getCategory())) {
                result.add(p);
            }
        }
        return result;
    }

    public static String getUserPhone(String email) {
        if (email == null || email.isBlank()) {
            return "Chưa cập nhật";
        }
        try {
            String key = userKey(email);
            String json = request("GET", "/users/" + key + "/phone.json", null);
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
            String key = userKey(email);
            String json = "{"
                + "\"email\":\"" + escape(email) + "\","
                + "\"phone\":\"" + escape(phone) + "\","
                + "\"role\":\"" + (email.equalsIgnoreCase("Admin@gmail.com") ? "admin" : "user") + "\","
                + "\"status\":\"active\""
                + "}";
            request("PATCH", "/users/" + key + ".json", json);
        } catch (Exception e) {
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
                String phone = stringValue(obj, "phone", "Chưa cập nhật");
                users.add(new User(email, "", phone));
            }
        } catch (Exception e) {
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
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
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
            e.printStackTrace();
        }
        return result;
    }

    public static String formatTimestamp(Long ts) {
        long timestamp = ts != null ? ts : System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        return sdf.format(new Date(timestamp));
    }

    public static String formatVNDStatic(double amount) {
        return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(amount);
    }

    public static String getLastError() {
        return lastError;
    }

    private static String request(String method, String path, String body) throws Exception {
        URL url = new URL(BASE_URL + withAuth(path));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);

        if (body != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }

        int responseCode = conn.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(),
            StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        if (responseCode >= 400) {
            throw new IllegalStateException("Firebase HTTP " + responseCode + ": " + response);
        }
        return response.toString();
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
}
