// Khai báo package chứa file khởi chạy
package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

// Lớp MainApp bắt buộc phải kế thừa Application của JavaFX
public class MainApp extends Application {

    // Hàm start là điểm bắt đầu của mọi ứng dụng JavaFX
    @Override
    public void start(Stage primaryStage) throws Exception {

        // 1. Chỉ định đường dẫn tới file thiết kế Login.fxml
        URL fxmlLocation =
            getClass().getResource(
                "/client/view/Register.fxml"
            );

        // 2. Dùng FXMLLoader để đọc file FXML
        FXMLLoader loader =
            new FXMLLoader(fxmlLocation);

        Parent root =
            loader.load();

        // 3. Tạo Scene
        Scene scene =
            new Scene(root, 600, 550);

        // 4. Thiết lập cửa sổ
        primaryStage.setTitle(
            "Hệ thống Đấu giá Trực tuyến"
        );

        primaryStage.setScene(scene);

        primaryStage.setResizable(false);

        // 5. Hiển thị cửa sổ
        primaryStage.setMaximized(true);

        primaryStage.show();
    }

    // Hàm main tiêu chuẩn của Java
    public static void main(String[] args) {

        launch(args);
    }
}