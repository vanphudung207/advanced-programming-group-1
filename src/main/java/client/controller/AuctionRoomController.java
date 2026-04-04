package client.controller; // Khai báo thư mục chứa file controller này

import client.model.Product; // Nhúng class Product để lấy thông tin sản phẩm
import javafx.animation.KeyFrame; // Nhúng KeyFrame để tạo khung thời gian cho đồng hồ
import javafx.animation.Timeline; // Nhúng Timeline để tạo vòng lặp đếm ngược
import javafx.event.ActionEvent; // Nhúng ActionEvent để bắt sự kiện click nút
import javafx.fxml.FXML; // Nhúng FXML để liên kết với giao diện Scene Builder
import javafx.scene.control.Button; // Nhúng class Button để điều khiển nút bấm
import javafx.scene.control.Label; // Nhúng class Label để điều khiển các dòng chữ
import javafx.scene.control.ListView; // Nhúng ListView để điều khiển danh sách lịch sử
import javafx.scene.control.TextField; // Nhúng TextField để lấy dữ liệu từ ô nhập giá
import javafx.util.Duration; // Nhúng Duration để cài đặt thời gian cho đồng hồ (giây)
import javafx.scene.Node; // Import Node để lấy thành phần giao diện (cái nút bấm)
import javafx.scene.Parent; // Import Parent làm gốc chứa giao diện mới
import javafx.scene.Scene; // Import Scene để tạo khung cảnh mới
import javafx.stage.Stage; // Import Stage để điều khiển cửa sổ phần mềm
import javafx.fxml.FXMLLoader; // Import FXMLLoader để đọc file thiết kế FXML
import javafx.scene.image.Image; // Nhúng class Image để tải file ảnh
import javafx.scene.image.ImageView; // Nhúng class ImageView để hiển thị ảnh lên giao diện
import java.text.NumberFormat; // Nhúng thư viện định dạng số
import java.util.Locale; // Nhúng thư viện cấu hình quốc gia (Việt Nam)
import javafx.geometry.Pos; // Nhúng thư viện căn chỉnh vị trí
import javafx.scene.effect.DropShadow; // Nhúng hiệu ứng đổ bóng cho giao diện
import javafx.scene.layout.VBox; // Nhúng layout xếp dọc để vẽ Popup
import javafx.scene.paint.Color; // Nhúng bảng màu
import javafx.stage.Modality; // Nhúng thư viện khóa màn hình (buộc người dùng phải tương tác với popup)
import javafx.stage.StageStyle; // Nhúng thư viện xóa viền cửa sổ mặc định của Windows
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType; // Nhúng AlertType để chọn kiểu hộp thoại (Lỗi, Cảnh báo, Thông tin...)

public class AuctionRoomController { // Bắt đầu khai báo class Controller

    @FXML private ImageView imgProduct; 
    @FXML private Label lblProductName; 
    @FXML private Label lblCurrentPrice; 
    @FXML private Label lblTimer; 
    @FXML private TextField txtBidAmount; 
    @FXML private Button btnSubmitBid; 
    @FXML private ListView<String> listBidHistory; 
    
    // Khai báo các thành phần giao diện liên quan đến thông tin chi tiết
    @FXML private Label lblSellerName; // Nhãn hiển thị tên người đăng bán
    @FXML private Label lblProductDescription; // Nhãn hiển thị mô tả sản phẩm
    
    // MỚI THÊM: Khai báo biến điều khiển nút xem chi tiết để tạo hiệu ứng hover
    @FXML private Button btnShowDetails; 

    private Product currentProduct; 
    private int totalSeconds = 60; 
    private Timeline countdownTimer; 

