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
    // HÀM TẢI VÀ VẼ DANH SÁCH SẢN PHẨM LÊN LƯỚI (ĐÃ TRANG TRÍ HOVER & BO GÓC)
    // ==============================================================
    private void loadProductsToGrid() {
        productContainer.getChildren().clear();
        List<Product> products = MockDatabase.getAllProducts();

        for (Product p : products) {
            
            // --- 1. TẠO KHUNG NGOÀI CỦA THẺ (VBox) ---
            VBox card = new VBox(10); 
            // Lưu sẵn 2 chuỗi Style để lát nữa dùng cho hiệu ứng Hover (Di chuột)
            String normalStyle = "-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);";
            String hoverStyle  = "-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 15, 0, 0, 5); -fx-cursor: hand;";
            
            card.setStyle(normalStyle);
            card.setPrefWidth(220); 
            card.setAlignment(Pos.TOP_CENTER); 

            // HIỆU ỨNG HOVER CHO THẺ: Đưa chuột vào thì đổ bóng to ra (nổi lên), lôi ra thì xẹp xuống
            card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
            card.setOnMouseExited(e -> card.setStyle(normalStyle));

            // --- 2. XỬ LÝ ẢNH SẢN PHẨM (Có bo tròn góc) ---
            ImageView imgView = new ImageView();
            try {
                Image img = new Image(p.getImagePath(), true);
                imgView.setImage(img);
            } catch (Exception ex) {
                System.out.println("Lỗi tải ảnh cho: " + p.getName());
            }
            
            imgView.setFitWidth(180); 
            imgView.setFitHeight(130);
            imgView.setPreserveRatio(true);

            // TẠO MẶT NẠ BO GÓC CHO ẢNH (Clip)
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(180, 130);
            clip.setArcWidth(15);  // Độ cong của góc (số càng to càng tròn)
            clip.setArcHeight(15);
            imgView.setClip(clip); // Áp mặt nạ vào ảnh

            VBox imageContainer = new VBox(imgView);
            imageContainer.setAlignment(Pos.CENTER);
            imageContainer.setPrefHeight(130);

            // --- 3. HIỂN THỊ TÊN SẢN PHẨM ---
            Label nameLabel = new Label(p.getName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #2c3e50;");

            // --- 4. HIỂN THỊ CHỮ "Current bid" ---
            Label bidSubtitle = new Label("Current bid");
            bidSubtitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

            // --- 5. TẠO KHUNG CHỨA GIÁ TIỀN & THỜI GIAN (Xếp dọc VBox để chống tràn chữ) ---
            // Đổi HBox thành VBox, khoảng cách giữa 2 dòng là 8px
            VBox priceTimeBox = new VBox(8); 
            priceTimeBox.setAlignment(Pos.CENTER);

            Label priceLabel = new Label(String.format("%,.0f VNĐ", p.getCurrentBid()));
            priceLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 16px;");

            Label timeLabel = new Label("⏱ " + p.getTimeRemaining());
            timeLabel.setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold; -fx-background-color: #ebf5fb; -fx-padding: 3px 8px; -fx-background-radius: 5px;");

            // Nhét Giá (trên) và Thời gian (dưới) vào chung 1 cột
            priceTimeBox.getChildren().addAll(priceLabel, timeLabel);

            // --- 6. TẠO NÚT "Bid Now" (Có hiệu ứng Hover đổi màu) ---
            Button btnBid = new Button("Bid Now");
            String btnNormal = "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5px; -fx-padding: 8px 15px;";
            String btnHover  = "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5px; -fx-padding: 8px 15px;";
            
            btnBid.setStyle(btnNormal);
            btnBid.setMaxWidth(Double.MAX_VALUE); 
            
            // Đưa chuột vào nút thì nút sáng màu xanh lên
            btnBid.setOnMouseEntered(e -> btnBid.setStyle(btnHover));
            btnBid.setOnMouseExited(e -> btnBid.setStyle(btnNormal));
            
            // Xử lý sự kiện click chuyển sang phòng đấu giá (Code cũ giữ nguyên)
            btnBid.setOnAction(event -> {
                try {
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/AuctionRoom.fxml"));
                    javafx.scene.Parent auctionRoot = loader.load();
                    
                    AuctionRoomController roomController = loader.getController();
                    roomController.setProductData(p);
                    
                    javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                    stage.setScene(new javafx.scene.Scene(auctionRoot, 900, 650));
                    stage.setMaximized(true); 
                    stage.centerOnScreen();
                    stage.setTitle("Auction Room - " + p.getName());
                    
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            });

            // --- 7. RÁP TẤT CẢ VÀO THẺ ---
            card.getChildren().addAll(imageContainer, nameLabel, bidSubtitle, priceTimeBox, btnBid);

            // --- 8. ĐƯA THẺ VÀO LƯỚI ---
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
    // HÀM XỬ LÝ NÚT "THÔNG TIN" TÀI KHOẢN (ĐÃ NÂNG CẤP HIỂN THỊ SĐT)
    // ==============================================================
    @FXML
    private void handleShowInfo(javafx.event.ActionEvent event) {
        if (MockDatabase.registeredUsername != null) {
            
            // 1. Lấy tên tài khoản hiện tại
            String username = MockDatabase.registeredUsername;
            
            // 2. Chạy sang Database để "lục" số điện thoại của người này
            String phone = MockDatabase.getUserPhone(username);
            
            // 3. Gắn cả Tên và SĐT lên cái nút 
            btnInfo.setText("Tên TK: " + username + "\nSĐT: " + phone);
            
            // 4. Đổi màu chữ sang xanh báo hiệu thành công
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