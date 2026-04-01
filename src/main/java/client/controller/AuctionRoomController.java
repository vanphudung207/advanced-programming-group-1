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

    // ĐÃ FIX: Khai báo thêm biến imgProduct để liên kết với khung ảnh bên FXML
    @FXML private ImageView imgProduct; // Liên kết với thẻ <ImageView fx:id="imgProduct">

    @FXML private Label lblProductName; // Liên kết với tên sản phẩm (VD: iPhone 15 Pro Max)
    @FXML private Label lblCurrentPrice; // Liên kết với chữ hiển thị giá màu cam (VD: $950.0)
    @FXML private Label lblTimer; // Liên kết với đồng hồ đếm ngược màu xanh nhạt
    @FXML private TextField txtBidAmount; // Liên kết với ô "Nhập mức giá bạn muốn trả ($).."
    @FXML private Button btnSubmitBid; // Liên kết với nút xanh lá "GỬI GIÁ BÁN"
    @FXML private ListView<String> listBidHistory; // Liên kết với khung trắng "Lịch sử trả giá"

    private Product currentProduct; // Khởi tạo biến lưu trữ món hàng đang được đấu giá
    private int totalSeconds = 4500; // Giả lập thời gian là 4500 giây (tương đương 01:15:00)
    private Timeline countdownTimer; // Khởi tạo bộ máy cho đồng hồ đếm ngược

    // =================================================================================
    // HÀM NHẬN DỮ LIỆU TỪ MÀN HÌNH DANH SÁCH CHUYỂN SANG
    // =================================================================================
    public void setProductData(Product product) { // Hàm này được gọi từ ProductListController
        this.currentProduct = product; // Nhận dữ liệu và gán vào biến cục bộ của phòng
        
        if (currentProduct != null) { // Kiểm tra chắc chắn sản phẩm có tồn tại
            lblProductName.setText(currentProduct.getName()); // Ghi đè tên sản phẩm lên UI
            lblCurrentPrice.setText("$" + currentProduct.getCurrentBid()); // Ghi đè mức giá lên UI

            // Xử lý tải và đổ dữ liệu ảnh sản phẩm từ Model ra giao diện
            try {
                // Tạo đối tượng Image từ URL của sản phẩm, cờ 'true' giúp tải ảnh ngầm không làm đơ app
                Image img = new Image(currentProduct.getImagePath(), true); 
                imgProduct.setImage(img); // Gắn bức ảnh vừa tải vào khung ImageView
            } catch (Exception e) {
                System.out.println("Lỗi không thể tải ảnh: " + e.getMessage()); // Log lỗi nếu link ảnh bị hỏng
            }
            
            listBidHistory.getItems().add("Hệ thống: Bắt đầu phiên đấu giá!"); // Thông báo mở phòng
            startCountdownTimer(); // Kích hoạt cho đồng hồ bắt đầu chạy
        } // Kết thúc lệnh if
    } // Kết thúc hàm setProductData

    // =================================================================================
    // HÀM XỬ LÝ ĐỒNG HỒ ĐẾM NGƯỢC
    // =================================================================================
    private void startCountdownTimer() { // Bắt đầu hàm cài đặt đồng hồ

        updateTimerDisplay(); // Ép đồng hồ hiển thị ngay lập tức ở giây số 0
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> { // Tạo sự kiện lặp lại mỗi 1 giây
            totalSeconds--; // Trừ đi 1 giây trong tổng thời gian còn lại
            updateTimerDisplay(); // Cập nhật lại giao diện sau khi trừ giờ
            
            if (totalSeconds <= 0) { // Kiểm tra nếu hết giờ
                countdownTimer.stop(); // Dừng đồng hồ lại
                lblTimer.setText("Phiên đấu giá đã đóng!"); // Thông báo hết giờ lên màn hình
                btnSubmitBid.setDisable(true); // Làm mờ nút gửi giá, không cho bấm nữa
                txtBidAmount.setDisable(true); // Khóa ô nhập liệu, không cho gõ phím
                listBidHistory.getItems().add(0, "Hệ thống: Chốt giá thành công!"); // Báo kết thúc vào lịch sử
            } // Kết thúc if kiểm tra hết giờ
        })); // Kết thúc khởi tạo bộ đếm
        
        countdownTimer.setCycleCount(Timeline.INDEFINITE); // Cho phép đồng hồ chạy vô tận tới khi gọi stop()
        countdownTimer.play(); // Lệnh kích hoạt đồng hồ chạy
    } // Kết thúc hàm đồng hồ

    // =================================================================================
    // HÀM PHỤ TRỢ: TÍNH TOÁN & HIỂN THỊ THỜI GIAN
    // =================================================================================
    private void updateTimerDisplay() {
        int hours = totalSeconds / 3600; // Phép chia lấy phần nguyên để ra số giờ
        int minutes = (totalSeconds % 3600) / 60; // Lấy phần dư của giờ chia tiếp cho 60 để ra phút
        int seconds = totalSeconds % 60; // Lấy phần dư cuối cùng để ra số giây lẻ
        
        // Dùng String.format để ép chuẩn hiển thị luôn có 2 chữ số (VD: 01 thay vì 1) và đổ text ra UI
        lblTimer.setText(String.format("⏱ Còn lại: %02d:%02d:%02d", hours, minutes, seconds)); 
    }

    // =================================================================================
    // HÀM XỬ LÝ KHI NGƯỜI DÙNG BẤM NÚT "GỬI GIÁ BÁN"
    // =================================================================================
    @FXML private void handleBidAction(ActionEvent event) { // Hàm gắn vào sự kiện OnAction của nút bấm
        String inputStr = txtBidAmount.getText(); // Lấy chuỗi dữ liệu người dùng vừa nhập
        
        try { // Thử bắt đầu ép kiểu và xử lý logic
            double bidAmount = Double.parseDouble(inputStr); // Ép chuỗi nhập vào thành kiểu số thực
            double currentHighestBid = currentProduct.getCurrentBid(); // Lấy mức giá cao nhất hiện tại ra để so sánh
            
            // Bước kiểm tra logic cơ bản trước khi ném cho Thành viên 2 xử lý
            if (bidAmount <= currentHighestBid) { // Nếu giá nhập vào nhỏ hơn hoặc bằng giá hiện tại
                listBidHistory.getItems().add(0, "Lỗi: Bạn phải trả giá cao hơn $" + currentHighestBid); // Cảnh báo lỗi
                return; // Thoát hàm ngay, không chạy tiếp các dòng bên dưới
            } // Kết thúc if kiểm tra giá
            
            // TẠM THỜI MOCK DATA (Đợi ghép API của Thành viên 1)
            currentProduct = new Product(currentProduct.getId(), currentProduct.getName(), bidAmount, currentProduct.getTimeRemaining(), currentProduct.getImagePath()); // Tạo mới lại object sản phẩm với giá mới
            
            lblCurrentPrice.setText("$" + bidAmount); // Cập nhật mức giá kỷ lục lên màn hình
            listBidHistory.getItems().add(0, "Bạn trả giá: $" + bidAmount); // Ghi nhận vào lịch sử
            txtBidAmount.clear(); // Xóa trắng ô nhập giá để tiện nhập lần sau
            
        } catch (NumberFormatException e) { // Bắt lỗi nếu người dùng cố tình nhập chữ (VD: 'abc') vào ô giá
            listBidHistory.getItems().add(0, "Lỗi: Vui lòng nhập số tiền hợp lệ!"); // Báo lỗi yêu cầu nhập số
        } // Kết thúc khối try-catch
    } // Kết thúc hàm gửi giá

    // =================================================================================
    // HÀM XỬ LÝ KHI BẤM NÚT "QUAY LẠI TRANG CHỦ"
    // =================================================================================
    @FXML 
    private void handleGoBack(ActionEvent event) { // Hàm gắn vào sự kiện click nút Quay lại
        try { // Bắt đầu khối try-catch để kiểm soát lỗi khi load file giao diện mới
            
            if (countdownTimer != null) { // Kiểm tra xem đồng hồ đếm ngược có đang chạy không
                countdownTimer.stop(); // Bắt buộc dừng đồng hồ lại để tránh tốn RAM và lỗi chạy ngầm khi đã thoát phòng
            } // Kết thúc lệnh if
            
            // Đọc file thiết kế của màn hình Danh sách sản phẩm (ProductList)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/ProductList.fxml")); 
            Parent root = loader.load(); // Tải toàn bộ giao diện đó vào bộ nhớ
            
            // Lấy ra cửa sổ (Stage) hiện tại đang chứa cái nút mà bạn vừa click vào
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); 
            
            // Tạo một không gian (Scene) mới chứa giao diện ProductList và ráp vào cửa sổ cũ
            stage.setScene(new Scene(root, 900, 650)); 
            stage.setMaximized(true); // Đảm bảo cửa sổ vẫn mở to toàn màn hình
            stage.setTitle("Online Auction System - Danh sách sản phẩm"); // Đổi lại tiêu đề cửa sổ cho đúng ngữ cảnh
            
        } catch (Exception e) { // Khối bắt lỗi nếu quá trình chuyển trang thất bại (VD: sai đường dẫn file)
            e.printStackTrace(); // In dải lỗi màu đỏ ra Terminal để lập trình viên tự fix
        } // Kết thúc khối try-catch
    } // Kết thúc hàm handleGoBack

} // Kết thúc file