package client.controller;

import client.model.Product;
import client.service.AuthService;
import client.service.FirebaseService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProductListController {

    @FXML private Button btnInfo;
    @FXML private Label lblDateTime;
    @FXML private FlowPane productContainer;
    @FXML private HBox categoryBox;
    @FXML private Button btnSearch;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbSort;
    @FXML private Button btnNotificationBell;
    @FXML private Label lblNotificationBadge;

    private List<Product> currentDisplayedProducts = new ArrayList<>();
    private Timeline globalCardTimer;
    private Timeline notificationTimer;
    private boolean notificationLoadInFlight = false;
    private String selectedCategory = null;
    private String allProductsCategory = null;
    private final List<ProductCardBinding> currentCardBindings = new ArrayList<>();
    private final Map<String, Image> imageCache = new HashMap<>();
    private final List<FirebaseService.WinnerNotification> winnerNotifications = new ArrayList<>();

    private static class ProductCardBinding {
        private final Product product;
        private final Label priceLabel;
        private final Label timeLabel;
        private final Label badge;
        private final Button bidButton;

        private ProductCardBinding(Product product, Label priceLabel, Label timeLabel,
                                   Label badge, Button bidButton) {
            this.product = product;
            this.priceLabel = priceLabel;
            this.timeLabel = timeLabel;
            this.badge = badge;
            this.bidButton = bidButton;
        }
    }

    @FXML
    public void initialize() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a | dd MMM, yyyy");
        Timeline clock = new Timeline(
            new KeyFrame(Duration.ZERO, e -> lblDateTime.setText(LocalDateTime.now().format(formatter))),
            new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();

        if (cbSort != null) {
            cbSort.getItems().addAll(
                "Mặc định",
                "Đang đấu giá trước",
                "Giá: Thấp đến Cao",
                "Giá: Cao đến Thấp"
            );
            cbSort.setValue("Mặc định");
        }

        fetchDataAsync();
        startAutoReload();
        startNotificationPolling();

        if (categoryBox != null) {
            for (Node node : categoryBox.getChildren()) {
                if (node instanceof Button) {
                    Button button = (Button) node;
                    if (allProductsCategory == null) {
                        allProductsCategory = button.getText();
                        selectedCategory = allProductsCategory;
                    }
                    applySmoothHover(button);
                }
            }
            updateCategoryButtonStyles();
        }
        if (btnSearch != null) {
            applySmoothHover(btnSearch);
        }
    }

    private void startAutoReload() {
        Thread reloadThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(5_000);
                    String category = selectedCategory;
                    List<Product> freshData = loadProductsByCategory(category);
                    Platform.runLater(() -> {
                        boolean searchEmpty = txtSearch == null || txtSearch.getText().trim().isEmpty();
                        boolean defaultSort = cbSort == null || "Mặc định".equals(cbSort.getValue());
                        boolean sameCategory = sameCategory(category, selectedCategory);
                        if (searchEmpty && defaultSort && sameCategory && productContainer != null) {
                            applyAutoReloadData(freshData);
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        reloadThread.setDaemon(true);
        reloadThread.start();
    }

    private void startNotificationPolling() {
        if (btnNotificationBell != null) {
            btnNotificationBell.setTooltip(new Tooltip("Thông báo kết quả đấu giá"));
        }

        refreshWinnerNotificationsAsync();
        if (notificationTimer != null) {
            notificationTimer.stop();
        }
        notificationTimer = new Timeline(
            new KeyFrame(Duration.seconds(30), e -> refreshWinnerNotificationsAsync())
        );
        notificationTimer.setCycleCount(Animation.INDEFINITE);
        notificationTimer.play();
    }

    private void refreshWinnerNotificationsAsync() {
        String user = currentUserEmail();
        if (user == null || user.isBlank() || notificationLoadInFlight) {
            updateNotificationBadge();
            return;
        }

        notificationLoadInFlight = true;
        Thread thread = new Thread(() -> {
            List<FirebaseService.WinnerNotification> data = new ArrayList<>();
            try {
                data = FirebaseService.getWinnerNotificationsForSeller(user);
            } catch (Exception e) {
                e.printStackTrace();
            }

            List<FirebaseService.WinnerNotification> finalData = data;
            Platform.runLater(() -> {
                notificationLoadInFlight = false;
                winnerNotifications.clear();
                winnerNotifications.addAll(finalData);
                updateNotificationBadge();
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void updateNotificationBadge() {
        if (lblNotificationBadge == null) {
            return;
        }

        long unreadCount = winnerNotifications.stream()
            .filter(notification -> !notification.isSeen())
            .count();
        boolean hasUnread = unreadCount > 0;
        lblNotificationBadge.setVisible(hasUnread);
        lblNotificationBadge.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));

        if (btnNotificationBell != null) {
            String tooltip = hasUnread
                ? unreadCount + " thông báo kết quả đấu giá mới"
                : "Thông báo kết quả đấu giá";
            btnNotificationBell.setTooltip(new Tooltip(tooltip));
        }
    }

    @FXML
    private void handleShowNotifications(ActionEvent event) {
        showNotificationsDialog();
        markUnreadNotificationsSeen();
    }

    private void showNotificationsDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (btnNotificationBell != null && btnNotificationBell.getScene() != null) {
            dialog.initOwner(btnNotificationBell.getScene().getWindow());
        }
        dialog.setTitle("Thông báo đấu giá");

        VBox root = new VBox(14);
        root.setPadding(new Insets(22));
        root.setStyle("-fx-background-color:#f8fafc;");

        Label title = new Label("Thông báo kết quả đấu giá");
        title.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#1f2937;");

        Label subtitle = new Label("Các phiên đã kết thúc có người thắng thuộc sản phẩm bạn đăng.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size:13px;-fx-text-fill:#64748b;");

        VBox notificationList = new VBox(10);
        if (winnerNotifications.isEmpty()) {
            Label empty = new Label("Chưa có thông báo người thắng đấu giá.");
            empty.setStyle("-fx-text-fill:#64748b;-fx-font-size:15px;-fx-padding:30px;");
            empty.setAlignment(Pos.CENTER);
            empty.setMaxWidth(Double.MAX_VALUE);
            notificationList.getChildren().add(empty);
        } else {
            for (FirebaseService.WinnerNotification notification : winnerNotifications) {
                notificationList.getChildren().add(createNotificationCard(notification));
            }
        }

        ScrollPane scrollPane = new ScrollPane(notificationList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(340);
        scrollPane.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;");

        Button closeButton = new Button("Đóng");
        closeButton.setStyle("-fx-background-color:#1f2937;-fx-text-fill:white;-fx-font-weight:bold;"
            + "-fx-padding:8px 22px;-fx-background-radius:6px;-fx-cursor:hand;");
        closeButton.setOnAction(e -> dialog.close());

        HBox footer = new HBox(closeButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, subtitle, scrollPane, footer);

        Scene scene = new Scene(root, 560, 500);
        dialog.setScene(scene);
        dialog.centerOnScreen();
        dialog.showAndWait();
    }

    private VBox createNotificationCard(FirebaseService.WinnerNotification notification) {
        boolean unread = !notification.isSeen();
        VBox card = new VBox(7);
        String background = unread ? "#fff7ed" : "#ffffff";
        String border = unread ? "#fb923c" : "#e2e8f0";
        card.setStyle("-fx-background-color:" + background + ";"
            + "-fx-padding:14px;"
            + "-fx-background-radius:8px;"
            + "-fx-border-color:" + border + ";"
            + "-fx-border-radius:8px;"
            + "-fx-effect:dropshadow(three-pass-box,rgba(15,23,42,0.06),8,0,0,2);");

        Label productName = new Label(nvl(notification.getProductName(), "Sản phẩm"));
        productName.setWrapText(true);
        productName.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#111827;");

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        titleRow.getChildren().addAll(productName, spacer);
        if (unread) {
            Label newBadge = new Label("Mới");
            newBadge.setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;-fx-font-weight:bold;"
                + "-fx-font-size:11px;-fx-padding:2px 8px;-fx-background-radius:10px;");
            titleRow.getChildren().add(newBadge);
        }

        Label winner = new Label("Người thắng: " + nvl(notification.getWinner(), "Chưa có"));
        winner.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#166534;");

        Label price = new Label("Giá chốt: " + formatVND(notification.getFinalPrice()));
        price.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#e64a19;");

        Label contact = new Label("Liên hệ: "
            + nvl(notification.getWinnerPhone(), "Chưa cập nhật")
            + " | " + nvl(notification.getWinnerEmail(), "Chưa cập nhật"));
        contact.setWrapText(true);
        contact.setStyle("-fx-font-size:13px;-fx-text-fill:#334155;");

        Label endedAt = new Label("Kết thúc: " + FirebaseService.formatTimestamp(notification.getEndedAt()));
        endedAt.setStyle("-fx-font-size:12px;-fx-text-fill:#64748b;");

        card.getChildren().addAll(titleRow, winner, price, contact, endedAt);
        return card;
    }

    private void markUnreadNotificationsSeen() {
        String user = currentUserEmail();
        if (user == null || user.isBlank()) {
            return;
        }

        List<String> unreadKeys = new ArrayList<>();
        for (FirebaseService.WinnerNotification notification : winnerNotifications) {
            if (!notification.isSeen()
                    && notification.getProductKey() != null
                    && !notification.getProductKey().isBlank()) {
                unreadKeys.add(notification.getProductKey());
            }
        }

        if (unreadKeys.isEmpty()) {
            return;
        }

        if (lblNotificationBadge != null) {
            lblNotificationBadge.setVisible(false);
        }

        Thread thread = new Thread(() -> {
            FirebaseService.markWinnerNotificationsSeen(user, unreadKeys);
            Platform.runLater(this::refreshWinnerNotificationsAsync);
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void stopNotificationPolling() {
        if (notificationTimer != null) {
            notificationTimer.stop();
            notificationTimer = null;
        }
    }

    private void fetchDataAsync() {
        showLoadingState();
        String category = selectedCategory;
        Thread thread = new Thread(() -> {
            List<Product> data = loadProductsByCategory(category);
            Platform.runLater(() -> {
                currentDisplayedProducts = data;
                loadProductsToGrid(currentDisplayedProducts);
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleSortAction(ActionEvent event) {
        if (currentDisplayedProducts == null || currentDisplayedProducts.isEmpty()) {
            return;
        }

        List<Product> sorted = new ArrayList<>(currentDisplayedProducts);
        String value = cbSort != null ? cbSort.getValue() : "Mặc định";

        if ("Đang đấu giá trước".equals(value)) {
            sorted.sort((a, b) -> {
                int statusCompare = Boolean.compare(a.isEnded(), b.isEnded());
                if (statusCompare != 0) {
                    return statusCompare;
                }
                return !a.isEnded()
                    ? Long.compare(a.getEndTime(), b.getEndTime())
                    : Long.compare(b.getEndTime(), a.getEndTime());
            });
        } else if ("Giá: Thấp đến Cao".equals(value)) {
            sorted.sort((a, b) -> Double.compare(a.getCurrentBid(), b.getCurrentBid()));
        } else if ("Giá: Cao đến Thấp".equals(value)) {
            sorted.sort((a, b) -> Double.compare(b.getCurrentBid(), a.getCurrentBid()));
        }

        loadProductsToGrid(sorted);
    }

    @FXML
    private void handleSearchAction(ActionEvent event) {
        String keyword = txtSearch == null ? "" : txtSearch.getText().trim();
        showLoadingState();

        Thread thread = new Thread(() -> {
            List<Product> data = keyword.isEmpty()
                ? FirebaseService.getAllProducts()
                : FirebaseService.searchProducts(keyword);
            Platform.runLater(() -> {
                currentDisplayedProducts = data;
                if (cbSort != null) {
                    cbSort.setValue("Mặc định");
                }
                loadProductsToGrid(currentDisplayedProducts);
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleFilterCategory(ActionEvent event) {
        String category = ((Button) event.getSource()).getText();
        selectedCategory = category;
        updateCategoryButtonStyles();
        showLoadingState();

        Thread thread = new Thread(() -> {
            List<Product> data = loadProductsByCategory(category);
            Platform.runLater(() -> {
                currentDisplayedProducts = data;
                if (txtSearch != null) {
                    txtSearch.clear();
                }
                if (cbSort != null) {
                    cbSort.setValue("Mặc định");
                }
                loadProductsToGrid(currentDisplayedProducts);
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleReloadAction(ActionEvent event) {
        if (txtSearch != null) {
            txtSearch.clear();
        }
        if (cbSort != null) {
            cbSort.setValue("Mặc định");
        }
        fetchDataAsync();
    }

    private List<Product> loadProductsByCategory(String category) {
        return isAllProductsCategory(category)
            ? FirebaseService.getAllProducts()
            : FirebaseService.getProductsByCategory(category);
    }

    private boolean isAllProductsCategory(String category) {
        return category == null
            || allProductsCategory == null
            || category.equals(allProductsCategory);
    }

    private boolean sameCategory(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private void updateCategoryButtonStyles() {
        if (categoryBox == null) {
            return;
        }

        int buttonCount = 0;
        for (Node node : categoryBox.getChildren()) {
            if (node instanceof Button) {
                buttonCount++;
            }
        }

        int buttonIndex = 0;
        for (Node node : categoryBox.getChildren()) {
            if (!(node instanceof Button)) {
                continue;
            }

            Button button = (Button) node;
            boolean active = sameCategory(button.getText(), selectedCategory);
            boolean last = buttonIndex == buttonCount - 1;
            button.setStyle(categoryButtonStyle(active, last));
            buttonIndex++;
        }
    }

    private String categoryButtonStyle(boolean active, boolean last) {
        String rightBorder = last ? "0" : "1px";
        String background = active ? "#fff5f0" : "transparent";
        String textColor = active ? "#e64a19" : "#2c3e50";
        String bottomBorder = active ? "#e64a19" : "#bdc3c7";
        String bottomWidth = active ? "3px" : "0";

        return "-fx-background-color:" + background + ";"
            + "-fx-text-fill:" + textColor + ";"
            + "-fx-font-size:15px;"
            + "-fx-font-weight:bold;"
            + "-fx-cursor:hand;"
            + "-fx-padding:12px;"
            + "-fx-border-color:transparent #bdc3c7 " + bottomBorder + " transparent;"
            + "-fx-border-width:0 " + rightBorder + " " + bottomWidth + " 0;";
    }

    private void applyAutoReloadData(List<Product> freshData) {
        if (freshData == null) {
            return;
        }

        if (requiresGridRender(freshData)) {
            currentDisplayedProducts = freshData;
            loadProductsToGrid(currentDisplayedProducts);
            return;
        }

        for (int i = 0; i < freshData.size(); i++) {
            Product current = currentDisplayedProducts.get(i);
            Product fresh = freshData.get(i);
            copyProductState(current, fresh);
            if (i < currentCardBindings.size()) {
                refreshCardBinding(currentCardBindings.get(i));
            }
        }
    }

    private boolean requiresGridRender(List<Product> freshData) {
        if (currentDisplayedProducts == null
                || currentDisplayedProducts.size() != freshData.size()
                || currentCardBindings.size() != freshData.size()) {
            return true;
        }

        for (int i = 0; i < freshData.size(); i++) {
            Product current = currentDisplayedProducts.get(i);
            Product fresh = freshData.get(i);
            if (!sameText(productIdentity(current), productIdentity(fresh))
                    || !sameText(current.getName(), fresh.getName())
                    || !sameText(current.getImagePath(), fresh.getImagePath())) {
                return true;
            }
        }
        return false;
    }

    private String productIdentity(Product product) {
        if (product == null) {
            return "";
        }
        if (product.getFirebaseKey() != null && !product.getFirebaseKey().isBlank()) {
            return product.getFirebaseKey();
        }
        if (product.getId() != null && !product.getId().isBlank()) {
            return product.getId();
        }
        return product.getName() != null ? product.getName() : "";
    }

    private boolean sameText(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private void copyProductState(Product target, Product source) {
        target.setId(source.getId());
        target.setFirebaseKey(source.getFirebaseKey());
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setCurrentBid(source.getCurrentBid());
        target.setTimeRemaining(source.getTimeRemaining());
        target.setImagePath(source.getImagePath());
        target.setSellerUsername(source.getSellerUsername());
        target.setCategory(source.getCategory());
        target.setStepPrice(source.getStepPrice());
        target.setStatus(source.getStatus());
        target.setEndTime(source.getEndTime());
        target.setHighestBidder(source.getHighestBidder());
        target.setHighestBidderPhone(source.getHighestBidderPhone());
        target.setHighestBidderEmail(source.getHighestBidderEmail());
    }

    private void refreshCardBinding(ProductCardBinding binding) {
        binding.priceLabel.setText(formatVND(binding.product.getCurrentBid()));
        refreshTimeLabel(binding.timeLabel, binding.product);
        refreshBadge(binding.badge, binding.product);

        String buttonText = binding.product.isEnded() ? "Xem kết quả" : "Bid Now";
        binding.bidButton.setText(buttonText);
        binding.bidButton.setStyle(bidButtonStyle(binding.product, false));
    }

    private void showLoadingState() {
        if (productContainer == null) {
            return;
        }
        if (globalCardTimer != null) {
            globalCardTimer.stop();
        }
        currentCardBindings.clear();
        productContainer.getChildren().clear();
        productContainer.setAlignment(Pos.CENTER);
        Label loading = new Label("Đang tải dữ liệu...");
        loading.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#7f8c8d;");
        productContainer.getChildren().add(loading);
    }

    private void loadProductsToGrid(List<Product> products) {
        if (globalCardTimer != null) {
            globalCardTimer.stop();
        }
        productContainer.getChildren().clear();

        if (products == null || products.isEmpty()) {
            productContainer.setAlignment(Pos.CENTER);
            VBox empty = new VBox(15);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(100, 0, 100, 0));

            Label icon = new Label("Không có sản phẩm");
            icon.setStyle("-fx-font-size:24px;-fx-font-weight:bold;-fx-text-fill:#7f8c8d;");
            Label message = new Label("Hãy thử tải lại, tìm từ khóa khác hoặc chọn danh mục khác.");
            message.setStyle("-fx-font-size:14px;-fx-text-fill:#95a5a6;");

            empty.getChildren().addAll(icon, message);
            productContainer.getChildren().add(empty);
            return;
        }

        productContainer.setAlignment(Pos.TOP_CENTER);
        for (Product product : products) {
            VBox card = createProductCard(product);
            productContainer.getChildren().add(card);
        }

        globalCardTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            for (ProductCardBinding binding : currentCardBindings) {
                refreshTimeLabel(binding.timeLabel, binding.product);
                refreshBadge(binding.badge, binding.product);
            }
        }));
        globalCardTimer.setCycleCount(Animation.INDEFINITE);
        globalCardTimer.play();
    }

    private VBox createProductCard(Product product) {
        VBox card = new VBox(10);
        card.setStyle(productCardStyle(false));
        card.setPrefWidth(220);
        card.setMinHeight(405);
        card.setPrefHeight(405);
        card.setAlignment(Pos.TOP_CENTER);
        card.setFillWidth(true);
        card.setOnMouseEntered(e -> card.setStyle(productCardStyle(true)));
        card.setOnMouseExited(e -> card.setStyle(productCardStyle(false)));

        ImageView imgView = new ImageView();
        String imagePath = product.getImagePath();
        if (imagePath != null && !imagePath.trim().isEmpty()) {
            try {
                imgView.setImage(loadProductImage(imagePath));
            } catch (Exception ex) {
                System.out.println("Lỗi tải ảnh: " + product.getName());
            }
        }
        imgView.setFitWidth(180);
        imgView.setFitHeight(130);
        imgView.setPreserveRatio(true);
        Rectangle clip = new Rectangle(180, 130);
        clip.setArcWidth(15);
        clip.setArcHeight(15);
        imgView.setClip(clip);

        StackPane imgContainer = new StackPane(imgView);
        imgContainer.setMinSize(190, 130);
        imgContainer.setPrefSize(190, 130);
        imgContainer.setMaxSize(190, 130);
        imgContainer.setAlignment(Pos.CENTER);
        Label badge = new Label();
        refreshBadge(badge, product);
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(5));
        imgContainer.getChildren().add(badge);

        Label nameLabel = new Label(product.getName() != null ? product.getName() : "Sản phẩm ẩn danh");
        nameLabel.setWrapText(true);
        nameLabel.setMinWidth(190);
        nameLabel.setPrefWidth(190);
        nameLabel.setMaxWidth(190);
        nameLabel.setMinHeight(72);
        nameLabel.setPrefHeight(72);
        nameLabel.setMaxHeight(72);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setTextAlignment(TextAlignment.CENTER);
        nameLabel.setStyle("-fx-font-weight:bold;-fx-font-size:15px;-fx-text-fill:#2c3e50;");

        Label bidSub = new Label("Giá hiện tại");
        bidSub.setMinWidth(190);
        bidSub.setPrefWidth(190);
        bidSub.setMaxWidth(190);
        bidSub.setAlignment(Pos.CENTER);
        bidSub.setTextAlignment(TextAlignment.CENTER);
        bidSub.setStyle("-fx-text-fill:#7f8c8d;-fx-font-size:11px;");

        Label priceLabel = new Label(formatVND(product.getCurrentBid()));
        priceLabel.setMinWidth(190);
        priceLabel.setPrefWidth(190);
        priceLabel.setMaxWidth(190);
        priceLabel.setAlignment(Pos.CENTER);
        priceLabel.setTextAlignment(TextAlignment.CENTER);
        priceLabel.setStyle("-fx-text-fill:#e74c3c;-fx-font-weight:bold;-fx-font-size:16px;");

        Label timeLabel = new Label();
        refreshTimeLabel(timeLabel, product);
        timeLabel.setAlignment(Pos.CENTER);
        timeLabel.setTextAlignment(TextAlignment.CENTER);

        VBox priceBox = new VBox(8);
        priceBox.setMinHeight(86);
        priceBox.setPrefHeight(86);
        priceBox.setMaxHeight(86);
        priceBox.setMinWidth(190);
        priceBox.setPrefWidth(190);
        priceBox.setMaxWidth(190);
        priceBox.setAlignment(Pos.CENTER);
        priceBox.setFillWidth(true);
        priceBox.getChildren().addAll(bidSub, priceLabel, timeLabel);

        Button btnBid = new Button(product.isEnded() ? "Xem kết quả" : "Bid Now");
        btnBid.setStyle(bidButtonStyle(product, false));
        btnBid.setMinWidth(190);
        btnBid.setPrefWidth(190);
        btnBid.setMaxWidth(Double.MAX_VALUE);
        btnBid.setAlignment(Pos.CENTER);
        btnBid.setTextAlignment(TextAlignment.CENTER);
        btnBid.setOnMouseEntered(e -> btnBid.setStyle(bidButtonStyle(product, true)));
        btnBid.setOnMouseExited(e -> btnBid.setStyle(bidButtonStyle(product, false)));
        btnBid.setOnAction(event -> openAuctionRoom(product, event));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(imgContainer, nameLabel, priceBox, spacer, btnBid);
        currentCardBindings.add(new ProductCardBinding(product, priceLabel, timeLabel, badge, btnBid));
        return card;
    }

    private Image loadProductImage(String imagePath) {
        Image cachedImage = imageCache.get(imagePath);
        if (cachedImage != null) {
            return cachedImage;
        }

        Image image = new Image(imagePath, true);
        imageCache.put(imagePath, image);
        return image;
    }

    private void refreshTimeLabel(Label label, Product product) {
        if (product.isEnded()) {
            label.setText("Đã kết thúc");
            label.setStyle("-fx-text-fill:#7f8c8d;-fx-font-weight:bold;"
                + "-fx-background-color:#ecf0f1;-fx-padding:3px 8px;-fx-background-radius:5px;");
            return;
        }

        int secs = product.getSecondsRemainingNow();
        if (secs <= 0) {
            product.setStatus("ended");
            label.setText("Đã kết thúc");
            label.setStyle("-fx-text-fill:#7f8c8d;-fx-font-weight:bold;"
                + "-fx-background-color:#ecf0f1;-fx-padding:3px 8px;-fx-background-radius:5px;");
            return;
        }

        int h = secs / 3600;
        int m = (secs % 3600) / 60;
        int s = secs % 60;
        label.setText(h > 0 ? String.format("%02d:%02d:%02d", h, m, s)
            : String.format("%02d:%02d", m, s));
        String color = secs <= 60 ? "#e74c3c" : "#2980b9";
        String bg = secs <= 60 ? "#fdecea" : "#ebf5fb";
        label.setStyle("-fx-text-fill:" + color + ";-fx-font-weight:bold;"
            + "-fx-background-color:" + bg + ";-fx-padding:3px 8px;-fx-background-radius:5px;");
    }

    private void refreshBadge(Label badge, Product product) {
        if (product.isEnded()) {
            badge.setText("ĐÃ KẾT THÚC");
            badge.setStyle("-fx-background-color:#7f8c8d;-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-font-size:10px;-fx-padding:4px 8px;-fx-background-radius:5px;");
        } else if (product.getSecondsRemainingNow() <= 60) {
            badge.setText("SẮP KẾT THÚC");
            badge.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-font-size:10px;-fx-padding:4px 8px;-fx-background-radius:5px;");
        } else {
            badge.setText("ĐANG ĐẤU GIÁ");
            badge.setStyle("-fx-background-color:#27ae60;-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-font-size:10px;-fx-padding:4px 8px;-fx-background-radius:5px;");
        }
    }

    private String productCardStyle(boolean hover) {
        String shadow = hover
            ? "dropshadow(three-pass-box,rgba(0,0,0,0.3),15,0,0,5)"
            : "dropshadow(three-pass-box,rgba(0,0,0,0.1),5,0,0,2)";
        return "-fx-background-color:white;"
            + "-fx-padding:15px;"
            + "-fx-background-radius:10px;"
            + "-fx-border-color:transparent;"
            + "-fx-effect:" + shadow + ";"
            + (hover ? "-fx-cursor:hand;" : "");
    }

    private String bidButtonStyle(Product product, boolean hover) {
        boolean ended = product != null && product.isEnded();
        String color = ended
            ? (hover ? "#95a5a6" : "#7f8c8d")
            : (hover ? "#2ecc71" : "#27ae60");

        return "-fx-background-color:" + color + ";"
            + "-fx-text-fill:white;"
            + "-fx-font-weight:bold;"
            + "-fx-cursor:hand;"
            + "-fx-background-radius:5px;"
            + "-fx-border-color:transparent;"
            + "-fx-padding:8px 15px;";
    }

    private String formatVND(double amount) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount) + " VNĐ";
    }

    private void openAuctionRoom(Product product, ActionEvent event) {
        if (globalCardTimer != null) {
            globalCardTimer.stop();
        }
        stopNotificationPolling();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/AuctionRoom.fxml"));
            Parent root = loader.load();
            AuctionRoomController controller = loader.getController();
            controller.setProductData(product);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Auction Room - " + (product.getName() != null ? product.getName() : "Sản phẩm"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void applySmoothHover(Button button) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(150), button);
        button.setOnMouseEntered(e -> {
            transition.setToX(1.1);
            transition.setToY(1.1);
            transition.playFromStart();
        });
        button.setOnMouseExited(e -> {
            transition.setToX(1.0);
            transition.setToY(1.0);
            transition.playFromStart();
        });
    }

    @FXML
    private void handleLogoutAction(ActionEvent event) {
        if (globalCardTimer != null) {
            globalCardTimer.stop();
        }
        stopNotificationPolling();
        try {
            AuthService.logout();
            FirebaseService.currentUserEmail = null;
            FirebaseService.registeredUsername = null;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) productContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Online Auction System - Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowInfo(ActionEvent event) {
        String user = currentUserEmail();
        if (user != null) {
            String phone = FirebaseService.getUserPhone(user);
            btnInfo.setText("Email: " + user + "\nSĐT: " + phone);
            btnInfo.setStyle("-fx-background-color:transparent;-fx-text-fill:#27ae60;"
                + "-fx-font-weight:bold;-fx-cursor:default;");
        } else {
            btnInfo.setText("Chưa đăng nhập!");
        }
    }

    @FXML
    private void handleGoToAddProduct(ActionEvent event) {
        if (globalCardTimer != null) {
            globalCardTimer.stop();
        }
        stopNotificationPolling();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/AddProduct.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Online Auction System - Add Product");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToUserProducts(ActionEvent event) {
        if (globalCardTimer != null) {
            globalCardTimer.stop();
        }
        stopNotificationPolling();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/UserProducts.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) productContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Online Auction System - Quản lý sản phẩm của tôi");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String nvl(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private String currentUserEmail() {
        if (AuthService.currentUserEmail != null && !AuthService.currentUserEmail.isBlank()) {
            return AuthService.currentUserEmail;
        }
        if (FirebaseService.currentUserEmail != null && !FirebaseService.currentUserEmail.isBlank()) {
            return FirebaseService.currentUserEmail;
        }
        return FirebaseService.registeredUsername;
    }
}
