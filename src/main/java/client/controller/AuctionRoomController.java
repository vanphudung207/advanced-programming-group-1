package client.controller;

import client.model.Product;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AuctionRoomController {

    @FXML
    private Label lblProductName;

    // Biến lưu trữ sản phẩm hiện tại đang được đấu giá trong phòng này
    private Product currentProduct;

    // ==============================================================
    // HÀM NHẬN DỮ LIỆU TỪ TRANG CHỦ TRUYỀN SANG
    // Thành viên số 3 sẽ dùng hàm này để lấy tên, giá, ảnh... hiển thị lên phòng
    // ==============================================================
    public void setProductData(Product product) {
        this.currentProduct = product;
        
        // Cập nhật tạm tên sản phẩm lên màn hình để test xem đã nhận được data chưa
        if (currentProduct != null) {
            lblProductName.setText("Đang đấu giá: " + currentProduct.getName() + " - Giá hiện tại: $" + currentProduct.getCurrentBid());
            System.out.println("Đã mở phòng đấu giá cho sản phẩm ID: " + currentProduct.getId());
        }
    }
}