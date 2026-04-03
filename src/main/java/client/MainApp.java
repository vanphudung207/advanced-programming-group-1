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

    // Hàm start là điểm bắt đầu của mọi ứng dụng JavaFX (tương tự hàm main bình thường)
    @Override
    public void start(Stage primaryStage) throws Exception {
        
        // 1. Chỉ định đường dẫn tới file thiết kế Login.fxml của bạn
        // Lưu ý: Dấu "/" ở đầu đại diện cho thư mục gốc của resources
        URL fxmlLocation = getClass().getResource("/client/view/Register.fxml");
        
        // 2. Dùng FXMLLoader để đọc file FXML và biến nó thành đối tượng giao diện (Parent)
        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();

        // 3. Đặt toàn bộ giao diện vào một "khung cảnh" (Scene) với kích thước 450x300 pixel
        Scene scene = new Scene(root, 600, 550);

        // 4. Thiết lập các thông số cho cửa sổ phần mềm (Stage)
        primaryStage.setTitle("Hệ thống Đấu giá Trực tuyến"); // Tiêu đề cửa sổ
        primaryStage.setScene(scene); // Lắp Scene vào Stage
        primaryStage.setResizable(false); // Tạm khóa tính năng kéo giãn cửa sổ để form không bị xô lệch
        
        // 5. Hiển thị cửa sổ lên màn hình
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    // Hàm main tiêu chuẩn của Java để kích hoạt ứng dụng
    public static void main(String[] args) {
        launch(args);
    }
}