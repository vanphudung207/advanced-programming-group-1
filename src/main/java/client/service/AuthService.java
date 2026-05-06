package client.service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AuthService {

    private static final String API_KEY =
        "AIzaSyAGYt-4tSNEK6U2qZZvkBTYDglkygMlqVA";

    // Đăng ký
    public static boolean register(
        String email,
        String password
    ) {

        try {
            //Api endpoint để kết nối tới fb
            URL url = new URL(
                "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key="
                + API_KEY
            );
            // mở kết nối http đến fb
            HttpURLConnection conn =
                (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");

            conn.setRequestProperty(
                "Content-Type",
                "application/json"
            );

            conn.setDoOutput(true);
            //định dạng dữ liệu gửi đi
            String json =
                "{"
                + "\"email\":\"" + email + "\","
                + "\"password\":\"" + password + "\","
                + "\"returnSecureToken\":true"
                + "}";

            try(OutputStream os =
                    conn.getOutputStream()) {

                byte[] input =
                    json.getBytes("utf-8");

                os.write(input, 0, input.length);
            }
            //nếu dữ liệu đúng fb sẽ phản hổi 200
            int responseCode = conn.getResponseCode();
            System.out.println("REGISTER CODE: " + responseCode);
            return responseCode == 200;

        } catch(Exception e) {

            e.printStackTrace();
            return false;
        }
    }

    // Đăng nhập
    public static boolean login(
        String email,
        String password
    ) {

        try {

            URL url = new URL(
                "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key="
                + API_KEY
            );

            HttpURLConnection conn =
                (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");

            conn.setRequestProperty(
                "Content-Type",
                "application/json"
            );

            conn.setDoOutput(true);

            String json =
                "{"
                + "\"email\":\"" + email + "\","
                + "\"password\":\"" + password + "\","
                + "\"returnSecureToken\":true"
                + "}";

            try(OutputStream os =
                    conn.getOutputStream()) {

                byte[] input =
                    json.getBytes("utf-8");

                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            System.out.println("LOGIN CODE: " + responseCode);
            return responseCode == 200;

        } catch(Exception e) {

            e.printStackTrace();
            return false;
        }
    }
}