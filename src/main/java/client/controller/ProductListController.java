package client.controller;

// Import đầy đủ các thư viện cần thiết
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox; 
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane; 
import javafx.geometry.Pos;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList; 
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import client.model.Product;

public class ProductListController {

    @FXML private Button btnInfo;
    @FXML private Label lblDateTime;
    @FXML private FlowPane productContainer;
    @FXML private HBox categoryBox;
    @FXML private Button btnSearch;
    @FXML private javafx.scene.control.TextField txtSearch; 
    
    @FXML private ComboBox<String> cbSort;
    
    private List<Product> currentDisplayedProducts;

    // ==============================================================
    // HÀM KHỞI TẠO CHÍNH
    // ==============================================================
    @FXML
    public void initialize() {
        
        // 1. Chạy đồng hồ
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a | dd MMM, yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            lblDateTime.setText(LocalDateTime.now().format(formatter)); 
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE); 
        clock.play(); 

        // 2. Cài đặt Hộp Sắp xếp
        if (cbSort != null) {
            cbSort.getItems().addAll("Mặc định", "Giá: Thấp đến Cao", "Giá: Cao đến Thấp");
            cbSort.setValue("Mặc định");
        }

        // 3. Tải toàn bộ sản phẩm
        currentDisplayedProducts = client.service.FirebaseService.getAllProducts();
        loadProductsToGrid(currentDisplayedProducts);

