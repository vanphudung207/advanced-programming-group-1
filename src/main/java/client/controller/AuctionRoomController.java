package client.controller;

import client.model.Product;
import client.service.AuthService;
import client.service.FirebaseService;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
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
    private boolean resultShown = false;

    public void setProductData(Product product) {
        currentProduct = product;
        auctionEnded = false;
        resultShown = false;

        if (currentProduct == null) {
            return;
        }

        renderProduct();
        configureBidControls();
        loadBidHistoryAsync();
        refreshProductFromFirebaseAsync();

        Platform.runLater(() -> {
            try {
                Scene scene = btnSubmitBid.getScene();
                if (scene != null) {
                    String css = getClass().getResource("/client/css/auction_room.css").toExternalForm();
                    if (!scene.getStylesheets().contains(css)) {
                        scene.getStylesheets().add(css);
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }

    private void renderProduct() {
        lblProductName.setText(nvl(currentProduct.getName(), "Không có tên"));
        lblCurrentPrice.setText(formatVND(currentProduct.getCurrentBid()));

        if (lblSellerName != null) {
            lblSellerName.setText(nvl(currentProduct.getSellerUsername(), "Khách vãng lai"));
        }

        if (lblProductDescription != null) {
            String desc = currentProduct.getDescription();
            lblProductDescription.setText(
                desc != null && !desc.isBlank()
                    ? desc
                    : "Người bán chưa cung cấp mô tả chi tiết.");
        }

        String imagePath = currentProduct.getImagePath();
        if (imagePath != null && !imagePath.isBlank()) {
            try {
                imgProduct.setImage(new Image(imagePath, true));
            } catch (Exception e) {
                System.out.println("Lỗi tải ảnh: " + e.getMessage());
            }
        }
    }

    private void configureBidControls() {
        boolean isSeller = isCurrentUserSeller();
        boolean isEnded = currentProduct.isEnded();
        auctionEnded = isEnded;

        if (isSeller) {
            listBidHistory.getItems().add("Hệ thống: Bạn là chủ sở hữu của món hàng này.");
            txtBidAmount.setPromptText("Bạn không thể tự đấu giá hàng của mình");
        }

        txtBidAmount.setDisable(isSeller || isEnded);
        btnSubmitBid.setDisable(isSeller || isEnded);

        if (isEnded) {
            lblTimer.setText("Đã đóng!");
            showAuctionResult();
        } else {
            listBidHistory.getItems().add("Hệ thống: Bắt đầu phiên đấu giá!");
            startCountdownTimer();
        }
    }

    private void loadBidHistoryAsync() {
        Task<List<FirebaseService.BidHistoryEntry>> task = new Task<>() {
            @Override
            protected List<FirebaseService.BidHistoryEntry> call() {
                boolean[] hasBids = {false};
                return FirebaseService.loadBidHistorySync(currentProduct.getFirebaseKey(), hasBids);
            }
        };

        task.setOnSucceeded(e -> {
            List<FirebaseService.BidHistoryEntry> history = task.getValue();
            if (history == null || history.isEmpty()) {
                return;
            }
            listBidHistory.getItems().add(0, "── Lịch sử trả giá trước đó ──");
            for (FirebaseService.BidHistoryEntry entry : history) {
                listBidHistory.getItems().add(1, entry.getLine());
            }
            listBidHistory.getItems().add(1 + history.size(), "─────────────────────────────");
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void refreshProductFromFirebaseAsync() {
        Task<Product> task = new Task<>() {
            @Override
            protected Product call() {
                return FirebaseService.getProductByKey(currentProduct.getFirebaseKey());
            }
        };

        task.setOnSucceeded(e -> {
            Product latest = task.getValue();
            if (latest == null) {
                return;
            }
            applyLatestProduct(latest);
            if (currentProduct.isEnded() && !auctionEnded) {
                auctionEnded = true;
                endAuction(false);
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void applyLatestProduct(Product latest) {
        currentProduct.setCurrentBid(latest.getCurrentBid());
        currentProduct.setStatus(latest.getStatus());
        currentProduct.setEndTime(latest.getEndTime());
        currentProduct.setDescription(latest.getDescription());
        currentProduct.setHighestBidder(latest.getHighestBidder());
        currentProduct.setHighestBidderPhone(latest.getHighestBidderPhone());
        currentProduct.setHighestBidderEmail(latest.getHighestBidderEmail());
        renderProduct();
    }

    private void startCountdownTimer() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        updateTimerDisplay();
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (auctionEnded || currentProduct == null) {
                return;
            }

            int secsLeft = currentProduct.getSecondsRemainingNow();
            updateTimerDisplay(secsLeft);
            if (secsLeft <= 0) {
                countdownTimer.stop();
                auctionEnded = true;
                syncFinalStateAndEnd();
            }
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private void syncFinalStateAndEnd() {
        Task<Product> task = new Task<>() {
            @Override
            protected Product call() {
                return FirebaseService.getProductByKey(currentProduct.getFirebaseKey());
            }
        };

        task.setOnSucceeded(e -> {
            Product latest = task.getValue();
            if (latest != null) {
                applyLatestProduct(latest);
                if (!latest.isEnded() && latest.getEndTime() > System.currentTimeMillis()) {
                    auctionEnded = false;
                    startCountdownTimer();
                    if (!isCurrentUserSeller()) {
                        txtBidAmount.setDisable(false);
                        btnSubmitBid.setDisable(false);
                    }
                    return;
                }
            }
            endAuction(true);
        });

        task.setOnFailed(e -> endAuction(true));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void endAuction(boolean updateFirebase) {
        lblTimer.setText("Đã đóng!");
        txtBidAmount.setDisable(true);
        btnSubmitBid.setDisable(true);
        currentProduct.setStatus("ended");

        if (updateFirebase) {
            FirebaseService.markAuctionEnded(currentProduct.getFirebaseKey());
        }
        showAuctionResult();
    }

    private void showAuctionResult() {
        if (resultShown) {
            return;
        }
        resultShown = true;

        if (currentProduct.getHighestBidder() == null
                || currentProduct.getHighestBidder().isBlank()) {
            listBidHistory.getItems().add(0, "Hệ thống: Phiên kết thúc - không có ai trả giá.");
            Platform.runLater(this::showNoWinnerPopup);
            return;
        }

        String winner = currentProduct.getHighestBidder();
        String phone = nvl(currentProduct.getHighestBidderPhone(), "Chưa cập nhật");
        String email = nvl(currentProduct.getHighestBidderEmail(), "Chưa cập nhật");
        String price = formatVND(currentProduct.getCurrentBid());
        listBidHistory.getItems().add(0, winner + " đã thắng với giá " + price);
        Platform.runLater(() -> showWinnerPopup(winner, price, phone, email, isCurrentUserSeller()));
    }

    @FXML
    private void handleBidAction(ActionEvent event) {
        if (auctionEnded) {
            listBidHistory.getItems().add(0, "Phiên đã kết thúc, không thể trả giá.");
            return;
        }

        String bidder = AuthService.currentUserEmail;
        if (bidder == null || bidder.isBlank()) {
            listBidHistory.getItems().add(0, "Lỗi: Bạn cần đăng nhập để trả giá.");
            return;
        }

        if (isCurrentUserSeller()) {
            listBidHistory.getItems().add(0, "Lỗi: Bạn không thể tự đấu giá hàng của mình.");
            return;
        }

        String inputStr = txtBidAmount.getText().trim();
        if (inputStr.isEmpty()) {
            listBidHistory.getItems().add(0, "Lỗi: Vui lòng nhập số tiền.");
            return;
        }

        double bid;
        try {
            bid = Double.parseDouble(inputStr);
        } catch (NumberFormatException e) {
            listBidHistory.getItems().add(0, "Lỗi: Vui lòng nhập số hợp lệ, ví dụ 500000.");
            return;
        }

        double current = currentProduct.getCurrentBid();
        double step = currentProduct.getStepPrice();
        if (step > 0 && !client.service.AuctionService.isValidBid(bid, current, step)) {
            listBidHistory.getItems().add(0,
                "Lỗi: Giá tối thiểu là " + formatVND(current + step)
                    + " (bước giá: " + formatVND(step) + ")");
            return;
        }
        if (bid <= current) {
            listBidHistory.getItems().add(0, "Lỗi: Giá phải cao hơn " + formatVND(current));
            return;
        }

        btnSubmitBid.setDisable(true);
        txtBidAmount.setDisable(true);

        Task<FirebaseService.BidResult> task = new Task<>() {
            @Override
            protected FirebaseService.BidResult call() {
                String phone = FirebaseService.getUserPhone(bidder);
                return FirebaseService.placeBid(
                    currentProduct.getFirebaseKey(),
                    bid,
                    bidder,
                    phone,
                    bidder
                );
            }
        };

        task.setOnSucceeded(e -> {
            FirebaseService.BidResult result = task.getValue();
            if (result.success) {
                currentProduct.setCurrentBid(result.currentBid);
                currentProduct.setHighestBidder(result.highestBidder);
                currentProduct.setHighestBidderPhone(result.highestBidderPhone);
                currentProduct.setHighestBidderEmail(result.highestBidderEmail);
                if (result.endTime > 0) {
                    currentProduct.setEndTime(result.endTime);
                }

                lblCurrentPrice.setText(formatVND(result.currentBid));
                listBidHistory.getItems().add(0,
                    "[" + FirebaseService.formatTimestamp(System.currentTimeMillis()) + "] "
                        + result.highestBidder + " trả: " + formatVND(result.currentBid));
                if (result.extended) {
                    listBidHistory.getItems().add(0, "Anti-sniping: cộng thêm 10 giây.");
                }
                txtBidAmount.clear();
            } else {
                listBidHistory.getItems().add(0, "Lỗi: " + result.message);
                if ("Phiên đấu giá đã kết thúc.".equals(result.message)) {
                    auctionEnded = true;
                    endAuction(false);
                }
            }

            if (!auctionEnded && !isCurrentUserSeller()) {
                btnSubmitBid.setDisable(false);
                txtBidAmount.setDisable(false);
            }
        });

        task.setOnFailed(e -> {
            listBidHistory.getItems().add(0, "Lỗi: Không thể gửi giá. Vui lòng thử lại.");
            if (!auctionEnded && !isCurrentUserSeller()) {
                btnSubmitBid.setDisable(false);
                txtBidAmount.setDisable(false);
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleGoBack(ActionEvent event) {
        try {
            if (countdownTimer != null) {
                countdownTimer.stop();
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/ProductList.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Online Auction System - Danh sách sản phẩm");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowDetails(ActionEvent event) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Thông tin chi tiết sản phẩm");

        VBox layout = new VBox(15);
        layout.setStyle("-fx-padding:25;-fx-background-color:#ffffff;");
        layout.setAlignment(Pos.TOP_LEFT);

        String pName = nvl(currentProduct.getName(), "Không có");
        String pSeller = nvl(currentProduct.getSellerUsername(), "Không có");
        String pCategory = nvl(currentProduct.getCategory(), "Không có");
        String pDesc = currentProduct.getDescription();
        if (pDesc == null || pDesc.isBlank()) {
            pDesc = "Người bán chưa cung cấp mô tả.";
        }

        layout.getChildren().addAll(
            lbl("CHI TIẾT SẢN PHẨM", "-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#2c3e50;"),
            lbl("Tên sản phẩm: " + pName, "-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#2c3e50;"),
            lbl("Danh mục: " + pCategory, "-fx-font-size:15px;-fx-text-fill:#8e44ad;"),
            lbl("Người đăng bán: " + pSeller, "-fx-font-size:15px;-fx-text-fill:#e67e22;"),
            lbl("Giá hiện tại: " + formatVND(currentProduct.getCurrentBid()),
                "-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#e74c3c;"),
            lbl("Bước giá tối thiểu: " + formatVND(currentProduct.getStepPrice()),
                "-fx-font-size:15px;-fx-text-fill:#27ae60;"),
            lbl(nvl(currentProduct.getTimeRemaining(), "Không có"),
                "-fx-font-size:14px;-fx-text-fill:#7f8c8d;"),
            lbl("Mô tả chi tiết:", "-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#2c3e50;"),
            lbl(pDesc, "-fx-font-size:14px;-fx-text-fill:#34495e;")
        );

        Button btnClose = new Button("ĐÓNG");
        btnClose.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;"
            + "-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;");
        btnClose.setOnAction(e -> dialog.close());
        layout.getChildren().add(btnClose);

        dialog.setScene(new Scene(layout, 480, 480));
        dialog.centerOnScreen();
        dialog.showAndWait();
    }

    private boolean isCurrentUserSeller() {
        String me = AuthService.currentUserEmail;
        return me != null && currentProduct != null && me.equals(currentProduct.getSellerUsername());
    }

    private void updateTimerDisplay() {
        updateTimerDisplay(currentProduct.getSecondsRemainingNow());
    }

    private void updateTimerDisplay(int secs) {
        if (secs <= 0) {
            lblTimer.setText("Còn lại: 00:00:00");
            return;
        }
        lblTimer.setText(String.format("Còn lại: %02d:%02d:%02d",
            secs / 3600, (secs % 3600) / 60, secs % 60));
    }

    private String formatVND(double amount) {
        return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(amount);
    }

    private String nvl(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private void showWinnerPopup(String winner, String price, String phone,
                                 String email, boolean isSeller) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);

        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color:white;-fx-padding:40;-fx-background-radius:15;"
            + "-fx-border-radius:15;-fx-border-color:#f1c40f;-fx-border-width:4;");
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.25));
        box.setEffect(shadow);

        String pName = nvl(currentProduct != null ? currentProduct.getName() : null, "Sản phẩm");
        box.getChildren().addAll(
            lbl("KẾT THÚC PHIÊN ĐẤU GIÁ!", "-fx-font-size:26px;-fx-font-weight:bold;-fx-text-fill:#f39c12;"),
            lbl("Sản phẩm: " + pName, "-fx-font-size:18px;-fx-text-fill:#34495e;"),
            lbl("Người chiến thắng: " + winner, "-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#27ae60;"),
            lbl("Với mức giá: " + price, "-fx-font-size:24px;-fx-font-weight:bold;-fx-text-fill:#e74c3c;")
        );

        if (isSeller) {
            VBox contact = new VBox(5);
            contact.setAlignment(Pos.CENTER);
            contact.setStyle("-fx-background-color:#fdf2e9;-fx-padding:15;"
                + "-fx-background-radius:8;-fx-border-color:#e67e22;-fx-border-radius:8;");
            contact.getChildren().addAll(
                lbl("THÔNG TIN LIÊN HỆ NGƯỜI MUA:", "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#d35400;"),
                lbl("SĐT: " + phone, "-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#2c3e50;"),
                lbl("Email: " + email, "-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#2c3e50;")
            );
            box.getChildren().add(contact);
        } else {
            box.getChildren().add(lbl(
                "Thông tin liên lạc được ẩn. Chỉ người đăng bán mới có thể xem.",
                "-fx-font-size:13px;-fx-text-fill:#7f8c8d;-fx-font-style:italic;"));
        }

        Button btnOk = new Button("XÁC NHẬN");
        btnOk.setStyle("-fx-background-color:#2980b9;-fx-text-fill:white;-fx-font-weight:bold;"
            + "-fx-font-size:14px;-fx-padding:10 30;-fx-background-radius:5;-fx-cursor:hand;");
        btnOk.setOnAction(e -> popup.close());
        box.getChildren().add(btnOk);

        Scene scene = new Scene(box);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.centerOnScreen();
        popup.showAndWait();
    }

    private void showNoWinnerPopup() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);

        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color:white;-fx-padding:40;-fx-background-radius:15;"
            + "-fx-border-radius:15;-fx-border-color:#95a5a6;-fx-border-width:4;");
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.25));
        box.setEffect(shadow);

        String pName = nvl(currentProduct != null ? currentProduct.getName() : null, "Sản phẩm");
        box.getChildren().addAll(
            lbl("PHIÊN ĐẤU GIÁ ĐÃ KẾT THÚC", "-fx-font-size:24px;-fx-font-weight:bold;-fx-text-fill:#7f8c8d;"),
            lbl("Sản phẩm: " + pName, "-fx-font-size:18px;-fx-text-fill:#34495e;"),
            lbl("Không có ai trả giá trong phiên.", "-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#e74c3c;")
        );

        Button btnOk = new Button("ĐÓNG");
        btnOk.setStyle("-fx-background-color:#7f8c8d;-fx-text-fill:white;-fx-font-weight:bold;"
            + "-fx-font-size:14px;-fx-padding:10 30;-fx-background-radius:5;-fx-cursor:hand;");
        btnOk.setOnAction(e -> popup.close());
        box.getChildren().add(btnOk);

        Scene scene = new Scene(box);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.centerOnScreen();
        popup.showAndWait();
    }

    @FXML
    private void handleMouseEnter() {
        if (btnShowDetails != null) {
            btnShowDetails.setStyle(
                "-fx-background-color:#2980b9;-fx-text-fill:white;-fx-font-weight:bold;"
                    + "-fx-padding:8px 15px;-fx-background-radius:5px;-fx-cursor:hand;"
                    + "-fx-effect:dropshadow(three-pass-box,rgba(41,128,185,0.8),15,0,0,5);");
        }
    }

    @FXML
    private void handleMouseExit() {
        if (btnShowDetails != null) {
            btnShowDetails.setStyle(
                "-fx-background-color:#3498db;-fx-text-fill:white;-fx-font-weight:bold;"
                    + "-fx-padding:8px 15px;-fx-background-radius:5px;-fx-cursor:hand;");
        }
    }

    private Label lbl(String text, String style) {
        Label label = new Label(text);
        label.setStyle(style);
        label.setWrapText(true);
        return label;
    }
}
