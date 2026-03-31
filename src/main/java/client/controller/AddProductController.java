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
import javafx.stage.FileChooser; // Công cụ chọn file
import java.io.File; // Thư viện file chuẩn của Java
import javafx.scene.image.Image; // Thư viện ảnh của JavaFX
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

public class AddProductController {

    // Liên kết với ô nhập Tên sản phẩm trên FXML
    @FXML private TextField txtName;
    
    // Liên kết với ô nhập Mô tả chi tiết
    @FXML private TextArea txtDescription;
    
    // Liên kết với ô nhập Giá khởi điểm
    @FXML private TextField txtStartPrice;
    
    // Liên kết với ô nhập Bước giá tối thiểu
    @FXML private TextField txtStepPrice;
    
    // Liên kết với bộ chọn Ngày kết thúc
    @FXML private DatePicker dpEndDate;

    @FXML private ComboBox<String> cbCategory; // Hộp chọn danh mục (kiểu chữ String)

    @FXML private ImageView imgPreview; // Khung xem trước ảnh trên giao diện
    @FXML private Label lblImageStatus; // Nhãn báo trạng thái (Ví dụ: "Đã chọn file...")

    //Biến dùng để lưu đường dẫn file ảnh để giao cho người khác (Backend)
    private String selectedImagePath = null;

    // =========================================================================
    // HÀM XỬ LÝ KHI BẤM NÚT "ĐĂNG SẢN PHẨM"
    // Nhiệm vụ: Gom dữ liệu trên form -> Kiểm tra sơ bộ -> Chuyển cho người số 2 xử lý
    // =========================================================================
    @FXML
    private void handleAddProduct(ActionEvent event) {
        // 1. Lấy toàn bộ dữ liệu người dùng vừa nhập dưới dạng chuỗi (String)
        String name = txtName.getText();
        String description = txtDescription.getText();
        String startPriceStr = txtStartPrice.getText();
        String stepPriceStr = txtStepPrice.getText();
        String category = cbCategory.getValue(); // LẤY LOẠI SẢN PHẨM NGƯỜI DÙNG ĐÃ CHỌN
        
        // Kiểm tra xem người dùng đã điền đủ các trường bắt buộc chưa
        if (name.isEmpty() || startPriceStr.isEmpty() || stepPriceStr.isEmpty() || dpEndDate.getValue() == null) {
            // Nếu thiếu, hiển thị một hộp thoại cảnh báo (Alert) màu vàng
            showAlert(AlertType.WARNING, "Thiếu thông tin", "Vui lòng điền đầy đủ các trường có dấu *");
            return; // Dừng hàm lại, không chạy tiếp xuống dưới
        }
        if (name.isEmpty() || category == null || startPriceStr.isEmpty() ) {
            showAlert(AlertType.WARNING, "Thiếu thông tin", "Vui lòng chọn Danh mục sản phẩm!");
            return;
        }

        // Kiểm tra thêm điều kiện: Bắt buộc phải có ảnh sản phẩm
        if (name.isEmpty() || category == null || selectedImagePath == null || startPriceStr.isEmpty() || stepPriceStr.isEmpty() || dpEndDate.getValue() == null) {
            // Nếu thiếu, hiển thị popup thông báo lỗi
            showAlert(AlertType.WARNING, "Thiếu thông tin", "Vui lòng tải ảnh sản phẩm lên!");
            return;
        }

        System.out.println("- Ảnh: " + selectedImagePath);

        System.out.println("- Danh mục: " + category);

        try {
            // 2. Chuyển đổi chuỗi chữ số sang kiểu số thực (Double) để tính toán
            double startPrice = Double.parseDouble(startPriceStr);
            double stepPrice = Double.parseDouble(stepPriceStr);

            // TODO: GỬI THÔNG ĐIỆP CHO THÀNH VIÊN SỐ 2 VÀ SỐ 1
            // Chỗ này sau này Thành viên số 2 sẽ gọi hàm: Database.saveProduct(name, description, startPrice, stepPrice, dpEndDate.getValue());
            // Hiện tại mình chỉ in ra màn hình Console để test luồng chạy
            System.out.println("Đang chuẩn bị đăng sản phẩm mới:");
            System.out.println("- Tên: " + name);
            System.out.println("- Giá khởi điểm: $" + startPrice);
            System.out.println("- Hạn chót: " + dpEndDate.getValue().toString());

            // 3. Thông báo thành công cho người dùng
            showAlert(AlertType.INFORMATION, "Thành công", "Sản phẩm của bạn đã được đưa lên sàn đấu giá!");

            // 4. Đăng xong thì tự động quay về màn hình Danh sách (ProductList)
            returnToProductList(event);

        } catch (NumberFormatException e) {
            // Nếu người dùng nhập chữ (ví dụ "abc") vào ô giá tiền thì sẽ bị bắt lỗi ở đây
            showAlert(AlertType.ERROR, "Lỗi nhập liệu", "Giá khởi điểm và Bước giá phải là con số hợp lệ!");
        }
    }