    // =================================================================================
    // HÀM NHẬN DỮ LIỆU TỪ MÀN HÌNH DANH SÁCH CHUYỂN SANG
    // =================================================================================
    public void setProductData(Product product) { 
        this.currentProduct = product; 
        
        if (currentProduct != null) { 
            lblProductName.setText(currentProduct.getName()); 
            lblCurrentPrice.setText(String.format("%,.0f VNĐ", currentProduct.getCurrentBid()));

            // HIỂN THỊ THÔNG TIN NGƯỜI BÁN VÀ MÔ TẢ LÊN GIAO DIỆN
            if (lblSellerName != null) {
                lblSellerName.setText(currentProduct.getSellerUsername()); // Cập nhật tên người bán
            }
            
            if (lblProductDescription != null) {
                // TẠM THỜI ẨN GỌI HÀM getDescription() ĐỂ TRÁNH LỖI BUILD
                // Đợi khi nào team (người làm Product) thêm mô tả vào database thì bạn mở lại sau
                lblProductDescription.setText("Thông tin mô tả sẽ được cập nhật sớm..."); 
            }

            try {
                Image img = new Image(currentProduct.getImagePath(), true); 
                imgProduct.setImage(img); 
            } catch (Exception e) {
                System.out.println("Lỗi không thể tải ảnh: " + e.getMessage()); 
            }
            
            // =================================================================================
            // THUẬT TOÁN NHẬN DIỆN NGƯỜI MUA / NGƯỜI BÁN (Contextual Roles)
            // =================================================================================
            String currentUser = MockDatabase.registeredUsername; // Lấy tên người đang đăng nhập
            String sellerUser = currentProduct.getSellerUsername(); // Lấy tên người sở hữu món hàng
            
            // So sánh: Nếu đang đăng nhập và tên trùng với tên chủ hàng
            if (currentUser != null && currentUser.equals(sellerUser)) {
                listBidHistory.getItems().add("Hệ thống: Bạn là chủ sở hữu của món hàng này.");
                
                // Khóa quyền đấu giá
                btnSubmitBid.setDisable(true); 
                txtBidAmount.setDisable(true); 
                txtBidAmount.setPromptText("Bạn không thể tự đấu giá hàng của mình");
                
            } else {
                // Nếu là người mua bình thường
                listBidHistory.getItems().add("Hệ thống: Bắt đầu phiên đấu giá!"); 
            }
            // =================================================================================

            startCountdownTimer(); 
        } 
    } 

