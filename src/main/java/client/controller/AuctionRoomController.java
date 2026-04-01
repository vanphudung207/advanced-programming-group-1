package client.controller;

import client.model.Product;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

//Các thư viện cần thiết để chuyển đổi giữa các màn hình
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
// Thư viện xử lý hình ảnh
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class AuctionRoomController {

    @FXML
    private Label lblProductName;

    // Biến lưu trữ sản phẩm hiện tại đang được đấu giá trong phòng này
    private Product currentProduct;

    @FXML private Label lblCurrentPrice; // Nhãn hiển thị giá siêu to
    @FXML private ImageView imgProduct;  // Khung chứa ảnh sản phẩm

    // ==============================================================
    // HÀM NHẬN DỮ LIỆU TỪ TRANG CHỦ TRUYỀN SANG
    // ==============================================================
    public void setProductData(Product product) {
        this.currentProduct = product;
        if (currentProduct != null) {
            lblProductName.setText(currentProduct.getName());
            lblCurrentPrice.setText("$" + currentProduct.getCurrentBid());
            

            try {
                Image img = new Image(currentProduct.getImagePath(), true); // true = tải ngầm
                imgProduct.setImage(img);
            } catch (Exception ex) {
                System.out.println("Lỗi tải ảnh trong phòng đấu giá cho: " + currentProduct.getName());
            }

            System.out.println("Đã mở phòng đấu giá cho sản phẩm ID: " + currentProduct.getId());
        }
    }

    // ==============================================================
    // HÀM XỬ LÝ KHI BẤM NÚT "QUAY LẠI TRANG CHỦ"
    // Công dụng: Thoát khỏi phòng đấu giá, dọn dẹp và tải lại màn hình ProductList
    // ==============================================================
    @FXML
    private void handleGoBack(ActionEvent event) {
        try {
            // 1. Tìm và tải file giao diện Danh sách sản phẩm (Trang chủ)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/ProductList.fxml"));
            Parent root = loader.load();

            // 2. Xác định cửa sổ (Stage) hiện tại đang mở dựa vào cái nút người dùng vừa bấm
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // 3. Trải giao diện Trang chủ lên cửa sổ này
            stage.setScene(new Scene(root, 900, 650));
            
            // 4. Ép cửa sổ phóng to hết cỡ (Maximized) để Trang chủ dàn đều ra
            stage.setMaximized(true); 
            stage.setTitle("Online Auction System - Dashboard");

            // In ra console để theo dõi luồng chạy
            System.out.println("Đã thoát phòng đấu giá, quay về Trang chủ.");
            
        } catch (Exception e) {
            System.out.println("Lỗi khi tải trang chủ!");
            e.printStackTrace();
        }
    }
}