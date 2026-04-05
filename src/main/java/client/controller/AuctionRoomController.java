package client.controller; // Khai báo thư mục chứa file controller này

import client.model.Product; // Nhúng class Product để lấy thông tin sản phẩm
import javafx.animation.KeyFrame; // Nhúng KeyFrame để tạo khung thời gian cho đồng hồ
import javafx.animation.Timeline; // Nhúng Timeline để tạo vòng lặp đếm ngược
import javafx.application.Platform; // Xử lý an toàn các Popup để tránh kẹt luồng
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
import javafx.stage.Modality; // Nhúng thư viện khóa màn hình
import javafx.stage.StageStyle; // Nhúng thư viện xóa viền cửa sổ mặc định

public class AuctionRoomController { // Bắt đầu khai báo class Controller

    @FXML private ImageView imgProduct; 
    @FXML private Label lblProductName; 
    @FXML private Label lblCurrentPrice; 
    @FXML private Label lblTimer; 
    @FXML private TextField txtBidAmount; 
    @FXML private Button btnSubmitBid; 
    @FXML private ListView<String> listBidHistory; 
    
    @FXML private Label lblSellerName; // Nhãn hiển thị tên người đăng bán
    @FXML private Label lblProductDescription; // Nhãn hiển thị mô tả sản phẩm
    @FXML private Button btnShowDetails; // Biến điều khiển nút xem chi tiết

    private Product currentProduct; 
    private int totalSeconds = 60; 
    private Timeline countdownTimer; 
    
    // Biến cờ đánh dấu xem đã có ai tham gia trả giá chưa (Mặc định luôn là CHƯA CÓ)
    private boolean hasBids = false; 

