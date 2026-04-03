package client.controller;

// Import các thư viện xử lý sự kiện và lấy dữ liệu từ giao diện
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.stage.FileChooser; 
import java.io.File; 
import javafx.scene.image.Image; 
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import java.time.format.DateTimeFormatter; // Phục vụ xử lý ngày tháng

public class AddProductController {

    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtStartPrice;
    @FXML private TextField txtStepPrice;
    @FXML private DatePicker dpEndDate;
    @FXML private ComboBox<String> cbCategory; 
    
    // =========================================================
    // MỚI THÊM: Khai báo 2 biến để hứng Giờ và Phút từ FXML
    // =========================================================
    @FXML private ComboBox<String> cbHour;
    @FXML private ComboBox<String> cbMinute;

    @FXML private ImageView imgPreview; 
    @FXML private Label lblImageStatus; 

    private String selectedImagePath = null;

    // =========================================================================
    // HÀM KHỞI TẠO: Chạy đầu tiên khi mở form
    // =========================================================================
    @FXML
    public void initialize() {
        // 1. Nạp danh mục sản phẩm
        cbCategory.getItems().addAll("Điện tử", "Gia dụng", "Sách", "Thể thao", "Giải trí", "Khác");
        
        // 2. NẠP DỮ LIỆU GIỜ (Từ 00 đến 23)
        for (int i = 0; i <= 23; i++) {
            // String.format("%02d", i) giúp ép số 1 thành "01", số 9 thành "09"
            cbHour.getItems().add(String.format("%02d", i));
        }
        cbHour.setValue("23"); // Mặc định để 23 giờ

        // 3. NẠP DỮ LIỆU PHÚT (Từ 00 đến 59, liệt kê đầy đủ từng phút một)
        for (int i = 0; i <= 59; i++) { 
            // String.format("%02d", i) tự động độn thêm số 0 đằng trước nếu số đó bé hơn 10 (VD: biến 5 thành "05")
            cbMinute.getItems().add(String.format("%02d", i));
        }
        // Tự động chọn sẵn giá trị "59" lúc vừa mở form (Giúp người dùng dễ chốt giờ đẹp kiểu 23:59)
        cbMinute.setValue("59");
    }

    // =========================================================================
    // HÀM XỬ LÝ KHI BẤM NÚT "ĐĂNG SẢN PHẨM"
    // =========================================================================
    @FXML
    private void handleAddProduct(ActionEvent event) {
        String name = txtName.getText();
        String description = txtDescription.getText();
        String startPriceStr = txtStartPrice.getText();
        String stepPriceStr = txtStepPrice.getText();
        String category = cbCategory.getValue(); 
        
        // MỚI THÊM: Lấy giá trị Giờ và Phút
        String hour = cbHour.getValue();
        String minute = cbMinute.getValue();
        
        // Kiểm tra xem đã điền đủ ngày, giờ và phút chưa
        if (name.isEmpty() || startPriceStr.isEmpty() || stepPriceStr.isEmpty() || dpEndDate.getValue() == null || hour == null || minute == null) {
            showAlert(AlertType.WARNING, "Thiếu thông tin", "Vui lòng điền đầy đủ thông tin, bao gồm Ngày và Giờ kết thúc!");
            return; 
        }

        if (category == null) {
            showAlert(AlertType.WARNING, "Thiếu thông tin", "Vui lòng chọn Danh mục sản phẩm!");
            return;
        }

        if (selectedImagePath == null) {
            showAlert(AlertType.WARNING, "Thiếu thông tin", "Vui lòng tải ảnh sản phẩm lên!");
            return;
        }

        try {
            double startPrice = Double.parseDouble(startPriceStr);
            double stepPrice = Double.parseDouble(stepPriceStr);

            // Gắn Giờ và Phút vào chung với Ngày để in ra kết quả
            String timeString = hour + ":" + minute;
            String fullDateTime = dpEndDate.getValue().toString() + " " + timeString;

            System.out.println("--- ĐĂNG SẢN PHẨM MỚI ---");
            System.out.println("- Tên: " + name);
            System.out.println("- Danh mục: " + category);
            System.out.println("- Giá khởi điểm: " + String.format("%,.0f VNĐ", startPrice));
            System.out.println("- Hạn chót: " + fullDateTime); // In ra cả Ngày lẫn Giờ
            System.out.println("- Ảnh: " + selectedImagePath);

            showAlert(AlertType.INFORMATION, "Thành công", "Sản phẩm của bạn đã được đưa lên sàn đấu giá!");

            returnToProductList(event);

        } catch (NumberFormatException e) {
            showAlert(AlertType.ERROR, "Lỗi nhập liệu", "Giá khởi điểm và Bước giá phải là con số hợp lệ!");
        }
    }

    // =========================================================================
    // HÀM XỬ LÝ KHI BẤM NÚT "HỦY BỎ"
    // =========================================================================
    @FXML
    private void handleCancel(ActionEvent event) {
        System.out.println("Đã hủy thao tác đăng sản phẩm. Quay về trang chủ.");
        returnToProductList(event);
    }

    // =========================================================================
    // HÀM HỖ TRỢ CHUYỂN TRANG
    // =========================================================================
    private void returnToProductList(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/ProductList.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Online Auction System - Danh sách sản phẩm");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // HÀM HỖ TRỢ: HIỂN THỊ POPUP
    // =========================================================================
    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null); 
        alert.setContentText(content);
        alert.showAndWait(); 
    }

    // =========================================================================
    // HÀM XỬ LÝ TẢI ẢNH LÊN
    // =========================================================================
    @FXML
    private void handleChooseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm đấu giá");

        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            Image image = new Image(file.toURI().toString());
            imgPreview.setImage(image);
            selectedImagePath = file.getName(); 

            lblImageStatus.setText("Đã chọn: " + file.getName());
            lblImageStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        }
    }
}