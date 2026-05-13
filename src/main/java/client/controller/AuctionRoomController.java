package client.controller;

import client.model.Product;
import client.service.FirebaseService;
import client.service.AuthService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AuctionRoomController {

    @FXML private ImageView imgProduct;
    @FXML private Label lblProductName;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTimer;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnSubmitBid;
    @FXML private ListView<String> listBidHistory;
    @FXML private Label lblSellerName;
    @FXML private Label lblProductDescription;
    @FXML private Button btnShowDetails;

    private Product currentProduct;
    private Timeline countdownTimer;
    private boolean auctionEnded = false;
    private boolean hasBids      = false;

    // =========================================================================
    // NHẬN DỮ LIỆU
    // =========================================================================
    public void setProductData(Product product) {
        this.currentProduct = product;
        this.auctionEnded   = false;
        this.hasBids        = false;
        if (currentProduct == null) return;

        lblProductName.setText(nvl(currentProduct.getName(), "Không có tên"));
        lblCurrentPrice.setText(formatVND(currentProduct.getCurrentBid()));

        if (lblSellerName != null)
            lblSellerName.setText(nvl(currentProduct.getSellerUsername(), "Khách vãng lai"));

        if (lblProductDescription != null) {
            String desc = currentProduct.getDescription();
            lblProductDescription.setText(
                (desc != null && !desc.isEmpty()) ? desc : "Người bán chưa cung cấp mô tả chi tiết.");
        }

        try { imgProduct.setImage(new Image(currentProduct.getImagePath(), true)); }
        catch (Exception e) { System.out.println("Lỗi tải ảnh: " + e.getMessage()); }

        String me = AuthService.currentUserEmail;
        boolean isSeller = me != null && me.equals(currentProduct.getSellerUsername());
        if (isSeller) {
            listBidHistory.getItems().add("Hệ thống: Bạn là chủ sở hữu của món hàng này.");
            btnSubmitBid.setDisable(true);
            txtBidAmount.setDisable(true);
            txtBidAmount.setPromptText("Bạn không thể tự đấu giá hàng của mình");
        }

        // Tải lịch sử đấu giá
        loadBidHistoryAsync();

        // ĐÃ FIX XUNG ĐỘT: Dùng cách khởi tạo của đồng đội
        listBidHistory.getItems().add("Hệ thống: Bắt đầu phiên đấu giá!");
        if (currentProduct.isEnded()) {
            auctionEnded = true;
            lblTimer.setText("Đã đóng!");
            txtBidAmount.setDisable(true);
            btnSubmitBid.setDisable(true);
            showAuctionResult();
        } else {
            startCountdownTimer();
        }

        Platform.runLater(() -> {
            try {
                Scene scene = btnSubmitBid.getScene();
                if (scene != null) {
                    String css = getClass().getResource("/client/css/auction_room.css").toExternalForm();
                    if (!scene.getStylesheets().contains(css)) scene.getStylesheets().add(css);
                }
            } catch (Exception ignored) {}
        });
    }

    // =========================================================================
    // TẢI LỊCH SỬ ĐẤU GIÁ TỪ FIREBASE (background thread)
    // =========================================================================
    private void loadBidHistoryAsync() {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                // TODO: Đồng đội cần chuyển hàm loadBidHistory sang HTTP
                // Hiện tại FirebaseService đã xóa hàm này ở bản sửa trước
                return null; 
            }
        };
        task.setOnSucceeded(e -> {
            List<String> history = task.getValue();
            if (history != null && !history.isEmpty()) {
                listBidHistory.getItems().add("── Lịch sử trả giá trước đó ──");
                listBidHistory.getItems().addAll(history);
                listBidHistory.getItems().add("─────────────────────────────");
            }
        });
        new Thread(task).start();
    }

    // =========================================================================
    // TIMER
    // =========================================================================
    private void startCountdownTimer() {
        updateTimerDisplay();
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (auctionEnded) return;
            int secsLeft = currentProduct.getSecondsRemainingNow();
            updateTimerDisplay(secsLeft);
            if (secsLeft <= 0) {
                countdownTimer.stop();
                if (!auctionEnded) {
                    auctionEnded = true;
                    endAuction();
                }
            }
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    // =========================================================================
    // KẾT THÚC ĐẤU GIÁ
    // =========================================================================
    private void endAuction() {
        lblTimer.setText("Đã đóng!");
        txtBidAmount.setDisable(true);
        btnSubmitBid.setDisable(true);
        FirebaseService.markAuctionEnded(currentProduct.getFirebaseKey());
        showAuctionResult();
    }

    private void showAuctionResult() {
        if (!hasBids || currentProduct.getHighestBidder() == null
                || currentProduct.getHighestBidder().isEmpty()) {
            listBidHistory.getItems().add(0, "Hệ thống: Phiên kết thúc — không có ai trả giá.");
            Platform.runLater(this::showNoWinnerPopup);
            return;
        }
        String winner = currentProduct.getHighestBidder();
        String phone  = nvl(currentProduct.getHighestBidderPhone(), "Chưa cập nhật");
        String email  = nvl(currentProduct.getHighestBidderEmail(), "Chưa cập nhật");
        String price  = formatVND(currentProduct.getCurrentBid());
        listBidHistory.getItems().add(0, "🏆 " + winner + " đã thắng với giá " + price);
        boolean isSeller = isCurrentUserSeller();
        Platform.runLater(() -> showWinnerPopup(winner, price, phone, email, isSeller));
    }

    // =========================================================================
    // GỬI GIÁ 
    // =========================================================================
    @FXML
    private void handleBidAction(ActionEvent event) {
        if (auctionEnded) {
            listBidHistory.getItems().add(0, "⚠️ Phiên đã kết thúc, không thể trả giá.");
            return;
        }
        String inputStr = txtBidAmount.getText().trim();
        if (inputStr.isEmpty()) {
            listBidHistory.getItems().add(0, "Lỗi: Vui lòng nhập số tiền.");
            return;
        }
        try {
            double bid     = Double.parseDouble(inputStr);
            double current = currentProduct.getCurrentBid();
            double step    = currentProduct.getStepPrice();

            if (step > 0 && !client.service.AuctionService.isValidBid(bid, current, step)) {
                listBidHistory.getItems().add(0,
                    "Lỗi: Giá tối thiểu là " + formatVND(current + step)
                    + " (bước giá: " + formatVND(step) + ")");
                return;
            } else if (bid <= current) {
                listBidHistory.getItems().add(0, "Lỗi: Giá phải cao hơn " + formatVND(current));
                return;
            }

            hasBids = true;
            currentProduct.setCurrentBid(bid);
            String me = AuthService.currentUserEmail;
            if (me != null) {
                currentProduct.setHighestBidder(me);
            }
            lblCurrentPrice.setText(formatVND(bid));

            String timeStr = nowTimestamp();
            listBidHistory.getItems().add(0,
                "✅ [" + timeStr + "] Bạn (" + nvl(me, "Ẩn danh")
                + ") trả: " + formatVND(bid));
            txtBidAmount.clear();

            // ĐÃ FIX XUNG ĐỘT: Dùng HTTP updateBid của đồng đội
            FirebaseService.updateBid(
                currentProduct.getFirebaseKey(), bid, me,
                currentProduct.getHighestBidderPhone(),
                currentProduct.getHighestBidderEmail()
            );

        } catch (NumberFormatException e) {
            listBidHistory.getItems().add(0, "Lỗi: Vui lòng nhập số hợp lệ (ví dụ: 500000)");
        }
    }

    // =========================================================================
    // QUAY LẠI
    // =========================================================================
    @FXML
    private void handleGoBack(ActionEvent event) {
        try {
            if (countdownTimer != null) countdownTimer.stop();
            // ĐÃ FIX XUNG ĐỘT: Xóa sự kiện listener cũ theo code của đồng đội
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/ProductList.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Online Auction System - Danh sách sản phẩm");
        } catch (Exception e) { e.printStackTrace(); }
    }

    // =========================================================================
    // XEM CHI TIẾT
    // =========================================================================
    @FXML
    private void handleShowDetails(ActionEvent event) {
        Stage d = new Stage();
        d.initModality(Modality.APPLICATION_MODAL);
        d.setTitle("Thông tin chi tiết sản phẩm");
        VBox layout = new VBox(15);
        layout.setStyle("-fx-padding:25;-fx-background-color:#ffffff;");
        layout.setAlignment(Pos.TOP_LEFT);

        String pName     = nvl(currentProduct.getName(),           "Không có");
        String pSeller   = nvl(currentProduct.getSellerUsername(), "Không có");
        String pCategory = nvl(currentProduct.getCategory(),       "Không có");
        String pDesc     = currentProduct.getDescription();
        if (pDesc == null || pDesc.isEmpty()) pDesc = "Người bán chưa cung cấp mô tả.";
        String pPrice = formatVND(currentProduct.getCurrentBid());
        String pStep  = formatVND(currentProduct.getStepPrice());
        String pTime  = nvl(currentProduct.getTimeRemaining(), "Không có");

        layout.getChildren().addAll(
            lbl("📋 CHI TIẾT SẢN PHẨM",
                "-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#2c3e50;"),
            lbl("🏷  Tên sản phẩm: " + pName,
                "-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#2c3e50;"),
            lbl("📂 Danh mục: " + pCategory,
                "-fx-font-size:15px;-fx-text-fill:#8e44ad;"),
            lbl("👤 Người đăng bán: " + pSeller,
                "-fx-font-size:15px;-fx-text-fill:#e67e22;"),
            lbl("💰 Giá hiện tại: " + pPrice,
                "-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#e74c3c;"),
            lbl("📈 Bước giá tối thiểu: " + pStep,
                "-fx-font-size:15px;-fx-text-fill:#27ae60;"),
            lbl("⏰ " + pTime,
                "-fx-font-size:14px;-fx-text-fill:#7f8c8d;"),
            lbl("📝 Mô tả chi tiết:",
                "-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#2c3e50;"),
            lbl(pDesc, "-fx-font-size:14px;-fx-text-fill:#34495e;")
        );

        Button btnClose = new Button("ĐÓNG");
        btnClose.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;"
                        + "-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;");
        btnClose.setOnAction(e -> d.close());
        layout.getChildren().add(btnClose);
        d.setScene(new Scene(layout, 480, 480));
        d.centerOnScreen();
        d.showAndWait();
    }

    // =========================================================================
    // TIỆN ÍCH
    // =========================================================================
    private boolean isCurrentUserSeller() {
        String me = AuthService.currentUserEmail;
        return me != null && currentProduct != null && me.equals(currentProduct.getSellerUsername());
    }

    private void updateTimerDisplay() {
        updateTimerDisplay(currentProduct.getSecondsRemainingNow());
    }

    private void updateTimerDisplay(int secs) {
        if (secs <= 0) { lblTimer.setText(" Còn lại: 00:00:00"); return; }
        lblTimer.setText(String.format(" Còn lại: %02d:%02d:%02d",
            secs / 3600, (secs % 3600) / 60, secs % 60));
    }

    private String nowTimestamp() {
        return new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault())
            .format(new Date());
    }

    private String formatVND(double amount) {
        return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(amount);
    }

    private String nvl(String s, String fallback) { return s != null ? s : fallback; }

    // =========================================================================
    // POPUP NGƯỜI THẮNG
    // =========================================================================
    private void showWinnerPopup(String winner, String price, String phone,
                                  String email, boolean isSeller) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color:white;-fx-padding:40;-fx-background-radius:15;"
                   + "-fx-border-radius:15;-fx-border-color:#f1c40f;-fx-border-width:4;");
        DropShadow sh = new DropShadow(); sh.setColor(Color.rgb(0,0,0,.25)); box.setEffect(sh);

        String pName = nvl(currentProduct != null ? currentProduct.getName() : null, "Sản phẩm");
        box.getChildren().addAll(
            lbl("🏆 KẾT THÚC PHIÊN ĐẤU GIÁ!",
                "-fx-font-size:26px;-fx-font-weight:bold;-fx-text-fill:#f39c12;"),
            lbl("Sản phẩm: " + pName,   "-fx-font-size:18px;-fx-text-fill:#34495e;"),
            lbl("Người chiến thắng: " + winner,
                "-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#27ae60;"),
            lbl("Với mức giá: " + price,
                "-fx-font-size:24px;-fx-font-weight:bold;-fx-text-fill:#e74c3c;")
        );
        if (isSeller) {
            VBox contact = new VBox(5); contact.setAlignment(Pos.CENTER);
            contact.setStyle("-fx-background-color:#fdf2e9;-fx-padding:15;"
                           + "-fx-background-radius:8;-fx-border-color:#e67e22;-fx-border-radius:8;");
            contact.getChildren().addAll(
                lbl("THÔNG TIN LIÊN HỆ NGƯỜI MUA:",
                    "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#d35400;"),
                lbl("📞 SĐT: " + phone,
                    "-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#2c3e50;"),
                lbl("📧 Email: " + email,
                    "-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#2c3e50;")
            );
            box.getChildren().add(contact);
        } else {
            box.getChildren().add(lbl(
                "🔒 Thông tin liên lạc đã được bảo mật.\n(Chỉ người đăng bán mới có thể xem)",
                "-fx-font-size:13px;-fx-text-fill:#7f8c8d;-fx-font-style:italic;"));
        }
        Button btnOk = new Button("XÁC NHẬN");
        btnOk.setStyle("-fx-background-color:#2980b9;-fx-text-fill:white;-fx-font-weight:bold;"
                     + "-fx-font-size:14px;-fx-padding:10 30;-fx-background-radius:5;-fx-cursor:hand;");
        btnOk.setOnAction(e -> popup.close());
        box.getChildren().add(btnOk);
        Scene sc = new Scene(box); sc.setFill(Color.TRANSPARENT);
        popup.setScene(sc); popup.centerOnScreen(); popup.showAndWait();
    }

    // =========================================================================
    // POPUP KHÔNG AI TRẢ GIÁ
    // =========================================================================
    private void showNoWinnerPopup() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);
        VBox box = new VBox(15); box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color:white;-fx-padding:40;-fx-background-radius:15;"
                   + "-fx-border-radius:15;-fx-border-color:#95a5a6;-fx-border-width:4;");
        DropShadow sh = new DropShadow(); sh.setColor(Color.rgb(0,0,0,.25)); box.setEffect(sh);
        String pName = nvl(currentProduct != null ? currentProduct.getName() : null, "Sản phẩm");
        box.getChildren().addAll(
            lbl("PHIÊN ĐẤU GIÁ ĐÃ KẾT THÚC",
                "-fx-font-size:24px;-fx-font-weight:bold;-fx-text-fill:#7f8c8d;"),
            lbl("Sản phẩm: " + pName,   "-fx-font-size:18px;-fx-text-fill:#34495e;"),
            lbl("Không có ai trả giá trong phiên.",
                "-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#e74c3c;")
        );
        Button btnOk = new Button("ĐÓNG");
        btnOk.setStyle("-fx-background-color:#7f8c8d;-fx-text-fill:white;-fx-font-weight:bold;"
                     + "-fx-font-size:14px;-fx-padding:10 30;-fx-background-radius:5;-fx-cursor:hand;");
        btnOk.setOnAction(e -> popup.close());
        box.getChildren().add(btnOk);
        Scene sc = new Scene(box); sc.setFill(Color.TRANSPARENT);
        popup.setScene(sc); popup.centerOnScreen(); popup.showAndWait();
    }

    @FXML private void handleMouseEnter() {
        if (btnShowDetails != null)
            btnShowDetails.setStyle(
                "-fx-background-color:#2980b9;-fx-text-fill:white;-fx-font-weight:bold;"
                + "-fx-padding:8px 15px;-fx-background-radius:5px;-fx-cursor:hand;"
                + "-fx-effect:dropshadow(three-pass-box,rgba(41,128,185,0.8),15,0,0,5);");
    }

    @FXML private void handleMouseExit() {
        if (btnShowDetails != null)
            btnShowDetails.setStyle(
                "-fx-background-color:#3498db;-fx-text-fill:white;-fx-font-weight:bold;"
                + "-fx-padding:8px 15px;-fx-background-radius:5px;-fx-cursor:hand;");
    }

    private Label lbl(String text, String style) {
        Label l = new Label(text); l.setStyle(style); l.setWrapText(true); return l;
    }
}