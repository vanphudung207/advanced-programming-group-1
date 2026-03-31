package client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
public class ProductListController {

    // Liên kết với dòng chữ Welcome trên góc phải
    @FXML
    private Label lblWelcome;

    // Liên kết với dòng chữ hiển thị Ngày Giờ
    @FXML
    private Label lblDateTime;

    // Liên kết với cái khung bự ở giữa để sau này nhét sản phẩm vào
    @FXML
    private FlowPane productContainer;

    // Hàm tự động chạy khi màn hình được bật lên
    @FXML
    public void initialize() {
        
        // 1. CẬP NHẬT NGÀY GIỜ CHẠY LIÊN TỤC (ĐỒNG HỒ ĐỘNG)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a | dd MMM, yyyy");
        
        // Tạo một vòng lặp Timeline: Cứ mỗi 1 giây (Duration.seconds(1)) sẽ lấy giờ hệ thống gắn lại vào Label
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            lblDateTime.setText(LocalDateTime.now().format(formatter));
        }), new KeyFrame(Duration.seconds(1)));
        
        clock.setCycleCount(Animation.INDEFINITE); // Cho vòng lặp chạy vô hạn
        clock.play(); // Bắt đầu đếm giờ!

        // 2. Cập nhật lời chào (Giả sử lấy từ MockDatabase)
        // Nếu lúc trước đã đăng nhập thành công, ta sẽ lấy tên user ra hiển thị
        if (MockDatabase.registeredUsername != null) {
            lblWelcome.setText("Welcome, " + MockDatabase.registeredUsername + "!");
        }
    }

    @FXML
    private void handleLogoutAction(javafx.event.ActionEvent event) {
        try {
            // 1. Xóa dấu vết đăng nhập trong hệ thống giả lập
            MockDatabase.registeredUsername = null;

            // 2. Tải lại file giao diện Đăng nhập
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/Login.fxml"));
            javafx.scene.Parent loginRoot = loader.load();
            
            // 3. Lấy cửa sổ hiện tại và thay thế bằng giao diện Login
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            
            // Quay về kích thước nhỏ gọn của màn hình Login
            stage.setScene(new javafx.scene.Scene(loginRoot, 550, 500));
            stage.setMaximized(false); // Tắt chế độ toàn màn hình khi về Login
            stage.centerOnScreen();
            stage.setTitle("Online Auction System - Login");

            System.out.println("Logged out successfully!");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}