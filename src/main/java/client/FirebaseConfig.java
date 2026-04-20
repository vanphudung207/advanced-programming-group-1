package client;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.FileInputStream;
import java.io.IOException;

public class FirebaseConfig {

    public static void init() {
        if (!FirebaseApp.getApps().isEmpty()) return;
        try {
            FileInputStream serviceAccount =
                new FileInputStream("serviceAccountKey.json");

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

            FirebaseApp.initializeApp(options);
            System.out.println("Firebase connected!");

        } catch (IOException e) {
            System.err.println("Firebase init failed: " + e.getMessage());
        }
    }
}