package client.controller;

import client.model.Product;
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
        String name         = txtName.getText().trim();
        String startPriceStr= txtStartPrice.getText().trim();
        String stepPriceStr = txtStepPrice.getText().trim();
        String category     = cbCategory.getValue();
        String hour         = cbHour.getValue();
        String minute       = cbMinute.getValue();

        // --- Validate ---
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

            String timeRemaining = "Kết thúc vào: "
                + dpEndDate.getValue().toString() + " " + hour + ":" + minute;

            String seller = (FirebaseService.registeredUsername != null)
                            ? FirebaseService.registeredUsername : "Khách vãng lai";

            // FIX: Dùng constructor đầy đủ với stepPrice
            // ID tạm để sau Firebase trả về key thực
            Product newProduct = new Product(
                null, name, startPrice, timeRemaining,
                selectedImagePath, seller, category,
                stepPrice, "active", 60
            );

            // FIX: addProduct trả về Firebase key thực → set ngược vào product
            String firebaseKey = FirebaseService.addProduct(newProduct);
            if (firebaseKey != null) {
                newProduct.setFirebaseKey(firebaseKey);
                showAlert(AlertType.INFORMATION, "Thành công",
                    "Sản phẩm của bạn đã được đưa lên sàn đấu giá!");
                returnToProductList(event);
            } else {
                showAlert(AlertType.ERROR, "Lỗi",
                    "Không thể kết nối Firebase. Vui lòng kiểm tra lại kết nối mạng!");
            }

        } catch (NumberFormatException e) {
            showAlert(AlertType.ERROR, "Lỗi nhập liệu",
                "Giá khởi điểm và Bước giá phải là số hợp lệ!");
        }
    }

    @FXML
    private void handleChooseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm đấu giá");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            String absoluteUrl = file.toURI().toString();
            selectedImagePath = absoluteUrl;
            imgPreview.setImage(new Image(absoluteUrl));
            lblImageStatus.setText("Đã chọn: " + file.getName());
            lblImageStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        returnToProductList(event);
    }

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
