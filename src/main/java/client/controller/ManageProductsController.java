package client.controller;

import client.model.Product;
import client.service.FirebaseService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ManageProductsController {

    @FXML private TableView<Product> tableProducts;
    
    // Khai báo các cột đã đặt fx:id bên FXML
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colSeller;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, String> colStatus;
    @FXML private TableColumn<Product, Void> colAction;

    @FXML
    public void initialize() {

        // --- Bổ sung Empty Placeholder ---
        Label emptyLabel = new Label("📦 Không có dữ liệu để hiển thị.");
        emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-font-style: italic;");
        tableProducts.setPlaceholder(emptyLabel);
        setupActionColumn();
        
        // Gắn Tooltip chờ 2 giây cho các cột chữ
        addTooltipToTextColumn(colName);
        addTooltipToTextColumn(colSeller);
        addTooltipToTextColumn(colStatus);
        
        // Gắn Tooltip và Format tiền tệ cho cột Giá
        setupPriceColumn();

        loadProducts();
    }

    private void loadProducts() {
        new Thread(() -> {
            List<Product> products = FirebaseService.getAllProducts();
            ObservableList<Product> data = FXCollections.observableArrayList(products);
            Platform.runLater(() -> {
                tableProducts.setItems(data);
            });
        }).start();
    }

    // ==============================================================
    // CỖ MÁY TẠO TOOLTIP (CHỜ 2 GIÂY MỚI HIỆN)
    // ==============================================================
    private void addTooltipToTextColumn(TableColumn<Product, String> column) {
        column.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item); // Vẫn hiện chữ (có thể bị ...)
                    
                    // Tạo Tooltip chứa nội dung đầy đủ
                    Tooltip tooltip = new Tooltip(item);
                    tooltip.setShowDelay(Duration.seconds(2)); // Trễ 2 giây
                    tooltip.setStyle("-fx-font-size: 14px; -fx-background-color: #334155;");
                    setTooltip(tooltip);
                }
            }
        });
    }

    // ==============================================================
    // FORMAT CỘT GIÁ TIỀN & THÊM TOOLTIP
    // ==============================================================
    private void setupPriceColumn() {
        colPrice.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    // Format giá tiền đẹp: 1.0E7 -> 10.000.000 VNĐ
                    String formattedPrice = formatVND(price);
                    setText(formattedPrice);
                    
                    Tooltip tooltip = new Tooltip(formattedPrice);
                    tooltip.setShowDelay(Duration.seconds(2));
                    tooltip.setStyle("-fx-font-size: 14px; -fx-background-color: #334155;");
                    setTooltip(tooltip);
                }
            }
        });
    }

    // ==============================================================
    // CỘT NÚT BẤM THAO TÁC
    // ==============================================================
    private void setupActionColumn() {
        Callback<TableColumn<Product, Void>, TableCell<Product, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Product, Void> call(final TableColumn<Product, Void> param) {
                return new TableCell<>() {
                    private final Button btnStop = new Button("Dừng");
                    private final Button btnDelete = new Button("Xóa");
                    private final HBox pane = new HBox(10, btnStop, btnDelete);

                    {
                        btnStop.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 15 5 15;");
                        btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 15 5 15;");

                        btnStop.setOnAction((ActionEvent event) -> {
                            Product data = getTableView().getItems().get(getIndex());
                            FirebaseService.markAuctionEnded(data.getFirebaseKey());
                            data.setStatus("ended");
                            tableProducts.refresh();
                        });

                        btnDelete.setOnAction((ActionEvent event) -> {
                            Product data = getTableView().getItems().get(getIndex());
                            FirebaseService.deleteProduct(data.getFirebaseKey());
                            getTableView().getItems().remove(data);
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            Product data = getTableView().getItems().get(getIndex());
                            btnStop.setDisable("ended".equals(data.getStatus()));
                            setGraphic(pane);
                        }
                    }
                };
            }
        };
        colAction.setCellFactory(cellFactory);
    }

    private String formatVND(double amount) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount) + " VNĐ";
    }
}