    // =================================================================================
    // HÀM NHẬN DỮ LIỆU TỪ MÀN HÌNH DANH SÁCH CHUYỂN SANG
    // =================================================================================
    public void setProductData(Product product) { 
        this.currentProduct = product; 
        this.hasBids = false; // Đảm bảo mỗi khi load sản phẩm mới, cờ luôn được reset về false
        
        if (currentProduct != null) { 
            lblProductName.setText(currentProduct.getName()); 
            lblCurrentPrice.setText(String.format("%,.0f VNĐ", currentProduct.getCurrentBid()));

            if (lblSellerName != null) {
                lblSellerName.setText(currentProduct.getSellerUsername());
            }
            if (lblProductDescription != null) {
                lblProductDescription.setText("Thông tin mô tả sẽ được cập nhật sớm..."); 
            }

            try {
                Image img = new Image(currentProduct.getImagePath(), true); 
                imgProduct.setImage(img); 
            } catch (Exception e) {
                System.out.println("Lỗi không thể tải ảnh: " + e.getMessage()); 
            }
            
            // XỬ LÝ PHÂN QUYỀN
            // Đổi chữ "nguoimua123" thành tên của bạn để test giao diện chủ hàng.
            String currentUser = "nguoimua123"; 
            
            String sellerUser = currentProduct.getSellerUsername(); 
            
            if (currentUser != null && currentUser.equals(sellerUser)) {
                listBidHistory.getItems().add("Hệ thống: Bạn là chủ sở hữu của món hàng này.");
                btnSubmitBid.setDisable(true); 
                txtBidAmount.setDisable(true); 
                txtBidAmount.setPromptText("Bạn không thể tự đấu giá hàng của mình");
            } else {
                listBidHistory.getItems().add("Hệ thống: Bắt đầu phiên đấu giá!"); 
            }

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
                endAuction(); // Hết giờ thì gọi hàm kết thúc
            }
        })); 
        
        countdownTimer.setCycleCount(Timeline.INDEFINITE); 
        countdownTimer.play(); 
    } 

    // =================================================================================
    // HÀM XỬ LÝ KẾT THÚC ĐẤU GIÁ
    // =================================================================================
    private void endAuction() {
        lblTimer.setText("Đã đóng!"); 
        txtBidAmount.setDisable(true); 
        btnSubmitBid.setDisable(true); 
        
        // NẾU KHÔNG CÓ AI TRẢ GIÁ TRONG SUỐT PHIÊN
        if (!hasBids) {
            listBidHistory.getItems().add(0, "Hệ thống: Phiên đấu giá đã kết thúc và không có ai trả giá.");
            
            Platform.runLater(() -> {
                showNoWinnerPopup(); 
            });
            
            return; 
        }

        // NẾU CÓ NGƯỜI TRẢ GIÁ THÌ XỬ LÝ NHƯ BÌNH THƯỜNG
        String winnerName = "Nguyễn Văn A"; 
        String winnerPhone = "0987.654.321"; 
        String winnerEmail = "nguyenvana@gmail.com"; 

        double winningPrice = currentProduct.getCurrentBid(); 
        String formattedVND = formatVND(winningPrice);

        listBidHistory.getItems().add(0, "Hệ thống: " + winnerName + " đã thắng với giá " + formattedVND);
        
        boolean checkSeller = false;
        if (currentProduct != null) {
            String currentUser = "nguoimua123"; 
            
            String sellerUser = currentProduct.getSellerUsername(); 
            if (currentUser != null && currentUser.equals(sellerUser)) {
                checkSeller = true; 
            }
        }
        
        final boolean isSellerToPass = checkSeller;

        Platform.runLater(() -> {
            showWinnerPopup(winnerName, formattedVND, winnerPhone, winnerEmail, isSellerToPass);
        });
    }

    // =================================================================================
    // HÀM XỬ LÝ KHI NGƯỜI DÙNG BẤM NÚT "GỬI GIÁ BÁN" 
    // =================================================================================
    @FXML private void handleBidAction(ActionEvent event) { 
        String inputStr = txtBidAmount.getText(); 
        
        try { 
            double bidAmount = Double.parseDouble(inputStr); 
            double currentHighestBid = currentProduct.getCurrentBid(); 
            
            if (bidAmount <= currentHighestBid) { 
                listBidHistory.getItems().add(0, "Lỗi: Bạn phải trả giá cao hơn " + formatVND(currentHighestBid)); 
                return; 
            } 
            
            // XÁC NHẬN CÓ NGƯỜI TRẢ GIÁ HỢP LỆ THÌ MỚI BẬT CỜ TRUE
            hasBids = true; 
            
            currentProduct = new Product(currentProduct.getId(), currentProduct.getName(), bidAmount, currentProduct.getTimeRemaining(), currentProduct.getImagePath(), currentProduct.getSellerUsername()); 
            
            lblCurrentPrice.setText(formatVND(bidAmount)); 
            listBidHistory.getItems().add(0, "Bạn trả giá: " + formatVND(bidAmount)); 
            txtBidAmount.clear(); 
            
            // ANTI-SNIPING (+1.5đ)
            if (totalSeconds > 0 && totalSeconds <= 10) { 
                totalSeconds += 10; 
                listBidHistory.getItems().add(0, "🔥 Anti-sniping kích hoạt: Thời gian được cộng thêm 10s!"); 
                updateTimerDisplay(); 
            }
            
        } catch (NumberFormatException e) { 
            listBidHistory.getItems().add(0, "Lỗi: Vui lòng nhập số tiền hợp lệ!"); 
        } 
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
    // HÀM PHỤ TRỢ: CHUYỂN ĐỔI SỐ THỰC SANG TIỀN TỆ VNĐ
    // =================================================================================
    private String formatVND(double amount) {
        Locale vietnamLocale = new Locale("vi", "VN"); 
        NumberFormat vnFormat = NumberFormat.getCurrencyInstance(vietnamLocale); 
        return vnFormat.format(amount); 
    }

    // =================================================================================
    // HÀM VẼ CỬA SỔ POPUP VINH DANH NGƯỜI CHIẾN THẮNG
    // =================================================================================
    private void showWinnerPopup(String winnerName, String formattedPrice, String winnerPhone, String winnerEmail, boolean isSeller) { 
        Stage popupStage = new Stage(); 
        popupStage.initModality(Modality.APPLICATION_MODAL); 
        popupStage.initStyle(StageStyle.TRANSPARENT); 

        VBox popupBox = new VBox(15); 
        popupBox.setAlignment(Pos.CENTER); 
        popupBox.setStyle("-fx-background-color: white; -fx-padding: 40; -fx-background-radius: 15; -fx-border-radius: 15; -fx-border-color: #f1c40f; -fx-border-width: 4;");

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.25)); 
        popupBox.setEffect(shadow);

        Label lblTitle = new Label("🏆 KẾT THÚC PHIÊN ĐẤU GIÁ!");
        lblTitle.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #f39c12;"); 

        String pName = (currentProduct != null) ? currentProduct.getName() : "Sản phẩm";
        Label lblProduct = new Label("Sản phẩm: " + pName);
        lblProduct.setStyle("-fx-font-size: 18px; -fx-text-fill: #34495e;"); 

        Label lblWinner = new Label("Người chiến thắng: " + winnerName);
        lblWinner.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #27ae60;"); 

        Label lblPrice = new Label("Với mức giá: " + formattedPrice);
        lblPrice.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;"); 
        
        popupBox.getChildren().addAll(lblTitle, lblProduct, lblWinner, lblPrice);

        if (isSeller) {
            VBox contactBox = new VBox(5);
            contactBox.setAlignment(Pos.CENTER);
            contactBox.setStyle("-fx-background-color: #fdf2e9; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #e67e22; -fx-border-radius: 8;");
            
            Label lblContactTitle = new Label("THÔNG TIN LIÊN HỆ NGƯỜI MUA:");
            lblContactTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #d35400;");
            Label lblPhone = new Label("📞 SĐT: " + winnerPhone);
            lblPhone.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            Label lblEmailInfo = new Label("📧 Email: " + winnerEmail);
            lblEmailInfo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            
            contactBox.getChildren().addAll(lblContactTitle, lblPhone, lblEmailInfo);
            popupBox.getChildren().add(contactBox);
        } else {
            Label lblHidden = new Label("🔒 Thông tin liên lạc đã được bảo mật.\n(Chỉ người đăng bán mới có thể xem thông tin này)");
            lblHidden.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d; -fx-font-style: italic; -fx-text-alignment: center;");
            popupBox.getChildren().add(lblHidden);
        }

        Button btnClose = new Button("XÁC NHẬN");
        btnClose.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 30; -fx-background-radius: 5; -fx-cursor: hand; -fx-translate-y: 10;");
        btnClose.setOnAction(e -> popupStage.close()); 

        popupBox.getChildren().add(btnClose);

        Scene scene = new Scene(popupBox);
        scene.setFill(Color.TRANSPARENT); 
        popupStage.setScene(scene);
        popupStage.centerOnScreen();
        popupStage.showAndWait();
    }

    // =================================================================================
    // HÀM MỚI: VẼ CỬA SỔ POPUP KHI KHÔNG CÓ AI TRẢ GIÁ
    // =================================================================================
    private void showNoWinnerPopup() { 
        Stage popupStage = new Stage(); 
        popupStage.initModality(Modality.APPLICATION_MODAL); 
        popupStage.initStyle(StageStyle.TRANSPARENT); 

        VBox popupBox = new VBox(15); 
        popupBox.setAlignment(Pos.CENTER); 
        // Viền màu xám xịt báo hiệu sự thất bại/hủy bỏ
        popupBox.setStyle("-fx-background-color: white; -fx-padding: 40; -fx-background-radius: 15; -fx-border-radius: 15; -fx-border-color: #95a5a6; -fx-border-width: 4;");

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.25)); 
        popupBox.setEffect(shadow);

        Label lblTitle = new Label("PHIÊN ĐẤU GIÁ ĐÃ KẾT THÚC");
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #7f8c8d;"); 

        String pName = (currentProduct != null) ? currentProduct.getName() : "Sản phẩm";
        Label lblProduct = new Label("Sản phẩm: " + pName);
        lblProduct.setStyle("-fx-font-size: 18px; -fx-text-fill: #34495e;"); 

        Label lblNoWinner = new Label("Không có ai trả giá.");
        lblNoWinner.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;"); 

        Button btnClose = new Button("ĐÓNG");
        btnClose.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 30; -fx-background-radius: 5; -fx-cursor: hand; -fx-translate-y: 10;");
        btnClose.setOnAction(e -> popupStage.close()); 

        popupBox.getChildren().addAll(lblTitle, lblProduct, lblNoWinner, btnClose);

        Scene scene = new Scene(popupBox);
        scene.setFill(Color.TRANSPARENT); 
        popupStage.setScene(scene);
        popupStage.centerOnScreen();
        popupStage.showAndWait();
    }

    // =================================================================================
    // HÀM XỬ LÝ KHI BẤM NÚT "QUAY LẠI TRANG CHỦ"
    // =================================================================================
    @FXML 
    private void handleGoBack(ActionEvent event) { 
        try { 
            if (countdownTimer != null) { countdownTimer.stop(); } 
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
    // HÀM XỬ LÝ KHI BẤM NÚT "XEM ĐẦY ĐỦ CHI TIẾT"
    // =================================================================================
    @FXML 
    private void handleShowDetails(ActionEvent event) { 
        Stage detailStage = new Stage(); 
        detailStage.initModality(Modality.APPLICATION_MODAL); 
        detailStage.setTitle("Thông tin chi tiết sản phẩm"); 

        VBox layout = new VBox(15); 
        layout.setStyle("-fx-padding: 25; -fx-background-color: #ffffff;"); 

        Label lblTitle = new Label("CHI TIẾT SẢN PHẨM");
        lblTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        String pName = (currentProduct != null) ? currentProduct.getName() : "Không có dữ liệu";
        Label lblName = new Label("Tên sản phẩm: " + pName);
        lblName.setStyle("-fx-font-size: 16px; -fx-text-fill: #34495e; -fx-font-weight: bold;");

        String pSeller = (currentProduct != null) ? currentProduct.getSellerUsername() : "Không có dữ liệu";
        Label lblSeller = new Label("Người đăng bán: " + pSeller);
        lblSeller.setStyle("-fx-font-size: 16px; -fx-text-fill: #e67e22;"); 

        Label lblDesc = new Label("Mô tả đầy đủ:\nHiện tại người bán chưa cung cấp thêm thông tin chi tiết nào khác cho sản phẩm này.");
        lblDesc.setStyle("-fx-font-size: 15px; -fx-text-fill: #7f8c8d; -fx-line-spacing: 5px;");
        lblDesc.setWrapText(true); 

        Button btnClose = new Button("ĐÓNG");
        btnClose.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand;");
        btnClose.setOnAction(e -> detailStage.close()); 

        layout.getChildren().addAll(lblTitle, lblName, lblSeller, lblDesc, btnClose);
        layout.setAlignment(Pos.TOP_LEFT); 

        Scene scene = new Scene(layout, 450, 350); 
        detailStage.setScene(scene);
        detailStage.centerOnScreen(); 
        detailStage.showAndWait(); 
    }
    
    // =================================================================================
    // HÀM TẠO HIỆU ỨNG DI CHUỘT (HOVER) CHO NÚT "XEM CHI TIẾT"
    // =================================================================================
    @FXML 
    private void handleMouseEnter() { 
        if (btnShowDetails != null) {
            btnShowDetails.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(41, 128, 185, 0.8), 15, 0, 0, 5);");
        }
    }

    @FXML 
    private void handleMouseExit() { 
        if (btnShowDetails != null) {
            btnShowDetails.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px; -fx-cursor: hand;");
        }
    }
}