package client.controller;

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
import client.model.Product; // Import Model

public class AddProductController {

    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtStartPrice;
    @FXML private TextField txtStepPrice;
    @FXML private DatePicker dpEndDate;
    @FXML private ComboBox<String> cbCategory; 
    @FXML private ComboBox<String> cbHour;
    @FXML private ComboBox<String> cbMinute;
    @FXML private ImageView imgPreview; 
    @FXML private Label lblImageStatus; 

    private String selectedImagePath = null;

    @FXML
    public void initialize() {
        cbCategory.getItems().addAll("Điện tử", "Gia dụng", "Sách", "Thể thao", "Giải trí", "Khác");
        for (int i = 0; i <= 23; i++) cbHour.getItems().add(String.format("%02d", i));
        cbHour.setValue("23"); 
        for (int i = 0; i <= 59; i++) cbMinute.getItems().add(String.format("%02d", i));
        cbMinute.setValue("59");
    }

    @FXML
    private void handleAddProduct(ActionEvent event) {
        String name = txtName.getText();
        String description = txtDescription.getText();
        String startPriceStr = txtStartPrice.getText();
        String stepPriceStr = txtStepPrice.getText();
        String category = cbCategory.getValue(); 
        String hour = cbHour.getValue();
        String minute = cbMinute.getValue();
        
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
            
            // 1. Tạo ID mới ngẫu nhiên cho sản phẩm
            String newId = "SP0" + (client.service.FirebaseService.getAllProducts().size() + 1);

            // 2. Chắp nối Thời gian
            String timeRemaining = "Kết thúc vào: " + dpEndDate.getValue().toString() + " " + hour + ":" + minute;

            // 3. Lấy tên người đăng bán (Ai đang đăng nhập thì lấy tên người đó)
            String seller = (client.service.FirebaseService.registeredUsername != null) ? client.service.FirebaseService.registeredUsername : "Khách vãng lai";

            // 4. Khởi tạo cục Sản Phẩm Mới
            Product newProduct = new Product(newId, name, startPrice, timeRemaining, selectedImagePath, seller, category);

            // 5. Ném hàng vào Kho Database
            client.service.FirebaseService.addProduct(newProduct);

            showAlert(AlertType.INFORMATION, "Thành công", "Sản phẩm của bạn đã được đưa lên sàn đấu giá!");

            // Đăng xong thì tự động quay về Trang chủ để xem thành quả
            returnToProductList(event);

        } catch (NumberFormatException e) {
            showAlert(AlertType.ERROR, "Lỗi nhập liệu", "Giá khởi điểm và Bước giá phải là con số hợp lệ!");
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        returnToProductList(event);
    }

    private void returnToProductList(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/ProductList.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Online Auction System - Danh sách sản phẩm");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null); 
        alert.setContentText(content);
        alert.showAndWait(); 
    }

    @FXML
    private void handleChooseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm đấu giá");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            // ĐÃ SỬA: Lưu đường dẫn URI tuyệt đối thay vì chỉ lưu tên file, giúp app đọc được ảnh từ bất cứ đâu trên máy tính
            String absoluteUrl = file.toURI().toString();
            selectedImagePath = absoluteUrl; 

            Image image = new Image(absoluteUrl);
            imgPreview.setImage(image);

            lblImageStatus.setText("Đã chọn: " + file.getName());
            lblImageStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        }
    }
}