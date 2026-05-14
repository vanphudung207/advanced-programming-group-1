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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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

    private List<Product> currentDisplayedProducts = new ArrayList<>();
    private Timeline globalCardTimer;

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

        if (categoryBox != null) {
            for (Node n : categoryBox.getChildren()) {
                if (n instanceof Button) {
                    applySmoothHover((Button) n);
                }
            }
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
                    List<Product> freshData = FirebaseService.getAllProducts();
                    Platform.runLater(() -> {
                        boolean searchEmpty = txtSearch == null || txtSearch.getText().trim().isEmpty();
                        boolean defaultSort = cbSort == null || "Mặc định".equals(cbSort.getValue());
                        if (searchEmpty && defaultSort) {
                            currentDisplayedProducts = freshData;
                            loadProductsToGrid(currentDisplayedProducts);
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

    private void fetchDataAsync() {
        showLoadingState();
        Thread thread = new Thread(() -> {
            List<Product> data = FirebaseService.getAllProducts();
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
                if (!a.isEnded()) {
                    return Long.compare(a.getEndTime(), b.getEndTime());
                }
                return Long.compare(b.getEndTime(), a.getEndTime());
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
        showLoadingState();

        Thread thread = new Thread(() -> {
            List<Product> data = FirebaseService.getProductsByCategory(category);
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

    private void showLoadingState() {
        if (productContainer == null) {
            return;
        }
        if (globalCardTimer != null) {
            globalCardTimer.stop();
        }
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

        productContainer.setAlignment(Pos.TOP_LEFT);
        List<Label> cardTimerLabels = new ArrayList<>();
        List<Label> cardBadgeLabels = new ArrayList<>();
        List<Product> cardProducts = new ArrayList<>();

        for (Product product : products) {
            VBox card = createProductCard(product, cardTimerLabels, cardBadgeLabels, cardProducts);
            productContainer.getChildren().add(card);
        }

        globalCardTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            for (int i = 0; i < cardProducts.size(); i++) {
                Product p = cardProducts.get(i);
                refreshTimeLabel(cardTimerLabels.get(i), p);
                refreshBadge(cardBadgeLabels.get(i), p);
            }
        }));
        globalCardTimer.setCycleCount(Animation.INDEFINITE);
        globalCardTimer.play();
    }

    private VBox createProductCard(Product product, List<Label> timerLabels,
                                   List<Label> badgeLabels, List<Product> cardProducts) {
        VBox card = new VBox(10);
        String normalStyle = "-fx-background-color:white;-fx-padding:15px;-fx-background-radius:10px;"
            + "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.1),5,0,0,2);";
        String hoverStyle = "-fx-background-color:white;-fx-padding:15px;-fx-background-radius:10px;"
            + "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.3),15,0,0,5);-fx-cursor:hand;";
        card.setStyle(normalStyle);
        card.setPrefWidth(220);
        card.setMinHeight(405);
        card.setPrefHeight(405);
        card.setAlignment(Pos.TOP_CENTER);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(normalStyle));

        ImageView imgView = new ImageView();
        String imagePath = product.getImagePath();
        if (imagePath != null && !imagePath.trim().isEmpty()) {
            try {
                imgView.setImage(new Image(imagePath, true));
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
        Label badge = new Label();
        refreshBadge(badge, product);
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(5));
        imgContainer.getChildren().add(badge);

        Label nameLabel = new Label(product.getName() != null ? product.getName() : "Sản phẩm ẩn danh");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(190);
        nameLabel.setMinHeight(72);
        nameLabel.setPrefHeight(72);
        nameLabel.setMaxHeight(72);
        nameLabel.setAlignment(Pos.TOP_LEFT);
        nameLabel.setStyle("-fx-font-weight:bold;-fx-font-size:15px;-fx-text-fill:#2c3e50;");

        Label bidSub = new Label("Giá hiện tại");
        bidSub.setStyle("-fx-text-fill:#7f8c8d;-fx-font-size:11px;");

        Label priceLabel = new Label(String.format("%,.0f VNĐ", product.getCurrentBid()));
        priceLabel.setMaxWidth(190);
        priceLabel.setStyle("-fx-text-fill:#e74c3c;-fx-font-weight:bold;-fx-font-size:16px;");

        Label timeLabel = new Label();
        refreshTimeLabel(timeLabel, product);

        VBox priceBox = new VBox(8);
        priceBox.setMinHeight(86);
        priceBox.setPrefHeight(86);
        priceBox.setMaxHeight(86);
        priceBox.setAlignment(Pos.CENTER);
        priceBox.getChildren().addAll(bidSub, priceLabel, timeLabel);

        Button btnBid = new Button(product.isEnded() ? "Xem kết quả" : "Bid Now");
        btnBid.setStyle(product.isEnded()
            ? "-fx-background-color:#7f8c8d;-fx-text-fill:white;-fx-font-weight:bold;-fx-cursor:hand;-fx-background-radius:5px;-fx-padding:8px 15px;"
            : "-fx-background-color:#27ae60;-fx-text-fill:white;-fx-font-weight:bold;-fx-cursor:hand;-fx-background-radius:5px;-fx-padding:8px 15px;");
        btnBid.setMaxWidth(Double.MAX_VALUE);
        btnBid.setOnAction(ev -> openAuctionRoom(product, ev));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(imgContainer, nameLabel, priceBox, spacer, btnBid);
        timerLabels.add(timeLabel);
        badgeLabels.add(badge);
        cardProducts.add(product);
        return card;
    }

    private void refreshTimeLabel(Label label, Product p) {
        if (p.isEnded()) {
            label.setText("Đã kết thúc");
            label.setStyle("-fx-text-fill:#7f8c8d;-fx-font-weight:bold;"
                + "-fx-background-color:#ecf0f1;-fx-padding:3px 8px;-fx-background-radius:5px;");
            return;
        }

        int secs = p.getSecondsRemainingNow();
        if (secs <= 0) {
            p.setStatus("ended");
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

    private void refreshBadge(Label badge, Product p) {
        if (p.isEnded()) {
            badge.setText("ĐÃ KẾT THÚC");
            badge.setStyle("-fx-background-color:#7f8c8d;-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-font-size:10px;-fx-padding:4px 8px;-fx-background-radius:5px;");
        } else if (p.getSecondsRemainingNow() <= 60) {
            badge.setText("SẮP KẾT THÚC");
            badge.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-font-size:10px;-fx-padding:4px 8px;-fx-background-radius:5px;");
        } else {
            badge.setText("ĐANG ĐẤU GIÁ");
            badge.setStyle("-fx-background-color:#27ae60;-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-font-size:10px;-fx-padding:4px 8px;-fx-background-radius:5px;");
        }
    }

    private void openAuctionRoom(Product product, ActionEvent event) {
        if (globalCardTimer != null) {
            globalCardTimer.stop();
        }
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

    private void applySmoothHover(Button btn) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(150), btn);
        btn.setOnMouseEntered(e -> {
            transition.setToX(1.1);
            transition.setToY(1.1);
            transition.playFromStart();
        });
        btn.setOnMouseExited(e -> {
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
        try {
            AuthService.currentUserEmail = null;
            FirebaseService.currentUserEmail = null;

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
        if (AuthService.currentUserEmail != null) {
            btnInfo.setText("Email: " + AuthService.currentUserEmail);
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
}