    // =========================================================================
    // HÀM XỬ LÝ KHI BẤM NÚT "HỦY BỎ"
    // Công dụng: Không lưu gì cả và đưa người dùng quay lại trang Danh sách sản phẩm
    // =========================================================================
    @FXML
    private void handleCancel(ActionEvent event) {
        System.out.println("Đã hủy thao tác đăng sản phẩm. Quay về trang chủ.");
        returnToProductList(event);
    }

    // =========================================================================
    // HÀM HỖ TRỢ: CHUYỂN TRANG VỀ MÀN HÌNH DANH SÁCH (Dùng chung cho cả 2 nút)
    // =========================================================================
    private void returnToProductList(ActionEvent event) {
        try {
            // Load lại file giao diện Danh sách sản phẩm
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/ProductList.fxml"));
            Parent root = loader.load();
            
            // Lấy ra cái cửa sổ (Stage) hiện tại đang hiển thị
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            // Đặt giao diện Danh sách vào cửa sổ và cho nó phóng to
            stage.setScene(new Scene(root, 900, 650));
            stage.setMaximized(true);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // HÀM HỖ TRỢ: HIỂN THỊ HỘP THOẠI THÔNG BÁO POP-UP
    // Công dụng: Giúp code gọn hơn, tái sử dụng được nhiều lần khi cần chửi/khen người dùng
    // =========================================================================
    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null); // Tắt phần Header phụ cho popup đỡ cồng kềnh
        alert.setContentText(content);
        alert.showAndWait(); // Hiển thị popup và bắt người dùng phải bấm OK mới cho đi tiếp
    }

    @FXML
    public void initialize() {
        // Nạp các loại sản phẩm vào ComboBox
        // Bạn có thể thêm hoặc bớt các loại tùy ý ở đây
        cbCategory.getItems().addAll(
            "Điện tử", 
            "Gia dụng", 
            "Sách", 
            "Thể thao", 
            "Giải trí", 
            "Khác"
        );
    }

    // =========================================================================
    // HÀM XỬ LÝ KHI BẤM NÚT "TẢI ẢNH LÊN"
    // Nhiệm vụ: Mở cửa sổ chọn file -> Lấy ảnh -> Hiển thị xem trước
    // =========================================================================
    @FXML
    private void handleChooseImage(ActionEvent event) {
        // 1. Tạo đối tượng FileChooser (Cửa sổ chọn file của hệ điều hành)
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm đấu giá");

        // 2. Chỉ cho phép người dùng chọn các định dạng ảnh phổ biến (PNG, JPG, JPEG)
        // Nếu không có dòng này, người dùng có thể chọn file .mp3, .pdf... làm app bị crash
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        // 3. Mở cửa sổ chọn file ra và bắt người dùng phải chọn một tấm ảnh
        // (Chúng ta lấy Stage từ nút bấm hiện tại để neo cái popup chọn file vào)
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        // 4. Xử lý kết quả trả về từ cửa sổ chọn file
        if (file != null) {
            // -- NẾU NGƯỜI DÙNG ĐÃ CHỌN 1 TẤM ẢNH THÀNH CÔNG --

            // A. Chuyển đổi file từ ổ cứng thành Object Image của JavaFX để hiển thị
            // Chúng dùng File.toURI().toString() để tạo ra đường dẫn file chuẩn xác
            Image image = new Image(file.toURI().toString());
            
            // B. Đổ tấm ảnh vừa chọn vào khung ImageView để xem trước
            imgPreview.setImage(image);

            // C. Lưu đường dẫn đầy đủ của file ảnh vào biến tạm để lát nữa giao cho Backend
            // selectedImagePath = file.getAbsolutePath(); // Chú ý: Backend cần đường dẫn này để lưu dữ liệu
            selectedImagePath = file.getName(); // Cho bài tập lớn, chúng ta chỉ lấy tên file cho đơn giản

            // D. Cập nhật nhãn trạng thái để báo cho người dùng biết là đã chọn ảnh xong
            lblImageStatus.setText("Đã chọn: " + file.getName());
            // Đổi màu nhãn sang xanh lá cây để báo thành công
            lblImageStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");

            System.out.println("Đã tải ảnh lên thành công từ đường dẫn: " + file.getAbsolutePath());
        
        } else {
            // -- NẾU NGƯỜI DÙNG BẤM "CANCEL" HOẶC ĐÓNG CỬA SỔ CHỌN FILE MÀ KHÔNG CHỌN ẢNH --
            System.out.println("Người dùng đã hủy thao tác chọn ảnh.");
        }
    }
}