package client.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AuthService {
    public static String currentUserEmail = null;
    public static String currentUserIdToken = null;
    public static String currentUserLocalId = null;

    private static final String API_KEY =
        "AIzaSyAGYt-4tSNEK6U2qZZvkBTYDglkygMlqVA";

    public static boolean register(String email, String password) {
        return authenticate(
            "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + API_KEY,
            email,
            password,
            "REGISTER"
        );
    }

    public static boolean login(String email, String password) {
        return authenticate(
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + API_KEY,
            email,
            password,
            "LOGIN"
        );
    }

    public static void logout() {
        currentUserEmail = null;
        currentUserIdToken = null;
        currentUserLocalId = null;
    }

    private static boolean authenticate(String endpoint, String email, String password, String label) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            String json = "{"
                + "\"email\":\"" + escape(email) + "\","
                + "\"password\":\"" + escape(password) + "\","
                + "\"returnSecureToken\":true"
                + "}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            String response = readResponse(conn, responseCode);
            System.out.println(label + " CODE: " + responseCode);

            if (responseCode != 200) {
                System.out.println(label + " ERROR: " + response);
                currentUserEmail = null;
                currentUserIdToken = null;
                currentUserLocalId = null;
                return false;
            }

            JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
            currentUserEmail = email;
            currentUserIdToken = obj.has("idToken") ? obj.get("idToken").getAsString() : null;
            currentUserLocalId = obj.has("localId") ? obj.get("localId").getAsString() : null;
            FirebaseService.currentUserEmail = email;
            return currentUserIdToken != null && !currentUserIdToken.isBlank();
        } catch (Exception e) {
            e.printStackTrace();
            currentUserEmail = null;
            currentUserIdToken = null;
            currentUserLocalId = null;
            return false;
        }
    }

    private static String readResponse(HttpURLConnection conn, int responseCode) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(),
            StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
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
}
