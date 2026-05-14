package client.controller;

import client.model.Product;
import client.model.User;
import client.service.AuthService;
import client.service.FirebaseService;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminController {

    @FXML private StackPane contentArea;
    @FXML private Label lblTotalUsers;
    @FXML private Label lblActiveAuctions;
    
    // Các nút bấm Menu
    @FXML private Button btnDashboard;
    @FXML private Button btnManageUsers;
    @FXML private Button btnManageProducts;
    @FXML private PieChart categoryChart;

    private Node dashboardView;

    @FXML
    public void initialize() {
        if (!contentArea.getChildren().isEmpty()) {
            dashboardView = contentArea.getChildren().get(0);
        }
        
        // Mặc định chọn nút Dashboard khi mới vào
        setActiveButton(btnDashboard);
        loadDashboardStats();
    }

    private void loadDashboardStats() {
        new Thread(() -> {
            List<Product> allProducts = FirebaseService.getAllProducts();
            
            // Đếm số phiên đang mở
            long activeCount = allProducts.stream()
                                          .filter(p -> "active".equals(p.getStatus()))
                                          .count();

            // Tính toán dữ liệu cho Biểu đồ (Nhóm theo Danh mục và Đếm)
            Map<String, Long> categoryCounts = allProducts.stream()
                    .collect(Collectors.groupingBy(
                            p -> (p.getCategory() != null && !p.getCategory().isEmpty()) ? p.getCategory() : "Khác",
                            Collectors.counting()
                    ));

            Platform.runLater(() -> {
                if (lblActiveAuctions != null) lblActiveAuctions.setText(String.valueOf(activeCount));
                
                // Đổ dữ liệu vào PieChart
                if (categoryChart != null) {
                    ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
                    categoryCounts.forEach((category, count) -> {
                        pieChartData.add(new PieChart.Data(category + " (" + count + ")", count));
                    });
                    categoryChart.setData(pieChartData);

                    // ĐÃ THÊM: Gắn tương tác sau khi đổ dữ liệu
                    setupPieChartInteraction(); 
                }
            });
            List<User> allUsers = FirebaseService.getAllUsers();
            Platform.runLater(() -> {
                if (lblTotalUsers != null) lblTotalUsers.setText(String.valueOf(allUsers.size()));
            });
        }).start();
    }

    // ==============================================================
    // ĐÃ THÊM: CỖ MÁY GẮN TƯƠNG TÁC CHO BIỂU ĐỒ (HOVER EFFECT)
    // ==============================================================
    private void setupPieChartInteraction() {
        if (categoryChart == null || categoryChart.getData().isEmpty()) return;

        for (final PieChart.Data data : categoryChart.getData()) {
            Node node = data.getNode();
            
            // Tạo hiệu ứng bóng đổ phát sáng (Glow effect) để trông chuyên nghiệp hơn
            DropShadow glow = new DropShadow();
            glow.setColor(Color.valueOf("#deff9a")); // Màu vàng xanh sáng đặc trưng của Admin Panel
            glow.setRadius(20);
            glow.setSpread(0.2);

            // Khi di chuột vào
            node.setOnMouseEntered(event -> {
                // Hiệu ứng phóng to mượt mà (X và Y nở ra 1.1 lần trong 0.15s)
                ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), node);
                scaleUp.setToX(1.1);
                scaleUp.setToY(1.1);
                scaleUp.play();

                node.setEffect(glow); // Thêm bóng đổ phát sáng
                node.toFront(); // Đưa phần này lên trước để không bị che
                node.setCursor(javafx.scene.Cursor.HAND); // Đổi con trỏ thành hình bàn tay
            });

            // Khi di chuột ra
            node.setOnMouseExited(event -> {
                // Hiệu ứng thu nhỏ lại mượt mà về 1.0 lần
                ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), node);
                scaleDown.setToX(1.0);
                scaleDown.setToY(1.0);
                scaleDown.play();

                node.setEffect(null); // Xóa hiệu ứng bóng đổ
                node.setCursor(javafx.scene.Cursor.DEFAULT); // Trả lại con trỏ mặc định
            });
        }
    }

    // ==============================================================
    // CỖ MÁY CHUYỂN CẢNH MƯỢT MÀ & ĐỔI MÀU NÚT
    // ==============================================================
    private void switchView(Node view, Button activeBtn) {
        setActiveButton(activeBtn);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(view);
        FadeTransition ft = new FadeTransition(Duration.millis(300), view);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    private void setActiveButton(Button activeBtn) {
        btnDashboard.getStyleClass().remove("sidebar-button-active");
        btnManageUsers.getStyleClass().remove("sidebar-button-active");
        btnManageProducts.getStyleClass().remove("sidebar-button-active");
        if (activeBtn != null) {
            activeBtn.getStyleClass().add("sidebar-button-active");
        }
    }

    // ==============================================================
    // CÁC HÀM XỬ LÝ SỰ KIỆN MENU
    // ==============================================================
    @FXML
    private void showDashboard(ActionEvent event) {
        if (dashboardView != null) {
            switchView(dashboardView, btnDashboard);
            loadDashboardStats(); 
        }
    }

    @FXML
    private void showUserManagement(ActionEvent event) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/client/view/ManageUsers.fxml"));
            switchView(view, btnManageUsers);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void showProductManagement(ActionEvent event) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/client/view/ManageProducts.fxml"));
            switchView(view, btnManageProducts);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            AuthService.logout();
            FirebaseService.currentUserEmail = null;
            FirebaseService.registeredUsername = null;
            Parent root = FXMLLoader.load(getClass().getResource("/client/view/Login.fxml"));
            Stage
             stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Online Auction System - Login");
        } catch (IOException e) { e.printStackTrace(); }
    }
}