    // =================================================================================
    // HÀM XỬ LÝ ĐỒNG HỒ ĐẾM NGƯỢC
    // =================================================================================
    private void startCountdownTimer() { 

        updateTimerDisplay(); 
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> { 
            totalSeconds--; 
            updateTimerDisplay(); 
            
            if (totalSeconds <= 0) { 
                countdownTimer.stop(); 
                lblTimer.setText("Phiên đấu giá đã đóng!"); 
                btnSubmitBid.setDisable(true); 
                txtBidAmount.setDisable(true); 
                
                double winningPrice = currentProduct.getCurrentBid(); 
                String formattedVND = formatVND(winningPrice);
                
                // MOCK DATA: Tạo một cái tên giả lập chờ Thành viên 1 & 2 ghép code Server vào
                // Note cho team: Chỗ này ae lấy tên user thắng cuộc từ DB / Socket truyền vào biến này nhé!
                String mockWinnerName = "Người chơi hệ VIP"; 
                
                listBidHistory.getItems().add(0, "Hệ thống: " + mockWinnerName + " đã thắng với giá " + formattedVND); 
                
                // Truyền cả 3 thông tin: Tên SP, Giá tiền, và Tên người thắng vào Popup
                showWinnerPopup(currentProduct.getName(), formattedVND, mockWinnerName);
            }
        })); 
        
        countdownTimer.setCycleCount(Timeline.INDEFINITE); 
        countdownTimer.play(); 
    } 

    // =================================================================================
    // HÀM PHỤ TRỢ: TÍNH TOÁN & HIỂN THỊ THỜI GIAN
    // =================================================================================
    private void updateTimerDisplay() {
        int hours = totalSeconds / 3600; 
        int minutes = (totalSeconds % 3600) / 60; 
        int seconds = totalSeconds % 60; 
        
        lblTimer.setText(String.format("⏱ Còn lại: %02d:%02d:%02d", hours, minutes, seconds)); 
    }

    // =================================================================================
    // HÀM XỬ LÝ KHI NGƯỜI DÙNG BẤM NÚT "GỬI GIÁ BÁN" (Đã thêm Anti-sniping)
    // =================================================================================
    @FXML private void handleBidAction(ActionEvent event) { 
        String inputStr = txtBidAmount.getText(); 
        
        try { 
            double bidAmount = Double.parseDouble(inputStr); 
            double currentHighestBid = currentProduct.getCurrentBid(); 
            
            if (bidAmount <= currentHighestBid) { 
                // Đổi cảnh báo $ thành VNĐ
                listBidHistory.getItems().add(0, "Lỗi: Bạn phải trả giá cao hơn " + String.format("%,.0f VNĐ", currentHighestBid)); 
                return; 
            } 
            
            currentProduct = new Product(currentProduct.getId(), currentProduct.getName(), bidAmount, currentProduct.getTimeRemaining(), currentProduct.getImagePath(), currentProduct.getSellerUsername()); 
            
            // Đổi text cập nhật giá thành VNĐ
            lblCurrentPrice.setText(String.format("%,.0f VNĐ", bidAmount)); 
            listBidHistory.getItems().add(0, "Bạn trả giá: " + String.format("%,.0f VNĐ", bidAmount)); 
            txtBidAmount.clear(); 
            
            // =================================================================================
            // TÍNH NĂNG NÂNG CAO: ANTI-SNIPING (+1.5đ)
            // =================================================================================
            // Nếu người dùng trả giá hợp lệ khi thời gian còn lại từ 1 đến 10 giây
            if (totalSeconds > 0 && totalSeconds <= 10) { 
                totalSeconds += 10; // Cộng thêm 10 giây vào tổng thời gian còn lại
                listBidHistory.getItems().add(0, "🔥 Anti-sniping kích hoạt: Thời gian được cộng thêm 10s!"); // Thông báo lên lịch sử
                updateTimerDisplay(); // Cập nhật lại nhãn đồng hồ hiển thị ngay lập tức
            }
            // =================================================================================
            
        } catch (NumberFormatException e) { 
            listBidHistory.getItems().add(0, "Lỗi: Vui lòng nhập số tiền hợp lệ!"); 
        } 
    } 

    // =================================================================================
    // HÀM XỬ LÝ KHI BẤM NÚT "QUAY LẠI TRANG CHỦ"
    // =================================================================================
    @FXML 
    private void handleGoBack(ActionEvent event) { 
        try { 
            
            if (countdownTimer != null) { 
                countdownTimer.stop(); 
            } 
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/ProductList.fxml")); 
            Parent root = loader.load(); 
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); 
            stage.getScene().setRoot(root); 
            stage.setTitle("Online Auction System - Danh sách sản phẩm");
            
        } catch (Exception e) { 
            e.printStackTrace(); 
        } 
    } 
    // =================================================================================
    // HÀM PHỤ TRỢ: CHUYỂN ĐỔI SỐ THỰC SANG TIỀN TỆ VNĐ
    // =================================================================================
    private String formatVND(double amount) {
        Locale vietnamLocale = new Locale("vi", "VN"); // Cài đặt vùng quốc gia là Việt Nam
        NumberFormat vnFormat = NumberFormat.getCurrencyInstance(vietnamLocale); // Lấy bộ formater tiền tệ chuẩn của VN
        return vnFormat.format(amount); // Trả về chuỗi đã được định dạng đẹp mắt
    }

    // =================================================================================
    // HÀM PHỤ TRỢ: VẼ CỬA SỔ POPUP VINH DANH NGƯỜI CHIẾN THẮNG
    // =================================================================================
    private void showWinnerPopup(String productName, String formattedPrice, String winnerName) { // Bổ sung thêm biến winnerName
        Stage popupStage = new Stage(); 
        popupStage.initModality(Modality.APPLICATION_MODAL); 
        popupStage.initStyle(StageStyle.TRANSPARENT); 

        VBox popupBox = new VBox(15); // Đổi khoảng cách các dòng lại cho vừa vặn hơn
        popupBox.setAlignment(Pos.CENTER); 
        // Đổi viền sang màu vàng gold cho đúng chất "Vô địch"
        popupBox.setStyle("-fx-background-color: white; -fx-padding: 40; -fx-background-radius: 15; -fx-border-radius: 15; -fx-border-color: #f1c40f; -fx-border-width: 4;");

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.25)); 
        popupBox.setEffect(shadow);

        Label lblTitle = new Label("🏆 KẾT THÚC PHIÊN ĐẤU GIÁ!");
        lblTitle.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #f39c12;"); 

        Label lblProduct = new Label("Sản phẩm: " + productName);
        lblProduct.setStyle("-fx-font-size: 18px; -fx-text-fill: #34495e;"); 

        // Thêm nhãn xướng tên người chiến thắng
        Label lblWinner = new Label("Người chiến thắng: " + winnerName);
        lblWinner.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #27ae60;"); // Màu xanh lá cây nổi bật

        Label lblPrice = new Label("Với mức giá: " + formattedPrice);
        lblPrice.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;"); 

        Button btnClose = new Button("XÁC NHẬN");
        btnClose.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 30; -fx-background-radius: 5; -fx-cursor: hand;");
        btnClose.setOnAction(e -> popupStage.close()); 

        // Ráp thêm lblWinner vào danh sách hiển thị
        popupBox.getChildren().addAll(lblTitle, lblProduct, lblWinner, lblPrice, btnClose);

        Scene scene = new Scene(popupBox);
        scene.setFill(Color.TRANSPARENT); 
        popupStage.setScene(scene);
        
        popupStage.centerOnScreen();
        popupStage.showAndWait();
    }

    // =================================================================================
    // HÀM XỬ LÝ KHI BẤM NÚT "XEM ĐẦY ĐỦ CHI TIẾT"
    // =================================================================================
    @FXML 
    private void handleShowDetails(ActionEvent event) { 
        // 1. Tạo một cửa sổ (Stage) mới để làm Popup
        Stage detailStage = new Stage(); 
        
        // Lệnh này bắt buộc người dùng phải đóng popup này thì mới bấm lại được vào màn hình chính
        detailStage.initModality(Modality.APPLICATION_MODAL); 
        detailStage.setTitle("Thông tin chi tiết sản phẩm"); // Đặt tiêu đề cho cửa sổ nhỏ

        // 2. Tạo bố cục (Layout) cho Popup
        VBox layout = new VBox(15); // Tạo hộp xếp dọc, khoảng cách các dòng là 15px
        layout.setStyle("-fx-padding: 25; -fx-background-color: #ffffff;"); // Nền trắng, lề 25px

        // 3. Tạo các thành phần chữ (Label) để nhét vào Popup
        Label lblTitle = new Label("CHI TIẾT SẢN PHẨM");
        lblTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Lấy tên sản phẩm từ biến currentProduct (nếu có)
        String pName = (currentProduct != null) ? currentProduct.getName() : "Không có dữ liệu";
        Label lblName = new Label("Tên sản phẩm: " + pName);
        lblName.setStyle("-fx-font-size: 16px; -fx-text-fill: #34495e; -fx-font-weight: bold;");

        // Lấy tên người bán
        String pSeller = (currentProduct != null) ? currentProduct.getSellerUsername() : "Không có dữ liệu";
        Label lblSeller = new Label("Người đăng bán: " + pSeller);
        lblSeller.setStyle("-fx-font-size: 16px; -fx-text-fill: #e67e22;"); // Tên người bán màu cam

        // Tạm thời để text cứng cho mô tả vì team chưa làm hàm getDescription()
        Label lblDesc = new Label("Mô tả đầy đủ:\nHiện tại người bán chưa cung cấp thêm thông tin chi tiết nào khác cho sản phẩm này.");
        lblDesc.setStyle("-fx-font-size: 15px; -fx-text-fill: #7f8c8d; -fx-line-spacing: 5px;");
        lblDesc.setWrapText(true); // Tự động xuống dòng nếu văn bản dài

        // 4. Tạo nút "Đóng"
        Button btnClose = new Button("ĐÓNG");
        btnClose.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand;");
        btnClose.setOnAction(e -> detailStage.close()); // Khi bấm nút này thì đóng cửa sổ (Stage) hiện tại lại

        // 5. Gom tất cả các thành phần trên nhét vào layout
        layout.getChildren().addAll(lblTitle, lblName, lblSeller, lblDesc, btnClose);
        layout.setAlignment(Pos.TOP_LEFT); // Căn lề trên bên trái

        // 6. Hiển thị Popup
        Scene scene = new Scene(layout, 450, 350); // Thiết lập kích thước cửa sổ popup là 450x350
        detailStage.setScene(scene);
        detailStage.centerOnScreen(); // Cho cửa sổ bật ra ở chính giữa màn hình
        detailStage.showAndWait(); // Hiển thị màn hình xem chi tiết
    }
    
    // =================================================================================
    // HÀM TẠO HIỆU ỨNG DI CHUỘT (HOVER) CHO NÚT "XEM CHI TIẾT"
    // =================================================================================
    @FXML 
    private void handleMouseEnter() { 
        // Khi chuột lướt vào: Nền chuyển xanh đậm hơn (#2980b9) và thêm hiệu ứng bóng đổ tỏa ra (dropshadow)
        if (btnShowDetails != null) {
            btnShowDetails.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(41, 128, 185, 0.8), 15, 0, 0, 5);");
        }
    }

    @FXML 
    private void handleMouseExit() { 
        // Khi chuột rời đi: Trả lại màu nền mặc định ban đầu (#3498db) và bỏ bóng đổ
        if (btnShowDetails != null) {
            btnShowDetails.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px; -fx-cursor: hand;");
        }
    }
}