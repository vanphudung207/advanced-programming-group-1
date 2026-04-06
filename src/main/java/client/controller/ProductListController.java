package client.controller;

// Import đầy đủ các thư viện
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane; // MỚI THÊM: Dùng để xếp chồng Badge lên Ảnh
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

    @FXML private Button btnInfo;
    @FXML private Label lblDateTime;
    @FXML private FlowPane productContainer;
    
    // Khai báo để làm hiệu ứng Hover
    @FXML private HBox categoryBox;
    @FXML private Button btnSearch;

    // ==============================================================
    // HÀM KHỞI TẠO CHÍNH
    // ==============================================================
    @FXML
    public void initialize() {
        
        // 1. Chạy đồng hồ thời gian thực góc trái màn hình
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a | dd MMM, yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            lblDateTime.setText(LocalDateTime.now().format(formatter)); // Cập nhật chữ mỗi giây
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE); // Cho vòng lặp chạy vô hạn
        clock.play(); 

        // 2. Tải toàn bộ sản phẩm lên lưới
        loadProductsToGrid(MockDatabase.getAllProducts());

        // 3. Kích hoạt hiệu ứng Hover (Phóng to nút) cho thanh Danh mục
        if (categoryBox != null) {
            for (javafx.scene.Node node : categoryBox.getChildren()) {
                if (node instanceof Button) {
                    applySmoothHover((Button) node); // Gắn phép thuật phóng to cho từng nút
                }
            }
        }
        // Gắn luôn hiệu ứng Hover cho nút Tìm kiếm
        if (btnSearch != null) {
            applySmoothHover(btnSearch);
        }
    }

    // ==============================================================
    // HÀM HIỆU ỨNG: PHÓNG TO NÚT KHI DI CHUỘT
    // ==============================================================
    private void applySmoothHover(Button btn) {
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(150), btn);
        btn.setOnMouseEntered(e -> { // Di chuột vào
            st.setToX(1.1); // Phóng to chiều ngang 10%
            st.setToY(1.1); // Phóng to chiều dọc 10%
            st.playFromStart();
        });
        btn.setOnMouseExited(e -> { // Rút chuột ra
            st.setToX(1.0); // Trả về tỷ lệ 1:1 ban đầu
            st.setToY(1.0);
            st.playFromStart();
        });
    }

    // ==============================================================
    // HÀM LỌC SẢN PHẨM THEO TỪNG NÚT BẤM (ĐIỆN TỬ, GIA DỤNG...)
    // ==============================================================
    @FXML
    private void handleFilterCategory(javafx.event.ActionEvent event) {
        Button clickedButton = (Button) event.getSource(); // Chộp lấy cái nút vừa bấm
        String categoryName = clickedButton.getText(); // Rút chữ trên nút ra đọc
        List<Product> filteredProducts = MockDatabase.getProductsByCategory(categoryName); // Hỏi CSDL
        loadProductsToGrid(filteredProducts); // Ném danh sách mới vào hàm vẽ
    }

    // ==============================================================
    // HÀM TẢI VÀ VẼ DANH SÁCH SẢN PHẨM LÊN LƯỚI (ĐÃ CÓ BADGE TRẠNG THÁI)
    // ==============================================================
    private void loadProductsToGrid(List<Product> products) {
        productContainer.getChildren().clear(); // Dọn sạch bàn cũ trước khi bày cỗ mới
        
        for (Product p : products) {
            
            // --- 1. KHUNG THẺ CHÍNH ---
            VBox card = new VBox(10); 
            String normalStyle = "-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);";
            String hoverStyle  = "-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 15, 0, 0, 5); -fx-cursor: hand;";
            card.setStyle(normalStyle);
            card.setPrefWidth(220); 
            card.setAlignment(Pos.TOP_CENTER); 
            card.setOnMouseEntered(e -> card.setStyle(hoverStyle)); // Bóng nổi lên khi di chuột
            card.setOnMouseExited(e -> card.setStyle(normalStyle)); // Bóng xẹp xuống

            // --- 2. XỬ LÝ ẢNH ---
            ImageView imgView = new ImageView();
            try {
                Image img = new Image(p.getImagePath(), true); // Bắn link web tải ảnh
                imgView.setImage(img);
            } catch (Exception ex) {
                System.out.println("Lỗi tải ảnh cho: " + p.getName());
            }
            imgView.setFitWidth(180); 
            imgView.setFitHeight(130);
            imgView.setPreserveRatio(true); // Giữ tỷ lệ ảnh không bị méo

            // Cắt góc ảnh cho bo tròn
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(180, 130);
            clip.setArcWidth(15); 
            clip.setArcHeight(15);
            imgView.setClip(clip); 

            // --- 3. ĐÃ SỬA: DÙNG STACKPANE ĐỂ XẾP CHỒNG NHÃN LÊN ẢNH ---
            StackPane imageContainer = new StackPane(imgView);
            imageContainer.setPrefHeight(130);
            
            // MỚI: Khởi tạo Nhãn dán Trạng thái (Badge)
            Label statusBadge = new Label();
            String time = p.getTimeRemaining().toLowerCase(); // Lấy chữ thời gian và đổi hết thành chữ thường để dễ soi
            
            // THUẬT TOÁN ĐỊNH VỊ TRẠNG THÁI:
            if (time.equals("0") || time.contains("kết thúc") || time.contains("hết")) {
                // Nếu hết giờ
                statusBadge.setText("⚫ ĐÃ KẾT THÚC");
                statusBadge.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-radius: 5px;");
            } else if (!time.contains("h") && !time.contains("d")) {
                // Nếu không có chữ 'h' (giờ) và 'd' (ngày) -> Tức là chỉ còn tính bằng phút (Ví dụ: 45m)
                statusBadge.setText("🔴 SẮP KẾT THÚC");
                statusBadge.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-radius: 5px;");
            } else {
                // Còn lại là dư dả thời gian
                statusBadge.setText("🟢 ĐANG ĐẤU GIÁ");
                statusBadge.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-radius: 5px;");
            }
            
            // Ép cái nhãn dán bay lên góc trên bên trái của khung ảnh
            StackPane.setAlignment(statusBadge, Pos.TOP_LEFT);
            // Đẩy nhãn lùi vào trong 5px để không bị dính sát lề ảnh
            StackPane.setMargin(statusBadge, new javafx.geometry.Insets(5));
            // Cuối cùng, nhét cái nhãn dán đè lên trên tấm ảnh
            imageContainer.getChildren().add(statusBadge);

            // --- 4. THÔNG TIN CHỮ BÊN DƯỚI ---
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

            priceTimeBox.getChildren().addAll(priceLabel, timeLabel); // Ném giá và giờ vào cột

            // --- 5. NÚT VÀO PHÒNG ĐẤU ---
            Button btnBid = new Button("Bid Now");
            String btnNormal = "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5px; -fx-padding: 8px 15px;";
            String btnHover  = "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5px; -fx-padding: 8px 15px;";
            btnBid.setStyle(btnNormal);
            btnBid.setMaxWidth(Double.MAX_VALUE); // Bắt nút căng ngang 100%
            btnBid.setOnMouseEntered(e -> btnBid.setStyle(btnHover)); // Sáng lên khi chuột vào
            btnBid.setOnMouseExited(e -> btnBid.setStyle(btnNormal)); // Tối đi khi chuột ra
            
            // Xử lý chuyển trang Thay ruột lột vỏ sang phòng đấu giá
            btnBid.setOnAction(event -> {
                try {
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/AuctionRoom.fxml"));
                    javafx.scene.Parent auctionRoot = loader.load();
                    AuctionRoomController roomController = loader.getController();
                    roomController.setProductData(p); // Truyền dữ liệu hàng hóa sang phòng đấu
                    javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                    stage.getScene().setRoot(auctionRoot); // Thay ruột
                    stage.setTitle("Auction Room - " + p.getName());
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            });

            // Gói ghém tất cả Ảnh, Tên, Giá, Nút vào trong Thẻ
            card.getChildren().addAll(imageContainer, nameLabel, bidSubtitle, priceTimeBox, btnBid);
            
            // Phục vụ món ăn lên Lưới màn hình
            productContainer.getChildren().add(card);
        }
    }

    // ==============================================================
    // HÀM XỬ LÝ ĐĂNG XUẤT
    // ==============================================================
    @FXML
    private void handleLogoutAction(javafx.event.ActionEvent event) {
        try {
            MockDatabase.registeredUsername = null; // Xóa trí nhớ hệ thống về user hiện tại
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/Login.fxml"));
            javafx.scene.Parent loginRoot = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) productContainer.getScene().getWindow();
            stage.getScene().setRoot(loginRoot); // Bay thẳng về trang Đăng nhập
            stage.setTitle("Online Auction System - Login");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // ==============================================================
    // HÀM XỬ LÝ HIỆN THÔNG TIN CÁ NHÂN
    // ==============================================================
    @FXML
    private void handleShowInfo(javafx.event.ActionEvent event) {
        if (MockDatabase.registeredUsername != null) {
            String username = MockDatabase.registeredUsername;
            String phone = MockDatabase.getUserPhone(username); // Xin Số điện thoại từ DB
            // Đổi chữ trên nút để in thông tin ra
            btnInfo.setText("Tên TK: " + username + "\nSĐT: " + phone);
            btnInfo.setStyle("-fx-background-color: transparent; -fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-cursor: default;");
        } else {
            btnInfo.setText("Chưa đăng nhập!");
        }
    }

    // ==============================================================
    // HÀM CHUYỂN TRANG ĐĂNG BÁN SẢN PHẨM
    // ==============================================================
    @FXML
    private void handleGoToAddProduct(javafx.event.ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/AddProduct.fxml"));
            javafx.scene.Parent addProductRoot = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(addProductRoot); // Lột vỏ thay ruột
            stage.setTitle("Online Auction System - Add Product");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}