        // 4. Kích hoạt hiệu ứng Hover
        if (categoryBox != null) {
            for (javafx.scene.Node node : categoryBox.getChildren()) {
                if (node instanceof Button) {
                    applySmoothHover((Button) node); 
                }
            }
        }
        if (btnSearch != null) {
            applySmoothHover(btnSearch);
        }
    }

    // ==============================================================
    // XỬ LÝ SẮP XẾP
    // ==============================================================
    @FXML
    private void handleSortAction(javafx.event.ActionEvent event) {
        if (currentDisplayedProducts == null || currentDisplayedProducts.isEmpty()) {
            return;
        }

        String sortType = cbSort.getValue();
        List<Product> sortedList = new ArrayList<>(currentDisplayedProducts);

        if ("Giá: Thấp đến Cao".equals(sortType)) {
            sortedList.sort((p1, p2) -> Double.compare(p1.getCurrentBid(), p2.getCurrentBid()));
        } else if ("Giá: Cao đến Thấp".equals(sortType)) {
            sortedList.sort((p1, p2) -> Double.compare(p2.getCurrentBid(), p1.getCurrentBid()));
        } 

        loadProductsToGrid(sortedList);
        System.out.println("-> Đã sắp xếp lại lưới theo: " + sortType);
    }

    // ==============================================================
    // XỬ LÝ TÌM KIẾM
    // ==============================================================
    @FXML
    private void handleSearchAction(javafx.event.ActionEvent event) {
        String keyword = txtSearch.getText().trim(); 

        if (keyword.isEmpty()) {
            currentDisplayedProducts = client.service.FirebaseService.getAllProducts();
        } else {
            currentDisplayedProducts = client.service.FirebaseService.searchProducts(keyword);
        }

        cbSort.setValue("Mặc định");
        loadProductsToGrid(currentDisplayedProducts);
        System.out.println("Đã tìm: '" + keyword + "' - Trả về " + currentDisplayedProducts.size() + " kết quả.");
    }

    // ==============================================================
    // LỌC DANH MỤC
    // ==============================================================
    @FXML
    private void handleFilterCategory(javafx.event.ActionEvent event) {
        Button clickedButton = (Button) event.getSource(); 
        String categoryName = clickedButton.getText(); 
        
        currentDisplayedProducts = client.service.FirebaseService.getProductsByCategory(categoryName); 
        
        txtSearch.clear();
        cbSort.setValue("Mặc định");
        
        loadProductsToGrid(currentDisplayedProducts); 
    }

    // ==============================================================
    // HIỆU ỨNG HOVER
    // ==============================================================
    private void applySmoothHover(Button btn) {
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(150), btn);
        btn.setOnMouseEntered(e -> { 
            st.setToX(1.1); 
            st.setToY(1.1); 
            st.playFromStart();
        });
        btn.setOnMouseExited(e -> { 
            st.setToX(1.0); 
            st.setToY(1.0);
            st.playFromStart();
        });
    }

    // ==============================================================
    // TẢI SẢN PHẨM LÊN LƯỚI (ĐÃ BỌC THÉP CHỐNG LỖI NULL)
    // ==============================================================
    private void loadProductsToGrid(List<Product> products) {
        productContainer.getChildren().clear(); 

        if (products == null || products.isEmpty()) {
            productContainer.setAlignment(Pos.CENTER); 
            VBox emptyStateBox = new VBox(15);
            emptyStateBox.setAlignment(Pos.CENTER);
            emptyStateBox.setPadding(new javafx.geometry.Insets(100, 0, 100, 0));

            Label iconLabel = new Label("📦");
            iconLabel.setStyle("-fx-font-size: 60px; -fx-text-fill: #95a5a6;");
            Label messageLabel = new Label("Rất tiếc, không tìm thấy sản phẩm nào!");
            messageLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #7f8c8d;");
            Label subMessageLabel = new Label("Vui lòng thử lại với từ khóa khác hoặc chọn danh mục khác.");
            subMessageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #95a5a6;");

            emptyStateBox.getChildren().addAll(iconLabel, messageLabel, subMessageLabel);
            productContainer.getChildren().add(emptyStateBox);
            return; 
        }

        productContainer.setAlignment(Pos.TOP_LEFT);

        for (Product p : products) {
            
            // --- 1. KHUNG THẺ CHÍNH ---
            VBox card = new VBox(10); 
            String normalStyle = "-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);";
            String hoverStyle  = "-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 15, 0, 0, 5); -fx-cursor: hand;";
            card.setStyle(normalStyle);
            card.setPrefWidth(220); 
            card.setAlignment(Pos.TOP_CENTER); 
            card.setOnMouseEntered(e -> card.setStyle(hoverStyle)); 
            card.setOnMouseExited(e -> card.setStyle(normalStyle)); 

            // --- 2. XỬ LÝ ẢNH (BỌC THÉP CHỐNG NULL) ---
            ImageView imgView = new ImageView();
            String imagePath = p.getImagePath();
            
            if (imagePath != null && !imagePath.trim().isEmpty()) {
                try {
                    Image img = new Image(imagePath, true); 
                    imgView.setImage(img);
                } catch (Exception ex) {
                    System.out.println("Lỗi tải ảnh cho: " + p.getName());
                }
            } else {
                System.out.println("Cảnh báo: Sản phẩm thiếu link ảnh -> " + p.getName());
                // Nếu muốn, bạn có thể set 1 cái ảnh mặc định (Placeholder) ở đây
            }
            
            imgView.setFitWidth(180); 
            imgView.setFitHeight(130);
            imgView.setPreserveRatio(true); 

            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(180, 130);
            clip.setArcWidth(15); 
            clip.setArcHeight(15);
            imgView.setClip(clip); 

            // --- 3. DÙNG STACKPANE & XỬ LÝ THỜI GIAN (BỌC THÉP CHỐNG NULL NPE) ---
            StackPane imageContainer = new StackPane(imgView);
            imageContainer.setPrefHeight(130);
            
            Label statusBadge = new Label();
            
            // Rút thời gian an toàn
            String rawTime = p.getTimeRemaining();
            String safeTime = "Không xác định"; 
            String timeLower = ""; // Biến dùng riêng cho logic phân loại
            
            if (rawTime != null && !rawTime.trim().isEmpty()) {
                safeTime = rawTime;
                timeLower = rawTime.toLowerCase();
            }

            // Phân loại trạng thái an toàn
            if (timeLower.isEmpty()) {
                statusBadge.setText("⚪ CHƯA RÕ");
                statusBadge.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-radius: 5px;");
            } else if (timeLower.equals("0") || timeLower.contains("đã kết thúc") || timeLower.contains("hết")) {
                statusBadge.setText("⚫ ĐÃ KẾT THÚC");
                statusBadge.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-radius: 5px;");
            } else if (!timeLower.contains("h") && !timeLower.contains("d")) {
                statusBadge.setText("🔴 SẮP KẾT THÚC");
                statusBadge.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-radius: 5px;");
            } else {
                statusBadge.setText("🟢 ĐANG ĐẤU GIÁ");
                statusBadge.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-radius: 5px;");
            }
            
            StackPane.setAlignment(statusBadge, Pos.TOP_LEFT);
            StackPane.setMargin(statusBadge, new javafx.geometry.Insets(5));
            imageContainer.getChildren().add(statusBadge);

            // --- 4. THÔNG TIN CHỮ BÊN DƯỚI ---
            String safeName = (p.getName() != null) ? p.getName() : "Sản phẩm ẩn danh";
            Label nameLabel = new Label(safeName);
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #2c3e50;");

            Label bidSubtitle = new Label("Current bid");
            bidSubtitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

            VBox priceTimeBox = new VBox(8); 
            priceTimeBox.setAlignment(Pos.CENTER);

            Label priceLabel = new Label(String.format("%,.0f VNĐ", p.getCurrentBid()));
            priceLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 16px;");

            // Hiển thị biến safeTime đã được lọc
            Label timeLabel = new Label("⏱ " + safeTime);
            timeLabel.setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold; -fx-background-color: #ebf5fb; -fx-padding: 3px 8px; -fx-background-radius: 5px;");

            priceTimeBox.getChildren().addAll(priceLabel, timeLabel); 

            // --- 5. NÚT VÀO PHÒNG ĐẤU ---
            Button btnBid = new Button("Bid Now");
            String btnNormal = "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5px; -fx-padding: 8px 15px;";
            String btnHover  = "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5px; -fx-padding: 8px 15px;";
            btnBid.setStyle(btnNormal);
            btnBid.setMaxWidth(Double.MAX_VALUE); 
            btnBid.setOnMouseEntered(e -> btnBid.setStyle(btnHover)); 
            btnBid.setOnMouseExited(e -> btnBid.setStyle(btnNormal)); 
            
            btnBid.setOnAction(event -> {
                try {
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/AuctionRoom.fxml"));
                    javafx.scene.Parent auctionRoot = loader.load();
                    AuctionRoomController roomController = loader.getController();
                    roomController.setProductData(p); 
                    javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                    stage.getScene().setRoot(auctionRoot); 
                    stage.setTitle("Auction Room - " + safeName);
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            });

            card.getChildren().addAll(imageContainer, nameLabel, bidSubtitle, priceTimeBox, btnBid);
            productContainer.getChildren().add(card);
        }
    }

    // ==============================================================
    // CÁC HÀM TIỆN ÍCH
    // ==============================================================
    @FXML
    private void handleLogoutAction(javafx.event.ActionEvent event) {
        try {
            client.service.FirebaseService.registeredUsername = null; 
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/Login.fxml"));
            javafx.scene.Parent loginRoot = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) productContainer.getScene().getWindow();
            stage.getScene().setRoot(loginRoot); 
            stage.setTitle("Online Auction System - Login");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowInfo(javafx.event.ActionEvent event) {
        if (client.service.FirebaseService.registeredUsername != null) {
            String username = client.service.FirebaseService.registeredUsername;
            String phone = client.service.FirebaseService.getUserPhone(username); 
            btnInfo.setText("Tên TK: " + username + "\nSĐT: " + phone);
            btnInfo.setStyle("-fx-background-color: transparent; -fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-cursor: default;");
        } else {
            btnInfo.setText("Chưa đăng nhập!");
        }
    }

    @FXML
    private void handleGoToAddProduct(javafx.event.ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/AddProduct.fxml"));
            javafx.scene.Parent addProductRoot = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(addProductRoot); 
            stage.setTitle("Online Auction System - Add Product");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}