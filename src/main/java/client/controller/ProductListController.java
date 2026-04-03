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
    
    @FXML
    private Label lblDateTime;

    @FXML
    private FlowPane productContainer;

    // ==============================================================
    // HÀM KHỞI TẠO
    // ==============================================================
    @FXML
    public void initialize() {
        
        // 1. CẬP NHẬT NGÀY GIỜ CHẠY LIÊN TỤC
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a | dd MMM, yyyy");
        
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            lblDateTime.setText(LocalDateTime.now().format(formatter));
        }), new KeyFrame(Duration.seconds(1)));
        
        clock.setCycleCount(Animation.INDEFINITE); 
        clock.play(); 

        // 2. GỌI HÀM VẼ SẢN PHẨM LÊN MÀN HÌNH
        loadProductsToGrid();
    }

    // ==============================================================
    // HÀM TẢI VÀ VẼ DANH SÁCH SẢN PHẨM LÊN LƯỚI
    // ==============================================================
    private void loadProductsToGrid() {
        productContainer.getChildren().clear();
        List<Product> products = MockDatabase.getAllProducts();

        for (Product p : products) {
            
            VBox card = new VBox(10); 
            String normalStyle = "-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);";
            String hoverStyle  = "-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 15, 0, 0, 5); -fx-cursor: hand;";
            
            card.setStyle(normalStyle);
            card.setPrefWidth(220); 
            card.setAlignment(Pos.TOP_CENTER); 

            card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
            card.setOnMouseExited(e -> card.setStyle(normalStyle));

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

            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(180, 130);
            clip.setArcWidth(15); 
            clip.setArcHeight(15);
            imgView.setClip(clip); 

            VBox imageContainer = new VBox(imgView);
            imageContainer.setAlignment(Pos.CENTER);
            imageContainer.setPrefHeight(130);

            Label nameLabel = new Label(p.getName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #2c3e50;");

            Label bidSubtitle = new Label("Current bid");
            bidSubtitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

            VBox priceTimeBox = new VBox(8); 
            priceTimeBox.setAlignment(Pos.CENTER);

            Label priceLabel = new Label(String.format("%,.0f VNĐ", p.getCurrentBid()));
            priceLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 16px;");

            Label timeLabel = new Label("⏱ " + p.getTimeRemaining());
            timeLabel.setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold; -fx-background-color: #ebf5fb; -fx-padding: 3px 8px; -fx-background-radius: 5px;");

            priceTimeBox.getChildren().addAll(priceLabel, timeLabel);

            Button btnBid = new Button("Bid Now");
            String btnNormal = "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5px; -fx-padding: 8px 15px;";
            String btnHover  = "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5px; -fx-padding: 8px 15px;";
            
            btnBid.setStyle(btnNormal);
            btnBid.setMaxWidth(Double.MAX_VALUE); 
            
            btnBid.setOnMouseEntered(e -> btnBid.setStyle(btnHover));
            btnBid.setOnMouseExited(e -> btnBid.setStyle(btnNormal));
            
            // ==============================================================
            // ĐÃ SỬA: CHUYỂN SANG PHÒNG ĐẤU GIÁ (Áp dụng Công thức Vàng)
            // ==============================================================
            btnBid.setOnAction(event -> {
                try {
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/AuctionRoom.fxml"));
                    javafx.scene.Parent auctionRoot = loader.load();
                    
                    AuctionRoomController roomController = loader.getController();
                    roomController.setProductData(p);
                    
                    javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                    
                    // Lột vỏ thay ruột
                    stage.getScene().setRoot(auctionRoot);
                    stage.setTitle("Auction Room - " + p.getName());
                    
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            });

            card.getChildren().addAll(imageContainer, nameLabel, bidSubtitle, priceTimeBox, btnBid);
            productContainer.getChildren().add(card);
        }
    }

    // ==============================================================
    // ĐÃ SỬA: HÀM XỬ LÝ ĐĂNG XUẤT (Áp dụng Công thức Vàng)
    // ==============================================================
    @FXML
    private void handleLogoutAction(javafx.event.ActionEvent event) {
        try {
            MockDatabase.registeredUsername = null;
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/Login.fxml"));
            javafx.scene.Parent loginRoot = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) productContainer.getScene().getWindow();
            
            // Lột vỏ thay ruột
            stage.getScene().setRoot(loginRoot);
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
            String username = MockDatabase.registeredUsername;
            String phone = MockDatabase.getUserPhone(username);
            
            btnInfo.setText("Tên TK: " + username + "\nSĐT: " + phone);
            btnInfo.setStyle("-fx-background-color: transparent; -fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-cursor: default;");
            
        } else {
            btnInfo.setText("Chưa đăng nhập!");
        }
    }

    // ==============================================================
    // ĐÃ SỬA: HÀM CHUYỂN SANG TRANG "ĐĂNG SẢN PHẨM" (Áp dụng Công thức Vàng)
    // ==============================================================
    @FXML
    private void handleGoToAddProduct(javafx.event.ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/AddProduct.fxml"));
            javafx.scene.Parent addProductRoot = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            
            // Lột vỏ thay ruột
            stage.getScene().setRoot(addProductRoot);
            stage.setTitle("Online Auction System - Add Product");
            
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}