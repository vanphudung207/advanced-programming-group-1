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

public class AuctionRoomController { // Bắt đầu khai báo class Controller

    @FXML private ImageView imgProduct; 
    @FXML private Label lblProductName; 
    @FXML private Label lblCurrentPrice; 
    @FXML private Label lblTimer; 
    @FXML private TextField txtBidAmount; 
    @FXML private Button btnSubmitBid; 
    @FXML private ListView<String> listBidHistory; 

    private Product currentProduct; 
    private int totalSeconds = 4500; 
    private Timeline countdownTimer; 

    // =================================================================================
    // HÀM NHẬN DỮ LIỆU TỪ MÀN HÌNH DANH SÁCH CHUYỂN SANG
    // =================================================================================
    public void setProductData(Product product) { 
        this.currentProduct = product; 
        
        if (currentProduct != null) { 
            lblProductName.setText(currentProduct.getName()); 
            lblCurrentPrice.setText(String.format("%,.0f VNĐ", currentProduct.getCurrentBid()));

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
                listBidHistory.getItems().add(0, "Hệ thống: Chốt giá thành công!"); 
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
    // HÀM XỬ LÝ KHI NGƯỜI DÙNG BẤM NÚT "GỬI GIÁ BÁN"
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

} // Kết thúc file