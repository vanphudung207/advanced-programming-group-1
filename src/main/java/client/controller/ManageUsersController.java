package client.controller;

import client.model.User;
import client.service.FirebaseService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

import java.util.List;

public class ManageUsersController {

    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, Void> colAction;

    @FXML
    public void initialize() {
        // --- Bổ sung Empty Placeholder ---
        Label emptyLabel = new Label("📦 Không có dữ liệu để hiển thị.");
        emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-font-style: italic;");
        tableUsers.setPlaceholder(emptyLabel);
        setupActionColumn();
        loadUsers();
    }

    private void loadUsers() {
        new Thread(() -> {
            List<User> users = FirebaseService.getAllUsers();
            ObservableList<User> data = FXCollections.observableArrayList(users);
            Platform.runLater(() -> {
                tableUsers.setItems(data);
            });
        }).start();
    }

    // ==============================================================
    // TẠO NÚT BẤM "KHÓA / MỞ KHÓA" ĐỘNG TRONG BẢNG
    // ==============================================================
    private void setupActionColumn() {
        Callback<TableColumn<User, Void>, TableCell<User, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<User, Void> call(final TableColumn<User, Void> param) {
                return new TableCell<>() {
                    private final Button btn = new Button();

                    {
                        // Khi Admin bấm nút
                        btn.setOnAction((ActionEvent event) -> {
                            User data = getTableView().getItems().get(getIndex());
                            
                            // Đảo ngược trạng thái
                            String newStatus = data.getStatus().equals("active") ? "banned" : "active";
                            
                            // Lưu lên Firebase
                            FirebaseService.toggleUserStatus(data.getEmail(), newStatus);
                            
                            // Cập nhật lại giao diện ngay lập tức
                            data.setStatus(newStatus);
                            tableUsers.refresh();
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            User data = getTableView().getItems().get(getIndex());
                            
                            // Không cho phép tự khóa tài khoản Admin
                            if ("admin".equalsIgnoreCase(data.getRole()) || "Admin@gmail.com".equals(data.getEmail())) {
                                setGraphic(new Label("⭐ Quản trị viên"));
                            } else {
                                // Tự động đổi màu và chữ tùy theo trạng thái
                                if ("active".equals(data.getStatus())) {
                                    btn.setText("Khóa (Ban)");
                                    btn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
                                } else {
                                    btn.setText("Mở Khóa");
                                    btn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-cursor: hand;");
                                }
                                setGraphic(btn);
                            }
                        }
                    }
                };
            }
        };
        colAction.setCellFactory(cellFactory);
    }
}