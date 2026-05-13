package client.service;

import client.model.Product;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class FirebaseService {

    private static final String BASE_URL = "https://advanced-programming-group-1-default-rtdb.asia-southeast1.firebasedatabase.app";
    public static String currentUserEmail = null;

    // ==============================================================
    // 1. THÊM SẢN PHẨM 
    // ==============================================================
    public static String addProduct(Product newProduct) {
        try {
            URL url = new URL(BASE_URL + "/products.json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            long endTime = newProduct.getEndTime() > 0
                ? newProduct.getEndTime()
                : System.currentTimeMillis() + 60000;

            String json = "{"
                + "\"name\":\"" + escape(newProduct.getName()) + "\","
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

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                br.close();
                System.out.println(" Đăng sản phẩm thành công (HTTP)!");
                return response.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ==============================================================
    // 2. LẤY TOÀN BỘ SẢN PHẨM 
    // ==============================================================
    public static List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        try {
            URL url = new URL(BASE_URL + "/products.json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            String json = response.toString();
            if (json == null || json.equals("null")) return products;

            // Dùng Gson để biến chuỗi HTTP thành danh sách Sản phẩm
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : jsonObject.entrySet()) {
                String key = entry.getKey();
                com.google.gson.JsonObject obj = entry.getValue().getAsJsonObject();

                Product p = new Product(
                    obj.has("id") ? obj.get("id").getAsString() : key,
                    obj.has("name") ? obj.get("name").getAsString() : "Không tên",
                    obj.has("description") ? obj.get("description").getAsString() : "",
                    obj.has("currentBid") ? obj.get("currentBid").getAsDouble() : 0.0,
                    obj.has("timeRemaining") ? obj.get("timeRemaining").getAsString() : "",
                    obj.has("imagePath") ? obj.get("imagePath").getAsString() : "",
                    obj.has("sellerUsername") ? obj.get("sellerUsername").getAsString() : "Ẩn danh",
                    obj.has("category") ? obj.get("category").getAsString() : "Khác",
                    obj.has("stepPrice") ? obj.get("stepPrice").getAsDouble() : 0.0,
                    obj.has("status") ? obj.get("status").getAsString() : "active",
                    obj.has("endTime") ? obj.get("endTime").getAsLong() : 0L
                );
                p.setFirebaseKey(key);
                p.setHighestBidder(obj.has("highestBidder") ? obj.get("highestBidder").getAsString() : "");
                p.setHighestBidderPhone(obj.has("highestBidderPhone") ? obj.get("highestBidderPhone").getAsString() : "");
                p.setHighestBidderEmail(obj.has("highestBidderEmail") ? obj.get("highestBidderEmail").getAsString() : "");

                products.add(0, p); // Đưa sản phẩm mới lên đầu
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return products;
    }

    // ==============================================================
    // 3. UPDATE GIÁ ĐẤU (HTTP request)
    // ==============================================================
    public static void updateBid(String firebaseKey, double bid, String bidder, String phone, String email) {
        try {
            URL url = new URL(BASE_URL + "/products/" + firebaseKey + ".json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PATCH");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String json = "{"
                + "\"currentBid\":" + bid + ","
                + "\"highestBidder\":\"" + escape(bidder) + "\","
                + "\"highestBidderPhone\":\"" + escape(phone) + "\","
                + "\"highestBidderEmail\":\"" + escape(email) + "\""
                + "}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            System.out.println(" Update bid thành công (HTTP)!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==============================================================
    // 4. ĐÁNH DẤU KẾT THÚC ĐẤU GIÁ 
    // ==============================================================
    public static void markAuctionEnded(String firebaseKey) {
        try {
            URL url = new URL(BASE_URL + "/products/" + firebaseKey + ".json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PATCH");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String json = "{"
                + "\"status\":\"ended\""
                + "}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            System.out.println(" Đấu giá kết thúc (HTTP)!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==============================================================
    // 5. CÁC HÀM TIỆN ÍCH LỌC TÌM KIẾM 
    // ==============================================================
    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static List<Product> searchProducts(String keyword) {
        List<Product> result = new ArrayList<>();
        String kw = keyword.toLowerCase();
        for (Product p : getAllProducts()) {
            if (p.getName() != null && p.getName().toLowerCase().contains(kw)) {
                result.add(p);
            }
        }
        return result;
    }

    public static List<Product> getProductsByCategory(String category) {
        if (category.equals("Tất cả sản phẩm")) {
            return getAllProducts();
        }
        List<Product> result = new ArrayList<>();
        for (Product p : getAllProducts()) {
            if (p.getCategory() != null && p.getCategory().equals(category)) {
                result.add(p);
            }
        }
        return result;
    }

    // ==============================================================
    // CÁC HÀM CỦA ADMIN
    // ==============================================================
    public static void saveBasicUser(String email) {
        try {
            if (email == null) return;

            // Firebase không cho dùng dấu "." trong key
            String key = email.replace(".", ",");

            URL url = new URL(BASE_URL + "/users/" + key + ".json");

            HttpURLConnection conn =
                (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("PUT");
            conn.setRequestProperty(
                "Content-Type",
                "application/json"
            );

            conn.setDoOutput(true);

            String json =
                "{"
                + "\"email\":\"" + email + "\","
                + "\"role\":\"user\","
                + "\"status\":\"active\""
                + "}";

            try(OutputStream os = conn.getOutputStream()) {

                byte[] input =
                    json.getBytes("utf-8");

                os.write(input, 0, input.length);
            }

            System.out.println(
                "SAVE USER CODE: " + conn.getResponseCode()
            );

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // ==============================================================
    // LẤY TOÀN BỘ USER BẰNG HTTP GET
    // ==============================================================
    public static List<client.model.User> getAllUsers() {

        List<client.model.User> users =
            new ArrayList<>();
        try {
            // URL lấy toàn bộ users
            URL url =
                new URL(
                    BASE_URL
                    + "/users.json"
                );
            HttpURLConnection conn =
                (HttpURLConnection)
                url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader in =
                new BufferedReader(
                    new InputStreamReader(
                        conn.getInputStream()
                    )
                );
            String inputLine;
            StringBuilder response =
                new StringBuilder();
            while (
                (inputLine = in.readLine()) != null
            ) {
                response.append(inputLine);
            }
            in.close();
            String json =
                response.toString();
            // Nếu database trống
            if (
                json == null
                || json.equals("null")
            ) {

                return users;
            }
            // Parse JSON bằng Gson
            com.google.gson.JsonObject jsonObject =
                com.google.gson.JsonParser
                    .parseString(json)
                    .getAsJsonObject();
            // Duyệt từng user
            for (
                java.util.Map.Entry
                <
                    String,
                    com.google.gson.JsonElement
                > entry
                : jsonObject.entrySet()
            ) {
                com.google.gson.JsonObject obj =
                    entry.getValue()
                        .getAsJsonObject();
                String email =
                    obj.has("email")
                    ? obj.get("email").getAsString()
                    : entry.getKey().replace(",", ".");
                String role =
                    obj.has("role")
                    ? obj.get("role").getAsString()
                    : "user";
                String status =
                    obj.has("status")
                    ? obj.get("status").getAsString()
                    : "active";
                users.add(
                    new client.model.User(
                        email,
                        role,
                        status
                    )
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }

    public static void toggleUserStatus(
    String email,
    String newStatus
    ) {
        try {
            String key =
                email.replace(".", ",");
            URL url =
                new URL(
                    BASE_URL
                    + "/users/"
                    + key
                    + ".json"
                );
            HttpURLConnection conn =(HttpURLConnection)url.openConnection();
            conn.setRequestMethod("PATCH");
            conn.setRequestProperty("Content-Type","application/json"
            );
            conn.setDoOutput(true);
            String json =
                "{"
                + "\"status\":\""
                + newStatus
                + "\""
                + "}";
            try(OutputStream os =conn.getOutputStream()) {
                byte[] input =
                    json.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            System.out.println("Update status success!");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // ==============================================================
    // ĐỒNG BỘ USER CŨ NẾU CHƯA TỒN TẠI, tạo role cho user trong database
    // ==============================================================
    public static void syncOldUserIfNeeded(
        String email
    ) {

        // Email null thì thôi
        if (email == null) return;

        try {

            // Firebase key không cho dấu .
            String key =
                email.replace(".", ",");

            // ======================================================
            // 1. KIỂM TRA USER ĐÃ TỒN TẠI CHƯA
            // ======================================================
            URL getUrl =
                new URL(
                    BASE_URL
                    + "/users/"
                    + key
                    + ".json"
                );

            HttpURLConnection getConn =
                (HttpURLConnection)
                getUrl.openConnection();

            getConn.setRequestMethod("GET");

            BufferedReader in =
                new BufferedReader(
                    new InputStreamReader(
                        getConn.getInputStream()
                    )
                );

            String inputLine;

            StringBuilder response =
                new StringBuilder();

            while (
                (inputLine = in.readLine()) != null
            ) {

                response.append(inputLine);
            }

            in.close();

            // ======================================================
            // 2. NẾU USER CHƯA CÓ -> TẠO MỚI
            // ======================================================
            if (
                response.toString().equals("null")
            ) {

                URL putUrl =
                    new URL(
                        BASE_URL
                        + "/users/"
                        + key
                        + ".json"
                    );

                HttpURLConnection putConn =
                    (HttpURLConnection)
                    putUrl.openConnection();

                putConn.setRequestMethod("PUT");

                putConn.setRequestProperty(
                    "Content-Type",
                    "application/json"
                );

                putConn.setDoOutput(true);

                // Admin thì role admin
                String role =
                    email.equalsIgnoreCase(
                        "Admin@gmail.com"
                    )
                    ? "admin"
                    : "user";

                String json =
                    "{"
                    + "\"email\":\"" + email + "\","
                    + "\"status\":\"active\","
                    + "\"role\":\"" + role + "\""
                    + "}";

                try(OutputStream os =
                        putConn.getOutputStream()) {

                    byte[] input =
                        json.getBytes("utf-8");

                    os.write(input, 0, input.length);
                }

                System.out.println(
                    "Đồng bộ user thành công!"
                );
            }

        } catch(Exception e) {

            e.printStackTrace();
        }
    }

    public static String getUserStatus(
    String email
    ) {
        try {
            String key =
                email.replace(".", ",");
            URL url =
                new URL(
                    BASE_URL
                    + "/users/"
                    + key
                    + "/status.json"
                );
            HttpURLConnection conn =(HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader in =
                new BufferedReader(
                    new InputStreamReader(
                        conn.getInputStream()
                    )
                );
            String inputLine;
            StringBuilder response =
                new StringBuilder();
            while (
                (inputLine = in.readLine()) != null
            ) {
                response.append(inputLine);
            }
            in.close();
            return response.toString()
                .replace("\"", "");
        } catch(Exception e) {
            e.printStackTrace();
            return "active";
        }
    }

    public static void deleteProduct(String key) {

        // Nếu key null thì thôi
        if (key == null) return;
        try {
            // URL tới đúng sản phẩm cần xóa trên Firebase
            URL url =
                new URL(
                    BASE_URL
                    + "/products/"
                    + key
                    + ".json"
                );
            // Mở kết nối HTTP
            HttpURLConnection conn =
                (HttpURLConnection)
                url.openConnection();
            // Dùng phương thức DELETE
            conn.setRequestMethod("DELETE");
            // Gửi request và lấy mã phản hồi
            int responseCode =
                conn.getResponseCode();
            System.out.println(
                "DELETE PRODUCT CODE: "
                + responseCode
            );
            // 200 = thành công
            if (responseCode == 200) {
                System.out.println(
                    "Đã xóa sản phẩm thành công!"
                );
            } else {
                System.out.println(
                    "Xóa sản phẩm thất bại!"
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}