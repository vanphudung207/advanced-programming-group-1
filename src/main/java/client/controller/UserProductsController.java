package client.controller;

import client.model.Product;
import client.service.AuthService;
import client.service.FirebaseService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class UserProductsController {

    @FXML private Label lblCurrentUser;
    @FXML private Label lblTotalProducts;
    @FXML private TableView<Product> tableProducts;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, String> colStatus;
    @FXML private TableColumn<Product, String> colEndTime;
    @FXML private TableColumn<Product, Void> colAction;

    private final ObservableList<Product> productData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        String currentUser = currentUserEmail();
        lblCurrentUser.setText(currentUser != null ? currentUser : "Chưa đăng nhập");

        tableProducts.setPlaceholder(createEmptyLabel("Bạn chưa đăng sản phẩm nào."));
        tableProducts.setItems(productData);

        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentBid"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("timeRemaining"));

        addTooltipToTextColumn(colName);
        addTooltipToTextColumn(colCategory);
        addTooltipToTextColumn(colEndTime);
        setupPriceColumn();
        setupStatusColumn();
        setupActionColumn();
        loadUserProducts();
    }

    private void loadUserProducts() {
        new Thread(() -> {
            List<Product> products = FirebaseService.getProductsBySeller(currentUserEmail());
            Platform.runLater(() -> {
                productData.setAll(products);
                updateTotalLabel();
            });
        }).start();
    }

    private Label createEmptyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("user-table-empty");
        return label;
    }

    private void updateTotalLabel() {
        lblTotalProducts.setText(String.valueOf(productData.size()));
    }

    private void addTooltipToTextColumn(TableColumn<Product, String> column) {
        column.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    Tooltip tooltip = new Tooltip(item);
                    tooltip.setShowDelay(Duration.seconds(1));
                    setTooltip(tooltip);
                }
            }
        });
    }

    private void setupPriceColumn() {
        colPrice.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : formatVND(price));
            }
        });
    }

    private void setupStatusColumn() {
        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Product product = getTableRow() != null ? getTableRow().getItem() : null;
                boolean ended = product != null && product.isEnded();
                Label badge = new Label(ended ? "Đã kết thúc" : "Đang đấu giá");
                badge.getStyleClass().add(ended ? "status-badge-ended" : "status-badge-active");
                setText(null);
                setGraphic(badge);
            }
        });
    }

    private void setupActionColumn() {
        Callback<TableColumn<Product, Void>, TableCell<Product, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Product, Void> call(TableColumn<Product, Void> param) {
                return new TableCell<>() {
                    private final Button btnEdit = new Button("Sửa");
                    private final Button btnDelete = new Button("Xóa");
                    private final HBox actions = new HBox(10, btnEdit, btnDelete);

                    {
                        actions.setAlignment(Pos.CENTER);
                        btnEdit.getStyleClass().add("user-action-edit");
                        btnDelete.getStyleClass().add("user-action-delete");

                        btnEdit.setOnAction(event -> {
                            Product product = getTableView().getItems().get(getIndex());
                            openEditProduct(product, event);
                        });

                        btnDelete.setOnAction(event -> {
                            Product product = getTableView().getItems().get(getIndex());
                            confirmDeleteProduct(product);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                            return;
                        }

                        Product product = getTableView().getItems().get(getIndex());
                        btnEdit.setDisable(product.isEnded());
                        setGraphic(actions);
                    }
                };
            }
        };
        colAction.setCellFactory(cellFactory);
    }

    private void openEditProduct(Product product, ActionEvent event) {
        if (product == null || product.isEnded()) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/AddProduct.fxml"));
            Parent root = loader.load();
            AddProductController controller = loader.getController();
            controller.setProductForEdit(product);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Online Auction System - Sửa sản phẩm");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở màn hình chỉnh sửa sản phẩm.");
        }
    }

    private void confirmDeleteProduct(Product product) {
        if (product == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xóa sản phẩm");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc muốn xóa sản phẩm \"" + product.getName() + "\"?");

        confirm.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;

            new Thread(() -> {
                boolean deleted = FirebaseService.deleteProductIfOwner(
                    product.getFirebaseKey(),
                    currentUserEmail()
                );

                Platform.runLater(() -> {
                    if (deleted) {
                        productData.remove(product);
                        updateTotalLabel();
                        showAlert(Alert.AlertType.INFORMATION, "Đã xóa",
                            "Sản phẩm đã được xóa khỏi danh sách của bạn.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Không thể xóa",
                            "Bạn chỉ có thể xóa sản phẩm do chính tài khoản của mình đăng.");
                    }
                });
            }).start();
        });
    }

    @FXML
    private void handleBackToProductList(ActionEvent event) {
        navigateTo(event, "/client/view/ProductList.fxml", "Online Auction System - Danh sách sản phẩm");
    }

    @FXML
    private void handleGoToAddProduct(ActionEvent event) {
        navigateTo(event, "/client/view/AddProduct.fxml", "Online Auction System - Add Product");
    }

    private void navigateTo(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể chuyển màn hình.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
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

    private String formatVND(double amount) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount) + " VNĐ";
    }
}
