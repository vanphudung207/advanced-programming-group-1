package client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import client.model.Product;

public class ProductListController {

    @FXML 
    private Button btnInfo;
    
    // Liên kết với dòng chữ hiển thị Ngày Giờ
    @FXML
    private Label lblDateTime;

    // Liên kết với cái khung bự ở giữa để sau này nhét sản phẩm vào
    @FXML
    private FlowPane productContainer;

    // ==============================================================
    // HÀM KHỞI TẠO: Tự động chạy khi màn hình được bật lên
    // ==============================================================
    @FXML
    public void initialize() {
        
        // 1. CẬP NHẬT NGÀY GIỜ CHẠY LIÊN TỤC (ĐỒNG HỒ ĐỘNG)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a | dd MMM, yyyy");
        
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            lblDateTime.setText(LocalDateTime.now().format(formatter));
        }), new KeyFrame(Duration.seconds(1)));
        
        clock.setCycleCount(Animation.INDEFINITE); 
        clock.play(); 

        // 2. GỌI HÀM VẼ SẢN PHẨM LÊN MÀN HÌNH (Rất quan trọng, lúc nãy bị thiếu dòng này)
        loadProductsToGrid();
    }

    // ==============================================================
    // HÀM TẢI VÀ VẼ DANH SÁCH SẢN PHẨM LÊN LƯỚI
    // ==============================================================
    private void loadProductsToGrid() {
        // Xóa sạch các sản phẩm cũ (nếu có) trước khi vẽ cái mới để không bị nhân đôi
        productContainer.getChildren().clear();

        // Lấy danh sách sản phẩm từ cơ sở dữ liệu giả (Sau này sẽ lấy từ Backend)
        List<Product> products = MockDatabase.getAllProducts();

        // Vòng lặp: Duyệt qua từng món hàng để "vẽ" ra một cái Thẻ (Card)
        for (Product p : products) {
            
            // --- 1. TẠO KHUNG NGOÀI CỦA THẺ (VBox) ---
            VBox card = new VBox(10); // 10 là khoảng cách (spacing) giữa các dòng bên trong thẻ
            card.setStyle("-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 10px; "
                        + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
            card.setPrefWidth(220); // Ép chiều rộng của thẻ là 220px để các thẻ đều nhau
            card.setAlignment(Pos.TOP_CENTER); // Căn lề giữa cho đẹp

            // --- 2. XỬ LÝ ẢNH SẢN PHẨM (Nâng cấp lên ImageView) ---
            ImageView imgView = new ImageView();
            try {
                // Tải ảnh từ đường dẫn. Chữ 'true' ở cuối giúp tải ngầm, app không bị đơ
                Image img = new Image(p.getImagePath(), true);
                imgView.setImage(img);
            } catch (Exception ex) {
                System.out.println("Lỗi tải ảnh cho: " + p.getName());
            }
            
            // Ép kích thước ảnh cố định để khung hình không bị nhảy lung tung
            imgView.setFitWidth(180); 
            imgView.setFitHeight(130);
            imgView.setPreserveRatio(true); // Giữ tỷ lệ gốc để ảnh không bị bóp méo

            // Đặt ảnh vào một khung VBox riêng để căn giữa dễ dàng hơn
            VBox imageContainer = new VBox(imgView);
            imageContainer.setAlignment(Pos.CENTER);
            imageContainer.setPrefHeight(130);

            // --- 3. HIỂN THỊ TÊN SẢN PHẨM ---
            Label nameLabel = new Label(p.getName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            // --- 4. HIỂN THỊ CHỮ "Current bid" NHỎ MÀU XÁM ---
            Label bidSubtitle = new Label("Current bid");
            bidSubtitle.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");

            // --- 5. TẠO KHUNG CHỨA GIÁ TIỀN & THỜI GIAN (HBox: Dàn hàng ngang) ---
            HBox priceTimeBox = new HBox();
            priceTimeBox.setAlignment(Pos.CENTER);
            priceTimeBox.setSpacing(20); // Ép giá và thời gian cách nhau 20px

            Label priceLabel = new Label("$" + p.getCurrentBid());
            priceLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 16px;");

            Label timeLabel = new Label("⏱ " + p.getTimeRemaining());
            timeLabel.setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;");

            priceTimeBox.getChildren().addAll(priceLabel, timeLabel); // Nhét giá và giờ vào chung 1 dòng ngang

            // --- 6. TẠO NÚT "Bid Now" ---
            Button btnBid = new Button("Bid Now");
            btnBid.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; "
                          + "-fx-cursor: hand; -fx-background-radius: 5px; -fx-padding: 8px 15px;");
            btnBid.setMaxWidth(Double.MAX_VALUE); // Cho nút dàn trải full chiều ngang của thẻ
            
            // (Chúng ta sẽ thêm sự kiện chuyển trang cho nút này ở bước tiếp theo)
            // ==============================================================
            // MỚI THÊM VÀO: Bắt sự kiện khi người dùng click vào nút Bid Now
            // ==============================================================
            btnBid.setOnAction(event -> {
                try {
                    // 1. Tải file giao diện Phòng đấu giá (Bản nháp)
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/AuctionRoom.fxml"));
                    javafx.scene.Parent auctionRoot = loader.load();
                    
                    // 2. LẤY CONTROLLER CỦA PHÒNG ĐẤU GIÁ ĐỂ TRUYỀN DỮ LIỆU
                    AuctionRoomController roomController = loader.getController();
                    // Truyền nguyên cái thẻ sản phẩm 'p' (Product) hiện tại sang cho phòng đấu giá
                    roomController.setProductData(p);
                    
                    // 3. Mở cửa sổ Phòng đấu giá
                    javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                    stage.setScene(new javafx.scene.Scene(auctionRoot, 900, 650));
                    stage.setMaximized(true); 
                    stage.centerOnScreen();
                    stage.setTitle("Auction Room - " + p.getName());
                    
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            });
            // --- 7. RÁP TẤT CẢ VÀO THẺ (Từ trên xuống dưới) ---
            card.getChildren().addAll(imageContainer, nameLabel, bidSubtitle, priceTimeBox, btnBid);

            // --- 8. ĐƯA THẺ VÀO LƯỚI TRÊN MÀN HÌNH ---
            productContainer.getChildren().add(card);
        }
    }

    // ==============================================================
    // HÀM XỬ LÝ ĐĂNG XUẤT
    // ==============================================================
    @FXML
    private void handleLogoutAction(javafx.event.ActionEvent event) {
        try {
            MockDatabase.registeredUsername = null;
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/Login.fxml"));
            javafx.scene.Parent loginRoot = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) productContainer.getScene().getWindow();
            
            stage.setScene(new javafx.scene.Scene(loginRoot, 550, 500));
            stage.setMaximized(false); 
            stage.centerOnScreen();
            stage.setTitle("Online Auction System - Login");
            System.out.println("Logged out successfully!");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // ==============================================================
    // HÀM XỬ LÝ NÚT "THÔNG TIN" TÀI KHOẢN
    // ==============================================================
    @FXML
    private void handleShowInfo(javafx.event.ActionEvent event) {
        if (MockDatabase.registeredUsername != null) {
            btnInfo.setText("Tên TK: " + MockDatabase.registeredUsername);
            btnInfo.setStyle("-fx-background-color: transparent; -fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-cursor: default;");
        } else {
            btnInfo.setText("Chưa đăng nhập!");
        }
    }

    // ==============================================================
    // HÀM CHUYỂN SANG TRANG "ĐĂNG SẢN PHẨM"
    // ==============================================================
    @FXML
    private void handleGoToAddProduct(javafx.event.ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/AddProduct.fxml"));
            javafx.scene.Parent addProductRoot = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            
            stage.setScene(new javafx.scene.Scene(addProductRoot, 800, 600));
            stage.setMaximized(false); 
            stage.centerOnScreen();
            stage.setTitle("Online Auction System - Add Product");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}