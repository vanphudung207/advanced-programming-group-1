package client.controller;

// Import đầy đủ các thư viện cần thiết
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox; // Dùng cho hộp Sắp xếp
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane; 
import javafx.geometry.Pos;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList; // Dùng để tạo mảng sao chép
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
    
    // ĐÃ THÊM: Biến nối với Hộp chọn sắp xếp trên giao diện
    @FXML private ComboBox<String> cbSort;
    
    // ĐÃ THÊM: Biến "Trí nhớ ngắn hạn" lưu giữ danh sách sản phẩm ĐANG HIỂN THỊ trên màn hình để đem đi sắp xếp
    private List<Product> currentDisplayedProducts;

    // ==============================================================
    // HÀM KHỞI TẠO CHÍNH (Chạy đầu tiên khi mở app)
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

        // 2. Cài đặt các tùy chọn cho Hộp Sắp xếp (ComboBox)
        if (cbSort != null) {
            // Nạp 3 chế độ vào hộp thả xuống
            cbSort.getItems().addAll("Mặc định", "Giá: Thấp đến Cao", "Giá: Cao đến Thấp");
            // Set giá trị lúc mới bật app lên là Mặc định
            cbSort.setValue("Mặc định");
        }

        // 3. Tải toàn bộ sản phẩm từ DB lên và lưu vào Trí nhớ ngắn hạn
        currentDisplayedProducts = MockDatabase.getAllProducts();
        // Gọi hàm vẽ lưới để hiển thị danh sách đó ra màn hình
        loadProductsToGrid(currentDisplayedProducts);

        // 4. Kích hoạt hiệu ứng Hover (Phóng to nút) cho thanh Danh mục
        if (categoryBox != null) {
            // Quét qua mọi thành phần trong HBox
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
    // HÀM XỬ LÝ KHI CHỌN CHẾ ĐỘ SẮP XẾP TỪ COMBOBOX
    // ==============================================================
    @FXML
    private void handleSortAction(javafx.event.ActionEvent event) {
        // Nếu danh sách hiện tại đang trống (chưa có hàng) thì không cần sắp xếp, thoát hàm luôn
        if (currentDisplayedProducts == null || currentDisplayedProducts.isEmpty()) {
            return;
        }

        // Đọc xem người dùng vừa chọn chế độ gì (Mặc định hay Cao-Thấp...)
        String sortType = cbSort.getValue();
        
        // Tạo một bản sao chép của danh sách hiện tại để tránh làm hỏng dữ liệu gốc
        List<Product> sortedList = new ArrayList<>(currentDisplayedProducts);

        // Chạy thuật toán sắp xếp dựa trên lựa chọn
        if ("Giá: Thấp đến Cao".equals(sortType)) {
            // Lệnh sort() sẽ đảo vị trí: So sánh Giá hiện tại của p1 và p2, xếp tăng dần
            sortedList.sort((p1, p2) -> Double.compare(p1.getCurrentBid(), p2.getCurrentBid()));
        } else if ("Giá: Cao đến Thấp".equals(sortType)) {
            // Lệnh sort() đảo vị trí: Đảo ngược lại p2 với p1 để xếp giảm dần
            sortedList.sort((p1, p2) -> Double.compare(p2.getCurrentBid(), p1.getCurrentBid()));
        } 
        // (Nếu là "Mặc định" thì bỏ qua khối if này, giữ nguyên mảng cũ)

        // Gọi hàm xóa lưới cũ và vẽ lại lưới mới với danh sách đã được sắp xếp
        loadProductsToGrid(sortedList);
        System.out.println("-> Đã sắp xếp lại lưới theo: " + sortType);
    }

    // ==============================================================
    // HÀM XỬ LÝ KHI NGƯỜI DÙNG GÕ CHỮ VÀ BẤM TÌM KIẾM
    // ==============================================================
    @FXML
    private void handleSearchAction(javafx.event.ActionEvent event) {
        // Lấy dòng chữ người dùng vừa gõ, dùng hàm trim() để cắt bỏ dấu cách thừa ở đầu/cuối
        String keyword = txtSearch.getText().trim(); 

        // Nếu người dùng không gõ gì mà cứ bấm nút tìm
        if (keyword.isEmpty()) {
            // Lấy lại toàn bộ kho hàng gốc
            currentDisplayedProducts = MockDatabase.getAllProducts();
        } else {
            // Gọi điện nhờ Database giả lập đi tìm hàng giúp
            currentDisplayedProducts = MockDatabase.searchProducts(keyword);
        }

        // Reset hộp Sắp xếp về "Mặc định" để tránh lỗi logic khi lọc ra danh sách mới
        cbSort.setValue("Mặc định");
        // Vẽ danh sách mới lên lưới
        loadProductsToGrid(currentDisplayedProducts);
        System.out.println("Đã tìm: '" + keyword + "' - Trả về " + currentDisplayedProducts.size() + " kết quả.");
    }

    // ==============================================================
    // HÀM LỌC SẢN PHẨM THEO TỪNG NÚT BẤM (ĐIỆN TỬ, GIA DỤNG...)
    // ==============================================================
    @FXML
    private void handleFilterCategory(javafx.event.ActionEvent event) {
        // Chộp lấy cái nút vừa bấm
        Button clickedButton = (Button) event.getSource(); 
        // Rút chữ trên nút ra đọc
        String categoryName = clickedButton.getText(); 
        
        // Hỏi CSDL lấy danh sách theo loại, rồi lưu vào Trí nhớ ngắn hạn
        currentDisplayedProducts = MockDatabase.getProductsByCategory(categoryName); 
        
        // Cập nhật lại thanh tìm kiếm cho trống và Reset sắp xếp
        txtSearch.clear();
        cbSort.setValue("Mặc định");
        
        // Ném danh sách mới vào hàm vẽ
        loadProductsToGrid(currentDisplayedProducts); 
    }

    // ==============================================================
    // HÀM HIỆU ỨNG: PHÓNG TO NÚT KHI DI CHUỘT
    // ==============================================================
    private void applySmoothHover(Button btn) {
        // Tạo bộ máy chuyển động phóng to trong 0.15 giây
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
    // HÀM TẢI VÀ VẼ DANH SÁCH SẢN PHẨM LÊN LƯỚI (BAO GỒM EMPTY STATE & TRẠNG THÁI)
    // ==============================================================
    private void loadProductsToGrid(List<Product> products) {
        // Dọn sạch bàn cũ trước khi bày cỗ mới
        productContainer.getChildren().clear(); 

        // -- XỬ LÝ TRẠNG THÁI TRỐNG (EMPTY STATE) --
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
            return; // Thoát hàm luôn
        }

        // -- NẾU CÓ SẢN PHẨM, TRẢ LẠI CĂN CHỈNH MẶC ĐỊNH --
        productContainer.setAlignment(Pos.TOP_LEFT);

        // Lấy từng sản phẩm chạy vào vòng lặp để đóng gói thành Thẻ
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

            // --- 2. XỬ LÝ ẢNH ---
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

            // Cắt góc ảnh cho bo tròn
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(180, 130);
            clip.setArcWidth(15); 
            clip.setArcHeight(15);
            imgView.setClip(clip); 

            // --- 3. DÙNG STACKPANE ĐỂ XẾP CHỒNG NHÃN LÊN ẢNH ---
            StackPane imageContainer = new StackPane(imgView);
            imageContainer.setPrefHeight(130);
            
            Label statusBadge = new Label();
            String time = p.getTimeRemaining().toLowerCase(); 
            
            // Định vị Nhãn Trạng Thái
            if (time.equals("0") || time.contains("kết thúc") || time.contains("hết")) {
                statusBadge.setText("⚫ ĐÃ KẾT THÚC");
                statusBadge.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-radius: 5px;");
            } else if (!time.contains("h") && !time.contains("d")) {
                statusBadge.setText("🔴 SẮP KẾT THÚC");
                statusBadge.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-radius: 5px;");
            } else {
                statusBadge.setText("🟢 ĐANG ĐẤU GIÁ");
                statusBadge.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-radius: 5px;");
            }
            
            // Ép dính góc ảnh
            StackPane.setAlignment(statusBadge, Pos.TOP_LEFT);
            StackPane.setMargin(statusBadge, new javafx.geometry.Insets(5));
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

            priceTimeBox.getChildren().addAll(priceLabel, timeLabel); 

            // --- 5. NÚT VÀO PHÒNG ĐẤU ---
            Button btnBid = new Button("Bid Now");
            String btnNormal = "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5px; -fx-padding: 8px 15px;";
            String btnHover  = "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5px; -fx-padding: 8px 15px;";
            btnBid.setStyle(btnNormal);
            btnBid.setMaxWidth(Double.MAX_VALUE); 
            btnBid.setOnMouseEntered(e -> btnBid.setStyle(btnHover)); 
            btnBid.setOnMouseExited(e -> btnBid.setStyle(btnNormal)); 
            
            // Chuyển trang lột vỏ
            btnBid.setOnAction(event -> {
                try {
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/AuctionRoom.fxml"));
                    javafx.scene.Parent auctionRoot = loader.load();
                    AuctionRoomController roomController = loader.getController();
                    roomController.setProductData(p); 
                    javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                    stage.getScene().setRoot(auctionRoot); 
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
    // CÁC HÀM TIỆN ÍCH (Đăng xuất, Hiển thị thông tin, Đăng SP)
    // ==============================================================
    @FXML
    private void handleLogoutAction(javafx.event.ActionEvent event) {
        try {
            MockDatabase.registeredUsername = null; 
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
        if (MockDatabase.registeredUsername != null) {
            String username = MockDatabase.registeredUsername;
            String phone = MockDatabase.getUserPhone(username); 
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