package client.controller;

import client.model.Product;
import client.service.FirebaseService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProductListController {

    @FXML private Button btnInfo;
    @FXML private Label lblDateTime;
    @FXML private FlowPane productContainer;
    @FXML private HBox categoryBox;
    @FXML private Button btnSearch;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbSort;

    private List<Product> currentDisplayedProducts;

    // Timer toàn cục đếm ngược song song cho tất cả thẻ trên màn hình danh sách
    private Timeline globalCardTimer;

    // =========================================================================
    // KHỞI TẠO
    // =========================================================================
    @FXML
    public void initialize() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm a | dd MMM, yyyy");
        Timeline clock = new Timeline(
            new KeyFrame(Duration.ZERO, e -> lblDateTime.setText(LocalDateTime.now().format(fmt))),
            new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();

        if (cbSort != null) {
            cbSort.getItems().addAll("Mặc định", "Giá: Thấp đến Cao", "Giá: Cao đến Thấp");
            cbSort.setValue("Mặc định");
        }

        currentDisplayedProducts = FirebaseService.getAllProducts();
        loadProductsToGrid(currentDisplayedProducts);

        if (categoryBox != null)
            for (Node n : categoryBox.getChildren())
                if (n instanceof Button) applySmoothHover((Button) n);
        if (btnSearch != null) applySmoothHover(btnSearch);
    }

    // =========================================================================
    // SẮP XẾP
    // =========================================================================
    @FXML
    private void handleSortAction(javafx.event.ActionEvent event) {
        if (currentDisplayedProducts == null || currentDisplayedProducts.isEmpty()) return;
        List<Product> sorted = new ArrayList<>(currentDisplayedProducts);
        String v = cbSort.getValue();
        if ("Giá: Thấp đến Cao".equals(v))
            sorted.sort((a, b) -> Double.compare(a.getCurrentBid(), b.getCurrentBid()));
        else if ("Giá: Cao đến Thấp".equals(v))
            sorted.sort((a, b) -> Double.compare(b.getCurrentBid(), a.getCurrentBid()));
        loadProductsToGrid(sorted);
    }

    // =========================================================================
    // TÌM KIẾM
    // =========================================================================
    @FXML
    private void handleSearchAction(javafx.event.ActionEvent event) {
        String kw = txtSearch.getText().trim();
        currentDisplayedProducts = kw.isEmpty()
            ? FirebaseService.getAllProducts()
            : FirebaseService.searchProducts(kw);
        if (cbSort != null) cbSort.setValue("Mặc định");
        loadProductsToGrid(currentDisplayedProducts);
    }

    // =========================================================================
    // LỌC DANH MỤC
    // =========================================================================
    @FXML
    private void handleFilterCategory(javafx.event.ActionEvent event) {
        String cat = ((Button) event.getSource()).getText();
        currentDisplayedProducts = FirebaseService.getProductsByCategory(cat);
        txtSearch.clear();
        if (cbSort != null) cbSort.setValue("Mặc định");
        loadProductsToGrid(currentDisplayedProducts);
    }

    // =========================================================================
    // VẼ LƯỚI SẢN PHẨM + TIMER SONG SONG TRÊN MỖI THẺ
    // =========================================================================
    private void loadProductsToGrid(List<Product> products) {
        // Dừng timer cũ trước khi vẽ lại
        if (globalCardTimer != null) globalCardTimer.stop();
        productContainer.getChildren().clear();

        // Empty state
        if (products == null || products.isEmpty()) {
            productContainer.setAlignment(Pos.CENTER);
            VBox empty = new VBox(15); empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(100, 0, 100, 0));
            Label ico  = new Label("📦"); ico.setStyle("-fx-font-size:60px;-fx-text-fill:#95a5a6;");
            Label msg  = new Label("Rất tiếc, không tìm thấy sản phẩm nào!");
            msg.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#7f8c8d;");
            Label sub  = new Label("Vui lòng thử lại với từ khóa khác hoặc chọn danh mục khác.");
            sub.setStyle("-fx-font-size:14px;-fx-text-fill:#95a5a6;");
            empty.getChildren().addAll(ico, msg, sub);
            productContainer.getChildren().add(empty);
            return;
        }

        productContainer.setAlignment(Pos.TOP_LEFT);

        // Danh sách Label thời gian + badge để timer cập nhật mỗi giây
        List<Label>   cardTimerLabels  = new ArrayList<>();
        List<Label>   cardBadgeLabels  = new ArrayList<>();
        List<Product> cardProducts     = new ArrayList<>();

        for (Product p : products) {
            // --- Khung thẻ ---
            VBox card = new VBox(10);
            String ns = "-fx-background-color:white;-fx-padding:15px;-fx-background-radius:10px;"
                + "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.1),5,0,0,2);";
            String hs = "-fx-background-color:white;-fx-padding:15px;-fx-background-radius:10px;"
                + "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.3),15,0,0,5);-fx-cursor:hand;";
            card.setStyle(ns); card.setPrefWidth(220); card.setAlignment(Pos.TOP_CENTER);
            card.setOnMouseEntered(e -> card.setStyle(hs));
            card.setOnMouseExited(e -> card.setStyle(ns));

            // --- Ảnh (ĐÃ GỘP BỌC THÉP CHỐNG NULL VÀ TRY-CATCH) ---
            ImageView imgView = new ImageView();
            String imagePath = p.getImagePath();
            if (imagePath != null && !imagePath.trim().isEmpty()) {
                try { imgView.setImage(new Image(imagePath, true)); }
                catch (Exception ex) { System.out.println("Lỗi tải ảnh cho: " + p.getName()); }
            } else {
                System.out.println("Cảnh báo: Sản phẩm thiếu link ảnh -> " + p.getName());
            }
            imgView.setFitWidth(180); imgView.setFitHeight(130); imgView.setPreserveRatio(true);
            Rectangle clip = new Rectangle(180, 130);
            clip.setArcWidth(15); clip.setArcHeight(15); imgView.setClip(clip);

            // --- Badge trạng thái (sẽ được cập nhật bởi globalCardTimer) ---
            StackPane imgContainer = new StackPane(imgView);
            imgContainer.setPrefHeight(130);
            Label badge = new Label();
            refreshBadge(badge, p);
            StackPane.setAlignment(badge, Pos.TOP_LEFT);
            StackPane.setMargin(badge, new Insets(5));
            imgContainer.getChildren().add(badge);

            // --- Tên, giá (BẢO VỆ NULL TÊN) ---
            String safeName = (p.getName() != null) ? p.getName() : "Sản phẩm ẩn danh";
            Label nameLabel = new Label(safeName);
            nameLabel.setStyle("-fx-font-weight:bold;-fx-font-size:15px;-fx-text-fill:#2c3e50;");
            Label bidSub = new Label("Current bid");
            bidSub.setStyle("-fx-text-fill:#7f8c8d;-fx-font-size:11px;");
            Label priceLabel = new Label(String.format("%,.0f VNĐ", p.getCurrentBid()));
            priceLabel.setStyle("-fx-text-fill:#e74c3c;-fx-font-weight:bold;-fx-font-size:16px;");

            // --- Label thời gian đếm ngược song song ---
            Label timeLabel = new Label();
            refreshTimeLabel(timeLabel, p);
            timeLabel.setStyle("-fx-text-fill:#2980b9;-fx-font-weight:bold;"
                + "-fx-background-color:#ebf5fb;-fx-padding:3px 8px;-fx-background-radius:5px;");

            VBox priceBox = new VBox(8); priceBox.setAlignment(Pos.CENTER);
            priceBox.getChildren().addAll(priceLabel, timeLabel);

            // --- Nút Bid ---
            Button btnBid = new Button(p.isEnded() ? "Xem kết quả" : "Bid Now");
            String btnNs = p.isEnded()
                ? "-fx-background-color:#7f8c8d;-fx-text-fill:white;-fx-font-weight:bold;-fx-cursor:hand;-fx-background-radius:5px;-fx-padding:8px 15px;"
                : "-fx-background-color:#27ae60;-fx-text-fill:white;-fx-font-weight:bold;-fx-cursor:hand;-fx-background-radius:5px;-fx-padding:8px 15px;";
            String btnHs = p.isEnded()
                ? "-fx-background-color:#95a5a6;-fx-text-fill:white;-fx-font-weight:bold;-fx-cursor:hand;-fx-background-radius:5px;-fx-padding:8px 15px;"
                : "-fx-background-color:#2ecc71;-fx-text-fill:white;-fx-font-weight:bold;-fx-cursor:hand;-fx-background-radius:5px;-fx-padding:8px 15px;";
            btnBid.setStyle(btnNs); btnBid.setMaxWidth(Double.MAX_VALUE);
            btnBid.setOnMouseEntered(e -> btnBid.setStyle(btnHs));
            btnBid.setOnMouseExited(e -> btnBid.setStyle(btnNs));
            btnBid.setOnAction(ev -> openAuctionRoom(p, ev));

            card.getChildren().addAll(imgContainer, nameLabel, bidSub, priceBox, btnBid);
            productContainer.getChildren().add(card);

            // Đăng ký để globalTimer cập nhật
            cardTimerLabels.add(timeLabel);
            cardBadgeLabels.add(badge);
            cardProducts.add(p);
        }

        // =====================================================================
        // GLOBAL TIMER: chạy mỗi giây, cập nhật TẤT CẢ thẻ đồng thời
        // =====================================================================
        globalCardTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            for (int i = 0; i < cardProducts.size(); i++) {
                Product p = cardProducts.get(i);
                if (p.isEnded()) continue; // Đã kết thúc thì bỏ qua
                refreshTimeLabel(cardTimerLabels.get(i), p);
                refreshBadge(cardBadgeLabels.get(i), p);
            }
        }));
        globalCardTimer.setCycleCount(Animation.INDEFINITE);
        globalCardTimer.play();
    }

    // =========================================================================
    // HELPER: Cập nhật label thời gian đếm ngược
    // =========================================================================
    private void refreshTimeLabel(Label label, Product p) {
        if (p.isEnded()) {
            label.setText("⏹ Đã kết thúc");
            label.setStyle("-fx-text-fill:#7f8c8d;-fx-font-weight:bold;"
                + "-fx-background-color:#ecf0f1;-fx-padding:3px 8px;-fx-background-radius:5px;");
            return;
        }
        int secs = p.getSecondsRemainingNow();
        if (secs <= 0) {
            p.setStatus("ended");
            label.setText("⏹ Đã kết thúc");
            label.setStyle("-fx-text-fill:#7f8c8d;-fx-font-weight:bold;"
                + "-fx-background-color:#ecf0f1;-fx-padding:3px 8px;-fx-background-radius:5px;");
        } else {
            int h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
            String text = h > 0
                ? String.format("⏱ %02d:%02d:%02d", h, m, s)
                : String.format("⏱ %02d:%02d", m, s);
            label.setText(text);
            // Đổi màu đỏ nếu sắp hết (dưới 60 giây)
            String color = secs <= 60 ? "#e74c3c" : "#2980b9";
            String bg    = secs <= 60 ? "#fdecea" : "#ebf5fb";
            label.setStyle("-fx-text-fill:" + color + ";-fx-font-weight:bold;"
                + "-fx-background-color:" + bg + ";-fx-padding:3px 8px;-fx-background-radius:5px;");
        }
    }

    // =========================================================================
    // HELPER: Cập nhật badge trạng thái
    // =========================================================================
    private void refreshBadge(Label badge, Product p) {
        if (p.isEnded()) {
            badge.setText("⚫ ĐÃ KẾT THÚC");
            badge.setStyle("-fx-background-color:#7f8c8d;-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-font-size:10px;-fx-padding:4px 8px;-fx-background-radius:5px;");
        } else if (p.getSecondsRemainingNow() <= 60) {
            badge.setText("🔴 SẮP KẾT THÚC");
            badge.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-font-size:10px;-fx-padding:4px 8px;-fx-background-radius:5px;");
        } else {
            badge.setText("🟢 ĐANG ĐẤU GIÁ");
            badge.setStyle("-fx-background-color:#27ae60;-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-font-size:10px;-fx-padding:4px 8px;-fx-background-radius:5px;");
        }
    }

    // =========================================================================
    // MỞ PHÒNG ĐẤU GIÁ
    // =========================================================================
    private void openAuctionRoom(Product p, javafx.event.ActionEvent ev) {
        // Dừng timer danh sách khi vào phòng
        if (globalCardTimer != null) globalCardTimer.stop();
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/client/view/AuctionRoom.fxml"));
            Parent root = loader.load();
            AuctionRoomController ctrl = loader.getController();
            ctrl.setProductData(p);
            Stage stage = (Stage) ((Node) ev.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            
            String safeName = (p.getName() != null) ? p.getName() : "Sản phẩm ẩn danh";
            stage.setTitle("Auction Room - " + safeName);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // =========================================================================
    // HOVER DANH MỤC
    // =========================================================================
    private void applySmoothHover(Button btn) {
        ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
        btn.setOnMouseEntered(e -> { st.setToX(1.1); st.setToY(1.1); st.playFromStart(); });
        btn.setOnMouseExited(e ->  { st.setToX(1.0); st.setToY(1.0); st.playFromStart(); });
    }

    // =========================================================================
    // ĐĂNG XUẤT / THÔNG TIN / ĐĂNG SẢN PHẨM
    // =========================================================================
    @FXML
    private void handleLogoutAction(javafx.event.ActionEvent event) {
        if (globalCardTimer != null) globalCardTimer.stop();
        try {
            FirebaseService.registeredUsername = null;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) productContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Online Auction System - Login");
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleShowInfo(javafx.event.ActionEvent event) {
        if (FirebaseService.registeredUsername != null) {
            String u = FirebaseService.registeredUsername;
            String ph = FirebaseService.getUserPhone(u);
            btnInfo.setText("Tên TK: " + u + "\nSĐT: " + ph);
            btnInfo.setStyle("-fx-background-color:transparent;-fx-text-fill:#27ae60;"
                + "-fx-font-weight:bold;-fx-cursor:default;");
        } else { btnInfo.setText("Chưa đăng nhập!"); }
    }

    @FXML
    private void handleGoToAddProduct(javafx.event.ActionEvent event) {
        if (globalCardTimer != null) globalCardTimer.stop();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/AddProduct.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Online Auction System - Add Product");
        } catch (IOException e) { e.printStackTrace(); }
    }
}