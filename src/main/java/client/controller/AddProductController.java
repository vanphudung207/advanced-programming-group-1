package client.controller;

import client.model.Product;
import client.service.AuthService;
import client.service.FirebaseService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public class AddProductController {

    @FXML private TextField txtName;
    @FXML private TextArea  txtDescription;
    @FXML private TextField txtStartPrice;
    @FXML private TextField txtStepPrice;
    @FXML private DatePicker dpEndDate;
    @FXML private ComboBox<String> cbCategory;
    @FXML private ComboBox<String> cbHour;
    @FXML private ComboBox<String> cbMinute;
    @FXML private ImageView imgPreview;
    @FXML private Label lblImageStatus;

    private String selectedImagePath = null;
    private java.io.File selectedImageFile = null;

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
        String name          = txtName.getText().trim();
        // LẤY MÔ TẢ TỪ FORM
        String description   = (txtDescription != null) ? txtDescription.getText().trim() : "";
        String startPriceStr = txtStartPrice.getText().trim();
        String stepPriceStr  = txtStepPrice.getText().trim();
        String category      = cbCategory.getValue();
        String hour          = cbHour.getValue();
        String minute        = cbMinute.getValue();

        if (name.isEmpty() || startPriceStr.isEmpty() || stepPriceStr.isEmpty()
                || dpEndDate.getValue() == null || hour == null || minute == null) {
            showAlert(AlertType.WARNING, "Thiếu thông tin",
                "Vui lòng điền đầy đủ thông tin, bao gồm Ngày và Giờ kết thúc!");
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
            double stepPrice  = Double.parseDouble(stepPriceStr);

            if (startPrice <= 0 || stepPrice <= 0) {
                showAlert(AlertType.WARNING, "Giá trị không hợp lệ",
                    "Giá khởi điểm và Bước giá phải lớn hơn 0!");
                return;
            }

            LocalDateTime endDateTime = LocalDateTime.of(
                dpEndDate.getValue(),
                LocalTime.of(Integer.parseInt(hour), Integer.parseInt(minute), 0)
            );
            long endTime = endDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

            if (endTime <= System.currentTimeMillis()) {
                showAlert(AlertType.WARNING, "Thời gian không hợp lệ",
                    "Thời gian kết thúc phải sau thời điểm hiện tại!");
                return;
            }

            String timeRemaining = "Kết thúc vào: "
                + dpEndDate.getValue().toString() + " " + hour + ":" + minute;

            // ĐÃ FIX XUNG ĐỘT: Ưu tiên dùng cách của đồng đội (AuthService)
            String seller = (AuthService.currentUserEmail != null)
                            ? AuthService.currentUserEmail : "Khách vãng lai";

            // CONSTRUCTOR MỚI CÓ description
            lblImageStatus.setText("Đang tải ảnh lên...");
            String imageUrl = FirebaseService.uploadImage(selectedImageFile);
            if (imageUrl == null) {
                String detail = FirebaseService.getLastError();
                showAlert(AlertType.ERROR, "Lỗi",
                    detail == null || detail.isBlank()
                        ? "Không thể tải ảnh lên Firebase Storage. Kiểm tra lại kết nối!"
                        : "Không thể tải ảnh lên Firebase Storage:\n" + detail);
                lblImageStatus.setText("Tải ảnh thất bại!");
                return;
            }

            Product newProduct = new Product(
                null, name, description,
                startPrice, timeRemaining,
                imageUrl, seller, category,
                stepPrice, "active", endTime
            );

            String firebaseKey = FirebaseService.addProduct(newProduct);
            if (firebaseKey != null) {
                newProduct.setFirebaseKey(firebaseKey);
                showAlert(AlertType.INFORMATION, "Thành công",
                    "Sản phẩm của bạn đã được đưa lên sàn đấu giá!");
                returnToProductList(event);
            } else {
                String detail = FirebaseService.getLastError();
                showAlert(AlertType.ERROR, "Lỗi",
                    detail == null || detail.isBlank()
                        ? "Không thể lưu sản phẩm lên Firebase."
                        : "Không thể lưu sản phẩm lên Firebase:\n" + detail);
            }

        } catch (NumberFormatException e) {
            showAlert(AlertType.ERROR, "Lỗi nhập liệu",
                "Giá khởi điểm và Bước giá phải là số hợp lệ!");
        }
    }

    @FXML
    private void handleChooseImage(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Chọn ảnh sản phẩm đấu giá");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            selectedImageFile = file;
            selectedImagePath = file.toURI().toString();
            imgPreview.setImage(new Image(selectedImagePath));
            lblImageStatus.setText("Đã chọn: " + file.getName());
            lblImageStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) { returnToProductList(event); }

    private void returnToProductList(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/client/view/ProductList.fxml"));
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
}